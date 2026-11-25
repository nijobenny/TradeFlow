package com.nijobenny.tradeflow.ui.customers

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.core.CustomerDto
import com.nijobenny.tradeflow.core.CustomerTier
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.can
import com.nijobenny.tradeflow.ui.components.Avatar
import com.nijobenny.tradeflow.ui.components.Chip
import com.nijobenny.tradeflow.ui.components.EmptyView
import com.nijobenny.tradeflow.ui.components.Fab
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.ProgressBar
import com.nijobenny.tradeflow.ui.components.SearchField
import com.nijobenny.tradeflow.ui.components.TierBadge
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.emoji
import com.nijobenny.tradeflow.ui.main.BottomBarSpace
import com.nijobenny.tradeflow.ui.moneyShort
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import com.nijobenny.tradeflow.ui.timeAgo

@Composable
fun CustomersScreen() {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val colors = LocalTradeFlowColors.current
    val session = LocalSession.current
    val sessionState by session.state.collectAsStateLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var tier by rememberSaveable { mutableStateOf<CustomerTier?>(null) }
    val loader = rememberLoader(query, tier) { api.customers(query, tier) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Clients", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp).enterUp(0))
                val list = loader.data
                Text(
                    if (list == null) "Loading…" else "${list.size} business accounts · ${moneyShort(list.sumOf { it.outstandingCents })} outstanding",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(12.dp))
                SearchField(query, { query = it }, "Search by name, contact or city")
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("All", tier == null, onClick = { tier = null }) }
                items(CustomerTier.entries.reversed()) { t -> Chip("${t.emoji()} ${t.label}", tier == t, onClick = { tier = if (tier == t) null else t }) }
            }
            Spacer(Modifier.height(6.dp))
            LoadView(loader) { list ->
                if (list.isEmpty()) {
                    EmptyView("🤝", "No clients found", "Try another search.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = BottomBarSpace + 70.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(list, key = { it.id }) { c ->
                            CustomerCard(c, Modifier.animateItem()) { nav.push(Route.CustomerDetail(c.id)) }
                        }
                    }
                }
            }
        }
        if (sessionState.user.can(Permission.MANAGE_CUSTOMERS)) {
            Fab(
                "Add client",
                onClick = { nav.push(Route.CustomerForm()) },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = BottomBarSpace - 8.dp),
            )
        }
    }
}

@Composable
fun CustomerCard(c: CustomerDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    val used = c.outstandingCents / c.creditLimitCents.toFloat().coerceAtLeast(1f)
    GlassPanel(modifier.fillMaxWidth().bounceClick(onClick = onClick), shape = RoundedCornerShape(20.dp), elevation = 6.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(c.name, size = 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${c.contactName} · ${c.city}", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TierBadge(c.tier)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lifetime", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    Text(moneyShort(c.totalSpentCents), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text("Orders", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    Text("${c.orderCount}", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                Column(Modifier.weight(1.3f)) {
                    Text("Last order", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    Text(c.lastOrderAt?.let { timeAgo(it) } ?: "—", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1)
                }
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(
                used,
                height = 6.dp,
                brush = if (used > 0.85f) Brush.horizontalGradient(listOf(Feedback.Warning, Feedback.Error)) else null,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Credit used ${moneyShort(c.outstandingCents)} of ${moneyShort(c.creditLimitCents)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (used > 0.85f) Feedback.Warning else colors.textMuted,
            )
        }
    }
}
