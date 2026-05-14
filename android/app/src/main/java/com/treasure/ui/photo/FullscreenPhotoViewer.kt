package com.treasure.ui.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.treasure.core.domain.PhotoCallout
import com.treasure.theme.LocalTreasureColors

/**
 * Cycle 0010：全屏看图。横滑翻页 / 双指缩放 / 长按某点加文字标注。
 * Cycle 0012：长按已有 dot 弹菜单 → 编辑 / 删除。
 *
 * - 单指拖：图未放大时被 HorizontalPager 拦截做翻页；放大后变 pan
 * - 双指缩放：1× ~ 5×
 * - 长按图（空白处）：弹文字输入，新增 callout
 * - 长按已有 dot：弹 dialog，可改文字也可删除
 *
 * 视觉是 ink 黑全屏底，照片居中 fit；顶部 ← 关闭，下方 photoIndex 计数 +
 * “长按加注” 提示。
 */
@Composable
fun FullscreenPhotoViewer(
    photos: List<String>,
    initialIndex: Int,
    callouts: Map<String, List<PhotoCallout>>,
    onSetCallouts: (path: String, callouts: List<PhotoCallout>) -> Unit,
    onClose: () -> Unit,
    /** Cycle 0034 v4：viewer 里点 "调整裁剪" 时回调，传 (path, 当前 rect)；
     *  调用方负责弹 CropScreen + 拿新 rect 写回。null = 不显示这颗按钮。 */
    onEditCrop: ((path: String, current: com.treasure.core.domain.PhotoCrop) -> Unit)? = null,
    /** 已有的裁剪映射 — viewer 显示原图，但 "调整裁剪" 默认 rect 取这里。 */
    photoCrops: Map<String, com.treasure.core.domain.PhotoCrop> = emptyMap(),
) {
    if (photos.isEmpty()) {
        onClose()
        return
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, photos.lastIndex),
        pageCount = { photos.size },
    )
    var addPending by remember { mutableStateOf<PendingAdd?>(null) }
    var editPending by remember { mutableStateOf<PendingEdit?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E0C)),
    ) {
        // Cycle 0020：pager 上下让出 64dp，给 back 键 / 底部提示留黑边；图片
        // letterbox 在中间这块区域，跟 controls 不再交叠。
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 64.dp, bottom = 64.dp),
        ) { page ->
            val path = photos[page]
            val pageCallouts = callouts[path].orEmpty()
            ZoomableImageWithCallouts(
                path = path,
                callouts = pageCallouts,
                onLongPressEmpty = { x, y -> addPending = PendingAdd(path, x, y) },
                onLongPressPin = { idx ->
                    val c = pageCallouts.getOrNull(idx) ?: return@ZoomableImageWithCallouts
                    editPending = PendingEdit(path, idx, c)
                },
            )
        }

        // top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "←",
                color = Color(0xFFF4F1EA),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${pagerState.currentPage + 1} / ${photos.size}",
                color = Color(0xFFF4F1EA).copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
            // Cycle 0034 v4：右上 "调整裁剪" — 把当前页 path + 已有 rect（或
            // 整图 default）抛给调用方让它弹 CropScreen 拿新 rect。
            if (onEditCrop != null) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "调整裁剪",
                    color = Color(0xFFE8E2D4),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            val path = photos[pagerState.currentPage]
                            val current = photoCrops[path]
                                ?: com.treasure.core.domain.PhotoCrop.Full
                            onEditCrop(path, current)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        // hint
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "长按图任意处加注 · 长按已有标注可改 / 删",
                color = Color(0xFFF4F1EA).copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }

    addPending?.let { p ->
        CalloutEditDialog(
            initial = "",
            allowDelete = false,
            onCancel = { addPending = null },
            onConfirm = { text ->
                if (text.isNotBlank()) {
                    val current = callouts[p.path].orEmpty()
                    onSetCallouts(
                        p.path,
                        current + PhotoCallout(p.x, p.y, text.trim()),
                    )
                }
                addPending = null
            },
            onDelete = { /* not reachable when allowDelete = false */ },
        )
    }
    editPending?.let { p ->
        CalloutEditDialog(
            initial = p.callout.text,
            allowDelete = true,
            onCancel = { editPending = null },
            onConfirm = { text ->
                if (text.isNotBlank()) {
                    val current = callouts[p.path].orEmpty().toMutableList()
                    current[p.index] = p.callout.copy(text = text.trim())
                    onSetCallouts(p.path, current.toList())
                }
                editPending = null
            },
            onDelete = {
                val current = callouts[p.path].orEmpty().toMutableList()
                if (p.index in current.indices) current.removeAt(p.index)
                onSetCallouts(p.path, current.toList())
                editPending = null
            },
        )
    }
}

private data class PendingAdd(val path: String, val x: Float, val y: Float)
private data class PendingEdit(val path: String, val index: Int, val callout: PhotoCallout)

@Composable
private fun ZoomableImageWithCallouts(
    path: String,
    callouts: List<PhotoCallout>,
    onLongPressEmpty: (xNorm: Float, yNorm: Float) -> Unit,
    onLongPressPin: (index: Int) -> Unit,
) {
    var scale by remember(path) { mutableStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }
    var size by remember(path) { mutableStateOf(IntSize.Zero) }

    // Cycle 0020：上一刀的 transformable / detectTransformGestures 都会吃掉
    // 单指拖，导致 HorizontalPager 翻不了页。这里用 awaitEachGesture 自己写：
    // 看每次 pointerEvent 时按下的 pointer 数。
    //
    //   - 1 个手指 + scale==1 → 不消费，让 pager 处理 swipe
    //   - 1 个手指 + scale > 1 → 消费 + 平移已放大图片
    //   - 2+ 个手指 → 消费 + 处理 pinch zoom（顺手也吃 pan）
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(path) {
                    awaitEachGesture {
                        do {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            when {
                                pressed >= 2 -> {
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    if (scale > 1f) offset += pan
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                                pressed == 1 && scale > 1f -> {
                                    val pan = event.calculatePan()
                                    offset += pan
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }
                                // pressed == 1 && scale == 1 → 不消费，pager 翻页
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(path) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        },
                        onLongPress = { tap ->
                            if (size.width == 0 || size.height == 0) return@detectTapGestures
                            val xNorm = (tap.x / size.width).coerceIn(0f, 1f)
                            val yNorm = (tap.y / size.height).coerceIn(0f, 1f)
                            onLongPressEmpty(xNorm, yNorm)
                        },
                    )
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            AsyncImage(
                model = path,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }

        // overlay callouts. Each pin gets its own pointer gesture so a
        // long-press on the dot opens edit/delete (and is consumed before
        // bubbling to the image's onLongPress = "add new").
        callouts.forEachIndexed { idx, c ->
            CalloutPin(
                xPercent = c.x,
                yPercent = c.y,
                text = c.text,
                viewportWidth = size.width,
                viewportHeight = size.height,
                onLongPress = { onLongPressPin(idx) },
            )
        }
    }
}

@Composable
private fun CalloutPin(
    xPercent: Float,
    yPercent: Float,
    text: String,
    viewportWidth: Int,
    viewportHeight: Int,
    onLongPress: () -> Unit,
) {
    if (viewportWidth == 0) return
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.TopStart),
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    translationX = (xPercent * viewportWidth) - 6.dp.toPx()
                    translationY = (yPercent * viewportHeight) - 6.dp.toPx()
                }
                .widthIn(max = 220.dp)
                .pointerInput(text, xPercent, yPercent) {
                    detectTapGestures(onLongPress = { onLongPress() })
                },
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(colors.terra)
                    .border(1.dp, Color(0xFFF4F1EA), CircleShape),
            )
            Spacer(Modifier.size(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCCF4F1EA))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = text,
                    color = colors.ink,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun CalloutEditDialog(
    initial: String,
    allowDelete: Boolean,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (allowDelete) "改 / 删一条标注" else "加一条标注") },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.card)
                        .border(0.5.dp, colors.line)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "比如 “左侧有一道擦痕”",
                            color = colors.sub.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = false,
                        cursorBrush = SolidColor(colors.terra),
                        textStyle = LocalTextStyle.current.copy(
                            color = colors.ink,
                            fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (allowDelete) {
                    Spacer(Modifier.size(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .border(0.5.dp, colors.line)
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "删除这条标注",
                            color = colors.terra,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text("✕", color = colors.terra, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text("保存", color = colors.terra) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        },
        containerColor = colors.paper,
        titleContentColor = colors.ink,
        textContentColor = colors.sub,
    )
}
