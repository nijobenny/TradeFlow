package com.nijobenny.tradeflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/*
 * Hand-drawn vector glyphs (no icon library needed), so every icon matches the brand
 * and can be animated.
 */

@Composable
fun SearchGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.11f, cap = StrokeCap.Round)
        drawCircle(color, radius = s * 0.30f, center = Offset(s * 0.42f, s * 0.42f), style = stroke)
        drawLine(color, Offset(s * 0.64f, s * 0.64f), Offset(s * 0.88f, s * 0.88f), strokeWidth = s * 0.11f, cap = StrokeCap.Round)
    }
}


@Composable
fun SunGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        drawCircle(color, radius = s * 0.2f, center = c)
        repeat(8) { i ->
            val a = i * kotlin.math.PI / 4
            val dx = cos(a).toFloat()
            val dy = sin(a).toFloat()
            drawLine(color, Offset(c.x + dx * s * 0.32f, c.y + dy * s * 0.32f), Offset(c.x + dx * s * 0.45f, c.y + dy * s * 0.45f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun MoonGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(s / 2, s / 2), s * 0.36f)) }
        val bite = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(s * 0.68f, s * 0.34f), s * 0.30f)) }
        val crescent = Path().apply { op(outer, bite, PathOperation.Difference) }
        drawPath(crescent, color)
    }
}

/** Half-filled circle: "follow system appearance". */
@Composable
fun AutoThemeGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        drawCircle(color, radius = s * 0.34f, center = c, style = Stroke(width = s * 0.08f))
        drawArc(color, startAngle = 90f, sweepAngle = 180f, useCenter = true, topLeft = Offset(c.x - s * 0.34f, c.y - s * 0.34f), size = androidx.compose.ui.geometry.Size(s * 0.68f, s * 0.68f))
    }
}


@Composable
fun BackGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.1f
        drawLine(color, Offset(s * 0.82f, s * 0.5f), Offset(s * 0.2f, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.45f, s * 0.25f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.2f, s * 0.5f), Offset(s * 0.45f, s * 0.75f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun CloseGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.12f
        drawLine(color, Offset(s * 0.2f, s * 0.2f), Offset(s * 0.8f, s * 0.8f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.8f, s * 0.2f), Offset(s * 0.2f, s * 0.8f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun PlusGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.12f
        drawLine(color, Offset(s * 0.5f, s * 0.15f), Offset(s * 0.5f, s * 0.85f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.15f, s * 0.5f), Offset(s * 0.85f, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

/** Chevron pointing up (or down when [down]). */
@Composable
fun ChevronGlyph(color: Color, down: Boolean = false, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.13f
        val tipY = if (down) s * 0.68f else s * 0.32f
        val baseY = if (down) s * 0.36f else s * 0.64f
        drawLine(color, Offset(s * 0.2f, baseY), Offset(s * 0.5f, tipY), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, tipY), Offset(s * 0.8f, baseY), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun GearGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        repeat(8) { i ->
            val a = i * kotlin.math.PI / 4
            drawLine(color, Offset(c.x + cos(a).toFloat() * s * 0.28f, c.y + sin(a).toFloat() * s * 0.28f), Offset(c.x + cos(a).toFloat() * s * 0.44f, c.y + sin(a).toFloat() * s * 0.44f), strokeWidth = s * 0.14f, cap = StrokeCap.Round)
        }
        drawCircle(color, radius = s * 0.24f, center = c, style = Stroke(width = s * 0.13f))
    }
}

@Composable
fun StarGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s * 0.53f)
        val star = Path().apply {
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) s * 0.46f else s * 0.2f
                val a = -kotlin.math.PI / 2 + i * kotlin.math.PI / 5
                val p = Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r)
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
        if (filled) drawPath(star, color)
        else drawPath(star, color, style = Stroke(width = s * 0.08f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}


@Composable
fun HomeGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val house = Path().apply {
            moveTo(s * 0.14f, s * 0.46f)
            lineTo(s * 0.5f, s * 0.15f)
            lineTo(s * 0.86f, s * 0.46f)
            lineTo(s * 0.86f, s * 0.86f)
            lineTo(s * 0.14f, s * 0.86f)
            close()
        }
        if (filled) drawPath(house, color) else drawPath(house, color, style = Stroke(width = s * 0.09f, join = StrokeJoin.Round))
        drawLine(if (filled) Color.White.copy(alpha = 0.9f) else color, Offset(s * 0.42f, s * 0.86f), Offset(s * 0.42f, s * 0.64f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
        drawLine(if (filled) Color.White.copy(alpha = 0.9f) else color, Offset(s * 0.58f, s * 0.86f), Offset(s * 0.58f, s * 0.64f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun BookGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.085f
        val left = Path().apply {
            moveTo(s * 0.5f, s * 0.26f); quadraticTo(s * 0.32f, s * 0.16f, s * 0.1f, s * 0.2f)
            lineTo(s * 0.1f, s * 0.8f); quadraticTo(s * 0.32f, s * 0.76f, s * 0.5f, s * 0.86f); close()
        }
        val right = Path().apply {
            moveTo(s * 0.5f, s * 0.26f); quadraticTo(s * 0.68f, s * 0.16f, s * 0.9f, s * 0.2f)
            lineTo(s * 0.9f, s * 0.8f); quadraticTo(s * 0.68f, s * 0.76f, s * 0.5f, s * 0.86f); close()
        }
        if (filled) {
            drawPath(left, color)
            drawPath(right, color.copy(alpha = 0.75f))
        } else {
            drawPath(left, color, style = Stroke(width = w, join = StrokeJoin.Round))
            drawPath(right, color, style = Stroke(width = w, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun CompassGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s / 2, s / 2)
        drawCircle(color, radius = s * 0.4f, center = c, style = Stroke(width = s * 0.085f))
        val needle = Path().apply {
            moveTo(s * 0.66f, s * 0.34f); lineTo(s * 0.56f, s * 0.56f); lineTo(s * 0.34f, s * 0.66f); lineTo(s * 0.44f, s * 0.44f); close()
        }
        if (filled) drawPath(needle, color) else drawPath(needle, color, style = Stroke(width = s * 0.07f, join = StrokeJoin.Round))
    }
}

@Composable
fun ChatGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val bubble = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(s * 0.1f, s * 0.16f, s * 0.9f, s * 0.7f, s * 0.18f, s * 0.18f))
        }
        val tail = Path().apply {
            moveTo(s * 0.3f, s * 0.66f); lineTo(s * 0.24f, s * 0.88f); lineTo(s * 0.48f, s * 0.68f); close()
        }
        if (filled) {
            drawPath(bubble, color)
            drawPath(tail, color)
            listOf(0.32f, 0.5f, 0.68f).forEach { x -> drawCircle(Color.White.copy(alpha = 0.9f), s * 0.05f, Offset(s * x, s * 0.43f)) }
        } else {
            drawPath(bubble, color, style = Stroke(width = s * 0.085f, join = StrokeJoin.Round))
            drawPath(tail, color)
        }
    }
}

@Composable
fun UserGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = s * 0.085f)
        drawCircle(color, radius = s * 0.17f, center = Offset(s * 0.5f, s * 0.33f), style = style)
        drawArc(color, 180f, 180f, useCenter = filled, topLeft = Offset(s * 0.17f, s * 0.58f), size = androidx.compose.ui.geometry.Size(s * 0.66f, s * 0.5f), style = style)
    }
}

/** Speaker with sound waves; [level] 0..1 animates the outer wave. */
@Composable
fun SpeakerGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp, level: Float = 1f) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val body = Path().apply {
            moveTo(s * 0.12f, s * 0.38f); lineTo(s * 0.28f, s * 0.38f); lineTo(s * 0.48f, s * 0.2f)
            lineTo(s * 0.48f, s * 0.8f); lineTo(s * 0.28f, s * 0.62f); lineTo(s * 0.12f, s * 0.62f); close()
        }
        drawPath(body, color)
        val w = Stroke(width = s * 0.08f, cap = StrokeCap.Round)
        drawArc(color, -45f, 90f, false, topLeft = Offset(s * 0.42f, s * 0.32f), size = androidx.compose.ui.geometry.Size(s * 0.24f, s * 0.36f), style = w)
        drawArc(color.copy(alpha = color.alpha * level), -50f, 100f, false, topLeft = Offset(s * 0.4f, s * 0.16f), size = androidx.compose.ui.geometry.Size(s * 0.5f, s * 0.68f), style = w)
    }
}

@Composable
fun CheckGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.13f
        drawLine(color, Offset(s * 0.18f, s * 0.52f), Offset(s * 0.42f, s * 0.75f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.42f, s * 0.75f), Offset(s * 0.84f, s * 0.28f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun HeartGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val heart = Path().apply {
            moveTo(s * 0.5f, s * 0.84f)
            cubicTo(s * 0.1f, s * 0.58f, s * 0.02f, s * 0.3f, s * 0.24f, s * 0.2f)
            cubicTo(s * 0.38f, s * 0.13f, s * 0.48f, s * 0.22f, s * 0.5f, s * 0.3f)
            cubicTo(s * 0.52f, s * 0.22f, s * 0.62f, s * 0.13f, s * 0.76f, s * 0.2f)
            cubicTo(s * 0.98f, s * 0.3f, s * 0.9f, s * 0.58f, s * 0.5f, s * 0.84f)
            close()
        }
        if (filled) drawPath(heart, color) else drawPath(heart, color, style = Stroke(width = s * 0.08f, join = StrokeJoin.Round))
    }
}

@Composable
fun LockGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawArc(color, 180f, 180f, false, topLeft = Offset(s * 0.3f, s * 0.14f), size = androidx.compose.ui.geometry.Size(s * 0.4f, s * 0.44f), style = Stroke(width = s * 0.1f))
        drawLine(color, Offset(s * 0.3f, s * 0.36f), Offset(s * 0.3f, s * 0.46f), strokeWidth = s * 0.1f)
        drawLine(color, Offset(s * 0.7f, s * 0.36f), Offset(s * 0.7f, s * 0.46f), strokeWidth = s * 0.1f)
        drawRoundRect(color, topLeft = Offset(s * 0.2f, s * 0.44f), size = androidx.compose.ui.geometry.Size(s * 0.6f, s * 0.44f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.1f))
    }
}

/** Flame used for the streak counter. [flicker] 0..1 bends the tip. */
@Composable
fun FlameGlyph(outer: Color, inner: Color, modifier: Modifier = Modifier, size: Dp = 26.dp, flicker: Float = 0f) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val tipX = s * (0.5f + (flicker - 0.5f) * 0.12f)
        fun flame(scale: Float, yOffset: Float) = Path().apply {
            val cx = s * 0.5f
            val bottom = s * 0.94f - yOffset
            val h = s * 0.84f * scale
            val w = s * 0.38f * scale
            moveTo(cx + (tipX - cx) * scale, bottom - h)
            cubicTo(cx + w * 0.4f, bottom - h * 0.62f, cx + w * 1.15f, bottom - h * 0.42f, cx + w, bottom - h * 0.22f)
            cubicTo(cx + w * 0.9f, bottom, cx - w * 0.9f, bottom, cx - w, bottom - h * 0.22f)
            cubicTo(cx - w * 1.1f, bottom - h * 0.48f, cx - w * 0.3f, bottom - h * 0.55f, cx + (tipX - cx) * scale, bottom - h)
            close()
        }
        drawPath(flame(1f, 0f), outer)
        drawPath(flame(0.55f, s * 0.04f), inner)
    }
}

@Composable
fun RefreshGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawArc(color, 40f, 290f, false, topLeft = Offset(s * 0.16f, s * 0.16f), size = androidx.compose.ui.geometry.Size(s * 0.68f, s * 0.68f), style = Stroke(width = s * 0.1f, cap = StrokeCap.Round))
        val head = Path().apply { moveTo(s * 0.86f, s * 0.22f); lineTo(s * 0.88f, s * 0.48f); lineTo(s * 0.62f, s * 0.42f); close() }
        drawPath(head, color)
    }
}

@Composable
fun BoltGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val bolt = Path().apply {
            moveTo(s * 0.58f, s * 0.06f); lineTo(s * 0.2f, s * 0.56f); lineTo(s * 0.46f, s * 0.56f)
            lineTo(s * 0.4f, s * 0.94f); lineTo(s * 0.8f, s * 0.42f); lineTo(s * 0.54f, s * 0.42f); close()
        }
        drawPath(bolt, color)
    }
}
