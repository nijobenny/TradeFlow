package com.nijobenny.tradeflow.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.core.Money
import com.nijobenny.tradeflow.core.PointDto
import com.nijobenny.tradeflow.ui.theme.LocalTradeFlowColors

/**
 * Smooth area chart that draws itself in from left to right. Touch or drag to scrub:
 * a guide line and bubble show the exact value for that day.
 */
@Composable
fun AreaChart(
    points: List<PointDto>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    format: (Long) -> String = { Money.format(it, showCents = false) },
) {
    val colors = LocalTradeFlowColors.current
    if (points.isEmpty()) return
    val reveal = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { reveal.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) }
    var selected by remember(points) { mutableStateOf<Int?>(null) }
    val max = (points.maxOf { it.value }.coerceAtLeast(1)).toFloat()

    Column(modifier.fillMaxWidth()) {
        val index = selected ?: points.lastIndex
        Row(verticalAlignment = Alignment.Bottom) {
            Text(format(points[index].value), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            Spacer(Modifier.width(8.dp))
            Text(
                if (selected == null) "today" else points[index].label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(points) {
                    detectTapGestures(
                        onPress = { offset ->
                            selected = indexAt(offset.x, size.width.toFloat(), points.size)
                            tryAwaitRelease()
                            selected = null
                        },
                    )
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures(
                        onDragEnd = { selected = null },
                        onDragCancel = { selected = null },
                    ) { change, _ ->
                        selected = indexAt(change.position.x, size.width.toFloat(), points.size)
                    }
                },
        ) {
            val w = size.width
            val h = size.height
            val top = 10.dp.toPx()
            val bottom = h - 4.dp.toPx()
            val stepX = if (points.size > 1) w / (points.size - 1) else w
            fun y(v: Long) = bottom - (v / max) * (bottom - top)

            // Grid lines.
            repeat(4) { i ->
                val gy = top + (bottom - top) * i / 3f
                drawLine(colors.textMuted.copy(alpha = 0.12f), Offset(0f, gy), Offset(w, gy), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f)))
            }

            val line = Path()
            points.forEachIndexed { i, p ->
                val x = i * stepX
                val py = y(p.value)
                if (i == 0) {
                    line.moveTo(x, py)
                } else {
                    val px = (i - 1) * stepX
                    val prevY = y(points[i - 1].value)
                    val mid = (px + x) / 2
                    line.cubicTo(mid, prevY, mid, py, x, py)
                }
            }
            val area = Path().apply {
                addPath(line)
                lineTo((points.size - 1) * stepX, bottom)
                lineTo(0f, bottom)
                close()
            }
            clipRect(right = w * reveal.value) {
                drawPath(area, Brush.verticalGradient(listOf(colors.accent.copy(alpha = 0.45f), colors.accent.copy(alpha = 0.02f)), startY = top, endY = bottom))
                drawPath(line, Brush.horizontalGradient(listOf(colors.accent2, colors.accent, colors.accent3)), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }

            val sel = selected
            if (sel != null) {
                val x = sel * stepX
                val py = y(points[sel].value)
                drawLine(colors.textPrimary.copy(alpha = 0.35f), Offset(x, top), Offset(x, bottom), strokeWidth = 1.5.dp.toPx())
                drawCircle(colors.background, radius = 8.dp.toPx(), center = Offset(x, py))
                drawCircle(colors.accent, radius = 5.dp.toPx(), center = Offset(x, py))
            } else if (reveal.value >= 1f) {
                val x = (points.size - 1) * stepX
                val py = y(points.last().value)
                drawCircle(colors.accent.copy(alpha = 0.25f), radius = 10.dp.toPx(), center = Offset(x, py))
                drawCircle(colors.accent, radius = 5.dp.toPx(), center = Offset(x, py))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(0, points.size / 2, points.lastIndex).distinct().forEach { i ->
                Text(points[i].label, style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
            }
        }
    }
}

private fun indexAt(x: Float, width: Float, count: Int): Int {
    if (count <= 1) return 0
    val step = width / (count - 1)
    return (x / step + 0.5f).toInt().coerceIn(0, count - 1)
}

data class Slice(val label: String, val value: Float, val color: Color)

/** Donut chart whose segments sweep in one after another. */
@Composable
fun DonutChart(
    slices: List<Slice>,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    stroke: Dp = 18.dp,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val colors = LocalTradeFlowColors.current
    val total = slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    val sweep = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) { sweep.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arcSize = Size(this.size.width - s, this.size.height - s)
            val topLeft = Offset(s / 2, s / 2)
            drawArc(colors.surfaceHigh, 0f, 360f, false, topLeft, arcSize, style = Stroke(s))
            var start = -90f
            val gap = if (slices.count { it.value > 0 } > 1) 3f else 0f
            slices.forEach { slice ->
                val full = 360f * slice.value / total
                val angle = (full * sweep.value - gap).coerceAtLeast(0f)
                if (angle > 0f) {
                    drawArc(slice.color, start + gap / 2, angle, false, topLeft, arcSize, style = Stroke(s, cap = StrokeCap.Round))
                }
                start += full * sweep.value
            }
        }
        center()
    }
}

/** Horizontal bar with label and value; the bar grows in with a spring. */
@Composable
fun RankBar(
    leading: String,
    title: String,
    subtitle: String,
    value: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color? = null,
    index: Int = 0,
) {
    val colors = LocalTradeFlowColors.current
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animated by animateFloatAsState(
        if (started) fraction.coerceIn(0.02f, 1f) else 0f,
        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessVeryLow),
        label = "rank$index",
    )
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        EmojiTile(leading, size = 40.dp, tint = color)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            }
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)).background(colors.surfaceHigh)) {
                Box(
                    Modifier.fillMaxWidth(animated).fillMaxHeight().clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(color ?: colors.accent, (color ?: colors.accent2).copy(alpha = 0.6f)))),
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1)
        }
    }
}

/** Small vertical bars (e.g. units sold per week) that rise in one after another. */
@Composable
fun MiniBars(points: List<PointDto>, modifier: Modifier = Modifier, height: Dp = 110.dp) {
    val colors = LocalTradeFlowColors.current
    val max = (points.maxOfOrNull { it.value } ?: 1L).coerceAtLeast(1L).toFloat()
    val grow = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { grow.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            points.forEachIndexed { i, p ->
                val fraction = (p.value / max).coerceAtLeast(0.03f) * grow.value
                val last = i == points.lastIndex
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (grow.value >= 1f) Text("${p.value}", style = MaterialTheme.typography.labelMedium, color = if (last) colors.accent else colors.textMuted)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().fillMaxHeight(fraction * 0.8f).clip(RoundedCornerShape(8.dp))
                            .background(
                                if (last) Brush.verticalGradient(listOf(colors.accent2, colors.accent))
                                else Brush.verticalGradient(listOf(colors.accent.copy(alpha = 0.45f), colors.accent.copy(alpha = 0.2f))),
                            ),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            points.forEach { p ->
                Text(p.label, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
