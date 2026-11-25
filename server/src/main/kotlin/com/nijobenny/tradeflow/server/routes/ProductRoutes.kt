package com.nijobenny.tradeflow.server.routes

import com.nijobenny.tradeflow.core.LiveEventType
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.PointDto
import com.nijobenny.tradeflow.core.ProductDetailDto
import com.nijobenny.tradeflow.core.ProductDto
import com.nijobenny.tradeflow.core.ProductInput
import com.nijobenny.tradeflow.core.StockAdjustRequest
import com.nijobenny.tradeflow.core.StockMovementDto
import com.nijobenny.tradeflow.core.UserDto
import com.nijobenny.tradeflow.core.Validation
import com.nijobenny.tradeflow.server.DAY_MS
import com.nijobenny.tradeflow.server.LiveBus
import com.nijobenny.tradeflow.server.badRequest
import com.nijobenny.tradeflow.server.conflict
import com.nijobenny.tradeflow.server.db.OrderLines
import com.nijobenny.tradeflow.server.db.Orders
import com.nijobenny.tradeflow.server.db.Products
import com.nijobenny.tradeflow.server.db.StockMovements
import com.nijobenny.tradeflow.server.db.Users
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

fun ResultRow.toProductDto(sold30d: Int = 0) = ProductDto(
    id = this[Products.id],
    sku = this[Products.sku],
    name = this[Products.name],
    category = this[Products.category],
    unit = this[Products.unit],
    emoji = this[Products.emoji],
    priceCents = this[Products.priceCents],
    costCents = this[Products.costCents],
    stock = this[Products.stock],
    reorderLevel = this[Products.reorderLevel],
    supplier = this[Products.supplier],
    active = this[Products.active],
    sold30d = sold30d,
)

/** Units sold per product in the last 30 days (cancelled orders excluded). Must run inside a transaction. */
fun soldLast30Days(): Map<Int, Int> {
    val since = now() - 30 * DAY_MS
    return OrderLines.join(Orders, JoinType.INNER, OrderLines.orderId, Orders.id)
        .select(OrderLines.productId, OrderLines.quantity)
        .where { (Orders.createdAt greaterEq since) and (Orders.status neq OrderStatus.CANCELLED) }
        .groupBy({ it[OrderLines.productId] }, { it[OrderLines.quantity] })
        .mapValues { (_, quantities) -> quantities.sum() }
}

fun Route.productRoutes() {
    route("/products") {
        // GET /api/products?q=&category=&lowStock=true
        get {
            val q = call.request.queryParameters["q"]?.trim()?.lowercase().orEmpty()
            val category = call.request.queryParameters["category"]?.takeIf { it.isNotBlank() }
            val lowOnly = call.request.queryParameters["lowStock"] == "true"
            val products = dbQuery {
                val sold = soldLast30Days()
                Products.selectAll()
                    .where {
                        var op: Op<Boolean> = Products.active eq true
                        if (category != null) op = op and (Products.category eq category)
                        if (q.isNotEmpty()) op = op and ((Products.name.lowerCase() like "%$q%") or (Products.sku.lowerCase() like "%$q%"))
                        op
                    }
                    .orderBy(Products.name to SortOrder.ASC)
                    .map { it.toProductDto(sold[it[Products.id]] ?: 0) }
            }
            call.respond(if (lowOnly) products.filter { it.isLowStock } else products)
        }

        get("/categories") {
            val categories = dbQuery {
                Products.select(Products.category).withDistinct().map { it[Products.category] }.sorted()
            }
            call.respond(categories)
        }

        get("/{id}") {
            val id = call.intParam()
            call.respond(dbQuery { productDetail(id) })
        }

        post {
            val user = call.requirePermission(Permission.MANAGE_PRODUCTS)
            val input = call.receive<ProductInput>()
            Validation.product(input)?.let { badRequest(it) }
            val (detail, event) = dbQuery {
                val exists = Products.selectAll().where { Products.sku eq input.sku.trim().uppercase() }.count() > 0
                if (exists) conflict("SKU ${input.sku.uppercase()} already exists")
                val id = Products.insert {
                    it[sku] = input.sku.trim().uppercase()
                    it[name] = input.name.trim()
                    it[category] = input.category.trim().ifEmpty { "General" }
                    it[unit] = input.unit.trim().ifEmpty { "each" }
                    it[emoji] = input.emoji.ifBlank { "📦" }
                    it[priceCents] = input.priceCents
                    it[costCents] = input.costCents
                    it[stock] = input.initialStock
                    it[reorderLevel] = input.reorderLevel
                    it[supplier] = input.supplier.trim()
                    it[createdAt] = now()
                } get Products.id
                if (input.initialStock > 0) addMovement(id, input.initialStock, "Opening stock", user)
                val event = recordActivity(LiveEventType.PRODUCT, "${user.name} added ${input.name.trim()}", user, "product", id)
                productDetail(id) to event
            }
            LiveBus.publish(event)
            call.respond(HttpStatusCode.Created, detail)
        }

        put("/{id}") {
            val user = call.requirePermission(Permission.MANAGE_PRODUCTS)
            val id = call.intParam()
            val input = call.receive<ProductInput>()
            Validation.product(input)?.let { badRequest(it) }
            val (detail, event) = dbQuery {
                val updated = Products.update({ Products.id eq id }) {
                    it[name] = input.name.trim()
                    it[category] = input.category.trim().ifEmpty { "General" }
                    it[unit] = input.unit.trim().ifEmpty { "each" }
                    it[emoji] = input.emoji.ifBlank { "📦" }
                    it[priceCents] = input.priceCents
                    it[costCents] = input.costCents
                    it[reorderLevel] = input.reorderLevel
                    it[supplier] = input.supplier.trim()
                }
                if (updated == 0) notFound("Product")
                val event = recordActivity(LiveEventType.PRODUCT, "${user.name} updated ${input.name.trim()}", user, "product", id)
                productDetail(id) to event
            }
            LiveBus.publish(event)
            call.respond(detail)
        }

        // POST /api/products/{id}/stock  {"delta": 24, "reason": "Restock"}
        post("/{id}/stock") {
            val user = call.requirePermission(Permission.ADJUST_STOCK)
            val id = call.intParam()
            val request = call.receive<StockAdjustRequest>()
            if (request.delta == 0) badRequest("Change can't be zero")
            val (detail, event) = dbQuery {
                val row = Products.selectAll().where { Products.id eq id }.forUpdate().singleOrNull() ?: notFound("Product")
                val newStock = row[Products.stock] + request.delta
                if (newStock < 0) badRequest("Only ${row[Products.stock]} in stock")
                Products.update({ Products.id eq id }) { it[stock] = newStock }
                addMovement(id, request.delta, request.reason.ifBlank { "Adjustment" }, user)
                val sign = if (request.delta > 0) "+" else ""
                val event = recordActivity(
                    LiveEventType.STOCK,
                    "${user.name} adjusted ${row[Products.name]} by $sign${request.delta} (${request.reason})",
                    user, "product", id,
                )
                productDetail(id) to event
            }
            LiveBus.publish(event)
            call.respond(detail)
        }
    }
}

fun addMovement(productId: Int, delta: Int, reason: String, user: UserDto, at: Long = now()) {
    StockMovements.insert {
        it[StockMovements.productId] = productId
        it[StockMovements.delta] = delta
        it[StockMovements.reason] = reason
        it[StockMovements.userId] = user.id
        it[StockMovements.createdAt] = at
    }
}

/** Product with 30-day revenue, weekly unit sales for the last 8 weeks, and recent stock movements. */
fun productDetail(id: Int): ProductDetailDto {
    val row = Products.selectAll().where { Products.id eq id }.singleOrNull() ?: notFound("Product")
    val since = now() - 56 * DAY_MS
    val sales = OrderLines.join(Orders, JoinType.INNER, OrderLines.orderId, Orders.id)
        .select(OrderLines.quantity, OrderLines.lineTotalCents, Orders.createdAt)
        .where { (OrderLines.productId eq id) and (Orders.createdAt greaterEq since) and (Orders.status neq OrderStatus.CANCELLED) }
        .map { Triple(it[OrderLines.quantity], it[OrderLines.lineTotalCents], it[Orders.createdAt]) }
    val monthAgo = now() - 30 * DAY_MS
    val sold30 = sales.filter { it.third >= monthAgo }.sumOf { it.first }
    val revenue30 = sales.filter { it.third >= monthAgo }.sumOf { it.second }
    val weeks = (7 downTo 0).map { weeksAgo ->
        val end = now() - weeksAgo * 7 * DAY_MS
        val start = end - 7 * DAY_MS
        val units = sales.filter { it.third in start until end }.sumOf { it.first }
        PointDto(if (weeksAgo == 0) "This wk" else "-${weeksAgo}w", units.toLong())
    }
    val movements = StockMovements.join(Users, JoinType.INNER, StockMovements.userId, Users.id)
        .selectAll()
        .where { StockMovements.productId eq id }
        .orderBy(StockMovements.createdAt to SortOrder.DESC)
        .limit(25)
        .map {
            StockMovementDto(it[StockMovements.id], it[StockMovements.delta], it[StockMovements.reason], it[Users.name], it[StockMovements.createdAt])
        }
    return ProductDetailDto(row.toProductDto(sold30), revenue30, weeks, movements)
}
