package com.treasure.ui.photo

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.treasure.theme.LocalTreasureColors

/**
 * Cycle 0033：用户添加影集照片前的最简单裁剪界面。
 *
 * 设计取舍：写一个 free-form 矩形，四角四边都能拖；图片以"contain"方式
 * 居中显示在画布里（保留长宽比），所以裁剪矩形归一化到图片实际像素范围
 * 时一一对应。没有旋转、没有 aspect lock、没有 zoom — 跟原型里的"基本
 * 裁剪"用意一致（这是录入流的微调，不是 Photoshop）。
 *
 * 输出 onConfirm(rect) 中 rect 是图片内坐标系归一化到 [0..1]，由调用方
 * 拿原图按比例真正裁剪 + 落盘（[com.treasure.ui.add.AddViewModel.persistDraftPhoto]
 * 已经实现了）。
 */
@Composable
fun CropScreen(
    source: Uri,
    onCancel: () -> Unit,
    onConfirm: (Rect) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val density = LocalDensity.current
    // 图片在 viewport 里的实际像素 bounds（contain 模式后真正绘的位置 / 尺寸）。
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var imageBounds by remember { mutableStateOf<Rect?>(null) }
    var aspect by remember { mutableStateOf(1f) }
    // 裁剪矩形（在 viewport 坐标系里，px）。
    var crop by remember { mutableStateOf<Rect?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        // 顶部 toolbar — 取消 / 确认
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text("取消", color = colors.sub) }
            Spacer(Modifier.weight(1f))
            Text(
                text = "裁剪",
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                val c = crop
                val b = imageBounds
                // Cycle 0033 v2：图还没解码完用户就点确定 — 不要取消（=丢图），
                // 直接用整图当裁剪结果，保证选了的照片一定落进影集。
                if (c == null || b == null) {
                    onConfirm(Rect(0f, 0f, 1f, 1f))
                    return@TextButton
                }
                val left = ((c.left - b.left) / b.width).coerceIn(0f, 1f)
                val top = ((c.top - b.top) / b.height).coerceIn(0f, 1f)
                val right = ((c.right - b.left) / b.width).coerceIn(0f, 1f)
                val bottom = ((c.bottom - b.top) / b.height).coerceIn(0f, 1f)
                if (right - left < 0.02f || bottom - top < 0.02f) {
                    onConfirm(Rect(0f, 0f, 1f, 1f))
                } else {
                    onConfirm(Rect(left, top, right, bottom))
                }
            }) { Text("确定", color = colors.terra) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ink.copy(alpha = 0.92f))
                .onSizeChanged { sz ->
                    viewportSize = sz
                    if (aspect > 0f && sz.width > 0 && sz.height > 0) {
                        imageBounds = computeContainBounds(sz, aspect)
                        crop = imageBounds?.let { centeredCrop(it) }
                    }
                },
        ) {
            AsyncImage(
                model = source,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                onSuccess = { state ->
                    val w = state.result.drawable.intrinsicWidth
                    val h = state.result.drawable.intrinsicHeight
                    if (w > 0 && h > 0) {
                        aspect = w.toFloat() / h.toFloat()
                        if (viewportSize.width > 0 && viewportSize.height > 0) {
                            imageBounds = computeContainBounds(viewportSize, aspect)
                            crop = imageBounds?.let { centeredCrop(it) }
                        }
                    }
                },
            )
            val cropBox = crop
            val bounds = imageBounds
            if (cropBox != null && bounds != null) {
                CropOverlay(
                    crop = cropBox,
                    bounds = bounds,
                    onChange = { crop = it },
                    minSizePx = with(density) { 60.dp.toPx() },
                )
            } else {
                // Cycle 0033 v2：图还没解码完时给个明确提示，免得用户以为
                // 卡死立刻退出。
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "正在加载图片…",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .navigationBarsPadding())
    }
}

@Composable
private fun CropOverlay(
    crop: Rect,
    bounds: Rect,
    onChange: (Rect) -> Unit,
    minSizePx: Float,
) {
    val handleSize = with(LocalDensity.current) { 24.dp.toPx() }
    val edgeHit = with(LocalDensity.current) { 18.dp.toPx() }

    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(bounds) {
                detectDragGestures(
                    onDragStart = { /* mode picked on first move via state below */ },
                    onDrag = { change, drag ->
                        val pos = change.position
                        val mode = pickMode(pos, crop, edgeHit)
                        onChange(applyDrag(crop, bounds, mode, drag, minSizePx))
                    },
                )
            },
    ) {
        // 灰色蒙层：图片范围内剪掉裁剪矩形那块亮起
        val maskColor = Color(0x99000000)
        drawRect(
            color = maskColor,
            topLeft = Offset(bounds.left, bounds.top),
            size = Size(bounds.width, crop.top - bounds.top),
        )
        drawRect(
            color = maskColor,
            topLeft = Offset(bounds.left, crop.bottom),
            size = Size(bounds.width, bounds.bottom - crop.bottom),
        )
        drawRect(
            color = maskColor,
            topLeft = Offset(bounds.left, crop.top),
            size = Size(crop.left - bounds.left, crop.height),
        )
        drawRect(
            color = maskColor,
            topLeft = Offset(crop.right, crop.top),
            size = Size(bounds.right - crop.right, crop.height),
        )
        // 矩形边线
        drawRect(
            color = Color.White,
            topLeft = Offset(crop.left, crop.top),
            size = Size(crop.width, crop.height),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        // 4 个角的小记号
        val corners = listOf(
            Offset(crop.left, crop.top),
            Offset(crop.right, crop.top),
            Offset(crop.left, crop.bottom),
            Offset(crop.right, crop.bottom),
        )
        corners.forEach { c ->
            drawRect(
                color = Color.White,
                topLeft = Offset(c.x - handleSize / 2, c.y - handleSize / 2),
                size = Size(handleSize, handleSize),
                style = Stroke(width = 2.5.dp.toPx()),
            )
        }
    }
}

private enum class DragMode { Move, TL, TR, BL, BR, T, B, L, R, None }

private fun pickMode(pos: Offset, crop: Rect, edge: Float): DragMode {
    val nearL = kotlin.math.abs(pos.x - crop.left) <= edge
    val nearR = kotlin.math.abs(pos.x - crop.right) <= edge
    val nearT = kotlin.math.abs(pos.y - crop.top) <= edge
    val nearB = kotlin.math.abs(pos.y - crop.bottom) <= edge
    val insideX = pos.x in (crop.left - edge)..(crop.right + edge)
    val insideY = pos.y in (crop.top - edge)..(crop.bottom + edge)
    return when {
        nearL && nearT -> DragMode.TL
        nearR && nearT -> DragMode.TR
        nearL && nearB -> DragMode.BL
        nearR && nearB -> DragMode.BR
        nearT && insideX -> DragMode.T
        nearB && insideX -> DragMode.B
        nearL && insideY -> DragMode.L
        nearR && insideY -> DragMode.R
        pos.x in crop.left..crop.right && pos.y in crop.top..crop.bottom -> DragMode.Move
        else -> DragMode.None
    }
}

private fun applyDrag(
    crop: Rect,
    bounds: Rect,
    mode: DragMode,
    drag: Offset,
    minSize: Float,
): Rect {
    var l = crop.left
    var t = crop.top
    var r = crop.right
    var b = crop.bottom
    when (mode) {
        DragMode.Move -> {
            val dx = drag.x.coerceIn(bounds.left - l, bounds.right - r)
            val dy = drag.y.coerceIn(bounds.top - t, bounds.bottom - b)
            l += dx; r += dx; t += dy; b += dy
        }
        DragMode.TL -> { l = (l + drag.x).coerceIn(bounds.left, r - minSize); t = (t + drag.y).coerceIn(bounds.top, b - minSize) }
        DragMode.TR -> { r = (r + drag.x).coerceIn(l + minSize, bounds.right); t = (t + drag.y).coerceIn(bounds.top, b - minSize) }
        DragMode.BL -> { l = (l + drag.x).coerceIn(bounds.left, r - minSize); b = (b + drag.y).coerceIn(t + minSize, bounds.bottom) }
        DragMode.BR -> { r = (r + drag.x).coerceIn(l + minSize, bounds.right); b = (b + drag.y).coerceIn(t + minSize, bounds.bottom) }
        DragMode.T  -> { t = (t + drag.y).coerceIn(bounds.top, b - minSize) }
        DragMode.B  -> { b = (b + drag.y).coerceIn(t + minSize, bounds.bottom) }
        DragMode.L  -> { l = (l + drag.x).coerceIn(bounds.left, r - minSize) }
        DragMode.R  -> { r = (r + drag.x).coerceIn(l + minSize, bounds.right) }
        DragMode.None -> Unit
    }
    return Rect(l, t, r, b)
}

private fun computeContainBounds(viewport: IntSize, aspect: Float): Rect {
    val vw = viewport.width.toFloat()
    val vh = viewport.height.toFloat()
    val viewportAspect = vw / vh
    return if (aspect > viewportAspect) {
        // 宽限制
        val w = vw
        val h = vw / aspect
        val top = (vh - h) / 2f
        Rect(0f, top, w, top + h)
    } else {
        val h = vh
        val w = vh * aspect
        val left = (vw - w) / 2f
        Rect(left, 0f, left + w, h)
    }
}

private fun centeredCrop(bounds: Rect): Rect {
    val w = bounds.width * 0.85f
    val h = bounds.height * 0.85f
    val cx = bounds.left + bounds.width / 2f
    val cy = bounds.top + bounds.height / 2f
    return Rect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
}
