package com.nijobenny.tradeflow.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PricingTest {
    @Test
    fun bronzePaysFullPricePlusVat() {
        val q = Pricing.quote(listOf(Pricing.Line(1_000, 3)), CustomerTier.BRONZE)
        assertEquals(3_000, q.subtotalCents)
        assertEquals(0, q.discountCents)
        assertEquals(630, q.taxCents)
        assertEquals(3_630, q.totalCents)
    }

    @Test
    fun goldGetsTenPercentOff() {
        val q = Pricing.quote(listOf(Pricing.Line(10_000, 1)), CustomerTier.GOLD)
        assertEquals(1_000, q.tierDiscountCents)
        assertEquals(1_890, q.taxCents)
        assertEquals(10_890, q.totalCents)
    }

    @Test
    fun bulkDiscountKicksInAtThreshold() {
        val below = Pricing.quote(listOf(Pricing.Line(Pricing.BULK_THRESHOLD_CENTS - 1, 1)), CustomerTier.BRONZE)
        val at = Pricing.quote(listOf(Pricing.Line(Pricing.BULK_THRESHOLD_CENTS, 1)), CustomerTier.BRONZE)
        assertFalse(below.qualifiesForBulk)
        assertTrue(at.qualifiesForBulk)
        assertEquals(4_000, at.bulkDiscountCents)
        assertEquals(1, Pricing.toBulk(Pricing.BULK_THRESHOLD_CENTS - 1))
    }

    @Test
    fun percentRoundsHalfUp() {
        assertEquals(1, Pricing.percent(5, 1_000)) // 0.5 → 1
        assertEquals(0, Pricing.percent(4, 1_000)) // 0.4 → 0
    }
}

class OrderStatusTest {
    @Test
    fun happyPathMovesForward() {
        assertEquals(OrderStatus.CONFIRMED, OrderStatus.PENDING.next)
        assertEquals(null, OrderStatus.DELIVERED.next)
        assertTrue(OrderStatus.PACKED.canMoveTo(OrderStatus.SHIPPED))
        assertFalse(OrderStatus.PENDING.canMoveTo(OrderStatus.SHIPPED))
    }

    @Test
    fun cannotCancelAfterShipping() {
        assertTrue(OrderStatus.PACKED.canMoveTo(OrderStatus.CANCELLED))
        assertFalse(OrderStatus.SHIPPED.canMoveTo(OrderStatus.CANCELLED))
    }
}

class PermissionsTest {
    @Test
    fun rolesHaveTheRightAccess() {
        assertTrue(Permissions.allowed(Role.ADMIN, Permission.RECORD_PAYMENT))
        assertTrue(Permissions.allowed(Role.FINANCE, Permission.RECORD_PAYMENT))
        assertFalse(Permissions.allowed(Role.SALES, Permission.RECORD_PAYMENT))
        assertTrue(Permissions.allowed(Role.WAREHOUSE, Permission.ADJUST_STOCK))
        assertFalse(Permissions.allowed(Role.WAREHOUSE, Permission.CREATE_ORDER))
        assertFalse(Permissions.canMoveOrder(Role.WAREHOUSE, OrderStatus.CONFIRMED))
        assertTrue(Permissions.canMoveOrder(Role.WAREHOUSE, OrderStatus.SHIPPED))
    }
}

class MoneyTest {
    @Test
    fun formatsWithGroupingAndCents() {
        assertEquals("€1,234.50", Money.format(123_450))
        assertEquals("€0.05", Money.format(5))
        assertEquals("-€12.00", Money.format(-1_200))
        assertEquals("€1,234", Money.format(123_450, showCents = false))
    }

    @Test
    fun compactFormat() {
        assertEquals("€12.4k", Money.compact(1_240_000))
        assertEquals("€2M", Money.compact(200_000_000))
        assertEquals("€950", Money.compact(95_000))
    }

    @Test
    fun parsesUserInput() {
        assertEquals(1_250, Money.parse("12.5"))
        assertEquals(1_250, Money.parse("12,50"))
        assertEquals(1_200, Money.parse("€12"))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("1.234"))
    }
}

class ValidationTest {
    @Test
    fun emailCheck() {
        assertTrue(Validation.isEmail("maya@tradeflow.dev"))
        assertFalse(Validation.isEmail("not an email"))
    }
}
