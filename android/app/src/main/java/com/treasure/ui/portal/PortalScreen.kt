package com.treasure.ui.portal

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.Category
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.Item
import com.treasure.theme.LocalTreasureColors
import com.treasure.illust.HeroIllustration
import com.treasure.ui.components.HeroAvatar
import com.treasure.ui.components.Ornament

@Composable
fun PortalRoute(
    onEnterCategory: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    vm: PortalViewModel = viewModel(factory = PortalViewModel.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    PortalScreen(state = state, onEnterCategory = onEnterCategory, onOpenItem = onOpenItem)
}

@Composable
fun PortalScreen(
    state: PortalUiState,
    onEnterCategory: (String) -> Unit,
    onOpenItem: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = 36.dp, bottom = 110.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Ornament(modifier = Modifier.padding(horizontal = 24.dp)) }
            item { Spacer(Modifier.height(20.dp)) }
            item { GrandTitle() }
            item { Spacer(Modifier.height(26.dp)) }
            item { Tally(state) }
            item { Spacer(Modifier.height(26.dp)) }
            item {
                SectionLabel(
                    text = "✦ The Rooms ✦",
                    modifier = Modifier.padding(horizontal = 22.dp),
                    centered = true,
                )
            }
            item { Spacer(Modifier.height(14.dp)) }
            item { DoorwaysGrid(state, onEnterCategory) }
            item { Spacer(Modifier.height(28.dp)) }
            item {
                SectionLabel(
                    text = "✦ Latest entry",
                    modifier = Modifier.padding(horizontal = 22.dp),
                    centered = false,
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
            item { state.latestOverall?.let { LatestEntryCard(it, onOpenItem) } }
        }
    }
}

@Composable
private fun GrandTitle() {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Treasure",
            color = colors.ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "私人博物馆 · 图鉴",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Tally(state: PortalUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TallyItem(state.totalItems, "items")
        TallyItem(state.visibleCategories.size, "rooms")
    }
}

@Composable
private fun TallyItem(n: Int, label: String) {
    val colors = LocalTreasureColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = n.toString().padStart(2, '0'),
            color = colors.ink,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier, centered: Boolean) {
    val colors = LocalTreasureColors.current
    Text(
        text = text.uppercase(),
        color = colors.sub,
        style = MaterialTheme.typography.labelSmall,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        modifier = if (centered) modifier.fillMaxWidth() else modifier,
    )
}

@Composable
private fun DoorwaysGrid(
    state: PortalUiState,
    onEnterCategory: (String) -> Unit,
) {
    val cats = state.visibleCategories
    val romans = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cats.chunked(2).forEachIndexed { rowIdx, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEachIndexed { colIdx, c ->
                    val romanIdx = rowIdx * 2 + colIdx
                    DoorwayCard(
                        info = c,
                        roman = romans.getOrNull(romanIdx) ?: "",
                        count = state.countByCategoryId[c.id] ?: 0,
                        latest = state.latestByCategoryId[c.id],
                        modifier = Modifier.weight(1f),
                        onClick = { onEnterCategory(c.id) },
                    )
                }
                if (pair.size == 1) {
                    Box(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

/**
 * 当某品类还没有任何物品时，给 doorway 用 CategoryInfo 自身的 hero vector
 * 合成一个 stub Item，让 [HeroIllustration] 能画出该品类的代表插画。
 *
 * Item.category 字段必须是 [Category] enum，但对于用户自定义分类我们没有
 * 对应 enum。这里塞 Category.TECH 仅作占位 — HeroAvatar / HeroIllustration
 * 只读 item.heroVector + item.palette，所以 category 字段不影响渲染。
 */
private fun stubItemFor(info: CategoryInfo): Item {
    val builtIn = Category.entries.firstOrNull { it.id == info.id }
    val tpl = builtIn?.let { com.treasure.ui.add.CategoryTemplates.forCategory(it) }
    return Item(
        id = "stub-${info.id}",
        category = info.id,
        brand = "", model = "", nickname = "", acquired = "", parted = null,
        status = com.treasure.core.domain.ItemStatus.OWNED,
        palette = tpl?.palette ?: listOf("#0e0e0e", "#a47836", "#e8e2d4", "#5a5a5a"),
        oneLiner = "",
        heroVector = info.heroVector,
        specs = emptyList(),
        history = emptyList(),
        photos = emptyList(),
        createdAt = 0L, updatedAt = 0L,
    )
}

@Composable
private fun DoorwayCard(
    info: CategoryInfo,
    roman: String,
    count: Int,
    latest: Item?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = modifier
            .background(colors.card)
            .border(0.5.dp, colors.line)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = roman,
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
            contentAlignment = Alignment.Center,
        ) {
            val displayItem = latest ?: remember(info.id, info.heroVector) { stubItemFor(info) }
            HeroAvatar(item = displayItem, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.line),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = info.nameZh,
            color = colors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "$count pcs · ${info.nameEn.ifBlank { "—" }}",
            color = colors.sub,
            fontSize = 10.sp,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LatestEntryCard(item: Item, onOpenItem: (String) -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .border(0.5.dp, colors.line)
            .clickable { onOpenItem(item.id) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            modifier = Modifier
                .height(54.dp)
                .aspectRatio(1.4f),
        ) {
            HeroAvatar(item = item, modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier.padding(start = 14.dp).weight(1f),
        ) {
            Text(
                text = "${item.brand} ${item.model}",
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.oneLiner,
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = item.acquired.replace("-", "."),
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
