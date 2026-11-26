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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.wizlabs.billbox.R
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
    /** Emerald "ink": primary buttons, selected chips. Use as a fill with [onAccent]. */
    val accent: Color,
    /** Text/icons drawn on [accent] (bold 16 sp+ on light). */
    val onAccent: Color,
    /** Emerald that stays readable as small text/lines on [surface]. */
    val accentInk: Color,
    /** Saffron: warranty time, reminders. */
    val warranty: Color,
    /** Saffron dark enough for small text on [surface]. */
    val warrantyInk: Color,
    val good: Color,
    val warn: Color,
    val bad: Color,
) {
    /** Kept so older call sites read the warranty colour as the secondary accent. */
    val accent2: Color get() = warranty
}

val DarkPalette = WizPalette(
    isDark = true,
    bg = Color(0xFF0C1110), surface = Color(0xFF141C1A), surface2 = Color(0xFF1C2724), line = Color(0xFF25322E),
    text = Color(0xFFEEF5F1), muted = Color(0xFF8FA39A),
    accent = Color(0xFF19C48B), onAccent = Color(0xFF04130D), accentInk = Color(0xFF19C48B),
    warranty = Color(0xFFF5B841), warrantyInk = Color(0xFFF5B841),
    good = Color(0xFF34D399), warn = Color(0xFFFB923C), bad = Color(0xFFF87171),
)

val LightPalette = WizPalette(
    isDark = false,
    bg = Color(0xFFF7F6F2), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEFEDE6), line = Color(0xFFE3E0D6),
    text = Color(0xFF111A16), muted = Color(0xFF5E6B64),
    accent = Color(0xFF0E9F6E), onAccent = Color(0xFFFFFFFF), accentInk = Color(0xFF0B7D57),
    warranty = Color(0xFFD99A1E), warrantyInk = Color(0xFF8F5F00),
    good = Color(0xFF16A34A), warn = Color(0xFFC2410C), bad = Color(0xFFCC2020),
)

/** 12 fixed category tints (same in light and dark). */
fun categoryTint(c: com.wizlabs.billbox.model.Category?): Color = when (c) {
    com.wizlabs.billbox.model.Category.ELECTRONICS -> Color(0xFF2563EB)
    com.wizlabs.billbox.model.Category.APPLIANCES -> Color(0xFF0891B2)
    com.wizlabs.billbox.model.Category.GROCERY -> Color(0xFF16A34A)
    com.wizlabs.billbox.model.Category.FUEL -> Color(0xFFEA580C)
    com.wizlabs.billbox.model.Category.MEDICAL -> Color(0xFFDB2777)
    com.wizlabs.billbox.model.Category.CLOTHING -> Color(0xFF7C3AED)
    com.wizlabs.billbox.model.Category.FURNITURE -> Color(0xFFA16207)
    com.wizlabs.billbox.model.Category.VEHICLE -> Color(0xFF475569)
    com.wizlabs.billbox.model.Category.UTILITIES -> Color(0xFF0D9488)
    com.wizlabs.billbox.model.Category.RESTAURANT -> Color(0xFFE11D48)
    com.wizlabs.billbox.model.Category.TRAVEL -> Color(0xFF4F46E5)
    com.wizlabs.billbox.model.Category.OTHER, null -> Color(0xFF6B7280)
}

val LocalWiz = staticCompositionLocalOf { DarkPalette }

object Wiz {
    val c: WizPalette @Composable get() = LocalWiz.current
}

/** IBM Plex Mono for amounts, dates, invoice numbers and GSTIN. */
val PlexMono = FontFamily(Font(R.font.plexmono, FontWeight.Medium))

/** Manrope for headings (variable weight axis). */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Manrope = FontFamily(
    Font(R.font.manrope, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.manrope, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.manrope, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    Font(R.font.manrope, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

/** Mono numerals (amounts, dates). Tabular by nature. */
val Numeric = TextStyle(fontFamily = PlexMono, fontFeatureSettings = "tnum")

@Composable
fun WizTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.DARK -> true; ThemeMode.LIGHT -> false }
    val p = animatedPalette(if (dark) DarkPalette else LightPalette)
    val scheme = if (dark) darkColorScheme(
        primary = p.accent, onPrimary = p.onAccent, secondary = p.warranty, background = p.bg, onBackground = p.text,
        surface = p.surface, onSurface = p.text, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
        outline = p.line, outlineVariant = p.line, error = p.bad, surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface2, surfaceContainerHighest = p.surface2, surfaceContainerLow = p.surface,
        primaryContainer = p.accent, onPrimaryContainer = p.onAccent, secondaryContainer = p.surface2,
        onSecondaryContainer = p.text,
    ) else lightColorScheme(
        primary = p.accent, onPrimary = p.onAccent, secondary = p.warranty, background = p.bg, onBackground = p.text,
        surface = p.surface, onSurface = p.text, surfaceVariant = p.surface2, onSurfaceVariant = p.muted,
        outline = p.line, outlineVariant = p.line, error = p.bad, surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface, surfaceContainerHighest = p.surface2, surfaceContainerLow = p.surface,
        primaryContainer = p.accent, onPrimaryContainer = p.onAccent, secondaryContainer = p.surface2,
        onSecondaryContainer = p.text,
    )
    val base = Typography()
    val type = base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp),
        headlineMedium = base.headlineMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp),
        headlineSmall = base.headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = base.titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = base.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleSmall = base.titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = base.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
        bodyMedium = base.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = base.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = base.labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.8.sp),
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
        warranty = a(t.warranty), warrantyInk = a(t.warrantyInk), good = a(t.good), warn = a(t.warn), bad = a(t.bad),
    )
}
