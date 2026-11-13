package com.wizlabs.billbox.ui.anim

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Predictive back: while the user drags the back gesture, the current screen shrinks toward the
 * swipe edge with rounded corners (Android 14+). Releasing goes back; cancelling springs it home.
 * Transform-only (graphicsLayer), so the drag never recomposes.
 */
@Stable
class BackPeek {
    internal val progress = Animatable(0f)
    internal var fromLeft by mutableStateOf(true)
    /** Route the peek belongs to; only that screen is transformed. */
    var route by mutableStateOf<String?>(null)
        internal set

    suspend fun reset() { route = null; progress.snapTo(0f) }
}

val LocalBackPeek = staticCompositionLocalOf<BackPeek?> { null }

private val peekShape = RoundedCornerShape(28.dp)

/** Drop-in for BackHandler on simple "go back" screens; [route] is the screen's own route. */
@Composable
fun WizBackHandler(route: String, enabled: Boolean = true, onBack: () -> Unit) {
    val peek = LocalBackPeek.current
    val reduce = LocalReduceMotion.current
    val back by rememberUpdatedState(onBack)
    PredictiveBackHandler(enabled) { events ->
        try {
            if (peek != null && !reduce) peek.route = route
            events.collect { e ->
                if (peek != null && !reduce) {
                    peek.fromLeft = e.swipeEdge == BackEventCompat.EDGE_LEFT
                    peek.progress.snapTo(e.progress)
                }
            }
            back()
        } catch (c: CancellationException) {
            if (peek != null) withContext(NonCancellable) {
                peek.progress.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 400f))
                peek.route = null
            }
            throw c
        }
    }
}

/** Applies the back-peek transform to the screen showing [route]. */
fun Modifier.backPeek(route: String): Modifier = composed {
    val peek = LocalBackPeek.current ?: return@composed this
    val px = with(LocalDensity.current) { 24.dp.toPx() }
    graphicsLayer {
        val p = if (peek.route == route) peek.progress.value else 0f
        if (p > 0f) {
            val s = 1f - 0.08f * p
            scaleX = s; scaleY = s
            translationX = (if (peek.fromLeft) 1f else -1f) * px * p
            shape = peekShape
            clip = true
        }
    }
}
