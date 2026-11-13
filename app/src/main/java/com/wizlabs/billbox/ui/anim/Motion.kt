@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.wizlabs.billbox.ui.anim

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wizlabs.billbox.ui.theme.Wiz
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/** Shared motion language for Wiz Bill Box: one set of springs and timings, used everywhere. */
object Motion {
    /** Snappy and slightly bouncy: presses, pops, chips. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
    /** Smooth with no overshoot: layout and position changes. */
    fun <T> smooth() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    const val SCREEN_MS = 380
}

/** True when the user turned animations off in Android's settings (Accessibility › Remove animations). */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun rememberReduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        !android.animation.ValueAnimator.areAnimatorsEnabled() ||
            runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
                .getOrDefault(false)
    }
}

// ---------- Shared element (photo flies from the list into the bill screen) ----------

val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@Composable
fun ProvideNavScope(scope: AnimatedVisibilityScope, content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalNavScope provides scope, content = content)

private val heroBounds = BoundsTransform { _, _ -> spring(dampingRatio = 0.8f, stiffness = 380f) }

/** Marks a view as the same element across screens (by [key]); it morphs between them on navigation. */
fun Modifier.sharedHero(key: String): Modifier = composed {
    val shared = LocalSharedScope.current
    val nav = LocalNavScope.current
    if (shared == null || nav == null || LocalReduceMotion.current) this
    else with(shared) {
        this@composed.sharedElement(rememberSharedContentState(key), animatedVisibilityScope = nav, boundsTransform = heroBounds)
    }
}

/** Like [sharedHero] but for text/containers that change size: they crossfade while moving. */
fun Modifier.sharedBoundsHero(key: String): Modifier = composed {
    val shared = LocalSharedScope.current
    val nav = LocalNavScope.current
    if (shared == null || nav == null || LocalReduceMotion.current) this
    else with(shared) {
        this@composed.sharedBounds(rememberSharedContentState(key), animatedVisibilityScope = nav, boundsTransform = heroBounds)
    }
}

// ---------- Small building blocks ----------

/** Shrinks a little while pressed and springs back: makes cards and buttons feel physical. */
fun Modifier.pressScale(interaction: MutableInteractionSource, pressed: Float = 0.965f): Modifier = composed {
    val isPressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (isPressed) pressed else 1f, Motion.bouncy(), label = "press")
    graphicsLayer { scaleX = s; scaleY = s }
}

/**
 * Fades and slides content up when it first appears, [index] steps later than the one before.
 * Pass [enabled] = false for items that were already shown (e.g. scrolled back into view).
 */
fun Modifier.enterUp(index: Int = 0, enabled: Boolean = true, distance: Dp = 28.dp, stepMs: Int = 55): Modifier = composed {
    val reduce = LocalReduceMotion.current
    if (!enabled || reduce) return@composed this
    val px = with(LocalDensity.current) { distance.toPx() }
    val p = remember { Animatable(0f) }
    val born = remember { System.nanoTime() }
    LaunchedEffect(Unit) {
        // If the phone is busy (cold start, heavy screen) and frames arrive late, show the content at once:
        // never leave the screen blank just to play an entrance.
        withFrameNanos { }
        if (System.nanoTime() - born > 450_000_000L) { p.snapTo(1f); return@LaunchedEffect }
        delay((index.coerceAtMost(10) * stepMs).toLong())
        p.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 260f))
    }
    graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        translationY = (1f - p.value) * px
        val s = 0.96f + 0.04f * p.value
        scaleX = s; scaleY = s
    }
}

/**
 * Shows a number that counts up from zero the first time and rolls to new values after that.
 * Only the digits that change slide (one short AnimatedContent per character), so it costs a few
 * frames instead of recomposing and re-formatting the whole text every frame.
 */
@Composable
fun CountUpText(value: Double, format: (Double) -> String, style: TextStyle, color: Color, modifier: Modifier = Modifier, maxLines: Int = 1) {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(if (reduce) value else 0.0) }
    LaunchedEffect(value) { if (!reduce) withFrameNanos { }; shown = value }
    RollingNumber(format(shown), style, color, modifier, rising = value >= 0)
}

/**
 * Text whose characters roll vertically when they change (odometer style). Characters are matched
 * from the right, so "₹950" → "₹1,200" rolls the units, tens and hundreds and slides the new ones in.
 */
@Composable
fun RollingNumber(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, rising: Boolean = true) {
    if (LocalReduceMotion.current) { Text(text, modifier = modifier, style = style, color = color, maxLines = 1); return }
    var prev by remember { mutableStateOf(text) }
    val up = remember(text) { rising && text.filter(Char::isDigit).let { n -> n.length > prev.filter(Char::isDigit).length || (n.length == prev.filter(Char::isDigit).length && n >= prev.filter(Char::isDigit)) } }
    LaunchedEffect(text) { prev = text }
    Row(modifier) {
        val n = text.length
        for (i in 0 until n) {
            val ch = text[i]
            // key by position from the right so units stay units when the length changes
            androidx.compose.runtime.key(n - i) {
                AnimatedContent(
                    ch,
                    transitionSpec = {
                        val dir = if (up) 1 else -1
                        (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { dir * it / 2 } + fadeIn(tween(160))) togetherWith
                            (slideOutVertically(tween(160)) { -dir * it / 2 } + fadeOut(tween(120))) using SizeTransform(clip = false)
                    },
                    label = "digit",
                ) { c -> Text(c.toString(), style = style, color = color, maxLines = 1) }
            }
        }
    }
}

/** Shakes horizontally once each time [trigger] changes (form errors). */
fun Modifier.shakeOn(trigger: Int): Modifier = composed {
    val dx = remember { Animatable(0f) }
    val px = with(LocalDensity.current) { 10.dp.toPx() }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (v in listOf(1f, -0.8f, 0.6f, -0.4f, 0.2f, 0f)) dx.animateTo(v, tween(55))
    }
    graphicsLayer { translationX = dx.value * px }
}

