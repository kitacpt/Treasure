package com.treasure.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors

/**
 * 头像式选择器 + 影集管理一体。
 *
 * 闭合：112dp 圆形头像（照片或线描）
 * 展开（点头像）：
 *   1) 📷 拍照 / + 选照片 两个 terra-描边动作 chip
 *   2) 影集照片小圆 — tap 当头像、long-press 删除（确认）
 *   3) 一道竖分隔
 *   4) 品类相关线描小圆 — tap 当头像（同时清掉照片头像）
 *
 * 没有 onTake/onPick/onRemove 这些回调时（手动录入页 item 还没生），动作 chip
 * 自动隐藏，行为退化为纯插画选择器。
 */
@Composable
fun HeroAvatarPicker(
    categoryId: String,
    palette: List<String>,
    options: List<HeroVector>,
    selected: HeroVector,
    onSelect: (HeroVector) -> Unit,
    modifier: Modifier = Modifier,
    photoOptions: List<String> = emptyList(),
    selectedPhoto: String? = null,
    onSelectPhoto: ((String) -> Unit)? = null,
    onTakePhoto: (() -> Unit)? = null,
    onPickPhotos: (() -> Unit)? = null,
    onRemovePhoto: ((String) -> Unit)? = null,
) {
    val colors = LocalTreasureColors.current
    var open by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val showingPhoto = selectedPhoto != null && photoOptions.contains(selectedPhoto)
    val canManagePhotos = onTakePhoto != null || onPickPhotos != null

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(colors.paper)
                .border(0.8.dp, colors.line, CircleShape)
                .clickable { open = !open },
            contentAlignment = Alignment.Center,
        ) {
            if (showingPhoto) {
                AsyncImage(
                    model = selectedPhoto,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(modifier = Modifier.padding(14.dp).fillMaxSize()) {
                    HeroIllustration(
                        item = previewItem(selected, palette, categoryId),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                open && canManagePhotos -> "选 / 加 / 长按删 · 点头像收起"
                open -> "选一张 · 点头像收起"
                else -> if (canManagePhotos) "点头像 · 换插画 / 用照片" else "点头像 · 换插画"
            },
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        if (open) {
            // 动作 chip 行：📷 拍照 / + 选照片
            if (canManagePhotos) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    onTakePhoto?.let {
                        ActionChip(label = "📷 拍照", onClick = it, modifier = Modifier.weight(1f))
                    }
                    onPickPhotos?.let {
                        ActionChip(label = "+ 选照片", onClick = it, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                photoOptions.forEach { path ->
                    val on = path == selectedPhoto
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                            .border(if (on) 1.5.dp else 0.5.dp, if (on) colors.terra else colors.line, CircleShape)
                            .pointerInput(path, onSelectPhoto, onRemovePhoto) {
                                detectTapGestures(
                                    onTap = { onSelectPhoto?.invoke(path) },
                                    onLongPress = {
                                        if (onRemovePhoto != null) pendingDelete = path
                                    },
                                )
                            },
                    ) {
                        AsyncImage(
                            model = path,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                if (photoOptions.isNotEmpty()) {
                    Box(modifier = Modifier
                        .width(0.5.dp)
                        .height(56.dp)
                        .background(colors.line))
                }
                options.forEach { v ->
                    val on = !showingPhoto && v == selected
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                            .border(if (on) 1.5.dp else 0.5.dp, if (on) colors.terra else colors.line, CircleShape)
                            .clickable { onSelect(v) }
                            .padding(7.dp),
                    ) {
                        HeroIllustration(
                            item = previewItem(v, palette, categoryId),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { path ->
        val onRemove = onRemovePhoto
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这张照片？") },
            text = {
                Text(
                    text = "文件会一并清除，无法恢复。如果它被设为头像，头像会回到线描。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemove?.invoke(path)
                    pendingDelete = null
                }) { Text("删除", color = colors.terra) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

@Composable
private fun ActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(colors.card)
            .border(0.5.dp, colors.terra.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = colors.terra,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun previewItem(
    v: HeroVector,
    palette: List<String>,
    categoryId: String,
): Item = Item(
    id = "preview",
    category = categoryId,
    brand = "", model = "", nickname = "", acquired = "", parted = null,
    status = ItemStatus.OWNED,
    palette = palette,
    oneLiner = "",
    heroVector = v,
    specs = emptyList(),
    history = emptyList(),
    photos = emptyList(),
    callouts = emptyMap(),
    createdAt = 0L, updatedAt = 0L,
)
