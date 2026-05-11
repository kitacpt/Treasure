@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.treasure.core.domain.HeroVector
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors
import kotlin.math.roundToInt

/**
 * Cycle 0026：分类管理抽屉。Cycle 0028 重构：
 *   - 去掉副标题"隐藏只是 ..."
 *   - 去掉每行 [隐藏 / 显示] pill 按钮，**用拖动取代**
 *   - 去掉底部 [完成] 按钮（实时生效）
 *   - "编辑 →" 文字换成右侧小红点（点开进编辑页）
 *   - 拖动逻辑：长按行的左边握把 → 上下拖；中间一条灰线分割"显示中 / 已隐
 *     藏"两段。拖到分割线另一侧 = 切换 hidden；同段拖动 = 改 sort_order。
 *     松手时一次性提交 reorder + setHidden 到仓库。
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
        // Cycle 0029：编辑 / 新建都拆成全屏路由，Manager 只剩 List 模式。点击
        // 时 onClose() 收抽屉再 navigate 过去，避免抽屉和全屏页同时存在。
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
// Cycle 0030：divider 跟 row 同高，作为一个 "块" 占用一个 slot —— 之前是
// 单独 36dp 浮层，跟 drag 索引数学错位，导致拖到底部"看不到下面去"。

@Composable
private fun CategoryList(
    all: List<CategoryInfo>,
    onEdit: (CategoryInfo) -> Unit,
    onAdd: () -> Unit,
    onCommit: (orderedIds: List<String>, hiddenIds: Set<String>) -> Unit,
) {
    val colors = LocalTreasureColors.current

    // 本地"工作副本"：仓库一变化就 reset，但拖动期间所有移动都在这上面跑，
    // 松手时一次性把最终 orderedIds + hiddenIds 提交回仓库。
    var workVisible by remember(all) {
        mutableStateOf(all.filter { !it.hidden })
    }
    var workHidden by remember(all) {
        mutableStateOf(all.filter { it.hidden })
    }

    val density = LocalDensity.current
    val rowPx = with(density) { ROW_HEIGHT.toPx() }

    // 拖动状态。dragIndex 是 *合并后 combined* 里的逻辑索引（不含 divider）。
    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val combined = workVisible + workHidden
    val visibleCount = workVisible.size

    // Cycle 0030：divider 占用一个 row-height slot。视觉布局 (visualSlot):
    //   [0..visibleCount-1]   = workVisible 各行
    //   [visibleCount]        = divider slot（同高）
    //   [visibleCount+1..N]   = workHidden 各行
    // 总 slot 数 = combined.size + 1
    fun combinedToVisual(i: Int): Int = if (i < visibleCount) i else i + 1
    val totalSlots = combined.size + 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        // 头：左 "分类管理"，右 "+ 新增分类"
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

        // 渲染区。所有 slot（含 divider）等高 ROW_HEIGHT，drag 数学统一在
        // visualSlot 域里跑。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT * totalSlots),
        ) {
            // 计算被拖行的目标 visualSlot。坐标域：visualSlot 0..totalSlots-1
            // dragOffsetY 是相对于原位的视觉偏移
            val targetVisualSlot = if (dragIndex == -1) -1 else {
                val originVisual = combinedToVisual(dragIndex)
                val draggedCenterY = originVisual * rowPx + dragOffsetY + rowPx / 2f
                (draggedCenterY / rowPx).toInt()
                    .coerceIn(0, totalSlots - 1)
            }
            // divider visualSlot = visibleCount。drag 到 divider 上时按拖动
            // 方向 snap 到邻近 row 那侧：从上拖来 → 落到 divider 上方；从下
            // 拖来 → 落到 divider 下方。
            val effectiveTargetSlot = when {
                targetVisualSlot < 0 -> -1
                targetVisualSlot == visibleCount -> {
                    val originVisual = combinedToVisual(dragIndex)
                    if (originVisual < visibleCount) visibleCount  // 从上来 → 落进 hidden 首位
                    else visibleCount  // 从下来 → 也落进 hidden 首位（divider 上方是 visible 末，正好替换那个位置反而更直觉？此处按 dropMode 决定）
                    // 实际更简单：dragging 行到 divider slot 表示用户希望
                    // 跨段。从上来：行变 hidden，新 slot = visibleCount（divider 自身让位）
                    //         从下来：行变 visible，新 slot = visibleCount - 1（divider 自身让位向下）
                }
                else -> targetVisualSlot
            }

            combined.forEachIndexed { idx, info ->
                val isDragging = idx == dragIndex
                val baseVisual = combinedToVisual(idx)
                val baseY = baseVisual * rowPx

                // make-room shift：dragging 的目标 visualSlot 决定其它行怎么让
                // 自己跳过 divider slot — 把 divider 也算成"占位"。
                val shift = computeShift(
                    rowVisual = baseVisual,
                    dragOriginVisual = if (dragIndex == -1) -1 else combinedToVisual(dragIndex),
                    dragTargetVisual = effectiveTargetSlot,
                    dividerVisual = visibleCount,
                    rowPx = rowPx,
                )
                val translateY = if (isDragging) dragOffsetY else shift

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .graphicsLayer {
                            translationY = baseY + translateY
                            if (isDragging) shadowElevation = 8.dp.toPx()
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .background(
                            if (isDragging) colors.card else Color.Transparent,
                        ),
                ) {
                    CategoryRow(
                        info = info,
                        onEdit = { onEdit(info) },
                        onDragStart = {
                            dragIndex = idx
                            dragOffsetY = 0f
                        },
                        onDrag = { dy -> dragOffsetY += dy },
                        onDragEnd = {
                            commitDrag(
                                combined = combined,
                                visibleCount = visibleCount,
                                dragIndex = dragIndex,
                                targetVisualSlot = effectiveTargetSlot,
                                onApply = { newCombined, newVisibleCount ->
                                    workVisible = newCombined.take(newVisibleCount)
                                    workHidden = newCombined.drop(newVisibleCount)
                                    val orderedIds = newCombined.map { it.id }
                                    val hiddenIds = newCombined.drop(newVisibleCount)
                                        .map { it.id }.toSet()
                                    onCommit(orderedIds, hiddenIds)
                                },
                            )
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

            // Divider slot — 占一个 row-height 的位置，里面是分割线 + 标签
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .graphicsLayer { translationY = visibleCount * rowPx },
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

/** Make-room shift：拖动期间，其它行让开 dragging 行原来的位置 / 把目标位
 *  让出来。Visual 坐标域，divider 占自己的 slot。 */
private fun computeShift(
    rowVisual: Int,
    dragOriginVisual: Int,
    dragTargetVisual: Int,
    dividerVisual: Int,
    rowPx: Float,
): Float {
    if (dragOriginVisual < 0 || dragTargetVisual < 0) return 0f
    if (rowVisual == dragOriginVisual) return 0f
    // 拖动行将移到 dragTargetVisual。其它行的 visualSlot 怎么变？
    // 把"divider 不动"作为约束，dragging 行从 origin 移到 target，介于两者
    // 之间的非 divider 行需要让位。
    return when {
        dragOriginVisual < dragTargetVisual -> {
            // 向下拖。介于 (origin, target] 的行向上让 rowPx 一个 slot。
            // 但 divider slot 不实际有内容，跳过它。
            if (rowVisual in (dragOriginVisual + 1)..dragTargetVisual && rowVisual != dividerVisual) -rowPx
            else 0f
        }
        dragOriginVisual > dragTargetVisual -> {
            // 向上拖。介于 [target, origin) 的行向下让 rowPx 一个 slot。
            if (rowVisual in dragTargetVisual until dragOriginVisual && rowVisual != dividerVisual) rowPx
            else 0f
        }
        else -> 0f
    }
}

/** drag end 提交。基于 dragging 行原索引 + visualSlot 目标，算出新的
 *  combined 列表 + 新 visibleCount。 */
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

    // 1. 移除 dragging 行 → 中间态 combined
    val withoutDragged = combined.toMutableList().also { it.removeAt(dragIndex) }
    val newVisibleCountWithoutDragged =
        if (dragIndex < visibleCount) visibleCount - 1 else visibleCount

    // 2. 把 targetVisualSlot 换算成新 combined 的索引 + 是否 visible
    //    在中间态里 visualSlot:
    //      [0..newVis-1]   visible
    //      [newVis]        divider
    //      [newVis+1..N-1] hidden（N = withoutDragged.size + 1）
    val newVis = newVisibleCountWithoutDragged
    // targetVisualSlot 是用户期望的"落点"，但用户是基于"含 dragging 的原 visualSlot"
    // 选的位置。在中间态里要补偿：如果 dragging 来自 visible 段，那段所有 slot 在
    // 中间态里都往左 / 上移一格。
    val adjustedTarget = if (originVisual < targetVisualSlot) targetVisualSlot - 1
                         else targetVisualSlot

    val (newCombinedIdx, newIsVisible) = when {
        adjustedTarget < newVis -> Pair(adjustedTarget, true)
        adjustedTarget == newVis -> {
            // 落在 divider slot — 按拖动方向决定吸附
            val droppedToHidden = originVisual < targetVisualSlot
            if (droppedToHidden) Pair(newVis, false)  // 第一个 hidden 位
            else Pair(newVis - 1, true)               // 最后一个 visible 位
        }
        else -> {
            // adjustedTarget > newVis：hidden 段
            val hiddenIdxOffset = adjustedTarget - newVis - 1
            Pair(newVis + hiddenIdxOffset, false)
        }
    }

    val newCombined = withoutDragged.toMutableList().also {
        it.add(newCombinedIdx.coerceIn(0, it.size), combined[dragIndex])
    }
    val newVisibleCount = when {
        newIsVisible && dragIndex < visibleCount -> visibleCount  // visible → visible
        newIsVisible && dragIndex >= visibleCount -> visibleCount + 1  // hidden → visible
        !newIsVisible && dragIndex < visibleCount -> visibleCount - 1  // visible → hidden
        else -> visibleCount  // hidden → hidden
    }.coerceIn(0, newCombined.size)

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
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragCancel() },
                        onDrag = { _, drag -> onDrag(drag.y) },
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
