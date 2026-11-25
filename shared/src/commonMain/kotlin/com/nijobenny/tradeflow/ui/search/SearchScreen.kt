package com.nijobenny.tradeflow.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.core.SearchResultDto
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.components.Avatar
import com.nijobenny.tradeflow.ui.components.EmojiTile
import com.nijobenny.tradeflow.ui.components.EmptyView
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.InvoicePill
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.SearchField
import com.nijobenny.tradeflow.ui.components.StatusPill
import com.nijobenny.tradeflow.ui.components.TierBadge
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.money
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors

private val empty = SearchResultDto(emptyList(), emptyList(), emptyList(), emptyList())

/** One search box across products, clients, orders and invoices (server-side, PostgreSQL). */
@Composable
fun SearchScreen() {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val colors = LocalTradeFlowColors.current
    var query by remember { mutableStateOf("") }
    val loader = rememberLoader(query) { if (query.trim().length < 2) empty else api.search(query.trim()) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader("Search", "Products, clients, orders and invoices", onBack = { nav.pop() })
        SearchField(query, { query = it }, "Try “oat”, “SO-10”, “Rotterdam”…")
        Spacer(Modifier.height(10.dp))
        LoadView(loader) { r ->
            val total = r.products.size + r.customers.size + r.orders.size + r.invoices.size
            if (query.trim().length < 2) {
                EmptyView("🔎", "Search everything", "Type at least two letters.")
            } else if (total == 0 && loader.refreshing) {
                EmptyView("⏳", "Searching…", "Looking through products, clients, orders and invoices.")
            } else if (total == 0) {
                EmptyView("🤷", "No results", "Nothing matches \"$query\".")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (r.customers.isNotEmpty()) {
                        item { Header("Clients") }
                        items(r.customers, key = { "c${it.id}" }) { c ->
                            ResultRow(onClick = { nav.push(Route.CustomerDetail(c.id)) }) {
                                Avatar(c.name, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(c.city, style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                                }
                                TierBadge(c.tier)
                            }
                        }
                    }
                    if (r.products.isNotEmpty()) {
                        item { Header("Products") }
                        items(r.products, key = { "p${it.id}" }) { p ->
                            ResultRow(onClick = { nav.push(Route.ProductDetail(p.id)) }) {
                                EmojiTile(p.emoji, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${p.sku} · ${p.stock} in stock", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                                }
                                Text(money(p.priceCents), style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                            }
                        }
                    }
                    if (r.orders.isNotEmpty()) {
                        item { Header("Orders") }
                        items(r.orders, key = { "o${it.id}" }) { o ->
                            ResultRow(onClick = { nav.push(Route.OrderDetail(o.id)) }) {
                                Column(Modifier.weight(1f)) {
                                    Text(o.number, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text("${o.customerName} · ${money(o.totalCents)}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1)
                                }
                                StatusPill(o.status)
                            }
                        }
                    }
                    if (r.invoices.isNotEmpty()) {
                        item { Header("Invoices") }
                        items(r.invoices, key = { "i${it.id}" }) { inv ->
                            ResultRow(onClick = { nav.push(Route.InvoiceDetail(inv.id)) }) {
                                Column(Modifier.weight(1f)) {
                                    Text(inv.number, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text("${inv.customerName} · ${money(inv.amountCents)}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1)
                                }
                                InvoicePill(inv.status)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(text: String) {
    val colors = LocalTradeFlowColors.current
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.textMuted, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

@Composable
private fun ResultRow(onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().bounceClick(onClick = onClick), shape = RoundedCornerShape(18.dp), elevation = 5.dp) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
