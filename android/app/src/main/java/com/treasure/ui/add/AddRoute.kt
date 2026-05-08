@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.treasure.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.Category
import com.treasure.theme.LocalTreasureColors

/**
 * Outer Add page — intentionally left mostly blank pending the user's
 * redesign of the entry-point interaction. The four manual category
 * shortcuts at the bottom are temporary scaffolding so the underlying
 * CategoryForm + AI flow stay reachable for testing.
 */
private data class FormSession(
    val template: CategoryTemplate,
    val initial: ItemDraft? = null,
)

@Composable
fun AddRoute(
    onSaved: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") onGoSettings: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var session by remember { mutableStateOf<FormSession?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 28.dp, bottom = 100.dp)) {
            Header()

            Spacer(Modifier.weight(1f))

            // Empty middle — left intentionally blank, awaiting redesign
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "录入页交互重新设计中",
                    color = colors.sub.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
            }

            Spacer(Modifier.weight(1f))

            // Temporary fallback: 4 plain category chips so manual + AI flows
            // remain testable until the new outer interaction is wired up.
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
                Text(
                    text = "临时入口 · 重设计后会移除",
                    color = colors.sub.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Category.entries.forEach { c ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
                                .clickable {
                                    session = FormSession(
                                        template = CategoryTemplates.forCategory(c),
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = c.nameZh,
                                color = colors.ink,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    if (session != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { session = null },
            sheetState = sheetState,
            containerColor = colors.paper,
            contentColor = colors.ink,
        ) {
            CategoryForm(
                template = session!!.template,
                initial = session!!.initial,
                onCancel = { session = null },
                onSaved = { id ->
                    session = null
                    onSaved(id)
                },
            )
        }
    }
}

@Composable
private fun Header() {
    val colors = LocalTreasureColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp)) {
        Text(
            text = "Treasure",
            color = colors.ink,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "NEW ENTRY",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
