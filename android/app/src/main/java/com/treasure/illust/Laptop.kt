package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun Laptop(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(340f, 240f) {
            val (c0, c1, c2, _) = palette4(palette)

            // lid
            val lid = Path().apply {
                moveTo(76f, 38f); lineTo(262f, 38f)
                lineTo(272f, 156f); lineTo(66f, 156f); close()
            }
            drawPath(lid, color = c0.copy(alpha = 0.5f))

            // screen
            val screen = Path().apply {
                moveTo(82f, 46f); lineTo(256f, 46f)
                lineTo(264f, 150f); lineTo(74f, 150f); close()
            }
            drawPath(screen, color = c1.copy(alpha = 0.7f))

            // screen content blocks
            drawRect(c2.copy(alpha = 0.45f), Offset(98f, 62f), Size(58f, 6f))
            drawRect(c2.copy(alpha = 0.45f), Offset(98f, 74f), Size(120f, 3f))
            drawRect(c2.copy(alpha = 0.45f), Offset(98f, 82f), Size(86f, 3f))
            drawRect(c2.copy(alpha = 0.45f), Offset(98f, 100f), Size(36f, 28f))
            drawRect(c2.copy(alpha = 0.45f), Offset(138f, 100f), Size(36f, 28f))
            drawRect(c2.copy(alpha = 0.45f), Offset(178f, 100f), Size(36f, 28f))

            // hinge
            val hinge = Path().apply {
                moveTo(66f, 156f); lineTo(272f, 156f)
                lineTo(280f, 162f); lineTo(60f, 162f); close()
            }
            drawPath(hinge, color = c1.copy(alpha = 0.7f))

            // base
            val base = Path().apply {
                moveTo(30f, 162f); lineTo(308f, 162f)
                lineTo(320f, 200f); lineTo(20f, 200f); close()
            }
            drawPath(base, color = c0.copy(alpha = 0.42f))

            // trackpad
            drawRect(c1.copy(alpha = 0.55f), Offset(124f, 180f), Size(86f, 3f))

            // ink contours
            drawPath(lid, color = INK, style = Stroke(0.9f))
            drawPath(screen, color = INK, style = Stroke(0.9f))
            drawPath(hinge, color = INK, style = Stroke(0.9f))
            drawPath(base, color = INK, style = Stroke(0.9f))
            drawRect(INK.copy(alpha = 0.6f), Offset(124f, 180f), Size(86f, 3f), style = Stroke(0.9f))

            // keyboard rows
            for (r in 0 until 4) {
                drawLine(
                    color = INK.copy(alpha = 0.5f),
                    start = Offset(50f + r * 2f, 170f + r * 4f),
                    end = Offset(290f - r * 4f, 170f + r * 4f),
                    strokeWidth = 0.4f,
                )
            }
        }
    }
}
