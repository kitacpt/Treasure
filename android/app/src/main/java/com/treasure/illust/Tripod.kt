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
fun Tripod(palette: List<Color>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(240f, 340f) {
            val (c0, c1, c2, _) = palette4(palette)

            // head plate
            drawRect(c0.copy(alpha = 0.55f), Offset(100f, 40f), Size(40f, 14f))
            // ball head
            drawCircle(c1.copy(alpha = 0.45f), 20f, Offset(120f, 68f))
            drawCircle(c0.copy(alpha = 0.7f), 11f, Offset(120f, 68f))
            // center column
            drawRect(c0.copy(alpha = 0.55f), Offset(115f, 84f), Size(10f, 42f))
            // apex
            val apex = Path().apply {
                moveTo(84f, 116f); lineTo(120f, 104f); lineTo(156f, 116f)
                lineTo(156f, 134f); lineTo(84f, 134f); close()
            }
            drawPath(apex, color = c1.copy(alpha = 0.45f))

            // legs
            data class Leg(val x1: Float, val x2: Float)
            val legs = listOf(Leg(96f, 30f), Leg(120f, 120f), Leg(144f, 210f))
            legs.forEach { leg ->
                fun seg(t1: Float, t2: Float): Pair<Offset, Offset> {
                    val a = Offset(leg.x1 + (leg.x2 - leg.x1) * t1, 130f + (308f - 130f) * t1)
                    val b = Offset(leg.x1 + (leg.x2 - leg.x1) * t2, 130f + (308f - 130f) * t2)
                    return a to b
                }
                val s1 = seg(0f, 0.34f)
                val s2 = seg(0.32f, 0.66f)
                val s3 = seg(0.64f, 1f)
                drawLine(c0.copy(alpha = 0.45f), s1.first, s1.second, strokeWidth = 9f)
                drawLine(c0.copy(alpha = 0.55f), s2.first, s2.second, strokeWidth = 7f)
                drawLine(c0.copy(alpha = 0.7f), s3.first, s3.second, strokeWidth = 5f)
                drawLine(INK, s1.first, s1.second, strokeWidth = 0.7f)
                drawLine(INK, s2.first, s2.second, strokeWidth = 0.7f)
                drawLine(INK, s3.first, s3.second, strokeWidth = 0.7f)
                // rubber foot
                drawCircle(c2, 4f, Offset(leg.x2, 310f))
                drawCircle(INK, 4f, Offset(leg.x2, 310f), style = Stroke(0.6f))
            }

            // contours on head + apex
            drawRect(INK, Offset(100f, 40f), Size(40f, 14f), style = Stroke(0.8f))
            drawCircle(INK, 20f, Offset(120f, 68f), style = Stroke(0.8f))
            drawCircle(INK, 11f, Offset(120f, 68f), style = Stroke(0.8f))
            drawRect(INK, Offset(115f, 84f), Size(10f, 42f), style = Stroke(0.8f))
            drawPath(apex, color = INK, style = Stroke(0.8f))
        }
    }
}
