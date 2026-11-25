package com.nijobenny.tradeflow.core

/** Prices an order. The app uses it for a live preview and the server uses the same code to charge, so they always agree. */
object Pricing {
    /** Dutch standard VAT, 21%. */
    const val TAX_RATE_BP = 2100

    /** Orders at or above this subtotal get an extra bulk discount. */
    const val BULK_THRESHOLD_CENTS = 200_000L
    const val BULK_DISCOUNT_BP = 200

    data class Line(val unitPriceCents: Long, val quantity: Int) {
        val totalCents: Long get() = unitPriceCents * quantity
    }

    data class Quote(
        val subtotalCents: Long,
        val tierDiscountCents: Long,
        val bulkDiscountCents: Long,
        val taxCents: Long,
        val totalCents: Long,
    ) {
        val discountCents: Long get() = tierDiscountCents + bulkDiscountCents
        val qualifiesForBulk: Boolean get() = bulkDiscountCents > 0
    }

    fun quote(lines: List<Line>, tier: CustomerTier): Quote {
        val subtotal = lines.sumOf { it.totalCents }
        val tierDiscount = percent(subtotal, tier.discountBp)
        val bulkDiscount = if (subtotal >= BULK_THRESHOLD_CENTS) percent(subtotal, BULK_DISCOUNT_BP) else 0L
        val taxable = subtotal - tierDiscount - bulkDiscount
        val tax = percent(taxable, TAX_RATE_BP)
        return Quote(subtotal, tierDiscount, bulkDiscount, tax, taxable + tax)
    }

    /** [amount] × [bp] / 10 000, rounded half up. */
    fun percent(amount: Long, bp: Int): Long = (amount * bp + 5_000) / 10_000

    /** How much more a customer must add to unlock the bulk discount (0 when already unlocked). */
    fun toBulk(subtotalCents: Long): Long = (BULK_THRESHOLD_CENTS - subtotalCents).coerceAtLeast(0)
}

/** Role-based access control. The server enforces it; the app uses it to hide actions a user can't take. */
enum class Permission {
    CREATE_ORDER,
    ADVANCE_ORDER,
    CANCEL_ORDER,
    MANAGE_PRODUCTS,
    ADJUST_STOCK,
    MANAGE_CUSTOMERS,
    RECORD_PAYMENT,
}

object Permissions {
    fun allowed(role: Role, permission: Permission): Boolean = when (role) {
        Role.ADMIN -> true
        Role.SALES -> permission in setOf(Permission.CREATE_ORDER, Permission.ADVANCE_ORDER, Permission.CANCEL_ORDER, Permission.MANAGE_CUSTOMERS)
        Role.WAREHOUSE -> permission in setOf(Permission.ADVANCE_ORDER, Permission.ADJUST_STOCK, Permission.MANAGE_PRODUCTS)
        Role.FINANCE -> permission in setOf(Permission.RECORD_PAYMENT, Permission.CANCEL_ORDER)
    }

    /** Which roles may move an order into [target]. Warehouse packs and ships; sales confirms. */
    fun canMoveOrder(role: Role, target: OrderStatus): Boolean = when (target) {
        OrderStatus.CANCELLED -> allowed(role, Permission.CANCEL_ORDER)
        OrderStatus.CONFIRMED -> role == Role.ADMIN || role == Role.SALES
        OrderStatus.PACKED, OrderStatus.SHIPPED, OrderStatus.DELIVERED -> role == Role.ADMIN || role == Role.WAREHOUSE || role == Role.SALES
        OrderStatus.PENDING -> false
    }
}

object Money {
    /** "€1,234.50" — formatted by hand so it works the same on every platform. */
    fun format(cents: Long, symbol: String = "€", showCents: Boolean = true): String {
        val negative = cents < 0
        val abs = if (negative) -cents else cents
        val whole = abs / 100
        val fraction = (abs % 100).toInt()
        val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
        val body = if (showCents) "$grouped.${fraction.toString().padStart(2, '0')}" else grouped
        return (if (negative) "-" else "") + symbol + body
    }

    /** "€12.4k", "€1.2M" for tight spaces like chart axes. */
    fun compact(cents: Long, symbol: String = "€"): String {
        val euros = cents / 100.0
        return when {
            euros >= 1_000_000 -> symbol + oneDecimal(euros / 1_000_000) + "M"
            euros >= 1_000 -> symbol + oneDecimal(euros / 1_000) + "k"
            else -> symbol + euros.toLong()
        }
    }

    private fun oneDecimal(value: Double): String {
        val tenths = kotlin.math.round(value * 10).toLong()
        return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    }

    /** Parses "12.50", "12,50" or "12" into cents; null when it isn't a number. */
    fun parse(text: String): Long? {
        val clean = text.trim().replace("€", "").replace(" ", "").replace(',', '.')
        if (clean.isEmpty()) return null
        val parts = clean.split('.')
        if (parts.size > 2) return null
        val whole = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        val fractionText = parts.getOrNull(1) ?: ""
        if (fractionText.length > 2 || fractionText.any { !it.isDigit() }) return null
        val fraction = fractionText.padEnd(2, '0').ifEmpty { "00" }.toLong()
        return whole * 100 + fraction
    }
}

object Validation {
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isEmail(text: String): Boolean = emailPattern.matches(text.trim())

    fun product(input: ProductInput): String? = when {
        input.sku.isBlank() -> "SKU is required"
        input.name.isBlank() -> "Name is required"
        input.priceCents <= 0 -> "Price must be above zero"
        input.costCents < 0 -> "Cost can't be negative"
        input.reorderLevel < 0 -> "Reorder level can't be negative"
        input.initialStock < 0 -> "Stock can't be negative"
        else -> null
    }

    fun customer(input: CustomerInput): String? = when {
        input.name.isBlank() -> "Business name is required"
        input.contactName.isBlank() -> "Contact name is required"
        !isEmail(input.email) -> "Enter a valid email"
        input.creditLimitCents < 0 -> "Credit limit can't be negative"
        input.paymentTermsDays !in 0..120 -> "Payment terms must be 0–120 days"
        else -> null
    }
}
