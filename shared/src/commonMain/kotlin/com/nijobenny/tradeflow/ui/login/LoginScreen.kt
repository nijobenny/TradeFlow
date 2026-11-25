package com.nijobenny.tradeflow.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.core.HealthDto
import com.nijobenny.tradeflow.core.Role
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.components.AuroraBackground
import com.nijobenny.tradeflow.ui.components.Avatar
import com.nijobenny.tradeflow.ui.components.ChevronGlyph
import com.nijobenny.tradeflow.ui.components.FormField
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.GradientButton
import com.nijobenny.tradeflow.ui.components.GradientText
import com.nijobenny.tradeflow.ui.components.ServerGlyph
import com.nijobenny.tradeflow.ui.components.SoftButton
import com.nijobenny.tradeflow.ui.components.TradeFlowLogo
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.components.floating
import com.nijobenny.tradeflow.ui.components.shake
import com.nijobenny.tradeflow.ui.state.readable
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import kotlinx.coroutines.launch

private data class DemoUser(val name: String, val email: String, val role: Role, val title: String)

private val demoUsers = listOf(
    DemoUser("Maya Visser", "maya@tradeflow.dev", Role.ADMIN, "Full access"),
    DemoUser("Arjun Menon", "arjun@tradeflow.dev", Role.SALES, "Orders & customers"),
    DemoUser("Lars de Boer", "lars@tradeflow.dev", Role.WAREHOUSE, "Stock & shipping"),
    DemoUser("Sofie Janssen", "sofie@tradeflow.dev", Role.FINANCE, "Invoices & payments"),
)

@Composable
fun LoginScreen() {
    val session = LocalSession.current
    val colors = LocalTradeFlowColors.current
    val state by session.state.collectAsState()
    val scope = rememberCoroutineScope()

    var server by remember { mutableStateOf(state.serverUrl) }
    var email by remember { mutableStateOf("maya@tradeflow.dev") }
    var password by remember { mutableStateOf("demo1234") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var shakes by remember { mutableStateOf(0) }
    var showServer by remember { mutableStateOf(false) }
    var health by remember { mutableStateOf<HealthDto?>(null) }
    var testing by remember { mutableStateOf(false) }
    var healthError by remember { mutableStateOf<String?>(null) }

    fun signIn() {
        if (loading) return
        loading = true
        error = null
        scope.launch {
            try {
                session.login(server, email, password)
            } catch (e: Throwable) {
                error = e.readable()
                shakes++
                if (e.readable().startsWith("Can't reach")) showServer = true
            } finally {
                loading = false
            }
        }
    }

    fun testConnection() {
        testing = true
        health = null
        healthError = null
        scope.launch {
            try {
                health = session.testServer(server)
            } catch (e: Throwable) {
                healthError = e.readable()
            } finally {
                testing = false
            }
        }
    }

    AuroraBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(36.dp))
            TradeFlowLogo(Modifier.size(92.dp).floating().enterUp(0))
            Spacer(Modifier.height(18.dp))
            GradientText("TradeFlow", MaterialTheme.typography.displaySmall, Modifier.enterUp(1))
            Text(
                "Sign in to your wholesale workspace",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.enterUp(2),
            )
            Spacer(Modifier.height(24.dp))

            GlassPanel(Modifier.fillMaxWidth().enterUp(3).shake(shakes), shape = RoundedCornerShape(28.dp), elevation = 20.dp) {
                Column(Modifier.padding(20.dp)) {
                    FormField(
                        "Work email", email, { email = it; error = null },
                        keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None,
                    )
                    Spacer(Modifier.height(12.dp))
                    FormField("Password", password, { password = it; error = null }, password = true, capitalization = KeyboardCapitalization.None)
                    AnimatedVisibility(visible = error != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Text(
                            error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Feedback.Error,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    GradientButton(if (loading) "Signing in…" else "Sign in", onClick = ::signIn, enabled = !loading, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("TRY A DEMO ROLE", style = MaterialTheme.typography.labelSmall, color = colors.textMuted, modifier = Modifier.enterUp(4))
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).enterUp(5),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                demoUsers.forEach { user ->
                    val selected = email == user.email
                    val border by animateColorAsState(if (selected) colors.accent else Color.Transparent, tween(250), label = "demoBorder")
                    Column(
                        Modifier
                            .width(128.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.glass)
                            .border(2.dp, border, RoundedCornerShape(20.dp))
                            .bounceClick {
                                email = user.email
                                password = "demo1234"
                                error = null
                            }
                            .padding(14.dp),
                    ) {
                        Avatar(user.name, size = 38.dp)
                        Spacer(Modifier.height(8.dp))
                        Text(user.role.label, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Text(user.title, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 2)
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            // Server address (collapsible).
            GlassPanel(Modifier.fillMaxWidth().enterUp(6), shape = RoundedCornerShape(22.dp), elevation = 6.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth().bounceClick { showServer = !showServer }, verticalAlignment = Alignment.CenterVertically) {
                        ServerGlyph(colors.accent, size = 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Server", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(server, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1)
                        }
                        ChevronGlyph(colors.textMuted, down = !showServer)
                    }
                    AnimatedVisibility(visible = showServer, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column(Modifier.padding(top = 14.dp)) {
                            FormField(
                                "Ktor API address", server, { server = it; health = null; healthError = null },
                                keyboardType = KeyboardType.Uri, capitalization = KeyboardCapitalization.None,
                                placeholder = "http://192.168.1.20:8080",
                            )
                            Spacer(Modifier.height(10.dp))
                            SoftButton(if (testing) "Testing…" else "Test connection", onClick = ::testConnection, modifier = Modifier.fillMaxWidth())
                            health?.let { h ->
                                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(Feedback.Success))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Connected · API ${h.version} · ${h.database}", style = MaterialTheme.typography.bodyMedium, color = Feedback.Success)
                                }
                            }
                            healthError?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = Feedback.Error, modifier = Modifier.padding(top = 10.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Phone on USB: run  adb reverse tcp:8080 tcp:8080  and use http://127.0.0.1:8080. " +
                                    "On Wi-Fi: use the Network address the server prints when it starts.",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textMuted,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}
