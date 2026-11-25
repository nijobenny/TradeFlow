package com.nijobenny.tradeflow.core

import kotlinx.serialization.Serializable

// All money values are integer cents (Long) and all times are epoch milliseconds, so nothing
// is lost to floating point or time-zone conversion between the server and the app.

@Serializable
data class ErrorDto(val error: String)

@Serializable
data class HealthDto(
    val status: String,
    val version: String,
    val database: String,
    val serverTime: Long,
)

@Serializable
data class CompanyDto(
    val name: String,
    val city: String,
    val currency: String,
    val taxRateBp: Int,
)

// ---------- Auth ----------

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: Role,
    val title: String,
)

@Serializable
data class AuthResponse(val token: String, val user: UserDto, val company: CompanyDto)

// ---------- Products & inventory ----------

@Serializable
data class ProductDto(
    val id: Int,
    val sku: String,
    val name: String,
    val category: String,
    val unit: String,
    val emoji: String,
    val priceCents: Long,
    val costCents: Long,
    val stock: Int,
    val reorderLevel: Int,
    val supplier: String,
    val active: Boolean,
    val sold30d: Int = 0,
) {
    val isLowStock: Boolean get() = stock <= reorderLevel
    val marginBp: Int get() = if (priceCents == 0L) 0 else ((priceCents - costCents) * 10_000 / priceCents).toInt()
}

@Serializable
data class ProductInput(
    val sku: String,
    val name: String,
    val category: String,
    val unit: String,
    val emoji: String,
    val priceCents: Long,
    val costCents: Long,
    val reorderLevel: Int,
    val supplier: String,
    val initialStock: Int = 0,
)

@Serializable
data class StockAdjustRequest(val delta: Int, val reason: String)

@Serializable
data class StockMovementDto(
    val id: Int,
    val delta: Int,
    val reason: String,
    val userName: String,
    val createdAt: Long,
)

@Serializable
data class ProductDetailDto(
    val product: ProductDto,
    val revenue30dCents: Long,
    val salesByWeek: List<PointDto>,
    val movements: List<StockMovementDto>,
)

// ---------- Customers ----------

@Serializable
data class CustomerDto(
    val id: Int,
    val name: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val tier: CustomerTier,
    val creditLimitCents: Long,
    val paymentTermsDays: Int,
    val outstandingCents: Long = 0,
    val totalSpentCents: Long = 0,
    val orderCount: Int = 0,
    val lastOrderAt: Long? = null,
) {
    val availableCreditCents: Long get() = (creditLimitCents - outstandingCents).coerceAtLeast(0)
}

@Serializable
data class CustomerInput(
    val name: String,
    val contactName: String,
    val email: String,
    val phone: String,
    val city: String,
    val country: String,
    val tier: CustomerTier,
    val creditLimitCents: Long,
    val paymentTermsDays: Int,
)

@Serializable
data class CustomerDetailDto(
    val customer: CustomerDto,
    val orders: List<OrderSummaryDto>,
    val invoices: List<InvoiceDto>,
    val spendByMonth: List<PointDto>,
)

// ---------- Orders ----------

@Serializable
data class OrderSummaryDto(
    val id: Int,
    val number: String,
    val customerId: Int,
    val customerName: String,
    val status: OrderStatus,
    val totalCents: Long,
    val itemCount: Int,
    val createdAt: Long,
)

@Serializable
data class OrderLineDto(
    val productId: Int,
    val name: String,
    val sku: String,
    val emoji: String,
    val quantity: Int,
    val unitPriceCents: Long,
    val lineTotalCents: Long,
)

@Serializable
data class OrderEventDto(
    val status: OrderStatus,
    val note: String,
    val userName: String,
    val createdAt: Long,
)

@Serializable
data class OrderDto(
    val id: Int,
    val number: String,
    val customer: CustomerDto,
    val status: OrderStatus,
    val lines: List<OrderLineDto>,
    val subtotalCents: Long,
    val discountCents: Long,
    val taxCents: Long,
    val totalCents: Long,
    val notes: String,
    val createdBy: String,
    val createdAt: Long,
    val events: List<OrderEventDto>,
    val invoice: InvoiceDto?,
)

@Serializable
data class OrderLineInput(val productId: Int, val quantity: Int)

@Serializable
data class CreateOrderRequest(
    val customerId: Int,
    val lines: List<OrderLineInput>,
    val notes: String = "",
)

@Serializable
data class StatusUpdateRequest(val status: OrderStatus, val note: String = "")

@Serializable
data class OrderListDto(
    val items: List<OrderSummaryDto>,
    val counts: Map<OrderStatus, Int>,
)

// ---------- Invoices ----------

@Serializable
data class PaymentDto(
    val id: Int,
    val amountCents: Long,
    val method: PaymentMethod,
    val userName: String,
    val createdAt: Long,
)

@Serializable
data class InvoiceDto(
    val id: Int,
    val number: String,
    val orderId: Int,
    val orderNumber: String,
    val customerId: Int,
    val customerName: String,
    val amountCents: Long,
    val paidCents: Long,
    val issuedAt: Long,
    val dueAt: Long,
    val status: InvoiceStatus,
    val payments: List<PaymentDto> = emptyList(),
) {
    val balanceCents: Long get() = (amountCents - paidCents).coerceAtLeast(0)
}

@Serializable
data class InvoiceListDto(
    val items: List<InvoiceDto>,
    val outstandingCents: Long,
    val overdueCents: Long,
    val paidThisMonthCents: Long,
)

@Serializable
data class PaymentRequest(val amountCents: Long, val method: PaymentMethod)

// ---------- Dashboard, team, activity, search ----------

/** A labelled value for charts, e.g. revenue on one day. */
@Serializable
data class PointDto(val label: String, val value: Long)

@Serializable
data class StatusCountDto(val status: OrderStatus, val count: Int)

@Serializable
data class RankDto(val id: Int, val name: String, val emoji: String, val valueCents: Long, val detail: String)

@Serializable
data class ActivityDto(
    val id: Int,
    val type: LiveEventType,
    val message: String,
    val userName: String,
    val entity: String,
    val entityId: Int,
    val createdAt: Long,
)

@Serializable
data class DashboardDto(
    val revenueMonthCents: Long,
    val revenuePrevMonthCents: Long,
    val ordersToday: Int,
    val openOrders: Int,
    val outstandingCents: Long,
    val overdueCents: Long,
    val lowStockCount: Int,
    val averageOrderCents: Long,
    val revenueByDay: List<PointDto>,
    val statusBreakdown: List<StatusCountDto>,
    val topProducts: List<RankDto>,
    val topCustomers: List<RankDto>,
    val lowStock: List<ProductDto>,
    val recentActivity: List<ActivityDto>,
)

@Serializable
data class TeamMemberDto(
    val user: UserDto,
    val ordersThisMonth: Int,
    val revenueThisMonthCents: Long,
    val actionsThisWeek: Int,
)

@Serializable
data class SearchResultDto(
    val products: List<ProductDto>,
    val customers: List<CustomerDto>,
    val orders: List<OrderSummaryDto>,
    val invoices: List<InvoiceDto>,
)

@Serializable
data class LiveEventDto(
    val type: LiveEventType,
    val message: String,
    val entity: String,
    val entityId: Int,
    val userName: String,
    val createdAt: Long,
)
