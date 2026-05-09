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

/** Cycle 0023：聊天页里的图片点开后用全屏 viewer 看；只针对 chat 里发出的
 *  那些 UserPhoto，不走 callout 编辑（那是影集才有的功能）。 */
private data class ChatPhotoPreview(val photos: List<String>, val initialIndex: Int)

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
    val recents by vm.recentConversations.collectAsStateWithLifecycle()
    var mode by remember { mutableStateOf(AddMode.Chat) }
    var historyOpen by remember { mutableStateOf(false) }
    var manualPickerOpen by remember { mutableStateOf(false) }
    var manualSession by remember { mutableStateOf<CategoryTemplate?>(null) }
    var photoPreview by remember { mutableStateOf<ChatPhotoPreview?>(null) }

    // Re-read AI key state when this screen is recomposed (user might have
    // come back from Settings).
    LaunchedEffect(Unit) { vm.refreshAiAvailability() }

    // Cycle 0019：消费 shareIntake — 从外部 app 分享进来的文字一旦到，就把
    // 它当成用户在录入页发了一条文本消息派给 AI；消费后清空。
    val app = androidx.compose.ui.platform.LocalContext.current
        .applicationContext as com.treasure.TreasureApp
    LaunchedEffect(Unit) {
        app.shareIntake.collect { text ->
            if (!text.isNullOrBlank()) {
                vm.sendText(text)
                app.shareIntake.value = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.paper)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            when (mode) {
                AddMode.Chat -> AddChat(
                    state = state,
                    conversations = recents,
                    historyOpen = historyOpen,
                    onToggleHistory = { historyOpen = !historyOpen },
                    onOpenManual = { manualPickerOpen = true },
                    onNewChat = { vm.newConversation() },
                    onPickConversation = { id, title -> vm.openConversation(id, title) },
                    onRenameConversation = vm::renameConversation,
                    onDeleteConversation = vm::deleteConversation,
                    onSendText = vm::sendText,
                    onSendPhoto = vm::sendPhoto,
                    onOpenDraft = { mode = AddMode.Preview },
                    onGoSettings = onGoSettings,
                    onPreviewPhoto = { tapped ->
                        val all = state.messages
                            .filterIsInstance<AddMessage.UserPhoto>()
                            .map { it.uri.toString() }
                        val idx = all.indexOf(tapped.toString()).coerceAtLeast(0)
                        photoPreview = ChatPhotoPreview(all, idx)
                    },
                )
                AddMode.Preview -> AddPreview(
                    draft = state.draft,
                    onBack = { mode = AddMode.Chat },
                    onUpdateField = vm::updateDraftField,
                    onUpdateSpec = vm::updateDraftSpec,
                    onAddSpec = vm::addDraftSpec,
                    onRemoveSpec = vm::removeDraftSpec,
                    onConfirm = { status ->
                        vm.commitDraft(status = status) { id ->
                            vm.newConversation()
                            mode = AddMode.Chat
                            onSaved(id)
                        }
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

        // Cycle 0023：聊天里的图片点开 → 全屏 viewer。复用影集那边的
        // [FullscreenPhotoViewer]，但 callout 这里没存（chat 图就是临时 hint
        // 给 AI 用），所以传空 map + no-op 写回。多张图自动可横滑。
        photoPreview?.let { p ->
            com.treasure.ui.photo.FullscreenPhotoViewer(
                photos = p.photos,
                initialIndex = p.initialIndex,
                callouts = emptyMap(),
                onSetCallouts = { _, _ -> /* chat 图不存 callout */ },
                onClose = { photoPreview = null },
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
