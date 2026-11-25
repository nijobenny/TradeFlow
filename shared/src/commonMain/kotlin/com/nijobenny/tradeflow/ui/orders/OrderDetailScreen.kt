package com.nijobenny.tradeflow.ui.orders

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.OrderDto
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.Permissions
import com.nijobenny.tradeflow.core.Pricing
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.LocalToast
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.actionLabel
import com.nijobenny.tradeflow.ui.color
import com.nijobenny.tradeflow.ui.components.Avatar
import com.nijobenny.tradeflow.ui.components.ChevronGlyph
import com.nijobenny.tradeflow.ui.components.ConfettiBurst
import com.nijobenny.tradeflow.ui.components.ConfirmDialog
import com.nijobenny.tradeflow.ui.components.EmojiTile
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.GradientButton
import com.nijobenny.tradeflow.ui.components.InfoRow
import com.nijobenny.tradeflow.ui.components.InvoicePill
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.ProgressBar
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.SectionCard
import com.nijobenny.tradeflow.ui.components.SoftButton
import com.nijobenny.tradeflow.ui.components.StatusPill
import com.nijobenny.tradeflow.ui.components.TierBadge
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.components.pulse
import com.nijobenny.tradeflow.ui.dateTimeLabel
import com.nijobenny.tradeflow.ui.dueLabel
import com.nijobenny.tradeflow.ui.emoji
import com.nijobenny.tradeflow.ui.money
import com.nijobenny.tradeflow.ui.percentLabel
import com.nijobenny.tradeflow.ui.state.readable
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import com.nijobenny.tradeflow.ui.timeAgo
import kotlinx.coroutines.launch

@Composable
fun OrderDetailScreen(id: Int) {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val toast = LocalToast.current
    val session = LocalSession.current
    val sessionState by session.state.collectAsStateLifecycle()
    val scope = rememberCoroutineScope()
    val loader = rememberLoader(id) { api.order(id) }
    var busy by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }

    fun move(target: OrderStatus) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val updated = api.updateOrderStatus(id, target)
                loader.set(updated)
                toast.success("${updated.number} is now ${target.label}")
                if (target == OrderStatus.DELIVERED || target == OrderStatus.CONFIRMED) confetti++
                session.refreshAll()
            } catch (e: Throwable) {
                toast.error(e.readable())
            } finally {
                busy = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            val order = loader.data
            ScreenHeader(
                title = order?.number ?: "Order",
                subtitle = order?.customer?.name,
                onBack = { nav.pop() },
            ) { if (order != null) StatusPill(order.status) }
            LoadView(loader) { data -> OrderContent(data) }
        }

        // Action bar
        val order = loader.data
        val user = sessionState.user
        if (order != null && user != null && order.status.isOpen) {
            val next = order.status.next
            val canAdvance = next != null && Permissions.canMoveOrder(user.role, next)
            val canCancel = order.status.canCancel && Permissions.canMoveOrder(user.role, OrderStatus.CANCELLED)
            if (canAdvance || canCancel) {
                GlassPanel(
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(14.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    elevation = 26.dp,
                ) {
                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (canCancel) {
                            SoftButton("Cancel", onClick = { confirmCancel = true }, modifier = Modifier.weight(if (canAdvance) 0.7f else 1f))
                        }
                        if (canAdvance && next != null) {
                            GradientButton(
                                if (busy) "Saving…" else "${next.emoji()}  ${next.actionLabel()}",
                                onClick = { move(next) },
                                enabled = !busy,
                                modifier = Modifier.weight(1.3f),
                            )
                        }
                    }
                }
            }
        }

        ConfirmDialog(
            visible = confirmCancel,
            emoji = "🛑",
            title = "Cancel ${order?.number ?: "order"}?",
            message = "Reserved stock goes back to the shelf and the invoice is voided. This can't be undone.",
            confirmText = "Cancel order",
            destructive = true,
            onConfirm = {
                confirmCancel = false
                move(OrderStatus.CANCELLED)
            },
            onDismiss = { confirmCancel = false },
        )
        ConfettiBurst(confetti)
    }
}

@Composable
private fun OrderContent(order: OrderDto) {
    val colors = LocalTradeFlowColors.current
    val nav = LocalNavigator.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        StatusStepper(order.status, Modifier.enterUp(0))

        // Customer
        GlassPanel(Modifier.fillMaxWidth().enterUp(1).bounceClick { nav.push(Route.CustomerDetail(order.customer.id)) }, elevation = 8.dp) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(order.customer.name, size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(order.customer.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${order.customer.contactName} · ${order.customer.city}", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1)
                    Spacer(Modifier.height(5.dp))
                    TierBadge(order.customer.tier)
                }
                ChevronGlyph(colors.textMuted, modifier = Modifier.graphicsLayer { rotationZ = 90f })
            }
        }

        // Lines
        SectionCard("${order.lines.sumOf { it.quantity }} items", Modifier.enterUp(2)) {
            order.lines.forEach { line ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).bounceClick { nav.push(Route.ProductDetail(line.productId)) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EmojiTile(line.emoji, size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(line.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${line.quantity} × ${money(line.unitPriceCents)} · ${line.sku}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    }
                    Text(money(line.lineTotalCents), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))
            Spacer(Modifier.height(8.dp))
            InfoRow("Subtotal", money(order.subtotalCents))
            if (order.discountCents > 0) {
                InfoRow("${order.customer.tier.label} & volume discount", "−" + money(order.discountCents), valueColor = Feedback.Success)
            }
            InfoRow("VAT ${percentLabel(Pricing.TAX_RATE_BP)}", money(order.taxCents))
            InfoRow("Total", money(order.totalCents), bold = true)
        }

        // Invoice
        order.invoice?.let { invoice ->
            GlassPanel(Modifier.fillMaxWidth().enterUp(3).bounceClick { nav.push(Route.InvoiceDetail(invoice.id)) }, elevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧾", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(invoice.number, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(dueLabel(invoice.dueAt), style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                        }
                        InvoicePill(invoice.status)
                    }
                    Spacer(Modifier.height(12.dp))
                    ProgressBar(invoice.paidCents / invoice.amountCents.toFloat().coerceAtLeast(1f), height = 8.dp)
                    Spacer(Modifier.height(6.dp))
                    Text("${money(invoice.paidCents)} of ${money(invoice.amountCents)} paid", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                }
            }
        }

        if (order.notes.isNotBlank()) {
            SectionCard("Delivery notes", Modifier.enterUp(4)) {
                Text("“${order.notes}”", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            }
        }

        // Timeline
        SectionCard("History", Modifier.enterUp(5)) {
            order.events.asReversed().forEachIndexed { i, e ->
                Row(Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(e.status.color()))
                        if (i < order.events.lastIndex) Box(Modifier.width(2.dp).height(46.dp).background(colors.glassBorder))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f).padding(bottom = 10.dp)) {
                        Text(e.status.label, style = MaterialTheme.typography.titleMedium, color = e.status.color())
                        Text(e.note, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                        Text("${e.userName} · ${dateTimeLabel(e.createdAt)} · ${timeAgo(e.createdAt)}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    }
                }
            }
        }
        Text(
            "Created by ${order.createdBy} · ${dateTimeLabel(order.createdAt)}",
            style = MaterialTheme.typography.labelMedium,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(110.dp))
    }
}

/** Five-step progress with a gradient line that springs forward and a pulsing current step. */
@Composable
fun StatusStepper(status: OrderStatus, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    if (status == OrderStatus.CANCELLED) {
        Row(
            modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Feedback.Error.copy(alpha = 0.14f)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✖️", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Order cancelled", style = MaterialTheme.typography.titleMedium, color = Feedback.Error)
                Text("Stock was returned and the invoice voided.", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            }
        }
        return
    }
    val steps = OrderStatus.flow
    val index = steps.indexOf(status).coerceAtLeast(0)
    val progress by animateFloatAsState(
        index / (steps.size - 1f),
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow),
        label = "stepper",
    )
    GlassPanel(modifier.fillMaxWidth(), elevation = 10.dp) {
        Column(Modifier.padding(vertical = 18.dp, horizontal = 14.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth().height(44.dp)) {
                val inset = maxWidth / (steps.size * 2)
                val track = maxWidth - inset * 2
                Box(Modifier.offset(x = inset, y = 20.dp).width(track).height(4.dp).clip(RoundedCornerShape(50)).background(colors.surfaceHigh))
                Box(
                    Modifier.offset(x = inset, y = 20.dp).width(track * progress).height(4.dp).clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(colors.accent2, colors.accent))),
                )
                Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                    steps.forEachIndexed { i, step ->
                        val done = i <= index
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .size(if (i == index) 40.dp else 32.dp)
                                    .pulse(enabled = i == index && status != OrderStatus.DELIVERED, amount = 0.08f)
                                    .clip(CircleShape)
                                    .background(if (done) Brush.linearGradient(listOf(colors.accent, colors.accent2)) else Brush.linearGradient(listOf(colors.surfaceHigh, colors.surfaceHigh))),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(step.emoji(), fontSize = if (i == index) 17.sp else 13.sp, color = if (done) Color.White else colors.textMuted)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                steps.forEachIndexed { i, step ->
                    Text(
                        step.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (i <= index) colors.textPrimary else colors.textMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
