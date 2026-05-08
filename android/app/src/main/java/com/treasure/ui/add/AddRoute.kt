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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.Category
import com.treasure.theme.LocalTreasureColors

private enum class AddMode { Chat, Preview }

/**
 * Outer Add page — chat-first per the cycle 0007 design (see
 * `prototype/add-page-v2/HANDOFF.md`). Manual-entry stays in CategoryForm
 * (cycle 0006), reached via the small "手动" button in the chat header.
 */
@Composable
fun AddRoute(
    onSaved: (String) -> Unit,
    onGoSettings: () -> Unit,
    vm: AddViewModel = viewModel(factory = AddViewModel.Factory),
) {
    val colors = LocalTreasureColors.current
    val state by vm.state.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(AddMode.Chat) }
    var voiceOn by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    var manualPickerOpen by remember { mutableStateOf(false) }
    var manualSession by remember { mutableStateOf<CategoryTemplate?>(null) }

    // Re-read AI key state when this screen is recomposed (user might have
    // come back from Settings).
    LaunchedEffect(Unit) { vm.refreshAiAvailability() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .statusBarsPadding(),
    ) {
        when (mode) {
            AddMode.Chat -> AddChat(
                state = state,
                conversations = vm.recentConversations,
                historyOpen = historyOpen,
                onToggleHistory = { historyOpen = !historyOpen },
                onOpenManual = { manualPickerOpen = true },
                onNewChat = { vm.newConversation() },
                onSendText = vm::sendText,
                onSendPhoto = vm::sendPhoto,
                onStartVoice = { voiceOn = true },
                onOpenDraft = { mode = AddMode.Preview },
                onGoSettings = onGoSettings,
            )
            AddMode.Preview -> AddPreview(
                draft = state.draft,
                onBack = { mode = AddMode.Chat },
                onUpdateField = vm::updateDraftField,
                onConfirm = {
                    vm.commitDraft { id ->
                        // After save, drop back into a fresh chat so the
                        // user can keep adding without going back through
                        // the control island.
                        vm.newConversation()
                        mode = AddMode.Chat
                        onSaved(id)
                    }
                },
            )
        }

        if (voiceOn) {
            VoiceOverlay(
                onDismiss = {
                    voiceOn = false
                    vm.sendVoiceStub()
                },
            )
        }
    }

    // Manual pop-up: 4 category chips → opens CategoryForm in a sheet
    if (manualPickerOpen) {
        ManualCategoryPicker(
            onCancel = { manualPickerOpen = false },
            onPick = { c ->
                manualPickerOpen = false
                manualSession = CategoryTemplates.forCategory(c)
            },
        )
    }
    manualSession?.let { template ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { manualSession = null },
            sheetState = sheetState,
            containerColor = colors.paper,
            contentColor = colors.ink,
        ) {
            CategoryForm(
                template = template,
                onCancel = { manualSession = null },
                onSaved = { id ->
                    manualSession = null
                    onSaved(id)
                },
            )
        }
    }
}

// ─── manual category picker overlay ──────────────────────────────────────

@Composable
private fun ManualCategoryPicker(
    onCancel: () -> Unit,
    onPick: (Category) -> Unit,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink.copy(alpha = 0.32f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.paper)
                .border(0.5.dp, colors.line)
                .padding(20.dp)
                .clickable(enabled = false) {},
        ) {
            Text(
                text = "手动录入 · 选品类",
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "TAP A ROOM TO BEGIN",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .border(0.5.dp, colors.line)
                            .clickable { onPick(c) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = c.nameZh,
                            color = colors.ink,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = c.nameEn.uppercase(),
                            color = colors.sub,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "取消",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = onCancel)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}
