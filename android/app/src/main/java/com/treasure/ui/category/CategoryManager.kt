@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.CategoryInfo
import com.treasure.theme.LocalTreasureColors

/**
 * Cycle 0026：分类管理抽屉。
 * Cycle 0029：编辑 / 新建拆全屏路由。
 * Cycle 0030：divider 占自己一个 row-height slot。
 * Cycle 0031：拖动数学按"预览终态布局"算 — 每行（含 divider）拖动期间直接
 *   跳到它在终态新布局里的 visualSlot。修两个问题：(a) 跨过分割线落点错一格
 *   导致用户感觉"拖不下去"；(b) 分割线视觉不动 → 拖动到下面 hidden 行重叠
 *   分割线感觉很怪。
 */
@Composable
fun CategoryManager(
    onClose: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (CategoryInfo) -> Unit,
    vm: CategoryManagerViewModel = viewModel(factory = CategoryManagerViewModel.Factory),
) {
    val colors = LocalTreasureColors.current
    val all by vm.all.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        CategoryList(
            all = all,
            onEdit = { info ->
                onClose()
                onEditCategory(info)
            },
            onAdd = {
                onClose()
                onAddCategory()
            },
            onCommit = { orderedIds, hiddenIds ->
                vm.applyReorder(orderedIds, hiddenIds)
            },
        )
        Spacer(Modifier.height(20.dp))
    }
}

// ─── List with drag-to-reorder ─────────────────────────────────────────

private val ROW_HEIGHT = 60.dp

@Composable
private fun CategoryList(
    all: List<CategoryInfo>,
    onEdit: (CategoryInfo) -> Unit,
    onAdd: () -> Unit,
    onCommit: (orderedIds: List<String>, hiddenIds: Set<String>) -> Unit,
) {
    val colors = LocalTreasureColors.current

    // 工作副本：cycle 0031 修订 — 只在分类**集合**变化（新增 / 删除）时再
    // sync，单纯 reorder / hidden toggle 不要因为 Room 中间 emit 把本地优化
    // 后状态覆盖掉。否则会出现"拖动后视觉先到新位、随后 sort_order 写一半
    // 的中间 emit 反推 workVisible 重置回旧顺序"的弹回观感。
    var workVisible by remember { mutableStateOf(all.filter { !it.hidden }) }
    var workHidden by remember { mutableStateOf(all.filter { it.hidden }) }
    val incomingIds = remember(all) { all.map { it.id }.toSet() }
    LaunchedEffect(incomingIds) {
        val localIds = (workVisible + workHidden).map { it.id }.toSet()
        if (localIds != incomingIds) {
            workVisible = all.filter { !it.hidden }
            workHidden = all.filter { it.hidden }
        }
    }

    val density = LocalDensity.current
    val rowPx = with(density) { ROW_HEIGHT.toPx() }

    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val combined = workVisible + workHidden
    val visibleCount = workVisible.size
    val totalSlots = combined.size + 1  // +1 给 divider

    // visualSlot 编号：visible 占 0..V-1，divider 占 V，hidden 占 V+1..N。
    fun combinedToVisual(i: Int): Int = if (i < visibleCount) i else i + 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "分类管理",
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+ 新增分类",
                color = colors.terra,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT * totalSlots),
        ) {
            // 用户指尖中心当前所在 visualSlot
            val originVisual = if (dragIndex < 0) -1 else combinedToVisual(dragIndex)
            val targetVisualSlot = if (dragIndex < 0) -1 else {
                val centerY = originVisual * rowPx + dragOffsetY + rowPx / 2f
                (centerY / rowPx).toInt().coerceIn(0, totalSlots - 1)
            }

            // 预览终态：拖动行将落进哪段、newVisibleCount 多少、插在 combined' 哪
            // 个 idx。跟 commitDrag 用同一套公式 — 拖动手感与松手结果一致。
            val originIsVisible = dragIndex in 0 until visibleCount
            val previewTargetIsVisible = when {
                dragIndex < 0 -> originIsVisible
                targetVisualSlot < visibleCount -> true
                targetVisualSlot > visibleCount -> false
                else -> !originIsVisible  // 拖到 divider 槽 → 跨段去对侧
            }
            val previewNewVis = when {
                dragIndex < 0 -> visibleCount
                originIsVisible && previewTargetIsVisible -> visibleCount
                originIsVisible && !previewTargetIsVisible -> visibleCount - 1
                !originIsVisible && previewTargetIsVisible -> visibleCount + 1
                else -> visibleCount
            }
            val previewNewCombinedIdx = if (dragIndex < 0) -1 else {
                val raw = if (previewTargetIsVisible) targetVisualSlot else targetVisualSlot - 1
                raw.coerceIn(0, combined.size - 1)
            }

            combined.forEachIndexed { idx, info ->
                val isDragging = idx == dragIndex
                val translateY: Float = when {
                    isDragging -> combinedToVisual(idx) * rowPx + dragOffsetY
                    dragIndex < 0 -> combinedToVisual(idx) * rowPx
                    else -> {
                        // 在 combined' (移除 dragging 行) 里这行的 idx
                        val newI = if (idx < dragIndex) idx else idx - 1
                        // 把 dragging 行插回后，这行最终的 combined idx
                        val finalI = if (newI < previewNewCombinedIdx) newI else newI + 1
                        // 终态 visualSlot：finalI < newVis 是 visible，否则跳过 divider 加 1
                        val newVisual =
                            if (finalI < previewNewVis) finalI else finalI + 1
                        newVisual * rowPx
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .graphicsLayer {
                            translationY = translateY
                            if (isDragging) shadowElevation = 8.dp.toPx()
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .background(if (isDragging) colors.card else Color.Transparent),
                ) {
                    CategoryRow(
                        info = info,
                        onEdit = { onEdit(info) },
                        onDragStart = {
                            // 用当前 combined 里 info.id 的位置当 dragIndex —
                            // 不依赖外层闭包里捕获的 idx（commit 后重组、行序
                            // 变了，旧 idx 就指向别的行）。
                            dragIndex = combined.indexOfFirst { it.id == info.id }
                            dragOffsetY = 0f
                        },
                        onDrag = { dy -> dragOffsetY += dy },
                        onDragEnd = {
                            // 用当前 state（不依赖 outer 闭包捕获）现算 target
                            // 落点 — gesture 触发与重组之间存在一帧间隙，闭包
                            // 捕获的 targetVisualSlot 可能是上一次 frame 的值。
                            val d = dragIndex
                            if (d >= 0) {
                                val originVisualNow =
                                    if (d < visibleCount) d else d + 1
                                val centerY = originVisualNow * rowPx +
                                    dragOffsetY + rowPx / 2f
                                val target = (centerY / rowPx).toInt()
                                    .coerceIn(0, totalSlots - 1)
                                commitDrag(
                                    combined = combined,
                                    visibleCount = visibleCount,
                                    dragIndex = d,
                                    targetVisualSlot = target,
                                    onApply = { newCombined, newVisibleCount ->
                                        workVisible = newCombined.take(newVisibleCount)
                                        workHidden = newCombined.drop(newVisibleCount)
                                        val orderedIds = newCombined.map { it.id }
                                        val hiddenIds = newCombined.drop(newVisibleCount)
                                            .map { it.id }.toSet()
                                        onCommit(orderedIds, hiddenIds)
                                    },
                                )
                            }
                            dragIndex = -1
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            dragIndex = -1
                            dragOffsetY = 0f
                        },
                    )
                }
            }

            // Divider slot — 跟着 previewNewVis 搬：跨段拖时它视觉滑到新位置，
            // 让分割线始终在 visible / hidden 段交界处。
            val dividerTranslate =
                (if (dragIndex < 0) visibleCount else previewNewVis) * rowPx
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .graphicsLayer { translationY = dividerTranslate },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(0.5.dp)
                            .background(colors.line),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "↑ 显示中 · ↓ 已隐藏",
                        color = colors.sub.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(0.5.dp)
                            .background(colors.line),
                    )
                }
            }
        }
    }
}

/**
 * Cycle 0031：拖动落点 → 新 combined 列表 + newVisibleCount。
 *
 * 关键不变量：用户指尖 Y 与 visualSlot 一一对应（slot = Y / rowPx）。所以
 * 用户希望拖动行在终态布局里的 visualSlot 就等于他松手时的 targetVisualSlot。
 *
 * 推导：
 *   - 终态 newVis = 拖动行落进哪段决定的。
 *   - 在终态布局里，combined idx k 的 visualSlot = k (if k < newVis) else k+1。
 *     所以反推：要拖动行处在 visualSlot = t 上，它的 combined 终态 idx =
 *       t       (if 目标 visible)
 *       t - 1   (if 目标 hidden — 跳过 divider 占的那个 slot)
 *   - 该 idx 就是把拖动行插进 combined'(去掉 dragIndex) 的位置。
 */
private fun commitDrag(
    combined: List<CategoryInfo>,
    visibleCount: Int,
    dragIndex: Int,
    targetVisualSlot: Int,
    onApply: (newCombined: List<CategoryInfo>, newVisibleCount: Int) -> Unit,
) {
    if (dragIndex < 0 || targetVisualSlot < 0) return
    val originVisual = if (dragIndex < visibleCount) dragIndex else dragIndex + 1
    if (targetVisualSlot == originVisual) return  // 没动

    val originIsVisible = dragIndex < visibleCount
    val targetIsVisible = when {
        targetVisualSlot < visibleCount -> true
        targetVisualSlot > visibleCount -> false
        else -> !originIsVisible  // 拖到 divider 槽 → 跨段去对侧
    }
    val newVisibleCount = when {
        originIsVisible && targetIsVisible -> visibleCount
        originIsVisible && !targetIsVisible -> visibleCount - 1
        !originIsVisible && targetIsVisible -> visibleCount + 1
        else -> visibleCount
    }
    val newCombinedIdx = (if (targetIsVisible) targetVisualSlot else targetVisualSlot - 1)
        .coerceIn(0, combined.size - 1)

    val withoutDragged = combined.toMutableList().also { it.removeAt(dragIndex) }
    val newCombined = withoutDragged.toMutableList().also {
        it.add(newCombinedIdx, combined[dragIndex])
    }
    onApply(newCombined, newVisibleCount)
}

@Composable
private fun CategoryRow(
    info: CategoryInfo,
    onEdit: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    // Cycle 0031 复修：pointerInput(key) 块的 closure 只在 key 变化时重新捕
    // 获 — `info.id` 不变 → 块里抓住的是首次组合时那一份 lambda。父级 forEachIndexed
    // 每次重组都生成新 lambda（捕获新的 idx）但永远进不来。结果第一次拖
    // 动 OK，之后所有重组里 idx 都对不上，拖 B 实际改了 C 的位置 → 用户看到
    // B 弹回去。用 rememberUpdatedState 把"最新"那份 lambda 注入到 gesture
    // detector 的 closure 里。
    val latestOnDragStart by rememberUpdatedState(onDragStart)
    val latestOnDrag by rememberUpdatedState(onDrag)
    val latestOnDragEnd by rememberUpdatedState(onDragEnd)
    val latestOnDragCancel by rememberUpdatedState(onDragCancel)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左：拖动握把
        Box(
            modifier = Modifier
                .size(36.dp)
                .pointerInput(info.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { latestOnDragStart() },
                        onDragEnd = { latestOnDragEnd() },
                        onDragCancel = { latestOnDragCancel() },
                        onDrag = { _, drag -> latestOnDrag(drag.y) },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(1.dp)
                            .background(colors.sub.copy(alpha = 0.55f)),
                    )
                }
            }
        }
        Spacer(Modifier.width(6.dp))
        // 中：分类名 + 英文 + 自定义标签
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.nameZh,
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!info.isBuiltIn) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.terra.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "自定义",
                            color = colors.terra,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            if (info.nameEn.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = info.nameEn,
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        // 右：小红点入口（编辑）
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFC5392E)),
            )
        }
    }
}
