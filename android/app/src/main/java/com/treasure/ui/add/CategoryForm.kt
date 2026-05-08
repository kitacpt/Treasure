package com.treasure.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors
import java.time.LocalDate

@Composable
fun CategoryForm(
    template: CategoryTemplate,
    onCancel: () -> Unit,
    onSaved: (String) -> Unit,
    vm: AddViewModel = viewModel(factory = AddViewModel.Factory),
) {
    val colors = LocalTreasureColors.current
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var acquired by remember { mutableStateOf(LocalDate.now().toString()) }
    var oneLiner by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ItemStatus.OWNED) }
    val specValues = remember { mutableStateListOf("", "", "", "") }

    val canSave = brand.isNotBlank() && model.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 24.dp),
    ) {
        // Top row: cancel left, title center, save right
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                text = "新增 · ${template.category.nameZh}",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "保存",
                color = if (canSave) colors.terra else colors.sub.copy(alpha = 0.4f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clickable(enabled = canSave) {
                        vm.save(
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
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Hero preview using template
        Box(
            modifier = Modifier
                .fillMaxWidth(0.55f)
                .align(Alignment.CenterHorizontally)
                .aspectRatio(1f)
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            HeroIllustration(
                item = templatePreviewItem(template),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))

        SectionLabel("BASICS")
        FormField("品牌 brand", brand, { brand = it })
        FormField("型号 model", model, { model = it })
        FormField("昵称 nickname", nickname, { nickname = it })
        FormField("购入日期 (YYYY-MM-DD)", acquired, { acquired = it })
        FormField("一句话简介", oneLiner, { oneLiner = it })

        Spacer(Modifier.height(20.dp))
        SectionLabel("STATUS")
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FormChip("Owned",  status == ItemStatus.OWNED)  { status = ItemStatus.OWNED }
            FormChip("Parted", status == ItemStatus.PARTED) { status = ItemStatus.PARTED }
            FormChip("Rented", status == ItemStatus.RENTED) { status = ItemStatus.RENTED }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("HERO SPECS · ${template.category.nameZh}模板")
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            template.heroSpecLabels.forEachIndexed { i, label ->
                FormField(label, specValues[i], { specValues[i] = it })
            }
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
        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp),
    )
}

@Composable
private fun FormField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Column(modifier = modifier) {
        Text(
            text = placeholder,
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
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
    }
}

@Composable
private fun FormChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    val bg = if (selected) colors.ink else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) colors.paper else colors.ink
    val border = if (selected) colors.ink else colors.line
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(0.5.dp, border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, color = fg, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun templatePreviewItem(template: CategoryTemplate): Item =
    Item(
        id = "preview",
        category = template.category,
        brand = "",
        model = "",
        nickname = "",
        acquired = "",
        parted = null,
        status = ItemStatus.OWNED,
        palette = template.palette,
        oneLiner = "",
        heroVector = template.heroVector,
        heroSpecs = emptyList(),
        specs = emptyMap(),
        history = emptyList(),
        photos = emptyList(),
        createdAt = 0L,
        updatedAt = 0L,
    )
