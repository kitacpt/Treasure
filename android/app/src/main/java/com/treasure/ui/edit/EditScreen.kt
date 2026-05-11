package com.treasure.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import com.treasure.theme.TreasureColors
import com.treasure.ui.components.BackArrow
import com.treasure.ui.components.EditPageHeader
import com.treasure.ui.components.InlineDropdown
import com.treasure.ui.components.SectionDivider
import kotlin.math.roundToInt

@Composable
fun EditRoute(
    itemId: String,
    onDone: () -> Unit,
    vm: com.treasure.ui.detail.DetailViewModel = viewModel(
        factory = com.treasure.ui.detail.DetailViewModel.factory(itemId),
    ),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val item = state.item
    val colors = LocalTreasureColors.current
    // Cycle 0027：从 TreasureApp 拉分类仓库给 InlineDropdown 用
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext
        as com.treasure.TreasureApp
    val categories by remember { app.categoryRepository.observeAll() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    if (!state.loaded || item == null) {
        Box(modifier = Modifier.fillMaxSize().background(colors.paper))
        return
    }
    EditScreen(
        item = item,
        categories = categories,
        onCancel = onDone,
        onUpdate = vm::update,
        onAddPhoto = vm::addPhoto,
        onAddPhotos = vm::addPhotos,
        onRemovePhoto = vm::removePhoto,
        onDelete = { vm.delete(onDone) },
    )
}

@Composable
fun EditScreen(
    item: Item,
    categories: List<com.treasure.core.domain.CategoryInfo>,
    onCancel: () -> Unit,
    onUpdate: (Item) -> Unit,
    onAddPhoto: (android.net.Uri) -> Unit,
    onAddPhotos: (List<android.net.Uri>) -> Unit,
    onRemovePhoto: (String) -> Unit,
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
    var avatarPhoto by remember(item.id) { mutableStateOf(item.avatarPhotoPath) }
    val specs = remember(item.id) {
        mutableStateListOf<HeroSpec>().apply { addAll(item.specs) }
    }

    val dirty = brand != item.brand ||
        model != item.model ||
        nickname != item.nickname ||
        oneLiner != item.oneLiner ||
        acquired != item.acquired ||
        parted != (item.parted ?: "") ||
        status != item.status ||
        category != item.category ||
        heroVector != item.heroVector ||
        avatarPhoto != item.avatarPhotoPath ||
        specs.toList() != item.specs

    fun commit() {
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
            avatarPhotoPath = avatarPhoto,
            specs = specs.toList()
                .filter { it.label.isNotBlank() || it.value.isNotBlank() },
        ))
    }

    // 影集管理：launchers 必须在 Composable scope，所以扯到 EditScreen 顶层，
    // 然后把 onTakePhoto / onPickPhotos 当回调传给头像选择器。
    val context = androidx.compose.ui.platform.LocalContext.current
    var pendingCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val pickMultiple = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { uris -> if (uris.isNotEmpty()) onAddPhotos(uris) }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCaptureUri
        pendingCaptureUri = null
        if (success && uri != null) onAddPhoto(uri)
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) launchCamera(context, takePicture) { pendingCaptureUri = it }
    }
    val startCamera: () -> Unit = {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CAMERA,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            launchCamera(context, takePicture) { pendingCaptureUri = it }
        } else {
            cameraPermLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    val pickPhotos: () -> Unit = {
        pickMultiple.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding()
            .imePadding(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 60.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                // Cycle 0027：item.category 是 String id，查 CategoryInfo 拿
                // 显示名；查不到（孤儿 id，比如自定义分类被删了）回退到原 id。
                val subtitle = categories.firstOrNull { it.id == item.category }?.nameZh
                    ?: item.category
                EditPageHeader(
                    title = "Edit",
                    subtitle = subtitle,
                    leading = { BackArrow(color = colors.ink, onClick = onCancel) },
                    trailing = {
                        Text(
                            text = if (dirty) "保存" else "已保存",
                            color = if (dirty) colors.terra else colors.sub.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clickable(enabled = dirty, onClick = ::commit)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
            }

            // 头像式插画选择器：默认只展示选中那一张大图，点开才展候选
            item {
                Spacer(Modifier.height(6.dp))
                com.treasure.ui.components.HeroAvatarPicker(
                    categoryId = category,
                    palette = item.palette,
                    options = remember(category) { com.treasure.ui.add.heroVectorOptionsForId(category) },
                    selected = heroVector,
                    onSelect = {
                        heroVector = it
                        // 选了线描就清掉头像照片，让头像回到插画
                        avatarPhoto = null
                    },
                    photoOptions = item.photos,
                    selectedPhoto = avatarPhoto,
                    onSelectPhoto = { avatarPhoto = it },
                    onTakePhoto = startCamera,
                    onPickPhotos = pickPhotos,
                    onRemovePhoto = onRemovePhoto,
                )
                Spacer(Modifier.height(8.dp))
            }

            item { Section("基础") }
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("品牌",   brand,    { brand = it })
                    LabeledField("型号",   model,    { model = it })
                    LabeledField("昵称",   nickname, { nickname = it })
                    LabeledField("简介",   oneLiner, { oneLiner = it })
                }
            }

            // “时间” section 移除：购入 / 出手 都由 “历史” section 的 ACQUIRED /
            // PARTED 事件负责。Item.acquired / parted 字段仍存（兼容老物品 +
            // 历史事件转换），用户改时间走历史。
            item { Section("标签") }
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FieldLabel("状态")
                        Spacer(Modifier.width(LABEL_GAP))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip("Owned",  status == ItemStatus.OWNED)  { status = ItemStatus.OWNED }
                            Chip("Parted", status == ItemStatus.PARTED) { status = ItemStatus.PARTED }
                            Chip("Rented", status == ItemStatus.RENTED) { status = ItemStatus.RENTED }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FieldLabel("品类")
                        Spacer(Modifier.width(LABEL_GAP))
                        // Cycle 0027：选项来自分类仓库（含内建 + 用户自定义）。
                        // selected 不在列表里（孤儿 id）时给一个伪 CategoryInfo
                        // 占位，否则 dropdown 显示不出当前值。
                        val current = categories.firstOrNull { it.id == category }
                            ?: com.treasure.core.domain.CategoryInfo(
                                id = category, nameZh = category, nameEn = "",
                                heroVector = com.treasure.core.domain.HeroVector.GENERIC,
                                hidden = false, sortOrder = -1, isBuiltIn = false,
                            )
                        val visibleOptions = remember(categories, current) {
                            val list = categories.filter { !it.hidden }
                            if (list.any { it.id == current.id }) list else list + current
                        }
                        InlineDropdown(
                            options = visibleOptions,
                            selected = current,
                            label = { it.nameZh },
                            onSelect = { category = it.id },
                        )
                    }
                }
            }

            // “插画” section gone — moved to the avatar picker at the top.
            // Keep an empty placeholder item so existing surrounding code
            // doesn't shift unexpectedly when re-reading.
            item {
                Spacer(Modifier.height(0.dp))
            }

            item { Section("参数 · 拖动选前 4 作关键参数") }
            item {
                ReorderableSpecs(
                    specs = specs,
                    onChange = { i, spec -> specs[i] = spec },
                    onDelete = { i -> specs.removeAt(i) },
                    onMove = { from, to ->
                        val moved = specs.removeAt(from)
                        specs.add(to, moved)
                    },
                )
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                    AddRowButton(label = "+ 加一行 参数") { specs.add(HeroSpec("", "")) }
                }
            }

            item { Section("历史") }
            item {
                HistorySection(
                    item = item,
                    onUpdateHistory = { history ->
                        onUpdate(item.copy(history = history.sortedBy { it.date }))
                    },
                )
            }

            // 实拍 section 整合进顶部头像选择器（cycle 0017）：
            // 拍照 / 选照片 / 长按删都在那里。
            item { Section("DANGER ZONE") }
            item { DangerZone(onDelete = onDelete) }

            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

private val LABEL_WIDTH = 56.dp
private val LABEL_GAP = 12.dp


@Composable
internal fun Section(label: String) = SectionDivider(label = label)

@Composable
internal fun FieldLabel(text: String) {
    val colors = LocalTreasureColors.current
    Text(
        text = text,
        color = colors.sub,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.width(LABEL_WIDTH),
    )
}

@Composable
internal fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
) {
    val colors = LocalTreasureColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        FieldLabel(label)
        Spacer(Modifier.width(LABEL_GAP))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                if (value.isEmpty()) {
                    Text(
                        text = hint ?: "",
                        color = colors.sub.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    cursorBrush = SolidColor(colors.terra),
                    textStyle = LocalTextStyle.current.copy(
                        color = colors.ink,
                        fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
        }
    }
}

@Composable
internal fun InlineField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = modifier
            .background(colors.card)
            .border(0.5.dp, colors.line)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = colors.sub.copy(alpha = 0.4f),
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

@Composable
internal fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
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
internal fun DeleteIcon(onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, colors.line)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("−", color = colors.sub, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun AddRowButton(label: String, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, colors.terra.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.terra, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Hero vector picker ─────────────────────────────────────────────────

// ── Reorderable specs ─────────────────────────────────────────────────

private val ROW_HEIGHT = 56.dp

@Composable
private fun ReorderableSpecs(
    specs: List<HeroSpec>,
    onChange: (Int, HeroSpec) -> Unit,
    onDelete: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val density = LocalDensity.current
    val rowHeightPx = with(density) { ROW_HEIGHT.toPx() }
    var dragging by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    val draggedTarget = if (dragging != -1) {
        (dragging + (dragOffset / rowHeightPx).roundToInt())
            .coerceIn(0, specs.size - 1)
    } else -1

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        specs.forEachIndexed { idx, spec ->
            // Hero divider sits BEFORE row index HERO_COUNT (between hero and tail).
            // 拖动时 make-room shift 只按 rowHeightPx 算，divider 自己的高度会让
            // 视觉上相邻行错位 + 被拖动行盖住分割线。所以拖动期间换成同高 Spacer，
            // 拖完再展示分割线 — 看起来才像上下换位。
            if (idx == Item.HERO_SPEC_COUNT && specs.size > Item.HERO_SPEC_COUNT) {
                if (dragging == -1) HeroDivider() else HeroDividerSpacer()
            }

            val isDragging = dragging == idx
            // Compute "make-room" shift for non-dragging rows
            val shift = when {
                dragging == -1 || idx == dragging -> 0f
                dragging < idx && idx <= draggedTarget -> -rowHeightPx
                dragging > idx && idx >= draggedTarget -> rowHeightPx
                else -> 0f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else shift
                        if (isDragging) shadowElevation = 6.dp.toPx()
                    }
                    .zIndex(if (isDragging) 1f else 0f)
                    .background(if (isDragging) colors.card else Color.Transparent),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InlineField(
                        placeholder = if (idx < Item.HERO_SPEC_COUNT) "key (hero)" else "key",
                        value = spec.label,
                        onValueChange = { onChange(idx, HeroSpec(it, spec.value)) },
                        modifier = Modifier.weight(1f),
                    )
                    InlineField(
                        placeholder = "value",
                        value = spec.value,
                        onValueChange = { onChange(idx, HeroSpec(spec.label, it)) },
                        modifier = Modifier.weight(1.4f),
                    )
                    DragHandle(
                        modifier = Modifier
                            .size(36.dp)
                            .pointerInput(idx) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        dragging = idx
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        val target = (idx + (dragOffset / rowHeightPx)
                                            .roundToInt()).coerceIn(0, specs.size - 1)
                                        if (target != idx) onMove(idx, target)
                                        dragging = -1
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        dragging = -1
                                        dragOffset = 0f
                                    },
                                    onDrag = { _, drag -> dragOffset += drag.y },
                                )
                            },
                    )
                    DeleteIcon { onDelete(idx) }
                }
            }
        }
        if (specs.isEmpty()) {
            Text(
                text = "暂无参数 · 点下面 + 添加",
                color = colors.sub.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "长按 ≡ 拖动重排 · 顶部 ${Item.HERO_SPEC_COUNT} 行作为关键参数显示",
                color = colors.sub.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun HeroDivider() {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(colors.terra.copy(alpha = 0.5f)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "↑ 关键 ${Item.HERO_SPEC_COUNT} 项",
            color = colors.terra.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(colors.terra.copy(alpha = 0.5f)),
        )
    }
}

/** 拖动时占位用的同高 spacer：跟 [HeroDivider] 视觉高度对齐
 *  (vertical padding 8dp × 2 + 0.5dp line ≈ 16.5dp)，避免 layout 跳动。 */
@Composable
private fun HeroDividerSpacer() {
    Spacer(modifier = Modifier.fillMaxWidth().height(16.5.dp))
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .border(0.5.dp, colors.line),
        contentAlignment = Alignment.Center,
    ) {
        Text("≡", color = colors.sub, style = MaterialTheme.typography.titleMedium)
    }
}

// ── History section ─────────────────────────────────────────────────────

@Composable
private fun HistorySection(
    item: Item,
    onUpdateHistory: (List<HistoryEvent>) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var editing by remember { mutableStateOf<HistoryEditTarget?>(null) }
    var deleting by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        if (item.history.isEmpty()) {
            Text(
                text = "还没有时间轴",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            item.history.forEachIndexed { idx, e ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(idx) {
                            detectTapGestures(
                                onTap = { editing = HistoryEditTarget.Existing(idx) },
                                onLongPress = { deleting = idx },
                            )
                        }
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        text = e.date.replace("-", "."),
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(64.dp),
                    )
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
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = e.title,
                            color = colors.ink,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (e.note.isNotBlank()) {
                            Text(
                                text = e.note,
                                color = colors.sub,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (idx != item.history.lastIndex) {
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "tap 编辑 · 长按删除",
                color = colors.sub.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(12.dp))
        AddRowButton(label = "+ 加一条 历史") { editing = HistoryEditTarget.New }
    }

    editing?.let { target ->
        val initial = when (target) {
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
                val newHistory = when (target) {
                    HistoryEditTarget.New -> item.history + e
                    is HistoryEditTarget.Existing -> item.history.toMutableList().also { it[target.index] = e }
                }
                onUpdateHistory(newHistory)
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
                    onUpdateHistory(newHistory)
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
                LabeledField("日期", date, { date = it }, hint = "YYYY-MM-DD")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldLabel("类型")
                    Spacer(Modifier.width(LABEL_GAP))
                    InlineDropdown(
                        options = HistoryKind.entries,
                        selected = kind,
                        label = { kindLabelZh(it) },
                        glyph = { kindGlyph(it) },
                        onSelect = { kind = it },
                    )
                }
                LabeledField("标题", title, { title = it })
                LabeledField("备注", note, { note = it })
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

private fun kindLabelZh(kind: HistoryKind): String = when (kind) {
    HistoryKind.ACQUIRED  -> "购入"
    HistoryKind.MILESTONE -> "里程碑"
    HistoryKind.MAINTAIN  -> "保养"
    HistoryKind.MOD       -> "改装"
    HistoryKind.PARTED    -> "出手"
}

private fun kindColor(kind: HistoryKind, colors: TreasureColors): Color = when (kind) {
    HistoryKind.ACQUIRED  -> colors.terra
    HistoryKind.MILESTONE -> colors.ink
    HistoryKind.MAINTAIN  -> colors.sub
    HistoryKind.MOD       -> colors.sub
    HistoryKind.PARTED    -> colors.sub
}

private fun launchCamera(
    context: android.content.Context,
    launcher: androidx.activity.compose.ManagedActivityResultLauncher<android.net.Uri, Boolean>,
    onPending: (android.net.Uri) -> Unit,
) {
    val dir = java.io.File(context.filesDir, "captures").apply { mkdirs() }
    val file = java.io.File(dir, "${java.util.UUID.randomUUID()}.jpg")
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    onPending(uri)
    launcher.launch(uri)
}

// ── Danger zone ─────────────────────────────────────────────────────────

@Composable
private fun DangerZone(onDelete: () -> Unit) {
    val colors = LocalTreasureColors.current
    var confirming by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
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
            Text(text = "→", color = colors.terra, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "记录会从图鉴里移除，不可恢复",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("确认删除？") },
            text = { Text("这件物品的所有记录会被清除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = { confirming = false; onDelete() }) {
                    Text("删除", color = colors.terra)
                }
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
