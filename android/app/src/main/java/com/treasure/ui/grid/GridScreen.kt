package com.treasure.ui.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.Item
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.components.HeroAvatar
import com.treasure.ui.category.CategoryManager

@Composable
fun GridRoute(
    initialCategoryId: String,
    onCategoryChanged: (String) -> Unit,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenCategoryManager: () -> Unit,
    onOpenSearch: () -> Unit,
    /** Cycle 0033：从图鉴编辑态点 [编辑] 按钮 — 把选中物品发给 MainScreen，
     *  由其切到 PAGE_ADD 并起新会话。 */
    onSendToAdd: (List<String>) -> Unit = {},
    vm: GridViewModel = viewModel(factory = GridViewModel.factory(initialCategoryId)),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selecting by vm.selecting.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedIds.collectAsStateWithLifecycle()
    // Cycle 0016：之前从门厅点 doorway 后，gridCategoryId 变了，但 ViewModelStore
    // 仍然返回最早创建的 GridViewModel（factory 的 initialCategory 被忽略），结果
    // 永远停在第一次落到 GRID 页时的品类。靠 LaunchedEffect 监听 initialCategoryId
    // 改动，调度 selectCategory。
    androidx.compose.runtime.LaunchedEffect(initialCategoryId) {
        val target: String? =
            if (initialCategoryId == GridViewModel.ALL_FILTER_ID || initialCategoryId.isBlank()) null
            else initialCategoryId
        if (state.currentCategoryId != target) vm.selectCategory(target)
    }
    // Cycle 0033：编辑态下"返回"键应该退出编辑态，而非回到首页。
    androidx.activity.compose.BackHandler(enabled = selecting) { vm.exitEditMode() }
    GridScreen(
        state = state,
        selecting = selecting,
        selectedIds = selectedIds,
        onSelectCategory = { id ->
            // Cycle 0019：chip 点击 → vm 立刻切 + 通知 MainScreen 更新 saved
            // state，避免 Detail pop 回来时 LaunchedEffect 用旧 id 覆盖。
            vm.selectCategory(id)
            onCategoryChanged(id ?: GridViewModel.ALL_FILTER_ID)
        },
        onOpenItem = onOpenItem,
        onBack = onBack,
        onOpenCategoryManager = onOpenCategoryManager,
        onOpenSearch = onOpenSearch,
        onLongPressItem = { id -> vm.enterEditMode(id) },
        onToggleSelect = vm::toggleSelection,
        onExitEditMode = vm::exitEditMode,
        onDeleteSelected = vm::deleteSelected,
        onSendSelectedToAdd = {
            val ids = selectedIds.toList()
            if (ids.isNotEmpty()) {
                onSendToAdd(ids)
                vm.exitEditMode()
            }
        },
        onReorder = vm::reorder,
    )
}

@Composable
fun GridScreen(
    state: GridUiState,
    selecting: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onSelectCategory: (String?) -> Unit,
    onOpenItem: (String) -> Unit,
    onBack: () -> Unit,
    onOpenCategoryManager: () -> Unit,
    onOpenSearch: () -> Unit,  // Cycle 0031 起没人用了 — 搜索改内联到 chip 条
    onLongPressItem: (String) -> Unit = {},
    onToggleSelect: (String) -> Unit = {},
    onExitEditMode: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onSendSelectedToAdd: () -> Unit = {},
    onReorder: (List<String>) -> Unit = {},
) {
    val colors = LocalTreasureColors.current

    // Cycle 0031：把搜索从全屏 push 路由折回页内 — 点 chip 条左侧的放大镜，
    // 整条 chip 退场，原地换成圆角输入框；空 query + 点别处取消；非空时下
    // 方 grid 直接换成命中结果。
    var searchActive by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var searchQuery by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    }
    val q = searchQuery.trim()
    val displayItems = if (searchActive && q.isNotBlank()) {
        state.allVisibleItems.filter { it.matchesQuery(q) }
    } else {
        state.itemsInCategory
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        // Cycle 0031：从 LazyVerticalGrid 改成 LazyColumn + chunked(2) Row 渲染 —
        // 为了让"同一行两张卡片中任一标题两行 → 两张都两行"的同步效果可控（vertical
        // grid 没办法跨行同步行高）。每对卡的标题行数靠 TextMeasurer 预测算出。
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 28.dp,
                bottom = 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                if (selecting) {
                    EditHeader(
                        selectedCount = selectedIds.size,
                        onExit = onExitEditMode,
                        onDelete = onDeleteSelected,
                        onSend = onSendSelectedToAdd,
                    )
                } else {
                    Header(
                        total = state.totalCount,
                        onOpenCategoryManager = onOpenCategoryManager,
                    )
                }
            }
            if (!selecting) {
                item {
                    if (searchActive) {
                        SearchInputBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onCancel = {
                                searchActive = false
                                searchQuery = ""
                            },
                        )
                    } else {
                        CategoryChipsWithSearch(
                            state = state,
                            onSelectCategory = onSelectCategory,
                            onStartSearch = { searchActive = true },
                        )
                    }
                }
            }

            if (selecting) {
                // Cycle 0033：编辑态下 1-列长方形行 + 拖拽调序 + 选中态。
                item {
                    EditReorderableList(
                        items = displayItems,
                        selectedIds = selectedIds,
                        onToggleSelect = onToggleSelect,
                        onReorder = onReorder,
                    )
                }
            } else {
                items(
                    items = displayItems.chunked(2),
                    key = { pair -> pair.joinToString(",") { it.id } },
                ) { pair ->
                    ItemPairRow(
                        pair = pair,
                        onOpenItem = onOpenItem,
                        onLongPressItem = onLongPressItem,
                    )
                }
            }

            if (displayItems.isEmpty()) {
                item {
                    if (searchActive && q.isNotBlank()) {
                        SearchEmptyHint(q)
                    } else if (!searchActive) {
                        val name = state.visibleCategories
                            .firstOrNull { it.id == state.currentCategoryId }?.nameZh
                        EmptyHint(name)
                    }
                }
            }
        }
    }
}

/**
 * Cycle 0031：图鉴一行两张卡 — 用 TextMeasurer 预测两张卡标题各自需要的行
 * 数，pair 内取 max（1 或 2）后传给每张卡当 minLines/maxLines，保证两张卡
 * 标题区域同高，副标 oneLiner 永远在同一水平线对齐。
 */
@Composable
private fun ItemPairRow(
    pair: List<Item>,
    onOpenItem: (String) -> Unit,
    onLongPressItem: (String) -> Unit = {},
) {
    val titleStyle = MaterialTheme.typography.bodyLarge
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    val cellWidthPx = remember(cfg.screenWidthDp) {
        // contentPadding 22 + 22，cell 间距 16，所以一张卡 = (screen - 60) / 2
        val cellDp = (cfg.screenWidthDp - 22 * 2 - 16) / 2
        with(density) { cellDp.dp.toPx().toInt() }.coerceAtLeast(1)
    }
    val titleLines = remember(pair, cellWidthPx, titleStyle) {
        pair.maxOfOrNull { item ->
            val title = "${item.brand} ${item.model}".trim().ifBlank { item.nickname }
            textMeasurer.measure(
                text = androidx.compose.ui.text.AnnotatedString(title),
                style = titleStyle,
                constraints = androidx.compose.ui.unit.Constraints(maxWidth = cellWidthPx),
                softWrap = true,
            ).lineCount.coerceAtMost(2)
        } ?: 1
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        pair.forEach { item ->
            ItemCard(
                item = item,
                titleLines = titleLines,
                onClick = { onOpenItem(item.id) },
                onLongPress = { onLongPressItem(item.id) },
                modifier = Modifier.weight(1f),
            )
        }
        if (pair.size == 1) Box(modifier = Modifier.weight(1f)) {}
    }
}

private fun com.treasure.core.domain.Item.matchesQuery(q: String): Boolean {
    val needle = q.lowercase()
    return brand.lowercase().contains(needle) ||
        model.lowercase().contains(needle) ||
        nickname.lowercase().contains(needle)
}

@Composable
private fun SearchEmptyHint(q: String) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "没有匹配 \"$q\" 的物品",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        )
    }
}

@Composable
private fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val focusRequester = androidx.compose.runtime.remember {
        androidx.compose.ui.focus.FocusRequester()
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(999.dp))
            .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        SearchGlyph(color = colors.sub)
        Spacer(Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "搜索 品牌 / 型号 / 昵称",
                    color = colors.sub.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.terra),
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    color = colors.ink,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        // 取消：query 空时直接退出 search 态，非空时先清空 query。
        Text(
            text = if (query.isEmpty()) "取消" else "清空",
            color = colors.terra,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable {
                    if (query.isEmpty()) onCancel() else onQueryChange("")
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CategoryChipsWithSearch(
    state: GridUiState,
    onSelectCategory: (String?) -> Unit,
    onStartSearch: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
    ) {
        // Cycle 0031：搜索 icon 移到 chip 条最左 — 之前是右上角全屏 push。
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(0.5.dp, colors.line, CircleShape)
                .clickable(onClick = onStartSearch),
            contentAlignment = Alignment.Center,
        ) {
            SearchGlyph(color = colors.ink)
        }
        Spacer(Modifier.size(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            CategoryChips(state, onSelectCategory)
        }
    }
}

@Composable
private fun SearchGlyph(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(16.dp),
    ) {
        val cx = size.width * 0.42f
        val cy = size.height * 0.42f
        val r = size.minDimension * 0.32f
        drawCircle(
            color = color,
            radius = r,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.10f),
        )
        // 手柄
        val handleStart = androidx.compose.ui.geometry.Offset(
            cx + r * 0.7f,
            cy + r * 0.7f,
        )
        val handleEnd = androidx.compose.ui.geometry.Offset(
            size.width * 0.88f,
            size.height * 0.88f,
        )
        drawLine(
            color = color,
            start = handleStart,
            end = handleEnd,
            strokeWidth = size.minDimension * 0.10f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}

@Composable
private fun Header(total: Int, onOpenCategoryManager: () -> Unit) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        // Cycle 0031：标题行用 LastBaseline 把 Edit 小字 + 小红点 的基线对齐
        // 到 "Treasure" 的文字基线 — 视觉上"贴底"一致。dot 不是 Text，包到
        // 一个 fixed height Box 内让基线对齐落到 Box 底部。
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Treasure",
                color = colors.ink,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onOpenCategoryManager)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "Edit",
                    color = colors.terra,
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.size(6.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFFC5392E)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "$total ITEMS",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun CategoryChips(
    state: GridUiState,
    onSelectCategory: (String?) -> Unit,
) {
    // 头号 chip = "全部"（null 过滤），后面跟可见分类（按 sort_order 排序）。
    //
    // Cycle 0024：之前 LaunchedEffect 每次 selectedIndex 变都 animateScrollToItem，
    // 用户点一下 chip 就把这个 chip 拽到行首，破坏了从门厅过来时建立的"位置感"。
    // 改成只在目标 chip "其实看不到"（不在 visibleItemsInfo 里）时才滚 — 用户
    // 自己点的 chip 一定可见，所以不会动；门厅跳过来时如果目标在屏幕外才滚。
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val selectedIndex: Int = if (state.currentCategoryId == null) 0
        else 1 + state.visibleCategories.indexOfFirst { it.id == state.currentCategoryId }
            .coerceAtLeast(0)
    androidx.compose.runtime.LaunchedEffect(selectedIndex) {
        val visible = listState.layoutInfo.visibleItemsInfo
        val isVisible = visible.any { it.index == selectedIndex }
        if (!isVisible) listState.animateScrollToItem(selectedIndex)
    }
    androidx.compose.foundation.lazy.LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            AllChip(
                count = state.totalCount,
                selected = state.currentCategoryId == null,
                onClick = { onSelectCategory(null) },
            )
        }
        items(state.visibleCategories.size) { idx ->
            val c = state.visibleCategories[idx]
            CategoryChip(
                info = c,
                count = state.countByCategoryId[c.id] ?: 0,
                selected = c.id == state.currentCategoryId,
                onClick = { onSelectCategory(c.id) },
            )
        }
    }
}

@Composable
private fun AllChip(count: Int, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    val bg = if (selected) colors.ink else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) colors.paper else colors.ink
    val border = if (selected) colors.ink else colors.line
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = "全部",
            color = fg,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "  $count",
            color = fg.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CategoryChip(
    info: CategoryInfo,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val bg = if (selected) colors.ink else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) colors.paper else colors.ink
    val border = if (selected) colors.ink else colors.line
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = info.nameZh,
            color = fg,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(0.dp))
        Text(
            text = "  $count",
            color = fg.copy(alpha = 0.5f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ItemCard(
    item: Item,
    titleLines: Int = 1,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongPress),
    ) {
        // Square hero plate
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.1f)
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(1f),
            ) {
                HeroAvatar(item = item, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${item.brand} ${item.model}",
            color = colors.ink,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = titleLines,
            minLines = titleLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.oneLiner,
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

// ─── Cycle 0033：编辑态 ────────────────────────────────────────────────

@Composable
private fun EditHeader(
    selectedCount: Int,
    onExit: () -> Unit,
    onDelete: () -> Unit,
    onSend: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "完成",
            color = colors.sub,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onExit)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (selectedCount == 0) "已选 0" else "已选 $selectedCount",
            color = colors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.weight(1f))
        val enabled = selectedCount > 0
        Text(
            text = "删除${if (enabled) " ($selectedCount)" else ""}",
            color = if (enabled) colors.terra else colors.sub.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(enabled = enabled, onClick = onDelete)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = "编辑${if (enabled) " ($selectedCount)" else ""}",
            color = if (enabled) colors.terra else colors.sub.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(enabled = enabled, onClick = onSend)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

/**
 * Cycle 0033：编辑态 1-列长方形行 + 拖拽调序。简单实现：每行右侧有"⋮⋮"
 * 把手；long-press + drag 启动拖动；松手时把行序写回 sortOrder。中间用
 * graphicsLayer 的 translationY 给视觉反馈，避免实际重排导致 LazyColumn
 * 滑动错位。
 */
private val EDIT_ROW_HEIGHT = 76.dp

@Composable
private fun EditReorderableList(
    items: List<Item>,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val rowPx = with(density) { EDIT_ROW_HEIGHT.toPx() }

    // 工作副本：拖动期间 work 反映 user-visible 顺序；松手时 onReorder。
    var work by remember(items) { mutableStateOf(items) }
    androidx.compose.runtime.LaunchedEffect(items) {
        // items 上游变了（删除等）就刷新 work，但拖动中不要打断。
        if (work.size != items.size || work.map { it.id }.toSet() != items.map { it.id }.toSet()) {
            work = items
        }
    }

    var dragIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(EDIT_ROW_HEIGHT * work.size),
    ) {
        work.forEachIndexed { idx, item ->
            val isDragging = idx == dragIndex
            val targetIdx = if (dragIndex < 0) idx else {
                val centerY = idx * rowPx + (if (isDragging) dragOffsetY else 0f) + rowPx / 2f
                (centerY / rowPx).toInt().coerceIn(0, work.size - 1)
            }
            val translateY: Float = when {
                isDragging -> idx * rowPx + dragOffsetY
                dragIndex < 0 -> idx * rowPx
                else -> {
                    val originIdx = dragIndex
                    val tgt = run {
                        val centerY = originIdx * rowPx + dragOffsetY + rowPx / 2f
                        (centerY / rowPx).toInt().coerceIn(0, work.size - 1)
                    }
                    val newI = when {
                        idx == originIdx -> tgt
                        originIdx < idx && idx <= tgt -> idx - 1
                        originIdx > idx && idx >= tgt -> idx + 1
                        else -> idx
                    }
                    newI * rowPx
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(EDIT_ROW_HEIGHT)
                    .androidx_graphicsLayer(translateY, isDragging)
                    .androidx_zIndex(if (isDragging) 1f else 0f)
                    .background(if (isDragging) colors.card else colors.paper),
            ) {
                EditRow(
                    item = item,
                    selected = item.id in selectedIds,
                    onTap = { onToggleSelect(item.id) },
                    onDragStart = {
                        dragIndex = idx
                        dragOffsetY = 0f
                    },
                    onDrag = { delta -> dragOffsetY += delta },
                    onDragEnd = {
                        if (dragIndex >= 0) {
                            val originIdx = dragIndex
                            val centerY = originIdx * rowPx + dragOffsetY + rowPx / 2f
                            val tgt = (centerY / rowPx).toInt().coerceIn(0, work.size - 1)
                            if (tgt != originIdx) {
                                val mut = work.toMutableList()
                                val moved = mut.removeAt(originIdx)
                                mut.add(tgt, moved)
                                work = mut
                                onReorder(mut.map { it.id })
                            }
                        }
                        dragIndex = -1
                        dragOffsetY = 0f
                    },
                )
            }
        }
    }
}

@Composable
private fun EditRow(
    item: Item,
    selected: Boolean,
    onTap: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val latestDragStart by androidx.compose.runtime.rememberUpdatedState(onDragStart)
    val latestDrag by androidx.compose.runtime.rememberUpdatedState(onDrag)
    val latestDragEnd by androidx.compose.runtime.rememberUpdatedState(onDragEnd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(EDIT_ROW_HEIGHT)
            .clickable(onClick = onTap)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 选中圆点 / 勾
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) colors.terra else androidx.compose.ui.graphics.Color.Transparent)
                .border(0.5.dp, if (selected) colors.terra else colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Text(
                    text = "✓",
                    color = colors.paper,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Spacer(Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(6.dp),
        ) {
            HeroAvatar(item = item, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val title = listOf(item.brand, item.model)
                .filter { it.isNotBlank() }
                .joinToString(" ").ifBlank { item.nickname }
            Text(
                text = title,
                color = colors.ink,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (item.oneLiner.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.oneLiner,
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        // 拖把手 — long-press 后 drag。
        Box(
            modifier = Modifier
                .size(40.dp)
                .dragHandle(
                    onDragStart = { latestDragStart() },
                    onDrag = { _, drag -> latestDrag(drag.y) },
                    onDragEnd = { latestDragEnd() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            DragHandleGlyph(colors.sub)
        }
    }
}

@Composable
private fun DragHandleGlyph(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val sw = 1.4.dp.toPx()
        val cy1 = w * 0.35f
        val cy2 = w * 0.65f
        listOf(cy1, cy2).forEach { y ->
            androidx.compose.ui.geometry.Offset(w * 0.25f, y).also { p1 ->
                drawLine(color, p1, androidx.compose.ui.geometry.Offset(w * 0.75f, y), strokeWidth = sw)
            }
        }
    }
}

private fun Modifier.androidx_graphicsLayer(
    translationY: Float,
    elevated: Boolean,
): Modifier = this then Modifier.graphicsLayer(
    translationY = translationY,
    shadowElevation = if (elevated) 24f else 0f,
)

private fun Modifier.androidx_zIndex(value: Float): Modifier =
    this then Modifier.zIndex(value)

private fun Modifier.dragHandle(
    onDragStart: () -> Unit,
    onDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, drag: androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: () -> Unit,
): Modifier = this then Modifier.pointerInput(Unit) {
    detectDragGesturesAfterLongPress(
        onDragStart = { onDragStart() },
        onDrag = onDrag,
        onDragEnd = onDragEnd,
    )
}

@Composable
private fun EmptyHint(categoryName: String?) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (categoryName == null) "图鉴还空着 — 去录入页加点东西"
                else "$categoryName 还没有收藏",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
