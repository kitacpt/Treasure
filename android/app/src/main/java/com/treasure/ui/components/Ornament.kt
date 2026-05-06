package com.treasure.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.treasure.theme.LocalTreasureColors

/**
 * Decorative compass rule from prototype Portal.
 * Two thin lines + concentric circles + diamonds top/bottom.
 */
@Composable
fun Ornament(
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
    ) {
        val w = size.width
        val h = size.height
        val cy = h / 2f
        val centerX = w / 2f
        val ringR = 5.5f.dp.toPx()
        val gap = 14.dp.toPx()

        val lineColor = colors.line.copy(alpha = 0.6f)
        val sub = colors.sub
        val ink = colors.ink

        // left line
        drawLine(
            color = lineColor,
            start = Offset(20f, cy),
            end = Offset(centerX - ringR - gap, cy),
            strokeWidth = 0.6f.dp.toPx(),
        )
        // right line
        drawLine(
            color = lineColor,
            start = Offset(centerX + ringR + gap, cy),
            end = Offset(w - 20f, cy),
            strokeWidth = 0.6f.dp.toPx(),
        )

        // outer ring
        drawCircle(
            color = sub,
            radius = ringR,
            center = Offset(centerX, cy),
            style = Stroke(width = 0.7f.dp.toPx()),
        )
        // inner dot
        drawCircle(
            color = sub,
            radius = 1.2f.dp.toPx(),
            center = Offset(centerX, cy),
        )

        // diamonds top + bottom
        val dSize = 2.6f.dp.toPx()
        listOf(cy - 10.dp.toPx(), cy + 10.dp.toPx()).forEach { y ->
            val path = Path().apply {
                moveTo(centerX, y - dSize)
                lineTo(centerX + dSize, y)
                lineTo(centerX, y + dSize)
                lineTo(centerX - dSize, y)
                close()
            }
            drawPath(path, color = ink.copy(alpha = 0.5f))
        }
    }
}
