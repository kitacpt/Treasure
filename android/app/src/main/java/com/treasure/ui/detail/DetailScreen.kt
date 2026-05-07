@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.detail

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.components.BackArrow

@Composable
fun DetailRoute(
    itemId: String,
    onBack: () -> Unit,
    vm: DetailViewModel = viewModel(factory = DetailViewModel.factory(itemId)),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    DetailScreen(
        state = state,
        onBack = onBack,
        onDelete = { vm.delete(onBack) },
    )
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val item = state.item

    if (!state.loaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper),
        )
        return
    }
    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "找不到这件物品 · 点击返回",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )
        }
        return
    }

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        // Just enough for the drag handle — tabs and content are hidden
        // until the user pulls the sheet up. Keeps the Detail screen quiet.
        sheetPeekHeight = 40.dp,
        sheetContainerColor = colors.paper,
        sheetContentColor = colors.ink,
        sheetDragHandle = { BottomSheetDefaults.DragHandle(color = colors.sub) },
        sheetShadowElevation = 8.dp,
        sheetTonalElevation = 0.dp,
        containerColor = colors.paper,
        sheetContent = {
            DrawerContent(item = item, onDelete = onDelete)
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            DetailFront(item = item, onBack = onBack)
        }
    }
}

@Composable
private fun DetailFront(item: Item, onBack: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { TopBar(onBack = onBack) }
        item { Spacer(Modifier.height(20.dp)) }
        item { FlippableHero(item) }
        item { Spacer(Modifier.height(24.dp)) }
        item { Title(item) }
        item { Spacer(Modifier.height(20.dp)) }
        item { HeroSpecsTable(item) }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackArrow(color = colors.ink, onClick = onBack)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FlippableHero(item: Item) {
    val colors = LocalTreasureColors.current
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "heroFlip",
    )
    val density = LocalDensity.current.density

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .background(colors.card)
            .border(0.5.dp, colors.line)
            .aspectRatio(1.0f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { flipped = !flipped },
    ) {
        if (rotation <= 90f) {
            HeroFront(item)
        } else {
            // Counter-rotate so the back face reads upright.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
            ) {
                HeroBack(item)
            }
        }
    }
}

@Composable
private fun HeroFront(item: Item) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        HeroIllustration(item = item, modifier = Modifier.fillMaxSize())
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 14.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Text(
            text = "0 PHOTOS · TAP TO FLIP",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun HeroBack(item: Item) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyPhotoPlate(modifier = Modifier
            .fillMaxWidth(0.66f)
            .aspectRatio(1.4f))
        Spacer(Modifier.height(20.dp))
        Text(
            text = "尚未收录实拍",
            color = colors.ink,
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${item.brand} ${item.model}",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(0.5.dp, colors.terra.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(
                text = "+ 添加照片",
                color = colors.terra.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Three slightly-rotated empty photo frames stacked. Reads as
 * "this exhibit has no plates yet" without resorting to a sad-face icon.
 */
@Composable
private fun EmptyPhotoPlate(modifier: Modifier = Modifier) {
    val colors = LocalTreasureColors.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val frameW = w * 0.62f
        val frameH = h * 0.72f
        val cx = w / 2f
        val cy = h / 2f

        data class Frame(val dx: Float, val dy: Float, val deg: Float, val alpha: Float)
        val frames = listOf(
            Frame(-w * 0.10f,  h * 0.04f, -6f, 0.35f),
            Frame( w * 0.08f, -h * 0.02f,  4f, 0.55f),
            Frame( 0f,            0f,        -1f, 0.85f),
        )
        frames.forEach { f ->
            val tlx = cx - frameW / 2f + f.dx
            val tly = cy - frameH / 2f + f.dy
            rotate(f.deg, pivot = Offset(tlx + frameW / 2f, tly + frameH / 2f)) {
                drawRect(
                    color = colors.card,
                    topLeft = Offset(tlx, tly),
                    size = Size(frameW, frameH),
                )
                drawRect(
                    color = colors.ink.copy(alpha = f.alpha),
                    topLeft = Offset(tlx, tly),
                    size = Size(frameW, frameH),
                    style = Stroke(width = 0.8f.dp.toPx()),
                )
                // central X mark on the topmost frame
                if (f.alpha >= 0.8f) {
                    val pad = 8.dp.toPx()
                    drawLine(
                        color = colors.sub,
                        start = Offset(tlx + pad, tly + pad),
                        end = Offset(tlx + frameW - pad, tly + frameH - pad),
                        strokeWidth = 0.6f.dp.toPx(),
                    )
                    drawLine(
                        color = colors.sub,
                        start = Offset(tlx + frameW - pad, tly + pad),
                        end = Offset(tlx + pad, tly + frameH - pad),
                        strokeWidth = 0.6f.dp.toPx(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Title(item: Item) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp),
    ) {
        Text(
            text = "${item.brand} ${item.model}",
            color = colors.ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (item.nickname.isNotBlank()) {
                Text(
                    text = "「${item.nickname}」",
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
                Spacer(Modifier.padding(start = 8.dp))
            }
            StatusBadge(item.status)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.oneLiner,
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StatusBadge(status: ItemStatus) {
    val colors = LocalTreasureColors.current
    val label = when (status) {
        ItemStatus.OWNED -> "OWNED"
        ItemStatus.PARTED -> "PARTED"
        ItemStatus.RENTED -> "RENTED"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, colors.sub, RoundedCornerShape(2.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, color = colors.sub, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HeroSpecsTable(item: Item) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .border(0.5.dp, colors.line),
    ) {
        item.heroSpecs.forEachIndexed { idx, spec ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = spec.label,
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = spec.value,
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (idx != item.heroSpecs.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.line),
                )
            }
        }
        if (item.heroSpecs.isEmpty()) {
            Text(
                text = "（暂无关键参数）",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

// ─── Drawer ─────────────────────────────────────────────────────────────────

private enum class DrawerTab(val label: String) {
    History("历史"), Specs("参数"), Album("影集"), Settings("设置")
}

@Composable
private fun DrawerContent(item: Item, onDelete: () -> Unit) {
    var selected by remember { mutableStateOf(DrawerTab.History) }
    // Lock drawer to a constant height across tabs — short tabs (Settings)
    // and long tabs (History) used to make the sheet pop/shrink visually.
    val configuration = LocalConfiguration.current
    val drawerHeight = (configuration.screenHeightDp * 0.78f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(drawerHeight),
    ) {
        TabRow(
            selected = selected,
            onSelect = { selected = it },
            historyCount = item.history.size,
            specsCount = item.specs.size,
            albumCount = 0,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (selected) {
                DrawerTab.History  -> HistoryList(item.history)
                DrawerTab.Specs    -> SpecsList(item.specs)
                DrawerTab.Album    -> AlbumStub()
                DrawerTab.Settings -> SettingsTab(onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun TabRow(
    selected: DrawerTab,
    onSelect: (DrawerTab) -> Unit,
    historyCount: Int,
    specsCount: Int,
    albumCount: Int,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Tab(DrawerTab.History,  historyCount, selected == DrawerTab.History)  { onSelect(DrawerTab.History)  }
        Tab(DrawerTab.Specs,    specsCount,   selected == DrawerTab.Specs)    { onSelect(DrawerTab.Specs)    }
        Tab(DrawerTab.Album,    albumCount,   selected == DrawerTab.Album)    { onSelect(DrawerTab.Album)    }
        Tab(DrawerTab.Settings, null,         selected == DrawerTab.Settings) { onSelect(DrawerTab.Settings) }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .height(0.5.dp)
            .background(colors.line),
    )
}

@Composable
private fun Tab(tab: DrawerTab, count: Int?, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = tab.label,
                color = if (selected) colors.ink else colors.sub,
                style = MaterialTheme.typography.titleMedium,
            )
            if (count != null) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = count.toString().padStart(2, '0'),
                    color = if (selected) colors.terra else colors.sub.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun HistoryList(events: List<HistoryEvent>) {
    val colors = LocalTreasureColors.current
    if (events.isEmpty()) {
        Empty("还没有时间轴 · 添加历史 — coming")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp),
    ) {
        events.forEachIndexed { idx, e ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(end = 14.dp),
                ) {
                    Text(
                        text = e.date.replace("-", "."),
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(kindColor(e.kind, colors))
                            .padding(2.dp),
                    ) {
                        Text(
                            text = kindGlyph(e.kind),
                            color = colors.paper,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    if (idx != events.lastIndex) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .height(36.dp)
                                .width(0.5.dp)
                                .background(colors.line),
                        )
                    }
                }
                Column(modifier = Modifier.padding(bottom = 18.dp).weight(1f)) {
                    Text(
                        text = e.title,
                        color = colors.ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = e.note,
                        color = colors.sub,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun kindGlyph(kind: HistoryKind): String = when (kind) {
    HistoryKind.ACQUIRED  -> "+"
    HistoryKind.MILESTONE -> "★"
    HistoryKind.MAINTAIN  -> "↻"
    HistoryKind.MOD       -> "Δ"
    HistoryKind.PARTED    -> "−"
}

private fun kindColor(
    kind: HistoryKind,
    colors: com.treasure.theme.TreasureColors,
): androidx.compose.ui.graphics.Color = when (kind) {
    HistoryKind.ACQUIRED  -> colors.terra
    HistoryKind.MILESTONE -> colors.ink
    HistoryKind.MAINTAIN  -> colors.sub
    HistoryKind.MOD       -> colors.sub
    HistoryKind.PARTED    -> colors.sub
}

@Composable
private fun SpecsList(specs: Map<String, String>) {
    val colors = LocalTreasureColors.current
    if (specs.isEmpty()) {
        Empty("（暂无完整参数）")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        specs.entries.forEachIndexed { idx, (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = key,
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1.6f),
                )
            }
            if (idx != specs.size - 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.line),
                )
            }
        }
    }
}

@Composable
private fun AlbumStub() {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(items = (0 until 9).toList()) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(colors.card)
                        .border(0.5.dp, colors.line),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "添加照片 — coming",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun SettingsTab(onDelete: () -> Unit) {
    val colors = LocalTreasureColors.current
    var confirming by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Text(
            text = "MANAGE",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(12.dp))

        // Delete row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .border(0.5.dp, colors.line)
                .clickable { confirming = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "删除这件物品",
                color = colors.terra,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "→",
                color = colors.terra,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "记录会从图鉴里移除，不可恢复",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )

        Spacer(Modifier.height(28.dp))
        Text(
            text = "更多操作 — coming",
            color = colors.sub.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
        )
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("确认删除？") },
            text = { Text("这件物品的所有记录会被清除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onDelete()
                }) { Text("删除", color = colors.terra) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("取消") }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

@Composable
private fun Empty(text: String) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
        )
    }
}
