package com.treasure.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.treasure.theme.LocalTreasureColors

/**
 * Shared "✦ 标题 ✦"-style section divider used by both the manual entry
 * form and the detail edit screen, so visual rhythm is identical.
 *
 * Layout: a thin ink-grey line on each side with the label centered
 * between them. Label is small but ink-coloured so it pops against
 * surrounding sub-grey body copy without screaming.
 */
@Composable
fun SectionDivider(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier
                .weight(1f)
                .height(0.6.dp)
                .background(colors.line))
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                color = colors.ink,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = 2.4.sp,
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier
                .weight(1f)
                .height(0.6.dp)
                .background(colors.line))
        }
    }
}
