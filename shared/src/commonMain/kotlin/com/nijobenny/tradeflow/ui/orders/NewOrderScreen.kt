package com.nijobenny.tradeflow.ui.orders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.CreateOrderRequest
import com.nijobenny.tradeflow.core.CustomerDto
import com.nijobenny.tradeflow.core.OrderLineInput
import com.nijobenny.tradeflow.core.Pricing
import com.nijobenny.tradeflow.core.ProductDto
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.LocalToast
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.components.Avatar
import com.nijobenny.tradeflow.ui.components.CheckGlyph
import com.nijobenny.tradeflow.ui.components.Chip
import com.nijobenny.tradeflow.ui.components.ConfettiBurst
import com.nijobenny.tradeflow.ui.components.EmojiTile
import com.nijobenny.tradeflow.ui.components.EmptyView
import com.nijobenny.tradeflow.ui.components.FormField
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.GradientButton
import com.nijobenny.tradeflow.ui.components.InfoRow
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.MoneyCounter
import com.nijobenny.tradeflow.ui.components.ProgressBar
import com.nijobenny.tradeflow.ui.components.QuantityStepper
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.SearchField
import com.nijobenny.tradeflow.ui.components.SectionCard
import com.nijobenny.tradeflow.ui.components.TierBadge
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.consumeTouches
import com.nijobenny.tradeflow.ui.money
import com.nijobenny.tradeflow.ui.moneyShort
import com.nijobenny.tradeflow.ui.percentLabel
import com.nijobenny.tradeflow.ui.state.readable
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Step(val title: String) { Customer("Choose a client"), Products("Add products"), Review("Review & place") }

@Composable
fun NewOrderScreen(presetCustomerId: Int?) {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val toast = LocalToast.current
    val session = LocalSession.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(Step.Customer) }
    var customer by remember { mutableStateOf<CustomerDto?>(null) }
    val cart = remember { mutableStateMapOf<Int, Int>() }
    var notes by remember { mutableStateOf("") }
    var placing by remember { mutableStateOf(false) }
    var placedId by remember { mutableStateOf<Int?>(null) }
    var confetti by remember { mutableStateOf(0) }

    val customers = rememberLoader { api.customers() }
    val products = rememberLoader { api.products() }

    // Coming from a client's page: skip straight to products.
    LaunchedEffect(customers.data) {
        val list = customers.data
        if (presetCustomerId != null && customer == null && list != null) {
            customer = list.firstOrNull { it.id == presetCustomerId }
            if (customer != null) step = Step.Products
        }
    }

    val productList = products.data.orEmpty()
    val lines = cart.entries.mapNotNull { (id, qty) -> productList.firstOrNull { it.id == id }?.let { it to qty } }.filter { it.second > 0 }
    val quote = Pricing.quote(lines.map { Pricing.Line(it.first.priceCents, it.second) }, customer?.tier ?: com.nijobenny.tradeflow.core.CustomerTier.BRONZE)

    fun back() {
        when (step) {
            Step.Customer -> nav.pop()
            Step.Products -> if (presetCustomerId != null) nav.pop() else step = Step.Customer
            Step.Review -> step = Step.Products
        }
    }

    fun place() {
        val c = customer ?: return
        if (placing) return
        placing = true
        scope.launch {
            try {
                val order = api.createOrder(CreateOrderRequest(c.id, lines.map { OrderLineInput(it.first.id, it.second) }, notes))
                confetti++
                placedId = order.id
                session.refreshAll()
                delay(1600)
                nav.replace(Route.OrderDetail(order.id))
            } catch (e: Throwable) {
                toast.error(e.readable())
            } finally {
                placing = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            ScreenHeader("New order", "Step ${step.ordinal + 1} of 3 · ${step.title}", onBack = ::back)
            StepDots(step.ordinal)
            Spacer(Modifier.height(10.dp))
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { if (forward) it / 2 else -it / 2 } + fadeIn(tween(280))) togetherWith
                        (slideOutHorizontally(tween(300)) { if (forward) -it / 3 else it / 3 } + fadeOut(tween(200)))
                },
                modifier = Modifier.weight(1f),
                label = "orderStep",
            ) { current ->
                when (current) {
                    Step.Customer -> LoadView(customers) { list ->
                        CustomerPicker(list, customer?.id) {
                            customer = it
                            step = Step.Products
                        }
                    }
                    Step.Products -> LoadView(products) { list ->
                        ProductPicker(list, customer, cart)
                    }
                    Step.Review -> ReviewStep(customer, lines, quote, notes) { notes = it }
                }
            }
        }

        // Bottom bar with the live total.
        val showBar = step != Step.Customer && lines.isNotEmpty()
        AnimatedVisibility(
            visible = showBar,
            enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CartBar(
                itemCount = lines.sumOf { it.second },
                quote = quote,
                buttonText = if (step == Step.Products) "Review" else if (placing) "Placing…" else "Place order",
                enabled = !placing,
                onClick = { if (step == Step.Products) step = Step.Review else place() },
            )
        }

        // Success overlay.
        AnimatedVisibility(visible = placedId != null, enter = fadeIn(tween(300)), exit = fadeOut()) {
            OrderPlaced()
        }
        ConfettiBurst(confetti)
    }
}

@Composable
private fun StepDots(current: Int) {
    val colors = LocalTradeFlowColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            Box(
                Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50))
                    .background(if (i <= current) colors.accent else colors.surfaceHigh),
            )
        }
    }
}

@Composable
private fun CustomerPicker(list: List<CustomerDto>, selectedId: Int?, onPick: (CustomerDto) -> Unit) {
    val colors = LocalTradeFlowColors.current
    var query by remember { mutableStateOf("") }
    val filtered = list.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) || it.city.contains(query, ignoreCase = true) }
    Column(Modifier.fillMaxSize()) {
        SearchField(query, { query = it }, "Search clients")
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
            items(filtered, key = { it.id }) { c ->
                val used = c.outstandingCents / c.creditLimitCents.toFloat().coerceAtLeast(1f)
                GlassPanel(
                    Modifier.fillMaxWidth().animateItem().bounceClick { onPick(c) },
                    shape = RoundedCornerShape(20.dp),
                    elevation = if (c.id == selectedId) 16.dp else 6.dp,
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(c.name, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${c.city} · ${c.tier.label} pricing (−${c.tier.discountBp / 100}%)", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                            Spacer(Modifier.height(6.dp))
                            ProgressBar(used, height = 5.dp)
                            Text("${moneyShort(c.availableCreditCents)} credit available", style = MaterialTheme.typography.labelMedium, color = if (used > 0.85f) Feedback.Warning else colors.textMuted)
                        }
                        Spacer(Modifier.width(8.dp))
                        TierBadge(c.tier)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductPicker(list: List<ProductDto>, customer: CustomerDto?, cart: MutableMap<Int, Int>) {
    val colors = LocalTradeFlowColors.current
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    val categories = list.map { it.category }.distinct().sorted()
    val filtered = list.filter {
        (category == null || it.category == category) &&
            (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.sku.contains(query, ignoreCase = true))
    }
    Column(Modifier.fillMaxSize()) {
        if (customer != null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.accent.copy(alpha = 0.12f)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(customer.name, size = 32.dp)
                Spacer(Modifier.width(10.dp))
                Text(customer.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TierBadge(customer.tier)
            }
            Spacer(Modifier.height(10.dp))
        }
        SearchField(query, { query = it }, "Search products or SKU")
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip("All", category == null, onClick = { category = null }) }
            items(categories) { c -> Chip(c, category == c, onClick = { category = if (category == c) null else c }) }
        }
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            EmptyView("🔍", "No products found", "Try another search or category.")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 4.dp, bottom = 130.dp)) {
            items(filtered, key = { it.id }) { p ->
                val qty = cart[p.id] ?: 0
                GlassPanel(Modifier.fillMaxWidth().animateItem(), shape = RoundedCornerShape(20.dp), elevation = if (qty > 0) 14.dp else 5.dp) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        EmojiTile(p.emoji, size = 48.dp, tint = if (qty > 0) colors.accent else null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${money(p.priceCents)} · ${p.unit}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1)
                            Text(
                                if (p.stock == 0) "Out of stock" else "${p.stock} in stock",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (p.isLowStock) Feedback.Warning else Feedback.Success,
                            )
                        }
                        QuantityStepper(qty, { cart[p.id] = it.coerceIn(0, p.stock) }, max = p.stock)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(
    customer: CustomerDto?,
    lines: List<Pair<ProductDto, Int>>,
    quote: Pricing.Quote,
    notes: String,
    onNotes: (String) -> Unit,
) {
    val colors = LocalTradeFlowColors.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (customer != null) {
            val overLimit = customer.outstandingCents + quote.totalCents > customer.creditLimitCents
            GlassPanel(Modifier.fillMaxWidth(), elevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(customer.name, size = 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(customer.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Pays within ${customer.paymentTermsDays} days", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                        }
                        TierBadge(customer.tier)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Credit after this order: ${money(customer.creditLimitCents - customer.outstandingCents - quote.totalCents)} of ${money(customer.creditLimitCents)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (overLimit) Feedback.Error else colors.textMuted,
                    )
                    if (overLimit) {
                        Spacer(Modifier.height(6.dp))
                        Text("⚠️ This order goes over the client's credit limit, so the server will refuse it.", style = MaterialTheme.typography.bodyMedium, color = Feedback.Error)
                    }
                }
            }
        }
        SectionCard("${lines.sumOf { it.second }} items") {
            lines.forEach { (p, qty) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("$qty × ${p.name}", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(money(p.priceCents * qty), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))
            Spacer(Modifier.height(8.dp))
            InfoRow("Subtotal", money(quote.subtotalCents))
            if (quote.tierDiscountCents > 0) InfoRow("${customer?.tier?.label} discount", "−" + money(quote.tierDiscountCents), valueColor = Feedback.Success)
            if (quote.bulkDiscountCents > 0) InfoRow("Bulk discount ${percentLabel(Pricing.BULK_DISCOUNT_BP)}", "−" + money(quote.bulkDiscountCents), valueColor = Feedback.Success)
            InfoRow("VAT ${percentLabel(Pricing.TAX_RATE_BP)}", money(quote.taxCents))
            InfoRow("Total", money(quote.totalCents), bold = true)
        }
        FormField("Delivery notes (optional)", notes, onNotes, placeholder = "e.g. Deliver before 9:00, back entrance", singleLine = false)
        Text(
            "Prices come from the same pricing rules the server uses, so this is exactly what will be charged.",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(130.dp))
    }
}

@Composable
private fun CartBar(itemCount: Int, quote: Pricing.Quote, buttonText: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    val toBulk = Pricing.toBulk(quote.subtotalCents)
    GlassPanel(
        Modifier.navigationBarsPadding().padding(14.dp).fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        elevation = 28.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            if (toBulk > 0) {
                Text("Add ${moneyShort(toBulk)} more to unlock a ${percentLabel(Pricing.BULK_DISCOUNT_BP)} bulk discount", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                Spacer(Modifier.height(5.dp))
                ProgressBar(quote.subtotalCents / Pricing.BULK_THRESHOLD_CENTS.toFloat(), height = 6.dp)
            } else {
                Text("🎉 Bulk discount unlocked", style = MaterialTheme.typography.labelLarge, color = Feedback.Success)
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("$itemCount items · incl. VAT", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    MoneyCounter(quote.totalCents, MaterialTheme.typography.titleLarge, colors.textPrimary, showCents = true)
                }
                GradientButton(buttonText, onClick = onClick, enabled = enabled, modifier = Modifier.width(160.dp))
            }
        }
    }
}

@Composable
private fun OrderPlaced() {
    val colors = LocalTradeFlowColors.current
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
    Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = 0.92f)).consumeTouches(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(120.dp).graphicsLayer { scaleX = scale.value; scaleY = scale.value }.clip(CircleShape).background(colors.gradient),
                contentAlignment = Alignment.Center,
            ) { CheckGlyph(Color.White, size = 56.dp) }
            Spacer(Modifier.height(20.dp))
            Text("Order placed!", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
            Text("Stock is reserved and the team has been notified.", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, textAlign = TextAlign.Center)
        }
    }
}
