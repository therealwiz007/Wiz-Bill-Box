package com.wizlabs.billbox.ui.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wizlabs.billbox.engine.Action
import com.wizlabs.billbox.engine.Expression
import com.wizlabs.billbox.engine.Mood
import com.wizlabs.billbox.engine.TallyBrain
import com.wizlabs.billbox.ui.anim.rememberLoopTime
import com.wizlabs.billbox.ui.anim.rememberLoopsAllowed
import com.wizlabs.billbox.ui.theme.Wiz
import kotlin.math.PI
import kotlin.math.sin

/**
 * Tally, the filing-box mascot. Decorative: it has no content description and never carries information that is
 * not also shown as text. Reads the shared throttled loop clock only in the draw phase; returns a still frame
 * when loops are not allowed (reduce motion, Battery Saver, unfocused window) or after the clock settles (idle).
 * [replay] changes re-trigger the one-shot action.
 */
@Composable
fun Tally(
    mood: Mood, size: Dp, modifier: Modifier = Modifier, replay: Int = 0, onTap: (() -> Unit)? = null,
) {
    val art = remember { TallyArt() }
    val pose = remember { TallyPose() }
    val allowed = rememberLoopsAllowed()
    val clock = rememberLoopTime(fast = mood.action == Action.LID_BOB && allowed)
    val burst = remember { Animatable(1f) }
    val outline = if (Wiz.c.isDark) Color(0xFF25322E) else null
    var tapCount by remember { mutableIntStateOf(0) }
    val shown = if (tapCount > 0) TallyBrain.onTap(mood) else mood
    LaunchedEffect(shown.action, replay, tapCount, allowed) {
        val d = TallyBrain.durationMs(shown.action)
        if (!allowed || d == 0) { burst.snapTo(1f); return@LaunchedEffect }
        burst.snapTo(0f)
        burst.animateTo(1f, tween(d, easing = LinearEasing))
    }
    val click = if (onTap != null || mood.expression == Expression.SLEEPY) Modifier.clickable(
        interactionSource = remember { MutableInteractionSource() }, indication = null,
    ) { tapCount++; onTap?.invoke() } else Modifier
    Canvas(modifier.size(size).semantics { contentDescription = "" }.then(click)) {
        pose.update(shown.expression, shown.action, if (allowed) clock.floatValue else 0f, burst.value, allowed)
        scale(this.size.width / 100f, this.size.height / 100f, pivot = Offset.Zero) { drawTally(art, pose, outline) }
    }
}

internal fun DrawScope.drawTally(a: TallyArt, p: TallyPose, outline: Color?) {
    translate(p.bodyX, p.bodyY) {
        // squash about the feet
        scale(1f + p.squash, 1f - p.squash, pivot = Offset(50f, 94f)) {
            // receipt first: the lid and body overlap its lower part
            translate(0f, p.receiptY) {
                rotate(p.receiptTilt, pivot = Offset(50f, 54f)) {
                    drawPath(a.receipt, Color.White)
                    drawPath(a.receipt, a.paperLine, style = S09)
                    for (i in 0..2) drawLine(a.paperLine, Offset(43f, 25f + i * 5f), Offset(if (i == 2) 52f else 57f, 25f + i * 5f), 1.4f, StrokeCap.Round)
                }
            }
            drawPath(a.body, a.kraft)
            drawPath(a.bodyShade, a.shade)
            if (outline != null) drawPath(a.body, outline, style = S1)
            drawPath(a.footL, a.shade); drawPath(a.footR, a.shade)
            // lid, hinged at its left-bottom corner
            translate(17f, 34f) {
                rotate(p.lid, pivot = Offset(2f, 10f)) {
                    drawPath(a.lid, a.lidColor)
                    if (outline != null) drawPath(a.lid, outline, style = S1)
                }
            }
            drawPath(a.labelPanel, a.label)
            drawFace(a, p)
            if (p.badge > 0f) translate(78f, 52f) { scale(p.badge, p.badge, pivot = Offset.Zero) {
                drawCircle(a.emerald, 6.5f)
                drawPath(a.check, Color.White, style = CheckStroke)
            } }
        }
        if (p.bell > 0f) translate(84f, 16f) { rotate(p.bellAngle, pivot = Offset(0f, -2f)) {
            drawPath(a.bell, a.bellColor); drawCircle(a.bellColor, 1.8f, Offset(0f, 16.5f))
        } }
        if (p.mag > 0f) translate(76f + p.magX, 24f) {
            drawCircle(Color.White.copy(alpha = 0.55f), 6f)
            drawCircle(a.ink, 6f, style = S2)
            drawLine(a.ink, Offset(4.4f, 4.4f), Offset(11f, 11f), 2.6f, StrokeCap.Round)
        }
        if (p.sweat > 0f) translate(70f, 52f) { drawCircle(a.drop, 2f, Offset(0f, 1f)); drawLine(a.drop, Offset(0f, -3f), Offset(0f, 0f), 1.6f, StrokeCap.Round) }
    }
}

private val S09 = Stroke(0.9f); private val S1 = Stroke(1f); private val S2 = Stroke(2f); private val S07 = Stroke(0.7f)
private val FaceStroke = Stroke(1.5f, cap = StrokeCap.Round)
private val CheckStroke = Stroke(1.8f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)

private fun DrawScope.drawFace(a: TallyArt, p: TallyPose) {
    val st = FaceStroke
    for (side in 0..1) {
        val c = if (side == 0) a.eyeL else a.eyeR
        when (p.eyeKind) {
            TallyPose.EYE_ROUND -> {
                val r = p.eyeR
                val h = r * p.eyeOpen.coerceIn(0.08f, 1f)
                scale(1f, h / r, pivot = c) {
                    drawCircle(Color.White, r, c); drawCircle(Color(0xFFC9A27A), r, c, style = S07)
                    val up = if (side == 0) p.pupilLeftUp else 0f
                    drawCircle(a.ink, 1.75f, Offset(c.x + p.pupilX, c.y + p.pupilY - up))
                }
                if (p.brow > 0f) {
                    val d = if (side == 0) 1f else -1f
                    drawLine(a.ink, Offset(c.x - 3.5f * d, c.y - 6.2f), Offset(c.x + 3.5f * d, c.y - 7.8f), 1.3f, StrokeCap.Round)
                }
            }
            TallyPose.EYE_HAPPY -> { // ^
                drawLine(a.ink, Offset(c.x - 3.2f, c.y + 1.4f), Offset(c.x, c.y - 1.8f), st.width, StrokeCap.Round)
                drawLine(a.ink, Offset(c.x, c.y - 1.8f), Offset(c.x + 3.2f, c.y + 1.4f), st.width, StrokeCap.Round)
            }
            TallyPose.EYE_CONTENT -> { // closed smile
                drawPath(if (side == 0) a.contentL else a.contentR, a.ink, style = st)
            }
            else -> drawLine(a.ink, Offset(c.x - 3.2f, c.y + 0.4f), Offset(c.x + 3.2f, c.y + 0.4f), st.width, StrokeCap.Round)
        }
    }
    val mx = 50f; val my = 67.6f
    when (p.mouth) {
        TallyPose.MOUTH_SMILE -> drawPath(a.smile, a.ink, style = st)
        TallyPose.MOUTH_BIG -> drawPath(a.big, a.ink)
        TallyPose.MOUTH_FLAT -> drawLine(a.ink, Offset(mx - 2.6f, my), Offset(mx + 2.6f, my), st.width, StrokeCap.Round)
        TallyPose.MOUTH_FROWN -> drawPath(a.frown, a.ink, style = st)
        else -> drawCircle(a.ink, 1.7f, Offset(mx, my + 0.4f))
    }
}
