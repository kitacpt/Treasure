package com.treasure.ui.category

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.HeroVector
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.components.BackArrow
import com.treasure.ui.components.EditPageHeader

/**
 * Cycle 0029：分类编辑改成全屏 route（之前在 Manager 抽屉里内嵌）。同款
 * 物品 Edit 页的 [EditPageHeader] 头 + [BackArrow] 返回，视觉一致。
 *
 * 路由两种入口：
 *   - Routes.CategoryNew → categoryId = null → 新建模式
 *   - Routes.CategoryEditPattern → categoryId 命中现有 row → 编辑模式
 */
@Composable
fun CategoryEditorRoute(
    categoryId: String?,
    onBack: () -> Unit,
    vm: CategoryManagerViewModel = viewModel(factory = CategoryManagerViewModel.Factory),
) {
    val all by vm.all.collectAsStateWithLifecycle()
    val initial = remember(all, categoryId) {
        if (categoryId.isNullOrBlank()) null
        else all.firstOrNull { it.id == categoryId }
    }
    // 编辑模式但仓库还没加载到（id 在但 list 还空）→ 静默等
    if (!categoryId.isNullOrBlank() && initial == null && all.isEmpty()) {
        val colors = LocalTreasureColors.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper),
        )
        return
    }
    CategoryEditorScreen(
        initial = initial,
        vm = vm,
        onBack = onBack,
        onSaveBuiltIn = { hidden ->
            initial?.let { info ->
                if (hidden != info.hidden) vm.setHidden(info.id, hidden)
            }
            onBack()
        },
        onSaveCustom = { nameZh, nameEn, hidden, photoPath ->
            if (initial != null) {
                vm.saveCustom(initial.id, nameZh, nameEn, initial.heroVector)
                if (hidden != initial.hidden) vm.setHidden(initial.id, hidden)
                onBack()
            } else if (photoPath != null) {
                vm.addCustomWithPhoto(nameZh, nameEn, photoPath) { onBack() }
            } else {
                // 防御：自定义新建没有 photo 时（按 canSave 应该被挡，但兜底）
                vm.addCustom(nameZh, nameEn, HeroVector.GENERIC) { onBack() }
            }
        },
        onDelete = {
            initial?.let { vm.deleteCustom(it.id) }
            onBack()
        },
    )
}

@Composable
private fun CategoryEditorScreen(
    initial: CategoryInfo?,
    vm: CategoryManagerViewModel,
    onBack: () -> Unit,
    onSaveBuiltIn: (hidden: Boolean) -> Unit,
    onSaveCustom: (nameZh: String, nameEn: String, hidden: Boolean, pendingPhotoPath: String?) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val isBuiltIn = initial?.isBuiltIn == true
    val isAdd = initial == null
    var nameZh by remember(initial) { mutableStateOf(initial?.nameZh.orEmpty()) }
    var nameEn by remember(initial) { mutableStateOf(initial?.nameEn.orEmpty()) }
    // Cycle 0030：插画统一改成从相册挑的图。null = 没图。built-in 在没图时
    // fall back 到 enum 默认插画；自定义必须有图才能创建。
    // pendingPhotoForNew 是 VM 在新建模式下暂存的本地路径（在用户点 [新建]
    // 之前不写 DB），collect 进来给 UI 回显。
    val currentPhoto = if (isAdd) vm.pendingPhotoForNew else initial?.heroPhotoPath
    var photoTick by remember { mutableStateOf(0) }  // 强迫 recompose 跟 pendingPhotoForNew
    var hidden by remember(initial) { mutableStateOf(initial?.hidden == true) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            vm.pickHeroPhoto(initial?.id, uri) { photoTick++ }
        }
    }

    val canSave = when {
        isBuiltIn -> true                                  // 内建始终能存（fall back 到默认插画）
        isAdd -> nameZh.isNotBlank() && currentPhoto != null  // 自定义新建必须有名 + 图
        else -> nameZh.isNotBlank()                        // 自定义编辑：图可保留也可清
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
                // 头：跟物品 Edit 页同款 — BackArrow + 居中标题 + 右侧 [保存]/[新建]
                EditPageHeader(
                    title = if (isAdd) "New" else "Edit",
                    subtitle = if (isAdd) "新增分类" else (initial?.nameZh ?: "分类"),
                    leading = { BackArrow(color = colors.ink, onClick = onBack) },
                    trailing = {
                        Text(
                            text = if (isAdd) "新建" else "保存",
                            color = if (canSave) colors.terra else colors.sub.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(enabled = canSave) {
                                    if (isBuiltIn) onSaveBuiltIn(hidden)
                                    else onSaveCustom(nameZh, nameEn, hidden, currentPhoto)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (isBuiltIn) {
                item {
                    Text(
                        text = "内建分类名字不可改；插画用内建默认，也可以从相册挑一张代表图覆盖。",
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            } else if (isAdd) {
                item {
                    Text(
                        text = "新建必须填中文名 + 从相册挑一张代表图。",
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // 头像 — 优先 photo，没图时 built-in fall back 到 enum 插画
            item {
                Spacer(Modifier.height(6.dp))
                @Suppress("UNUSED_EXPRESSION") photoTick  // 触发 recompose
                AvatarHero(
                    photoPath = currentPhoto,
                    fallbackHeroVector = if (isBuiltIn) initial?.heroVector else null,
                )
                Spacer(Modifier.height(10.dp))
            }

            // 相册 picker：[从相册选] [清除]
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    Text(
                        text = if (isAdd) "代表图 · 必选" else "代表图",
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(2.dp))
                                .border(0.5.dp, colors.line)
                                .clickable {
                                    pickPhoto.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly,
                                        ),
                                    )
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (currentPhoto == null) "+ 从相册选" else "换一张",
                                color = colors.terra,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (currentPhoto != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(2.dp))
                                    .border(0.5.dp, colors.line)
                                    .clickable {
                                        vm.clearHeroPhoto(initial?.id)
                                        photoTick++
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "清除",
                                    color = colors.sub,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            // 中文 / 英文名
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldRow(label = "中文名") {
                        EditorTextField(
                            value = nameZh,
                            onValueChange = { nameZh = it },
                            enabled = !isBuiltIn,
                            placeholder = "如 图书",
                        )
                    }
                    FieldRow(label = "英文名") {
                        EditorTextField(
                            value = nameEn,
                            onValueChange = { nameEn = it },
                            enabled = !isBuiltIn,
                            placeholder = "如 Books（可选）",
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            // 显示状态（仅编辑模式）
            if (!isAdd) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                        FieldRow(label = "显示") {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PillChip("显示", !hidden) { hidden = false }
                                PillChip("隐藏", hidden) { hidden = true }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }

            // 删除（仅自定义编辑）
            if (initial != null && !isBuiltIn) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                        Text(
                            text = "DANGER ZONE",
                            color = colors.sub,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .border(0.5.dp, colors.terra.copy(alpha = 0.55f))
                                .clickable { confirmingDelete = true }
                                .padding(vertical = 14.dp),
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
 * Cycle 0030：分类编辑页的"头像"。优先显示用户挑的代表图 [photoPath]，没图
 * 时若 [fallbackHeroVector] 非 null（= 内建分类）则画 enum 默认插画，否则
 * 是 italic "+ 从相册选" 占位。
 */
@Composable
private fun AvatarHero(photoPath: String?, fallbackHeroVector: HeroVector?) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier.fillMaxWidth(),
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
            when {
                !photoPath.isNullOrBlank() -> {
                    AsyncImage(
                        model = photoPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                }
                fallbackHeroVector != null -> {
                    val stub = remember(fallbackHeroVector) {
                        com.treasure.core.domain.Item(
                            id = "preview",
                            category = "preview",
                            brand = "", model = "", nickname = "", acquired = "", parted = null,
                            status = com.treasure.core.domain.ItemStatus.OWNED,
                            palette = listOf("#0e0e0e", "#a47836", "#e8e2d4", "#5a5a5a"),
                            oneLiner = "",
                            heroVector = fallbackHeroVector,
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
                else -> {
                    Text(
                        text = "+ 从相册选",
                        color = colors.sub.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                    )
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
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = enabled,
                cursorBrush = SolidColor(colors.terra),
                textStyle = LocalTextStyle.current.copy(
                    color = if (enabled) colors.ink else colors.sub.copy(alpha = 0.5f),
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.line),
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

