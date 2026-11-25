package com.nijobenny.tradeflow.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nijobenny.tradeflow.ui.components.TradeFlowLogo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Logo springs in, the flow arrow draws itself, data particles orbit, then the name fades up. */
@Composable
fun SplashIntro(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }
    val arrow = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    val transition = rememberInfiniteTransition(label = "splash")
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "spin")

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(350)) }
        launch { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
        delay(300)
        arrow.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        text.animateTo(1f, tween(500))
        delay(550)
        onFinished()
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0C0C0E), Color(0xFF1C1917), Color(0xFF0C0C0E)))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height * 0.44f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFD97706).copy(alpha = 0.35f * alpha.value), Color.Transparent), center = c, radius = size.width * 0.75f),
                radius = size.width * 0.75f,
                center = c,
            )
            // Orbiting "data" dots.
            repeat(10) { i ->
                val a = ((spin + i * 36f) * PI / 180f).toFloat()
                val r = size.width * (0.26f + 0.04f * (i % 3)) * arrow.value
                drawCircle(
                    if (i % 2 == 0) Color(0xFFF59E0B) else Color(0xFFD6D3D1),
                    radius = (3 + i % 3).dp.toPx(),
                    center = Offset(c.x + cos(a) * r, c.y + sin(a) * r),
                    alpha = 0.8f * arrow.value,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            TradeFlowLogo(
                Modifier.size(128.dp).graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                },
                progress = arrow.value,
            )
            Spacer(Modifier.height(26.dp))
            Text(
                "TradeFlow",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                modifier = Modifier.graphicsLayer { this.alpha = text.value; translationY = (1 - text.value) * 30f },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Wholesale ordering, inventory & invoicing",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.65f * text.value),
            )
        }
    }
}
