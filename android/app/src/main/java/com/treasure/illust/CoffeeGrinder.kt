package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** 锥形电动磨豆机 · 顶部豆仓玻璃 + 磨刀马达 + 出粉口 + 接粉杯 */
@Composable
fun CoffeeGrinder(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(220f, 320f) {
            val (c0, _, c2, c3) = palette4(palette)
            // hopper (transparent dome)
            val hopperPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(78f, 30f)
                cubicTo(78f, 14f, 142f, 14f, 142f, 30f)
                lineTo(146f, 90f)
                lineTo(74f, 90f)
                close()
            }
            drawPath(hopperPath, c2.copy(alpha = 0.35f))
            drawPath(hopperPath, INK, style = Stroke(0.8f))
            // hopper lid line
            drawLine(INK.copy(alpha = 0.4f), Offset(78f, 38f), Offset(142f, 38f), strokeWidth = 0.4f)
            // coffee beans inside (3 little bumps)
            for (i in 0 until 5) {
                val x = 86f + (i % 3) * 16f
                val y = 56f + (i / 3) * 14f
                drawOval(
                    color = c0.copy(alpha = 0.75f),
                    topLeft = Offset(x, y),
                    size = Size(10f, 7f),
                )
                drawLine(INK.copy(alpha = 0.5f), Offset(x + 1f, y + 4f), Offset(x + 9f, y + 3f), strokeWidth = 0.4f)
            }
            // body — main brushed-metal cylinder
            drawRect(c0.copy(alpha = 0.6f), Offset(64f, 92f), Size(92f, 90f))
            drawRect(INK, Offset(64f, 92f), Size(92f, 90f), style = Stroke(1f))
            // grind-setting collar with notches
            drawRect(c3.copy(alpha = 0.7f), Offset(60f, 98f), Size(100f, 12f))
            drawRect(INK, Offset(60f, 98f), Size(100f, 12f), style = Stroke(0.7f))
            for (i in 0 until 14) {
                val x = 64f + i * 7f
                drawLine(INK.copy(alpha = 0.45f), Offset(x, 100f), Offset(x, 108f), strokeWidth = 0.4f)
            }
            // dial / knob
            drawCircle(c2.copy(alpha = 0.6f), radius = 16f, center = Offset(110f, 138f))
            drawCircle(INK, radius = 16f, center = Offset(110f, 138f), style = Stroke(0.8f))
            drawLine(INK, Offset(110f, 138f), Offset(118f, 130f), strokeWidth = 1f)
            // chute below
            drawRect(c3.copy(alpha = 0.7f), Offset(98f, 182f), Size(24f, 18f))
            drawRect(INK, Offset(98f, 182f), Size(24f, 18f), style = Stroke(0.7f))
            // dosing cup
            drawRoundRect(
                color = c2.copy(alpha = 0.55f),
                topLeft = Offset(82f, 204f),
                size = Size(56f, 60f),
                cornerRadius = CornerRadius(4f, 4f),
            )
            drawRoundRect(
                color = INK,
                topLeft = Offset(82f, 204f),
                size = Size(56f, 60f),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(0.7f),
            )
            // grounds inside cup
            drawLine(INK.copy(alpha = 0.5f), Offset(86f, 250f), Offset(134f, 250f), strokeWidth = 0.4f)
            // base shadow
            drawRect(INK.copy(alpha = 0.25f), Offset(60f, 270f), Size(100f, 4f))
        }
    }
}
