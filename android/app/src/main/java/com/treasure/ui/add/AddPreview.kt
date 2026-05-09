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
    onBack: () -> Unit,
    onUpdateField: (PreviewField, String) -> Unit,
    onUpdateSpec: (Int, HeroSpec) -> Unit,
    onAddSpec: () -> Unit,
    onRemoveSpec: (Int) -> Unit,
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

    val category = remember(draft.category) {
        draft.category?.let { id ->
            Category.entries.firstOrNull { it.id == id }
        } ?: Category.TECH
    }
    val template = remember(category) { CategoryTemplates.forCategory(category) }
    var status by remember { mutableStateOf(ItemStatus.OWNED) }

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
                    subtitle = category.nameZh,
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
                                .clickable { onConfirm(status) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    },
                )
                Spacer(Modifier.height(6.dp))
            }
            item {
                HeroAvatarPicker(
                    category = template.category,
                    palette = template.palette,
                    options = remember(category) { heroVectorOptionsFor(category) },
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
                        InlineDropdown(
                            options = Category.entries,
                            selected = category,
                            label = { it.nameZh },
                            onSelect = { onUpdateField(PreviewField.Category, it.nameZh) },
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
            item { Spacer(Modifier.height(40.dp)) }
        }
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
