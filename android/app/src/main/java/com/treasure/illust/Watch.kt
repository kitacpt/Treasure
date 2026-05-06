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

@Composable
fun Watch(palette: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(240f, 320f) {
            val (c0, c1, c2, _) = palette4(palette)

            val upperBand = Path().apply {
                moveTo(76f, 0f); lineTo(164f, 0f)
                lineTo(172f, 76f); lineTo(68f, 76f); close()
            }
            val lowerBand = Path().apply {
                moveTo(68f, 244f); lineTo(172f, 244f)
                lineTo(164f, 320f); lineTo(76f, 320f); close()
            }
            drawPath(upperBand, color = c2.copy(alpha = 0.55f))
            drawPath(lowerBand, color = c2.copy(alpha = 0.55f))

            // case
            drawRoundRect(
                color = c0.copy(alpha = 0.6f),
                topLeft = Offset(44f, 64f), size = Size(152f, 192f),
                cornerRadius = CornerRadius(34f, 34f),
            )
            drawRoundRect(
                color = c1.copy(alpha = 0.7f),
                topLeft = Offset(52f, 74f), size = Size(136f, 172f),
                cornerRadius = CornerRadius(26f, 26f),
            )
            // face — solid dark
            drawRoundRect(
                color = Color(0xFF0A0A0A),
                topLeft = Offset(60f, 82f), size = Size(120f, 156f),
                cornerRadius = CornerRadius(20f, 20f),
            )

            // ink contours
            drawPath(upperBand, color = INK, style = Stroke(0.8f))
            drawPath(lowerBand, color = INK, style = Stroke(0.8f))
            drawRoundRect(
                color = INK,
                topLeft = Offset(44f, 64f), size = Size(152f, 192f),
                cornerRadius = CornerRadius(34f, 34f),
                style = Stroke(0.8f),
            )
            drawRoundRect(
                color = INK,
                topLeft = Offset(60f, 82f), size = Size(120f, 156f),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(0.8f),
            )
            // crown
            drawRoundRect(
                color = INK,
                topLeft = Offset(194f, 142f), size = Size(12f, 22f),
                cornerRadius = CornerRadius(2f, 2f),
                style = Stroke(0.8f),
            )
        }
    }
}
