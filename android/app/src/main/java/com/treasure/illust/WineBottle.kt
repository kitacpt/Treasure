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

/** 一瓶酒 · 玻璃瓶身 + 锡帽 + 标签纸；通用酒瓶（红 / 白 / 烈酒都能挪用）。 */
@Composable
fun WineBottle(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(180f, 360f) {
            val (c0, _, c2, c3) = palette4(palette)
            // bottle silhouette: neck → shoulder → body → punt
            val bottle = Path().apply {
                moveTo(78f, 30f)
                lineTo(102f, 30f)
                lineTo(102f, 78f)
                cubicTo(102f, 90f, 130f, 100f, 130f, 130f)
                lineTo(130f, 314f)
                cubicTo(130f, 326f, 120f, 332f, 90f, 332f)
                cubicTo(60f, 332f, 50f, 326f, 50f, 314f)
                lineTo(50f, 130f)
                cubicTo(50f, 100f, 78f, 90f, 78f, 78f)
                close()
            }
            drawPath(bottle, c0.copy(alpha = 0.55f))
            drawPath(bottle, INK, style = Stroke(1f))

            // foil capsule (top of neck)
            drawRect(c0, Offset(78f, 30f), Size(24f, 26f))
            drawRect(INK, Offset(78f, 30f), Size(24f, 26f), style = Stroke(0.7f))
            // capsule horizontal seam
            drawLine(INK.copy(alpha = 0.6f), Offset(78f, 50f), Offset(102f, 50f), strokeWidth = 0.5f)

            // label paper
            drawRoundRect(
                color = c2.copy(alpha = 0.85f),
                topLeft = Offset(56f, 180f),
                size = Size(68f, 96f),
                cornerRadius = CornerRadius(2f, 2f),
            )
            drawRoundRect(
                color = INK,
                topLeft = Offset(56f, 180f),
                size = Size(68f, 96f),
                cornerRadius = CornerRadius(2f, 2f),
                style = Stroke(0.7f),
            )
            // label decorative double-line
            drawLine(INK.copy(alpha = 0.7f), Offset(64f, 196f), Offset(116f, 196f), strokeWidth = 0.7f)
            drawLine(INK.copy(alpha = 0.4f), Offset(64f, 200f), Offset(116f, 200f), strokeWidth = 0.4f)
            // 3 lines of pretend type
            for (i in 0 until 3) {
                val y = 218f + i * 12f
                drawLine(INK.copy(alpha = 0.5f), Offset(70f, y), Offset(110f, y), strokeWidth = 0.4f)
            }
            // tiny vintage roman numeral box
            drawRect(INK, Offset(78f, 256f), Size(24f, 12f), style = Stroke(0.5f))

            // bottle highlight stripe along left
            drawLine(c3.copy(alpha = 0.55f), Offset(58f, 130f), Offset(58f, 308f), strokeWidth = 2f)

            // base / shadow
            drawOval(
                color = INK.copy(alpha = 0.2f),
                topLeft = Offset(46f, 332f),
                size = Size(88f, 6f),
            )
        }
    }
}
