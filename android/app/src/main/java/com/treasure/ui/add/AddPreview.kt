package com.treasure.ui.add

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.ItemStatus
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.components.EditPageHeader
import com.treasure.ui.components.HeroAvatarPicker
import com.treasure.ui.components.InlineDropdown
import com.treasure.ui.components.SectionDivider
import com.treasure.ui.edit.AddRowButton
import com.treasure.ui.edit.Chip
import com.treasure.ui.edit.DeleteIcon
import com.treasure.ui.edit.FieldLabel
import com.treasure.ui.edit.InlineField
import com.treasure.ui.edit.LabeledField

/**
 * Cycle 0023：草稿页全面镜像 Edit 页。AI 把任何字段填上来都直接显示，没有
 * 再写死的"颜色 / 入手日期 / 入手价格 / 入手渠道" 4 行模板。
 *
 * 排版：
 *   - 头：取消 / Refine · 品类 / 确认收入
 *   - HeroAvatarPicker（read-only — 录入时还没照片，只展示模板插画）
 *   - 基础: brand · model · nickname · oneLiner（LabeledField）
 *   - 标签: status · category（同 Edit）
 *   - 参数: 渲染 draft.specs 全部行，可改 label / value / 删 / 加（不做拖动
 *           重排，留给 Edit 页用户细调，草稿页讲究快进快出）
 *
 * status 是这里的本地 state（AI 不填），其它字段都走 vm 的 update*。
 */
@Composable
fun AddPreview(
    draft: ItemDraft?,
    categories: List<com.treasure.core.domain.CategoryInfo>,
    onBack: () -> Unit,
    onUpdateField: (PreviewField, String) -> Unit,
    onUpdateSpec: (Int, HeroSpec) -> Unit,
    onAddSpec: () -> Unit,
    onRemoveSpec: (Int) -> Unit,
    onUpdateHistory: (List<com.treasure.core.domain.HistoryEvent>) -> Unit = {},
    onConfirm: (ItemStatus) -> Unit,
) {
    val colors = LocalTreasureColors.current
    if (draft == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.paper)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "尚无草稿 · 点击返回对话",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )
        }
        return
    }

    // Cycle 0027：草稿的 category 是 String id；模板（hero / palette / tagline）
    // 命中内建走 CategoryTemplates，自定义分类回退到 TECH 模板的视觉占位。
    val builtIn = remember(draft.category) {
        draft.category?.let { id -> Category.entries.firstOrNull { it.id == id } }
    }
    val template = remember(builtIn) {
        CategoryTemplates.forCategory(builtIn ?: Category.TECH)
    }
    val categoryDisplay = remember(draft.category, categories) {
        categories.firstOrNull { it.id == draft.category }?.nameZh
            ?: builtIn?.nameZh
            ?: "未选择"
    }
    var status by remember { mutableStateOf(ItemStatus.OWNED) }
    // Cycle 0025：确认收入加二次确认，避免误触把还没整理好的草稿落到图鉴里
    var confirming by remember { mutableStateOf(false) }

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
                    title = "Refine",
                    subtitle = categoryDisplay,
                    leading = {
                        Text(
                            text = "取消",
                            color = colors.sub,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .clickable(onClick = onBack)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    },
                    trailing = {
                        Text(
                            text = "确认收入",
                            color = colors.terra,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clickable { confirming = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    },
                )
                Spacer(Modifier.height(6.dp))
            }
            item {
                HeroAvatarPicker(
                    categoryId = template.category.id,
                    palette = template.palette,
                    options = remember(template.category) { heroVectorOptionsFor(template.category) },
                    selected = template.heroVector,
                    onSelect = { /* read-only — 草稿页不让换插画 */ },
                )
                Spacer(Modifier.height(8.dp))
            }

            item { SectionDivider("基础") }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LabeledField(
                        label = "品牌",
                        value = draft.brand,
                        onValueChange = { onUpdateField(PreviewField.Brand, it) },
                        hint = "如 Yonex / Sony / Gaggia",
                    )
                    LabeledField(
                        label = "型号",
                        value = draft.model,
                        onValueChange = { onUpdateField(PreviewField.Model, it) },
                        hint = "如 Astrox 99 Pro",
                    )
                    LabeledField(
                        label = "昵称",
                        value = draft.nickname,
                        onValueChange = { onUpdateField(PreviewField.Nickname, it) },
                        hint = "可留空",
                    )
                    LabeledField(
                        label = "简介",
                        value = draft.oneLiner,
                        onValueChange = { onUpdateField(PreviewField.OneLiner, it) },
                        hint = "一句话介绍",
                    )
                }
            }

            item { SectionDivider("标签") }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
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
                        // Cycle 0027：从仓库读分类（内建 + 自定义），过掉隐藏的；
                        // 选中 id 不在列表里时给伪行让 dropdown 仍能 echo 当前值。
                        val current = categories.firstOrNull { it.id == draft.category }
                            ?: com.treasure.core.domain.CategoryInfo(
                                id = draft.category ?: "tech",
                                nameZh = categoryDisplay,
                                nameEn = "",
                                heroVector = com.treasure.core.domain.HeroVector.GENERIC,
                                hidden = false, sortOrder = -1, isBuiltIn = false,
                            )
                        val visible = remember(categories, current) {
                            val list = categories.filter { !it.hidden }
                            if (list.any { it.id == current.id }) list else list + current
                        }
                        InlineDropdown(
                            options = visible,
                            selected = current,
                            label = { it.nameZh },
                            onSelect = { onUpdateField(PreviewField.Category, it.id) },
                        )
                    }
                }
            }

            item { SectionDivider("参数 · AI 填的字段") }
            item {
                DraftSpecs(
                    specs = draft.specs,
                    onChange = onUpdateSpec,
                    onDelete = onRemoveSpec,
                    onAdd = onAddSpec,
                )
            }

            // Cycle 0031：草稿页加历史栏，跟物品 Edit 页同一份 UI（HistorySection
            // 复用）。AI 不直接填 history；用户手动加，commit 时带进 Item。
            item { SectionDivider("历史") }
            item {
                com.treasure.ui.edit.HistorySection(
                    history = draft.history,
                    onUpdateHistory = onUpdateHistory,
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    if (confirming) {
        val title = listOf(draft.brand, draft.model)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "这件 ${categoryDisplay}" }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirming = false },
            title = { androidx.compose.material3.Text("收入 $title？") },
            text = {
                androidx.compose.material3.Text(
                    text = "确认后会作为一件 ${categoryDisplay} 入图鉴，再改就要从图鉴里点进去编辑。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirming = false
                    onConfirm(status)
                }) { androidx.compose.material3.Text("收入", color = colors.terra) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirming = false }) {
                    androidx.compose.material3.Text("取消")
                }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

private val LABEL_GAP = 12.dp

/**
 * 比 Edit 页的 ReorderableSpecs 简化：草稿页不做拖动重排（AI 已经按重要性
 * 排好了，用户要细调可以在 Detail / Edit 页继续）。只支持 inline 改 label /
 * value、删除单行、加新行。
 */
@Composable
private fun DraftSpecs(
    specs: List<HeroSpec>,
    onChange: (Int, HeroSpec) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        if (specs.isEmpty()) {
            Text(
                text = "AI 没填参数 · 点下面 + 自己加",
                color = colors.sub.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            specs.forEachIndexed { idx, spec ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    InlineField(
                        placeholder = "label",
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
                    DeleteIcon { onDelete(idx) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        AddRowButton(label = "+ 加一行参数", onClick = onAdd)
    }
}
