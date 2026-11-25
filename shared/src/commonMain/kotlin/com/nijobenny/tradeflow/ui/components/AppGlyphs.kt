package com.nijobenny.tradeflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Business icons drawn on a Canvas so they match the brand and need no icon library.

/** Dashboard: three rising bars. */
@Composable
fun ChartGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.18f
        listOf(0.2f to 0.55f, 0.41f to 0.3f, 0.62f to 0.12f).forEach { (x, top) ->
            drawRoundRect(
                color,
                topLeft = Offset(s * x, s * top + s * 0.05f),
                size = Size(w, s * (0.85f - top)),
                cornerRadius = CornerRadius(w / 3),
                style = if (filled) Fill else Stroke(width = s * 0.08f),
            )
        }
    }
}

/** Orders: a receipt with a zig-zag bottom. */
@Composable
fun ReceiptGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val path = Path().apply {
            moveTo(s * 0.2f, s * 0.1f)
            lineTo(s * 0.8f, s * 0.1f)
            lineTo(s * 0.8f, s * 0.9f)
            lineTo(s * 0.7f, s * 0.82f)
            lineTo(s * 0.6f, s * 0.9f)
            lineTo(s * 0.5f, s * 0.82f)
            lineTo(s * 0.4f, s * 0.9f)
            lineTo(s * 0.3f, s * 0.82f)
            lineTo(s * 0.2f, s * 0.9f)
            close()
        }
        if (filled) drawPath(path, color) else drawPath(path, color, style = Stroke(width = s * 0.08f, join = StrokeJoin.Round))
        val ink = if (filled) Color.White.copy(alpha = 0.9f) else color
        listOf(0.32f, 0.46f, 0.6f).forEach { y ->
            drawLine(ink, Offset(s * 0.32f, s * y), Offset(s * 0.68f, s * y), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        }
    }
}

/** Inventory: a parcel box. */
@Composable
fun BoxGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val body = Path().apply {
            moveTo(s * 0.12f, s * 0.32f)
            lineTo(s * 0.5f, s * 0.14f)
            lineTo(s * 0.88f, s * 0.32f)
            lineTo(s * 0.88f, s * 0.72f)
            lineTo(s * 0.5f, s * 0.9f)
            lineTo(s * 0.12f, s * 0.72f)
            close()
        }
        if (filled) drawPath(body, color) else drawPath(body, color, style = Stroke(width = s * 0.08f, join = StrokeJoin.Round))
        val ink = if (filled) Color.White.copy(alpha = 0.85f) else color
        drawLine(ink, Offset(s * 0.12f, s * 0.32f), Offset(s * 0.5f, s * 0.5f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        drawLine(ink, Offset(s * 0.88f, s * 0.32f), Offset(s * 0.5f, s * 0.5f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
        drawLine(ink, Offset(s * 0.5f, s * 0.5f), Offset(s * 0.5f, s * 0.9f), strokeWidth = s * 0.07f, cap = StrokeCap.Round)
    }
}

/** Customers: two people. */
@Composable
fun UsersGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val style = if (filled) Fill else Stroke(width = s * 0.075f)
        drawCircle(color.copy(alpha = if (filled) 0.6f else 1f), radius = s * 0.12f, center = Offset(s * 0.68f, s * 0.32f), style = style)
        drawArc(color.copy(alpha = if (filled) 0.6f else 1f), 180f, 180f, useCenter = filled, topLeft = Offset(s * 0.48f, s * 0.5f), size = Size(s * 0.42f, s * 0.36f), style = style)
        drawCircle(color, radius = s * 0.14f, center = Offset(s * 0.36f, s * 0.36f), style = style)
        drawArc(color, 180f, 180f, useCenter = filled, topLeft = Offset(s * 0.1f, s * 0.56f), size = Size(s * 0.52f, s * 0.42f), style = style)
    }
}

/** More: four rounded squares. */
@Composable
fun GridGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val cell = s * 0.3f
        listOf(0.14f to 0.14f, 0.56f to 0.14f, 0.14f to 0.56f, 0.56f to 0.56f).forEachIndexed { i, (x, y) ->
            drawRoundRect(
                if (filled && i == 1) color.copy(alpha = 0.55f) else color,
                topLeft = Offset(s * x, s * y),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(cell / 3.5f),
                style = if (filled) Fill else Stroke(width = s * 0.075f),
            )
        }
    }
}

@Composable
fun TruckGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(color, topLeft = Offset(s * 0.06f, s * 0.26f), size = Size(s * 0.54f, s * 0.42f), cornerRadius = CornerRadius(s * 0.06f))
        val cab = Path().apply {
            moveTo(s * 0.62f, s * 0.38f); lineTo(s * 0.8f, s * 0.38f); lineTo(s * 0.94f, s * 0.54f)
            lineTo(s * 0.94f, s * 0.68f); lineTo(s * 0.62f, s * 0.68f); close()
        }
        drawPath(cab, color)
        drawCircle(color, radius = s * 0.09f, center = Offset(s * 0.26f, s * 0.76f))
        drawCircle(color, radius = s * 0.09f, center = Offset(s * 0.78f, s * 0.76f))
    }
}

@Composable
fun MinusGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawLine(color, Offset(s * 0.18f, s * 0.5f), Offset(s * 0.82f, s * 0.5f), strokeWidth = s * 0.12f, cap = StrokeCap.Round)
    }
}

@Composable
fun EditGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val pencil = Path().apply {
            moveTo(s * 0.18f, s * 0.82f); lineTo(s * 0.22f, s * 0.62f); lineTo(s * 0.64f, s * 0.2f)
            lineTo(s * 0.8f, s * 0.36f); lineTo(s * 0.38f, s * 0.78f); close()
        }
        drawPath(pencil, color, style = Stroke(width = s * 0.08f, join = StrokeJoin.Round))
        drawLine(color, Offset(s * 0.56f, s * 0.28f), Offset(s * 0.72f, s * 0.44f), strokeWidth = s * 0.08f)
    }
}

@Composable
fun PhoneGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(color, topLeft = Offset(s * 0.28f, s * 0.08f), size = Size(s * 0.44f, s * 0.84f), cornerRadius = CornerRadius(s * 0.1f), style = Stroke(width = s * 0.08f))
        drawLine(color, Offset(s * 0.44f, s * 0.78f), Offset(s * 0.56f, s * 0.78f), strokeWidth = s * 0.08f, cap = StrokeCap.Round)
    }
}

@Composable
fun MailGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        drawRoundRect(color, topLeft = Offset(s * 0.1f, s * 0.22f), size = Size(s * 0.8f, s * 0.56f), cornerRadius = CornerRadius(s * 0.08f), style = Stroke(width = s * 0.08f))
        val flap = Path().apply { moveTo(s * 0.14f, s * 0.28f); lineTo(s * 0.5f, s * 0.54f); lineTo(s * 0.86f, s * 0.28f) }
        drawPath(flap, color, style = Stroke(width = s * 0.08f, join = StrokeJoin.Round, cap = StrokeCap.Round))
    }
}

@Composable
fun LogoutGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.08f
        val door = Path().apply { moveTo(s * 0.5f, s * 0.16f); lineTo(s * 0.18f, s * 0.16f); lineTo(s * 0.18f, s * 0.84f); lineTo(s * 0.5f, s * 0.84f) }
        drawPath(door, color, style = Stroke(width = w, join = StrokeJoin.Round, cap = StrokeCap.Round))
        drawLine(color, Offset(s * 0.4f, s * 0.5f), Offset(s * 0.86f, s * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.86f, s * 0.5f), Offset(s * 0.7f, s * 0.34f), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.86f, s * 0.5f), Offset(s * 0.7f, s * 0.66f), strokeWidth = w, cap = StrokeCap.Round)
    }
}

@Composable
fun ServerGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        listOf(0.14f, 0.54f).forEach { y ->
            drawRoundRect(color, topLeft = Offset(s * 0.12f, s * y), size = Size(s * 0.76f, s * 0.32f), cornerRadius = CornerRadius(s * 0.08f), style = Stroke(width = s * 0.08f))
            drawCircle(color, radius = s * 0.05f, center = Offset(s * 0.28f, s * (y + 0.16f)))
        }
    }
}

@Composable
fun ArrowGlyph(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp, up: Boolean = true) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val w = s * 0.13f
        val tip = if (up) s * 0.16f else s * 0.84f
        val tail = if (up) s * 0.84f else s * 0.16f
        drawLine(color, Offset(s * 0.5f, tip), Offset(s * 0.5f, tail), strokeWidth = w, cap = StrokeCap.Round)
        val wing = if (up) s * 0.4f else s * 0.6f
        drawLine(color, Offset(s * 0.5f, tip), Offset(s * 0.22f, wing), strokeWidth = w, cap = StrokeCap.Round)
        drawLine(color, Offset(s * 0.5f, tip), Offset(s * 0.78f, wing), strokeWidth = w, cap = StrokeCap.Round)
    }
}
