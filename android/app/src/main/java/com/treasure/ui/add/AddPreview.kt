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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.Category
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.theme.LocalTreasureColors
import com.treasure.ui.components.EditPageHeader
import com.treasure.ui.components.HeroAvatarPicker
import com.treasure.ui.components.SectionDivider

/**
 * Cycle 0019：草稿页样式贴齐 Edit / 手动录入页 — EditPageHeader + 头像选择器
 * + SectionDivider + LabeledField。Header 左边 [取消] 右边 [确认收入图鉴]，
 * 下面分两段：基础（品牌/型号/昵称/一句话）+ 其他信息（颜色/购入/价格/渠道）。
 * 点任一行进入 inline edit；置信度小圆点跟在字段标签前面。
 */
@Composable
fun AddPreview(
    draft: ItemDraft?,
    onBack: () -> Unit,
    onUpdateField: (PreviewField, String) -> Unit,
    onConfirm: () -> Unit,
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
    val rows = previewRowsFor(draft)
    val basicFields = setOf(
        PreviewField.Brand, PreviewField.Model, PreviewField.Nickname, PreviewField.OneLiner,
    )
    val otherFields = setOf(
        PreviewField.Color, PreviewField.AcquiredDate, PreviewField.AcquiredPrice, PreviewField.AcquiredChannel,
    )
    val basicRows = rows.filter { it.field in basicFields }
    val otherRows = rows.filter { it.field in otherFields }
    var editing by remember { mutableStateOf<PreviewField?>(null) }

    val template = remember(draft.category) {
        CategoryTemplates.forCategory(
            Category.entries.firstOrNull { it.id == draft.category } ?: Category.TECH,
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
                EditPageHeader(
                    title = "Refine",
                    subtitle = template.category.nameZh,
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
                                .clickable(onClick = onConfirm)
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
                    options = remember(template.category) { heroVectorOptionsFor(template.category) },
                    selected = template.heroVector,
                    onSelect = { /* read-only in preview */ },
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                ConfidenceLegend(modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp))
            }
            item { SectionDivider("基础") }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    basicRows.forEach { row ->
                        DraftFieldRow(
                            row = row,
                            isEditing = editing == row.field,
                            onTap = { editing = row.field },
                            onCommit = { value ->
                                onUpdateField(row.field, value)
                                editing = null
                            },
                            onCancel = { editing = null },
                        )
                    }
                }
            }
            item { SectionDivider("其他信息") }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    otherRows.forEach { row ->
                        DraftFieldRow(
                            row = row,
                            isEditing = editing == row.field,
                            onTap = { editing = row.field },
                            onCommit = { value ->
                                onUpdateField(row.field, value)
                                editing = null
                            },
                            onCancel = { editing = null },
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun ConfidenceLegend(modifier: Modifier = Modifier) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegendItem(label = "确定", confidence = Confidence.High)
        LegendItem(label = "可能", confidence = Confidence.Medium)
        LegendItem(label = "需补充", confidence = Confidence.Low)
    }
}

@Composable
private fun LegendItem(label: String, confidence: Confidence) {
    val colors = LocalTreasureColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        ConfidenceDot(confidence = confidence)
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun ConfidenceDot(confidence: Confidence) {
    val colors = LocalTreasureColors.current
    val color = when (confidence) {
        Confidence.High -> colors.ink
        Confidence.Medium -> colors.terra
        Confidence.Low -> colors.sub.copy(alpha = 0.45f)
    }
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun DraftFieldRow(
    row: PreviewRow,
    isEditing: Boolean,
    onTap: () -> Unit,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    // text state 提到 row 级别，让 "确认" 按钮和 keyboard Done 都能拿到。
    var text by remember(row.field, isEditing) { mutableStateOf(row.value) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEditing, onClick = onTap),
    ) {
        ConfidenceDot(confidence = row.confidence)
        Spacer(Modifier.width(10.dp))
        Text(
            text = row.field.label,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(56.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                if (isEditing) {
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        cursorBrush = SolidColor(colors.terra),
                        textStyle = LocalTextStyle.current.copy(
                            color = colors.ink,
                            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onCommit(text) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = row.value.ifBlank { "（点击补充）" },
                        color = if (row.value.isBlank()) colors.sub.copy(alpha = 0.6f) else colors.ink,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = if (row.value.isBlank()) FontStyle.Italic else FontStyle.Normal,
                    )
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
        }
        if (isEditing) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "确认",
                color = colors.terra,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable { onCommit(text) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
            Text(
                text = "取消",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}
