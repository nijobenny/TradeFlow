package com.nijobenny.tradeflow.ui.customers

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.core.CustomerDetailDto
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.PointDto
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.can
import com.nijobenny.tradeflow.ui.colors
import com.nijobenny.tradeflow.ui.components.Avatar
import com.nijobenny.tradeflow.ui.components.EditGlyph
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.GradientButton
import com.nijobenny.tradeflow.ui.components.IconBubble
import com.nijobenny.tradeflow.ui.components.InvoicePill
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.MailGlyph
import com.nijobenny.tradeflow.ui.components.MiniBars
import com.nijobenny.tradeflow.ui.components.MoneyCounter
import com.nijobenny.tradeflow.ui.components.PhoneGlyph
import com.nijobenny.tradeflow.ui.components.ProgressRing
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.SectionCard
import com.nijobenny.tradeflow.ui.components.SoftButton
import com.nijobenny.tradeflow.ui.components.TierBadge
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.dueLabel
import com.nijobenny.tradeflow.ui.money
import com.nijobenny.tradeflow.ui.moneyShort
import com.nijobenny.tradeflow.ui.orders.OrderCard
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors

@Composable
fun CustomerDetailScreen(id: Int) {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val colors = LocalTradeFlowColors.current
    val session = LocalSession.current
    val sessionState by session.state.collectAsStateLifecycle()
    val loader = rememberLoader(id) { api.customer(id) }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        val customer = loader.data?.customer
        ScreenHeader(customer?.name ?: "Client", customer?.let { "${it.city}, ${it.country}" }, onBack = { nav.pop() }) {
            if (customer != null && sessionState.user.can(Permission.MANAGE_CUSTOMERS)) {
                IconBubble(onClick = { nav.push(Route.CustomerForm(customer)) }) { EditGlyph(colors.textPrimary, size = 18.dp) }
            }
        }
        LoadView(loader) { data -> CustomerContent(data, canOrder = sessionState.user.can(Permission.CREATE_ORDER)) }
    }
}

@Composable
private fun CustomerContent(data: CustomerDetailDto, canOrder: Boolean) {
    val colors = LocalTradeFlowColors.current
    val nav = LocalNavigator.current
    val uri = LocalUriHandler.current
    val c = data.customer
    val used = c.outstandingCents / c.creditLimitCents.toFloat().coerceAtLeast(1f)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Spacer(Modifier.height(4.dp))
        // Hero in the tier's colours.
        Box(
            Modifier.fillMaxWidth().enterUp(0)
                .shadow(22.dp, RoundedCornerShape(28.dp), ambientColor = c.tier.colors().first(), spotColor = c.tier.colors().first())
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(c.tier.colors()))
                .padding(20.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(c.name, size = 54.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${c.contactName} · ${c.paymentTermsDays}-day terms", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.88f))
                    }
                    TierBadge(c.tier)
                }
                Spacer(Modifier.height(16.dp))
                Text("LIFETIME SPEND", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                MoneyCounter(c.totalSpentCents, MaterialTheme.typography.displaySmall, Color.White)
                Text("${c.orderCount} orders · ${c.tier.label} pricing saves ${c.tier.discountBp / 100}%", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.88f))
            }
        }

        Row(Modifier.fillMaxWidth().enterUp(1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoftButton("Call", onClick = { runCatching { uri.openUri("tel:${c.phone.filter { it.isDigit() || it == '+' }}") } }, modifier = Modifier.weight(1f), leading = { PhoneGlyph(colors.textPrimary, size = 18.dp) })
            SoftButton("Email", onClick = { runCatching { uri.openUri("mailto:${c.email}") } }, modifier = Modifier.weight(1f), leading = { MailGlyph(colors.textPrimary, size = 18.dp) })
        }
        if (canOrder) {
            GradientButton("New order for ${c.name.substringBefore(' ')}", onClick = { nav.push(Route.NewOrder(c.id)) }, modifier = Modifier.fillMaxWidth().enterUp(2))
        }

        GlassPanel(Modifier.fillMaxWidth().enterUp(3), elevation = 10.dp) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(used, size = 88.dp, stroke = 10.dp) {
                    Text("${(used * 100).toInt().coerceAtMost(999)}%", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Credit used", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text("${money(c.outstandingCents)} of ${money(c.creditLimitCents)}", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${moneyShort(c.availableCreditCents)} available",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (used > 0.85f) Feedback.Warning else Feedback.Success,
                    )
                }
            }
        }

        SectionCard("Spend per month", Modifier.enterUp(4)) {
            MiniBars(data.spendByMonth.map { PointDto(it.label, it.value / 100) })
            Text("Values in euros", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, modifier = Modifier.padding(top = 6.dp))
        }

        if (data.invoices.isNotEmpty()) {
            SectionCard("Invoices", Modifier.enterUp(5)) {
                data.invoices.take(6).forEach { inv ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).bounceClick { nav.push(Route.InvoiceDetail(inv.id)) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(inv.number, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("${money(inv.amountCents)} · ${dueLabel(inv.dueAt)}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                        }
                        InvoicePill(inv.status)
                    }
                }
            }
        }

        Text("Recent orders", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.enterUp(6))
        data.orders.take(15).forEach { o ->
            OrderCard(o) { nav.push(Route.OrderDetail(o.id)) }
        }
        Spacer(Modifier.height(30.dp))
    }
}
