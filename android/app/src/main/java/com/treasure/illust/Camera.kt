package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Camera(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(340f, 240f) {
            val (c0, c1, c2, _) = palette4(palette)

            // body wash
            drawRoundRect(
                color = c0.copy(alpha = 0.18f),
                topLeft = Offset(40f, 70f),
                size = Size(240f, 130f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            // top hump (pentaprism)
            val hump = Path().apply {
                moveTo(124f, 70f); lineTo(124f, 46f)
                quadraticBezierTo(124f, 38f, 132f, 38f); lineTo(192f, 38f)
                quadraticBezierTo(200f, 38f, 200f, 46f); lineTo(200f, 70f); close()
            }
            drawPath(hump, color = c0.copy(alpha = 0.18f))
            // grip path
            val grip = Path().apply {
                moveTo(40f, 70f); lineTo(68f, 70f); lineTo(72f, 100f)
                lineTo(68f, 180f); lineTo(60f, 196f); lineTo(40f, 196f); close()
            }
            drawPath(grip, color = c0.copy(alpha = 0.28f))

            // lens mount — concentric circles
            val cx = 160f; val cy = 135f
            drawCircle(c1.copy(alpha = 0.32f), 56f, Offset(cx, cy))
            drawCircle(c0.copy(alpha = 0.45f), 46f, Offset(cx, cy))
            drawCircle(c2.copy(alpha = 0.40f), 34f, Offset(cx, cy))
            drawCircle(c0.copy(alpha = 0.65f), 22f, Offset(cx, cy))
            drawCircle(c1.copy(alpha = 0.55f), 12f, Offset(cx, cy))
            // glass highlight
            drawOval(c2.copy(alpha = 0.45f), Offset(146f, 121f), Size(12f, 6f))

            // ink contours
            drawRoundRect(INK, Offset(40f, 70f), Size(240f, 130f),
                cornerRadius = CornerRadius(8f, 8f), style = Stroke(0.9f))
            drawPath(hump, color = INK, style = Stroke(0.9f))
            drawPath(grip, color = INK, style = Stroke(0.9f))
            for (r in listOf(56f, 46f, 34f, 22f, 12f)) {
                drawCircle(INK, r, Offset(cx, cy), style = Stroke(0.9f))
            }
            // shutter button
            drawCircle(INK, 8f, Offset(248f, 86f), style = Stroke(0.9f))
            drawCircle(INK, 3.5f, Offset(248f, 86f), style = Stroke(0.9f))
            // dial
            drawCircle(INK, 13f, Offset(222f, 56f), style = Stroke(0.9f))
            drawCircle(INK.copy(alpha = 0.5f), 9f, Offset(222f, 56f), style = Stroke(0.9f))
            // viewfinder window
            drawRoundRect(INK, Offset(58f, 86f), Size(14f, 10f),
                cornerRadius = CornerRadius(1f, 1f), style = Stroke(0.9f))
            // hot shoe
            drawRect(INK, Offset(148f, 32f), Size(24f, 10f), style = Stroke(0.9f))
            // model engraving lines
            drawLine(INK.copy(alpha = 0.5f), Offset(80f, 172f), Offset(124f, 172f), strokeWidth = 0.9f)
            drawLine(INK.copy(alpha = 0.4f), Offset(80f, 178f), Offset(106f, 178f), strokeWidth = 0.9f)

            // dial ticks
            for (i in 0 until 12) {
                val a = (i / 12f) * (2f * Math.PI.toFloat())
                drawLine(
                    color = INK.copy(alpha = 0.55f),
                    start = Offset(222f + cos(a) * 9f, 56f + sin(a) * 9f),
                    end = Offset(222f + cos(a) * 13f, 56f + sin(a) * 13f),
                    strokeWidth = 0.5f,
                )
            }
        }
    }
}
