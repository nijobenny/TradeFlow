package com.nijobenny.tradeflow.server.db

import com.nijobenny.tradeflow.core.CustomerTier
import com.nijobenny.tradeflow.core.InvoiceStatus
import com.nijobenny.tradeflow.core.LiveEventType
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.PaymentMethod
import com.nijobenny.tradeflow.core.Role
import org.jetbrains.exposed.sql.Table

// PostgreSQL schema, defined with the Exposed DSL. Money is stored as cents (BIGINT) and times as epoch millis.

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 120)
    val email = varchar("email", 160).uniqueIndex()
    val passwordHash = varchar("password_hash", 100)
    val role = enumerationByName("role", 20, Role::class)
    val title = varchar("title", 120)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Products : Table("products") {
    val id = integer("id").autoIncrement()
    val sku = varchar("sku", 40).uniqueIndex()
    val name = varchar("name", 160)
    val category = varchar("category", 60).index()
    val unit = varchar("unit", 60)
    val emoji = varchar("emoji", 16)
    val priceCents = long("price_cents")
    val costCents = long("cost_cents")
    val stock = integer("stock")
    val reorderLevel = integer("reorder_level")
    val supplier = varchar("supplier", 120)
    val active = bool("active").default(true)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object StockMovements : Table("stock_movements") {
    val id = integer("id").autoIncrement()
    val productId = integer("product_id").references(Products.id).index()
    val delta = integer("delta")
    val reason = varchar("reason", 160)
    val userId = integer("user_id").references(Users.id)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Customers : Table("customers") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 160)
    val contactName = varchar("contact_name", 120)
    val email = varchar("email", 160)
    val phone = varchar("phone", 40)
    val city = varchar("city", 80)
    val country = varchar("country", 80)
    val tier = enumerationByName("tier", 20, CustomerTier::class)
    val creditLimitCents = long("credit_limit_cents")
    val paymentTermsDays = integer("payment_terms_days")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Orders : Table("orders") {
    val id = integer("id").autoIncrement()
    val customerId = integer("customer_id").references(Customers.id).index()
    val status = enumerationByName("status", 20, OrderStatus::class).index()
    val subtotalCents = long("subtotal_cents")
    val discountCents = long("discount_cents")
    val taxCents = long("tax_cents")
    val totalCents = long("total_cents")
    val notes = text("notes")
    val createdById = integer("created_by").references(Users.id)
    val createdAt = long("created_at").index()
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object OrderLines : Table("order_lines") {
    val id = integer("id").autoIncrement()
    val orderId = integer("order_id").references(Orders.id).index()
    val productId = integer("product_id").references(Products.id)
    val quantity = integer("quantity")
    val unitPriceCents = long("unit_price_cents")
    val lineTotalCents = long("line_total_cents")
    override val primaryKey = PrimaryKey(id)
}

object OrderEvents : Table("order_events") {
    val id = integer("id").autoIncrement()
    val orderId = integer("order_id").references(Orders.id).index()
    val status = enumerationByName("status", 20, OrderStatus::class)
    val note = varchar("note", 240)
    val userId = integer("user_id").references(Users.id)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Invoices : Table("invoices") {
    val id = integer("id").autoIncrement()
    val orderId = integer("order_id").references(Orders.id).uniqueIndex()
    val customerId = integer("customer_id").references(Customers.id).index()
    val amountCents = long("amount_cents")
    val paidCents = long("paid_cents").default(0)
    val issuedAt = long("issued_at")
    val dueAt = long("due_at")
    val voided = bool("voided").default(false)
    override val primaryKey = PrimaryKey(id)
}

object Payments : Table("payments") {
    val id = integer("id").autoIncrement()
    val invoiceId = integer("invoice_id").references(Invoices.id).index()
    val amountCents = long("amount_cents")
    val method = enumerationByName("method", 20, PaymentMethod::class)
    val userId = integer("user_id").references(Users.id)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

object ActivityLog : Table("activity_log") {
    val id = integer("id").autoIncrement()
    val type = enumerationByName("type", 30, LiveEventType::class)
    val message = varchar("message", 300)
    val userId = integer("user_id").references(Users.id)
    val entity = varchar("entity", 30)
    val entityId = integer("entity_id")
    val createdAt = long("created_at").index()
    override val primaryKey = PrimaryKey(id)
}

val allTables = arrayOf(Users, Products, StockMovements, Customers, Orders, OrderLines, OrderEvents, Invoices, Payments, ActivityLog)

/** Invoice status is derived from amounts and dates so it can never go stale. */
fun invoiceStatus(amount: Long, paid: Long, dueAt: Long, voided: Boolean, now: Long): InvoiceStatus = when {
    voided -> InvoiceStatus.VOID
    paid >= amount -> InvoiceStatus.PAID
    now > dueAt -> InvoiceStatus.OVERDUE
    paid > 0 -> InvoiceStatus.PARTIAL
    else -> InvoiceStatus.UNPAID
}

fun orderNumber(id: Int) = "SO-${10_000 + id}"
fun invoiceNumber(id: Int) = "INV-${5_000 + id}"
