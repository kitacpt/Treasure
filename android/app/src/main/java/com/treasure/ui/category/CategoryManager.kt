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
    vm: CategoryManagerViewModel = viewModel(factory = CategoryManagerViewModel.Factory),
) {
    val colors = LocalTreasureColors.current
    val all by vm.all.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf<Mode>(Mode.List) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        when (val m = mode) {
            is Mode.List -> CategoryList(
                all = all,
                onEdit = { mode = Mode.Edit(it) },
                onAdd = { mode = Mode.Add },
                onCommit = { orderedIds, hiddenIds ->
                    vm.applyReorder(orderedIds, hiddenIds)
                },
            )
            is Mode.Edit -> CategoryEditor(
                initial = m.info,
                onBack = { mode = Mode.List },
                onSaveBuiltIn = { heroVector, hidden ->
                    vm.saveHeroVectorOnly(m.info.id, heroVector)
                    if (hidden != m.info.hidden) vm.setHidden(m.info.id, hidden)
                    mode = Mode.List
                },
                onSaveCustom = { nameZh, nameEn, heroVector, hidden ->
                    vm.saveCustom(m.info.id, nameZh, nameEn, heroVector)
                    if (hidden != m.info.hidden) vm.setHidden(m.info.id, hidden)
                    mode = Mode.List
                },
                onDelete = {
                    vm.deleteCustom(m.info.id)
                    mode = Mode.List
                },
            )
            is Mode.Add -> CategoryEditor(
                initial = null,
                onBack = { mode = Mode.List },
                onSaveBuiltIn = { _, _ -> /* unreachable */ },
                onSaveCustom = { nameZh, nameEn, heroVector, _ ->
                    vm.addCustom(nameZh, nameEn, heroVector)
                    mode = Mode.List
                },
                onDelete = { /* unreachable when initial == null */ },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

private sealed interface Mode {
    data object List : Mode
    data class Edit(val info: CategoryInfo) : Mode
    data object Add : Mode
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = "长按 ≡ 拖动 — 跨过分割线即可隐藏 / 显示，所有变更实时生效",
            color = colors.sub.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
        )
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

// ─── Editor ───────────────────────────────────────────────────────────────

/** [initial] = null → 新增；non-null → 编辑既有项。 */
@Composable
private fun CategoryEditor(
    initial: CategoryInfo?,
    onBack: () -> Unit,
    onSaveBuiltIn: (heroVector: HeroVector, hidden: Boolean) -> Unit,
    onSaveCustom: (nameZh: String, nameEn: String, heroVector: HeroVector, hidden: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val isBuiltIn = initial?.isBuiltIn == true
    val isAdd = initial == null
    var nameZh by remember { mutableStateOf(initial?.nameZh.orEmpty()) }
    var nameEn by remember { mutableStateOf(initial?.nameEn.orEmpty()) }
    // Cycle 0028：插画必填 — null 表示用户还没挑过。内建自动锁定到 enum 的
    // defaultHeroVector（CategoryInfo.heroVector 已经被 repo 覆盖了）。自定
    // 义首次创建时给 null，强迫用户选；编辑现有自定义时回填已有的。
    var heroVector by remember {
        mutableStateOf<HeroVector?>(
            when {
                isAdd -> null
                isBuiltIn -> initial?.heroVector // 显示用，但保存时不写入
                else -> initial?.heroVector
            },
        )
    }
    var hidden by remember { mutableStateOf(initial?.hidden == true) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val canSave = (isBuiltIn || nameZh.isNotBlank()) && heroVector != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        // 头：左 ‹ 返回，右 保存 / 新建
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "‹ 返回",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (isAdd) "新建" else "保存",
                color = if (canSave) colors.terra else colors.sub.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(enabled = canSave) {
                        val hv = heroVector ?: return@clickable
                        if (isBuiltIn) onSaveBuiltIn(hv, hidden)
                        else onSaveCustom(nameZh, nameEn, hv, hidden)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isAdd) "新增分类" else "编辑 ${initial?.nameZh}",
            color = colors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        if (isBuiltIn) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "内建分类的名字和插画不可改；只能切显示状态。",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
            )
        } else if (isAdd) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "新建必须填中文名 + 选一张插画。",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
            )
        }
        Spacer(Modifier.height(18.dp))

        // 顶部：插画"头像" — 像物品编辑页那样的 112dp 大圆
        AvatarHero(
            heroVector = heroVector,
            placeholderEmpty = isAdd && heroVector == null,
        )
        Spacer(Modifier.height(10.dp))

        // 插画选择 row — 内建锁定（点击没反应），自定义可选
        Text(
            text = if (isBuiltIn) "插画（内建已固定）" else "插画 · 必选",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(6.dp))
        HeroVectorRow(
            selected = heroVector,
            enabled = !isBuiltIn,
            onSelect = { heroVector = it },
        )
        Spacer(Modifier.height(20.dp))

        // 中文 / 英文名（内建锁定）
        FieldRow(label = "中文名") {
            EditorTextField(
                value = nameZh,
                onValueChange = { nameZh = it },
                enabled = !isBuiltIn,
                placeholder = "如 图书",
            )
        }
        Spacer(Modifier.height(10.dp))
        FieldRow(label = "英文名") {
            EditorTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                enabled = !isBuiltIn,
                placeholder = "如 Books（可选）",
            )
        }
        Spacer(Modifier.height(16.dp))

        // 显示 toggle（仅编辑模式，新增模式默认显示状态）
        if (!isAdd) {
            FieldRow(label = "显示") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillChip("显示", !hidden) { hidden = false }
                    PillChip("隐藏", hidden) { hidden = true }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // 删除（仅自定义 + 编辑模式）
        if (initial != null && !isBuiltIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .border(0.5.dp, colors.terra.copy(alpha = 0.55f))
                    .clickable { confirmingDelete = true }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "删除分类",
                    color = colors.terra,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (confirmingDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("删除 ${initial.nameZh}？") },
            text = {
                Text(
                    text = "这只是删掉这个分类本身。原本收在这里的物品不会被删 — 它们会被自动重新归到\"电子产品\"分类下，进图鉴后可手动改类别。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) { Text("删除", color = colors.terra) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("取消") }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

/**
 * Cycle 0028：分类编辑页的"头像"。借用 HeroIllustration 但包成同样大小的
 * 112dp 圆，跟物品编辑页 HeroAvatarPicker 的 hero plate 视觉一致。null
 * 时画 + 占位提示用户去下面 row 挑。
 */
@Composable
private fun AvatarHero(
    heroVector: HeroVector?,
    placeholderEmpty: Boolean,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(colors.paper)
                .border(0.8.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (heroVector == null) {
                Text(
                    text = if (placeholderEmpty) "+ 选张插画" else "—",
                    color = colors.sub.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            } else {
                // 用一份兜底 palette + 这个 heroVector 合成 stub item 画
                val stub = remember(heroVector) {
                    com.treasure.core.domain.Item(
                        id = "preview",
                        category = "preview",
                        brand = "", model = "", nickname = "", acquired = "", parted = null,
                        status = com.treasure.core.domain.ItemStatus.OWNED,
                        palette = listOf("#0e0e0e", "#a47836", "#e8e2d4", "#5a5a5a"),
                        oneLiner = "",
                        heroVector = heroVector,
                        specs = emptyList(),
                        history = emptyList(),
                        photos = emptyList(),
                        createdAt = 0L, updatedAt = 0L,
                    )
                }
                Box(modifier = Modifier.size(80.dp)) {
                    HeroIllustration(item = stub, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, content: @Composable () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(64.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
) {
    val colors = LocalTreasureColors.current
    Column {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.sub.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = enabled,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.terra),
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    color = if (enabled) colors.ink else colors.sub.copy(alpha = 0.5f),
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.line),
        )
    }
}

@Composable
private fun PillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) colors.ink else Color.Transparent)
            .border(
                0.5.dp,
                if (selected) colors.ink else colors.line,
                RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (selected) colors.paper else colors.ink,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HeroVectorRow(
    selected: HeroVector?,
    enabled: Boolean,
    onSelect: (HeroVector) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HeroVector.entries.forEach { hv ->
            val isSel = hv == selected
            val rowAlpha = if (enabled) 1f else 0.55f
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        (if (isSel) colors.ink else Color.Transparent)
                            .copy(alpha = if (isSel) rowAlpha else 1f),
                    )
                    .border(
                        0.5.dp,
                        if (isSel) colors.ink.copy(alpha = rowAlpha) else colors.line,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable(enabled = enabled) { onSelect(hv) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = heroLabel(hv),
                    color = (if (isSel) colors.paper else colors.ink).copy(alpha = rowAlpha),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun heroLabel(hv: HeroVector): String = when (hv) {
    HeroVector.RACKET -> "球拍"
    HeroVector.SHOES -> "鞋"
    HeroVector.CAMERA_DSLR -> "单反"
    HeroVector.CAMERA_RANGEFINDER -> "旁轴"
    HeroVector.LENS_PRIME -> "镜头"
    HeroVector.TRIPOD -> "三脚架"
    HeroVector.CAR_SEDAN -> "轿车"
    HeroVector.CAR_SUV -> "SUV"
    HeroVector.LAPTOP -> "笔记本"
    HeroVector.TABLET -> "平板"
    HeroVector.EARBUDS -> "耳机"
    HeroVector.KINDLE -> "电纸书"
    HeroVector.WATCH -> "手表"
    HeroVector.ESPRESSO_MACHINE -> "意式机"
    HeroVector.COFFEE_GRINDER -> "磨豆机"
    HeroVector.COFFEE_BEAN -> "咖啡豆"
    HeroVector.WINE_BOTTLE -> "酒瓶"
    HeroVector.COCKTAIL_GLASS -> "鸡尾酒杯"
    HeroVector.GENERIC -> "通用"
}
