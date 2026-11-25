package com.nijobenny.tradeflow.ui.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.CustomerDto
import com.nijobenny.tradeflow.core.CustomerInput
import com.nijobenny.tradeflow.core.CustomerTier
import com.nijobenny.tradeflow.core.Validation
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.LocalToast
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.colors
import com.nijobenny.tradeflow.ui.components.Chip
import com.nijobenny.tradeflow.ui.components.FormField
import com.nijobenny.tradeflow.ui.components.GradientButton
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.shake
import com.nijobenny.tradeflow.ui.emoji
import com.nijobenny.tradeflow.ui.state.readable
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import kotlinx.coroutines.launch

/** Onboard a new business client, or edit one when [existing] is given. */
@Composable
fun CustomerFormScreen(existing: CustomerDto?) {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val toast = LocalToast.current
    val session = LocalSession.current
    val colors = LocalTradeFlowColors.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var contact by remember { mutableStateOf(existing?.contactName ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var phone by remember { mutableStateOf(existing?.phone ?: "") }
    var city by remember { mutableStateOf(existing?.city ?: "") }
    var country by remember { mutableStateOf(existing?.country ?: "Netherlands") }
    var tier by remember { mutableStateOf(existing?.tier ?: CustomerTier.BRONZE) }
    var credit by remember { mutableStateOf(((existing?.creditLimitCents ?: 500_000L) / 100).toString()) }
    var terms by remember { mutableStateOf(existing?.paymentTermsDays ?: 30) }
    var error by remember { mutableStateOf<String?>(null) }
    var shakes by remember { mutableStateOf(0) }
    var saving by remember { mutableStateOf(false) }

    fun save() {
        val input = CustomerInput(
            name = name, contactName = contact, email = email, phone = phone, city = city, country = country,
            tier = tier, creditLimitCents = (credit.toLongOrNull() ?: -1L) * 100, paymentTermsDays = terms,
        )
        val problem = Validation.customer(input)
        if (problem != null) {
            error = problem
            shakes++
            return
        }
        saving = true
        scope.launch {
            try {
                val saved = if (existing == null) api.createCustomer(input) else api.updateCustomer(existing.id, input)
                toast.success(if (existing == null) "Welcome aboard, ${saved.name}!" else "Changes saved")
                session.refreshAll()
                nav.replace(Route.CustomerDetail(saved.id))
            } catch (e: Throwable) {
                error = e.readable()
                shakes++
            } finally {
                saving = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader(if (existing == null) "New client" else "Edit client", existing?.name, onBack = { nav.pop() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().shake(shakes),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            FormField("Business name", name, { name = it; error = null }, placeholder = "e.g. Bean There Café", capitalization = KeyboardCapitalization.Words)
            FormField("Contact person", contact, { contact = it; error = null }, placeholder = "Full name", capitalization = KeyboardCapitalization.Words)
            FormField("Email", email, { email = it; error = null }, keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None, placeholder = "orders@business.com")
            FormField("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone, placeholder = "+31 6 1234 5678")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormField("City", city, { city = it }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
                FormField("Country", country, { country = it }, Modifier.weight(1f), capitalization = KeyboardCapitalization.Words)
            }

            Text("Pricing tier", style = MaterialTheme.typography.labelLarge, color = colors.textMuted)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomerTier.entries.forEach { t ->
                    val selected = t == tier
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) Brush.linearGradient(t.colors()) else Brush.linearGradient(listOf(colors.surfaceHigh, colors.surfaceHigh)))
                            .border(2.dp, if (selected) Color.White.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(18.dp))
                            .bounceClick { tier = t }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(t.emoji(), fontSize = 20.sp)
                        Text(t.label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else colors.textPrimary)
                        Text("−${t.discountBp / 100}%", style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White.copy(alpha = 0.85f) else colors.textMuted)
                    }
                }
            }

            FormField("Credit limit", credit, { credit = it.filter(Char::isDigit); error = null }, prefix = "€", keyboardType = KeyboardType.Number)
            Text("Payment terms", style = MaterialTheme.typography.labelLarge, color = colors.textMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(14, 30, 45, 60).forEach { d -> Chip("$d days", terms == d, onClick = { terms = d }, modifier = Modifier.weight(1f)) }
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Feedback.Error) }
            GradientButton(
                if (saving) "Saving…" else if (existing == null) "Add client" else "Save changes",
                onClick = ::save,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}
