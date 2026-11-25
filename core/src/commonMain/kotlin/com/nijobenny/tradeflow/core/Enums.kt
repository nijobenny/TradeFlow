package com.nijobenny.tradeflow.core

import kotlinx.serialization.Serializable

/** Staff roles. What each role may do is defined once in [Permissions] and enforced by the server. */
@Serializable
enum class Role(val label: String) {
    ADMIN("Admin"),
    SALES("Sales"),
    WAREHOUSE("Warehouse"),
    FINANCE("Finance"),
}

/** Customer pricing tiers. Discounts are in basis points (100 bp = 1%). */
@Serializable
enum class CustomerTier(val label: String, val discountBp: Int) {
    BRONZE("Bronze", 0),
    SILVER("Silver", 500),
    GOLD("Gold", 1000),
    PLATINUM("Platinum", 1500),
}

/** Order lifecycle: Pending → Confirmed → Packed → Shipped → Delivered, or Cancelled before shipping. */
@Serializable
enum class OrderStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PACKED("Packed"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    /** The next step in the happy path, or null when the order is finished. */
    val next: OrderStatus?
        get() = when (this) {
            PENDING -> CONFIRMED
            CONFIRMED -> PACKED
            PACKED -> SHIPPED
            SHIPPED -> DELIVERED
            DELIVERED, CANCELLED -> null
        }

    val canCancel: Boolean get() = this == PENDING || this == CONFIRMED || this == PACKED

    val isOpen: Boolean get() = this != DELIVERED && this != CANCELLED

    fun canMoveTo(target: OrderStatus): Boolean =
        target == next || (target == CANCELLED && canCancel)

    companion object {
        /** The statuses shown on the progress stepper, in order. */
        val flow = listOf(PENDING, CONFIRMED, PACKED, SHIPPED, DELIVERED)
    }
}

@Serializable
enum class InvoiceStatus(val label: String) {
    UNPAID("Unpaid"),
    PARTIAL("Partially paid"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    VOID("Void"),
}

@Serializable
enum class PaymentMethod(val label: String) {
    BANK_TRANSFER("Bank transfer"),
    IDEAL("iDEAL"),
    CARD("Card"),
    CASH("Cash"),
}

/** Live events pushed over the WebSocket. */
@Serializable
enum class LiveEventType {
    ORDER_CREATED,
    ORDER_STATUS,
    PAYMENT,
    STOCK,
    PRODUCT,
    CUSTOMER,
}
