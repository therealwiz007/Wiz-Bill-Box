package com.wizlabs.billbox.ui.mascot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** Tally's immutable geometry in a 100 x 100 box, built once. Same shapes as store-assets/mascot/tally.svg. */
class TallyArt {
    val kraft = Color(0xFFE8C9A0); val shade = Color(0xFFC9A27A); val lidColor = Color(0xFFD9B88F)
    val label = Color(0xFFFFFFFF); val ink = Color(0xFF111A16); val paperLine = Color(0xFFE3E0D6)
    val emerald = Color(0xFF0E9F6E); val bellColor = Color(0xFFD99A1E); val drop = Color(0xFF60A5FA)

    val body = Path().apply {
        moveTo(20f, 44f); lineTo(80f, 44f); lineTo(77f, 88f); quadraticTo(77f, 90f, 75f, 90f)
        lineTo(25f, 90f); quadraticTo(23f, 90f, 23f, 88f); close()
    }
    val bodyShade = Path().apply {
        moveTo(68f, 44f); lineTo(80f, 44f); lineTo(77f, 88f); quadraticTo(77f, 90f, 75f, 90f); lineTo(70f, 90f); close()
    }
    /** Lid in its own coordinates; the hinge is its bottom-left corner (0, 10). */
    val lid = Path().apply { addRoundRect(RoundRect(Rect(0f, 0f, 66f, 10f), CornerRadius(4f))) }
    val receipt = Path().apply {
        // zig-zag top edge, straight sides; the lower part hides behind the lid and body
        moveTo(40f, 54f); lineTo(40f, 20f)
        var x = 40f
        var up = false
        while (x < 60f) { x += 4f; lineTo(x, if (up) 20f else 17f); up = !up }
        lineTo(60f, 54f); close()
    }
    val labelPanel = Path().apply { addRoundRect(RoundRect(Rect(33f, 56f, 67f, 71f), CornerRadius(3f))) }
    val footL = Path().apply { addRoundRect(RoundRect(Rect(30f, 90f, 40f, 94.5f), CornerRadius(2f))) }
    val footR = Path().apply { addRoundRect(RoundRect(Rect(60f, 90f, 70f, 94.5f), CornerRadius(2f))) }
    val bell = Path().apply {
        moveTo(-6f, 12f); quadraticTo(-6f, 0f, 0f, 0f); quadraticTo(6f, 0f, 6f, 12f); lineTo(8f, 14f); lineTo(-8f, 14f); close()
    }
    val check = Path().apply { moveTo(-3f, 0f); lineTo(-0.8f, 2.4f); lineTo(3.2f, -2.4f) }

    private fun curve(x0: Float, y0: Float, cx: Float, cy: Float, x1: Float, y1: Float, close: Boolean = false) =
        Path().apply { moveTo(x0, y0); quadraticTo(cx, cy, x1, y1); if (close) close() }
    val contentL = curve(38.8f, 61.4f, 42f, 65.2f, 45.2f, 61.4f)
    val contentR = curve(54.8f, 61.4f, 58f, 65.2f, 61.2f, 61.4f)
    val smile = curve(46.6f, 66.6f, 50f, 70.2f, 53.4f, 66.6f)
    val big = curve(46.4f, 66.2f, 50f, 72.6f, 53.6f, 66.2f, close = true)
    val frown = curve(47f, 69f, 50f, 65.8f, 53f, 69f)

    val eyeL = Offset(42f, 62f)
    val eyeR = Offset(58f, 62f)
}
