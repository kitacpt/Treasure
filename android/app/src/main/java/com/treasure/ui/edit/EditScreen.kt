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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberUpdatedState
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

            item { Section("参数") }
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
                    AddRowButton(label = "+") { specs.add(HeroSpec("", "")) }
                }
            }

            item { Section("历史") }
            item {
                HistorySection(
                    history = item.history,
                    onUpdateHistory = { history ->
                        onUpdate(item.copy(history = history.sortedBy { it.date }))
                    },
                )
            }

            item { Section("操作") }
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

/**
 * Cycle 0031：参数行专用 — 没有自带边框 / 背景，外层负责整组卡片视觉。
 */
@Composable
internal fun BareField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Box(modifier = modifier) {
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

/**
 * Cycle 0031 复修：用 CategoryManager 那套"预览终态布局"算法做 specs 拖动 —
 * 每行（含 HERO/TAIL 分割线）按它在松手后新布局的 visualSlot 实时摆位，
 * 拖到哪松手到哪、行 + 分割线一起平移、不再 swap divider 为 spacer。
 * gesture callback 用 rememberUpdatedState 兜底（理论上 EditScreen 这边
 * `pointerInput(idx)` 已经按 idx 重 key，但写得更稳）。
 */
@Composable
private fun ReorderableSpecs(
    specs: List<HeroSpec>,
    onChange: (Int, HeroSpec) -> Unit,
    onDelete: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val density = LocalDensity.current
    val rowPx = with(density) { ROW_HEIGHT.toPx() }
    var dragging by remember { mutableStateOf(-1) }
    var dragOffset by remember { mutableStateOf(0f) }

    val N = specs.size
    val showDivider = N > Item.HERO_SPEC_COUNT
    // 视觉布局 slot：
    //   hero 段 idx 0..H-1   → visualSlot = idx
    //   divider               → visualSlot = H（如果 showDivider）
    //   tail 段 idx H..N-1   → visualSlot = idx + 1（如果 showDivider）否则 = idx
    fun toVisual(i: Int): Int =
        if (showDivider && i >= Item.HERO_SPEC_COUNT) i + 1 else i
    val totalSlots = if (showDivider) N + 1 else N

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT * totalSlots),
        ) {
            // 用户指尖中心当前 visualSlot
            val originVisual = if (dragging < 0) -1 else toVisual(dragging)
            val targetVisualSlot = if (dragging < 0) -1 else {
                val centerY = originVisual * rowPx + dragOffset + rowPx / 2f
                (centerY / rowPx).toInt().coerceIn(0, totalSlots - 1)
            }
            // 预览：把 dragging 行的"终态 specs idx"算出来 — visualSlot 反推
            // specs idx 时跳过 divider 那一格
            val previewNewSpecsIdx = if (dragging < 0) -1 else {
                val raw = if (showDivider && targetVisualSlot > Item.HERO_SPEC_COUNT)
                    targetVisualSlot - 1
                else if (showDivider && targetVisualSlot == Item.HERO_SPEC_COUNT) {
                    // 落在 divider slot：按拖动方向决定 snap 边
                    if (dragging < Item.HERO_SPEC_COUNT) Item.HERO_SPEC_COUNT - 1
                    else Item.HERO_SPEC_COUNT
                } else targetVisualSlot
                raw.coerceIn(0, N - 1)
            }

            specs.forEachIndexed { idx, spec ->
                val isDragging = idx == dragging
                val translateY: Float = when {
                    isDragging -> toVisual(idx) * rowPx + dragOffset
                    dragging < 0 -> toVisual(idx) * rowPx
                    else -> {
                        // 终态里这行的 specs idx
                        val newI = if (idx < dragging) idx else idx - 1
                        val finalI =
                            if (newI < previewNewSpecsIdx) newI else newI + 1
                        toVisual(finalI) * rowPx
                    }
                }

                val latestOnDragStart by rememberUpdatedState({
                    dragging = specs.indexOfFirst { it === spec }
                        .takeIf { it >= 0 } ?: idx
                    dragOffset = 0f
                })
                val latestOnDragEnd by rememberUpdatedState({
                    val d = dragging
                    if (d >= 0) {
                        val originV = toVisual(d)
                        val centerY = originV * rowPx + dragOffset + rowPx / 2f
                        val target = (centerY / rowPx).toInt()
                            .coerceIn(0, totalSlots - 1)
                        val newSpecsIdx =
                            if (showDivider && target > Item.HERO_SPEC_COUNT) target - 1
                            else if (showDivider && target == Item.HERO_SPEC_COUNT) {
                                if (d < Item.HERO_SPEC_COUNT) Item.HERO_SPEC_COUNT - 1
                                else Item.HERO_SPEC_COUNT
                            } else target
                        val clamped = newSpecsIdx.coerceIn(0, N - 1)
                        if (clamped != d) onMove(d, clamped)
                    }
                    dragging = -1
                    dragOffset = 0f
                })
                val latestOnDragCancel by rememberUpdatedState({
                    dragging = -1
                    dragOffset = 0f
                })

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .graphicsLayer {
                            translationY = translateY
                            if (isDragging) shadowElevation = 6.dp.toPx()
                        }
                        .zIndex(if (isDragging) 1f else 0f),
                ) {
                    // Cycle 0031：参数行整体一个圆角卡 — key / value 共用同一
                    // 个外框 + 中间 0.5dp 竖分隔；右侧贴一个握把 + 一个 ✕，没
                    // 边框，跟卡片左边对齐。
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isDragging) colors.card else colors.card.copy(alpha = 0.55f),
                                )
                                .border(0.5.dp, colors.line, RoundedCornerShape(6.dp)),
                        ) {
                            BareField(
                                placeholder = if (idx < Item.HERO_SPEC_COUNT) "key" else "key",
                                value = spec.label,
                                onValueChange = { onChange(idx, HeroSpec(it, spec.value)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .width(0.5.dp)
                                    .fillMaxHeight()
                                    .padding(vertical = 8.dp)
                                    .background(colors.line),
                            )
                            BareField(
                                placeholder = "value",
                                value = spec.value,
                                onValueChange = { onChange(idx, HeroSpec(spec.label, it)) },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                        // 拖动握把：3 横纹，长按触发。
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .pointerInput(spec) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { latestOnDragStart() },
                                        onDragEnd = { latestOnDragEnd() },
                                        onDragCancel = { latestOnDragCancel() },
                                        onDrag = { _, drag -> dragOffset += drag.y },
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
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
                        // 删除：极简 ✕，无边框
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .clickable { onDelete(idx) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "✕",
                                color = colors.sub,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            // 分割线（HERO / TAIL）— 跟 row 同高单独占一个 visualSlot，固定位置
            if (showDivider) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .graphicsLayer { translationY = Item.HERO_SPEC_COUNT * rowPx },
                    contentAlignment = Alignment.Center,
                ) {
                    HeroDividerInline()
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
        }
    }
}

/** Cycle 0031：关键 / 非关键 参数之间的提示行 — 只保留居中文案，两侧的
 *  横线删掉（视觉太重，跟上下 spec 卡的边框叠在一起像 2-3 条线）。 */
@Composable
private fun HeroDividerInline() {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "↑ 拖动选前 ${Item.HERO_SPEC_COUNT} 作关键参数",
            color = colors.terra.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
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

/**
 * Cycle 0031：内部公开，让 AddPreview 也能复用同一份历史时间轴 UI（用户
 * 原话："草稿也要有历史栏，和编辑页逻辑一致"）。签名从 `item: Item` 改成
 * 直接收 `history: List<HistoryEvent>`，跟具体宿主解耦。
 */
@Composable
internal fun HistorySection(
    history: List<HistoryEvent>,
    onUpdateHistory: (List<HistoryEvent>) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var editing by remember { mutableStateOf<HistoryEditTarget?>(null) }
    var deleting by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        if (history.isEmpty()) {
            Text(
                text = "还没有时间轴",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            history.forEachIndexed { idx, e ->
                // Cycle 0031：每条历史一张小卡 — 左侧 36dp 圆 + kind glyph，中
                // 间标题 + 备注，右侧完整日期。比之前的扁平 row 紧凑、视觉重
                // 量好。整行 tap 编辑 / 长按删。
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
                        .padding(vertical = 12.dp),
                ) {
                    // Cycle 0031：emoji 自带色，背景用 kindColor 淡填充 + 边框，
                    // emoji 摆中间，纸色不冲突。
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(kindColor(e.kind, colors).copy(alpha = 0.14f))
                            .border(0.5.dp, kindColor(e.kind, colors).copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = kindGlyph(e.kind),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = e.title.ifBlank { kindLabelZh(e.kind) },
                            color = colors.ink,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = formatHistoryDate(e.date),
                            color = colors.sub,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (e.note.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = e.note,
                                color = colors.sub,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                if (idx != history.lastIndex) {
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
        AddRowButton(label = "+") { editing = HistoryEditTarget.New }
    }

    editing?.let { target ->
        val initial = when (target) {
            HistoryEditTarget.New -> HistoryEvent(
                date = java.time.LocalDate.now().toString(),
                kind = HistoryKind.MILESTONE,
                title = "",
                note = "",
            )
            is HistoryEditTarget.Existing -> history[target.index]
        }
        HistoryEditDialog(
            initial = initial,
            onCancel = { editing = null },
            onSave = { e ->
                val newHistory = when (target) {
                    HistoryEditTarget.New -> history + e
                    is HistoryEditTarget.Existing -> history.toMutableList().also { it[target.index] = e }
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
            text = { Text(history[idx].title) },
            confirmButton = {
                TextButton(onClick = {
                    val newHistory = history.toMutableList().also { it.removeAt(idx) }
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

internal sealed interface HistoryEditTarget {
    data object New : HistoryEditTarget
    data class Existing(val index: Int) : HistoryEditTarget
}

/**
 * Cycle 0031：历史新增/编辑改成 ModalBottomSheet — 跟历史抽屉 / 分类抽屉
 * 同款上拖体验。日期用 Material DatePicker；类型选择改成顶部居中的圆形
 * icon picker（5 个 HistoryKind 摆一行）。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryEditDialog(
    initial: HistoryEvent,
    onCancel: () -> Unit,
    onSave: (HistoryEvent) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var date by remember { mutableStateOf(initial.date) }
    var kind by remember { mutableStateOf(initial.kind) }
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var pickingDate by remember { mutableStateOf(false) }

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 顶部居中：5 个圆形 icon 选 HistoryKind
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                HistoryKind.entries.forEach { k ->
                    val selected = k == kind
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 6.dp)
                            .size(if (selected) 48.dp else 40.dp)
                            .clip(CircleShape)
                            .background(kindColor(k, colors).copy(alpha = if (selected) 0.30f else 0.14f))
                            .border(
                                if (selected) 1.2.dp else 0.5.dp,
                                kindColor(k, colors).copy(alpha = if (selected) 0.9f else 0.55f),
                                CircleShape,
                            )
                            .clickable { kind = k },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = kindGlyph(k),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Text(
                text = kindLabelZh(kind),
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            // 日期 — 点击调出 DatePicker
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel("日期")
                Spacer(Modifier.width(LABEL_GAP))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .border(0.5.dp, colors.line, RoundedCornerShape(6.dp))
                        .clickable { pickingDate = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = if (date.isBlank()) "选择日期" else date,
                        color = if (date.isBlank()) colors.sub.copy(alpha = 0.5f) else colors.ink,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "📅",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            LabeledField("标题", title, { title = it })
            LabeledField("备注", note, { note = it })
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) { Text("取消", color = colors.sub) }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        onSave(HistoryEvent(date.trim(), kind, title.trim(), note.trim()))
                    },
                    enabled = title.isNotBlank() && date.isNotBlank(),
                ) { Text("保存", color = colors.terra) }
            }
        }
    }

    if (pickingDate) {
        val initialMillis = remember(date) {
            runCatching {
                val parsed = if (date.isNotBlank()) java.time.LocalDate.parse(date)
                else java.time.LocalDate.now()
                parsed.atStartOfDay(java.time.ZoneOffset.UTC)
                    .toInstant().toEpochMilli()
            }.getOrDefault(System.currentTimeMillis())
        }
        val dateState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { ms ->
                        date = java.time.Instant.ofEpochMilli(ms)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate().toString()
                    }
                    pickingDate = false
                }) { Text("确定", color = colors.terra) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) {
                    Text("取消", color = colors.sub)
                }
            },
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = colors.paper,
            ),
        ) {
            androidx.compose.material3.DatePicker(state = dateState)
        }
    }
}

/** Cycle 0031 复修：HistoryKind 用 emoji — 系统字体自带 emoji 渲染，比单字
 *  + 单色 ink 圆背景视觉重量更亲切。挑了 5 个广泛兼容的：购入🛒、里程碑🏆、
 *  保养🔧、改装⚙️、出手👋。 */
internal fun kindGlyph(kind: HistoryKind): String = when (kind) {
    HistoryKind.ACQUIRED  -> "🛒"
    HistoryKind.MILESTONE -> "🏆"
    HistoryKind.MAINTAIN  -> "🔧"
    HistoryKind.MOD       -> "⚙️"
    HistoryKind.PARTED    -> "👋"
}

internal fun kindLabelZh(kind: HistoryKind): String = when (kind) {
    HistoryKind.ACQUIRED  -> "购入"
    HistoryKind.MILESTONE -> "里程碑"
    HistoryKind.MAINTAIN  -> "保养"
    HistoryKind.MOD       -> "改装"
    HistoryKind.PARTED    -> "出手"
}

/** Cycle 0031：ISO 日期 ("2026-05-12") → "2026 年 5 月 12 日"。解析失败时
 *  原样回退。 */
internal fun formatHistoryDate(iso: String): String = runCatching {
    val d = java.time.LocalDate.parse(iso)
    "${d.year} 年 ${d.monthValue} 月 ${d.dayOfMonth} 日"
}.getOrDefault(iso)

internal fun kindColor(kind: HistoryKind, colors: TreasureColors): Color = when (kind) {
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
                text = "删除",
                color = colors.terra,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(text = "→", color = colors.terra, style = MaterialTheme.typography.bodyLarge)
        }
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
