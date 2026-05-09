package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/** 一颗咖啡豆 · 半剖式植物图鉴风 */
@Composable
fun CoffeeBean(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(220f, 240f) {
            val (c0, _, c2, c3) = palette4(palette)

            // shadow halo behind
            drawOval(
                color = c2.copy(alpha = 0.2f),
                topLeft = Offset(38f, 70f),
                size = Size(144f, 100f),
            )
            // bean outer outline (bowed oval)
            val bean = Path().apply {
                moveTo(110f, 38f)
                cubicTo(178f, 50f, 188f, 162f, 110f, 198f)
                cubicTo(32f, 162f, 42f, 50f, 110f, 38f)
                close()
            }
            drawPath(bean, c0.copy(alpha = 0.55f))
            drawPath(bean, INK, style = Stroke(1f))

            // central crease (the signature furrow)
            drawLine(INK.copy(alpha = 0.85f), Offset(110f, 50f), Offset(110f, 188f), strokeWidth = 1.4f)
            // tiny side hatches inside the crease
            for (i in 0 until 9) {
                val y = 60f + i * 14f
                drawLine(INK.copy(alpha = 0.45f), Offset(105f, y), Offset(115f, y), strokeWidth = 0.4f)
            }

            // light highlight strip on right side
            val highlight = Path().apply {
                moveTo(132f, 70f)
                cubicTo(146f, 90f, 152f, 138f, 134f, 178f)
            }
            drawPath(highlight, c3.copy(alpha = 0.5f), style = Stroke(2f))

            // botanical-plate corner mark
            drawLine(INK.copy(alpha = 0.5f), Offset(40f, 220f), Offset(60f, 220f), strokeWidth = 0.4f)
            drawLine(INK.copy(alpha = 0.5f), Offset(180f, 220f), Offset(160f, 220f), strokeWidth = 0.4f)
        }
    }
}
