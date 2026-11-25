package com.nijobenny.tradeflow.server.db

import com.nijobenny.tradeflow.core.CreateOrderRequest
import com.nijobenny.tradeflow.core.CustomerTier
import com.nijobenny.tradeflow.core.LiveEventDto
import com.nijobenny.tradeflow.core.OrderLineInput
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.PaymentMethod
import com.nijobenny.tradeflow.core.Role
import com.nijobenny.tradeflow.core.UserDto
import com.nijobenny.tradeflow.server.DAY_MS
import com.nijobenny.tradeflow.server.Passwords
import com.nijobenny.tradeflow.server.now
import com.nijobenny.tradeflow.server.routes.ZONE
import com.nijobenny.tradeflow.server.routes.addMovement
import com.nijobenny.tradeflow.server.routes.changeOrderStatus
import com.nijobenny.tradeflow.server.routes.createOrder
import com.nijobenny.tradeflow.server.routes.recordPayment
import com.nijobenny.tradeflow.server.toUserDto
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/**
 * Creates the demo company on first run and keeps its history up to date: every start-up
 * fills in orders for the days the server was off, using the real order, invoice and payment
 * logic, so all numbers are consistent.
 */
object Seeder {
    private val log = LoggerFactory.getLogger("Seeder")
    const val DEMO_PASSWORD = "demo1234"
    private const val HISTORY_DAYS = 75L

    private data class P(val sku: String, val name: String, val category: String, val unit: String, val emoji: String, val price: Long, val cost: Long, val reorder: Int, val supplier: String)
    private data class C(val name: String, val contact: String, val city: String, val country: String, val tier: CustomerTier, val credit: Long, val terms: Int)

    private val products = listOf(
        P("CF-ESP-1K", "Espresso Blend Beans 1kg", "Coffee & Tea", "1 kg bag", "☕", 1890, 1120, 40, "Van Dijk Roasters"),
        P("CF-DEC-1K", "Decaf Swiss Water Beans 1kg", "Coffee & Tea", "1 kg bag", "☕", 2150, 1340, 15, "Van Dijk Roasters"),
        P("CF-ETH-1K", "Ethiopia Single Origin 1kg", "Coffee & Tea", "1 kg bag", "🫘", 2600, 1610, 20, "Van Dijk Roasters"),
        P("TE-ENG-100", "English Breakfast Tea", "Coffee & Tea", "box of 100", "🍵", 780, 390, 25, "Thee Huis Leiden"),
        P("TE-GRN-100", "Sencha Green Tea", "Coffee & Tea", "box of 100", "🍵", 920, 460, 20, "Thee Huis Leiden"),
        P("TE-CHA-1L", "Chai Latte Concentrate", "Coffee & Tea", "1 L bottle", "🫖", 1240, 680, 18, "Thee Huis Leiden"),
        P("DY-OAT-12", "Barista Oat Milk", "Dairy & Alternatives", "case of 12 × 1 L", "🥛", 2640, 1700, 30, "Noord Zuivel"),
        P("DY-WHL-12", "Whole Milk", "Dairy & Alternatives", "case of 12 × 1 L", "🥛", 1490, 960, 30, "Noord Zuivel"),
        P("DY-ALM-12", "Almond Milk", "Dairy & Alternatives", "case of 12 × 1 L", "🥛", 2880, 1890, 15, "Noord Zuivel"),
        P("DY-CRM-6", "Whipping Cream", "Dairy & Alternatives", "case of 6 × 1 L", "🍶", 1950, 1210, 12, "Noord Zuivel"),
        P("BK-CRS-48", "Butter Croissants (frozen)", "Bakery", "box of 48", "🥐", 3200, 1920, 20, "Bakkerij Brood & Zo"),
        P("BK-SRD-10", "Sourdough Loaves", "Bakery", "crate of 10", "🍞", 2750, 1540, 15, "Bakkerij Brood & Zo"),
        P("BK-STR-20", "Stroopwafels", "Bakery", "carton of 20 packs", "🧇", 1680, 910, 25, "Bakkerij Brood & Zo"),
        P("BK-MUF-24", "Blueberry Muffins", "Bakery", "box of 24", "🧁", 2200, 1260, 15, "Bakkerij Brood & Zo"),
        P("BV-SPK-24", "Sparkling Mineral Water", "Beverages", "tray of 24 × 330 ml", "💧", 1190, 620, 30, "Lowlands Drinks"),
        P("BV-OJ-6", "Fresh Orange Juice", "Beverages", "case of 6 × 1 L", "🍊", 1740, 1030, 18, "Lowlands Drinks"),
        P("BV-KOM-12", "Ginger Kombucha", "Beverages", "case of 12 × 330 ml", "🍹", 2400, 1440, 12, "Lowlands Drinks"),
        P("BV-COL-24", "Craft Cola", "Beverages", "tray of 24 × 330 ml", "🥤", 1920, 1080, 20, "Lowlands Drinks"),
        P("SY-VAN-1L", "Vanilla Syrup", "Syrups", "1 L bottle", "🍯", 890, 470, 20, "Sweet Pour BV"),
        P("SY-CAR-1L", "Caramel Syrup", "Syrups", "1 L bottle", "🍯", 890, 470, 20, "Sweet Pour BV"),
        P("SY-HAZ-1L", "Hazelnut Syrup", "Syrups", "1 L bottle", "🌰", 940, 490, 15, "Sweet Pour BV"),
        P("PK-CUP8-1K", "Compostable Cups 8 oz", "Packaging", "carton of 1000", "🥤", 6200, 3800, 10, "GreenPack Europe"),
        P("PK-CUP12-1K", "Compostable Cups 12 oz", "Packaging", "carton of 1000", "🥤", 8400, 5200, 10, "GreenPack Europe"),
        P("PK-LID-1K", "Sip Lids", "Packaging", "carton of 1000", "🔘", 2900, 1600, 10, "GreenPack Europe"),
        P("PK-BAG-500", "Kraft Takeaway Bags", "Packaging", "pack of 500", "🛍️", 3800, 2100, 8, "GreenPack Europe"),
        P("PK-NAP-2K", "Recycled Napkins", "Packaging", "carton of 2000", "🧻", 2100, 1100, 10, "GreenPack Europe"),
        P("PK-BOX-200", "Pastry Boxes", "Packaging", "pack of 200", "📦", 4600, 2700, 8, "GreenPack Europe"),
        P("CL-DET-5L", "Dishwasher Detergent", "Cleaning", "5 L can", "🧴", 2450, 1300, 10, "CleanPro Benelux"),
        P("CL-SAN-5L", "Surface Sanitiser", "Cleaning", "5 L can", "🧽", 1990, 1040, 10, "CleanPro Benelux"),
        P("CL-GLV-100", "Nitrile Gloves", "Cleaning", "box of 100", "🧤", 980, 510, 15, "CleanPro Benelux"),
        P("CL-ESP-1K", "Espresso Machine Cleaner", "Cleaning", "1 kg tub", "🧼", 2200, 1220, 8, "CleanPro Benelux"),
        P("SN-CHO-50", "Belgian Chocolate Bars", "Snacks", "box of 50", "🍫", 4200, 2600, 10, "Chocolaterie Gent"),
        P("SN-ALM-1K", "Roasted Almonds", "Snacks", "1 kg bag", "🥜", 1760, 1020, 12, "Nuts & More"),
        P("SN-GRN-24", "Oat Granola Bars", "Snacks", "box of 24", "🌾", 1560, 880, 15, "Nuts & More"),
        P("SN-SPC-36", "Speculoos Biscuits", "Snacks", "box of 36", "🍪", 1320, 690, 20, "Chocolaterie Gent"),
        P("SN-PRZ-30", "Mini Pretzels", "Snacks", "box of 30 packs", "🥨", 1450, 790, 12, "Nuts & More"),
    )

    private val customers = listOf(
        C("Bean There Café", "Emma de Vries", "Amsterdam", "Netherlands", CustomerTier.GOLD, 15_000_00, 30),
        C("Hotel Maasview", "Pieter Smit", "Rotterdam", "Netherlands", CustomerTier.PLATINUM, 40_000_00, 45),
        C("De Kleine Bakkerij", "Sanne Mulder", "Utrecht", "Netherlands", CustomerTier.SILVER, 8_000_00, 30),
        C("Grand Café Lumière", "Julien Laurent", "Antwerp", "Belgium", CustomerTier.GOLD, 20_000_00, 30),
        C("Kaffeehaus Fischer", "Jonas Fischer", "Düsseldorf", "Germany", CustomerTier.SILVER, 10_000_00, 30),
        C("Noord Coworking", "Lisa Hendriks", "Amsterdam", "Netherlands", CustomerTier.BRONZE, 5_000_00, 14),
        C("The Brew Lab", "Daan Vermeer", "Eindhoven", "Netherlands", CustomerTier.GOLD, 12_000_00, 30),
        C("Seaside Hotel Scheveningen", "Femke Bos", "The Hague", "Netherlands", CustomerTier.PLATINUM, 45_000_00, 45),
        C("Café Bruxelles", "Lucas Peeters", "Brussels", "Belgium", CustomerTier.SILVER, 9_000_00, 30),
        C("Morning Glory Brunch", "Noor Jansen", "Haarlem", "Netherlands", CustomerTier.BRONZE, 4_000_00, 14),
        C("Leiden University Canteen", "Thomas van Leeuwen", "Leiden", "Netherlands", CustomerTier.GOLD, 25_000_00, 60),
        C("Köln Kaffee Kontor", "Mia Schneider", "Cologne", "Germany", CustomerTier.SILVER, 11_000_00, 30),
        C("Groen & Gezond", "Eva Visser", "Groningen", "Netherlands", CustomerTier.BRONZE, 6_000_00, 14),
        C("Tech Campus Delft", "Ruben de Groot", "Delft", "Netherlands", CustomerTier.GOLD, 18_000_00, 30),
        C("Patisserie Amélie", "Chloé Dubois", "Ghent", "Belgium", CustomerTier.SILVER, 7_000_00, 30),
        C("Harbour Hostel", "Sam Kuipers", "Rotterdam", "Netherlands", CustomerTier.BRONZE, 5_000_00, 14),
    )

    fun run() {
        transaction {
            if (Users.selectAll().count() == 0L) createCompany()
        }
        val created = transaction { backfill() }
        if (created > 0) log.info("Generated $created demo orders to bring history up to date")
    }

    private fun createCompany() {
        log.info("Seeding demo company …")
        val start = now() - HISTORY_DAYS * DAY_MS - DAY_MS
        val hash = Passwords.hash(DEMO_PASSWORD)
        listOf(
            Triple("Maya Visser", Role.ADMIN, "Operations Director"),
            Triple("Arjun Menon", Role.SALES, "Senior Account Manager"),
            Triple("Noah Bakker", Role.SALES, "Account Manager"),
            Triple("Lars de Boer", Role.WAREHOUSE, "Warehouse Lead"),
            Triple("Sofie Janssen", Role.FINANCE, "Finance Controller"),
        ).forEach { (name, role, title) ->
            Users.insert {
                it[Users.name] = name
                it[email] = name.substringBefore(' ').lowercase() + "@tradeflow.dev"
                it[passwordHash] = hash
                it[Users.role] = role
                it[Users.title] = title
                it[createdAt] = start
            }
        }
        val admin = Users.selectAll().where { Users.role eq Role.ADMIN }.first().toUserDto()
        val random = Random(7)
        products.forEach { p ->
            val opening = p.reorder * (3 + random.nextInt(4))
            val id = Products.insert {
                it[sku] = p.sku
                it[name] = p.name
                it[category] = p.category
                it[unit] = p.unit
                it[emoji] = p.emoji
                it[priceCents] = p.price
                it[costCents] = p.cost
                it[stock] = opening
                it[reorderLevel] = p.reorder
                it[supplier] = p.supplier
                it[createdAt] = start
            } get Products.id
            addMovement(id, opening, "Opening stock", admin, start)
        }
        customers.forEach { c ->
            val first = c.contact.substringBefore(' ').lowercase()
            val domain = c.name.lowercase().filter { it.isLetter() }.take(14)
            Customers.insert {
                it[name] = c.name
                it[contactName] = c.contact
                it[email] = "$first@$domain.example"
                it[phone] = "+31 6 ${1000_0000 + random.nextInt(8999_9999)}"
                it[city] = c.city
                it[country] = c.country
                it[tier] = c.tier
                it[creditLimitCents] = c.credit
                it[paymentTermsDays] = c.terms
                it[createdAt] = start
            }
        }
    }

    /** Generates orders for every day between the newest order and now. Returns how many were made. */
    private fun backfill(): Int {
        val latest = Orders.select(Orders.createdAt).orderBy(Orders.createdAt to SortOrder.DESC).limit(1).firstOrNull()?.get(Orders.createdAt)
        val today = LocalDate.now(ZONE)
        val firstDay = if (latest == null) today.minusDays(HISTORY_DAYS) else Instant.ofEpochMilli(latest).atZone(ZONE).toLocalDate().plusDays(1)
        if (firstDay.isAfter(today)) return 0
        val users = Users.selectAll().map { it.toUserDto() }
        val customerRows = Customers.selectAll().map { it[Customers.id] to it[Customers.tier] }
        val productRows = Products.selectAll().where { Products.active eq true }.map { it[Products.id] }
        if (users.isEmpty() || customerRows.isEmpty() || productRows.isEmpty()) return 0
        val random = Random(firstDay.toEpochDay())
        var count = 0
        var day = firstDay
        while (!day.isAfter(today)) {
            count += generateDay(day, users, customerRows, productRows, random)
            day = day.plusDays(1)
        }
        tidyStockLevels(users, random)
        return count
    }

    private fun generateDay(
        day: LocalDate,
        users: List<UserDto>,
        customerIds: List<Pair<Int, CustomerTier>>,
        productIds: List<Int>,
        random: Random,
    ): Int {
        val dayStart = day.atStartOfDay(ZONE).toInstant().toEpochMilli()
        val age = (now() - dayStart) / DAY_MS.toDouble() // 0 for today
        val growth = 1.0 + (HISTORY_DAYS - age.coerceAtMost(HISTORY_DAYS.toDouble())) / HISTORY_DAYS // 1.0 → 2.0
        val weekend = day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY
        val base = if (weekend) 1.0 else 3.2
        val orders = (base * growth + random.nextDouble() * 2).toInt()
        val reps = users.filter { it.role == Role.SALES }.ifEmpty { users }
        val warehouse = users.firstOrNull { it.role == Role.WAREHOUSE } ?: users.first()
        val finance = users.firstOrNull { it.role == Role.FINANCE } ?: users.first()
        var made = 0
        repeat(orders) {
            // Orders arrive between 07:00 and 17:00.
            val at = dayStart + (7 * 60 + random.nextInt(10 * 60)) * 60_000L
            if (at > now()) return@repeat
            val (customerId, tier) = weightedCustomer(customerIds, random)
            val rep = reps[random.nextInt(reps.size)]
            val lineCount = 2 + random.nextInt(4)
            val scale = when (tier) {
                CustomerTier.PLATINUM -> 3
                CustomerTier.GOLD -> 2
                else -> 1
            }
            val lines = productIds.shuffled(random).take(lineCount).map { OrderLineInput(it, (2 + random.nextInt(10)) * scale) }
            lines.forEach { ensureStock(it.productId, it.quantity, warehouse, at - 60 * 60_000L, random) }
            val (orderId, _) = runCatching {
                createOrder(CreateOrderRequest(customerId, lines, notes = randomNote(random)), rep, at, checkCredit = false)
            }.getOrNull() ?: return@repeat
            made++
            progressOrder(orderId, at, rep, warehouse, finance, random)
        }
        return made
    }

    private fun weightedCustomer(customers: List<Pair<Int, CustomerTier>>, random: Random): Pair<Int, CustomerTier> {
        val weighted = customers.flatMap { c ->
            val weight = when (c.second) {
                CustomerTier.PLATINUM -> 4
                CustomerTier.GOLD -> 3
                CustomerTier.SILVER -> 2
                CustomerTier.BRONZE -> 1
            }
            List(weight) { c }
        }
        return weighted[random.nextInt(weighted.size)]
    }

    private fun randomNote(random: Random): String = listOf(
        "", "", "", "Deliver before 9:00, back entrance", "Call on arrival", "Weekly standing order",
        "Please include invoice copy", "Loading dock B", "",
    )[random.nextInt(9)]

    /** If a product would run out, books a supplier delivery shortly before the order. */
    private fun ensureStock(productId: Int, needed: Int, user: UserDto, at: Long, random: Random) {
        val row = Products.selectAll().where { Products.id eq productId }.single()
        val stock = row[Products.stock]
        val reorder = row[Products.reorderLevel]
        if (stock - needed > reorder / 2) return
        val delivery = reorder * (4 + random.nextInt(4))
        Products.update({ Products.id eq productId }) { it[Products.stock] = stock + delivery }
        addMovement(productId, delivery, "Delivery from ${row[Products.supplier]}", user, at)
    }

    /** Walks an order through its lifecycle up to "now", with realistic gaps between steps. */
    private fun progressOrder(orderId: Int, createdAt: Long, rep: UserDto, warehouse: UserDto, finance: UserDto, random: Random) {
        val hour = 60 * 60_000L
        if (random.nextInt(100) < 4) {
            val at = createdAt + (1 + random.nextInt(5)) * hour
            if (at < now()) step(orderId, OrderStatus.CANCELLED, "Customer changed their mind", rep, at)
            return
        }
        val confirmedAt = createdAt + (1 + random.nextInt(4)) * hour
        val packedAt = confirmedAt + (14 + random.nextInt(10)) * hour
        val shippedAt = packedAt + (3 + random.nextInt(10)) * hour
        val deliveredAt = shippedAt + (12 + random.nextInt(24)) * hour
        if (!step(orderId, OrderStatus.CONFIRMED, "", rep, confirmedAt)) return
        val invoiceId = Invoices.select(Invoices.id, Invoices.dueAt).where { Invoices.orderId eq orderId }.single()
        payInvoice(invoiceId[Invoices.id], invoiceId[Invoices.dueAt], confirmedAt, finance, random)
        if (!step(orderId, OrderStatus.PACKED, "", warehouse, packedAt)) return
        if (!step(orderId, OrderStatus.SHIPPED, "", warehouse, shippedAt)) return
        step(orderId, OrderStatus.DELIVERED, "", warehouse, deliveredAt)
    }

    private fun step(orderId: Int, status: OrderStatus, note: String, user: UserDto, at: Long): Boolean {
        if (at > now()) return false
        changeOrderStatus(orderId, status, note, user, at)
        return true
    }

    /** Most customers pay around the due date, some early, a few late, and a few only part. */
    private fun payInvoice(invoiceId: Int, dueAt: Long, issuedAt: Long, finance: UserDto, random: Random) {
        val amount = Invoices.select(Invoices.amountCents).where { Invoices.id eq invoiceId }.single()[Invoices.amountCents]
        val roll = random.nextInt(100)
        val payAt = when {
            roll < 25 -> issuedAt + (2 + random.nextInt(7)) * DAY_MS // early
            roll < 80 -> dueAt - random.nextInt(6) * DAY_MS // around the due date
            roll < 90 -> dueAt + (3 + random.nextInt(10)) * DAY_MS // late
            else -> return // still unpaid
        }
        if (payAt > now()) return
        val method = PaymentMethod.entries[random.nextInt(PaymentMethod.entries.size - 1)] // rarely cash
        val partial = random.nextInt(100) < 8
        val paid = if (partial) amount / 2 else amount
        recordPayment(invoiceId, paid, method, finance, payAt)
    }

    /** Leaves a handful of products below their reorder level so the low-stock alerts have something to show. */
    private fun tidyStockLevels(users: List<UserDto>, random: Random) {
        val warehouse = users.firstOrNull { it.role == Role.WAREHOUSE } ?: users.first()
        val lowCount = Products.selectAll().count { it[Products.stock] <= it[Products.reorderLevel] }
        if (lowCount >= 4) return
        Products.selectAll().where { Products.active eq true }.shuffled(random).take(5 - lowCount).forEach { row ->
            val id = row[Products.id]
            val target = (row[Products.reorderLevel] * (2 + random.nextInt(7)) / 10).coerceAtLeast(1)
            val delta = target - row[Products.stock]
            if (delta < 0) {
                Products.update({ Products.id eq id }) { it[stock] = target }
                addMovement(id, delta, "Cycle count correction", warehouse, now() - random.nextInt(48) * 60 * 60_000L)
            }
        }
    }

    /** Used by the live demo: one order like the ones above, placed right now. */
    fun simulatedOrder(random: Random): LiveEventDto? {
        val users = Users.selectAll().map { it.toUserDto() }
        val reps = users.filter { it.role == Role.SALES }.ifEmpty { return null }
        val warehouse = users.firstOrNull { it.role == Role.WAREHOUSE } ?: return null
        val customer = weightedCustomer(Customers.selectAll().map { it[Customers.id] to it[Customers.tier] }, random)
        val productIds = Products.selectAll().where { Products.active eq true }.map { it[Products.id] }
        val lines = productIds.shuffled(random).take(2 + random.nextInt(3)).map { OrderLineInput(it, 2 + random.nextInt(8)) }
        lines.forEach { ensureStock(it.productId, it.quantity, warehouse, now() - 60_000, random) }
        val rep = reps[random.nextInt(reps.size)]
        return runCatching { createOrder(CreateOrderRequest(customer.first, lines), rep).second }.getOrNull()
    }

    /** Used by the live demo: moves the oldest open order one step forward. */
    fun simulatedProgress(): LiveEventDto? {
        val users = Users.selectAll().map { it.toUserDto() }
        val row = Orders.selectAll()
            .where { Orders.status inList listOf(OrderStatus.CONFIRMED, OrderStatus.PACKED, OrderStatus.SHIPPED) }
            .orderBy(Orders.updatedAt to SortOrder.ASC)
            .limit(1)
            .firstOrNull() ?: return null
        val next = row[Orders.status].next ?: return null
        val user = users.firstOrNull { it.role == Role.WAREHOUSE } ?: return null
        return runCatching { changeOrderStatus(row[Orders.id], next, "", user) }.getOrNull()
    }
}
