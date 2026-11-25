package com.nijobenny.tradeflow.ui.more

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.Permission
import com.nijobenny.tradeflow.core.Permissions
import com.nijobenny.tradeflow.core.Role
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.components.AutoThemeGlyph
import com.nijobenny.tradeflow.ui.components.Avatar
import com.nijobenny.tradeflow.ui.components.CheckGlyph
import com.nijobenny.tradeflow.ui.components.ChevronGlyph
import com.nijobenny.tradeflow.ui.components.ConfirmDialog
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.LiveBadge
import com.nijobenny.tradeflow.ui.components.LoadView
import com.nijobenny.tradeflow.ui.components.LogoutGlyph
import com.nijobenny.tradeflow.ui.components.MoonGlyph
import com.nijobenny.tradeflow.ui.components.ScreenHeader
import com.nijobenny.tradeflow.ui.components.SectionCard
import com.nijobenny.tradeflow.ui.components.SoftButton
import com.nijobenny.tradeflow.ui.components.SunGlyph
import com.nijobenny.tradeflow.ui.components.TradeFlowLogo
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.components.enterUp
import com.nijobenny.tradeflow.ui.dashboard.ActivityRow
import com.nijobenny.tradeflow.ui.main.BottomBarSpace
import com.nijobenny.tradeflow.ui.moneyShort
import com.nijobenny.tradeflow.ui.state.rememberLoader
import com.nijobenny.tradeflow.ui.theme.AccentTheme
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import com.nijobenny.tradeflow.ui.theme.ThemeMode

@Composable
fun MoreScreen() {
    val colors = LocalTradeFlowColors.current
    val nav = LocalNavigator.current
    val session = LocalSession.current
    val state by session.state.collectAsStateLifecycle()
    val user = state.user
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("More", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary, modifier = Modifier.padding(top = 12.dp).enterUp(0))
        if (user != null) {
            GlassPanel(Modifier.fillMaxWidth().enterUp(1).bounceClick { nav.push(Route.Settings) }, elevation = 12.dp) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(user.name, size = 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                        Text(user.title, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            user.role.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(colors.horizontalGradient).padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    LiveBadge(state.liveConnected)
                }
            }
        }
        val items = listOf(
            MenuItem("🧾", "Invoices", "Receivables, overdue and payments", Route.Invoices),
            MenuItem("🏆", "Team", "Leaderboard and who's doing what", Route.Team),
            MenuItem("⚡", "Activity log", "Every change, live from the server", Route.Activity),
            MenuItem("🔍", "Search everything", "Products, clients, orders and invoices", Route.Search),
            MenuItem("🎨", "Settings", "Theme, accent colour, server and account", Route.Settings),
        )
        items.forEachIndexed { i, item -> MenuRow(item, Modifier.enterUp(i + 2)) }

        if (user != null) {
            SectionCard("What you can do as ${user.role.label}", Modifier.enterUp(8)) {
                Permission.entries.forEach { p ->
                    val allowed = Permissions.allowed(user.role, p)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(22.dp).clip(CircleShape).background(if (allowed) Feedback.Success else colors.surfaceHigh),
                            contentAlignment = Alignment.Center,
                        ) { if (allowed) CheckGlyph(Color.White, size = 12.dp) }
                        Spacer(Modifier.width(10.dp))
                        Text(permissionLabel(p), style = MaterialTheme.typography.bodyMedium, color = if (allowed) colors.textPrimary else colors.textMuted)
                    }
                }
                Text(
                    "Rules live in the shared core module and are enforced by the Ktor server.",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        Spacer(Modifier.height(BottomBarSpace))
    }
}

private data class MenuItem(val emoji: String, val title: String, val subtitle: String, val route: Route)

@Composable
private fun MenuRow(item: MenuItem, modifier: Modifier) {
    val colors = LocalTradeFlowColors.current
    val nav = LocalNavigator.current
    GlassPanel(modifier.fillMaxWidth().bounceClick { nav.push(item.route) }, shape = RoundedCornerShape(20.dp), elevation = 6.dp) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(colors.accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Text(item.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ChevronGlyph(colors.textMuted, modifier = Modifier.graphicsLayer { rotationZ = 90f })
        }
    }
}

private fun permissionLabel(p: Permission): String = when (p) {
    Permission.CREATE_ORDER -> "Create orders"
    Permission.ADVANCE_ORDER -> "Move orders through fulfilment"
    Permission.CANCEL_ORDER -> "Cancel orders"
    Permission.MANAGE_PRODUCTS -> "Add and edit products"
    Permission.ADJUST_STOCK -> "Adjust stock levels"
    Permission.MANAGE_CUSTOMERS -> "Onboard and edit clients"
    Permission.RECORD_PAYMENT -> "Record payments"
}

@Composable
fun TeamScreen() {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val colors = LocalTradeFlowColors.current
    val loader = rememberLoader { api.team() }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader("Team", "Sales this month and activity this week", onBack = { nav.pop() })
        LoadView(loader) { team ->
            val top = team.maxOfOrNull { it.revenueThisMonthCents }?.coerceAtLeast(1L) ?: 1L
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(team, key = { _, m -> m.user.id }) { i, m ->
                    val medal = when (i) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> null
                    }
                    var started by remember { mutableStateOf(false) }
                    androidx.compose.runtime.LaunchedEffect(Unit) { started = true }
                    val fraction by animateFloatAsState(
                        if (started) (m.revenueThisMonthCents / top.toFloat()).coerceAtLeast(0.02f) else 0f,
                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessVeryLow),
                        label = "team$i",
                    )
                    GlassPanel(Modifier.fillMaxWidth().enterUp(i), shape = RoundedCornerShape(20.dp), elevation = if (i == 0) 16.dp else 6.dp) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box {
                                    Avatar(m.user.name, size = 50.dp)
                                    if (medal != null) Text(medal, fontSize = 18.sp, modifier = Modifier.align(Alignment.BottomEnd))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(m.user.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text("${m.user.title} · ${m.user.role.label}", style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(moneyShort(m.revenueThisMonthCents), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text("${m.ordersThisMonth} orders", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                                }
                            }
                            if (m.user.role == Role.SALES || m.user.role == Role.ADMIN || m.revenueThisMonthCents > 0) {
                                Spacer(Modifier.height(10.dp))
                                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(colors.surfaceHigh)) {
                                    Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(50)).background(colors.horizontalGradient))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("⚡ ${m.actionsThisWeek} actions this week", style = MaterialTheme.typography.labelMedium, color = colors.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityScreen() {
    val api = LocalApi.current
    val nav = LocalNavigator.current
    val session = LocalSession.current
    val state by session.state.collectAsStateLifecycle()
    val loader = rememberLoader { api.activity(100) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        ScreenHeader("Activity log", "Audit trail of every change", onBack = { nav.pop() }) { LiveBadge(state.liveConnected) }
        LoadView(loader) { list ->
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                itemsIndexed(list, key = { _, a -> a.id }) { _, a ->
                    ActivityRow(a, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val colors = LocalTradeFlowColors.current
    val nav = LocalNavigator.current
    val session = LocalSession.current
    val state by session.state.collectAsStateLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            ScreenHeader("Settings", onBack = { nav.pop() })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Spacer(Modifier.height(4.dp))
                SectionCard("Appearance", Modifier.enterUp(0)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ThemeCard(mode, state.themeMode == mode, Modifier.weight(1f)) { session.setThemeMode(mode) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Accent", style = MaterialTheme.typography.labelLarge, color = colors.textMuted)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        AccentTheme.entries.forEach { accent ->
                            AccentSwatch(accent, state.accent == accent) { session.setAccent(accent) }
                        }
                    }
                }
                SectionCard("Server", Modifier.enterUp(1)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(state.serverUrl, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Ktor REST API + WebSocket live feed", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                        }
                        LiveBadge(state.liveConnected)
                    }
                    state.company?.let {
                        Spacer(Modifier.height(10.dp))
                        Text("Workspace: ${it.name}, ${it.city}", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                    }
                }
                GlassPanel(Modifier.fillMaxWidth().enterUp(2), elevation = 8.dp) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        TradeFlowLogo(Modifier.size(48.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("TradeFlow 1.0", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text("Ktor · PostgreSQL · Compose Multiplatform", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                            Text("Made by Nijo Benny", style = MaterialTheme.typography.labelMedium, color = colors.accent)
                        }
                    }
                }
                SoftButton("Sign out", onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth(), leading = { LogoutGlyph(Feedback.Error, size = 18.dp) })
                Spacer(Modifier.height(30.dp))
            }
        }
        ConfirmDialog(
            visible = confirmLogout,
            emoji = "👋",
            title = "Sign out?",
            message = "You can sign in again with any demo role.",
            confirmText = "Sign out",
            destructive = true,
            onConfirm = {
                confirmLogout = false
                session.logout()
            },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
private fun ThemeCard(mode: ThemeMode, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.55f), label = "themeLift")
    Column(
        modifier
            .graphicsLayer { scaleX = 1f + 0.04f * lift; scaleY = 1f + 0.04f * lift }
            .shadow((4 + 10 * lift).dp, RoundedCornerShape(20.dp), ambientColor = colors.accent, spotColor = colors.accent)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.glass)
            .border(2.dp, if (selected) colors.accent else colors.glassBorder, RoundedCornerShape(20.dp))
            .bounceClick(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when (mode) {
                        ThemeMode.System -> Brush.horizontalGradient(0.5f to Color(0xFFF7F5F0), 0.5f to Color(0xFF0C0C0E))
                        ThemeMode.Light -> SolidColor(Color(0xFFF7F5F0))
                        ThemeMode.Dark -> SolidColor(Color(0xFF0C0C0E))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (mode) {
                ThemeMode.Light -> SunGlyph(Color(0xFFF59E0B))
                ThemeMode.Dark -> MoonGlyph(Color(0xFFA78BFA))
                ThemeMode.System -> AutoThemeGlyph(colors.accent)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(mode.name, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
    }
}

@Composable
private fun AccentSwatch(accent: AccentTheme, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    val ring by animateDpAsState(if (selected) 3.dp else 0.dp, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "ring")
    val check by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.5f), label = "swatchCheck")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.bounceClick(onClick = onClick)) {
        Box(
            Modifier
                .size(52.dp)
                .border(ring, if (selected) colors.textPrimary else Color.Transparent, CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(accent.tertiary), Color(accent.primary), Color(accent.secondary)))),
            contentAlignment = Alignment.Center,
        ) {
            CheckGlyph(Color.White, size = 18.dp, modifier = Modifier.graphicsLayer { scaleX = check; scaleY = check; alpha = check })
        }
        Spacer(Modifier.height(6.dp))
        Text(accent.title, style = MaterialTheme.typography.labelMedium, color = if (selected) colors.textPrimary else colors.textMuted)
    }
}
