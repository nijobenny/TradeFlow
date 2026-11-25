package com.nijobenny.tradeflow.ui.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.InvoiceDto
import com.nijobenny.tradeflow.core.InvoiceStatus
import com.nijobenny.tradeflow.core.Money
import com.nijobenny.tradeflow.core.PaymentMethod
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.LocalToast
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.can
import com.nijobenny.tradeflow.ui.color
import com.nijobenny.tradeflow.ui.components.BottomSheet
import com.nijobenny.tradeflow.ui.components.Chip
import com.nijobenny.tradeflow.ui.components.ConfettiBurst
import com.nijobenny.tradeflow.ui.components.EmptyView
import com.nijobenny.tradeflow.ui.components.FormField
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.GradientButton
import com.nijobenny.tradeflow.ui.components.InfoRow
import com.nijobenny.tradeflow.ui.components.InvoicePill
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.MoneyCounter
import com.nijobenny.tradeflow.ui.components.ProgressRing
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.SectionCard
import com.nijobenny.tradeflow.ui.components.SoftButton
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.dateLabel
import com.nijobenny.tradeflow.ui.dueLabel
import com.nijobenny.tradeflow.ui.money
import com.nijobenny.tradeflow.ui.moneyShort
import com.nijobenny.tradeflow.ui.state.readable
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import com.nijobenny.tradeflow.ui.timeAgo
import kotlinx.coroutines.launch

@Composable
fun InvoicesScreen() {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val colors = LocalTradeFlowColors.current
    var status by remember { mutableStateOf<InvoiceStatus?>(null) }
    val loader = rememberLoader(status) { api.invoices(status) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            ScreenHeader("Invoices", "Receivables and payments", onBack = { nav.pop() })
            val data = loader.data
            if (data != null) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryTile("Outstanding", data.outstandingCents, colors.accent, Modifier.weight(1f))
                    SummaryTile("Overdue", data.overdueCents, Feedback.Error, Modifier.weight(1f))
                    SummaryTile("Paid · 30d", data.paidThisMonthCents, Feedback.Success, Modifier.weight(1f))
                }
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Chip("All", status == null, onClick = { status = null }) }
            items(InvoiceStatus.entries) { s -> Chip(s.label, status == s, onClick = { status = if (status == s) null else s }) }
        }
        LoadView(loader) { data ->
            if (data.items.isEmpty()) {
                EmptyView("🧾", "No invoices", "Invoices are issued when an order is confirmed.")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(data.items, key = { it.id }) { inv ->
                        InvoiceCard(inv, Modifier.animateItem()) { nav.push(Route.InvoiceDetail(inv.id)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, cents: Long, tint: Color, modifier: Modifier) {
    val colors = LocalTradeFlowColors.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(tint.copy(alpha = 0.13f)).padding(12.dp)) {
        MoneyCounter(cents, MaterialTheme.typography.titleMedium, colors.textPrimary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

@Composable
fun InvoiceCard(inv: InvoiceDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    GlassPanel(modifier.fillMaxWidth().bounceClick(onClick = onClick), shape = RoundedCornerShape(20.dp), elevation = 6.dp) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(inv.paidCents / inv.amountCents.toFloat().coerceAtLeast(1f), size = 46.dp, stroke = 5.dp) {
                Text("🧾", fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${inv.number} · ${inv.orderNumber}", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1)
                Text(inv.customerName, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (inv.status == InvoiceStatus.PAID) "Paid in full" else dueLabel(inv.dueAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (inv.status == InvoiceStatus.OVERDUE) Feedback.Error else colors.textMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(inv.amountCents), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))
                InvoicePill(inv.status)
            }
        }
    }
}

@Composable
fun InvoiceDetailScreen(id: Int) {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val toast = LocalToast.current
    val session = LocalSession.current
    val colors = LocalTradeFlowColors.current
    val sessionState by session.state.collectAsStateLifecycle()
    val scope = rememberCoroutineScope()
    val loader = rememberLoader(id) { api.invoice(id) }
    var showPay by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            val inv = loader.data
            ScreenHeader(inv?.number ?: "Invoice", inv?.customerName, onBack = { nav.pop() }) {
                if (inv != null) InvoicePill(inv.status)
            }
            LoadView(loader) { invoice ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Spacer(Modifier.height(4.dp))
                    val paidFraction = invoice.paidCents / invoice.amountCents.toFloat().coerceAtLeast(1f)
                    Box(
                        Modifier.fillMaxWidth().enterUp(0)
                            .shadow(22.dp, RoundedCornerShape(28.dp), ambientColor = invoice.status.color(), spotColor = invoice.status.color())
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(invoice.status.color(), invoice.status.color().copy(alpha = 0.6f))))
                            .padding(20.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("BALANCE DUE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                                MoneyCounter(invoice.balanceCents, MaterialTheme.typography.displaySmall, Color.White, showCents = true)
                                Text("of ${money(invoice.amountCents)} · ${dueLabel(invoice.dueAt)}", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                            }
                            ProgressRing(paidFraction, size = 80.dp, stroke = 9.dp) {
                                Text("${(paidFraction * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                        }
                    }

                    if (sessionState.user.can(Permission.RECORD_PAYMENT) && invoice.balanceCents > 0 && invoice.status != InvoiceStatus.VOID) {
                        GradientButton("💶  Record payment", onClick = { showPay = true }, modifier = Modifier.fillMaxWidth().enterUp(1))
                    }

                    SectionCard("Details", Modifier.enterUp(2)) {
                        InfoRow("Client", invoice.customerName)
                        InfoRow("Order", invoice.orderNumber)
                        InfoRow("Issued", dateLabel(invoice.issuedAt))
                        InfoRow("Due", dateLabel(invoice.dueAt), valueColor = if (invoice.status == InvoiceStatus.OVERDUE) Feedback.Error else null)
                        InfoRow("Paid so far", money(invoice.paidCents), valueColor = Feedback.Success)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SoftButton("Open order", onClick = { nav.push(Route.OrderDetail(invoice.orderId)) }, modifier = Modifier.weight(1f))
                            SoftButton("Open client", onClick = { nav.push(Route.CustomerDetail(invoice.customerId)) }, modifier = Modifier.weight(1f))
                        }
                    }

                    SectionCard("Payments", Modifier.enterUp(3)) {
                        if (invoice.payments.isEmpty()) {
                            Text("No payments yet.", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                        }
                        invoice.payments.forEach { p ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(36.dp).clip(CircleShape).background(Feedback.Success.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                    Text("💶", fontSize = 16.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.method.label, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text("${p.userName} · ${timeAgo(p.createdAt)}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                                }
                                Text("+" + money(p.amountCents), style = MaterialTheme.typography.titleMedium, color = Feedback.Success)
                            }
                        }
                    }
                    Spacer(Modifier.height(30.dp))
                }
            }
        }

        val invoice = loader.data
        PaymentSheet(
            visible = showPay && invoice != null,
            balanceCents = invoice?.balanceCents ?: 0,
            onDismiss = { showPay = false },
            onSave = { amount, method ->
                scope.launch {
                    try {
                        val updated = api.recordPayment(id, amount, method)
                        loader.set(updated)
                        showPay = false
                        if (updated.status == InvoiceStatus.PAID) {
                            confetti++
                            toast.success("${updated.number} is paid in full 🎉")
                        } else {
                            toast.success("Payment of ${money(amount)} recorded")
                        }
                        session.refreshAll()
                    } catch (e: Throwable) {
                        toast.error(e.readable())
                    }
                }
            },
        )
        ConfettiBurst(confetti)
    }
}

@Composable
private fun PaymentSheet(visible: Boolean, balanceCents: Long, onDismiss: () -> Unit, onSave: (Long, PaymentMethod) -> Unit) {
    val colors = LocalTradeFlowColors.current
    var method by remember { mutableStateOf(PaymentMethod.BANK_TRANSFER) }
    var custom by remember { mutableStateOf("") }
    var full by remember { mutableStateOf(true) }
    val amount = if (full) balanceCents else Money.parse(custom) ?: 0L
    BottomSheet(visible = visible, onDismiss = onDismiss) {
        Text("Record payment", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text("Balance ${money(balanceCents)}", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Full balance", full, onClick = { full = true }, modifier = Modifier.weight(1f))
            Chip("Half", !full && custom == Money.format(balanceCents / 2, symbol = "").replace(",", ""), onClick = {
                full = false
                custom = Money.format(balanceCents / 2, symbol = "").replace(",", "")
            }, modifier = Modifier.weight(1f))
            Chip("Other", !full && custom.isEmpty(), onClick = {
                full = false
                custom = ""
            }, modifier = Modifier.weight(1f))
        }
        if (!full) {
            Spacer(Modifier.height(12.dp))
            FormField("Amount", custom, { custom = it }, prefix = "€", keyboardType = KeyboardType.Decimal, placeholder = "0.00")
        }
        Spacer(Modifier.height(16.dp))
        Text("Method", style = MaterialTheme.typography.labelLarge, color = colors.textMuted)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethod.entries.forEach { m -> Chip(m.label, method == m, onClick = { method = m }) }
        }
        Spacer(Modifier.height(20.dp))
        val valid = amount in 1..balanceCents
        GradientButton(
            if (valid) "Record ${money(amount)}" else "Enter an amount up to ${moneyShort(balanceCents)}",
            onClick = { if (valid) onSave(amount, method) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
