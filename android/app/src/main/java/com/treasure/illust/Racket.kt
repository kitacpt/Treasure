package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun Racket(palette: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(220f, 340f) {
            val (c0, _, c2, c3) = palette4(palette)

            // head wash
            drawOval(
                color = c2.copy(alpha = 0.18f),
                topLeft = Offset(110f - 64f, 92f - 74f),
                size = Size(128f, 148f),
            )
            // strings (vertical)
            for (i in 0 until 18) {
                val x = 50f + i * 7f
                drawLine(
                    color = INK.copy(alpha = 0.45f),
                    start = Offset(x, 14f),
                    end = Offset(x, 172f),
                    strokeWidth = 0.4f,
                )
            }
            // strings (horizontal)
            for (i in 0 until 20) {
                val y = 22f + i * 7f
                drawLine(
                    color = INK.copy(alpha = 0.45f),
                    start = Offset(40f, y),
                    end = Offset(180f, y),
                    strokeWidth = 0.4f,
                )
            }
            // head outer ring
            drawOval(
                color = INK,
                topLeft = Offset(110f - 64f, 92f - 74f),
                size = Size(128f, 148f),
                style = Stroke(width = 1f),
            )
            // head color band
            drawOval(
                color = c0.copy(alpha = 0.55f),
                topLeft = Offset(110f - 64f, 92f - 74f),
                size = Size(128f, 148f),
                style = Stroke(width = 2f),
            )
            // T-joint
            drawRect(c0.copy(alpha = 0.7f), Offset(100f, 160f), Size(20f, 14f))
            drawRect(INK, Offset(100f, 160f), Size(20f, 14f), style = Stroke(0.7f))
            // shaft
            drawRect(c0.copy(alpha = 0.55f), Offset(106f, 172f), Size(8f, 84f))
            drawRect(INK, Offset(106f, 172f), Size(8f, 84f), style = Stroke(0.7f))
            // grip
            drawRoundRect(
                color = c3.copy(alpha = 0.5f),
                topLeft = Offset(98f, 252f),
                size = Size(24f, 74f),
                cornerRadius = CornerRadius(2f, 2f),
            )
            drawRoundRect(
                color = INK,
                topLeft = Offset(98f, 252f),
                size = Size(24f, 74f),
                cornerRadius = CornerRadius(2f, 2f),
                style = Stroke(0.7f),
            )
            // grip wrap diagonals
            for (i in 0 until 12) {
                drawLine(
                    color = INK.copy(alpha = 0.4f),
                    start = Offset(98f, 258f + i * 6f),
                    end = Offset(122f, 254f + i * 6f),
                    strokeWidth = 0.4f,
                )
            }
            // grip cap
            drawRect(INK, Offset(96f, 324f), Size(28f, 6f), style = Stroke(0.7f))
        }
    }
}
