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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.HeroVector
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.add.canonical
import com.treasure.ui.add.heroVectorOptionsForId
import com.treasure.ui.add.uniqueHeroVectors
import com.treasure.ui.components.BackArrow
import com.treasure.ui.components.EditPageHeader
import com.treasure.ui.components.HeroAvatarPicker

/**
 * Cycle 0029：分类编辑全屏 route。
 * Cycle 0031：插画选择改用与物品 Edit 页同款 [HeroAvatarPicker] — 点头像即
 *   展开候选行（"+ 从相册选" 动作 chip + 当前 photo + 可选线描），跟物品编
 *   辑一致的肌肉记忆。
 *
 * 路由：
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
    // 编辑模式但仓库还没加载完（id 在但 list 还空）→ 静默等
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
    )
}

@Composable
private fun CategoryEditorScreen(
    initial: CategoryInfo?,
    vm: CategoryManagerViewModel,
    onBack: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val isBuiltIn = initial?.isBuiltIn == true
    val isAdd = initial == null

    var nameZh by remember(initial) { mutableStateOf(initial?.nameZh.orEmpty()) }
    var nameEn by remember(initial) { mutableStateOf(initial?.nameEn.orEmpty()) }
    var heroVector by remember(initial) {
        mutableStateOf(initial?.heroVector ?: HeroVector.GENERIC)
    }
    var hidden by remember(initial) { mutableStateOf(initial?.hidden == true) }
    var confirmingDelete by remember { mutableStateOf(false) }

    // 当前 photo 显示用：
    //   - 编辑模式：来自 initial（DB 行的 hero_photo_path），改动通过 vm
    //     立刻写 DB，all StateFlow 触发 recompose，所以这里再读 initial 是
    //     stale；用一个 photoTick 强迫 recompose 拉新 initial。
    //   - 新建模式：vm.pendingPhotoForNew 是 VM 字段（不是 StateFlow），手动
    //     用 photoTick 触发重组。
    var photoTick by remember { mutableStateOf(0) }
    @Suppress("UNUSED_EXPRESSION") photoTick
    val currentPhoto = if (isAdd) vm.pendingPhotoForNew else initial?.heroPhotoPath

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            vm.pickHeroPhoto(initial?.id, uri) { photoTick++ }
        }
    }

    // 自定义可挑全套插画；内建仅展示自己的默认线描（用户唯一能"恢复"的目标）。
    // Cycle 0031：渲染同线描的 HeroVector 折成一项 — picker 不会再出现重复
    // 小圆（用户原话："3/4、7/8、10/12 重复"）。
    val vectorOptions: List<HeroVector> = remember(initial?.id, isBuiltIn) {
        when {
            isBuiltIn -> listOfNotNull(initial?.heroVector?.canonical())
            isAdd -> uniqueHeroVectors
            else -> heroVectorOptionsForId(initial!!.id)
        }
    }

    val canSave = when {
        isBuiltIn -> true                 // 内建始终能存（什么都不改也无害）
        else -> nameZh.isNotBlank()       // 自定义：只要有中文名
    }

    fun commitSave() {
        when {
            isBuiltIn -> {
                if (hidden != (initial?.hidden == true)) vm.setHidden(initial!!.id, hidden)
                onBack()
            }
            isAdd -> {
                // 自定义新建：有照片走 addCustomWithPhoto；没图就用当前选的 heroVector。
                if (currentPhoto != null) {
                    vm.addCustomWithPhoto(nameZh, nameEn, currentPhoto) { onBack() }
                } else {
                    vm.addCustom(nameZh, nameEn, heroVector) { onBack() }
                }
            }
            else -> {
                vm.saveCustom(initial!!.id, nameZh, nameEn, heroVector)
                if (hidden != initial.hidden) vm.setHidden(initial.id, hidden)
                onBack()
            }
        }
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
                                .clickable(enabled = canSave) { commitSave() }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            if (isBuiltIn) {
                item {
                    Text(
                        text = "内建分类的名字不可改；插画用内建默认，也可以从相册挑一张代表图覆盖。",
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
                        text = "填中文名 → 点头像选一张代表图，或从下方挑一张线描插画。",
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

            // Avatar picker — 同物品 Edit 页同款交互
            item {
                Spacer(Modifier.height(6.dp))
                HeroAvatarPicker(
                    categoryId = initial?.id ?: "category-new",
                    palette = DefaultPalette,
                    options = vectorOptions,
                    selected = heroVector.canonical(),
                    onSelect = { v ->
                        heroVector = v
                        // 选了线描就清掉照片，让头像回归插画
                        if (currentPhoto != null) {
                            vm.clearHeroPhoto(initial?.id)
                            photoTick++
                        }
                        // 自定义编辑模式：vector 改动立刻写 DB（跟 photo 一样 eager），
                        // 否则用户改完不点 [保存] 也会丢。
                        if (!isBuiltIn && !isAdd) {
                            vm.saveHeroVectorOnly(initial!!.id, v)
                        }
                    },
                    photoOptions = listOfNotNull(currentPhoto),
                    selectedPhoto = currentPhoto,
                    onSelectPhoto = { /* 只有一张，点击 == 已选，no-op */ },
                    onTakePhoto = null,
                    onPickPhotos = {
                        pickPhoto.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    },
                    onRemovePhoto = {
                        vm.clearHeroPhoto(initial?.id)
                        photoTick++
                    },
                )
                Spacer(Modifier.height(18.dp))
            }

            // 中文 / 英文名
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
                    vm.deleteCustom(initial.id)
                    onBack()
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

private val DefaultPalette = listOf("#0e0e0e", "#a47836", "#e8e2d4", "#5a5a5a")

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
