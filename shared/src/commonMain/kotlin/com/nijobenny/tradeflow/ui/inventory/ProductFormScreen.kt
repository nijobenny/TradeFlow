package com.nijobenny.tradeflow.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.Money
import com.nijobenny.tradeflow.core.ProductDto
import com.nijobenny.tradeflow.core.ProductInput
import com.nijobenny.tradeflow.core.Validation
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.LocalToast
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.components.FormField
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.GradientButton
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.shake
import com.nijobenny.tradeflow.ui.percentLabel
import com.nijobenny.tradeflow.ui.state.readable
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import kotlinx.coroutines.launch

private val emojiChoices = listOf("📦", "☕", "🫘", "🍵", "🥛", "🥐", "🍞", "🧁", "🧇", "💧", "🍊", "🥤", "🍯", "🍫", "🍪", "🥜", "🧴", "🧽", "🧤", "🛍️")

private fun centsText(cents: Long?): String = cents?.let { Money.format(it, symbol = "") } ?: ""

/** Add a new product, or edit one when [existing] is given. */
@Composable
fun ProductFormScreen(existing: ProductDto?) {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val toast = LocalToast.current
    val session = LocalSession.current
    val colors = LocalTradeFlowColors.current
    val scope = rememberCoroutineScope()

    var sku by remember { mutableStateOf(existing?.sku ?: "") }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: "") }
    var unit by remember { mutableStateOf(existing?.unit ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "📦") }
    var price by remember { mutableStateOf(centsText(existing?.priceCents).replace(",", "")) }
    var cost by remember { mutableStateOf(centsText(existing?.costCents).replace(",", "")) }
    var reorder by remember { mutableStateOf(existing?.reorderLevel?.toString() ?: "10") }
    var supplier by remember { mutableStateOf(existing?.supplier ?: "") }
    var initialStock by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf<String?>(null) }
    var shakes by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }

    val priceCents = Money.parse(price)
    val costCents = Money.parse(cost)
    val margin = if (priceCents != null && costCents != null && priceCents > 0) ((priceCents - costCents) * 10_000 / priceCents).toInt() else null

    fun save() {
        val input = ProductInput(
            sku = sku, name = name, category = category, unit = unit, emoji = emoji,
            priceCents = priceCents ?: -1L, costCents = costCents ?: -1L,
            reorderLevel = reorder.toIntOrNull() ?: -1, supplier = supplier,
            initialStock = if (existing == null) initialStock.toIntOrNull() ?: -1 else 0,
        )
        val problem = Validation.product(input)
        if (problem != null) {
            error = problem
            shakes++
            return
        }
        saving = true
        scope.launch {
            try {
                val saved = if (existing == null) api.createProduct(input) else api.updateProduct(existing.id, input)
                toast.success(if (existing == null) "${saved.product.name} added" else "Changes saved")
                session.refreshAll()
                nav.replace(Route.ProductDetail(saved.product.id))
            } catch (e: Throwable) {
                error = e.readable()
                shakes++
            } finally {
                saving = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            ScreenHeader(if (existing == null) "New product" else "Edit product", existing?.sku, onBack = { nav.pop() })
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().shake(shakes),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Spacer(Modifier.height(4.dp))
                Text("Icon", style = MaterialTheme.typography.labelLarge, color = colors.textMuted)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojiChoices.forEach { e ->
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (e == emoji) colors.accent.copy(alpha = 0.2f) else colors.surfaceHigh)
                                .border(2.dp, if (e == emoji) colors.accent else Color.Transparent, RoundedCornerShape(14.dp))
                                .bounceClick { emoji = e },
                            contentAlignment = Alignment.Center,
                        ) { Text(e, fontSize = 22.sp) }
                    }
                }
                FormField("Product name", name, { name = it; error = null }, placeholder = "e.g. Barista Oat Milk")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        "SKU", sku, { sku = it.uppercase(); error = null }, Modifier.weight(1f),
                        placeholder = "DY-OAT-12", capitalization = KeyboardCapitalization.Characters,
                    )
                    FormField("Category", category, { category = it }, Modifier.weight(1f), placeholder = "Dairy")
                }
                if (existing != null) {
                    Text("SKU can't be changed after creation.", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                }
                FormField("Pack size / unit", unit, { unit = it }, placeholder = "case of 12 × 1 L")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField("Price", price, { price = it; error = null }, Modifier.weight(1f), prefix = "€", keyboardType = KeyboardType.Decimal, placeholder = "0.00")
                    FormField("Cost", cost, { cost = it; error = null }, Modifier.weight(1f), prefix = "€", keyboardType = KeyboardType.Decimal, placeholder = "0.00")
                }
                if (margin != null) {
                    GlassPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = 4.dp) {
                        Text(
                            "Margin ${percentLabel(margin)} · ${Money.format((priceCents ?: 0) - (costCents ?: 0))} per unit",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (margin < 1500) Feedback.Warning else Feedback.Success,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField("Reorder level", reorder, { reorder = it.filter(Char::isDigit) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    if (existing == null) {
                        FormField("Opening stock", initialStock, { initialStock = it.filter(Char::isDigit) }, Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    }
                }
                FormField("Supplier", supplier, { supplier = it }, placeholder = "e.g. Noord Zuivel")
                error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Feedback.Error) }
                GradientButton(
                    if (saving) "Saving…" else if (existing == null) "Add product" else "Save changes",
                    onClick = ::save,
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                )
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}
