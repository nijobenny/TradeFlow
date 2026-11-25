package com.nijobenny.tradeflow.server.routes

import com.nijobenny.tradeflow.core.InvoiceDto
import com.nijobenny.tradeflow.core.InvoiceListDto
import com.nijobenny.tradeflow.core.InvoiceStatus
import com.nijobenny.tradeflow.core.LiveEventDto
import com.nijobenny.tradeflow.core.LiveEventType
import com.nijobenny.tradeflow.core.Money
import com.nijobenny.tradeflow.core.PaymentDto
import com.nijobenny.tradeflow.core.PaymentMethod
import com.nijobenny.tradeflow.core.PaymentRequest
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.UserDto
import com.nijobenny.tradeflow.server.DAY_MS
import com.nijobenny.tradeflow.server.LiveBus
import com.nijobenny.tradeflow.server.badRequest
import com.nijobenny.tradeflow.server.conflict
import com.nijobenny.tradeflow.server.db.Customers
import com.nijobenny.tradeflow.server.db.Invoices
import com.nijobenny.tradeflow.server.db.Payments
import com.nijobenny.tradeflow.server.db.Users
import com.nijobenny.tradeflow.server.db.dbQuery
import com.nijobenny.tradeflow.server.db.invoiceNumber
import com.nijobenny.tradeflow.server.db.invoiceStatus
import com.nijobenny.tradeflow.server.db.orderNumber
import com.nijobenny.tradeflow.server.intParam
import com.nijobenny.tradeflow.server.notFound
import com.nijobenny.tradeflow.server.now
import com.nijobenny.tradeflow.server.recordActivity
import com.nijobenny.tradeflow.server.requirePermission
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.*

/** Invoices with customer names, live status and payments. Must run inside a transaction. */
fun loadInvoices(
    customerId: Int? = null,
    orderId: Int? = null,
    invoiceId: Int? = null,
): List<InvoiceDto> {
    val time = now()
    val rows = Invoices.join(Customers, JoinType.INNER, Invoices.customerId, Customers.id)
        .select(
            Invoices.id, Invoices.orderId, Invoices.customerId, Customers.name, Invoices.amountCents,
            Invoices.paidCents, Invoices.issuedAt, Invoices.dueAt, Invoices.voided,
        )
        .where {
            var op: Op<Boolean> = Op.TRUE
            if (customerId != null) op = op and (Invoices.customerId eq customerId)
            if (orderId != null) op = op and (Invoices.orderId eq orderId)
            if (invoiceId != null) op = op and (Invoices.id eq invoiceId)
            op
        }
        .orderBy(Invoices.issuedAt to SortOrder.DESC)
        .toList()
    if (rows.isEmpty()) return emptyList()
    val ids = rows.map { it[Invoices.id] }
    val payments = Payments.join(Users, JoinType.INNER, Payments.userId, Users.id)
        .selectAll()
        .where { Payments.invoiceId inList ids }
        .orderBy(Payments.createdAt to SortOrder.DESC)
        .groupBy(
            { it[Payments.invoiceId] },
            { PaymentDto(it[Payments.id], it[Payments.amountCents], it[Payments.method], it[Users.name], it[Payments.createdAt]) },
        )
    return rows.map {
        val id = it[Invoices.id]
        InvoiceDto(
            id = id,
            number = invoiceNumber(id),
            orderId = it[Invoices.orderId],
            orderNumber = orderNumber(it[Invoices.orderId]),
            customerId = it[Invoices.customerId],
            customerName = it[Customers.name],
            amountCents = it[Invoices.amountCents],
            paidCents = it[Invoices.paidCents],
            issuedAt = it[Invoices.issuedAt],
            dueAt = it[Invoices.dueAt],
            status = invoiceStatus(it[Invoices.amountCents], it[Invoices.paidCents], it[Invoices.dueAt], it[Invoices.voided], time),
            payments = payments[id].orEmpty(),
        )
    }
}

/** Applies a payment to an invoice. Must run inside a transaction. */
fun recordPayment(invoiceId: Int, amountCents: Long, method: PaymentMethod, user: UserDto, at: Long = now()): LiveEventDto {
    val row = Invoices.selectAll().where { Invoices.id eq invoiceId }.forUpdate().singleOrNull() ?: notFound("Invoice")
    if (row[Invoices.voided]) conflict("This invoice was voided")
    val balance = row[Invoices.amountCents] - row[Invoices.paidCents]
    if (amountCents <= 0) badRequest("Amount must be above zero")
    if (amountCents > balance) badRequest("Only ${Money.format(balance)} is left to pay")
    Payments.insert {
        it[Payments.invoiceId] = invoiceId
        it[Payments.amountCents] = amountCents
        it[Payments.method] = method
        it[Payments.userId] = user.id
        it[Payments.createdAt] = at
    }
    Invoices.update({ Invoices.id eq invoiceId }) { it[paidCents] = row[Invoices.paidCents] + amountCents }
    val customer = Customers.select(Customers.name).where { Customers.id eq row[Invoices.customerId] }.single()[Customers.name]
    val paidInFull = amountCents == balance
    return recordActivity(
        LiveEventType.PAYMENT,
        "${user.name} recorded ${Money.format(amountCents)} from $customer${if (paidInFull) " · ${invoiceNumber(invoiceId)} paid in full" else ""}",
        user, "invoice", invoiceId, at,
    )
}

fun Route.invoiceRoutes() {
    route("/invoices") {
        // GET /api/invoices?status=OVERDUE
        get {
            val status = call.request.queryParameters["status"]?.let { s -> InvoiceStatus.entries.firstOrNull { it.name == s } }
            val result = dbQuery {
                val all = loadInvoices()
                val monthStart = now() - 30 * DAY_MS
                val paidThisMonth = Payments.select(Payments.amountCents, Payments.createdAt)
                    .where { Payments.createdAt greaterEq monthStart }
                    .sumOf { it[Payments.amountCents] }
                InvoiceListDto(
                    items = if (status == null) all else all.filter { it.status == status },
                    outstandingCents = all.filter { it.status != InvoiceStatus.VOID }.sumOf { it.balanceCents },
                    overdueCents = all.filter { it.status == InvoiceStatus.OVERDUE }.sumOf { it.balanceCents },
                    paidThisMonthCents = paidThisMonth,
                )
            }
            call.respond(result)
        }

        get("/{id}") {
            val id = call.intParam()
            call.respond(dbQuery { loadInvoices(invoiceId = id).firstOrNull() ?: notFound("Invoice") })
        }

        // POST /api/invoices/{id}/payments  {"amountCents": 12000, "method": "IDEAL"}
        post("/{id}/payments") {
            val user = call.requirePermission(Permission.RECORD_PAYMENT)
            val id = call.intParam()
            val request = call.receive<PaymentRequest>()
            val (invoice, event) = dbQuery {
                val event = recordPayment(id, request.amountCents, request.method, user)
                loadInvoices(invoiceId = id).first() to event
            }
            LiveBus.publish(event)
            call.respond(invoice)
        }
    }
}
