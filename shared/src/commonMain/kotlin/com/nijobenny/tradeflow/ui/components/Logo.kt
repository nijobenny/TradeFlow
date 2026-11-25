package com.nijobenny.tradeflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * The TradeFlow mark: a stack of three boxes with a rising "flow" arrow on an graphite squircle with an amber glow.
 * [progress] (0..1) draws the arrow in, for the splash animation.
 */
@Composable
fun TradeFlowLogo(modifier: Modifier = Modifier, progress: Float = 1f, showBackground: Boolean = true) {
    Canvas(modifier) {
        val s = size.minDimension
        if (showBackground) {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFF2E2B29), Color(0xFF0C0C0E)), start = Offset.Zero, end = Offset(s, s)),
                cornerRadius = CornerRadius(s * 0.28f),
                size = Size(s, s),
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFD97706).copy(alpha = 0.45f), Color.Transparent), center = Offset(s * 0.9f, s * 0.1f), radius = s * 0.7f),
                radius = s * 0.7f,
                center = Offset(s * 0.9f, s * 0.1f),
            )
            drawRoundRect(Color.White.copy(alpha = 0.2f), cornerRadius = CornerRadius(s * 0.28f), size = Size(s, s), style = Stroke(width = s * 0.015f))
        }
        // Three stacked crates.
        val crate = Color(0xFFF5F5F4)
        val r = CornerRadius(s * 0.04f)
        drawRoundRect(crate, topLeft = Offset(s * 0.2f, s * 0.62f), size = Size(s * 0.26f, s * 0.22f), cornerRadius = r)
        drawRoundRect(Color(0xFFD6D3D1), topLeft = Offset(s * 0.5f, s * 0.62f), size = Size(s * 0.26f, s * 0.22f), cornerRadius = r)
        drawRoundRect(Color(0xFFA8A29E), topLeft = Offset(s * 0.35f, s * 0.38f), size = Size(s * 0.26f, s * 0.22f), cornerRadius = r)
        // Rising flow arrow.
        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            val start = Offset(s * 0.14f, s * 0.34f)
            val end = Offset(s * 0.86f, s * 0.1f)
            val tip = Offset(start.x + (end.x - start.x) * p, start.y + (end.y - start.y) * p)
            drawLine(Color(0xFFF59E0B), start, tip, strokeWidth = s * 0.06f, cap = StrokeCap.Round)
            if (p > 0.85f) {
                val head = Path().apply {
                    moveTo(end.x - s * 0.15f, end.y - s * 0.015f)
                    lineTo(end.x, end.y)
                    lineTo(end.x - s * 0.07f, end.y + s * 0.135f)
                }
                drawPath(head, Color(0xFFF59E0B), style = Stroke(width = s * 0.06f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
