@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.wizlabs.billbox.ui.anim

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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

/** Gentle endless bob up and down (logos, empty states). */
fun Modifier.floating(amplitude: Dp = 6.dp, periodMs: Int = 2600): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val px = with(LocalDensity.current) { amplitude.toPx() }
    val t = rememberInfiniteTransition(label = "float")
    val y by t.animateFloat(-1f, 1f, infiniteRepeatable(tween(periodMs / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
    graphicsLayer { translationY = y * px }
}

/** A soft highlight that sweeps across the content every few seconds (primary buttons). */
fun Modifier.shimmer(color: Color = Color.White.copy(alpha = 0.35f), periodMs: Int = 2800): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(-0.6f, 1.8f, infiniteRepeatable(tween(periodMs, easing = LinearEasing)), label = "x")
    drawWithContent {
        drawContent()
        val w = size.width
        val cx = x * w
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, color, Color.Transparent),
                start = Offset(cx - w * 0.25f, 0f), end = Offset(cx + w * 0.25f, size.height),
            ),
        )
    }
}

/** Slow breathing pulse of scale/alpha (glows, "ending soon" dots). */
@Composable
fun rememberPulse(min: Float = 0.85f, max: Float = 1.15f, periodMs: Int = 1800): Float {
    if (LocalReduceMotion.current) return 1f
    val t = rememberInfiniteTransition(label = "pulse")
    val v by t.animateFloat(min, max, infiniteRepeatable(tween(periodMs / 2, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    return v
}

/** Counts a number up from zero the first time it is shown and glides to new values after that. */
@Composable
fun CountUpText(value: Double, format: (Double) -> String, style: TextStyle, color: Color, modifier: Modifier = Modifier, maxLines: Int = 1) {
    val reduce = LocalReduceMotion.current
    val anim = remember { Animatable(if (reduce) value.toFloat() else 0f) }
    LaunchedEffect(value) {
        if (reduce) anim.snapTo(value.toFloat())
        else anim.animateTo(value.toFloat(), tween(900, easing = FastOutSlowInEasing))
    }
    // Show the exact value once settled so paise never look rounded.
    val shown = if (anim.value == value.toFloat()) value else anim.value.toDouble()
    Text(format(shown), modifier = modifier, style = style, color = color, maxLines = maxLines)
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

// ---------- "Saved!" celebration ----------

/**
 * A yellow disc pops in, a tick draws itself, and confetti bursts outward. Calls [onDone] after ~1.4 s.
 */
@Composable
fun SavedBurst(label: String, onDone: () -> Unit) {
    val c = Wiz.c
    val reduce = LocalReduceMotion.current
    val disc = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (reduce) { disc.snapTo(1f); tick.snapTo(1f); delay(700); onDone(); return@LaunchedEffect }
        launch { disc.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f)) }
        launch { delay(120); burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
        delay(180)
        tick.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        delay(520)
        fade.animateTo(0f, tween(260))
        onDone()
    }
    val confetti = listOf(c.accent, c.accent2, c.good, Color.White, c.accent, c.warn)
    Box(Modifier.size(220.dp).graphicsLayer { alpha = fade.value }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(220.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension * 0.22f
            // confetti
            val n = 14
            for (i in 0 until n) {
                val a = (i / n.toFloat()) * 2 * Math.PI + 0.3
                val d = r * (1.1f + 1.4f * burst.value) * (if (i % 2 == 0) 1f else 0.78f)
                val p = Offset(center.x + (cos(a) * d).toFloat(), center.y + (sin(a) * d).toFloat())
                val dotR = (if (i % 3 == 0) 7f else 5f) * (1f - burst.value * 0.7f)
                drawCircle(confetti[i % confetti.size].copy(alpha = (1f - burst.value).coerceIn(0f, 1f)), dotR.dp.toPx() / 2, p)
            }
            // glow + disc
            drawCircle(c.accent.copy(alpha = 0.25f * disc.value), r * 1.45f * disc.value, center)
            drawCircle(c.accent, r * disc.value, center)
            // tick drawn progressively
            val path = Path().apply {
                moveTo(center.x - r * 0.42f, center.y + r * 0.02f)
                lineTo(center.x - r * 0.10f, center.y + r * 0.34f)
                lineTo(center.x + r * 0.46f, center.y - r * 0.30f)
            }
            val pm = PathMeasure().apply { setPath(path, false) }
            val seg = Path()
            pm.getSegment(0f, pm.length * tick.value, seg, true)
            drawPath(seg, c.onAccent, style = Stroke(width = r * 0.17f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Text(
            label, style = MaterialTheme.typography.titleMedium, color = c.text,
            modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { alpha = tick.value; translationY = (1f - tick.value) * 20f },
        )
    }
}
