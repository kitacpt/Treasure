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
private val DIVIDER_HEIGHT = 36.dp

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
    val dividerPx = with(density) { DIVIDER_HEIGHT.toPx() }

    // 拖动状态。索引在 *合并后的 visible+hidden 列表* 里取值；divider 不占
    // 一个独立 index，只是渲染时插一段空高。
    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    val combined = workVisible + workHidden
    val visibleCount = workVisible.size

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

        // 渲染区。每条用 Box + graphicsLayer translateY 给"被拖动的行"
        // 移动 + 其他行做 make-room shift；不重排 list 本身，松手才重排。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    // visibleCount 个 row + divider + (combined.size - visibleCount) row
                    ROW_HEIGHT * combined.size + DIVIDER_HEIGHT,
                ),
        ) {
            // 1) 算每行此刻的视觉 y：基础位置 + （如果不是被拖动的）shift
            //    被拖动的行 = 基础位置 + dragOffsetY
            //    其他行 = 基础位置 + makeRoomShift
            val targetIndex = if (dragIndex == -1) -1 else {
                // 把 dragOffsetY 换算成 "目标 visual index"（在 combined 中）。
                // 但要考虑 divider 占空：visual 上 divider 位置 = visibleCount * rowPx
                // 一个被拖动的行视觉中心 y = dragIndex * rowPx + (if 跨过 divider 加 dividerPx) + dragOffsetY
                val baseY = dragIndex * rowPx +
                    (if (dragIndex >= visibleCount) dividerPx else 0f)
                val targetY = baseY + dragOffsetY
                // 把 targetY 反算成 index：先去掉 divider 偏移
                val withoutDivider = if (targetY > visibleCount * rowPx + dividerPx) {
                    targetY - dividerPx
                } else if (targetY > visibleCount * rowPx) {
                    // 落在 divider 区间内 — 视为靠近的那一侧
                    if (targetY < visibleCount * rowPx + dividerPx / 2) visibleCount * rowPx - 1f
                    else visibleCount * rowPx + 1f
                } else {
                    targetY
                }
                (withoutDivider / rowPx).roundToInt()
                    .coerceIn(0, combined.size - 1)
            }

            combined.forEachIndexed { idx, info ->
                val isDragging = idx == dragIndex
                // 基础 y
                val baseY = idx * rowPx + (if (idx >= visibleCount) dividerPx else 0f)
                // make-room shift
                val shift = when {
                    dragIndex == -1 || isDragging -> 0f
                    dragIndex < idx && idx <= targetIndex -> -rowPx
                    dragIndex > idx && idx >= targetIndex -> rowPx
                    else -> 0f
                }
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
                            if (dragIndex >= 0 && targetIndex != dragIndex) {
                                // commit move：把 dragIndex 行挪到 targetIndex
                                val newCombined = combined.toMutableList()
                                val moved = newCombined.removeAt(dragIndex)
                                newCombined.add(
                                    targetIndex.coerceIn(0, newCombined.size),
                                    moved,
                                )
                                // 重算 visibleCount：依旧由"基础位置 vs divider"
                                // 决定。即 targetIndex < visibleCount → 落在显示
                                // 段，否则隐藏段。需要按 movement 调 visibleCount：
                                //  从显示拖去隐藏：visibleCount--
                                //  从隐藏拖去显示：visibleCount++
                                //  同段：不变
                                val wasVisible = dragIndex < visibleCount
                                val nowVisible = targetIndex < visibleCount
                                val newVisibleCount = when {
                                    wasVisible && !nowVisible -> visibleCount - 1
                                    !wasVisible && nowVisible -> visibleCount + 1
                                    else -> visibleCount
                                }.coerceIn(0, newCombined.size)
                                workVisible = newCombined.take(newVisibleCount)
                                workHidden = newCombined.drop(newVisibleCount)
                                val orderedIds = newCombined.map { it.id }
                                val hiddenIds = newCombined.drop(newVisibleCount)
                                    .map { it.id }.toSet()
                                onCommit(orderedIds, hiddenIds)
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

            // Divider — 在显示段和隐藏段之间画一条灰线 + 小标签
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DIVIDER_HEIGHT)
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
