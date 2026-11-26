package com.wizlabs.billbox.ui.mascot

import com.wizlabs.billbox.engine.Action
import com.wizlabs.billbox.engine.Expression
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Tally's pose: plain numbers rewritten in place each frame by [update] (no allocation per frame, no Android types).
 * Angles are degrees; lengths are in the 100x100 art box.
 */
class TallyPose {
    var bodyX = 0f; var bodyY = 0f; var squash = 0f
    /** Lid angle: 0 = shut. Negative lifts the free end. */
    var lid = -4f
    var receiptY = 0f; var receiptTilt = 8f
    var eyeOpen = 1f; var eyeKind = EYE_ROUND; var eyeR = 3.5f
    var pupilX = 0f; var pupilY = 0f; var pupilLeftUp = 0f
    var mouth = MOUTH_SMILE
    var brow = 0f; var sweat = 0f
    var badge = 0f
    var bell = 0f; var bellAngle = 0f
    var mag = 0f; var magX = 0f

    /**
     * @param t seconds on the shared throttled clock (0 when still).
     * @param burst 0..1 progress of the one-shot for [a]; 1 = finished.
     * @param loops false = still frame (reduce motion, Battery Saver, idle, window unfocused).
     */
    fun update(e: Expression, a: Action, t: Float, burst: Float, loops: Boolean) {
        bodyX = 0f; bodyY = 0f; squash = 0f; lid = -4f; receiptY = 0f; receiptTilt = 8f
        eyeOpen = 1f; eyeR = 3.5f; pupilX = 0f; pupilY = 0.4f; pupilLeftUp = 0f
        brow = 0f; sweat = 0f; badge = 0f; bell = 0f; bellAngle = 0f; mag = 0f; magX = 0f
        when (e) {
            Expression.NEUTRAL -> { eyeKind = EYE_ROUND; mouth = MOUTH_SMILE }
            Expression.HAPPY -> { eyeKind = EYE_HAPPY; mouth = MOUTH_BIG }
            Expression.PROUD -> { eyeKind = EYE_CONTENT; mouth = MOUTH_SMILE; badge = 1f }
            Expression.CURIOUS -> { eyeKind = EYE_ROUND; mouth = MOUTH_FLAT; pupilLeftUp = 1.6f; pupilX = 0.6f }
            Expression.SLEEPY -> { eyeKind = EYE_LINE; mouth = MOUTH_FLAT; lid = 0f }
            Expression.WORRIED -> { eyeKind = EYE_ROUND; mouth = MOUTH_FROWN; brow = 1f; sweat = 1f; pupilY = 0.9f }
            Expression.ALERT -> { eyeKind = EYE_ROUND; eyeR = 4.2f; mouth = MOUTH_O; bell = 1f; pupilY = -0.2f }
            Expression.SEARCHING -> { eyeKind = EYE_ROUND; mouth = MOUTH_FLAT; mag = 1f; pupilX = 1.4f; pupilY = -0.4f }
        }
        // Blink (only while loops run).
        if (loops && e != Expression.SLEEPY && eyeKind == EYE_ROUND) {
            val ph = t % 3.6f
            if (ph < 0.14f) eyeOpen = abs(ph - 0.07f) / 0.07f
        }
        val p = burst.coerceIn(0f, 1f)
        when (a) {
            Action.STILL -> {}
            Action.LID_OPEN -> lid = -35f * sin(p * PI.toFloat())
            Action.RECEIPT_DROP -> {
                // receipt falls in while the lid lifts, then the lid closes and the badge pops
                receiptY = -26f * (1f - ease(p / 0.7f)); lid = if (p < 0.6f) -30f * sin(p / 0.6f * PI.toFloat() / 2f) else -30f * (1f - (p - 0.6f) / 0.4f)
                squash = if (p > 0.7f) sin((p - 0.7f) / 0.3f * PI.toFloat()) * 0.05f else 0f
                badge = ease((p - 0.55f) / 0.45f).coerceIn(0f, 1f)
            }
            Action.HOP -> bodyY = -abs(sin(p * PI.toFloat())) * 6f
            Action.LID_BOB -> lid = if (loops) -4f + sin(t * 6.2f) * 3f else -4f
            Action.SHAKE -> bodyX = if (p < 1f) sin(p * 3f * 2f * PI.toFloat()) * 2.2f * (1f - p) else 0f
            Action.BELL_RING, Action.BELL -> bellAngle = if (p < 1f) sin(p * 3f * 2f * PI.toFloat()) * 12f * (1f - p * 0.4f) else 0f
            Action.MAGNIFIER_SWEEP -> magX = if (p < 1f) sin(p * 2f * PI.toFloat()) * 5f else 0f
            Action.WAVE -> lid = -4f - (if (p < 1f) abs(sin(p * 3f * PI.toFloat())) * 22f else 0f)
            Action.FILE -> { receiptY = -26f * (1f - ease(p)); lid = if (p < 1f) -22f * sin(p * PI.toFloat()) else -4f; badge = ease((p - 0.6f) / 0.4f).coerceIn(0f, 1f) }
        }
    }

    private fun ease(x: Float): Float { val c = x.coerceIn(0f, 1f); return 1f - (1f - c) * (1f - c) * (1f - c) }

    companion object {
        const val EYE_ROUND = 0; const val EYE_HAPPY = 1; const val EYE_CONTENT = 2; const val EYE_LINE = 3
        const val MOUTH_SMILE = 0; const val MOUTH_BIG = 1; const val MOUTH_FLAT = 2; const val MOUTH_FROWN = 3; const val MOUTH_O = 4
    }
}
