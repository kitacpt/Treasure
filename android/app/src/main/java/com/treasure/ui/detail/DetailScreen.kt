@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HeroVector
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
        onUpdate = vm::update,
        onAddPhoto = vm::addPhoto,
        onRemovePhoto = vm::removePhoto,
    )
}

@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (Item) -> Unit,
    onAddPhoto: (android.net.Uri) -> Unit,
    onRemovePhoto: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val item = state.item

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

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 40.dp,
        sheetContainerColor = colors.paper,
        sheetContentColor = colors.ink,
        sheetDragHandle = { BottomSheetDefaults.DragHandle(color = colors.sub) },
        sheetShadowElevation = 8.dp,
        sheetTonalElevation = 0.dp,
        containerColor = colors.paper,
        sheetContent = {
            DrawerContent(
                item = item,
                onUpdate = onUpdate,
                onDelete = onDelete,
                onAddPhoto = onAddPhoto,
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
        HeroIllustration(item = item, modifier = Modifier.fillMaxSize())
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
        Spacer(Modifier.height(6.dp))
        Text(
            text = "上滑抽屉看影集",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = FontStyle.Italic,
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
        Spacer(Modifier.height(20.dp))
        Text(
            text = "上滑抽屉 · 影集 tab 添加",
            color = colors.terra.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
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
                text = "（暂无关键参数 · 上滑抽屉 · 参数 tab 添加）",
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
    Basics("基础"), Specs("参数"), History("历史"), Album("影集")
}

@Composable
private fun DrawerContent(
    item: Item,
    onUpdate: (Item) -> Unit,
    onDelete: () -> Unit,
    onAddPhoto: (android.net.Uri) -> Unit,
    onRemovePhoto: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(DrawerTab.Basics) }
    val configuration = LocalConfiguration.current
    val drawerHeight = (configuration.screenHeightDp * 0.78f).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(drawerHeight),
    ) {
        TabRowBar(
            selected = selected,
            onSelect = { selected = it },
            historyCount = item.history.size,
            specsCount = item.heroSpecs.size + item.specs.size,
            albumCount = item.photos.size,
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (selected) {
                DrawerTab.Basics  -> BasicsTab(item = item, onUpdate = onUpdate, onDelete = onDelete)
                DrawerTab.Specs   -> SpecsTab(item = item, onUpdate = onUpdate)
                DrawerTab.History -> HistoryTab(item = item, onUpdate = onUpdate)
                DrawerTab.Album   -> AlbumTab(
                    photos = item.photos,
                    onAddPhoto = onAddPhoto,
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
        Tab(DrawerTab.Basics,  null,         selected == DrawerTab.Basics)  { onSelect(DrawerTab.Basics)  }
        Tab(DrawerTab.Specs,   specsCount,   selected == DrawerTab.Specs)   { onSelect(DrawerTab.Specs)   }
        Tab(DrawerTab.History, historyCount, selected == DrawerTab.History) { onSelect(DrawerTab.History) }
        Tab(DrawerTab.Album,   albumCount,   selected == DrawerTab.Album)   { onSelect(DrawerTab.Album)   }
    }
    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 22.dp)
        .height(0.5.dp)
        .background(colors.line))
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

// ─── Tab: 基础 ─────────────────────────────────────────────────────────────

@Composable
private fun BasicsTab(
    item: Item,
    onUpdate: (Item) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var brand by remember(item.id) { mutableStateOf(item.brand) }
    var model by remember(item.id) { mutableStateOf(item.model) }
    var nickname by remember(item.id) { mutableStateOf(item.nickname) }
    var oneLiner by remember(item.id) { mutableStateOf(item.oneLiner) }
    var acquired by remember(item.id) { mutableStateOf(item.acquired) }
    var parted by remember(item.id) { mutableStateOf(item.parted ?: "") }
    var status by remember(item.id) { mutableStateOf(item.status) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var heroVector by remember(item.id) { mutableStateOf(item.heroVector) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val dirty = brand != item.brand ||
        model != item.model ||
        nickname != item.nickname ||
        oneLiner != item.oneLiner ||
        acquired != item.acquired ||
        parted != (item.parted ?: "") ||
        status != item.status ||
        category != item.category ||
        heroVector != item.heroVector

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        SectionLabel("BASICS")
        Spacer(Modifier.height(8.dp))
        FormField("品牌 brand",  brand, { brand = it })
        Spacer(Modifier.height(10.dp))
        FormField("型号 model",  model, { model = it })
        Spacer(Modifier.height(10.dp))
        FormField("昵称",        nickname, { nickname = it })
        Spacer(Modifier.height(10.dp))
        FormField("一句话简介",  oneLiner, { oneLiner = it })
        Spacer(Modifier.height(10.dp))
        FormField("购入日期 (YYYY-MM-DD)", acquired, { acquired = it })
        Spacer(Modifier.height(10.dp))
        FormField("出手日期 (YYYY-MM-DD · 没出手就空着)", parted, { parted = it })

        Spacer(Modifier.height(20.dp))
        SectionLabel("STATUS")
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FormChip("Owned",  status == ItemStatus.OWNED)  { status = ItemStatus.OWNED }
            FormChip("Parted", status == ItemStatus.PARTED) { status = ItemStatus.PARTED }
            FormChip("Rented", status == ItemStatus.RENTED) { status = ItemStatus.RENTED }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("CATEGORY")
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Category.entries.forEach { c ->
                FormChip(c.nameZh, category == c) { category = c }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("HERO ILLUSTRATION")
        // Compact picker: a horizontal scroll of small previews would be nicer,
        // but for a "concise" edit a chip per option keeps the surface small.
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val rows = HeroVector.entries.chunked(4)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { v ->
                        FormChip(
                            label = v.name.lowercase().replace('_', ' '),
                            selected = heroVector == v,
                        ) { heroVector = v }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SaveButton(dirty = dirty) {
            onUpdate(item.copy(
                brand = brand.trim(),
                model = model.trim(),
                nickname = nickname.trim(),
                oneLiner = oneLiner.trim(),
                acquired = acquired.trim(),
                parted = parted.trim().ifBlank { null },
                status = status,
                category = category,
                heroVector = heroVector,
            ))
        }

        Spacer(Modifier.height(36.dp))
        SectionLabel("DANGER ZONE")
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .border(0.5.dp, colors.line)
                .clickable { confirmingDelete = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "删除这件物品",
                color = colors.terra,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(text = "→", color = colors.terra, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "记录会从图鉴里移除，不可恢复",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(40.dp))
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("确认删除？") },
            text = { Text("这件物品的所有记录会被清除，无法恢复。") },
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

// ─── Tab: 参数 ─────────────────────────────────────────────────────────────

@Composable
private fun SpecsTab(item: Item, onUpdate: (Item) -> Unit) {
    val colors = LocalTreasureColors.current
    val heroSpecs = remember(item.id) {
        mutableStateListOf<HeroSpec>().apply {
            addAll(item.heroSpecs.ifEmpty { List(4) { HeroSpec("", "") } })
        }
    }
    val specRows = remember(item.id) {
        mutableStateListOf<Pair<String, String>>().apply {
            addAll(item.specs.toList())
        }
    }

    val dirty = heroSpecs.toList() != normalizedHeroSpecs(item.heroSpecs) ||
        specRows.toList() != item.specs.toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        SectionLabel("HERO SPECS · 顶部展示的关键 4 行")
        Spacer(Modifier.height(8.dp))
        heroSpecs.forEachIndexed { i, spec ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(
                    placeholder = "Label",
                    value = spec.label,
                    onValueChange = { heroSpecs[i] = HeroSpec(it, spec.value) },
                    modifier = Modifier.weight(1f),
                )
                FormField(
                    placeholder = "Value",
                    value = spec.value,
                    onValueChange = { heroSpecs[i] = HeroSpec(spec.label, it) },
                    modifier = Modifier.weight(1.4f),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("SPECS · 完整参数表")
        Spacer(Modifier.height(8.dp))
        if (specRows.isEmpty()) {
            Text(
                text = "还没有完整参数 · 点 + 加一行",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        specRows.forEachIndexed { i, (k, v) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FormField(
                    placeholder = "Key",
                    value = k,
                    onValueChange = { specRows[i] = it to v },
                    modifier = Modifier.weight(1f),
                )
                FormField(
                    placeholder = "Value",
                    value = v,
                    onValueChange = { specRows[i] = k to it },
                    modifier = Modifier.weight(1.4f),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .border(0.5.dp, colors.line)
                        .clickable { specRows.removeAt(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("−", color = colors.sub, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .border(0.5.dp, colors.terra.copy(alpha = 0.6f))
                .clickable { specRows.add("" to "") }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ 加一行", color = colors.terra, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        SaveButton(dirty = dirty) {
            onUpdate(item.copy(
                heroSpecs = heroSpecs.toList()
                    .filter { it.label.isNotBlank() || it.value.isNotBlank() },
                specs = specRows.toList()
                    .filter { (k, _) -> k.isNotBlank() }
                    .associate { (k, v) -> k.trim() to v.trim() },
            ))
        }
        Spacer(Modifier.height(40.dp))
    }
}

// ─── Tab: 历史 ─────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(item: Item, onUpdate: (Item) -> Unit) {
    val colors = LocalTreasureColors.current
    var editing by remember { mutableStateOf<HistoryEditTarget?>(null) }
    var deleting by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .border(0.5.dp, colors.terra.copy(alpha = 0.6f))
                .clickable { editing = HistoryEditTarget.New }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("+ 加一条历史", color = colors.terra, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(16.dp))

        if (item.history.isEmpty()) {
            Text(
                text = "还没有时间轴 · 点上面 + 添加",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        item.history.forEachIndexed { idx, e ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(idx) {
                        detectTapGestures(
                            onTap = { editing = HistoryEditTarget.Existing(idx) },
                            onLongPress = { deleting = idx },
                        )
                    },
            ) {
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
                    if (idx != item.history.lastIndex) {
                        Box(modifier = Modifier
                            .padding(top = 4.dp)
                            .height(36.dp)
                            .width(0.5.dp)
                            .background(colors.line))
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = "tap 编辑 · 长按删除",
            color = colors.sub.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(40.dp))
    }

    editing?.let { target ->
        val initial: HistoryEvent = when (target) {
            HistoryEditTarget.New -> HistoryEvent(
                date = java.time.LocalDate.now().toString(),
                kind = HistoryKind.MILESTONE,
                title = "",
                note = "",
            )
            is HistoryEditTarget.Existing -> item.history[target.index]
        }
        HistoryEditDialog(
            initial = initial,
            onCancel = { editing = null },
            onSave = { e ->
                val updated = when (target) {
                    HistoryEditTarget.New -> item.history + e
                    is HistoryEditTarget.Existing ->
                        item.history.toMutableList().also { it[target.index] = e }
                }
                onUpdate(item.copy(history = updated.sortedBy { it.date }))
                editing = null
            },
        )
    }

    deleting?.let { idx ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除这条历史？") },
            text = { Text(item.history[idx].title) },
            confirmButton = {
                TextButton(onClick = {
                    val newHistory = item.history.toMutableList().also { it.removeAt(idx) }
                    onUpdate(item.copy(history = newHistory))
                    deleting = null
                }) { Text("删除", color = colors.terra) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

private sealed interface HistoryEditTarget {
    data object New : HistoryEditTarget
    data class Existing(val index: Int) : HistoryEditTarget
}

@Composable
private fun HistoryEditDialog(
    initial: HistoryEvent,
    onCancel: () -> Unit,
    onSave: (HistoryEvent) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var date by remember { mutableStateOf(initial.date) }
    var kind by remember { mutableStateOf(initial.kind) }
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (initial.title.isBlank()) "新增历史" else "编辑历史") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField("日期 (YYYY-MM-DD)", date, { date = it })
                Text("KIND", color = colors.sub, style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    HistoryKind.entries.forEach { k ->
                        FormChip(
                            label = "${kindGlyph(k)} ${k.name.lowercase()}",
                            selected = kind == k,
                        ) { kind = k }
                    }
                }
                FormField("标题",  title, { title = it })
                FormField("备注", note, { note = it })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(HistoryEvent(date.trim(), kind, title.trim(), note.trim())) },
                enabled = title.isNotBlank() && date.isNotBlank(),
            ) { Text("保存", color = colors.terra) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("取消") }
        },
        containerColor = colors.paper,
        titleContentColor = colors.ink,
        textContentColor = colors.sub,
    )
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

// ─── Tab: 影集 ─────────────────────────────────────────────────────────────

@Composable
private fun AlbumTab(
    photos: List<String>,
    onAddPhoto: (android.net.Uri) -> Unit,
    onRemovePhoto: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onAddPhoto(uri) }

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
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(colors.card)
                        .border(0.5.dp, colors.terra.copy(alpha = 0.6f))
                        .clickable {
                            pickPhoto.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        color = colors.terra,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            itemsIndexed(photos, key = { _, path -> path }) { _, path ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(colors.card)
                        .border(0.5.dp, colors.line)
                        .pointerInput(path) {
                            detectTapGestures(onLongPress = { pendingDelete = path })
                        },
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
        Spacer(Modifier.height(10.dp))
        Text(
            text = if (photos.isEmpty()) "点 + 添加实拍照片"
                   else "长按一张可删除 · ${photos.size} 张",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }

    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这张照片？") },
            text = { Text("文件会一并清除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete?.let(onRemovePhoto)
                    pendingDelete = null
                }) { Text("删除", color = colors.terra) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

// ─── Shared helpers ────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalTreasureColors.current
    Text(
        text = text,
        color = colors.sub,
        style = MaterialTheme.typography.labelSmall,
    )
}

@Composable
private fun FormField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = colors.sub.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(colors.terra),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.ink,
                    fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FormChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    val bg = if (selected) colors.ink else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) colors.paper else colors.ink
    val border = if (selected) colors.ink else colors.line
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SaveButton(dirty: Boolean, onSave: () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(if (dirty) colors.ink else colors.card)
            .border(0.5.dp, if (dirty) colors.ink else colors.line)
            .clickable(enabled = dirty, onClick = onSave)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (dirty) "保存修改" else "未变动",
            color = if (dirty) colors.paper else colors.sub,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun normalizedHeroSpecs(input: List<HeroSpec>): List<HeroSpec> =
    if (input.isNotEmpty()) input
    else List(4) { HeroSpec("", "") }
