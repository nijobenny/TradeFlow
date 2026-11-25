package com.nijobenny.tradeflow.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.ActivityDto
import com.nijobenny.tradeflow.core.DashboardDto
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.color
import com.nijobenny.tradeflow.ui.components.AnimatedCounter
import com.nijobenny.tradeflow.ui.components.ArrowGlyph
import com.nijobenny.tradeflow.ui.components.Avatar
import com.nijobenny.tradeflow.ui.components.AreaChart
import com.nijobenny.tradeflow.ui.components.DonutChart
import com.nijobenny.tradeflow.ui.components.EmojiTile
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.IconBubble
import com.nijobenny.tradeflow.ui.components.LiveBadge
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.MoneyCounter
import com.nijobenny.tradeflow.ui.components.ProgressBar
import com.nijobenny.tradeflow.ui.components.RankBar
import com.nijobenny.tradeflow.ui.components.SearchGlyph
import com.nijobenny.tradeflow.ui.components.SectionCard
import com.nijobenny.tradeflow.ui.components.Slice
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.emoji
import com.nijobenny.tradeflow.ui.main.BottomBarSpace
import com.nijobenny.tradeflow.ui.main.Tab
import com.nijobenny.tradeflow.ui.moneyShort
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import com.nijobenny.tradeflow.ui.timeAgo
import com.nijobenny.tradeflow.platform.currentTimeMillis
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
fun DashboardScreen(onOpenTab: (Tab) -> Unit) {
    val api = LocalApi.current
    val loader = rememberLoader { api.dashboard() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        DashboardHeader()
        LoadView(loader) { data -> DashboardContent(data, onOpenTab) }
    }
}

@OptIn(ExperimentalTime::class)
private fun greeting(): String {
    val hour = Instant.fromEpochMilliseconds(currentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun DashboardHeader() {
    val colors = LocalTradeFlowColors.current
    val session = LocalSession.current
    val nav = LocalNavigator.current
    val state by session.state.collectAsStateLifecycle()
    val user = state.user
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting(), style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1)
            Text(
                user?.name?.substringBefore(' ') ?: "",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(state.company?.name ?: "TradeFlow", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LiveBadge(state.liveConnected)
        Spacer(Modifier.width(8.dp))
        IconBubble(onClick = { nav.push(Route.Search) }) { SearchGlyph(colors.textPrimary, size = 18.dp) }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.bounceClick { nav.push(Route.Settings) }) { Avatar(user?.name ?: "?", size = 42.dp) }
    }
}

@Composable
private fun DashboardContent(data: DashboardDto, onOpenTab: (Tab) -> Unit) {
    val colors = LocalTradeFlowColors.current
    val nav = LocalNavigator.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Spacer(Modifier.height(2.dp))
        RevenueHero(data, Modifier.enterUp(0))

        Row(Modifier.fillMaxWidth().enterUp(1), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiTile("📥", "Open orders", data.openOrders, "${data.ordersToday} new today", colors.accent, Modifier.weight(1f)) { onOpenTab(Tab.Orders) }
            KpiTile("⚠️", "Low stock", data.lowStockCount, "items to reorder", Feedback.Warning, Modifier.weight(1f)) { onOpenTab(Tab.Inventory) }
        }
        Row(Modifier.fillMaxWidth().enterUp(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyTile("🧾", "Outstanding", data.outstandingCents, "${moneyShort(data.overdueCents)} overdue", Feedback.Error, Modifier.weight(1f)) { nav.push(Route.Invoices) }
            MoneyTile("🛒", "Avg. order", data.averageOrderCents, "this month", colors.accent2, Modifier.weight(1f)) { onOpenTab(Tab.Orders) }
        }

        SectionCard("Revenue · last 30 days", Modifier.enterUp(3)) {
            AreaChart(data.revenueByDay)
            Text("Touch and drag the chart to see each day", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, modifier = Modifier.padding(top = 6.dp))
        }

        SectionCard("Order pipeline", Modifier.enterUp(4), action = "View orders", onAction = { onOpenTab(Tab.Orders) }) {
            val total = data.statusBreakdown.sumOf { it.count }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(
                    slices = data.statusBreakdown.map { Slice(it.status.label, it.count.toFloat(), it.status.color()) },
                    size = 130.dp,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedCounter(total, MaterialTheme.typography.headlineSmall, colors.textPrimary)
                        Text("open", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.statusBreakdown.forEach { s ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(s.status.color()))
                            Spacer(Modifier.width(8.dp))
                            Text("${s.status.emoji()} ${s.status.label}", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text("${s.count}", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        }
                    }
                }
            }
        }

        if (data.topProducts.isNotEmpty()) {
            SectionCard("Best sellers · 30 days", Modifier.enterUp(5), action = "Inventory", onAction = { onOpenTab(Tab.Inventory) }) {
                val max = data.topProducts.maxOf { it.valueCents }.coerceAtLeast(1)
                data.topProducts.forEachIndexed { i, p ->
                    Box(Modifier.bounceClick { nav.push(Route.ProductDetail(p.id)) }) {
                        RankBar(p.emoji, p.name, p.detail, moneyShort(p.valueCents), p.valueCents / max.toFloat(), index = i)
                    }
                }
            }
        }

        if (data.topCustomers.isNotEmpty()) {
            SectionCard("Top clients · 30 days", Modifier.enterUp(6), action = "All clients", onAction = { onOpenTab(Tab.Customers) }) {
                val max = data.topCustomers.maxOf { it.valueCents }.coerceAtLeast(1)
                data.topCustomers.forEachIndexed { i, c ->
                    Box(Modifier.bounceClick { nav.push(Route.CustomerDetail(c.id)) }) {
                        RankBar(c.emoji, c.name, c.detail, moneyShort(c.valueCents), c.valueCents / max.toFloat(), color = colors.accent3, index = i)
                    }
                }
            }
        }

        if (data.lowStock.isNotEmpty()) {
            SectionCard("Reorder soon", Modifier.enterUp(7), action = "See all", onAction = { onOpenTab(Tab.Inventory) }) {
                data.lowStock.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).bounceClick { nav.push(Route.ProductDetail(p.id)) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EmojiTile(p.emoji, size = 40.dp, tint = Feedback.Warning)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(5.dp))
                            ProgressBar(
                                p.stock / (p.reorderLevel * 2f).coerceAtLeast(1f),
                                height = 6.dp,
                                brush = Brush.horizontalGradient(listOf(Feedback.Error, Feedback.Warning)),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${p.stock} left", style = MaterialTheme.typography.labelLarge, color = Feedback.Error)
                            Text("min ${p.reorderLevel}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                        }
                    }
                }
            }
        }

        SectionCard("Live activity", Modifier.enterUp(8), action = "See all", onAction = { nav.push(Route.Activity) }) {
            data.recentActivity.forEach { ActivityRow(it) }
        }
        Spacer(Modifier.height(BottomBarSpace))
    }
}

@Composable
private fun RevenueHero(data: DashboardDto, modifier: Modifier) {
    val colors = LocalTradeFlowColors.current
    val prev = data.revenuePrevMonthCents
    val change = if (prev > 0) ((data.revenueMonthCents - prev) * 1000 / prev).toInt() else 0 // tenths of a percent
    val up = change >= 0
    Box(
        modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = colors.accent, spotColor = colors.accent)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.vividGradient),
    ) {
        Text("📈", fontSize = 92.sp, modifier = Modifier.align(Alignment.TopEnd).graphicsLayer { alpha = 0.16f; translationX = 30f; translationY = -16f; rotationZ = -8f })
        Column(Modifier.padding(22.dp)) {
            Text("REVENUE THIS MONTH", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(6.dp))
            MoneyCounter(data.revenueMonthCents, MaterialTheme.typography.displaySmall, Color.White)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArrowGlyph(Color.White, size = 12.dp, up = up)
                    Spacer(Modifier.width(4.dp))
                    val abs = if (change < 0) -change else change
                    Text("${abs / 10}.${abs % 10}%", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
                Spacer(Modifier.width(10.dp))
                Text("vs same period last month (${moneyShort(prev)})", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun KpiTile(emoji: String, title: String, value: Int, subtitle: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    GlassPanel(modifier.bounceClick(onClick = onClick), elevation = 10.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 16.sp) }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = colors.textMuted, maxLines = 1)
            }
            Spacer(Modifier.height(10.dp))
            AnimatedCounter(value, MaterialTheme.typography.headlineMedium, colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
        }
    }
}

@Composable
private fun MoneyTile(emoji: String, title: String, cents: Long, subtitle: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    GlassPanel(modifier.bounceClick(onClick = onClick), elevation = 10.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 16.sp) }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = colors.textMuted, maxLines = 1)
            }
            Spacer(Modifier.height(10.dp))
            MoneyCounter(cents, MaterialTheme.typography.titleLarge, colors.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
        }
    }
}

/** One audit-log line; tap to open what it's about. */
@Composable
fun ActivityRow(activity: ActivityDto, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    val nav = LocalNavigator.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .bounceClick {
                when (activity.entity) {
                    "order" -> nav.push(Route.OrderDetail(activity.entityId))
                    "product" -> nav.push(Route.ProductDetail(activity.entityId))
                    "customer" -> nav.push(Route.CustomerDetail(activity.entityId))
                    "invoice" -> nav.push(Route.InvoiceDetail(activity.entityId))
                }
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(colors.surfaceHigh), contentAlignment = Alignment.Center) {
            Text(activity.type.emoji(), fontSize = 16.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(activity.message, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(timeAgo(activity.createdAt), style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
        }
    }
}
