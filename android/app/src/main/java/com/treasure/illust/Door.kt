package com.treasure.illust

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Cycle 0031：Portal 空状态的"大门"插画 — 一扇拱顶双开木门，跟其它馆藏
 * 插画线描风一致（240×240 viewbox，[INK] 描边，palette c2 浅填充）。
 *
 * 用户点这扇门 → 进分类管理。
 */
@Composable
fun Door(palette: IllustPalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawInViewBox(240f, 240f) {
            val (_, _, c2, _) = palette4(palette)
            val stroke = Stroke(1.2f)
            val thin = Stroke(0.7f)

            // 地面线
            drawLine(INK, Offset(20f, 222f), Offset(220f, 222f), strokeWidth = 1.0f)

            // 门拱整体几何
            val doorL = 50f
            val doorR = 190f
            val doorB = 222f
            val archStart = 92f                // 拱起始 y（竖边顶端）
            val archRadius = (doorR - doorL) / 2f
            val archRect = Offset(doorL, archStart - archRadius)
            val archSize = Size(doorR - doorL, archRadius * 2)

            // 底色（拱顶 + 矩形主体一起一层淡 c2）
            drawArc(
                color = c2.copy(alpha = 0.20f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = archRect,
                size = archSize,
            )
            drawRect(
                c2.copy(alpha = 0.20f),
                Offset(doorL, archStart),
                Size(doorR - doorL, doorB - archStart),
            )

            // 拱顶轮廓
            drawArc(
                color = INK,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = archRect,
                size = archSize,
                style = stroke,
            )
            // 两条竖边
            drawLine(INK, Offset(doorL, archStart), Offset(doorL, doorB), strokeWidth = 1.2f)
            drawLine(INK, Offset(doorR, archStart), Offset(doorR, doorB), strokeWidth = 1.2f)

            // 两扇门交界中线
            val mid = (doorL + doorR) / 2f
            drawLine(INK, Offset(mid, archStart), Offset(mid, doorB), strokeWidth = 1.2f)

            // 门板内嵌矩形装饰（左板上下两块）
            val panelL = doorL + 10f
            val panelR = mid - 6f
            drawRect(INK, Offset(panelL, archStart + 14f), Size(panelR - panelL, 48f), style = thin)
            drawRect(INK, Offset(panelL, archStart + 72f), Size(panelR - panelL, 48f), style = thin)
            // 右板镜像
            val panelL2 = mid + 6f
            val panelR2 = doorR - 10f
            drawRect(INK, Offset(panelL2, archStart + 14f), Size(panelR2 - panelL2, 48f), style = thin)
            drawRect(INK, Offset(panelL2, archStart + 72f), Size(panelR2 - panelL2, 48f), style = thin)

            // 把手（两个小圆，靠中线）
            drawCircle(INK, radius = 2.6f, center = Offset(mid - 6f, archStart + 92f))
            drawCircle(INK, radius = 2.6f, center = Offset(mid + 6f, archStart + 92f))

            // 拱顶圆窗
            val rosaR = 14f
            val rosaY = archStart - 36f
            drawCircle(c2.copy(alpha = 0.45f), radius = rosaR, center = Offset(mid, rosaY))
            drawCircle(INK, radius = rosaR, center = Offset(mid, rosaY), style = thin)
            drawLine(INK, Offset(mid - rosaR, rosaY), Offset(mid + rosaR, rosaY), strokeWidth = 0.5f)
            drawLine(INK, Offset(mid, rosaY - rosaR), Offset(mid, rosaY + rosaR), strokeWidth = 0.5f)
        }
    }
}
