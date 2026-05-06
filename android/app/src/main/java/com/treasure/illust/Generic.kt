package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Catch-all when an item's heroVector has no specific Composable yet.
 * Renders a labeled blank museum plate so the layout still feels intentional.
 */
@Composable
fun Generic(palette: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(240f, 240f) {
            val (c0, c1, c2, _) = palette4(palette)

            // outer plate
            drawRect(c2.copy(alpha = 0.45f), Offset(30f, 40f), Size(180f, 160f))
            drawRect(INK, Offset(30f, 40f), Size(180f, 160f), style = Stroke(0.9f))
            // inner cartouche
            drawRect(c1.copy(alpha = 0.35f), Offset(60f, 78f), Size(120f, 84f))
            drawRect(INK, Offset(60f, 78f), Size(120f, 84f), style = Stroke(0.7f))
            // central swatch (palette c0)
            drawRect(c0.copy(alpha = 0.85f), Offset(90f, 100f), Size(60f, 40f))
            // accent corner
            drawRect(c1, Offset(150f - 4f, 140f - 4f), Size(4f, 4f))
        }
    }
}
