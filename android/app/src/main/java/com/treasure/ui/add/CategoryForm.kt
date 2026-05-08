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
import androidx.compose.foundation.layout.padding
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
import com.treasure.core.domain.ItemStatus
import com.treasure.theme.LocalTreasureColors
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
    val specValues = remember {
        mutableStateListOf<String>().apply {
            // First 4 of AI's specs map to the template hero slots positionally
            for (i in 0..3) {
                add(initial?.specs?.getOrNull(i)?.value.orEmpty())
            }
        }
    }

    val canSave = brand.isNotBlank() && model.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        TopBar(
            title = if (initial != null) "AI 预填 · ${template.category.nameZh}"
                    else "新增 · ${template.category.nameZh}",
            canSave = canSave,
            onCancel = onCancel,
            onSave = {
                vm.saveManual(
                    template = template,
                    brand = brand,
                    model = model,
                    nickname = nickname,
                    acquired = acquired,
                    oneLiner = oneLiner,
                    status = status,
                    heroSpecValues = specValues.toList(),
                    onSaved = onSaved,
                )
            },
        )

        Section("基础")
        Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("品牌", brand,    { brand = it })
            LabeledField("型号", model,    { model = it })
            LabeledField("昵称", nickname, { nickname = it })
            LabeledField("简介", oneLiner, { oneLiner = it })
        }

        Section("时间")
        Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledField("购入", acquired, { acquired = it }, hint = "YYYY-MM-DD")
        }

        Section("状态")
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Chip("Owned",  status == ItemStatus.OWNED)  { status = ItemStatus.OWNED }
            Chip("Parted", status == ItemStatus.PARTED) { status = ItemStatus.PARTED }
            Chip("Rented", status == ItemStatus.RENTED) { status = ItemStatus.RENTED }
        }

        Section("关键参数 · ${template.category.nameZh} 模板")
        Column(modifier = Modifier.padding(horizontal = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            template.heroSpecLabels.forEachIndexed { i, label ->
                LabeledField(label, specValues[i], { specValues[i] = it })
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TopBar(
    title: String,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "取消",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onCancel)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = title,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "保存",
            color = if (canSave) colors.terra else colors.sub.copy(alpha = 0.4f),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clickable(enabled = canSave, onClick = onSave)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Section(label: String) {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(colors.line))
        }
    }
}

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
