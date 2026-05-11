package com.treasure.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.components.EditPageHeader
import com.treasure.ui.components.HeroAvatarPicker
import com.treasure.ui.components.SectionDivider
import java.time.LocalDate

/**
 * Manual entry form. Layout deliberately mirrors EditScreen — same
 * Section header style, same LabeledField row, same Chip — so muscle
 * memory carries over.
 */
@Composable
fun CategoryForm(
    template: CategoryTemplate,
    initial: ItemDraft? = null,
    onCancel: () -> Unit,
    onSaved: (String) -> Unit,
    vm: AddViewModel = viewModel(factory = AddViewModel.Factory),
) {
    val colors = LocalTreasureColors.current
    var brand by remember { mutableStateOf(initial?.brand.orEmpty()) }
    var model by remember { mutableStateOf(initial?.model.orEmpty()) }
    var nickname by remember { mutableStateOf(initial?.nickname.orEmpty()) }
    var acquired by remember { mutableStateOf(LocalDate.now().toString()) }
    var oneLiner by remember { mutableStateOf(initial?.oneLiner.orEmpty()) }
    var status by remember { mutableStateOf(ItemStatus.OWNED) }
    var heroVector by remember(template.category) { mutableStateOf(template.heroVector) }
    val specValues = remember {
        mutableStateListOf<String>().apply {
            // First 4 of AI's specs map to the template hero slots positionally
            for (i in 0..3) {
                add(initial?.specs?.getOrNull(i)?.value.orEmpty())
            }
        }
    }

    val canSave = brand.isNotBlank() && model.isNotBlank()

    val pageTitle = if (initial != null) "Refine" else "New"
    val pageSubtitle = template.category.nameZh

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        EditPageHeader(
            title = pageTitle,
            subtitle = pageSubtitle,
            leading = {
                Text(
                    text = "取消",
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            },
            trailing = {
                Text(
                    text = "保存",
                    color = if (canSave) colors.terra else colors.sub.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clickable(enabled = canSave, onClick = {
                            vm.saveManual(
                                template = template,
                                brand = brand,
                                model = model,
                                nickname = nickname,
                                acquired = acquired,
                                oneLiner = oneLiner,
                                status = status,
                                heroSpecValues = specValues.toList(),
                                heroVector = heroVector,
                                onSaved = onSaved,
                            )
                        })
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            },
        )

        Spacer(Modifier.height(8.dp))
        HeroAvatarPicker(
            categoryId = template.category.id,
            palette = template.palette,
            options = remember(template.category) { heroVectorOptionsFor(template.category) },
            selected = heroVector,
            onSelect = { heroVector = it },
        )
        Spacer(Modifier.height(8.dp))

        // 跟 Edit 页一致的 Section 节奏：基础 / 标签 / 参数。手动录入还没物品，
        // 所以历史 / 实拍 / DANGER ZONE 不出现。
        Section("基础")
        Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("品牌", brand,    { brand = it }, hint = "如 Yonex / Sony / Apple")
            LabeledField("型号", model,    { model = it }, hint = "如 NF-700 · α7 IV")
            LabeledField("昵称", nickname, { nickname = it }, hint = "私密外号 · 可空")
            LabeledField("简介", oneLiner, { oneLiner = it }, hint = "一句话感受")
        }

        Section("标签")
        Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "状态",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(56.dp),
                )
                Spacer(Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("Owned",  status == ItemStatus.OWNED)  { status = ItemStatus.OWNED }
                    Chip("Parted", status == ItemStatus.PARTED) { status = ItemStatus.PARTED }
                    Chip("Rented", status == ItemStatus.RENTED) { status = ItemStatus.RENTED }
                }
            }
        }

        Section("参数 · ${template.category.nameZh}")
        Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            template.heroSpecLabels.forEachIndexed { i, label ->
                LabeledField(
                    label = label,
                    value = specValues[i],
                    onValueChange = { specValues[i] = it },
                    hint = template.heroSpecHints.getOrNull(i),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Section(label: String) = SectionDivider(label = label)

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String? = null,
) {
    val colors = LocalTreasureColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(56.dp),
        )
        Spacer(Modifier.width(12.dp))
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
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
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
