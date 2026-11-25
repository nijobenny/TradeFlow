package com.nijobenny.tradeflow.ui.orders

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.core.OrderSummaryDto
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.can
import com.nijobenny.tradeflow.ui.color
import com.nijobenny.tradeflow.ui.components.Chip
import com.nijobenny.tradeflow.ui.components.EmptyView
import com.nijobenny.tradeflow.ui.components.Fab
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.SearchField
import com.nijobenny.tradeflow.ui.components.StatusPill
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.emoji
import com.nijobenny.tradeflow.ui.main.BottomBarSpace
import com.nijobenny.tradeflow.ui.money
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import com.nijobenny.tradeflow.ui.timeAgo

@Composable
fun OrdersScreen() {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val colors = LocalTradeFlowColors.current
    val session = LocalSession.current
    val sessionState by session.state.collectAsStateLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf<OrderStatus?>(null) }
    val loader = rememberLoader(query, status) { api.orders(status, query) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Orders", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp).enterUp(0))
                val total = loader.data?.counts?.values?.sum()
                Text(
                    if (total == null) "Loading…" else "$total orders · ${loader.data?.counts?.get(OrderStatus.PENDING) ?: 0} waiting for confirmation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(12.dp))
                SearchField(query, { query = it }, "Search order number or client")
            }
            Spacer(Modifier.height(10.dp))
            val counts = loader.data?.counts.orEmpty()
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Chip("All", status == null, onClick = { status = null }) }
                items(OrderStatus.entries) { s ->
                    Chip("${s.emoji()} ${s.label} ${counts[s] ?: 0}", status == s, onClick = { status = if (status == s) null else s })
                }
            }
            Spacer(Modifier.height(6.dp))
            LoadView(loader) { data ->
                if (data.items.isEmpty()) {
                    EmptyView("📭", "No orders here", if (query.isBlank()) "Orders with this status will show up here." else "Nothing matches \"$query\".")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = BottomBarSpace + 70.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(data.items, key = { it.id }) { order ->
                            OrderCard(order, Modifier.animateItem()) { nav.push(Route.OrderDetail(order.id)) }
                        }
                    }
                }
            }
        }
        if (sessionState.user.can(Permission.CREATE_ORDER)) {
            Fab(
                "New order",
                onClick = { nav.push(Route.NewOrder()) },
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = BottomBarSpace - 8.dp),
            )
        }
    }
}

@Composable
fun OrderCard(order: OrderSummaryDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    GlassPanel(modifier.fillMaxWidth().bounceClick(onClick = onClick), shape = RoundedCornerShape(20.dp), elevation = 6.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(order.status.color().copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Text(order.status.emoji(), fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.number, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(timeAgo(order.createdAt), style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                }
                Text(order.customerName, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${order.itemCount} items", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(order.totalCents), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                StatusPill(order.status)
            }
        }
    }
}
