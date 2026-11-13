package com.wizlabs.billbox.ui.art

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wizlabs.billbox.ui.anim.LocalReduceMotion
import com.wizlabs.billbox.ui.anim.rememberLoopTime
import com.wizlabs.billbox.ui.anim.wave
import com.wizlabs.billbox.ui.theme.Wiz
import kotlin.math.cos
import kotlin.math.sin

/** What the vault is showing: the backup state of the user's bills. */
enum class VaultState { INTRO, SYNCED, SYNCING, PENDING, LOCAL, FAILED }

private val Paper = Color(0xFFFFF8E6)
private val Ink = Color(0xFF3A3630)

/** Builds a receipt outline centred on (0,0) with a torn, zig-zag bottom edge. */
internal fun receiptPath(w: Float, h: Float, teeth: Int = 4): Path = Path().apply {
    val tooth = h * 0.09f
    moveTo(-w / 2, -h / 2)
    lineTo(w / 2, -h / 2)
    lineTo(w / 2, h / 2)
    val step = w / teeth
    for (i in 0 until teeth) {
        lineTo(w / 2 - step * (i + 0.5f), h / 2 - tooth)
        lineTo(w / 2 - step * (i + 1), h / 2)
    }
    close()
}

/**
 * The Wiz Bill Box hero: a little bill vault with receipts orbiting it.
 * Green and calm when everything is safe in Drive, yellow and fast (with a receipt dropping into the slot)
 * while syncing, orange when bills wait to upload or only live on this phone, red when sync failed.
 *
 * All shapes are built once per size in drawWithCache; each tick only moves them (no allocation, no recomposition).
 */
@Composable
fun VaultOrbit(state: VaultState, modifier: Modifier = Modifier, size: Dp = 128.dp) {
    val c = Wiz.c
    val reduce = LocalReduceMotion.current
    val target = when (state) {
        VaultState.INTRO, VaultState.SYNCING -> c.accent
        VaultState.SYNCED -> c.good
        VaultState.PENDING, VaultState.LOCAL -> c.warn
        VaultState.FAILED -> c.bad
    }
    val tint = animateColorAsState(target, tween(500), label = "vaultTint")
    // degrees per second for the orbit; animated so speed changes glide instead of jumping
    val speed = animateFloatAsState(
        when (state) {
            VaultState.SYNCING -> 120f; VaultState.INTRO -> 42f; VaultState.SYNCED -> 22f
            VaultState.PENDING -> 30f; VaultState.LOCAL -> 7f; VaultState.FAILED -> 4f
        }, tween(900), label = "vaultSpeed",
    )
    val satAlpha = animateFloatAsState(if (state == VaultState.LOCAL || state == VaultState.FAILED) 0.5f else 1f, tween(500), label = "satA")
    val badge = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(state) {
        if (reduce) return@LaunchedEffect
        badge.snapTo(0.3f); badge.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 320f))
    }
    val t = rememberLoopTime(fast = state == VaultState.SYNCING)
    val orbit = remember { FloatArray(2) { -1f } } // [last clock time, accumulated angle]; plain memory, not state
    val surface2 = c.surface2
    val line = c.line
    val onTint = c.onAccent

    val label = when (state) {
        VaultState.INTRO -> "Bill vault"
        VaultState.SYNCED -> "All bills backed up to Google Drive"
        VaultState.SYNCING -> "Syncing with Google Drive"
        VaultState.PENDING -> "Bills waiting to upload"
        VaultState.LOCAL -> "Bills saved on this phone only"
        VaultState.FAILED -> "Sync problem"
    }

    Spacer(
        modifier.size(size).semantics { contentDescription = label }.drawWithCache {
            val s = this.size.minDimension
            val cx = this.size.width / 2; val cy = this.size.height / 2
            val r1 = s * 0.43f; val r2 = s * 0.31f
            val dash = PathEffect.dashPathEffect(floatArrayOf(s * 0.025f, s * 0.045f))
            val ringOuter = Stroke(width = s * 0.012f, pathEffect = dash, cap = StrokeCap.Round)
            val ringInner = Stroke(width = s * 0.008f)
            val trail = Stroke(width = s * 0.02f, cap = StrokeCap.Round)
            val thin = Stroke(width = s * 0.012f, join = StrokeJoin.Round)
            val glyph = Stroke(width = s * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val satW = s * 0.10f; val satH = s * 0.13f
            val sat = receiptPath(satW, satH, 3)
            val slipW = s * 0.17f; val slipH = s * 0.22f
            val slip = receiptPath(slipW, slipH, 4)
            // the vault box
            val bodyW = s * 0.38f; val bodyH = s * 0.28f
            val bodyTop = cy - s * 0.08f
            val lidH = s * 0.075f
            val lidW = bodyW * 1.08f
            val slotY = bodyTop + lidH * 0.45f
            val corner = CornerRadius(s * 0.05f)
            val lidCorner = CornerRadius(s * 0.035f)
            val glow = Brush.radialGradient(listOf(Color.White, Color.Transparent), Offset(cx, cy), s * 0.5f)
            val badgeC = Offset(cx + bodyW * 0.42f, bodyTop + bodyH * 0.92f)
            val badgeR = s * 0.075f
            val check = Path().apply {
                moveTo(badgeC.x - badgeR * 0.42f, badgeC.y + badgeR * 0.02f)
                lineTo(badgeC.x - badgeR * 0.10f, badgeC.y + badgeR * 0.34f)
                lineTo(badgeC.x + badgeR * 0.45f, badgeC.y - badgeR * 0.30f)
            }
            val arrow = Path().apply {
                moveTo(badgeC.x, badgeC.y + badgeR * 0.45f); lineTo(badgeC.x, badgeC.y - badgeR * 0.42f)
                moveTo(badgeC.x - badgeR * 0.38f, badgeC.y - badgeR * 0.05f); lineTo(badgeC.x, badgeC.y - badgeR * 0.45f)
                lineTo(badgeC.x + badgeR * 0.38f, badgeC.y - badgeR * 0.05f)
            }
            val bang = Path().apply {
                moveTo(badgeC.x, badgeC.y - badgeR * 0.45f); lineTo(badgeC.x, badgeC.y + badgeR * 0.08f)
                moveTo(badgeC.x, badgeC.y + badgeR * 0.40f); lineTo(badgeC.x, badgeC.y + badgeR * 0.42f)
            }
            val col = tint.value // read here: the cache rebuilds only while the colour is animating
            val glowTint = androidx.compose.ui.graphics.ColorFilter.tint(col, androidx.compose.ui.graphics.BlendMode.Modulate)
            val dashGlyph = Path().apply { moveTo(badgeC.x - badgeR * 0.40f, badgeC.y); lineTo(badgeC.x + badgeR * 0.40f, badgeC.y) }

            onDrawBehind {
                val now = t.floatValue
                // advance the orbit by elapsed time × current speed (continuous across speed changes)
                if (orbit[0] >= 0f) {
                    val dt = now - orbit[0]
                    if (dt in 0f..0.5f) orbit[1] = (orbit[1] + dt * speed.value) % 360f
                }
                orbit[0] = now
                val ang = orbit[1]

                drawCircle(glow, s * 0.5f, Offset(cx, cy), alpha = 0.10f + 0.10f * wave(now, 3.2f),
                    colorFilter = glowTint)
                drawCircle(col, r2, Offset(cx, cy), alpha = 0.18f, style = ringInner)
                rotate(-ang * 0.35f, Offset(cx, cy)) { drawCircle(col, r1, Offset(cx, cy), alpha = 0.55f, style = ringOuter) }

                // receipt dropping into the slot
                val p = when (state) {
                    VaultState.SYNCING -> (now % 1.4f) / 1.4f
                    VaultState.INTRO -> (now % 2.8f) / 2.8f
                    else -> 0.42f
                }
                val slipA = if (state == VaultState.SYNCING || state == VaultState.INTRO) (minOf(p / 0.18f, (1f - p) / 0.12f)).coerceIn(0f, 1f) else 1f
                val slipY = slotY - slipH / 2 - s * 0.02f + p * slipH * 0.95f
                clipRect(bottom = slotY) {
                    translate(cx, slipY) {
                        drawPath(slip, Paper, alpha = slipA)
                        drawPath(slip, line, alpha = slipA, style = thin)
                        for (i in 0..2) {
                            val y = -slipH * 0.30f + i * slipH * 0.17f
                            drawLine(Ink, Offset(-slipW * 0.30f, y), Offset(slipW * (if (i == 2) 0.05f else 0.30f), y), s * 0.012f, StrokeCap.Round, alpha = 0.55f * slipA)
                        }
                    }
                }
                // body + lid
                drawRoundRect(surface2, Offset(cx - bodyW / 2, bodyTop), Size(bodyW, bodyH), corner)
                drawRoundRect(col, Offset(cx - bodyW / 2, bodyTop), Size(bodyW, bodyH), corner, style = thin, alpha = 0.9f)
                drawRoundRect(col.copy(alpha = 0.22f), Offset(cx - bodyW * 0.22f, bodyTop + bodyH * 0.48f), Size(bodyW * 0.44f, bodyH * 0.16f), lidCorner)
                drawRoundRect(col, Offset(cx - lidW / 2, bodyTop - lidH * 0.15f), Size(lidW, lidH), lidCorner)
                drawLine(onTint, Offset(cx - bodyW * 0.26f, slotY), Offset(cx + bodyW * 0.26f, slotY), s * 0.02f, StrokeCap.Round)

                // orbiting receipts with comet trails
                val sa = satAlpha.value
                for (i in 0..2) {
                    val a = ang + i * 120f
                    val rad = Math.toRadians(a.toDouble())
                    val x = cx + (r1 * cos(rad)).toFloat(); val y = cy + (r1 * sin(rad)).toFloat()
                    if (speed.value > 18f) {
                        val sweep = (speed.value * 0.33f).coerceAtMost(46f)
                        drawArc(col, a - sweep - 6f, sweep, false, Offset(cx - r1, cy - r1), Size(r1 * 2, r1 * 2), alpha = 0.45f * sa, style = trail)
                    }
                    translate(x, y) {
                        rotate(a * 0.15f + i * 25f - 20f, Offset.Zero) {
                            drawPath(sat, Paper, alpha = sa)
                            drawPath(sat, col, alpha = sa, style = thin)
                            drawLine(Ink, Offset(-satW * 0.26f, -satH * 0.18f), Offset(satW * 0.26f, -satH * 0.18f), s * 0.01f, alpha = 0.5f * sa)
                            drawLine(Ink, Offset(-satW * 0.26f, satH * 0.02f), Offset(satW * 0.08f, satH * 0.02f), s * 0.01f, alpha = 0.5f * sa)
                        }
                    }
                }

                // status badge
                val bs = badge.value
                scale(bs, badgeC) {
                    drawCircle(surface2, badgeR * 1.28f, badgeC)
                    drawCircle(col, badgeR, badgeC)
                    when (state) {
                        VaultState.SYNCED, VaultState.INTRO -> drawPath(check, onTint, style = glyph)
                        VaultState.SYNCING, VaultState.PENDING -> {
                            val bob = if (state == VaultState.SYNCING) (wave(now, 0.7f) - 0.5f) * badgeR * 0.3f else 0f
                            translate(top = bob) { drawPath(arrow, onTint, style = glyph) }
                        }
                        VaultState.FAILED -> drawPath(bang, onTint, style = glyph)
                        VaultState.LOCAL -> drawPath(dashGlyph, onTint, style = glyph)
                    }
                }
            }
        },
    )
}

/**
 * Empty-state art: an open bill tray with two receipts floating down into it.
 */
@Composable
fun EmptyTrayArt(modifier: Modifier = Modifier, size: Dp = 112.dp) {
    val c = Wiz.c
    val t = rememberLoopTime()
    val accent = c.accentInk; val surface = c.surface; val line = c.line
    Spacer(
        modifier.size(size).drawWithCache {
            val s = this.size.minDimension
            val cx = this.size.width / 2
            val a = receiptPath(s * 0.30f, s * 0.38f, 4)
            val b = receiptPath(s * 0.24f, s * 0.30f, 3)
            val thin = Stroke(s * 0.018f, join = StrokeJoin.Round)
            val trayTop = s * 0.66f
            val tray = Path().apply {
                moveTo(cx - s * 0.40f, trayTop); lineTo(cx - s * 0.32f, s * 0.92f)
                lineTo(cx + s * 0.32f, s * 0.92f); lineTo(cx + s * 0.40f, trayTop)
            }
            val trayFill = Path().apply { addPath(tray); close() }
            onDrawBehind {
                val now = t.floatValue
                drawCircle(accent, s * 0.40f, Offset(cx, s * 0.5f), alpha = 0.08f)
                clipRect(bottom = trayTop + s * 0.04f) {
                    translate(cx - s * 0.10f, s * 0.40f + (wave(now, 2.6f) - 0.5f) * s * 0.08f) {
                        rotate(-8f, Offset.Zero) {
                            drawPath(a, Paper); drawPath(a, line, style = thin)
                            for (i in 0..2) drawLine(Ink, Offset(-s * 0.09f, -s * 0.1f + i * s * 0.07f), Offset(s * 0.09f, -s * 0.1f + i * s * 0.07f), s * 0.016f, StrokeCap.Round, alpha = 0.45f)
                        }
                    }
                    translate(cx + s * 0.15f, s * 0.36f + (wave(now, 2.6f, 0.35f) - 0.5f) * s * 0.10f) {
                        rotate(10f, Offset.Zero) {
                            drawPath(b, Paper); drawPath(b, line, style = thin)
                            drawLine(Ink, Offset(-s * 0.07f, -s * 0.05f), Offset(s * 0.07f, -s * 0.05f), s * 0.016f, StrokeCap.Round, alpha = 0.45f)
                        }
                    }
                }
                drawPath(trayFill, surface)
                drawPath(tray, accent, style = Stroke(s * 0.03f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(accent, Offset(cx - s * 0.12f, s * 0.79f), Offset(cx + s * 0.12f, s * 0.79f), s * 0.025f, StrokeCap.Round, alpha = 0.6f)
            }
        },
    )
}
