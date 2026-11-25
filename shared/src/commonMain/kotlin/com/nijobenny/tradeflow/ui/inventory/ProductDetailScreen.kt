package com.nijobenny.tradeflow.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.ProductDetailDto
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.LocalToast
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.can
import com.nijobenny.tradeflow.ui.components.BottomSheet
import com.nijobenny.tradeflow.ui.components.Chip
import com.nijobenny.tradeflow.ui.components.EditGlyph
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.GradientButton
import com.nijobenny.tradeflow.ui.components.IconBubble
import com.nijobenny.tradeflow.ui.components.InfoRow
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.MiniBars
import com.nijobenny.tradeflow.ui.components.ProgressRing
import com.nijobenny.tradeflow.ui.components.QuantityStepper
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.SectionCard
import com.nijobenny.tradeflow.ui.components.SegmentedSelector
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.components.floating
import com.nijobenny.tradeflow.ui.money
import com.nijobenny.tradeflow.ui.percentLabel
import com.nijobenny.tradeflow.ui.state.readable
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import com.nijobenny.tradeflow.ui.timeAgo
import kotlinx.coroutines.launch

@Composable
fun ProductDetailScreen(id: Int) {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val toast = LocalToast.current
    val session = LocalSession.current
    val colors = LocalTradeFlowColors.current
    val sessionState by session.state.collectAsStateLifecycle()
    val scope = rememberCoroutineScope()
    val loader = rememberLoader(id) { api.product(id) }
    var showAdjust by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            val product = loader.data?.product
            ScreenHeader(product?.name ?: "Product", product?.sku, onBack = { nav.pop() }) {
                if (product != null && sessionState.user.can(Permission.MANAGE_PRODUCTS)) {
                    IconBubble(onClick = { nav.push(Route.ProductForm(product)) }) { EditGlyph(colors.textPrimary, size = 18.dp) }
                }
            }
            LoadView(loader) { data ->
                ProductContent(data, canAdjust = sessionState.user.can(Permission.ADJUST_STOCK), onAdjust = { showAdjust = true })
            }
        }

        val product = loader.data?.product
        AdjustStockSheet(
            visible = showAdjust && product != null,
            currentStock = product?.stock ?: 0,
            onDismiss = { showAdjust = false },
            onSave = { delta, reason ->
                scope.launch {
                    try {
                        loader.set(api.adjustStock(id, delta, reason))
                        showAdjust = false
                        toast.success("Stock updated (${if (delta > 0) "+" else ""}$delta)")
                        session.refreshAll()
                    } catch (e: Throwable) {
                        toast.error(e.readable())
                    }
                }
            },
        )
    }
}

@Composable
private fun ProductContent(data: ProductDetailDto, canAdjust: Boolean, onAdjust: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    val p = data.product
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(4.dp))
        // Hero
        Box(
            Modifier.fillMaxWidth().enterUp(0)
                .shadow(22.dp, RoundedCornerShape(28.dp), ambientColor = colors.accent, spotColor = colors.accent)
                .clip(RoundedCornerShape(28.dp))
                .background(colors.vividGradient)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(4.dp))
                    Text(money(p.priceCents), style = MaterialTheme.typography.displaySmall, color = Color.White)
                    Text("per ${p.unit}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroChip("Cost ${money(p.costCents)}")
                        HeroChip("Margin ${percentLabel(p.marginBp)}")
                    }
                }
                Text(p.emoji, fontSize = 64.sp, modifier = Modifier.floating())
            }
        }

        // Stock
        GlassPanel(Modifier.fillMaxWidth().enterUp(1), elevation = 10.dp) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(p.stock / (p.reorderLevel * 3f).coerceAtLeast(1f), size = 92.dp, stroke = 10.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${p.stock}", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        Text("in stock", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (p.isLowStock) "⚠️ Below reorder level" else "✅ Healthy stock",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (p.isLowStock) Feedback.Warning else Feedback.Success,
                    )
                    Text("Reorder at ${p.reorderLevel} · supplier ${p.supplier}", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    Spacer(Modifier.height(10.dp))
                    if (canAdjust) GradientButton("Adjust stock", onClick = onAdjust, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        SectionCard("Units sold per week", Modifier.enterUp(2)) {
            MiniBars(data.salesByWeek)
            Spacer(Modifier.height(10.dp))
            InfoRow("Sold in the last 30 days", "${p.sold30d} units")
            InfoRow("Revenue in the last 30 days", money(data.revenue30dCents), bold = true)
        }

        SectionCard("Stock movements", Modifier.enterUp(3)) {
            if (data.movements.isEmpty()) {
                Text("No movements yet.", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
            }
            data.movements.forEach { m ->
                val incoming = m.delta > 0
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background((if (incoming) Feedback.Success else Feedback.Error).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (incoming) "↓" else "↑", style = MaterialTheme.typography.titleMedium, color = if (incoming) Feedback.Success else Feedback.Error) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.reason, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1)
                        Text("${m.userName} · ${timeAgo(m.createdAt)}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    }
                    Text(
                        (if (incoming) "+" else "") + m.delta,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (incoming) Feedback.Success else Feedback.Error,
                    )
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun HeroChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private enum class Direction(val label: String) { In("Stock in"), Out("Stock out") }

@Composable
private fun AdjustStockSheet(visible: Boolean, currentStock: Int, onDismiss: () -> Unit, onSave: (Int, String) -> Unit) {
    val colors = LocalTradeFlowColors.current
    var direction by remember { mutableStateOf(Direction.In) }
    var amount by remember { mutableIntStateOf(12) }
    var reason by remember { mutableStateOf("Supplier delivery") }
    val reasons = if (direction == Direction.In) listOf("Supplier delivery", "Customer return", "Count correction") else listOf("Damaged", "Expired", "Count correction", "Sample")
    BottomSheet(visible = visible, onDismiss = onDismiss) {
        Text("Adjust stock", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text("Currently $currentStock in stock", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        Spacer(Modifier.height(16.dp))
        SegmentedSelector(Direction.entries, direction, { it.label }, {
            direction = it
            reason = if (it == Direction.In) "Supplier delivery" else "Damaged"
        })
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Quantity", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
            QuantityStepper(amount, { amount = it.coerceAtLeast(1) }, max = if (direction == Direction.Out) currentStock.coerceAtLeast(1) else 9_999)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(6, 12, 24, 48).forEach { q ->
                Chip("$q", amount == q, onClick = { amount = q }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Reason", style = MaterialTheme.typography.labelLarge, color = colors.textMuted)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reasons.forEach { r -> Chip(r, reason == r, onClick = { reason = r }) }
        }
        Spacer(Modifier.height(20.dp))
        val delta = if (direction == Direction.In) amount else -amount
        GradientButton(
            "${if (delta > 0) "Add" else "Remove"} ${kotlin.math.abs(delta)} · new stock ${currentStock + delta}",
            onClick = { onSave(delta, reason) },
            enabled = currentStock + delta >= 0,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
