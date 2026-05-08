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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.Category
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors

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
    var editing by remember { mutableStateOf<PreviewField?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        LazyColumn(
            contentPadding = PaddingValues(top = 0.dp, bottom = 130.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { PreviewHeader(onBack = onBack) }
            item { Spacer(Modifier.height(16.dp)) }
            item { HeroCard(draft = draft) }
            item { Spacer(Modifier.height(20.dp)) }
            item { ConfidenceLegend() }
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.line))
            }
            items(rows.size) { idx ->
                val row = rows[idx]
                PreviewFieldRow(
                    row = row,
                    isEditing = editing == row.field,
                    onTap = { editing = row.field },
                    onCommit = { value ->
                        onUpdateField(row.field, value)
                        editing = null
                    },
                    onCancel = { editing = null },
                )
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(colors.line))
            }
            item { Spacer(Modifier.height(20.dp)) }
        }

        // Sticky footer above the floating control island
        Footer(
            onBack = onBack,
            onConfirm = onConfirm,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 78.dp),
        )
    }
}

private inline fun androidx.compose.foundation.lazy.LazyListScope.items(
    count: Int,
    crossinline content: @Composable (Int) -> Unit,
) {
    items(count = count) { idx -> content(idx) }
}

@Composable
private fun PreviewHeader(onBack: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "草稿预览",
                color = colors.ink,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "REVIEW · EDIT · CONFIRM",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "← 换一种",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
    val colors2 = LocalTreasureColors.current
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(0.5.dp)
        .background(colors2.line))
}

@Composable
private fun HeroCard(draft: ItemDraft) {
    val colors = LocalTreasureColors.current
    val template = remember(draft.category) {
        Category.entries.firstOrNull { it.id == draft.category }
            ?.let { CategoryTemplates.forCategory(it) }
            ?: CategoryTemplates.forCategory(Category.TECH)
    }
    val previewItem = remember(draft, template) {
        Item(
            id = "preview",
            category = template.category,
            brand = draft.brand, model = draft.model, nickname = "",
            acquired = "", parted = null,
            status = ItemStatus.OWNED,
            palette = template.palette,
            oneLiner = "",
            heroVector = template.heroVector,
            specs = emptyList(),
            history = emptyList(),
            photos = emptyList(),
            createdAt = 0L, updatedAt = 0L,
        )
    }
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 28.dp, end = 18.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .background(colors.paper)
                        .border(0.5.dp, colors.line)
                        .padding(8.dp),
                ) {
                    HeroIllustration(item = previewItem, modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val brandLine = listOf(draft.brand.uppercase(), readSpec(draft, "入手日期").take(4))
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    Text(
                        text = brandLine.ifBlank { "—" },
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = draft.model.ifBlank { "（未填型号）" },
                        color = colors.ink,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = draft.oneLiner.ifBlank { "（未填一句话）" },
                        color = colors.sub,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
            // Corner labels
            Text(
                text = "DRAFT №${draftNumberFor(draft)}",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 10.dp, top = 8.dp),
            )
            Text(
                text = "UNCONFIRMED",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun ConfidenceLegend() {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
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
private fun PreviewFieldRow(
    row: PreviewRow,
    isEditing: Boolean,
    onTap: () -> Unit,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isEditing) colors.card else Color.Transparent)
            .clickable(onClick = onTap)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ConfidenceDot(confidence = row.confidence)
        Spacer(Modifier.size(8.dp))
        Text(
            text = row.field.label,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .padding(end = 12.dp)
                .width(72.dp),
        )
        if (isEditing) {
            var draft by remember(row.field) { mutableStateOf(row.value) }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                cursorBrush = SolidColor(colors.terra),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.ink,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (row.field == PreviewField.AcquiredPrice) KeyboardType.Text else KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = false) {},
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = "确认",
                color = colors.terra,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable { onCommit(draft) }
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
        } else {
            Text(
                text = row.value.ifBlank { "（点击补充）" },
                color = if (row.value.isBlank()) colors.sub else colors.ink,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = if (row.value.isBlank()) FontStyle.Italic else FontStyle.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "✎",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun Footer(
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.paper)
            .border(0.5.dp, colors.line)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .border(0.5.dp, colors.line)
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = "继续修改",
                color = colors.ink,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.ink)
                .clickable(onClick = onConfirm)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✓",
                    color = colors.paper,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "确认收入图鉴",
                    color = colors.paper,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private fun readSpec(draft: ItemDraft, label: String): String =
    draft.specs.firstOrNull { it.label == label }?.value.orEmpty()

private fun draftNumberFor(draft: ItemDraft): String {
    val seed = (draft.brand + draft.model).hashCode()
    val n = (seed and 0x7fffffff) % 1000
    return n.toString().padStart(3, '0')
}
