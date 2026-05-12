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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.illust.HeroIllustration
import com.treasure.ui.components.HeroAvatar
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.components.BackArrow

@Composable
fun DetailRoute(
    itemId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    vm: DetailViewModel = viewModel(factory = DetailViewModel.factory(itemId)),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    DetailScreen(
        state = state,
        onBack = onBack,
        onEdit = { state.item?.let { onEdit(it.id) } },
        onSetCallouts = vm::setCallouts,
        onAddPhotos = vm::addPhotos,
        onRemovePhoto = vm::removePhoto,
    )
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSetCallouts: (String, List<com.treasure.core.domain.PhotoCallout>) -> Unit = { _, _ -> },
    onAddPhotos: (List<android.net.Uri>) -> Unit = {},
    onRemovePhoto: (String) -> Unit = {},
) {
    val colors = LocalTreasureColors.current
    val item = state.item
    var fullscreenIndex by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<Int?>(null)
    }

    if (!state.loaded) {
        Box(modifier = Modifier.fillMaxSize().background(colors.paper))
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
    val expanded = sheetState.currentValue == SheetValue.Expanded

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 52.dp,
        sheetContainerColor = colors.paper,
        sheetContentColor = colors.ink,
        // Cycle 0031：替换默认 36dp 灰线把手 — 一行小字"↑ 上拖看详情"。
        // 整块仍是 sheet 内置拖动响应区，竖向手势照旧工作。
        sheetDragHandle = { DetailSheetHandle(isExpanded = expanded) },
        sheetShadowElevation = 8.dp,
        sheetTonalElevation = 0.dp,
        containerColor = colors.paper,
        sheetContent = {
            DrawerContent(
                item = item,
                onOpenPhoto = { idx -> fullscreenIndex = idx },
                onAddPhotos = onAddPhotos,
                onRemovePhoto = onRemovePhoto,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
        ) {
            DetailFront(item = item, onBack = onBack, onEdit = onEdit)
        }
    }

    fullscreenIndex?.let { idx ->
        // Cycle 0031：全屏 viewer 打开时拦住 back — 先关 viewer，不要 pop 到 Main。
        androidx.activity.compose.BackHandler { fullscreenIndex = null }
        com.treasure.ui.photo.FullscreenPhotoViewer(
            photos = item.photos,
            initialIndex = idx,
            callouts = item.callouts,
            onSetCallouts = onSetCallouts,
            onClose = { fullscreenIndex = null },
        )
    }
}

/**
 * Cycle 0031：抽屉顶部把手区 — 一行小字提示"↑ 上拖看详情"（展开时变
 * "↓ 下拖收起"）。高度跟之前一样 52dp，整块仍是 sheet 内置拖动响应区。
 */
@Composable
private fun DetailSheetHandle(isExpanded: Boolean) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isExpanded) "↓ 下拖收起" else "↑ 上拖看详情",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
        )
    }
}

@Composable
private fun DetailFront(item: Item, onBack: () -> Unit, onEdit: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { TopBar(onBack = onBack, onEdit = onEdit) }
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
private fun TopBar(onBack: () -> Unit, onEdit: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackArrow(color = colors.ink, onClick = onBack)
        Spacer(Modifier.weight(1f))
        // Cycle 0031：和图鉴页右上 Edit + 小红点同款 — terra Edit 字 + 12dp
        // 0xFFC5392E 红圆点 + 整行 clickable 进编辑。
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onEdit)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = "Edit",
                color = colors.terra,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color(0xFFC5392E)),
            )
        }
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
        if (rotation <= 90f) HeroFront(item)
        else Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = 180f },
        ) { HeroBack(item) }
    }
}

@Composable
private fun HeroFront(item: Item) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        HeroAvatar(item = item, modifier = Modifier.fillMaxSize())
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(end = 14.dp, bottom = 12.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Text(
            text = "${item.photos.size} PHOTOS · TAP TO FLIP",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun HeroBack(item: Item) {
    val colors = LocalTreasureColors.current
    if (item.photos.isEmpty()) { EmptyHeroBack(item); return }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.photos.take(3).forEach { path ->
                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(2.dp)),
                ) {
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (item.photos.size <= 3) "${item.photos.size} 张实拍"
                   else "+${item.photos.size - 3} 张 · 共 ${item.photos.size} 张",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun EmptyHeroBack(item: Item) {
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
    }
}

@Composable
private fun EmptyPhotoPlate(modifier: Modifier = Modifier) {
    val colors = LocalTreasureColors.current
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val frameW = w * 0.62f; val frameH = h * 0.72f
        val cx = w / 2f; val cy = h / 2f
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
                drawRect(colors.card, Offset(tlx, tly), Size(frameW, frameH))
                drawRect(colors.ink.copy(alpha = f.alpha), Offset(tlx, tly), Size(frameW, frameH), style = Stroke(0.8f.dp.toPx()))
                if (f.alpha >= 0.8f) {
                    val pad = 8.dp.toPx()
                    drawLine(colors.sub, Offset(tlx + pad, tly + pad), Offset(tlx + frameW - pad, tly + frameH - pad), strokeWidth = 0.6f.dp.toPx())
                    drawLine(colors.sub, Offset(tlx + frameW - pad, tly + pad), Offset(tlx + pad, tly + frameH - pad), strokeWidth = 0.6f.dp.toPx())
                }
            }
        }
    }
}

@Composable
private fun Title(item: Item) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
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
                    text = spec.value.ifBlank { "—" },
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (idx != item.heroSpecs.lastIndex) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.line))
            }
        }
        if (item.heroSpecs.isEmpty()) {
            Text(
                text = "（暂无关键参数 · 点右上 · 编辑添加）",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

// ─── Drawer (read-only) ─────────────────────────────────────────────────────

private enum class DrawerTab(val label: String) {
    History("历史"), Specs("参数"), Album("影集")
}

@Composable
private fun DrawerContent(
    item: Item,
    onOpenPhoto: (Int) -> Unit,
    onAddPhotos: (List<android.net.Uri>) -> Unit,
    onRemovePhoto: (String) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val drawerHeight = (configuration.screenHeightDp * 0.78f).dp
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = 0,
        pageCount = { 3 },
    )
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(drawerHeight),
    ) {
        TabRowBar(
            selected = DrawerTab.entries[pagerState.currentPage],
            onSelect = { tab ->
                scope.launch {
                    pagerState.animateScrollToPage(tab.ordinal)
                }
            },
            historyCount = item.history.size,
            specsCount = item.specs.count { it.label.isNotBlank() },
            albumCount = item.photos.size,
        )
        Spacer(Modifier.height(8.dp))
        // Cycle 0031：3 个 tab 改成 HorizontalPager — 左右滑切换 history /
        // specs / album，跟主页 tab 同款触感。
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            when (DrawerTab.entries[page]) {
                DrawerTab.History -> HistoryList(item.history)
                DrawerTab.Specs   -> SpecsList(item)
                DrawerTab.Album   -> AlbumList(
                    photos = item.photos,
                    onOpenPhoto = onOpenPhoto,
                    onAddPhotos = onAddPhotos,
                    onRemovePhoto = onRemovePhoto,
                )
            }
        }
    }
}

@Composable
private fun TabRowBar(
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
        Tab(DrawerTab.History, historyCount, selected == DrawerTab.History) { onSelect(DrawerTab.History) }
        Tab(DrawerTab.Specs,   specsCount,   selected == DrawerTab.Specs)   { onSelect(DrawerTab.Specs)   }
        Tab(DrawerTab.Album,   albumCount,   selected == DrawerTab.Album)   { onSelect(DrawerTab.Album)   }
    }
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 22.dp)
        .height(0.5.dp)
        .background(colors.line))
}

@Composable
private fun Tab(tab: DrawerTab, count: Int, selected: Boolean, onClick: () -> Unit) {
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
            Spacer(Modifier.width(4.dp))
            Text(
                text = count.toString().padStart(2, '0'),
                color = if (selected) colors.terra else colors.sub.copy(alpha = 0.5f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun HistoryList(events: List<HistoryEvent>) {
    val colors = LocalTreasureColors.current
    if (events.isEmpty()) { Empty("还没有时间轴 · 点右上 · 编辑添加"); return }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 8.dp),
    ) {
        events.forEachIndexed { idx, e ->
            // Cycle 0031：时间轴左轨改成 — 月日两行（"5/12" 大字 + "2026" 小字
            // 在上方）+ 40dp 圆形 kind icon。右侧标题 titleMedium、备注下方独
            // 立小字。比之前 "2026.05.12" 一行字更易读。
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(72.dp)
                        .padding(end = 14.dp),
                ) {
                    val parsed = runCatching { java.time.LocalDate.parse(e.date) }.getOrNull()
                    if (parsed != null) {
                        Text(
                            text = parsed.year.toString(),
                            color = colors.sub.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = "${parsed.monthValue} / ${parsed.dayOfMonth}",
                            color = colors.ink,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    } else {
                        Text(
                            text = e.date,
                            color = colors.sub,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    // Cycle 0031：emoji 自带色，背景用 kindColor 淡填充 + 边框。
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(kindColor(e.kind, colors).copy(alpha = 0.14f))
                            .border(0.5.dp, kindColor(e.kind, colors).copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = kindGlyph(e.kind),
                            style = MaterialTheme.typography.titleMedium,
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
                Column(modifier = Modifier.padding(top = 22.dp, bottom = 18.dp).weight(1f)) {
                    Text(
                        text = e.title.ifBlank { kindLabelZh(e.kind) },
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
        Spacer(Modifier.height(40.dp))
    }
}

// Cycle 0031：跟 EditScreen 同一套 emoji glyph + 中文 label，Detail / Edit
// 两边历史 row 视觉一致。
private fun kindGlyph(kind: HistoryKind): String = when (kind) {
    HistoryKind.ACQUIRED  -> "🛒"
    HistoryKind.MILESTONE -> "🏆"
    HistoryKind.MAINTAIN  -> "🔧"
    HistoryKind.MOD       -> "⚙️"
    HistoryKind.PARTED    -> "👋"
}

private fun kindLabelZh(kind: HistoryKind): String = when (kind) {
    HistoryKind.ACQUIRED  -> "购入"
    HistoryKind.MILESTONE -> "里程碑"
    HistoryKind.MAINTAIN  -> "保养"
    HistoryKind.MOD       -> "改装"
    HistoryKind.PARTED    -> "出手"
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
private fun SpecsList(item: Item) {
    val colors = LocalTreasureColors.current
    val visible = item.specs.filter { it.label.isNotBlank() }
    if (visible.isEmpty()) {
        Empty("还没有参数 · 点右上 · 编辑添加")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        visible.forEachIndexed { idx, spec ->
            SpecRow(spec.label, spec.value)
            if (idx != visible.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
            }
            // Subtle hairline after the hero set so users know what's "promoted"
            if (idx == Item.HERO_SPEC_COUNT - 1 && idx != visible.lastIndex) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.terra.copy(alpha = 0.4f)),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value.ifBlank { "—" },
            color = colors.ink,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1.6f),
        )
    }
}

@Composable
private fun AlbumList(
    photos: List<String>,
    onOpenPhoto: (Int) -> Unit,
    onAddPhotos: (List<android.net.Uri>) -> Unit,
    onRemovePhoto: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var editMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }
    var confirmingDelete by remember { mutableStateOf(false) }

    // Cycle 0031：影集进入编辑态后，关掉就清空 selection。
    androidx.compose.runtime.LaunchedEffect(editMode) {
        if (!editMode) selected.clear()
    }

    val pickPhotos = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts
            .PickMultipleVisualMedia(maxItems = 9),
    ) { uris ->
        if (uris.isNotEmpty()) onAddPhotos(uris)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                // + 加号块（永远在第一格，非编辑态可点）
                item {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(colors.card.copy(alpha = 0.55f))
                            .border(0.5.dp, colors.line)
                            .then(
                                if (!editMode) Modifier.clickable {
                                    pickPhotos.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract
                                                .ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
                                } else Modifier,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+",
                            color = if (editMode) colors.sub.copy(alpha = 0.4f) else colors.terra,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                itemsIndexed(items = photos, key = { _, p -> p }) { idx, path ->
                    val isSelected = path in selected
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(colors.card)
                            .border(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) colors.terra else colors.line,
                            )
                            .pointerInput(path, editMode) {
                                detectTapGestures(
                                    onTap = {
                                        if (editMode) {
                                            if (isSelected) selected.remove(path)
                                            else selected.add(path)
                                        } else onOpenPhoto(idx)
                                    },
                                    onLongPress = {
                                        if (!editMode) {
                                            editMode = true
                                            selected.add(path)
                                        }
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
                        if (editMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSelected) colors.terra
                                        else colors.paper.copy(alpha = 0.7f),
                                    )
                                    .border(
                                        0.5.dp,
                                        if (isSelected) colors.terra else colors.line,
                                        RoundedCornerShape(50),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = colors.paper,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (editMode) "${selected.size} 张已选 · 点完成退出编辑"
                else "${photos.size} 张实拍 · 长按缩略图进入编辑态批量删除",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
            // 给底部删除条留位置
            if (editMode) Spacer(Modifier.height(64.dp))
        }
        // 底部删除长条 — 编辑态出现
        if (editMode) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .clip(RoundedCornerShape(6.dp))
                        .border(0.5.dp, colors.line, RoundedCornerShape(6.dp))
                        .clickable { editMode = false }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("完成", color = colors.sub, style = MaterialTheme.typography.bodyMedium)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (selected.isEmpty()) colors.terra.copy(alpha = 0.25f)
                            else colors.terra,
                        )
                        .clickable(enabled = selected.isNotEmpty()) {
                            confirmingDelete = true
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "删除${if (selected.isNotEmpty()) " ${selected.size}" else ""}",
                        color = colors.paper,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    if (confirmingDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("删除 ${selected.size} 张照片？") },
            text = {
                Text(
                    text = "选中的实拍照片会从图鉴里清掉，无法恢复。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    selected.toList().forEach { onRemovePhoto(it) }
                    selected.clear()
                    editMode = false
                    confirmingDelete = false
                }) { Text("删除", color = colors.terra) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmingDelete = false }) {
                    Text("取消")
                }
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
