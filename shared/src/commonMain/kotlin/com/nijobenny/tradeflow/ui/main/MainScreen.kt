package com.nijobenny.tradeflow.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.ui.components.AuroraBackground
import com.nijobenny.tradeflow.ui.components.BoxGlyph
import com.nijobenny.tradeflow.ui.components.ChartGlyph
import com.nijobenny.tradeflow.ui.components.GlassPanel
import com.nijobenny.tradeflow.ui.components.GridGlyph
import com.nijobenny.tradeflow.ui.components.ReceiptGlyph
import com.nijobenny.tradeflow.ui.components.UsersGlyph
import com.nijobenny.tradeflow.ui.components.bounceClick
import com.nijobenny.tradeflow.ui.customers.CustomersScreen
import com.nijobenny.tradeflow.ui.dashboard.DashboardScreen
import com.nijobenny.tradeflow.ui.inventory.InventoryScreen
import com.nijobenny.tradeflow.ui.more.MoreScreen
import com.nijobenny.tradeflow.ui.orders.OrdersScreen
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors

enum class Tab(val label: String) { Dashboard("Overview"), Orders("Orders"), Inventory("Stock"), Customers("Clients"), More("More") }

/** Space each tab leaves at the bottom so content can scroll above the floating bar. */
val BottomBarSpace = 112.dp

@Composable
fun MainScreen() {
    var tab by rememberSaveable { mutableStateOf(Tab.Dashboard) }
    AuroraBackground {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(320)) + scaleIn(tween(380, easing = FastOutSlowInEasing), initialScale = 0.97f)) togetherWith fadeOut(tween(160))
            },
            label = "tabs",
        ) { current ->
            when (current) {
                Tab.Dashboard -> DashboardScreen(onOpenTab = { tab = it })
                Tab.Orders -> OrdersScreen()
                Tab.Inventory -> InventoryScreen()
                Tab.Customers -> CustomersScreen()
                Tab.More -> MoreScreen()
            }
        }
        BottomBar(selected = tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    GlassPanel(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(26.dp),
        elevation = 24.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(6.dp)) {
            val itemWidth = maxWidth / Tab.entries.size
            val offset by animateDpAsState(itemWidth * selected.ordinal, spring(dampingRatio = 0.7f, stiffness = 380f), label = "tabPill")
            Box(
                Modifier
                    .offset(x = offset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.accent.copy(alpha = if (colors.isDark) 0.22f else 0.13f)),
            )
            Row(Modifier.fillMaxSize()) {
                Tab.entries.forEach { tab ->
                    val isSelected = tab == selected
                    val tint by animateColorAsState(if (isSelected) colors.accent else colors.textMuted, tween(220), label = "tabTint")
                    val scale by animateFloatAsState(if (isSelected) 1.12f else 1f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "tabScale")
                    Column(
                        Modifier.weight(1f).fillMaxHeight().bounceClick { onSelect(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                            when (tab) {
                                Tab.Dashboard -> ChartGlyph(tint, isSelected)
                                Tab.Orders -> ReceiptGlyph(tint, isSelected)
                                Tab.Inventory -> BoxGlyph(tint, isSelected)
                                Tab.Customers -> UsersGlyph(tint, isSelected)
                                Tab.More -> GridGlyph(tint, isSelected)
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(tab.label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1)
                    }
                }
            }
        }
    }
}
