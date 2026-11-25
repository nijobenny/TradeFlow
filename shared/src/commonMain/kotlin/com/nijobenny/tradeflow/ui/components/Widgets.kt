package com.nijobenny.tradeflow.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nijobenny.tradeflow.core.CustomerTier
import com.nijobenny.tradeflow.core.InvoiceStatus
import com.nijobenny.tradeflow.core.Money
import com.nijobenny.tradeflow.core.OrderStatus
import com.nijobenny.tradeflow.ui.Toast
import com.nijobenny.tradeflow.ui.ToastKind
import com.nijobenny.tradeflow.ui.avatarColors
import com.nijobenny.tradeflow.ui.color
import com.nijobenny.tradeflow.ui.colors
import com.nijobenny.tradeflow.ui.emoji
import com.nijobenny.tradeflow.ui.initials
import com.nijobenny.tradeflow.ui.state.Load
import com.nijobenny.tradeflow.ui.state.Loader
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import kotlinx.coroutines.delay

/** Stops taps reaching whatever is underneath an overlay. */
fun Modifier.consumeTouches(): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
}

/** Title bar used by pushed screens. */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalTradeFlowColors.current
    Row(modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            IconBubble(onClick = onBack) { BackGlyph(colors.textPrimary, size = 18.dp) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        trailing()
    }
}

/** Renders a [Loader]: shimmer skeleton, friendly error with retry, or the content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> LoadView(
    loader: Loader<T>,
    modifier: Modifier = Modifier,
    skeleton: @Composable () -> Unit = { SkeletonList() },
    content: @Composable (T) -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = loader.refreshing,
        onRefresh = { loader.reload() },
        modifier = modifier.fillMaxSize(),
    ) {
        when (val state = loader.state) {
            Load.Loading -> skeleton()
            is Load.Failed -> ErrorView(state.message, onRetry = { loader.reload() })
            is Load.Ready -> content(state.data)
        }
    }
}

/** Animated shimmer placeholder. */
@Composable
fun Shimmer(modifier: Modifier, shape: RoundedCornerShape = RoundedCornerShape(16.dp)) {
    val colors = LocalTradeFlowColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse), label = "shimmerAlpha")
    Box(modifier.clip(shape).background(colors.surfaceHigh.copy(alpha = alpha)))
}

@Composable
fun SkeletonList(rows: Int = 6, header: Boolean = true) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (header) Shimmer(Modifier.fillMaxWidth().height(120.dp), RoundedCornerShape(24.dp))
        repeat(rows) { Shimmer(Modifier.fillMaxWidth().height(72.dp)) }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    val colors = LocalTradeFlowColors.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(60.dp))
        Text("📡", fontSize = 56.sp, modifier = Modifier.floating())
        Spacer(Modifier.height(12.dp))
        Text("Couldn't load this", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        GradientButton("Try again", onClick = onRetry, modifier = Modifier.widthIn(min = 180.dp))
    }
}

@Composable
fun EmptyView(emoji: String, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    Column(modifier.fillMaxWidth().padding(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 48.sp, modifier = Modifier.floating())
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, textAlign = TextAlign.Center)
    }
}

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier, leading: String? = null) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Text(leading, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
        } else {
            Box(Modifier.size(7.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

@Composable
fun StatusPill(status: OrderStatus, modifier: Modifier = Modifier) = Pill(status.label, status.color(), modifier)

@Composable
fun InvoicePill(status: InvoiceStatus, modifier: Modifier = Modifier) = Pill(status.label, status.color(), modifier)

@Composable
fun TierBadge(tier: CustomerTier, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(tier.colors()))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(tier.emoji(), fontSize = 10.sp)
        Spacer(Modifier.width(4.dp))
        Text(tier.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp, emoji: String? = null) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(Brush.linearGradient(avatarColors(name))),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji != null) {
            Text(emoji, fontSize = (size.value * 0.45f).sp)
        } else {
            Text(initials(name), color = Color.White, fontSize = (size.value * 0.36f).sp, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Emoji tile for products. */
@Composable
fun EmojiTile(emoji: String, modifier: Modifier = Modifier, size: Dp = 48.dp, tint: Color? = null) {
    val colors = LocalTradeFlowColors.current
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background((tint ?: colors.accent).copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size.value * 0.48f).sp) }
}

@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    GlassPanel(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = 6.dp) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            SearchGlyph(colors.textMuted, size = 19.dp)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted, maxLines = 1)
                    inner()
                },
            )
            if (value.isNotEmpty()) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(colors.surfaceHigh).bounceClick { onValueChange("") }, contentAlignment = Alignment.Center) {
                    CloseGlyph(colors.textMuted, size = 11.dp)
                }
            }
        }
    }
}

/** Labelled text input with an optional error message. */
@Composable
fun FormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    password: Boolean = false,
    singleLine: Boolean = true,
    prefix: String? = null,
    error: String? = null,
) {
    val colors = LocalTradeFlowColors.current
    var focused by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = colors.textMuted, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceHigh)
                .border(1.5.dp, if (error != null) Feedback.Error else if (focused) colors.accent else Color.Transparent, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (prefix != null) {
                Text(prefix, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted)
                Spacer(Modifier.width(6.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboardType),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = if (singleLine) 0.dp else 70.dp)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.textMuted.copy(alpha = 0.7f))
                    inner()
                },
            )
        }
        if (error != null) {
            Text(error, style = MaterialTheme.typography.labelMedium, color = Feedback.Error, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}

/** Floating action button: gradient pill with a plus. */
@Composable
fun Fab(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    Row(
        modifier
            .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = colors.accent, spotColor = colors.accent)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.horizontalGradient)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusGlyph(Color.White, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

/** Money that counts up to its value. */
@Composable
fun MoneyCounter(cents: Long, style: TextStyle, color: Color, modifier: Modifier = Modifier, showCents: Boolean = false) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(cents) { anim.animateTo((cents / 100.0).toFloat(), tween(1000, easing = FastOutSlowInEasing)) }
    val shown = if (anim.isRunning) (anim.value * 100).toLong() else cents
    Text(Money.format(shown, showCents = showCents), style = style, color = color, modifier = modifier, maxLines = 1)
}

/** Label / value row used in detail screens. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null, bold: Boolean = false) {
    val colors = LocalTradeFlowColors.current
    Row(modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = valueColor ?: colors.textPrimary,
        )
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    Box(modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(colors.glassBorder))
}

/** − 12 + stepper with a springy number. */
@Composable
fun QuantityStepper(value: Int, onValueChange: (Int) -> Unit, max: Int, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(colors.surfaceHigh).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.glass).bounceClick(enabled = value > 0) { onValueChange(value - 1) },
            contentAlignment = Alignment.Center,
        ) { MinusGlyph(if (value > 0) colors.textPrimary else colors.textMuted, size = 14.dp) }
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith (slideOutVertically { if (up) -it else it } + fadeOut())
            },
            label = "qty",
        ) { q ->
            Text("$q", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
        }
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(colors.accent).bounceClick(enabled = value < max) { onValueChange(value + 1) },
            contentAlignment = Alignment.Center,
        ) { PlusGlyph(Color.White, size = 14.dp) }
    }
}

/** Dimmed backdrop + card that pops in the middle of the screen. */
@Composable
fun ConfirmDialog(
    visible: Boolean,
    emoji: String,
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalTradeFlowColors.current
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).bounceClick(onClick = onDismiss))
        }
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(initialScale = 0.85f) + fadeIn(),
            exit = scaleOut(targetScale = 0.9f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            GlassPanel(Modifier.padding(28.dp), shape = RoundedCornerShape(28.dp), elevation = 30.dp) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(emoji, fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(18.dp))
                    if (destructive) {
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Feedback.Error).bounceClick(onClick = onConfirm).padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text(confirmText, style = MaterialTheme.typography.titleMedium, color = Color.White) }
                    } else {
                        GradientButton(confirmText, onClick = onConfirm, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(10.dp))
                    SoftButton("Not now", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** Bottom sheet that slides up over a dimmed backdrop. */
@Composable
fun BottomSheet(visible: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTradeFlowColors.current
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).bounceClick(onClick = onDismiss))
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it },
            exit = slideOutVertically(tween(260)) { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(30.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(if (colors.isDark) colors.backgroundAlt else Color.White)
                    .consumeTouches()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 44.dp, height = 5.dp).clip(RoundedCornerShape(50)).background(colors.textMuted.copy(alpha = 0.4f)))
                Spacer(Modifier.height(14.dp))
                content()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** Top banner for success, error and live-event messages. Tapping a live event opens it. */
@Composable
fun ToastBanner(toast: Toast?, onDismiss: () -> Unit, onOpen: (Toast) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    var shown by remember { mutableStateOf(toast) }
    if (toast != null) shown = toast
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(if (toast.kind == ToastKind.Live) 4200 else 2800)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically(tween(420, easing = FastOutSlowInEasing)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(300)) { -it } + fadeOut(),
        modifier = modifier,
    ) {
        val t = shown ?: return@AnimatedVisibility
        val (tint, icon) = when (t.kind) {
            ToastKind.Success -> Feedback.Success to "✓"
            ToastKind.Error -> Feedback.Error to "!"
            ToastKind.Info -> colors.accent to "i"
            ToastKind.Live -> colors.accent2 to (t.event?.type?.emoji() ?: "⚡")
        }
        GlassPanel(
            Modifier.statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth().bounceClick { onOpen(t) },
            shape = RoundedCornerShape(22.dp),
            elevation = 24.dp,
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                    Text(icon, color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (t.kind == ToastKind.Live) {
                        Text("LIVE · just now", style = MaterialTheme.typography.labelSmall, color = tint)
                    }
                    Text(t.message, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Pulsing green dot with "Live" — shows the WebSocket is connected. */
@Composable
fun LiveBadge(connected: Boolean, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(0.6f, 1.6f, infiniteRepeatable(tween(1100), RepeatMode.Restart), label = "livePulse")
    val tint = if (connected) Feedback.Success else colors.textMuted
    Row(
        modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            if (connected) {
                Box(Modifier.size(8.dp).graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = (1.6f - pulse) / 1f }.clip(CircleShape).background(tint))
            }
            Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
        }
        Spacer(Modifier.width(4.dp))
        Text(if (connected) "Live" else "Offline", style = MaterialTheme.typography.labelMedium, color = tint)
    }
}

/** A section card with a title row. */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTradeFlowColors.current
    GlassPanel(modifier.fillMaxWidth(), elevation = 10.dp) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                if (action != null) Text(action, style = MaterialTheme.typography.labelLarge, color = colors.accent, modifier = Modifier.bounceClick(onClick = onAction))
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

/** Content area of a pushed screen: background, touch blocking, safe areas. */
@Composable
fun RouteSurface(content: @Composable BoxScope.() -> Unit) {
    AuroraBackground(Modifier.consumeTouches(), intensity = 0.7f) {
        Box(Modifier.fillMaxSize().statusBarsPadding(), content = content)
    }
}
