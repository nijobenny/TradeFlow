package com.nijobenny.tradeflow.server.routes

import com.nijobenny.tradeflow.core.CustomerDetailDto
import com.nijobenny.tradeflow.core.CustomerDto
import com.nijobenny.tradeflow.core.CustomerInput
import com.nijobenny.tradeflow.core.CustomerTier
import com.nijobenny.tradeflow.core.LiveEventType
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.PointDto
import com.nijobenny.tradeflow.core.Validation
import com.nijobenny.tradeflow.server.DAY_MS
import com.nijobenny.tradeflow.server.LiveBus
import com.nijobenny.tradeflow.server.badRequest
import com.nijobenny.tradeflow.server.db.Customers
import com.nijobenny.tradeflow.server.db.Invoices
import com.nijobenny.tradeflow.server.db.Orders
import com.nijobenny.tradeflow.server.db.dbQuery
import com.nijobenny.tradeflow.server.intParam
import com.nijobenny.tradeflow.server.notFound
import com.nijobenny.tradeflow.server.now
import com.nijobenny.tradeflow.server.recordActivity
import com.nijobenny.tradeflow.server.requirePermission
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.*

data class CustomerStats(
    val outstandingCents: Long = 0,
    val totalSpentCents: Long = 0,
    val orderCount: Int = 0,
    val lastOrderAt: Long? = null,
)

/**
 * Per-customer totals. Outstanding = unpaid invoice balances + pending orders not invoiced yet,
 * which is what counts against the credit limit. Must run inside a transaction.
 */
fun customerStats(customerId: Int? = null): Map<Int, CustomerStats> {
    val orders = Orders.select(Orders.customerId, Orders.status, Orders.totalCents, Orders.createdAt)
        .where { if (customerId != null) Orders.customerId eq customerId else Op.TRUE }
        .map { Triple(it[Orders.customerId], it[Orders.status], it[Orders.totalCents]) to it[Orders.createdAt] }
    val invoices = Invoices.select(Invoices.customerId, Invoices.amountCents, Invoices.paidCents, Invoices.voided)
        .where { if (customerId != null) Invoices.customerId eq customerId else Op.TRUE }
        .map { it[Invoices.customerId] to if (it[Invoices.voided]) 0L else (it[Invoices.amountCents] - it[Invoices.paidCents]).coerceAtLeast(0) }

    val result = mutableMapOf<Int, CustomerStats>()
    orders.forEach { (order, createdAt) ->
        val (cid, status, total) = order
        val s = result[cid] ?: CustomerStats()
        result[cid] = s.copy(
            outstandingCents = s.outstandingCents + if (status == OrderStatus.PENDING) total else 0L,
            totalSpentCents = s.totalSpentCents + if (status != OrderStatus.CANCELLED) total else 0L,
            orderCount = s.orderCount + 1,
            lastOrderAt = maxOf(s.lastOrderAt ?: 0L, createdAt),
        )
    }
    invoices.forEach { (cid, balance) ->
        val s = result[cid] ?: CustomerStats()
        result[cid] = s.copy(outstandingCents = s.outstandingCents + balance)
    }
    return result
}

fun ResultRow.toCustomerDto(stats: CustomerStats?) = CustomerDto(
    id = this[Customers.id],
    name = this[Customers.name],
    contactName = this[Customers.contactName],
    email = this[Customers.email],
    phone = this[Customers.phone],
    city = this[Customers.city],
    country = this[Customers.country],
    tier = this[Customers.tier],
    creditLimitCents = this[Customers.creditLimitCents],
    paymentTermsDays = this[Customers.paymentTermsDays],
    outstandingCents = stats?.outstandingCents ?: 0,
    totalSpentCents = stats?.totalSpentCents ?: 0,
    orderCount = stats?.orderCount ?: 0,
    lastOrderAt = stats?.lastOrderAt,
)

/** One customer with live totals. Must run inside a transaction. */
fun loadCustomer(id: Int): CustomerDto {
    val row = Customers.selectAll().where { Customers.id eq id }.singleOrNull() ?: notFound("Customer")
    return row.toCustomerDto(customerStats(id)[id])
}

fun Route.customerRoutes() {
    route("/customers") {
        // GET /api/customers?q=&tier=GOLD
        get {
            val q = call.request.queryParameters["q"]?.trim()?.lowercase().orEmpty()
            val tier = call.request.queryParameters["tier"]?.let { t -> CustomerTier.entries.firstOrNull { it.name == t } }
            val customers = dbQuery {
                val stats = customerStats()
                Customers.selectAll()
                    .where {
                        var op: Op<Boolean> = Op.TRUE
                        if (tier != null) op = op and (Customers.tier eq tier)
                        if (q.isNotEmpty()) {
                            op = op and (
                                (Customers.name.lowerCase() like "%$q%") or
                                    (Customers.contactName.lowerCase() like "%$q%") or
                                    (Customers.city.lowerCase() like "%$q%")
                                )
                        }
                        op
                    }
                    .map { it.toCustomerDto(stats[it[Customers.id]]) }
                    .sortedByDescending { it.totalSpentCents }
            }
            call.respond(customers)
        }

        get("/{id}") {
            val id = call.intParam()
            val detail = dbQuery {
                val customer = loadCustomer(id)
                val orders = orderSummaries(customerId = id, limit = 50)
                val invoices = loadInvoices(customerId = id)
                // Spend per month for the last 6 months.
                val monthMs = 30 * DAY_MS
                val spend = (5 downTo 0).map { monthsAgo ->
                    val end = now() - monthsAgo * monthMs
                    val start = end - monthMs
                    val total = orders.filter { it.createdAt in start until end && it.status != OrderStatus.CANCELLED }.sumOf { it.totalCents }
                    PointDto(if (monthsAgo == 0) "Now" else "-${monthsAgo}m", total)
                }
                CustomerDetailDto(customer, orders, invoices, spend)
            }
            call.respond(detail)
        }

        post {
            val user = call.requirePermission(Permission.MANAGE_CUSTOMERS)
            val input = call.receive<CustomerInput>()
            Validation.customer(input)?.let { badRequest(it) }
            val (customer, event) = dbQuery {
                val id = Customers.insert {
                    it[name] = input.name.trim()
                    it[contactName] = input.contactName.trim()
                    it[email] = input.email.trim().lowercase()
                    it[phone] = input.phone.trim()
                    it[city] = input.city.trim()
                    it[country] = input.country.trim()
                    it[tier] = input.tier
                    it[creditLimitCents] = input.creditLimitCents
                    it[paymentTermsDays] = input.paymentTermsDays
                    it[createdAt] = now()
                } get Customers.id
                val event = recordActivity(LiveEventType.CUSTOMER, "${user.name} onboarded ${input.name.trim()}", user, "customer", id)
                loadCustomer(id) to event
            }
            LiveBus.publish(event)
            call.respond(HttpStatusCode.Created, customer)
        }

        put("/{id}") {
            val user = call.requirePermission(Permission.MANAGE_CUSTOMERS)
            val id = call.intParam()
            val input = call.receive<CustomerInput>()
            Validation.customer(input)?.let { badRequest(it) }
            val (customer, event) = dbQuery {
                val updated = Customers.update({ Customers.id eq id }) {
                    it[name] = input.name.trim()
                    it[contactName] = input.contactName.trim()
                    it[email] = input.email.trim().lowercase()
                    it[phone] = input.phone.trim()
                    it[city] = input.city.trim()
                    it[country] = input.country.trim()
                    it[tier] = input.tier
                    it[creditLimitCents] = input.creditLimitCents
                    it[paymentTermsDays] = input.paymentTermsDays
                }
                if (updated == 0) notFound("Customer")
                val event = recordActivity(LiveEventType.CUSTOMER, "${user.name} updated ${input.name.trim()}", user, "customer", id)
                loadCustomer(id) to event
            }
            LiveBus.publish(event)
            call.respond(customer)
        }
    }
}
