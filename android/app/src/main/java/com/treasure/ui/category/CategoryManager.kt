@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.HeroVector
import com.treasure.theme.LocalTreasureColors

/**
 * Cycle 0026：分类管理抽屉。从 Grid 右上小红点点开。
 *
 * 三种模式（用 [mode] 切换）：
 *   - [Mode.List]：当前所有分类的列表，分割线分"显示中" / "已隐藏"两段；
 *     每行点击进编辑；底部 [+ 新增分类]
 *   - [Mode.EditCustom]：用户自定义分类的编辑页（中文 / 英文 / 插画 / 删
 *     除）
 *   - [Mode.EditBuiltIn]：内建分类只能改插画 + 显示/隐藏，不可删，不能改名
 *
 * Modal sheet 内部 mode 切换，避免再叠一层 sheet。
 */
@Composable
fun CategoryManager(
    onClose: () -> Unit,
    vm: CategoryManagerViewModel = viewModel(factory = CategoryManagerViewModel.Factory),
) {
    val colors = LocalTreasureColors.current
    val all by vm.all.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf<Mode>(Mode.List) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        when (val m = mode) {
            is Mode.List -> CategoryList(
                all = all,
                onEdit = { mode = Mode.Edit(it) },
                onAdd = { mode = Mode.Add },
                onToggleHidden = { id, hidden -> vm.setHidden(id, hidden) },
                onClose = onClose,
            )
            is Mode.Edit -> CategoryEditor(
                initial = m.info,
                onBack = { mode = Mode.List },
                onSaveBuiltIn = { heroVector, hidden ->
                    vm.saveHeroVectorOnly(m.info.id, heroVector)
                    if (hidden != m.info.hidden) vm.setHidden(m.info.id, hidden)
                    mode = Mode.List
                },
                onSaveCustom = { nameZh, nameEn, heroVector, hidden ->
                    vm.saveCustom(m.info.id, nameZh, nameEn, heroVector)
                    if (hidden != m.info.hidden) vm.setHidden(m.info.id, hidden)
                    mode = Mode.List
                },
                onDelete = {
                    vm.deleteCustom(m.info.id)
                    mode = Mode.List
                },
            )
            is Mode.Add -> CategoryEditor(
                initial = null,
                onBack = { mode = Mode.List },
                onSaveBuiltIn = { _, _ -> /* unreachable */ },
                onSaveCustom = { nameZh, nameEn, heroVector, _ ->
                    vm.addCustom(nameZh, nameEn, heroVector)
                    mode = Mode.List
                },
                onDelete = { /* unreachable when initial == null */ },
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

private sealed interface Mode {
    data object List : Mode
    data class Edit(val info: CategoryInfo) : Mode
    data object Add : Mode
}

// ─── List ─────────────────────────────────────────────────────────────────

@Composable
private fun CategoryList(
    all: List<CategoryInfo>,
    onEdit: (CategoryInfo) -> Unit,
    onAdd: () -> Unit,
    onToggleHidden: (id: String, hidden: Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val shown = all.filter { !it.hidden }
    val hidden = all.filter { it.hidden }
    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
        contentPadding = PaddingValues(horizontal = 22.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "分类管理",
                    color = colors.ink,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "+ 新增分类",
                    color = colors.terra,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onAdd)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "隐藏只是把分类从首页 / 图鉴入口里挪走，不会删数据。",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
            )
            Spacer(Modifier.height(16.dp))
            SectionLabel("显示中 · ${shown.size}")
        }
        items(shown.size) { idx ->
            CategoryRow(
                info = shown[idx],
                onEdit = { onEdit(shown[idx]) },
                onToggle = { onToggleHidden(shown[idx].id, true) },
                toggleLabel = "隐藏",
            )
        }
        if (shown.isEmpty()) {
            item {
                EmptyRow("没有显示中的分类 — 从下方滑回来一个")
            }
        }
        item {
            Spacer(Modifier.height(18.dp))
            DividerHidden()
            Spacer(Modifier.height(10.dp))
            SectionLabel("已隐藏 · ${hidden.size}")
        }
        items(hidden.size) { idx ->
            CategoryRow(
                info = hidden[idx],
                onEdit = { onEdit(hidden[idx]) },
                onToggle = { onToggleHidden(hidden[idx].id, false) },
                toggleLabel = "显示",
            )
        }
        if (hidden.isEmpty()) {
            item {
                EmptyRow("还没有隐藏的分类")
            }
        }
        item {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "完成",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .clickable(onClick = onClose)
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = LocalTreasureColors.current
    Text(
        text = text,
        color = colors.sub,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun DividerHidden() {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(colors.line),
    )
}

@Composable
private fun CategoryRow(
    info: CategoryInfo,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    toggleLabel: String,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = info.nameZh,
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (!info.isBuiltIn) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.terra.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "自定义",
                            color = colors.terra,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = info.nameEn.ifBlank { "—" },
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(
            text = toggleLabel,
            color = colors.sub,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "编辑 →",
            color = colors.terra,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onEdit)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun EmptyRow(text: String) {
    val colors = LocalTreasureColors.current
    Text(
        text = text,
        color = colors.sub.copy(alpha = 0.6f),
        style = MaterialTheme.typography.bodyMedium,
        fontStyle = FontStyle.Italic,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

// ─── Editor ───────────────────────────────────────────────────────────────

/** [initial] = null → 新增；non-null → 编辑既有项。 */
@Composable
private fun CategoryEditor(
    initial: CategoryInfo?,
    onBack: () -> Unit,
    onSaveBuiltIn: (heroVector: HeroVector, hidden: Boolean) -> Unit,
    onSaveCustom: (nameZh: String, nameEn: String, heroVector: HeroVector, hidden: Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val isBuiltIn = initial?.isBuiltIn == true
    val isAdd = initial == null
    var nameZh by remember { mutableStateOf(initial?.nameZh.orEmpty()) }
    var nameEn by remember { mutableStateOf(initial?.nameEn.orEmpty()) }
    var heroVector by remember { mutableStateOf(initial?.heroVector ?: HeroVector.GENERIC) }
    var hidden by remember { mutableStateOf(initial?.hidden == true) }
    var confirmingDelete by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "‹ 返回",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            val canSave = !isAdd || nameZh.isNotBlank()
            Text(
                text = if (isAdd) "新建" else "保存",
                color = if (canSave) colors.terra else colors.sub.copy(alpha = 0.45f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(enabled = canSave) {
                        if (isBuiltIn) onSaveBuiltIn(heroVector, hidden)
                        else onSaveCustom(nameZh, nameEn, heroVector, hidden)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isAdd) "新增分类" else "编辑 ${initial?.nameZh}",
            color = colors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        if (isBuiltIn) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "内建分类，名字不可改；只能换插画 / 改显示状态。",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
            )
        }
        Spacer(Modifier.height(18.dp))

        // Name fields
        FieldRow(label = "中文名") {
            EditorTextField(
                value = nameZh,
                onValueChange = { nameZh = it },
                enabled = !isBuiltIn,
                placeholder = "如 图书",
            )
        }
        Spacer(Modifier.height(10.dp))
        FieldRow(label = "英文名") {
            EditorTextField(
                value = nameEn,
                onValueChange = { nameEn = it },
                enabled = !isBuiltIn,
                placeholder = "如 Books（可选）",
            )
        }
        Spacer(Modifier.height(16.dp))

        // Hero vector picker
        Text(
            text = "插画",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.height(6.dp))
        HeroVectorRow(
            selected = heroVector,
            onSelect = { heroVector = it },
        )
        Spacer(Modifier.height(20.dp))

        // Visibility toggle
        if (!isAdd) {
            FieldRow(label = "显示") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PillChip("显示", !hidden) { hidden = false }
                    PillChip("隐藏", hidden) { hidden = true }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Delete (custom only; built-in can't be deleted)
        if (initial != null && !isBuiltIn) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .border(0.5.dp, colors.terra.copy(alpha = 0.55f))
                    .clickable { confirmingDelete = true }
                    .padding(vertical = 12.dp),
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

    if (confirmingDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("删除 ${initial.nameZh}？") },
            text = {
                Text(
                    text = "这只是删掉这个分类本身。已经收在这个分类下的物品不会被删，但它们会归到一个空 id（要重新指派分类）。",
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
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                enabled = enabled,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.terra),
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                    color = if (enabled) colors.ink else colors.sub.copy(alpha = 0.5f),
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.line),
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
    selected: HeroVector,
    onSelect: (HeroVector) -> Unit,
) {
    val colors = LocalTreasureColors.current
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HeroVector.entries.forEach { hv ->
            val isSel = hv == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSel) colors.ink else Color.Transparent)
                    .border(
                        0.5.dp,
                        if (isSel) colors.ink else colors.line,
                        RoundedCornerShape(999.dp),
                    )
                    .clickable { onSelect(hv) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = heroLabel(hv),
                    color = if (isSel) colors.paper else colors.ink,
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

