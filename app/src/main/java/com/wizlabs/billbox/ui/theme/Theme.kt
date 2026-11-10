package com.wizlabs.billbox.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.wizlabs.billbox.model.ThemeMode

@Immutable
data class WizPalette(
    val isDark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    /** Brand yellow, used as a fill. */
    val accent: Color,
    /** Text/icons drawn on [accent]. */
    val onAccent: Color,
    /** Yellow-family colour that stays readable as text/lines on [surface]. */
    val accentInk: Color,
    val accent2: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
)

val DarkPalette = WizPalette(
    isDark = true,
    bg = Color(0xFF0A0A0C), surface = Color(0xFF141418), surface2 = Color(0xFF1E1E24), line = Color(0xFF2A2A33),
    text = Color(0xFFF2F2F7), muted = Color(0xFF8E8E99),
    accent = Color(0xFFFFD60A), onAccent = Color(0xFF1C1400), accentInk = Color(0xFFFFD60A), accent2 = Color(0xFFFF9F0A),
    good = Color(0xFF30D158), warn = Color(0xFFFF9F0A), bad = Color(0xFFFF453A),
)

val LightPalette = WizPalette(
    isDark = false,
    bg = Color(0xFFF4F4F7), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEEEEF3), line = Color(0xFFE2E2E8),
    text = Color(0xFF111114), muted = Color(0xFF6B6B76),
    accent = Color(0xFFFFD60A), onAccent = Color(0xFF1C1400), accentInk = Color(0xFFB07D00), accent2 = Color(0xFFF08C00),
    good = Color(0xFF1E9E4A), warn = Color(0xFFD97706), bad = Color(0xFFD92D20),
)

val LocalWiz = staticCompositionLocalOf { DarkPalette }

object Wiz {
    val c: WizPalette @Composable get() = LocalWiz.current
}

/** Tabular figures so numbers don't jiggle as they change. */
val Numeric = TextStyle(fontFeatureSettings = "tnum")

@Composable
fun WizTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    val p = animatedPalette(if (dark) DarkPalette else LightPalette)
    val scheme = if (dark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, secondary = p.accent2, background = p.bg, onBackground = p.text,
        surface = p.surface, onSurface = p.text, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
        outline = p.line, outlineVariant = p.line, error = p.bad, surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2, surfaceContainerLow = p.surface,
        primaryContainer = p.accent, onPrimaryContainer = p.onAccent, secondaryContainer = p.surface2,
        onSecondaryContainer = p.text,
    ) else lightColorScheme(
        primary = p.accentInk, onPrimary = Color.White, secondary = p.accent2, background = p.bg, onBackground = p.text,
        surface = p.surface, onSurface = p.text, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
        outline = p.line, outlineVariant = p.line, error = p.bad, surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surface2, surfaceContainerLow = p.surface,
        primaryContainer = p.accent, onPrimaryContainer = p.onAccent, secondaryContainer = p.surface2,
        onSecondaryContainer = p.text,
    )
    val base = Typography()
    val type = base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
    )
    CompositionLocalProvider(LocalWiz provides p) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}

/** Cross-fades every colour when the theme changes instead of snapping. */
@Composable
private fun animatedPalette(t: WizPalette): WizPalette {
    @Composable fun a(c: Color) = animateColorAsState(c, tween(450), label = "theme").value
    return WizPalette(
        isDark = t.isDark, bg = a(t.bg), surface = a(t.surface), surface2 = a(t.surface2), line = a(t.line),
        text = a(t.text), muted = a(t.muted), accent = a(t.accent), onAccent = a(t.onAccent), accentInk = a(t.accentInk),
        accent2 = a(t.accent2), good = a(t.good), warn = a(t.warn), bad = a(t.bad),
    )
}
