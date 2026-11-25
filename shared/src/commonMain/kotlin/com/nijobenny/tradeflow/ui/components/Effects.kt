package com.nijobenny.tradeflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.ui.theme.Feedback
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Living background: three soft accent-coloured blobs that drift slowly behind the content.
 * Cheap to draw (three radial gradients) and pauses with the composition.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, intensity: Float = 1f, content: @Composable BoxScope.() -> Unit) {
    val colors = LocalTradeFlowColors.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "auroraT")
    val alpha = (if (colors.isDark) 0.2f else 0.12f) * intensity
    Box(modifier.fillMaxSize().background(colors.background)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val a = t * 2 * PI.toFloat()
            fun blob(color: Color, cx: Float, cy: Float, r: Float) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = Offset(cx, cy), radius = r),
                    radius = r,
                    center = Offset(cx, cy),
                )
            }
            blob(colors.accent, w * (0.2f + 0.12f * cos(a)), h * (0.12f + 0.06f * sin(a)), w * 0.85f)
            blob(colors.accent2, w * (0.9f + 0.1f * sin(a * 2)), h * (0.42f + 0.08f * cos(a)), w * 0.7f)
            blob(colors.accent3, w * (0.3f + 0.15f * sin(a)), h * (0.92f + 0.05f * cos(a * 2)), w * 0.8f)
        }
        content()
    }
}

/** Circular progress ring with a gradient sweep that springs to [progress]. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    stroke: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = LocalTradeFlowColors.current
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessVeryLow), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val inset = s / 2
            val arcSize = Size(this.size.width - s, this.size.height - s)
            drawArc(colors.surfaceHigh, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = Stroke(s))
            rotate(-90f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(colors.accent, colors.accent2, colors.accent3, colors.accent)),
                    startAngle = 0f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(s, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Horizontal progress bar with a glossy highlight. */
@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 12.dp, brush: Brush? = null) {
    val colors = LocalTradeFlowColors.current
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow), label = "bar")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceHigh),
    ) {
        if (animated > 0.001f) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(brush ?: colors.horizontalGradient),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(height / 3)
                        .align(Alignment.TopCenter)
                        .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(50)),
                )
            }
        }
    }
}

/** Number that counts up to [value] whenever it changes. */
@Composable
fun AnimatedCounter(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier, prefix: String = "", suffix: String = "") {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(900, easing = FastOutSlowInEasing)) }
    Text("$prefix${anim.value.toInt()}$suffix", style = style, color = color, modifier = modifier)
}

/** Gentle breathing scale, for "tap me" elements like the current lesson node. */
fun Modifier.pulse(enabled: Boolean = true, amount: Float = 0.06f, periodMillis: Int = 1400): Modifier = composed {
    if (!enabled) return@composed this
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(1f, 1f + amount, infiniteRepeatable(tween(periodMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulseScale")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Vertical floating motion, used on hero emoji and badges. */
fun Modifier.floating(distance: Dp = 6.dp, periodMillis: Int = 2600): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "float")
    val y by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(periodMillis / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "floatY")
    graphicsLayer { translationY = y * distance.toPx() }
}

/** Shakes horizontally each time [trigger] changes (wrong answer). */
fun Modifier.shake(trigger: Int): Modifier = composed {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (x in listOf(-18f, 16f, -12f, 9f, -5f, 0f)) offset.animateTo(x, tween(45))
    }
    graphicsLayer { translationX = offset.value * density }
}

/** Staggered entrance: fades and slides content up after [index] × 60 ms. */
fun Modifier.enterUp(index: Int = 0): Modifier = composed {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress by animateFloatAsState(
        if (visible) 1f else 0f,
        tween(durationMillis = 520, delayMillis = (index * 60).coerceAtMost(600), easing = FastOutSlowInEasing),
        label = "enter",
    )
    graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 28.dp.toPx()
    }
}

private data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val spin: Float,
    val shape: Int,
)

/**
 * Confetti burst from the top-centre. Fires again whenever [key] changes and is not 0.
 * Drawn on a Canvas so it costs nothing when idle.
 */
@Composable
fun ConfettiBurst(key: Int, modifier: Modifier = Modifier, particleCount: Int = 90) {
    val colors = LocalTradeFlowColors.current
    val palette = listOf(colors.accent, colors.accent2, colors.accent3, Feedback.Gold, Feedback.Success, Color.White)
    val particles = remember(key) {
        val random = Random(key * 31 + 7)
        List(particleCount) {
            Particle(
                angle = (-PI / 2 + (random.nextFloat() - 0.5f) * PI * 1.1).toFloat(),
                speed = 0.55f + random.nextFloat() * 0.75f,
                size = 6f + random.nextFloat() * 9f,
                color = palette[random.nextInt(palette.size)],
                spin = (random.nextFloat() - 0.5f) * 1440f,
                shape = random.nextInt(3),
            )
        }
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        if (key != 0) progress.animateTo(1f, tween(2200, easing = LinearEasing))
    }
    if (key == 0 || progress.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        val origin = Offset(size.width / 2, size.height * 0.28f)
        val gravity = size.height * 1.4f
        particles.forEach { p ->
            val v = p.speed * size.height * 1.1f
            val x = origin.x + cos(p.angle) * v * t
            val y = origin.y + sin(p.angle) * v * t + 0.5f * gravity * t * t
            val alpha = (1f - t).coerceIn(0f, 1f)
            val px = p.size * density / 2
            rotate(p.spin * t, pivot = Offset(x, y)) {
                when (p.shape) {
                    0 -> drawRect(p.color.copy(alpha = alpha), topLeft = Offset(x - px, y - px / 2), size = Size(px * 2, px))
                    1 -> drawCircle(p.color.copy(alpha = alpha), radius = px * 0.7f, center = Offset(x, y))
                    else -> drawLine(p.color.copy(alpha = alpha), Offset(x - px, y), Offset(x + px, y), strokeWidth = px * 0.6f, cap = StrokeCap.Round)
                }
            }
        }
    }
}

/** Text with a gradient fill that slowly shimmers. */
@Composable
fun GradientText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val colors = LocalTradeFlowColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shift by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "shift")
    val brush = Brush.linearGradient(
        listOf(colors.accent, colors.accent3, colors.accent2),
        start = Offset(-200f + 400f * shift, 0f),
        end = Offset(600f + 400f * shift, 200f),
    )
    Text(text, style = style.copy(brush = brush), modifier = modifier)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    val colors = LocalTradeFlowColors.current
    androidx.compose.foundation.layout.Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.textMuted, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = colors.accent, modifier = Modifier.bounceClick(onClick = onAction))
        }
    }
}
