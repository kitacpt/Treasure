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

@Composable
fun GridRoute(
    initialCategoryId: String,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    vm: GridViewModel = viewModel(factory = GridViewModel.factory(initialCategoryId)),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    GridScreen(
        state = state,
        onSelectCategory = vm::selectCategory,
        onOpenItem = onOpenItem,
        onBack = onBack,
    )
}

@Composable
fun GridScreen(
    state: GridUiState,
    onSelectCategory: (Category) -> Unit,
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
    onSelectCategory: (Category) -> Unit,
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Category.entries.forEach { c ->
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
                HeroIllustration(item = item, modifier = Modifier.fillMaxSize())
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
private fun EmptyHint(category: Category) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${category.nameZh} 还没有收藏",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

