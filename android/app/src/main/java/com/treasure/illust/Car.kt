package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Car(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(380f, 220f) {
            val (c0, c1, c2, c3) = palette4(palette)

            // shadow
            drawOval(INK.copy(alpha = 0.12f), Offset(190f - 158f, 190f - 5f), Size(316f, 10f))

            // body
            val body = Path().apply {
                moveTo(40f, 152f); lineTo(60f, 120f)
                quadraticBezierTo(82f, 98f, 122f, 92f); lineTo(162f, 76f)
                quadraticBezierTo(204f, 70f, 246f, 76f); lineTo(286f, 92f)
                quadraticBezierTo(328f, 96f, 342f, 124f); lineTo(350f, 152f)
                lineTo(350f, 168f); lineTo(40f, 168f); close()
            }
            drawPath(body, color = c0.copy(alpha = 0.42f))

            // roof
            val roof = Path().apply {
                moveTo(122f, 92f); quadraticBezierTo(162f, 76f, 218f, 76f)
                lineTo(264f, 90f); quadraticBezierTo(280f, 94f, 290f, 116f)
                lineTo(290f, 128f); lineTo(122f, 128f); close()
            }
            drawPath(roof, color = c0.copy(alpha = 0.55f))

            // windows
            val windows = Path().apply {
                moveTo(132f, 100f); quadraticBezierTo(162f, 84f, 200f, 82f)
                lineTo(246f, 92f); quadraticBezierTo(260f, 96f, 268f, 116f)
                lineTo(268f, 124f); lineTo(132f, 124f); close()
            }
            drawPath(windows, color = c2.copy(alpha = 0.32f))

            // ink contours
            drawPath(body, color = INK, style = Stroke(0.9f))
            drawPath(roof, color = INK, style = Stroke(0.9f))
            drawPath(windows, color = INK, style = Stroke(0.9f))
            // window divider
            drawLine(INK, Offset(200f, 82f), Offset(200f, 124f), strokeWidth = 0.9f)
            // beltline
            drawLine(INK.copy(alpha = 0.55f), Offset(60f, 128f), Offset(342f, 128f), strokeWidth = 0.9f)
            // door cuts
            drawLine(INK.copy(alpha = 0.55f), Offset(158f, 128f), Offset(162f, 168f), strokeWidth = 0.9f)
            drawLine(INK.copy(alpha = 0.55f), Offset(244f, 128f), Offset(248f, 168f), strokeWidth = 0.9f)

            // lights
            drawOval(c2.copy(alpha = 0.6f), Offset(42f, 142f), Size(20f, 12f))
            drawOval(c2.copy(alpha = 0.7f), Offset(329f, 143f), Size(18f, 10f))
            drawOval(INK, Offset(42f, 142f), Size(20f, 12f), style = Stroke(0.6f))
            drawOval(INK, Offset(329f, 143f), Size(18f, 10f), style = Stroke(0.6f))

            // wheels
            for (cx in listOf(110f, 290f)) {
                drawCircle(c1.copy(alpha = 0.85f), 22f, Offset(cx, 170f))
                drawCircle(INK, 22f, Offset(cx, 170f), style = Stroke(0.9f))
                drawCircle(c0.copy(alpha = 0.6f), 13f, Offset(cx, 170f))
                drawCircle(INK, 13f, Offset(cx, 170f), style = Stroke(0.6f))
                drawCircle(c3, 4f, Offset(cx, 170f))
                for (i in 0 until 5) {
                    val a = (i / 5f) * (2f * Math.PI.toFloat()) - Math.PI.toFloat() / 2f
                    drawLine(
                        color = INK.copy(alpha = 0.7f),
                        start = Offset(cx, 170f),
                        end = Offset(cx + cos(a) * 12f, 170f + sin(a) * 12f),
                        strokeWidth = 0.6f,
                    )
                }
            }
        }
    }
}
