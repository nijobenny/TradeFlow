package com.nijobenny.tradeflow.server.routes

import com.nijobenny.tradeflow.core.CreateOrderRequest
import com.nijobenny.tradeflow.core.LiveEventDto
import com.nijobenny.tradeflow.core.LiveEventType
import com.nijobenny.tradeflow.core.Money
import com.nijobenny.tradeflow.core.OrderDto
import com.nijobenny.tradeflow.core.OrderEventDto
import com.nijobenny.tradeflow.core.OrderLineDto
import com.nijobenny.tradeflow.core.OrderListDto
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.OrderSummaryDto
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.Permissions
import com.nijobenny.tradeflow.core.Pricing
import com.nijobenny.tradeflow.core.StatusUpdateRequest
import com.nijobenny.tradeflow.core.UserDto
import com.nijobenny.tradeflow.server.DAY_MS
import com.nijobenny.tradeflow.server.LiveBus
import com.nijobenny.tradeflow.server.badRequest
import com.nijobenny.tradeflow.server.conflict
import com.nijobenny.tradeflow.server.currentUser
import com.nijobenny.tradeflow.server.db.Customers
import com.nijobenny.tradeflow.server.db.Invoices
import com.nijobenny.tradeflow.server.db.OrderEvents
import com.nijobenny.tradeflow.server.db.OrderLines
import com.nijobenny.tradeflow.server.db.Orders
import com.nijobenny.tradeflow.server.db.Products
import com.nijobenny.tradeflow.server.db.Users
import com.nijobenny.tradeflow.server.db.dbQuery
import com.nijobenny.tradeflow.server.db.orderNumber
import com.nijobenny.tradeflow.server.forbidden
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
import io.ktor.server.routing.route
import org.jetbrains.exposed.sql.*

/** Order list rows with customer names and item counts. Must run inside a transaction. */
fun orderSummaries(
    customerId: Int? = null,
    status: OrderStatus? = null,
    query: String = "",
    limit: Int = 200,
): List<OrderSummaryDto> {
    val rows = Orders.join(Customers, JoinType.INNER, Orders.customerId, Customers.id)
        .select(Orders.id, Orders.customerId, Customers.name, Orders.status, Orders.totalCents, Orders.createdAt)
        .where {
            var op: Op<Boolean> = Op.TRUE
            if (customerId != null) op = op and (Orders.customerId eq customerId)
            if (status != null) op = op and (Orders.status eq status)
            op
        }
        .orderBy(Orders.createdAt to SortOrder.DESC)
        .map {
            OrderSummaryDto(
                id = it[Orders.id],
                number = orderNumber(it[Orders.id]),
                customerId = it[Orders.customerId],
                customerName = it[Customers.name],
                status = it[Orders.status],
                totalCents = it[Orders.totalCents],
                itemCount = 0,
                createdAt = it[Orders.createdAt],
            )
        }
        .filter { query.isEmpty() || it.number.lowercase().contains(query) || it.customerName.lowercase().contains(query) }
        .take(limit)
    if (rows.isEmpty()) return rows
    val ids = rows.map { it.id }
    val counts = OrderLines.select(OrderLines.orderId, OrderLines.quantity)
        .where { OrderLines.orderId inList ids }
        .groupBy({ it[OrderLines.orderId] }, { it[OrderLines.quantity] })
        .mapValues { (_, q) -> q.sum() }
    return rows.map { it.copy(itemCount = counts[it.id] ?: 0) }
}

/** Full order with lines, history and invoice. Must run inside a transaction. */
fun loadOrder(id: Int): OrderDto {
    val order = Orders.join(Users, JoinType.INNER, Orders.createdById, Users.id)
        .selectAll()
        .where { Orders.id eq id }
        .singleOrNull() ?: notFound("Order")
    val lines = OrderLines.join(Products, JoinType.INNER, OrderLines.productId, Products.id)
        .selectAll()
        .where { OrderLines.orderId eq id }
        .orderBy(OrderLines.id to SortOrder.ASC)
        .map {
            OrderLineDto(
                productId = it[Products.id],
                name = it[Products.name],
                sku = it[Products.sku],
                emoji = it[Products.emoji],
                quantity = it[OrderLines.quantity],
                unitPriceCents = it[OrderLines.unitPriceCents],
                lineTotalCents = it[OrderLines.lineTotalCents],
            )
        }
    val events = OrderEvents.join(Users, JoinType.INNER, OrderEvents.userId, Users.id)
        .selectAll()
        .where { OrderEvents.orderId eq id }
        .orderBy(OrderEvents.createdAt to SortOrder.ASC)
        .map { OrderEventDto(it[OrderEvents.status], it[OrderEvents.note], it[Users.name], it[OrderEvents.createdAt]) }
    val invoice = loadInvoices(orderId = id).firstOrNull()
    return OrderDto(
        id = id,
        number = orderNumber(id),
        customer = loadCustomer(order[Orders.customerId]),
        status = order[Orders.status],
        lines = lines,
        subtotalCents = order[Orders.subtotalCents],
        discountCents = order[Orders.discountCents],
        taxCents = order[Orders.taxCents],
        totalCents = order[Orders.totalCents],
        notes = order[Orders.notes],
        createdBy = order[Users.name],
        createdAt = order[Orders.createdAt],
        events = events,
        invoice = invoice,
    )
}

/**
 * Creates an order in one transaction: locks the product rows, checks stock and the customer's
 * credit limit, prices it with the shared [Pricing] rules, reserves stock and logs everything.
 */
fun createOrder(request: CreateOrderRequest, user: UserDto, at: Long = now(), checkCredit: Boolean = true): Pair<Int, LiveEventDto> {
    if (request.lines.isEmpty()) badRequest("Add at least one product")
    if (request.lines.any { it.quantity <= 0 }) badRequest("Quantities must be above zero")
    val merged = request.lines.groupBy { it.productId }.mapValues { (_, l) -> l.sumOf { it.quantity } }

    val customer = loadCustomer(request.customerId)
    val products = Products.selectAll()
        .where { Products.id inList merged.keys.toList() }
        .forUpdate()
        .associateBy { it[Products.id] }
    merged.forEach { (productId, quantity) ->
        val product = products[productId] ?: notFound("Product $productId")
        if (!product[Products.active]) badRequest("${product[Products.name]} is no longer sold")
        if (product[Products.stock] < quantity) {
            conflict("Only ${product[Products.stock]} × ${product[Products.name]} in stock")
        }
    }

    val quote = Pricing.quote(merged.map { (pid, qty) -> Pricing.Line(products.getValue(pid)[Products.priceCents], qty) }, customer.tier)
    if (checkCredit && customer.outstandingCents + quote.totalCents > customer.creditLimitCents) {
        conflict(
            "Credit limit exceeded: ${customer.name} has ${Money.format(customer.availableCreditCents)} available " +
                "and this order is ${Money.format(quote.totalCents)}",
        )
    }

    val orderId = Orders.insert {
        it[customerId] = customer.id
        it[status] = OrderStatus.PENDING
        it[subtotalCents] = quote.subtotalCents
        it[discountCents] = quote.discountCents
        it[taxCents] = quote.taxCents
        it[totalCents] = quote.totalCents
        it[notes] = request.notes.trim()
        it[createdById] = user.id
        it[createdAt] = at
        it[updatedAt] = at
    } get Orders.id

    merged.forEach { (productId, quantity) ->
        val product = products.getValue(productId)
        val price = product[Products.priceCents]
        OrderLines.insert {
            it[OrderLines.orderId] = orderId
            it[OrderLines.productId] = productId
            it[OrderLines.quantity] = quantity
            it[OrderLines.unitPriceCents] = price
            it[OrderLines.lineTotalCents] = price * quantity
        }
        Products.update({ Products.id eq productId }) { it[stock] = product[Products.stock] - quantity }
        addMovement(productId, -quantity, "Reserved for ${orderNumber(orderId)}", user, at)
    }
    addOrderEvent(orderId, OrderStatus.PENDING, "Order placed", user, at)
    val event = recordActivity(
        LiveEventType.ORDER_CREATED,
        "${user.name} placed ${orderNumber(orderId)} for ${customer.name} · ${Money.format(quote.totalCents)}",
        user, "order", orderId, at,
    )
    return orderId to event
}

fun addOrderEvent(orderId: Int, status: OrderStatus, note: String, user: UserDto, at: Long = now()) {
    OrderEvents.insert {
        it[OrderEvents.orderId] = orderId
        it[OrderEvents.status] = status
        it[OrderEvents.note] = note
        it[OrderEvents.userId] = user.id
        it[OrderEvents.createdAt] = at
    }
}

/**
 * Moves an order to [target]. Confirming issues an invoice; cancelling returns the stock
 * and voids the invoice.
 */
fun changeOrderStatus(orderId: Int, target: OrderStatus, note: String, user: UserDto, at: Long = now()): LiveEventDto {
    val row = Orders.selectAll().where { Orders.id eq orderId }.forUpdate().singleOrNull() ?: notFound("Order")
    val current = row[Orders.status]
    if (!current.canMoveTo(target)) conflict("${orderNumber(orderId)} is ${current.label} and can't become ${target.label}")
    if (!Permissions.canMoveOrder(user.role, target)) forbidden("${user.role.label} users can't mark orders as ${target.label}")

    Orders.update({ Orders.id eq orderId }) {
        it[status] = target
        it[updatedAt] = at
    }
    when (target) {
        OrderStatus.CONFIRMED -> {
            val terms = Customers.select(Customers.paymentTermsDays)
                .where { Customers.id eq row[Orders.customerId] }
                .single()[Customers.paymentTermsDays]
            Invoices.insert {
                it[Invoices.orderId] = orderId
                it[customerId] = row[Orders.customerId]
                it[amountCents] = row[Orders.totalCents]
                it[issuedAt] = at
                it[dueAt] = at + terms * DAY_MS
            }
        }
        OrderStatus.CANCELLED -> {
            OrderLines.selectAll().where { OrderLines.orderId eq orderId }.forEach { line ->
                val productId = line[OrderLines.productId]
                val stockNow = Products.select(Products.stock).where { Products.id eq productId }.single()[Products.stock]
                Products.update({ Products.id eq productId }) { it[stock] = stockNow + line[OrderLines.quantity] }
                addMovement(productId, line[OrderLines.quantity], "Returned from cancelled ${orderNumber(orderId)}", user, at)
            }
            Invoices.update({ Invoices.orderId eq orderId }) { it[voided] = true }
        }
        else -> Unit
    }
    val defaultNote = when (target) {
        OrderStatus.CONFIRMED -> "Order confirmed, invoice issued"
        OrderStatus.PACKED -> "Picked and packed in the warehouse"
        OrderStatus.SHIPPED -> "Handed to the carrier"
        OrderStatus.DELIVERED -> "Delivered to the customer"
        OrderStatus.CANCELLED -> "Order cancelled, stock returned"
        OrderStatus.PENDING -> "Order placed"
    }
    addOrderEvent(orderId, target, note.trim().ifEmpty { defaultNote }, user, at)
    return recordActivity(
        LiveEventType.ORDER_STATUS,
        "${user.name} marked ${orderNumber(orderId)} as ${target.label}",
        user, "order", orderId, at,
    )
}

fun Route.orderRoutes() {
    route("/orders") {
        // GET /api/orders?status=PENDING&q=cafe&customerId=3
        get {
            val status = call.request.queryParameters["status"]?.let { s -> OrderStatus.entries.firstOrNull { it.name == s } }
            val q = call.request.queryParameters["q"]?.trim()?.lowercase().orEmpty()
            val customerId = call.request.queryParameters["customerId"]?.toIntOrNull()
            val result = dbQuery {
                val counts = Orders.select(Orders.status).map { it[Orders.status] }.groupingBy { it }.eachCount()
                OrderListDto(orderSummaries(customerId, status, q), counts)
            }
            call.respond(result)
        }

        get("/{id}") {
            val id = call.intParam()
            call.respond(dbQuery { loadOrder(id) })
        }

        post {
            val user = call.requirePermission(Permission.CREATE_ORDER)
            val request = call.receive<CreateOrderRequest>()
            val (order, event) = dbQuery {
                val (id, event) = createOrder(request, user)
                loadOrder(id) to event
            }
            LiveBus.publish(event)
            call.respond(HttpStatusCode.Created, order)
        }

        // POST /api/orders/{id}/status  {"status": "PACKED", "note": "…"}
        post("/{id}/status") {
            val user = call.currentUser()
            val id = call.intParam()
            val request = call.receive<StatusUpdateRequest>()
            val (order, event) = dbQuery {
                val event = changeOrderStatus(id, request.status, request.note, user)
                loadOrder(id) to event
            }
            LiveBus.publish(event)
            call.respond(order)
        }
    }
}
