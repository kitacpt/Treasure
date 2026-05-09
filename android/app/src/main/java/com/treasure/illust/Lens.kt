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
fun Lens(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(340f, 240f) {
            val (c0, c1, c2, _) = palette4(palette)

            // mount
            drawRect(c1.copy(alpha = 0.4f), Offset(60f, 92f), Size(14f, 68f))
            // barrel sections
            drawRect(c0.copy(alpha = 0.22f), Offset(74f, 84f), Size(46f, 84f))
            drawRect(c0.copy(alpha = 0.32f), Offset(120f, 78f), Size(34f, 96f))
            drawRect(c0.copy(alpha = 0.22f), Offset(154f, 70f), Size(56f, 112f))
            drawRect(c0.copy(alpha = 0.36f), Offset(210f, 68f), Size(22f, 116f))
            // hood
            val hood = Path().apply {
                moveTo(232f, 68f); lineTo(274f, 60f)
                lineTo(274f, 192f); lineTo(232f, 184f); close()
            }
            drawPath(hood, color = c1.copy(alpha = 0.32f))
            // front element
            drawOval(c0.copy(alpha = 0.6f), Offset(263f, 62f), Size(14f, 128f))
            drawOval(c2.copy(alpha = 0.5f), Offset(270f, 80f), Size(6f, 80f))

            // ink contours
            drawRect(INK, Offset(60f, 92f), Size(14f, 68f), style = Stroke(0.8f))
            drawRect(INK, Offset(74f, 84f), Size(46f, 84f), style = Stroke(0.8f))
            drawRect(INK, Offset(120f, 78f), Size(34f, 96f), style = Stroke(0.8f))
            drawRect(INK, Offset(154f, 70f), Size(56f, 112f), style = Stroke(0.8f))
            drawRect(INK, Offset(210f, 68f), Size(22f, 116f), style = Stroke(0.8f))
            drawPath(hood, color = INK, style = Stroke(0.8f))
            drawOval(INK, Offset(263f, 62f), Size(14f, 128f), style = Stroke(0.8f))

            // focus ring grooves
            for (i in 0 until 14) {
                val x = 158f + i * 3.5f
                drawLine(
                    color = INK.copy(alpha = 0.55f),
                    start = Offset(x, 74f),
                    end = Offset(x, 178f),
                    strokeWidth = 0.4f,
                )
            }
            // aperture / red ring
            drawRect(c2.copy(alpha = 0.7f), Offset(150f, 68f), Size(3f, 116f))
        }
    }
}
