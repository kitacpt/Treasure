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
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.treasure.core.domain.HeroSpec
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
    // Cycle 0032 v2：drawer 状态从 remember 升到 rememberSaveable — 用户点
    // SAVED 行 → 跳 Detail → 返回时 AddRoute 重组也能复原；点 PENDING / MODIFIED
    // 进 Refine 也不丢，让回来时不用再点开一次。
    var itemDrawerOpen by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(false)
    }
    // Cycle 0033：跳 Detail / Refine 前先收 drawer（不然 nav 完成后 drawer 残
    // 影一闪），回来时 ON_RESUME 再弹起。两个 saveable 都活在跨 Activity 重
    // 建之后，足够覆盖 Detail 返回 / 锁屏唤醒等场景。
    var reopenDrawerOnResume by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(false)
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && reopenDrawerOnResume) {
                itemDrawerOpen = true
                reopenDrawerOnResume = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var photoPreview by remember { mutableStateOf<ChatPhotoPreview?>(null) }
    // Cycle 0033：刚选 / 拍出的照片先送 CropScreen 让用户裁一刀，确定后才落
    // 到 draft.photos。null = 没有挂起的裁剪。
    // Cycle 0033 v2：state 升到 rememberSaveable + Uri 用 String 保存，picker
    // 引发的 ON_PAUSE/ON_RESUME 也不丢；hoist 出 `when (mode)` 避免 launcher
    // 注册点不稳。
    var cropSourceStr by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val cropSource = cropSourceStr?.let { android.net.Uri.parse(it) }
    val cropScope = androidx.compose.runtime.rememberCoroutineScope()
    // Cycle 0033 v2：picker launcher hoist 到 AddRoute 顶部 — 之前嵌在
    // `when (mode) AddMode.Preview -> else { ... }` 里，mode 切换会让 launcher
    // 重新注册，配合 PickVisualMedia 的 result 派发偶尔会拿不到回调。这里挂
    // 在最外层只随 conversationId / lifecycle 重组，更稳定。
    val pickPhotoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) cropSourceStr = uri.toString()
    }

    // Cycle 0034 v2：录音入口 — 长按 Composer 麦克风进 RecordingOverlay。
    // recorder 是 nullable，非 null 时显示 overlay；start()/stop()/cancel()
    // 都在事件回调里就近做。permission 走 RECORD_AUDIO runtime 申请，
    // 拒绝直接收回 recorder。
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var recorder by remember { mutableStateOf<com.treasure.audio.VoiceRecorder?>(null) }
    val micPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val convoId = vm.state.value.conversationId.ifBlank { return@rememberLauncherForActivityResult }
            val dest = com.treasure.audio.VoiceRecorder.newFile(ctx, convoId)
            val r = com.treasure.audio.VoiceRecorder(ctx, dest)
            runCatching { r.start() }
                .onSuccess { recorder = r }
                .onFailure { /* 启动失败放弃 */ }
        }
    }
    fun startVoice() {
        if (recorder != null) return
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            micPermLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }
        val convoId = vm.state.value.conversationId.ifBlank { return }
        val dest = com.treasure.audio.VoiceRecorder.newFile(ctx, convoId)
        val r = com.treasure.audio.VoiceRecorder(ctx, dest)
        runCatching { r.start() }.onSuccess { recorder = r }
    }
    // Cycle 0031：用户点 DraftCta 卡片进 proposal-preview。proposalCta 非 null
    // 时 AddPreview 渲染 cta.draft（用户可微改），trailing 改成 [采用]。
    var proposalCta by remember { mutableStateOf<AddMessage.DraftCta?>(null) }
    // proposalCta 模式下的本地可编辑 draft（独立于 vm.confirmedDraft，不污染）。
    var proposalDraft by remember { mutableStateOf<com.treasure.core.ai.ItemDraft?>(null) }

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
    // Cycle 0031 redesign：drawer 里 SAVED 行要展示真物品（title / 插画），
    // picker + 按钮也要拉全量物品列表。
    val allItems by app.repository.items.collectAsStateWithLifecycle(initialValue = emptyList())
    val itemsById = remember(allItems) { allItems.associateBy { it.id } }
    LaunchedEffect(Unit) {
        app.shareIntake.collect { text ->
            if (!text.isNullOrBlank()) {
                vm.sendText(text)
                app.shareIntake.value = null
            }
        }
    }
    // Cycle 0033：图鉴编辑态 [编辑] 投递的 itemIds — 起一段新会话，把这些物
    // 品全部作为 SAVED 预填工作集。
    LaunchedEffect(Unit) {
        app.gridIntake.collect { ids ->
            if (!ids.isNullOrEmpty()) {
                vm.startConversationFromItems(ids)
                itemDrawerOpen = true // 引导用户看一眼工作集里的物品
                app.gridIntake.value = null
            }
        }
    }

    // Cycle 0031：录入页的局部返回栈优先级 — 自下而上：
    //   photoPreview 打开 → 关 viewer；history 抽屉打开 → 关抽屉；
    //   Preview 模式 → 退回 Chat。都不该把用户推到首页。
    //   全闭 / Chat 模式时让出来给 MainScreen 的全局 BackHandler（→ 回首页）。
    androidx.activity.compose.BackHandler(
        enabled = photoPreview != null || historyOpen || itemDrawerOpen || mode == AddMode.Preview,
    ) {
        when {
            photoPreview != null -> photoPreview = null
            // Cycle 0032 v2：Preview 期间 drawer 被 AddChat 一并隐藏（drawer
            // 在 AddChat 内）— 这时 back 必须先退 Preview，否则会去关一个不
            // 可见的 drawer，用户感知是"back 没反应"。
            mode == AddMode.Preview -> {
                proposalCta = null
                proposalDraft = null
                vm.clearEditing()
                mode = AddMode.Chat
            }
            historyOpen -> historyOpen = false
            itemDrawerOpen -> itemDrawerOpen = false
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
                    onNewChat = { vm.newConversation() },
                    onPickConversation = { id, title -> vm.openConversation(id, title) },
                    onRenameConversation = vm::renameConversation,
                    onDeleteConversation = vm::deleteConversation,
                    onSendText = vm::sendText,
                    onSendPhotos = vm::sendPhotos,
                    onStartVoice = { startVoice() },
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
                    // Cycle 0031：DraftCta 卡点击 → 切到 proposal-preview，
                    // AddPreview 用 cta.draft 当起手，用户可微改后 [采用]。
                    onPreviewProposal = { cta ->
                        proposalCta = cta
                        proposalDraft = cta.draft
                        mode = AddMode.Preview
                    },
                    onStopExtract = vm::stopExtract,
                    // Cycle 0031 redesign：工作集 drawer。
                    itemDrawerOpen = itemDrawerOpen,
                    onToggleItemDrawer = { itemDrawerOpen = !itemDrawerOpen },
                    workingItems = state.items,
                    itemsById = itemsById,
                    allItems = allItems,
                    onPickWorkingItem = { ci ->
                        // 状态胶囊点击的三态分发。Cycle 0033：跳出 AddChat 之
                        // 前先 explicit 收 drawer（避免 ModalBottomSheet 在
                        // Detail / Refine 浮上来之后才动画消失，造成"闪一下"），
                        // 然后置 reopenDrawerOnResume = true，AddRoute 重新可见
                        // 时 ON_RESUME 监听器再把 drawer 弹回来。
                        when (ci.status) {
                            com.treasure.core.repo.ConversationItemStatus.PENDING,
                            com.treasure.core.repo.ConversationItemStatus.MODIFIED -> {
                                vm.openWorkingItem(ci.id)
                                reopenDrawerOnResume = true
                                itemDrawerOpen = false
                                mode = AddMode.Preview
                            }
                            com.treasure.core.repo.ConversationItemStatus.SAVED -> {
                                val targetId = ci.itemRef
                                if (!targetId.isNullOrBlank()) {
                                    reopenDrawerOnResume = true
                                    itemDrawerOpen = false
                                    onSaved(targetId)
                                }
                            }
                        }
                    },
                    onAddExistingItem = vm::addExistingItem,
                    onRemoveWorkingItem = vm::removeWorkingItem,
                )
                AddMode.Preview -> {
                    val cta = proposalCta
                    if (cta != null) {
                        // Proposal preview mode：编辑本地 proposalDraft，跟
                        // vm.confirmedDraft 完全隔离。[采用] 才把 proposalDraft
                        // 升格成 confirmedDraft。
                        val d = proposalDraft ?: cta.draft
                        AddPreview(
                            draft = d,
                            categories = categories,
                            onBack = {
                                proposalCta = null
                                proposalDraft = null
                                mode = AddMode.Chat
                            },
                            onUpdateField = { f, v ->
                                proposalDraft = applyPreviewFieldEdit(d, f, v)
                            },
                            onUpdateSpec = { i, s ->
                                proposalDraft = d.copy(
                                    specs = d.specs.toMutableList().also { it[i] = s },
                                )
                            },
                            onAddSpec = {
                                proposalDraft = d.copy(specs = d.specs + HeroSpec("", ""))
                            },
                            onRemoveSpec = { i ->
                                proposalDraft = d.copy(
                                    specs = d.specs.toMutableList().also { it.removeAt(i) },
                                )
                            },
                            onMoveSpec = { from, to ->
                                val mut = d.specs.toMutableList()
                                if (from in mut.indices) {
                                    val moved = mut.removeAt(from)
                                    mut.add(to.coerceIn(0, mut.size), moved)
                                    proposalDraft = d.copy(specs = mut)
                                }
                            },
                            onUpdateHistory = { h ->
                                proposalDraft = d.copy(history = h.sortedBy { it.date })
                            },
                            onConfirm = { _ -> /* never fires in proposal mode */ },
                            proposalMode = true,
                            onAccept = {
                                vm.acceptProposalWithEdits(cta.id, proposalDraft ?: cta.draft)
                                proposalCta = null
                                proposalDraft = null
                                mode = AddMode.Chat
                            },
                        )
                    } else {
                        // Manual Refine：编辑 confirmedDraft，[确认收入] 入图鉴。
                        // Cycle 0033：Refine 页开始管理影集 — picker / camera
                        // 走 CropScreen 后落到 draft.photos。
                        // launcher 用的是 AddRoute 顶部的 pickPhotoLauncher（hoisted）。
                        AddPreview(
                            draft = state.confirmedDraft,
                            categories = categories,
                            onBack = {
                                vm.clearEditing()
                                mode = AddMode.Chat
                            },
                            onUpdateField = vm::updateDraftField,
                            onUpdateSpec = vm::updateDraftSpec,
                            onAddSpec = vm::addDraftSpec,
                            onRemoveSpec = vm::removeDraftSpec,
                            onMoveSpec = vm::moveDraftSpec,
                            onUpdateHistory = vm::setDraftHistory,
                            onConfirm = { status ->
                                vm.commitDraft(status = status) { id ->
                                    // Cycle 0031：commit 后不自动 new conversation，
                                    // 把会话封存留着，用户随时可以从历史抽屉看回。
                                    mode = AddMode.Chat
                                    onSaved(id)
                                }
                            },
                            onPickPhoto = {
                                pickPhotoLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                            // 拍照按钮先复用 picker（系统 picker 在多数厂商
                            // 上已含拍照入口）；专用相机 launcher 留给后续 cycle。
                            onTakePhoto = {
                                pickPhotoLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                            onRemovePhoto = vm::removeDraftPhoto,
                            onSelectAvatar = vm::setDraftAvatar,
                        )
                    }
                }
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

        // Cycle 0034 v2：录音全屏页 — 覆在最上面，长按麦克风进。
        recorder?.let { r ->
            RecordingOverlay(
                recorder = r,
                onCancel = { recorder = null },
                onSend = { path, dur ->
                    recorder = null
                    vm.sendVoiceAudio(path, dur)
                },
            )
        }

        // Cycle 0033：裁剪界面 — 覆在最上面。Confirm 把归一化 rect 传给 VM，
        // VM 异步落盘到 draft-photos/<convoId>/<uuid>.jpg。
        cropSource?.let { src ->
            com.treasure.ui.photo.CropScreen(
                source = src,
                onCancel = { cropSourceStr = null },
                onConfirm = { rect ->
                    cropScope.launch {
                        vm.persistDraftPhoto(src, rect)
                    }
                    cropSourceStr = null
                },
            )
        }
    }
}
