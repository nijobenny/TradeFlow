package com.nijobenny.tradeflow.ui.inventory

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.ProductDto
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.can
import com.nijobenny.tradeflow.ui.components.Chip
import com.nijobenny.tradeflow.ui.components.EmojiTile
import com.nijobenny.tradeflow.ui.components.EmptyView
import com.nijobenny.tradeflow.ui.components.Fab
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.ProgressBar
import com.nijobenny.tradeflow.ui.components.SearchField
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.main.BottomBarSpace
import com.nijobenny.tradeflow.ui.money
import com.nijobenny.tradeflow.ui.moneyShort
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors

@Composable
fun InventoryScreen() {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val colors = LocalTradeFlowColors.current
    val session = LocalSession.current
    val sessionState by session.state.collectAsStateLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var lowOnly by rememberSaveable { mutableStateOf(false) }
    val loader = rememberLoader(query, category, lowOnly) { api.products(query, category, lowOnly) }
    val categories = rememberLoader { api.categories() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Inventory", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp).enterUp(0))
                Text("Live stock across the warehouse", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                Spacer(Modifier.height(12.dp))
                SearchField(query, { query = it }, "Search products, SKU or category")
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("⚠️ Low stock", lowOnly, onClick = { lowOnly = !lowOnly }) }
                item { Chip("All", category == null, onClick = { category = null }) }
                items(categories.data.orEmpty()) { c -> Chip(c, category == c, onClick = { category = if (category == c) null else c }) }
            }
            Spacer(Modifier.height(6.dp))
            LoadView(loader) { list ->
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = BottomBarSpace + 70.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(2) }) { InventorySummary(list) }
                    if (list.isEmpty()) {
                        item(span = { GridItemSpan(2) }) { EmptyView("📦", "No products", "Nothing matches these filters.") }
                    }
                    items(list, key = { it.id }) { p ->
                        ProductCard(p, Modifier.animateItem()) { nav.push(Route.ProductDetail(p.id)) }
                    }
                }
            }
        }
        if (sessionState.user.can(Permission.MANAGE_PRODUCTS)) {
            Fab(
                "Add product",
                onClick = { nav.push(Route.ProductForm()) },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = BottomBarSpace - 8.dp),
            )
        }
    }
}

@Composable
private fun InventorySummary(list: List<ProductDto>) {
    val colors = LocalTradeFlowColors.current
    val value = list.sumOf { it.costCents * it.stock }
    val low = list.count { it.isLowStock }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple("SKUs", "${list.size}", colors.accent),
            Triple("Stock value", moneyShort(value), colors.accent2),
            Triple("Low stock", "$low", if (low > 0) Feedback.Warning else Feedback.Success),
        ).forEach { (label, v, tint) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(tint.copy(alpha = 0.13f)).padding(vertical = 12.dp, horizontal = 12.dp),
            ) {
                Text(v, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, maxLines = 1)
                Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
            }
        }
    }
}

@Composable
private fun ProductCard(p: ProductDto, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    val level = p.stock / (p.reorderLevel * 3f).coerceAtLeast(1f)
    GlassPanel(modifier.fillMaxWidth().bounceClick(onClick = onClick), shape = RoundedCornerShape(22.dp), elevation = 8.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                EmojiTile(p.emoji, size = 50.dp, tint = if (p.isLowStock) Feedback.Warning else null)
                Spacer(Modifier.weight(1f))
                if (p.isLowStock) {
                    Text(
                        "LOW",
                        style = MaterialTheme.typography.labelSmall,
                        color = Feedback.Warning,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Feedback.Warning.copy(alpha = 0.16f)).padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(p.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Text(p.unit, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Text(money(p.priceCents), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Spacer(Modifier.height(8.dp))
            ProgressBar(
                level,
                height = 6.dp,
                brush = if (p.isLowStock) Brush.horizontalGradient(listOf(Feedback.Error, Feedback.Warning)) else null,
            )
            Spacer(Modifier.height(4.dp))
            Text("${p.stock} in stock · ${p.sold30d} sold/30d", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1)
        }
    }
}
