package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/** 调酒杯 · 三角马天尼杯 + 高脚 + 橄榄签 + 一道液面线。 */
@Composable
fun CocktailGlass(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(220f, 320f) {
            val (c0, _, c2, c3) = palette4(palette)

            // bowl outline (V shape)
            val bowl = Path().apply {
                moveTo(40f, 38f)
                lineTo(180f, 38f)
                lineTo(110f, 168f)
                close()
            }
            drawPath(bowl, c2.copy(alpha = 0.35f))
            drawPath(bowl, INK, style = Stroke(1f))

            // rim double-line (museum etching feel)
            drawLine(INK.copy(alpha = 0.6f), Offset(46f, 44f), Offset(174f, 44f), strokeWidth = 0.5f)

            // liquid surface
            drawLine(c0.copy(alpha = 0.85f), Offset(70f, 84f), Offset(150f, 84f), strokeWidth = 1.5f)
            // tiny meniscus dots
            drawCircle(c0.copy(alpha = 0.7f), radius = 2f, center = Offset(80f, 82f))
            drawCircle(c0.copy(alpha = 0.5f), radius = 1.4f, center = Offset(140f, 81f))

            // pick stick + olive
            drawLine(INK, Offset(90f, 60f), Offset(150f, 100f), strokeWidth = 0.7f)
            drawCircle(c0.copy(alpha = 0.85f), radius = 6f, center = Offset(100f, 68f))
            drawCircle(INK, radius = 6f, center = Offset(100f, 68f), style = Stroke(0.6f))
            drawCircle(c2.copy(alpha = 0.7f), radius = 1.6f, center = Offset(99f, 67f))

            // stem
            drawRect(c3.copy(alpha = 0.6f), Offset(108f, 168f), Size(4f, 96f))
            drawRect(INK, Offset(108f, 168f), Size(4f, 96f), style = Stroke(0.6f))

            // foot (ellipse base)
            drawOval(
                color = c2.copy(alpha = 0.55f),
                topLeft = Offset(60f, 256f),
                size = Size(100f, 16f),
            )
            drawOval(
                color = INK,
                topLeft = Offset(60f, 256f),
                size = Size(100f, 16f),
                style = Stroke(0.7f),
            )
            // foot reflective sliver
            drawLine(INK.copy(alpha = 0.4f), Offset(74f, 268f), Offset(146f, 268f), strokeWidth = 0.4f)
        }
    }
}
