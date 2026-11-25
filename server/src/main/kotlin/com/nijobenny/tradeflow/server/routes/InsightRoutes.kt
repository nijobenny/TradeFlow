package com.nijobenny.tradeflow.server.routes

import com.nijobenny.tradeflow.core.ActivityDto
import com.nijobenny.tradeflow.core.AuthResponse
import com.nijobenny.tradeflow.core.CompanyDto
import com.nijobenny.tradeflow.core.DashboardDto
import com.nijobenny.tradeflow.core.InvoiceStatus
import com.nijobenny.tradeflow.core.LoginRequest
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.PointDto
import com.nijobenny.tradeflow.core.Pricing
import com.nijobenny.tradeflow.core.RankDto
import com.nijobenny.tradeflow.core.SearchResultDto
import com.nijobenny.tradeflow.core.StatusCountDto
import com.nijobenny.tradeflow.core.TeamMemberDto
import com.nijobenny.tradeflow.server.ApiError
import com.nijobenny.tradeflow.server.DAY_MS
import com.nijobenny.tradeflow.server.Jwt
import com.nijobenny.tradeflow.server.Passwords
import com.nijobenny.tradeflow.server.currentUser
import com.nijobenny.tradeflow.server.db.ActivityLog
import com.nijobenny.tradeflow.server.db.Customers
import com.nijobenny.tradeflow.server.db.OrderLines
import com.nijobenny.tradeflow.server.db.Orders
import com.nijobenny.tradeflow.server.db.Products
import com.nijobenny.tradeflow.server.db.Users
import com.nijobenny.tradeflow.server.db.dbQuery
import com.nijobenny.tradeflow.server.now
import com.nijobenny.tradeflow.server.toUserDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.jetbrains.exposed.sql.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val COMPANY = CompanyDto(
    name = "Harbour & Co. Wholesale",
    city = "Rotterdam",
    currency = "€",
    taxRateBp = Pricing.TAX_RATE_BP,
)

val ZONE: ZoneId = ZoneId.of("Europe/Amsterdam")

private fun startOfDay(date: LocalDate): Long = date.atStartOfDay(ZONE).toInstant().toEpochMilli()

/** POST /api/auth/login — public. */
fun Route.authRoutes() {
    post("/auth/login") {
        val request = call.receive<LoginRequest>()
        val user = dbQuery {
            Users.selectAll().where { Users.email eq request.email.trim().lowercase() }.singleOrNull()
                ?.takeIf { Passwords.matches(request.password, it[Users.passwordHash]) }
                ?.toUserDto()
        } ?: throw ApiError(HttpStatusCode.Unauthorized, "Wrong email or password")
        call.respond(AuthResponse(Jwt.issue(user.id, user.role), user, COMPANY))
    }
}

fun Route.insightRoutes() {
    get("/me") { call.respond(call.currentUser()) }

    get("/company") { call.respond(COMPANY) }

    get("/dashboard") {
        call.respond(dbQuery { dashboard() })
    }

    get("/team") {
        val members = dbQuery {
            val monthStart = startOfDay(LocalDate.now(ZONE).withDayOfMonth(1))
            val weekAgo = now() - 7 * DAY_MS
            val orders = Orders.select(Orders.createdById, Orders.totalCents, Orders.status)
                .where { Orders.createdAt greaterEq monthStart }
                .filter { it[Orders.status] != OrderStatus.CANCELLED }
                .groupBy({ it[Orders.createdById] }, { it[Orders.totalCents] })
            val actions = ActivityLog.select(ActivityLog.userId)
                .where { ActivityLog.createdAt greaterEq weekAgo }
                .map { it[ActivityLog.userId] }
                .groupingBy { it }
                .eachCount()
            Users.selectAll().orderBy(Users.name to SortOrder.ASC).map {
                val user = it.toUserDto()
                val mine = orders[user.id].orEmpty()
                TeamMemberDto(user, mine.size, mine.sum(), actions[user.id] ?: 0)
            }.sortedByDescending { it.revenueThisMonthCents }
        }
        call.respond(members)
    }

    // GET /api/activity?limit=50
    get("/activity") {
        val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 60).coerceIn(1, 200)
        call.respond(dbQuery { activity(limit) })
    }

    // GET /api/search?q=latte
    get("/search") {
        val q = call.request.queryParameters["q"]?.trim()?.lowercase().orEmpty()
        if (q.length < 2) {
            call.respond(SearchResultDto(emptyList(), emptyList(), emptyList(), emptyList()))
            return@get
        }
        val result = dbQuery {
            val products = Products.selectAll()
                .where { (Products.name.lowerCase() like "%$q%") or (Products.sku.lowerCase() like "%$q%") or (Products.category.lowerCase() like "%$q%") }
                .limit(8)
                .map { it.toProductDto() }
            val stats = customerStats()
            val customers = Customers.selectAll()
                .where { (Customers.name.lowerCase() like "%$q%") or (Customers.contactName.lowerCase() like "%$q%") or (Customers.city.lowerCase() like "%$q%") }
                .limit(8)
                .map { it.toCustomerDto(stats[it[Customers.id]]) }
            val orders = orderSummaries(query = q, limit = 8)
            val invoices = loadInvoices().filter { it.number.lowercase().contains(q) || it.customerName.lowercase().contains(q) }.take(8)
            SearchResultDto(products, customers, orders, invoices)
        }
        call.respond(result)
    }
}

fun activity(limit: Int): List<ActivityDto> =
    ActivityLog.join(Users, JoinType.INNER, ActivityLog.userId, Users.id)
        .selectAll()
        .orderBy(ActivityLog.createdAt to SortOrder.DESC)
        .limit(limit)
        .map {
            ActivityDto(
                id = it[ActivityLog.id],
                type = it[ActivityLog.type],
                message = it[ActivityLog.message],
                userName = it[Users.name],
                entity = it[ActivityLog.entity],
                entityId = it[ActivityLog.entityId],
                createdAt = it[ActivityLog.createdAt],
            )
        }

/** All dashboard numbers in one round trip. Must run inside a transaction. */
fun dashboard(): DashboardDto {
    val today = LocalDate.now(ZONE)
    val monthStart = startOfDay(today.withDayOfMonth(1))
    val prevMonthStart = startOfDay(today.withDayOfMonth(1).minusMonths(1))
    // Compare against the same number of days last month, so early in the month the trend is fair.
    val prevMonthSameDay = startOfDay(today.minusMonths(1).plusDays(1))
    val chartStart = startOfDay(today.minusDays(29))
    val earliest = minOf(prevMonthStart, chartStart)

    val orders = Orders.select(Orders.id, Orders.status, Orders.totalCents, Orders.createdAt)
        .where { Orders.createdAt greaterEq earliest }
        .map { Triple(it[Orders.status], it[Orders.totalCents], it[Orders.createdAt]) }
    val billable = orders.filter { it.first != OrderStatus.CANCELLED }

    val monthOrders = billable.filter { it.third >= monthStart }
    val revenueMonth = monthOrders.sumOf { it.second }
    val revenuePrev = billable.filter { it.third in prevMonthStart until prevMonthSameDay }.sumOf { it.second }
    val ordersToday = orders.count { it.third >= startOfDay(today) }

    val label = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    val byDay = (0 until 30).map { i ->
        val date = today.minusDays((29 - i).toLong())
        val start = startOfDay(date)
        val end = startOfDay(date.plusDays(1))
        PointDto(date.format(label), billable.filter { it.third in start until end }.sumOf { it.second })
    }

    val openStatuses = listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PACKED, OrderStatus.SHIPPED)
    val statusCounts = Orders.select(Orders.status)
        .where { Orders.status inList openStatuses }
        .map { it[Orders.status] }
        .groupingBy { it }
        .eachCount()
    val breakdown = openStatuses.map { StatusCountDto(it, statusCounts[it] ?: 0) }

    val since = now() - 30 * DAY_MS
    val revenue = OrderLines.lineTotalCents.sum()
    val units = OrderLines.quantity.sum()
    val topProducts = OrderLines.join(Orders, JoinType.INNER, OrderLines.orderId, Orders.id)
        .join(Products, JoinType.INNER, OrderLines.productId, Products.id)
        .select(Products.id, Products.name, Products.emoji, revenue, units)
        .where { (Orders.createdAt greaterEq since) and (Orders.status neq OrderStatus.CANCELLED) }
        .groupBy(Products.id, Products.name, Products.emoji)
        .orderBy(revenue to SortOrder.DESC)
        .limit(5)
        .map { RankDto(it[Products.id], it[Products.name], it[Products.emoji], it[revenue] ?: 0L, "${it[units] ?: 0} units") }

    val spend = Orders.totalCents.sum()
    val orderCount = Orders.id.count()
    val topCustomers = Orders.join(Customers, JoinType.INNER, Orders.customerId, Customers.id)
        .select(Customers.id, Customers.name, Customers.tier, spend, orderCount)
        .where { (Orders.createdAt greaterEq since) and (Orders.status neq OrderStatus.CANCELLED) }
        .groupBy(Customers.id, Customers.name, Customers.tier)
        .orderBy(spend to SortOrder.DESC)
        .limit(5)
        .map {
            val tier = it[Customers.tier]
            RankDto(it[Customers.id], it[Customers.name], tierEmoji(tier.name), it[spend] ?: 0L, "${it[orderCount]} orders · ${tier.label}")
        }

    val lowStock = Products.selectAll()
        .where { Products.active eq true }
        .map { it.toProductDto() }
        .filter { it.isLowStock }
        .sortedBy { it.stock.toDouble() / (it.reorderLevel.coerceAtLeast(1)) }

    val invoices = loadInvoices()
    return DashboardDto(
        revenueMonthCents = revenueMonth,
        revenuePrevMonthCents = revenuePrev,
        ordersToday = ordersToday,
        openOrders = breakdown.sumOf { it.count },
        outstandingCents = invoices.filter { it.status != InvoiceStatus.VOID }.sumOf { it.balanceCents },
        overdueCents = invoices.filter { it.status == InvoiceStatus.OVERDUE }.sumOf { it.balanceCents },
        lowStockCount = lowStock.size,
        averageOrderCents = if (monthOrders.isEmpty()) 0 else revenueMonth / monthOrders.size,
        revenueByDay = byDay,
        statusBreakdown = breakdown,
        topProducts = topProducts,
        topCustomers = topCustomers,
        lowStock = lowStock.take(6),
        recentActivity = activity(8),
    )
}

fun tierEmoji(tier: String): String = when (tier) {
    "PLATINUM" -> "💎"
    "GOLD" -> "🥇"
    "SILVER" -> "🥈"
    else -> "🥉"
}
