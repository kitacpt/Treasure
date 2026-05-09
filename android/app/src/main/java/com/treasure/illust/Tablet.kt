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
fun Tablet(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(240f, 320f) {
            val (c0, _, c2, _) = palette4(palette)

            drawRoundRect(
                color = c0.copy(alpha = 0.55f),
                topLeft = Offset(40f, 28f),
                size = Size(160f, 264f),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawRect(c2.copy(alpha = 0.55f), Offset(50f, 40f), Size(140f, 226f))

            // page text lines
            for (i in 0 until 18) {
                val w = if (i % 5 == 4) 80f else 116f
                drawRect(
                    color = INK.copy(alpha = 0.4f),
                    topLeft = Offset(62f, 56f + i * 12f),
                    size = Size(w, 2f),
                )
            }

            drawRoundRect(
                color = INK,
                topLeft = Offset(40f, 28f), size = Size(160f, 264f),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(0.9f),
            )
            drawRect(INK, Offset(50f, 40f), Size(140f, 226f), style = Stroke(0.9f))
            drawCircle(INK, 6f, Offset(120f, 280f), style = Stroke(0.9f))
        }
    }
}
