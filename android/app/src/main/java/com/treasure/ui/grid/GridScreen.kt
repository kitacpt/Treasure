package com.treasure.ui.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.Category
import com.treasure.core.domain.Item
import com.treasure.theme.LocalTreasureColors
import com.treasure.illust.HeroIllustration
import com.treasure.ui.components.HeroAvatar

@Composable
fun GridRoute(
    initialCategoryId: String,
    onCategoryChanged: (String) -> Unit,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    vm: GridViewModel = viewModel(factory = GridViewModel.factory(initialCategoryId)),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    // Cycle 0016：之前从门厅点 doorway 后，gridCategoryId 变了，但 ViewModelStore
    // 仍然返回最早创建的 GridViewModel（factory 的 initialCategory 被忽略），结果
    // 永远停在第一次落到 GRID 页时的品类。靠 LaunchedEffect 监听 initialCategoryId
    // 改动，调度 selectCategory。
    androidx.compose.runtime.LaunchedEffect(initialCategoryId) {
        val target: com.treasure.core.domain.Category? =
            if (initialCategoryId == GridViewModel.ALL_FILTER_ID || initialCategoryId.isBlank()) null
            else com.treasure.core.domain.Category.fromId(initialCategoryId)
        if (state.currentCategory != target) vm.selectCategory(target)
    }
    GridScreen(
        state = state,
        onSelectCategory = { cat ->
            // Cycle 0019：chip 点击 → vm 立刻切 + 通知 MainScreen 更新 saved
            // state，避免 Detail pop 回来时 LaunchedEffect 用旧 id 覆盖。
            vm.selectCategory(cat)
            onCategoryChanged(cat?.id ?: GridViewModel.ALL_FILTER_ID)
        },
        onOpenItem = onOpenItem,
        onBack = onBack,
    )
}

@Composable
fun GridScreen(
    state: GridUiState,
    onSelectCategory: (Category?) -> Unit,
    onOpenItem: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = 28.dp,
                bottom = 110.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Header occupies both columns
            item(span = { GridItemSpan(2) }) { Header(state.totalCount) }
            item(span = { GridItemSpan(2) }) { CategoryChips(state, onSelectCategory) }

            items(state.itemsInCategory, key = { it.id }) { item ->
                ItemCard(item = item, onClick = { onOpenItem(item.id) })
            }

            if (state.itemsInCategory.isEmpty()) {
                item(span = { GridItemSpan(2) }) { EmptyHint(state.currentCategory) }
            }
        }
    }
}

@Composable
private fun Header(total: Int) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Treasure",
            color = colors.ink,
            style = MaterialTheme.typography.titleLarge,
        )
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
    onSelectCategory: (Category?) -> Unit,
) {
    // 头号 chip = "全部"（null 过滤），后面跟 6 个品类。LazyRow 让我们能 animate
    // 滚到选中那个，从门厅过来时不必再手动拖。
    //
    // Cycle 0024：之前 LaunchedEffect 每次 selectedIndex 变都 animateScrollToItem，
    // 用户点一下 chip 就把这个 chip 拽到行首，破坏了从门厅过来时建立的"位置感"。
    // 改成只在目标 chip "其实看不到"（不在 visibleItemsInfo 里）时才滚 — 用户
    // 自己点的 chip 一定可见，所以不会动；门厅跳过来时如果目标在屏幕外才滚。
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val selectedIndex: Int = if (state.currentCategory == null) 0
        else 1 + Category.entries.indexOf(state.currentCategory).coerceAtLeast(0)
    androidx.compose.runtime.LaunchedEffect(selectedIndex) {
        val visible = listState.layoutInfo.visibleItemsInfo
        val isVisible = visible.any { it.index == selectedIndex }
        if (!isVisible) listState.animateScrollToItem(selectedIndex)
    }
    androidx.compose.foundation.lazy.LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            AllChip(
                count = state.totalCount,
                selected = state.currentCategory == null,
                onClick = { onSelectCategory(null) },
            )
        }
        items(Category.entries.size) { idx ->
            val c = Category.entries[idx]
            CategoryChip(
                category = c,
                count = state.countByCategory[c] ?: 0,
                selected = c == state.currentCategory,
                onClick = { onSelectCategory(c) },
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
    category: Category,
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
            text = category.nameZh,
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

@Composable
private fun ItemCard(item: Item, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .clickable(onClick = onClick),
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
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.oneLiner,
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyHint(category: Category?) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (category == null) "图鉴还空着 — 去录入页加点东西"
                else "${category.nameZh} 还没有收藏",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

