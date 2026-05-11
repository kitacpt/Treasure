package com.treasure.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
        onBack = onBack,
        onSaveBuiltIn = { heroVector, hidden ->
            initial?.let { info ->
                vm.saveHeroVectorOnly(info.id, heroVector)
                if (hidden != info.hidden) vm.setHidden(info.id, hidden)
            }
            onBack()
        },
        onSaveCustom = { nameZh, nameEn, heroVector, hidden ->
            if (initial != null) {
                vm.saveCustom(initial.id, nameZh, nameEn, heroVector)
                if (hidden != initial.hidden) vm.setHidden(initial.id, hidden)
                onBack()
            } else {
                vm.addCustom(nameZh, nameEn, heroVector) { onBack() }
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
    onBack: () -> Unit,
    onSaveBuiltIn: (heroVector: HeroVector, hidden: Boolean) -> Unit,
    onSaveCustom: (nameZh: String, nameEn: String, heroVector: HeroVector, hidden: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val isBuiltIn = initial?.isBuiltIn == true
    val isAdd = initial == null
    var nameZh by remember(initial) { mutableStateOf(initial?.nameZh.orEmpty()) }
    var nameEn by remember(initial) { mutableStateOf(initial?.nameEn.orEmpty()) }
    var heroVector by remember(initial) {
        mutableStateOf<HeroVector?>(
            when {
                isAdd -> null
                else -> initial?.heroVector
            },
        )
    }
    var hidden by remember(initial) { mutableStateOf(initial?.hidden == true) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val canSave = (isBuiltIn || nameZh.isNotBlank()) && heroVector != null

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
                                    val hv = heroVector ?: return@clickable
                                    if (isBuiltIn) onSaveBuiltIn(hv, hidden)
                                    else onSaveCustom(nameZh, nameEn, hv, hidden)
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
                        text = "内建分类的名字和插画不可改；只能切显示状态。",
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
                        text = "新建必须填中文名 + 选一张插画。",
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

            // 头像
            item {
                Spacer(Modifier.height(6.dp))
                AvatarHero(heroVector = heroVector, placeholderEmpty = isAdd && heroVector == null)
                Spacer(Modifier.height(10.dp))
            }

            // 插画 picker
            item {
                Column(modifier = Modifier.padding(horizontal = 22.dp)) {
                    Text(
                        text = if (isBuiltIn) "插画（内建已固定）" else "插画 · 必选",
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    HeroVectorRow(
                        selected = heroVector,
                        enabled = !isBuiltIn,
                        onSelect = { heroVector = it },
                    )
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

@Composable
private fun AvatarHero(heroVector: HeroVector?, placeholderEmpty: Boolean) {
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
            if (heroVector == null) {
                Text(
                    text = if (placeholderEmpty) "+ 选张插画" else "—",
                    color = colors.sub.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            } else {
                val stub = remember(heroVector) {
                    com.treasure.core.domain.Item(
                        id = "preview",
                        category = "preview",
                        brand = "", model = "", nickname = "", acquired = "", parted = null,
                        status = com.treasure.core.domain.ItemStatus.OWNED,
                        palette = listOf("#0e0e0e", "#a47836", "#e8e2d4", "#5a5a5a"),
                        oneLiner = "",
                        heroVector = heroVector,
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

@Composable
private fun HeroVectorRow(
    selected: HeroVector?,
    enabled: Boolean,
    onSelect: (HeroVector) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HeroVector.entries.forEach { hv ->
            val isSel = hv == selected
            val rowAlpha = if (enabled) 1f else 0.55f
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        (if (isSel) colors.ink else Color.Transparent)
                            .copy(alpha = if (isSel) rowAlpha else 1f),
                    )
                    .border(
                        0.5.dp,
                        if (isSel) colors.ink.copy(alpha = rowAlpha) else colors.line,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable(enabled = enabled) { onSelect(hv) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = heroLabel(hv),
                    color = (if (isSel) colors.paper else colors.ink).copy(alpha = rowAlpha),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun heroLabel(hv: HeroVector): String = when (hv) {
    HeroVector.RACKET -> "球拍"
    HeroVector.SHOES -> "鞋"
    HeroVector.CAMERA_DSLR -> "单反"
    HeroVector.CAMERA_RANGEFINDER -> "旁轴"
    HeroVector.LENS_PRIME -> "镜头"
    HeroVector.TRIPOD -> "三脚架"
    HeroVector.CAR_SEDAN -> "轿车"
    HeroVector.CAR_SUV -> "SUV"
    HeroVector.LAPTOP -> "笔记本"
    HeroVector.TABLET -> "平板"
    HeroVector.EARBUDS -> "耳机"
    HeroVector.KINDLE -> "电纸书"
    HeroVector.WATCH -> "手表"
    HeroVector.ESPRESSO_MACHINE -> "意式机"
    HeroVector.COFFEE_GRINDER -> "磨豆机"
    HeroVector.COFFEE_BEAN -> "咖啡豆"
    HeroVector.WINE_BOTTLE -> "酒瓶"
    HeroVector.COCKTAIL_GLASS -> "鸡尾酒杯"
    HeroVector.GENERIC -> "通用"
}
