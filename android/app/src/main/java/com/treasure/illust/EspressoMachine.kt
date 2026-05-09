package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** 单头家用意式咖啡机 · 侧立面，墙线 + 水箱压顶 + portafilter 把手 + 滴杯。 */
@Composable
fun EspressoMachine(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(220f, 280f) {
            val (c0, _, c2, c3) = palette4(palette)

            // body wash
            drawRoundRect(
                color = c0.copy(alpha = 0.55f),
                topLeft = Offset(54f, 70f),
                size = Size(112f, 138f),
                cornerRadius = CornerRadius(8f, 8f),
            )
            drawRoundRect(
                color = INK,
                topLeft = Offset(54f, 70f),
                size = Size(112f, 138f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 1f),
            )
            // tank on top
            drawRoundRect(
                color = c2.copy(alpha = 0.45f),
                topLeft = Offset(74f, 28f),
                size = Size(72f, 50f),
                cornerRadius = CornerRadius(4f, 4f),
            )
            drawRoundRect(
                color = INK,
                topLeft = Offset(74f, 28f),
                size = Size(72f, 50f),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(0.7f),
            )
            // tank water-line
            drawLine(INK.copy(alpha = 0.4f), Offset(80f, 60f), Offset(140f, 60f), strokeWidth = 0.5f)
            // group head (the brewing column)
            drawRect(c3.copy(alpha = 0.6f), Offset(96f, 110f), Size(28f, 24f))
            drawRect(INK, Offset(96f, 110f), Size(28f, 24f), style = Stroke(0.8f))
            // portafilter handle reaching out to the right
            drawRect(c0, Offset(120f, 122f), Size(56f, 8f), style = Stroke(1f))
            drawRect(c0.copy(alpha = 0.7f), Offset(120f, 122f), Size(56f, 8f))
            drawRoundRect(
                color = INK,
                topLeft = Offset(170f, 116f),
                size = Size(8f, 20f),
                cornerRadius = CornerRadius(2f, 2f),
                style = Stroke(0.7f),
            )
            // dual coffee streams
            for (i in 0 until 2) {
                val x = 104f + i * 12f
                drawLine(INK.copy(alpha = 0.7f), Offset(x, 134f), Offset(x, 158f), strokeWidth = 1f)
            }
            // demitasse cup
            drawRoundRect(
                color = c2.copy(alpha = 0.7f),
                topLeft = Offset(94f, 158f),
                size = Size(32f, 22f),
                cornerRadius = CornerRadius(2f, 2f),
            )
            drawRoundRect(
                color = INK,
                topLeft = Offset(94f, 158f),
                size = Size(32f, 22f),
                cornerRadius = CornerRadius(2f, 2f),
                style = Stroke(0.7f),
            )
            // cup ear
            drawArc(
                color = INK,
                startAngle = -90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(122f, 162f),
                size = Size(10f, 14f),
                style = Stroke(0.7f),
            )
            // base / drip tray slats
            drawRect(c3.copy(alpha = 0.7f), Offset(58f, 192f), Size(104f, 12f))
            drawRect(INK, Offset(58f, 192f), Size(104f, 12f), style = Stroke(0.7f))
            for (i in 0 until 9) {
                val x = 64f + i * 11f
                drawLine(INK.copy(alpha = 0.4f), Offset(x, 194f), Offset(x, 202f), strokeWidth = 0.4f)
            }
            // small steam knob (left)
            drawCircle(c0, radius = 4f, center = Offset(70f, 96f), style = Stroke(0.8f))
            // pressure gauge dial (right)
            drawCircle(c2.copy(alpha = 0.6f), radius = 7f, center = Offset(150f, 96f))
            drawCircle(INK, radius = 7f, center = Offset(150f, 96f), style = Stroke(0.7f))
            drawLine(INK, Offset(150f, 96f), Offset(154f, 92f), strokeWidth = 0.7f)
        }
    }
}
