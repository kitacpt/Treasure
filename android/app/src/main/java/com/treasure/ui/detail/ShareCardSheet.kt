@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.detail

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.Item
import com.treasure.theme.LocalTreasureColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Cycle 0038 v3 — 分享卡片底部抽屉（用户反馈 3 处迭代）：
 *   - 顶栏文字删了（v2 起）
 *   - 卡片改 16:9 左 1/3 hero / 右 2/3 上下分（v3 进一步精简：删历史段）
 *   - 全屏预览支持 pinch-zoom + pan（v3 新增）
 *   - 参数 > [ShareCard.MAX_SPECS]（6）时才弹 Selecting 让用户挑
 */
@Composable
fun ShareCardSheet(
    item: Item,
    onDismiss: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val visibleSpecs = remember(item) { item.specs.filter { it.label.isNotBlank() } }
    val needsSelecting = visibleSpecs.size > ShareCard.MAX_SPECS

    var selectedSpecIdx by remember(item.id) {
        mutableStateOf(visibleSpecs.indices.toList().take(ShareCard.MAX_SPECS).toSet())
    }
    var confirmed by remember(item.id) { mutableStateOf(!needsSelecting) }
    var cardFile by remember(item.id) { mutableStateOf<File?>(null) }
    var error by remember(item.id) { mutableStateOf<String?>(null) }
    var fullscreenPreview by remember { mutableStateOf(false) }

    LaunchedEffect(confirmed, item.id) {
        if (!confirmed) return@LaunchedEffect
        error = null
        cardFile = null
        val chosenSpecs: List<HeroSpec> = selectedSpecIdx
            .sorted().mapNotNull { visibleSpecs.getOrNull(it) }
        runCatching {
            withContext(Dispatchers.IO) {
                ShareCard.generate(context, item, chosenSpecs)
            }
        }.onSuccess { cardFile = it }
            .onFailure { error = it.message ?: "卡片生成失败" }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
        ) {
            if (!confirmed) {
                SelectingPane(
                    visibleSpecs = visibleSpecs,
                    selectedIdx = selectedSpecIdx,
                    onToggle = { idx ->
                        selectedSpecIdx = toggleWithLimit(
                            selectedSpecIdx, idx, ShareCard.MAX_SPECS,
                        )
                    },
                    onCancel = onDismiss,
                    onConfirm = { confirmed = true },
                )
            } else {
                PreviewPane(
                    cardFile = cardFile,
                    error = error,
                    onTapPreview = { fullscreenPreview = true },
                    onReSelect = if (needsSelecting) {
                        { confirmed = false; cardFile = null }
                    } else null,
                    onSaveGallery = {
                        scope.launch {
                            val f = cardFile ?: return@launch
                            val uri = withContext(Dispatchers.IO) {
                                ShareCard.saveToGallery(context, f)
                            }
                            val msg = if (uri != null) "已保存到相册 · Pictures/Treasure"
                            else "保存失败 · 检查存储权限"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onShareIntent = {
                        val f = cardFile ?: return@PreviewPane
                        runCatching {
                            context.startActivity(ShareCard.shareIntent(context, f))
                        }.onFailure {
                            Toast.makeText(context, "分享失败 · ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
        }
    }

    if (fullscreenPreview && cardFile != null) {
        FullscreenZoomablePreview(
            file = cardFile!!,
            onClose = { fullscreenPreview = false },
        )
    }
}

/* ─── Preview pane ────────────────────────────────────────────────── */

@Composable
private fun PreviewPane(
    cardFile: File?,
    error: String?,
    onTapPreview: () -> Unit,
    onReSelect: (() -> Unit)?,
    onSaveGallery: () -> Unit,
    onShareIntent: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Spacer(Modifier.height(8.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card)
            .border(0.5.dp, colors.line)
            .then(
                if (cardFile != null) Modifier.clickable(onClick = onTapPreview) else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val f = cardFile
        if (f != null) {
            AsyncImage(
                model = f,
                contentDescription = "卡片预览 · 点击放大",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Fit,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(
                    color = colors.terra,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error ?: "正在生成卡片…",
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "单击放大 · 双指缩放",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.weight(1f))
        if (onReSelect != null) {
            Text(
                text = "重新挑选 →",
                color = colors.terra,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onReSelect)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }

    Spacer(Modifier.height(14.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .border(0.5.dp, colors.line, RoundedCornerShape(6.dp))
                .clickable(enabled = cardFile != null, onClick = onSaveGallery)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "保存到相册",
                color = if (cardFile != null) colors.ink else colors.sub.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (cardFile != null) colors.terra else colors.terra.copy(alpha = 0.30f))
                .clickable(enabled = cardFile != null, onClick = onShareIntent)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "分享到其他 app",
                color = colors.paper,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/* ─── Selecting pane（仅参数；历史 v3 起删） ─────────────────────── */

@Composable
private fun SelectingPane(
    visibleSpecs: List<HeroSpec>,
    selectedIdx: Set<Int>,
    onToggle: (Int) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = "挑选上卡参数",
            color = colors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "参数超过 ${ShareCard.MAX_SPECS} 条 · 最多选 ${ShareCard.MAX_SPECS} 个",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
        )

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "参数", color = colors.ink, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${selectedIdx.size} / ${ShareCard.MAX_SPECS}",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
        Spacer(Modifier.height(8.dp))
        visibleSpecs.forEachIndexed { idx, sp ->
            CheckRow(
                selected = idx in selectedIdx,
                canSelect = selectedIdx.size < ShareCard.MAX_SPECS || idx in selectedIdx,
                title = sp.label,
                sub = sp.value,
                onClick = { onToggle(idx) },
            )
        }
    }

    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .border(0.5.dp, colors.line, RoundedCornerShape(6.dp))
                .clickable(onClick = onCancel)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "取消", color = colors.sub, style = MaterialTheme.typography.bodyMedium)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.terra)
                .clickable(onClick = onConfirm)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "生成卡片", color = colors.paper, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CheckRow(
    selected: Boolean,
    canSelect: Boolean,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = canSelect, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (selected) colors.terra else colors.paper)
                .border(
                    0.8.dp,
                    if (canSelect) colors.line else colors.line.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Text(text = "✓", color = colors.paper, style = MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.heightIn(min = 32.dp)) {
            Text(
                text = title,
                color = if (canSelect) colors.ink else colors.sub.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    color = colors.sub.copy(alpha = if (canSelect) 1f else 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

/* ─── Fullscreen zoomable preview ────────────────────────────────── */

/**
 * 全屏预览 + pinch-zoom + pan + 双击重置 + 单击关闭。
 *
 * - 双指 pinch：缩放 1f..5f
 * - 双指 / 单指拖：平移（仅当 scale > 1f 时）；边界 clamp 不让图飞出可见区
 * - 双击：缩放回 1f（带动画感的瞬时 reset，简单实现）
 * - 单击：关闭（仅在 scale == 1f 时；放大态下点击只重置缩放避免误关）
 */
@Composable
private fun FullscreenZoomablePreview(
    file: File,
    onClose: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ink.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center,
        ) {
            // 卡片视口宽 = 容器宽 - 32dp（horizontal padding），按 16:9 算高
            val viewportWPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                (maxWidth - 32.dp).toPx()
            }
            val viewportHPx = viewportWPx * 9f / 16f
            AsyncImage(
                model = file,
                contentDescription = "卡片全屏预览",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .aspectRatio(16f / 9f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            // pan 只有放大时有意义；clamp 到视口边界
                            if (scale > 1f) {
                                val maxOffX = viewportWPx * (scale - 1f) / 2f
                                val maxOffY = viewportHPx * (scale - 1f) / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-maxOffX, maxOffX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxOffY, maxOffY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (scale > 1.01f) {
                                    // 放大态下单击 = 回到 1x，不关闭
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    onClose()
                                }
                            },
                            onDoubleTap = {
                                if (scale > 1.01f) {
                                    scale = 1f; offsetX = 0f; offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            },
                        )
                    },
            )
            // 右上轻量 ✕
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.paper.copy(alpha = 0.16f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = colors.paper, style = MaterialTheme.typography.bodyMedium)
            }
            // 底部一行小提示（仅 scale==1 时显示）
            if (scale <= 1.01f) {
                Text(
                    text = "双指缩放 · 双击放大 · 点击关闭",
                    color = colors.paper.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp),
                )
            }
        }
    }
    // Suppress unused warning helpers
    @Suppress("UNUSED_EXPRESSION") max(0f, 0f) // keep imports tidy
    @Suppress("UNUSED_EXPRESSION") min(0f, 0f)
}

private fun toggleWithLimit(set: Set<Int>, idx: Int, limit: Int): Set<Int> =
    when {
        idx in set -> set - idx
        set.size < limit -> set + idx
        else -> set
    }
