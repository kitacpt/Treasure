package com.treasure.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.treasure.theme.LocalTreasureColors

/**
 * Shared header for editing surfaces (manual entry & detail edit), so
 * the two pages line up visually with Portal/Grid: a thin utility bar
 * with leading + trailing slots, the page title in Cormorant titleLarge,
 * and a mono small-caps caption below.
 */
@Composable
fun EditPageHeader(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.CenterStart) { leading() }
            Spacer(Modifier.weight(1f))
            Box(contentAlignment = Alignment.CenterEnd) { trailing() }
        }
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = colors.ink,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = subtitle,
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
