package com.wizlabs.billbox.ui.art

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.wizlabs.billbox.ui.anim.LocalReduceMotion
import com.wizlabs.billbox.ui.anim.rememberHaptics
import com.wizlabs.billbox.ui.theme.Wiz
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shown after a bill is saved: a receipt prints out of a slot, a yellow "saved" stamp thumps onto it
 * (with a haptic tap and a few sparks), then it all drops away. Calls [onDone] after ~1.7 s.
 */
@Composable
fun ReceiptPrint(label: String, onDone: () -> Unit) {
    val c = Wiz.c
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val print = remember { Animatable(0f) }
    val stamp = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    val sparks = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduce) { print.snapTo(1f); stamp.snapTo(1f); tick.snapTo(1f); delay(800); onDone(); return@LaunchedEffect }
        print.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        launch { stamp.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 520f)) }
        delay(110)
        haptics.confirm()
        launch { sparks.animateTo(1f, tween(620, easing = FastOutSlowInEasing)) }
        tick.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        delay(560)
        leave.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        onDone()
    }
    val paperCol = Color(0xFFFFF8E6); val ink = Color(0xFF3A3630)
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - leave.value }, contentAlignment = Alignment.Center) {
        // dim the screen a little so the receipt reads clearly
        Spacer(Modifier.fillMaxSize().drawBehind { drawRect(Color.Black, alpha = 0.35f * (print.value.coerceAtMost(1f))) })
        Box(Modifier.size(240.dp, 300.dp).graphicsLayer { translationY = leave.value * 120f }, contentAlignment = Alignment.Center) {
            Spacer(
                Modifier.fillMaxSize().drawWithCache {
                    val w = size.width; val h = size.height
                    val slotW = w * 0.78f; val slotY = h * 0.14f
                    val paperW = w * 0.6f; val paperH = h * 0.62f
                    val paper = receiptPath(paperW, paperH, 7)
                    val outline = Stroke(2f)
                    val stampR = w * 0.15f
                    val stampC = Offset(paperW * 0.18f, paperH * 0.16f) // relative to paper centre
                    val check = Path().apply {
                        moveTo(stampC.x - stampR * 0.42f, stampC.y + stampR * 0.02f)
                        lineTo(stampC.x - stampR * 0.10f, stampC.y + stampR * 0.34f)
                        lineTo(stampC.x + stampR * 0.46f, stampC.y - stampR * 0.30f)
                    }
                    val pm = PathMeasure().apply { setPath(check, false) }
                    val seg = Path()
                    val tickStroke = Stroke(stampR * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val ring = Stroke(stampR * 0.08f)
                    onDrawBehind {
                        val pr = print.value
                        // paper emerges downward from the slot (clipped at the slot)
                        val py = slotY - paperH / 2 + pr * (paperH / 2 + h * 0.06f)
                        clipRect(top = slotY) {
                            translate(w / 2, py) {
                                drawPath(paper, paperCol)
                                drawPath(paper, c.line, style = outline)
                                // shop header, items, dotted rule, total
                                drawRoundRect(ink, Offset(-paperW * 0.32f, -paperH * 0.40f), Size(paperW * 0.36f, paperH * 0.045f), CornerRadius(4f), alpha = 0.8f)
                                for (i in 0..2) {
                                    val y = -paperH * 0.22f + i * paperH * 0.1f
                                    drawLine(ink, Offset(-paperW * 0.32f, y), Offset(paperW * (0.0f - i * 0.05f), y), 5f, StrokeCap.Round, alpha = 0.45f)
                                    drawLine(ink, Offset(paperW * 0.18f, y), Offset(paperW * 0.32f, y), 5f, StrokeCap.Round, alpha = 0.45f)
                                }
                                drawLine(ink, Offset(-paperW * 0.32f, paperH * 0.12f), Offset(paperW * 0.32f, paperH * 0.12f), 2f, alpha = 0.4f)
                                drawRoundRect(c.accent, Offset(-paperW * 0.34f, paperH * 0.18f), Size(paperW * 0.68f, paperH * 0.09f), CornerRadius(6f))
                                drawLine(ink, Offset(-paperW * 0.28f, paperH * 0.225f), Offset(-paperW * 0.05f, paperH * 0.225f), 6f, StrokeCap.Round, alpha = 0.8f)
                                drawLine(ink, Offset(paperW * 0.12f, paperH * 0.225f), Offset(paperW * 0.28f, paperH * 0.225f), 6f, StrokeCap.Round, alpha = 0.8f)

                                // stamp: drops from large to size, slightly rotated
                                val st = stamp.value
                                if (st > 0f) {
                                    val sc = 1.9f - 0.9f * st
                                    rotate(-14f, stampC) {
                                        scale(sc, stampC) {
                                            drawCircle(c.accent, stampR, stampC, alpha = st.coerceIn(0f, 1f))
                                            drawCircle(c.onAccent, stampR * 0.82f, stampC, alpha = 0.6f * st.coerceIn(0f, 1f), style = ring)
                                            seg.reset()
                                            pm.getSegment(0f, pm.length * tick.value, seg, true)
                                            drawPath(seg, c.onAccent, style = tickStroke)
                                        }
                                    }
                                }
                                // sparks from the impact
                                val sp = sparks.value
                                if (sp > 0f && sp < 1f) for (i in 0 until 8) {
                                    val a = i / 8f * 6.283f + 0.4f
                                    val d = stampR * (1.1f + 1.3f * sp)
                                    drawCircle(if (i % 2 == 0) c.accent else c.accent2, (if (i % 3 == 0) 7f else 5f) * (1f - sp * 0.6f),
                                        Offset(stampC.x + cos(a) * d, stampC.y + sin(a) * d), alpha = 1f - sp)
                                }
                            }
                        }
                        // the printer slot
                        drawRoundRect(c.surface2, Offset((w - slotW) / 2, slotY - h * 0.05f), Size(slotW, h * 0.07f), CornerRadius(h * 0.035f))
                        drawRoundRect(c.accent, Offset((w - slotW) / 2, slotY - h * 0.05f), Size(slotW, h * 0.07f), CornerRadius(h * 0.035f), style = outline)
                        drawLine(Color.Black, Offset(w / 2 - paperW * 0.55f, slotY), Offset(w / 2 + paperW * 0.55f, slotY), 6f, StrokeCap.Round, alpha = 0.7f)
                        drawLine(c.accent, Offset(w / 2 - slotW * 0.42f, slotY - h * 0.03f), Offset(w / 2 - slotW * 0.30f, slotY - h * 0.03f), 4f, StrokeCap.Round, alpha = 0.6f)
                    }
                },
            )
            Text(
                label, style = MaterialTheme.typography.titleMedium, color = c.text,
                modifier = Modifier.align(Alignment.BottomCenter).graphicsLayer { alpha = tick.value; translationY = (1f - tick.value) * 24f }
                    .background(c.surface, CircleShape).padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
    }
}
