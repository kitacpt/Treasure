package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun Shoes(palette: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(340f, 220f) {
            val (c0, c1, c2, _) = palette4(palette)

            val sole = Path().apply {
                moveTo(30f, 156f); lineTo(300f, 144f)
                quadraticBezierTo(314f, 148f, 312f, 166f)
                lineTo(300f, 178f); lineTo(42f, 178f)
                quadraticBezierTo(26f, 170f, 30f, 156f); close()
            }
            val midsole = Path().apply {
                moveTo(40f, 134f); lineTo(300f, 130f)
                quadraticBezierTo(310f, 132f, 306f, 146f)
                lineTo(40f, 156f); close()
            }
            val upper = Path().apply {
                moveTo(50f, 132f)
                quadraticBezierTo(60f, 76f, 134f, 68f); lineTo(218f, 60f)
                quadraticBezierTo(272f, 60f, 296f, 100f); lineTo(302f, 132f); close()
            }
            val tongue = Path().apply {
                moveTo(154f, 70f); lineTo(218f, 60f)
                lineTo(228f, 100f); lineTo(164f, 110f); close()
            }

            drawPath(sole, color = c1.copy(alpha = 0.55f))
            drawPath(midsole, color = c0.copy(alpha = 0.7f))
            drawPath(upper, color = c0.copy(alpha = 0.55f))
            drawPath(tongue, color = c0.copy(alpha = 0.4f))

            // ink contours
            drawPath(sole, color = INK, style = Stroke(0.9f))
            drawPath(midsole, color = INK, style = Stroke(0.9f))
            drawPath(upper, color = INK, style = Stroke(0.9f))
            drawPath(tongue, color = INK, style = Stroke(0.9f))

            // swoosh-like accent
            val swoosh = Path().apply {
                moveTo(70f, 124f)
                quadraticBezierTo(140f, 94f, 218f, 100f)
            }
            drawPath(swoosh, color = c2.copy(alpha = 0.7f), style = Stroke(5f))
            drawPath(swoosh, color = INK.copy(alpha = 0.5f), style = Stroke(0.5f))

            // laces
            for (i in 0 until 5) {
                drawLine(
                    color = INK.copy(alpha = 0.7f),
                    start = Offset(176f - i * 4f, 84f + i * 6f),
                    end = Offset(222f - i * 4f, 80f + i * 6f),
                    strokeWidth = 1f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
