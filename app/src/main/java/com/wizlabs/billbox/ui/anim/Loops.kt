package com.wizlabs.billbox.ui.anim

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.PI
import kotlin.math.sin

/*
 * Looping animation, kept light on the phone:
 *  - ONE shared clock for the whole app, ticking at 24 fps (30 fps while something "fast" such as a sync is shown),
 *  - it only runs while something on screen subscribes to it,
 *  - it stops (everything freezes on a still frame) when the window loses focus, the app is in the
 *    background, Battery Saver is on, or animations are turned off in Android settings,
 *  - readers use the time only inside draw / graphicsLayer lambdas, so a tick redraws but never recomposes.
 */

@Stable
class LoopClock {
    internal val time = mutableFloatStateOf(0f)
    internal var subs by mutableIntStateOf(0)
    internal var fast by mutableIntStateOf(0)
}

val LocalLoopClock = staticCompositionLocalOf<LoopClock?> { null }

/** Touch counter fed by MainActivity.dispatchTouchEvent (throttled to one bump per second). */
object LoopIdle {
    const val SETTLE_NS = 9_000_000_000L
    var touches by mutableIntStateOf(0)
        private set
    private var lastBump = 0L
    fun onTouch() {
        val now = System.nanoTime()
        if (now - lastBump > 1_000_000_000L) { lastBump = now; touches++ }
    }
}

/** True when looping animation may run right now. */
@Composable
fun rememberLoopsAllowed(): Boolean {
    val reduce = LocalReduceMotion.current
    val focused = LocalWindowInfo.current.isWindowFocused
    val life by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val ctx = LocalContext.current
    val pm = remember(ctx) { ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var saver by remember { mutableStateOf(pm?.isPowerSaveMode == true) }
    DisposableEffect(ctx) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { saver = pm?.isPowerSaveMode == true }
        }
        ContextCompat.registerReceiver(ctx, r, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { ctx.unregisterReceiver(r) } }
    }
    return !reduce && !saver && focused && life.isAtLeast(Lifecycle.State.RESUMED)
}

@Composable
fun ProvideLoopClock(content: @Composable () -> Unit) {
    val clock = remember { LoopClock() }
    val allowed = rememberLoopsAllowed()
    LaunchedEffect(clock, allowed) {
        if (!allowed) return@LaunchedEffect
        snapshotFlow { Triple(clock.subs > 0, clock.fast > 0, LoopIdle.touches) }.collectLatest { (any, fast, _) ->
            if (!any) return@collectLatest
            // delay + wait-for-vsync ≈ 33 ms (30 fps) or ≈ 42 ms (24 fps)
            val pause = if (fast) 20L else 28L
            var last = -1L
            val start = System.nanoTime()
            while (true) {
                // Ambient loops settle on a still frame after a while without touches; a touch wakes them.
                // Loops that show live work (fast, e.g. a running sync) keep going.
                if (!fast && System.nanoTime() - start > LoopIdle.SETTLE_NS) return@collectLatest
                withFrameNanos { now ->
                    if (last >= 0) {
                        val dt = ((now - last) / 1e9f).coerceAtMost(0.1f)
                        clock.time.floatValue = (clock.time.floatValue + dt) % 3600f
                    }
                    last = now
                }
                delay(pause)
            }
        }
    }
    CompositionLocalProvider(LocalLoopClock provides clock, content = content)
}

/** Seconds on the shared loop clock. Read `.floatValue` only inside draw or graphicsLayer blocks. */
@Composable
fun rememberLoopTime(fast: Boolean = false): FloatState {
    val clock = LocalLoopClock.current ?: return remember { mutableFloatStateOf(0f) }
    DisposableEffect(clock, fast) {
        clock.subs++; if (fast) clock.fast++
        onDispose { clock.subs--; if (fast) clock.fast-- }
    }
    return clock.time
}

/** 0..1 sine wave with the given period, for a clock value in seconds. */
fun wave(t: Float, periodS: Float, phase: Float = 0f): Float = 0.5f + 0.5f * sin(2f * PI.toFloat() * (t / periodS + phase))

/** Gentle endless bob up and down (illustrations, empty states). */
fun Modifier.floating(amplitude: Dp = 6.dp, periodMs: Int = 2600): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val px = with(LocalDensity.current) { amplitude.toPx() }
    val t = rememberLoopTime()
    graphicsLayer { translationY = (wave(t.floatValue, periodMs / 1000f) * 2f - 1f) * px }
}

/** Breathing scale between [min] and [max] (status dots, badges). */
fun Modifier.pulseScale(min: Float = 0.9f, max: Float = 1.12f, periodMs: Int = 1800, enabled: Boolean = true): Modifier = composed {
    if (!enabled || LocalReduceMotion.current) return@composed this
    val t = rememberLoopTime()
    graphicsLayer { val s = min + (max - min) * wave(t.floatValue, periodMs / 1000f); scaleX = s; scaleY = s }
}

/** A rounded border whose opacity breathes (invites a tap). */
fun Modifier.pulseBorder(color: Color, corner: Dp, width: Dp = 1.5.dp, min: Float = 0.35f, max: Float = 0.8f, periodMs: Int = 2200): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val t = rememberLoopTime()
    drawWithCache {
        val w = width.toPx(); val r = CornerRadius(corner.toPx()); val stroke = Stroke(w)
        onDrawWithContent {
            drawContent()
            val a = if (reduce) max else min + (max - min) * wave(t.floatValue, periodMs / 1000f)
            drawRoundRect(color, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), r, stroke, alpha = a)
        }
    }
}

/** A soft highlight that sweeps across the content every few seconds (primary buttons). Allocation-free per frame. */
fun Modifier.shimmer(color: Color = Color.White.copy(alpha = 0.35f), periodMs: Int = 3200): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val t = rememberLoopTime()
    drawWithCache {
        val band = size.width * 0.45f
        val brush = Brush.horizontalGradient(listOf(Color.Transparent, color, Color.Transparent), startX = 0f, endX = band)
        val bandSize = Size(band, size.height)
        onDrawWithContent {
            drawContent()
            val p = (t.floatValue % (periodMs / 1000f)) / (periodMs / 1000f) // 0..1
            val sweep = p / 0.45f // sweep during first 45 %, rest is a pause
            if (sweep < 1f) clipRect {
                translate(left = -band + (size.width + band) * sweep) { drawRect(brush, size = bandSize) }
            }
        }
    }
}

/** Haptics for primary actions. */
class Haptics(private val view: View) {
    fun tap() { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
    fun tick() { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun confirm() {
        view.performHapticFeedback(if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    }
    fun reject() {
        view.performHapticFeedback(if (android.os.Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
    }
}

@Composable
fun rememberHaptics(): Haptics { val v = LocalView.current; return remember(v) { Haptics(v) } }
