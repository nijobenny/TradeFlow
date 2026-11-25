package com.nijobenny.tradeflow.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Fixed feedback colours used across every theme. */
object Feedback {
    val Success = Color(0xFF22C55E)
    val Error = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)
    val Gold = Color(0xFFFFC94A)
    val Flame = Color(0xFFFF7A1A)
    val FlameCore = Color(0xFFFFD25A)
}

@Immutable
data class TradeFlowColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundAlt: Color,
    val glass: Color,
    val glassBorder: Color,
    val surfaceHigh: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val shadow: Color,
    val accent: Color,
    val accent2: Color,
    val accent3: Color,
) {
    val gradient: Brush get() = Brush.linearGradient(listOf(accent, accent2))
    val horizontalGradient: Brush get() = Brush.horizontalGradient(listOf(accent, accent2))
    val vividGradient: Brush get() = Brush.linearGradient(listOf(accent3, accent, accent2))
}

private fun base(dark: Boolean, accent: AccentTheme) = if (dark) {
    TradeFlowColors(
        isDark = true,
        background = Color(0xFF0C0C0E),
        backgroundAlt = Color(0xFF161618),
        glass = Color(0xEB18181B),
        glassBorder = Color(0x1FFFFFFF),
        surfaceHigh = Color(0xFF26262A),
        textPrimary = Color(0xFFF5F5F4),
        textMuted = Color(0xFFA8A29E),
        shadow = Color(0xFF000000),
        accent = Color(accent.primary),
        accent2 = Color(accent.secondary),
        accent3 = Color(accent.tertiary),
    )
} else {
    TradeFlowColors(
        isDark = false,
        background = Color(0xFFF7F5F0),
        backgroundAlt = Color(0xFFEFEBE3),
        glass = Color(0xEBFFFFFF),
        glassBorder = Color(0x1A1C1917),
        surfaceHigh = Color(0xFFEEEAE2),
        textPrimary = Color(0xFF1C1917),
        textMuted = Color(0xFF78716C),
        shadow = Color(0xFF292524),
        accent = Color(accent.primary),
        accent2 = Color(accent.secondary),
        accent3 = Color(accent.tertiary),
    )
}

val LocalTradeFlowColors = staticCompositionLocalOf { base(true, AccentTheme.Amber) }


private val LinguaTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-1).sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.3).sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp),
)

/** Every colour animates when the theme or accent changes, so switching is a smooth cross-fade. */
@Composable
fun TradeFlowTheme(darkTheme: Boolean, accent: AccentTheme, content: @Composable () -> Unit) {
    val target = base(darkTheme, accent)
    val spec = tween<Color>(durationMillis = 500)
    val background by animateColorAsState(target.background, spec, label = "bg")
    val backgroundAlt by animateColorAsState(target.backgroundAlt, spec, label = "bgAlt")
    val glass by animateColorAsState(target.glass, spec, label = "glass")
    val glassBorder by animateColorAsState(target.glassBorder, spec, label = "border")
    val surfaceHigh by animateColorAsState(target.surfaceHigh, spec, label = "surfaceHigh")
    val textPrimary by animateColorAsState(target.textPrimary, spec, label = "text")
    val textMuted by animateColorAsState(target.textMuted, spec, label = "muted")
    val accent1 by animateColorAsState(target.accent, spec, label = "accent")
    val accent2 by animateColorAsState(target.accent2, spec, label = "accent2")
    val accent3 by animateColorAsState(target.accent3, spec, label = "accent3")

    val colors = target.copy(
        background = background,
        backgroundAlt = backgroundAlt,
        glass = glass,
        glassBorder = glassBorder,
        surfaceHigh = surfaceHigh,
        textPrimary = textPrimary,
        textMuted = textMuted,
        accent = accent1,
        accent2 = accent2,
        accent3 = accent3,
    )
    val scheme = if (darkTheme) {
        darkColorScheme(primary = accent1, secondary = accent2, tertiary = accent3, background = background, surface = glass, onSurface = textPrimary, onBackground = textPrimary)
    } else {
        lightColorScheme(primary = accent1, secondary = accent2, tertiary = accent3, background = background, surface = glass, onSurface = textPrimary, onBackground = textPrimary)
    }

    CompositionLocalProvider(LocalTradeFlowColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = LinguaTypography, content = content)
    }
}
