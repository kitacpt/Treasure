package com.treasure.ui.photo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import com.treasure.core.domain.PhotoCrop

/**
 * Cycle 0034 v4：非破坏式显示裁剪。原图磁盘上一字节没动，UI 这边按归一化
 * [crop] (x, y, w, h ∈ 0..1) 把"裁剪框"放大到 viewport，clipToBounds 切掉
 * 外面 — 视觉等价于裁剪图，但回头还能改 rect。
 *
 * 数学：把原图 fit-到 viewport（保留长宽比），然后 scale = 1 / crop.w
 * （或 crop.h，看 viewport 的对应维度匹配），translate 把 crop 中心平移到
 * viewport 中心。
 *
 * crop 为 [PhotoCrop.Full]（或 null）时退化到普通 ContentScale.Crop。
 */
@Composable
fun CroppedPhoto(
    model: Any?,
    crop: PhotoCrop?,
    modifier: Modifier = Modifier,
) {
    val effectiveCrop = crop ?: PhotoCrop.Full
    if (effectiveCrop.isFullImage) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
        return
    }

    // 用 graphicsLayer 做 fit-then-crop：先 ContentScale.Crop 把图填满，再按
    // crop rect 反向 scale + translate。最直接的做法是用 viewport size /
    // crop rect 大小算 zoom 倍率，并把 crop 中心平移到 viewport 中心。
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var imgIntrinsicW by remember { mutableStateOf(0) }
    var imgIntrinsicH by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .onSizeChanged { viewport = it }
            .clipToBounds(),
    ) {
        // viewport 尺寸（dp / px 同样比例）+ 原图长宽比 → 算出 fit-到-viewport
        // 时图占 viewport 的多少。对外 ContentScale.FillBounds，让图刚好填满
        // viewport，再用 graphicsLayer 在像素空间放大 + 平移到 crop 区域。
        val vw = viewport.width.toFloat()
        val vh = viewport.height.toFloat()
        val (scaleX, scaleY, transX, transY) = if (vw > 0f && vh > 0f && imgIntrinsicW > 0 && imgIntrinsicH > 0) {
            computeCropTransform(
                viewportW = vw,
                viewportH = vh,
                imgW = imgIntrinsicW.toFloat(),
                imgH = imgIntrinsicH.toFloat(),
                crop = effectiveCrop,
            )
        } else listOf(1f, 1f, 0f, 0f)
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    this.translationX = transX
                    this.translationY = transY
                },
            // 拿原图的"完整像素 fit 到 viewport"作为基线，再做 graphicsLayer
            // 变换。Fit 而不是 Crop —— 否则 Coil 自己已经做了 inscribe crop，
            // 我们的 rect 数学就对不上了。
            contentScale = ContentScale.Fit,
            onSuccess = { state ->
                val d = state.result.drawable
                imgIntrinsicW = d.intrinsicWidth
                imgIntrinsicH = d.intrinsicHeight
            },
        )
    }
}

/**
 * 计算 (scaleX, scaleY, transX, transY) 把"原图 fit 到 viewport"再变换
 * 成"crop 区域填满 viewport"。
 *
 * 思路：ContentScale.Fit 后图在 viewport 里占据的 rect 是 (fitX, fitY, fitW,
 * fitH)。crop 区域在原图坐标系是 (cx*imgW, cy*imgH, cw*imgW, ch*imgH)，映射
 * 到 fit 后的 viewport 坐标系就是 (fitX + cx*fitW, fitY + cy*fitH, cw*fitW,
 * ch*fitH)。把这个矩形拉伸到 (0, 0, viewportW, viewportH)：scale = viewport/
 * cropOnFit；translate = -cropTopLeft * scale。
 */
private fun computeCropTransform(
    viewportW: Float,
    viewportH: Float,
    imgW: Float,
    imgH: Float,
    crop: PhotoCrop,
): List<Float> {
    val imgAspect = imgW / imgH
    val viewportAspect = viewportW / viewportH
    val fitW: Float
    val fitH: Float
    val fitX: Float
    val fitY: Float
    if (imgAspect > viewportAspect) {
        fitW = viewportW
        fitH = viewportW / imgAspect
        fitX = 0f
        fitY = (viewportH - fitH) / 2f
    } else {
        fitH = viewportH
        fitW = viewportH * imgAspect
        fitX = (viewportW - fitW) / 2f
        fitY = 0f
    }
    val cropX = fitX + crop.x * fitW
    val cropY = fitY + crop.y * fitH
    val cropW = crop.w * fitW
    val cropH = crop.h * fitH
    if (cropW <= 0f || cropH <= 0f) return listOf(1f, 1f, 0f, 0f)
    val sx = viewportW / cropW
    val sy = viewportH / cropH
    val tx = -cropX * sx
    val ty = -cropY * sy
    return listOf(sx, sy, tx, ty)
}
