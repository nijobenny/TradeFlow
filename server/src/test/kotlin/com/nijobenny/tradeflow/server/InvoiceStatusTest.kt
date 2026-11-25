package com.nijobenny.tradeflow.server

import com.nijobenny.tradeflow.core.InvoiceStatus
import com.nijobenny.tradeflow.server.db.invoiceNumber
import com.nijobenny.tradeflow.server.db.invoiceStatus
import com.nijobenny.tradeflow.server.db.orderNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InvoiceStatusTest {
    private val due = 1_000L

    @Test
    fun statusFollowsAmountsAndDates() {
        assertEquals(InvoiceStatus.UNPAID, invoiceStatus(100, 0, due, voided = false, now = 500))
        assertEquals(InvoiceStatus.PARTIAL, invoiceStatus(100, 40, due, voided = false, now = 500))
        assertEquals(InvoiceStatus.PAID, invoiceStatus(100, 100, due, voided = false, now = 5_000))
        assertEquals(InvoiceStatus.OVERDUE, invoiceStatus(100, 40, due, voided = false, now = 5_000))
        assertEquals(InvoiceStatus.VOID, invoiceStatus(100, 0, due, voided = true, now = 5_000))
    }

    @Test
    fun documentNumbers() {
        assertEquals("SO-10042", orderNumber(42))
        assertEquals("INV-5007", invoiceNumber(7))
    }
}

class JwtTest {
    @Test
    fun tokensRoundTrip() {
        val token = Jwt.issue(3, com.nijobenny.tradeflow.core.Role.SALES)
        assertNotNull(Jwt.userIdOrNull(token))
        assertEquals(3, Jwt.userIdOrNull(token))
        assertNull(Jwt.userIdOrNull("not-a-token"))
    }
}
