package com.treasure.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.treasure.theme.LocalTreasureColors
import com.treasure.theme.SpaceGrotesk

enum class IslandTab(val label: String) {
    Portal("门厅"),
    Grid("图鉴"),
    Add("录入"),
    Settings("设置"),
}

@Composable
fun ControlIsland(
    selected: IslandTab,
    onSelect: (IslandTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(colors.ink.copy(alpha = 0.85f))
            .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .padding(5.dp),
    ) {
        IslandTab.entries.forEach { tab ->
            IslandPill(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun IslandPill(
    tab: IslandTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val bgTarget = if (selected) colors.paper else Color.Transparent
    val fgTarget = if (selected) colors.ink else colors.paper
    val bg by animateColorAsState(bgTarget, label = "islandBg")
    val fg by animateColorAsState(fgTarget, label = "islandFg")
    Text(
        text = tab.label,
        color = fg,
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = androidx.compose.ui.unit.TextUnit(12.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
