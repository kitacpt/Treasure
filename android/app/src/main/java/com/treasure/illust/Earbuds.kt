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
fun Earbuds(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(300f, 220f) {
            val (c0, c1, c2, _) = palette4(palette)

            // case body
            drawRoundRect(
                color = c0.copy(alpha = 0.7f),
                topLeft = Offset(68f, 56f),
                size = Size(164f, 116f),
                cornerRadius = CornerRadius(22f, 22f),
            )
            // hinge line
            drawLine(INK.copy(alpha = 0.65f), Offset(68f, 114f), Offset(232f, 114f), strokeWidth = 0.7f)
            // earbud heads (ovals)
            drawOval(c0.copy(alpha = 0.85f), Offset(96f, 70f), Size(40f, 44f))
            drawOval(c0.copy(alpha = 0.85f), Offset(164f, 70f), Size(40f, 44f))
            drawCircle(c1.copy(alpha = 0.7f), 7f, Offset(116f, 86f))
            drawCircle(c1.copy(alpha = 0.7f), 7f, Offset(184f, 86f))
            // stems
            drawRoundRect(
                color = c0.copy(alpha = 0.85f),
                topLeft = Offset(111f, 108f), size = Size(10f, 46f),
                cornerRadius = CornerRadius(3f, 3f),
            )
            drawRoundRect(
                color = c0.copy(alpha = 0.85f),
                topLeft = Offset(179f, 108f), size = Size(10f, 46f),
                cornerRadius = CornerRadius(3f, 3f),
            )
            // led
            drawCircle(c2.copy(alpha = 0.55f), 2.5f, Offset(150f, 138f))

            // ink contours
            drawRoundRect(
                color = INK,
                topLeft = Offset(68f, 56f), size = Size(164f, 116f),
                cornerRadius = CornerRadius(22f, 22f),
                style = Stroke(0.8f),
            )
            drawOval(INK, Offset(96f, 70f), Size(40f, 44f), style = Stroke(0.8f))
            drawOval(INK, Offset(164f, 70f), Size(40f, 44f), style = Stroke(0.8f))
            drawCircle(INK, 7f, Offset(116f, 86f), style = Stroke(0.8f))
            drawCircle(INK, 7f, Offset(184f, 86f), style = Stroke(0.8f))
            drawRoundRect(INK, Offset(111f, 108f), Size(10f, 46f),
                cornerRadius = CornerRadius(3f, 3f), style = Stroke(0.8f))
            drawRoundRect(INK, Offset(179f, 108f), Size(10f, 46f),
                cornerRadius = CornerRadius(3f, 3f), style = Stroke(0.8f))
        }
    }
}
