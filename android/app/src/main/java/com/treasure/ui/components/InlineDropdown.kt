package com.treasure.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.treasure.theme.LocalTreasureColors

/**
 * Shared single-select inline dropdown — 起到 chip-row 的功能，但
 * 当选项变多 / 标签变长（比如 “电子产品”）时不会换行。
 *
 * 折叠态：当前值 + ▾；点开内嵌列表，再点同一处或选中项关闭。
 */
@Composable
fun <T> InlineDropdown(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    glyph: ((T) -> String)? = null,
) {
    val colors = LocalTreasureColors.current
    var open by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            glyph?.invoke(selected)?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = colors.terra,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text = label(selected),
                color = colors.ink,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (open) "▴" else "▾",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (open) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.line))
            options.forEach { opt ->
                val isSelected = opt == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelect(opt)
                            open = false
                        }
                        .background(if (isSelected) colors.paper else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    glyph?.invoke(opt)?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = colors.terra, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = label(opt),
                        color = colors.ink,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.terra),
                        )
                    }
                }
            }
        }
    }
}
