package com.treasure.ui.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.theme.LocalTreasureColors

private enum class AddMode { Chat, Preview }

/** Cycle 0023：聊天页里的图片点开后用全屏 viewer 看；只针对 chat 里发出的
 *  那些 UserPhoto，不走 callout 编辑（那是影集才有的功能）。 */
private data class ChatPhotoPreview(val photos: List<String>, val initialIndex: Int)

/**
 * Outer Add page — chat-first per cycle 0007 design, redesigned cycle 0024
 * around "one conversation = one draft":
 *   - AI proposals show in chat as DraftCta cards with [采用] / [不要]
 *   - "采用" 把提案变成 confirmedDraft；后续 AI 跑都基于它做下一版
 *   - "手动" 按钮不再弹 CategoryForm 抽屉，而是进 Refine 页编辑 confirmedDraft
 *   - "确认收入" 仍在 Refine 页，把 confirmedDraft 落到 Room 当 Item
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
    var photoPreview by remember { mutableStateOf<ChatPhotoPreview?>(null) }

    // Re-read AI key state when this screen is recomposed (user might have
    // come back from Settings).
    LaunchedEffect(Unit) { vm.refreshAiAvailability() }

    // Cycle 0019：消费 shareIntake — 从外部 app 分享进来的文字一旦到，就把
    // 它当成用户在录入页发了一条文本消息派给 AI；消费后清空。
    val app = androidx.compose.ui.platform.LocalContext.current
        .applicationContext as com.treasure.TreasureApp
    // Cycle 0027：草稿页的品类 dropdown 要拉仓库里的分类列表
    val categories by app.categoryRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())
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
                    // Cycle 0024：手动按钮不再弹 CategoryForm；改成进 Refine
                    // 页面，直接在那里编辑会话的 confirmedDraft。没有 confirmed
                    // 就先建一个空的让 UI 有 anchor。
                    onOpenManual = {
                        vm.ensureDraftForManual()
                        mode = AddMode.Preview
                    },
                    onNewChat = { vm.newConversation() },
                    onPickConversation = { id, title -> vm.openConversation(id, title) },
                    onRenameConversation = vm::renameConversation,
                    onDeleteConversation = vm::deleteConversation,
                    onSendText = vm::sendText,
                    onSendPhoto = vm::sendPhoto,
                    onOpenDraft = {
                        vm.ensureDraftForManual()
                        mode = AddMode.Preview
                    },
                    onGoSettings = onGoSettings,
                    onPreviewPhoto = { tapped ->
                        val all = state.messages
                            .filterIsInstance<AddMessage.UserPhoto>()
                            .map { it.uri.toString() }
                        val idx = all.indexOf(tapped.toString()).coerceAtLeast(0)
                        photoPreview = ChatPhotoPreview(all, idx)
                    },
                    onAcceptProposal = vm::acceptProposal,
                    onRejectProposal = vm::rejectProposal,
                )
                AddMode.Preview -> AddPreview(
                    // Cycle 0024：草稿页编辑的是 confirmedDraft（已采用的状态）。
                    draft = state.confirmedDraft,
                    categories = categories,
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
