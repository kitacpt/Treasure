package com.treasure.ui.add

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.treasure.illust.HeroIllustration
import com.treasure.ui.components.HeroAvatar
import com.treasure.theme.Cormorant
import com.treasure.theme.LocalTreasureColors

private const val DEFAULT_TITLE = "New entry"

@Composable
fun AddChat(
    state: AddUiState,
    conversations: List<FakeConversation>,
    historyOpen: Boolean,
    onToggleHistory: () -> Unit,
    onNewChat: () -> Unit,
    onPickConversation: (id: String, title: String) -> Unit,
    onRenameConversation: (id: String, newTitle: String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSendText: (String) -> Unit,
    onSendPhotos: (List<android.net.Uri>, String) -> Unit,
    onStartVoice: () -> Unit,
    /** Cycle 0035 v2：press-and-hold 语音松手 — stop recorder + send。 */
    onPressVoiceCommit: () -> Unit,
    onRetryLastExtract: () -> Unit,
    onGoSettings: () -> Unit,
    onPreviewPhoto: (android.net.Uri) -> Unit,
    onPreviewPendingPhoto: (uris: List<String>, initialIndex: Int) -> Unit,
    onAcceptProposal: (String) -> Unit,
    onAcceptAndCommitProposal: (String) -> Unit,
    onRejectProposal: (String) -> Unit,
    onPreviewProposal: (AddMessage.DraftCta) -> Unit,
    onStopExtract: () -> Unit,
    // Cycle 0031 redesign：右上角 ManualPill 换成 list icon，点开 → 工作集
    // drawer。drawer 列表里每行有插画 + 标题 + 状态胶囊；plus 加入图鉴里已有
    // 物品；长按删除。
    itemDrawerOpen: Boolean,
    onToggleItemDrawer: () -> Unit,
    workingItems: List<com.treasure.core.repo.ConversationItem>,
    itemsById: Map<String, com.treasure.core.domain.Item>,
    allItems: List<com.treasure.core.domain.Item>,
    onPickWorkingItem: (com.treasure.core.repo.ConversationItem) -> Unit,
    onAddExistingItem: (String) -> Unit,
    onRemoveWorkingItem: (String) -> Unit,
    /** Cycle 0034 v5：drawer 右上一键录入 — commit 所有 PENDING / MODIFIED。 */
    onCommitAllPending: () -> Unit,
    /** Cycle 0035：录入页 chatbar 右下角"附件 / 模型"两个 chip 抽屉用的数据。 */
    aiProfiles: List<com.treasure.data.AiProfile>,
    selectedProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onPickFile: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    // Cycle 0035：Composer 实际渲染高度（含展开的 chatbar drawer）。下方
    // LazyColumn 的 bottom contentPadding 跟着它走 — 抽屉撑开时输入框被顶
    // 上去，最后一条消息也跟着被顶上去，不会被盖住。
    var composerHeightPx by remember { mutableStateOf(0) }
    // Cycle 0035 v2：press-and-hold 语音录制中 — true 时盖一层半屏毛玻璃在
    // 聊天列表上，"录音中… 松手发送"。
    var pressVoiceActive by remember { mutableStateOf(false) }

    // Cycle 0035 v5：IME 弹起 / 收起时把最后一条消息再拉到底 —— LazyColumn
    // 自己维护"距顶部"的滚动位置，viewport 一缩小，原本贴底的消息会被推
    // 出视野下边，看上去就是"聊天没跟着键盘上抬"。监听 IME 高度变化主动
    // animateScrollToItem 到末尾。
    val imeBottomDp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(imeBottomDp) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }
    // Cycle 0035：Composer 高度变化（抽屉展开 / 收起）时把最后一条消息再
    // 拉到底，避免被刚撑开的抽屉短暂盖住。
    LaunchedEffect(composerHeightPx) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Cycle 0034：可一次挑多张图作为同一轮 user-turn 的附件 — AI 在系统提示
    // 的 [ATTACHED PHOTOS] 里看到 N 张，分配给具体物品的影集。pendingPhotos
    // 暂存到 send 时一并发送；用户可再点一次 + 追加；每张缩略图有 ✕ 删除。
    var pendingPhotos by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { uris -> if (uris.isNotEmpty()) pendingPhotos = pendingPhotos + uris }

    val photoPermission = rememberPhotoPermissionName()
    val photoPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        // PhotoPicker works whether or not the runtime permission was
        // granted (the system Picker uses its own privileged process).
        // We still launch the picker afterward so the user gets through.
        pickPhoto.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly,
            ),
        )
    }

    fun launchPhotoFlow() {
        if (photoPermission == null ||
            ContextCompat.checkSelfPermission(context, photoPermission) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            pickPhoto.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly,
                ),
            )
        } else {
            photoPermLauncher.launch(photoPermission)
        }
    }

    // Cycle 0035 v3：架构改成 Header → LazyColumn(weight 1) → Composer 直接
    // 串在 Column 里，输入框上方就是 LazyColumn 的底；最后一条消息物理上
    // 不可能再被 Composer 盖住。键盘 / 抽屉撑高 Composer 时 LazyColumn 自动
    // 让位。不再用 contentPadding 估算 Composer 高度的 hack。
    //
    // Cycle 0035 v4：
    //  - 收紧 Composer 与底部胶囊的距离（100 → 72dp，跟胶囊顶部贴近一些）
    //  - 用 edge-to-edge + enableEdgeToEdge 之后系统不再自动 adjustResize，
    //    所以 IME 弹起得自己把内容上推 — bottom 取 max(IME, 72dp) 实现
    //    "无键盘 = 给胶囊留位，有键盘 = 顶到 IME 上方"。
    val bottomImeInset = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val imeOpen = bottomImeInset > 0.dp
    val composerBottomInset = if (imeOpen) bottomImeInset else 72.dp
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = composerBottomInset),
        ) {
            ChatHeader(
                title = state.conversationTitle,
                workingItemCount = workingItems.size,
                onTapTitle = onToggleHistory,
                onTapHistory = onToggleHistory,
                onTapItemList = onToggleItemDrawer,
            )

            if (!state.aiAvailable) {
                NotConfiguredBanner(onGoSettings = onGoSettings)
            }

            // Cycle 0035 v3：聊天列表 wrap 在 Box 里，press-voice 半屏遮罩
            // 直接铺这个 Box，物理上覆住 LazyColumn 区域，不挡下面的 Composer。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                androidx.compose.foundation.text.selection.SelectionContainer(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 22.dp,
                            end = 22.dp,
                            top = 18.dp,
                            bottom = 18.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.messages.size) { idx ->
                            val message = state.messages[idx]
                            MessageRow(
                                message = message,
                                onPreviewProposal = onPreviewProposal,
                                onPreviewPhoto = onPreviewPhoto,
                                onAcceptProposal = onAcceptProposal,
                                onAcceptAndCommitProposal = onAcceptAndCommitProposal,
                                onRejectProposal = onRejectProposal,
                            )
                        }
                        if (state.busy) {
                            item { TypingIndicator(startedAt = state.busyStartedAt) }
                        } else if (state.lastElapsedMs != null && state.messages.lastOrNull() !is AddMessage.User && state.messages.lastOrNull() !is AddMessage.UserPhoto) {
                            item { ElapsedHint(state.lastElapsedMs) }
                        }
                        if (state.retryAvailable && !state.busy) {
                            item { RetryRow(onRetry = onRetryLastExtract) }
                        }
                    }
                }

                // Cycle 0035 v3：press-hold 录音时半屏毛玻璃只盖聊天区，
                // Composer 仍在下方可见、可继续按住。
                if (pressVoiceActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.paper.copy(alpha = 0.88f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedSoundwave(color = colors.terra)
                            Spacer(Modifier.height(18.dp))
                            Text(
                                text = "录音中…",
                                color = colors.ink,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "松手发送",
                                color = colors.sub,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            // Cycle 0035 v3：Composer 直接接在 LazyColumn 下面 — LazyColumn
            // weight(1) 已经自动让出 Composer 高度，最后一条消息物理上不可能
            // 被盖住。.imePadding 让键盘弹起时 Composer 跟着上抬。
            Composer(
                input = input,
                onInputChange = { input = it },
                busy = state.busy,
                saved = state.saved,
                pendingPhotos = pendingPhotos,
                onRemovePending = { uri -> pendingPhotos = pendingPhotos - uri },
                onSend = {
                    if (pendingPhotos.isNotEmpty()) {
                        onSendPhotos(pendingPhotos, input)
                    } else {
                        onSendText(input)
                    }
                    input = ""
                    pendingPhotos = emptyList()
                },
                onStop = onStopExtract,
                onTakePhoto = ::launchPhotoFlow,
                onStartVoice = onStartVoice,
                onPreviewPending = { idx ->
                    onPreviewPendingPhoto(pendingPhotos.map { it.toString() }, idx)
                },
                aiProfiles = aiProfiles,
                selectedProfileId = selectedProfileId,
                onSelectProfile = onSelectProfile,
                onPickFile = onPickFile,
                allItems = allItems,
                onPressVoiceStart = { onStartVoice() },
                onPressVoiceSend = { onPressVoiceCommit() },
                onPressVoiceChange = { active -> pressVoiceActive = active },
                onAddExistingItem = onAddExistingItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .onSizeChanged { composerHeightPx = it.height },
            )
        }

        if (historyOpen) {
            HistoryDropdown(
                conversations = conversations,
                onDismiss = onToggleHistory,
                onPick = { c -> onPickConversation(c.id, c.title) }, // 抽屉里点切换不自动收
                onRename = onRenameConversation,
                onDelete = onDeleteConversation,
                onNewChat = onNewChat, // 同样：点新增也不自动收
            )
        }

        if (itemDrawerOpen) {
            ItemListDrawer(
                items = workingItems,
                itemsById = itemsById,
                allItems = allItems,
                onDismiss = onToggleItemDrawer,
                onPickItem = onPickWorkingItem,
                onAddExistingItem = onAddExistingItem,
                onRemoveItem = onRemoveWorkingItem,
                onCommitAllPending = onCommitAllPending,
            )
        }
    }
}


@Composable
private fun rememberPhotoPermissionName(): String? {
    return remember {
        when {
            // PickVisualMedia handles its own gating on Android 13+, but
            // many vendor galleries still gate access via this perm.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                Manifest.permission.READ_MEDIA_IMAGES
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                Manifest.permission.READ_EXTERNAL_STORAGE
            else -> null
        }
    }
}

// Local LazyListScope.items helper — alias to use lambdas with index
private inline fun androidx.compose.foundation.lazy.LazyListScope.items(
    count: Int,
    crossinline content: @Composable (Int) -> Unit,
) {
    items(count = count) { idx -> content(idx) }
}

// ─── header ────────────────────────────────────────────────────────────

@Composable
private fun ChatHeader(
    title: String,
    workingItemCount: Int,
    onTapTitle: () -> Unit,
    onTapHistory: () -> Unit,
    onTapItemList: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 14.dp)
            .border(0.dp, Color.Transparent),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Record",
                color = colors.ink,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            // Cycle 0018：副标永远 = 当前对话标题（已含 HH:MM 或 AI 改名后的
            // "Brand Model"）。点副标打开历史抽屉，方便跨对话切换。
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onTapTitle)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title.ifBlank { DEFAULT_TITLE },
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "▾",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        IconCircleButton(onClick = onTapHistory) { ClockGlyph(colors.ink) }
        // Cycle 0031 redesign：list icon → 工作集 drawer。右下角小角标 = 工作集行数。
        ItemListButton(count = workingItemCount, onClick = onTapItemList)
    }
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(0.5.dp)
        .background(colors.line))
}

@Composable
private fun IconCircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(0.5.dp, colors.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun ItemListButton(count: Int, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(0.5.dp, colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) { ItemListGlyph(colors.ink) }
        if (count > 0) {
            // 右上角小角标 — 跟主题相符的薄壳数字徽章
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(colors.terra)
                    .border(0.5.dp, colors.paper, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    color = colors.paper,
                    fontSize = 9.sp,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun ItemListGlyph(color: Color) {
    // 三短横 + 三圆点的列表 icon —— 一致于其他线条字形
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val sw = 1.1.dp.toPx()
        val dotR = 0.9.dp.toPx()
        listOf(0.22f, 0.50f, 0.78f).forEach { yFrac ->
            val y = w * yFrac
            drawCircle(color, radius = dotR, center = Offset(w * 0.18f, y))
            drawLine(color, Offset(w * 0.35f, y), Offset(w * 0.86f, y), strokeWidth = sw)
        }
    }
}

@Composable
private fun ManualPill(label: String, saved: Boolean, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    val fg = if (saved) colors.terra else colors.ink
    val border = if (saved) colors.terra.copy(alpha = 0.55f) else colors.line
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (saved) colors.terra.copy(alpha = 0.10f) else Color.Transparent)
            .border(0.5.dp, border, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ManualGlyph(fg)
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ClockGlyph(color: Color) {
    Canvas(modifier = Modifier.size(13.dp)) {
        val w = size.width
        val cx = w / 2f
        val cy = w / 2f
        val r = w * 0.42f
        drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(width = 1.2.dp.toPx()))
        drawLine(color, Offset(cx, cy), Offset(cx, cy - r * 0.55f), strokeWidth = 1.2.dp.toPx())
        drawLine(color, Offset(cx, cy), Offset(cx + r * 0.45f, cy), strokeWidth = 1.2.dp.toPx())
    }
}

@Composable
private fun PlusGlyph(color: Color) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val w = size.width
        drawLine(color, Offset(w / 2f, w * 0.18f), Offset(w / 2f, w * 0.82f), strokeWidth = 1.4.dp.toPx())
        drawLine(color, Offset(w * 0.18f, w / 2f), Offset(w * 0.82f, w / 2f), strokeWidth = 1.4.dp.toPx())
    }
}

@Composable
private fun ManualGlyph(color: Color) {
    Canvas(modifier = Modifier.size(11.dp)) {
        val w = size.width
        val sw = 1.2.dp.toPx()
        drawLine(color, Offset(w * 0.16f, w * 0.25f), Offset(w * 0.80f, w * 0.25f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.16f, w * 0.50f), Offset(w * 0.84f, w * 0.50f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.16f, w * 0.75f), Offset(w * 0.70f, w * 0.75f), strokeWidth = sw)
    }
}

// ─── history dropdown ─────────────────────────────────────────────────

/**
 * History menu — a softer card with rounded corners, gentle shadow, and a
 * tiny ornament header. The previous version was a sharp 2dp-corner panel
 * that felt too clinical against the museum-paper rest of the app.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDropdown(
    conversations: List<FakeConversation>,
    onDismiss: () -> Unit,
    onPick: (FakeConversation) -> Unit,
    onRename: (id: String, newTitle: String) -> Unit,
    onDelete: (String) -> Unit,
    onNewChat: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var renaming by remember { mutableStateOf<FakeConversation?>(null) }
    var deleting by remember { mutableStateOf<FakeConversation?>(null) }

    // Cycle 0018：跟手动录入屏一样的 ModalBottomSheet — 从底部上滑，
    // 一致的拖把手 + 圆角。点 scrim / 滑下去关；点对话行 / 新对话不
    // 自动收，让用户连续切几段。
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            HistoryHeader()
            Spacer(Modifier.height(10.dp))
            HistoryDivider(colors.line)
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
            ) {
                if (conversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "（暂无历史）",
                                color = colors.sub,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                } else {
                    items(conversations.size) { idx ->
                        val c = conversations[idx]
                        Spacer(Modifier.height(4.dp))
                        HistoryRow(
                            conv = c,
                            onClick = { onPick(c) }, // 不自动 onDismiss
                            onRename = { renaming = c },
                            onDelete = { deleting = c },
                        )
                        Spacer(Modifier.height(4.dp))
                        HistoryDivider(colors.line)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            NewChatRow(onClick = onNewChat) // 不自动 onDismiss
        }
    }

    renaming?.let { c ->
        RenameConversationDialog(
            initial = c.title,
            onCancel = { renaming = null },
            onConfirm = { newName ->
                onRename(c.id, newName)
                renaming = null
            },
        )
    }
    deleting?.let { c ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除这段对话？") },
            text = {
                Text(
                    text = "“${c.title}” 的所有消息都会被清掉，无法恢复。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onDelete(c.id)
                    deleting = null
                }) { Text("删除", color = colors.terra) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleting = null }) {
                    Text("取消")
                }
            },
            containerColor = colors.paper,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

@Composable
private fun RenameConversationDialog(
    initial: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var text by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("改对话名") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.card)
                    .border(0.5.dp, colors.line)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "比如 “Yonex VTZF2”",
                        color = colors.sub.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    cursorBrush = SolidColor(colors.terra),
                    textStyle = LocalTextStyle.current.copy(
                        color = colors.ink,
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text("保存", color = colors.terra) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onCancel) { Text("取消") }
        },
        containerColor = colors.paper,
        titleContentColor = colors.ink,
        textContentColor = colors.sub,
    )
}

@Composable
private fun HistoryHeader() {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("✦", color = colors.sub, style = MaterialTheme.typography.labelSmall)
        Text(
            text = "RECENT CONVERSATIONS",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
        )
        Box(modifier = Modifier
            .weight(1f)
            .height(0.5.dp)
            .background(colors.line))
        Text("✦", color = colors.sub, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HistoryDivider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(0.5.dp)
            .background(color),
    )
}

@Composable
private fun HistoryRow(
    conv: FakeConversation,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (conv.current) colors.card else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cycle 0018：title 在创建时就含 HH:MM 后缀（"New entry · 15:32"），
                // 这里直接展示 — 与 ChatHeader 副标保持一字不差。
                Text(
                    text = conv.title,
                    color = colors.ink,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (conv.current) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.terra),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = conv.date.replace("-", "·"),
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.width(6.dp))
        // Cycle 0031：current row 用 colors.ink 让 ✎ / ✕ 在 card bg 上更显眼，
        // 同时把按钮可点区扩到 36dp（之前 28dp 容易点到外面落到 row.onClick）。
        val glyphColor = if (conv.current) colors.ink else colors.sub
        IconGlyphButton(onClick = onRename) {
            Text("✎", color = glyphColor, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(2.dp))
        IconGlyphButton(onClick = onDelete) {
            Text("✕", color = glyphColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun IconGlyphButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun NewChatRow(onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusGlyph(colors.terra)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "新对话",
            color = colors.terra,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ─── messages ─────────────────────────────────────────────────────────

@Composable
private fun MessageRow(
    message: AddMessage,
    onPreviewProposal: (AddMessage.DraftCta) -> Unit,
    onPreviewPhoto: (android.net.Uri) -> Unit,
    onAcceptProposal: (String) -> Unit,
    onAcceptAndCommitProposal: (String) -> Unit,
    onRejectProposal: (String) -> Unit,
) {
    when (message) {
        is AddMessage.Assistant -> AssistantBubble(text = message.text)
        is AddMessage.User -> UserTextBubble(text = message.text)
        is AddMessage.UserPhoto -> UserPhotoBubble(
            uri = message.uri,
            onClick = { onPreviewPhoto(message.uri) },
        )
        is AddMessage.UserVoice -> UserVoiceBubble(
            text = message.text,
            duration = message.duration,
            audioPath = message.audioPath,
        )
        is AddMessage.DraftCta -> DraftCtaCard(
            message = message,
            onOpen = { onPreviewProposal(message) },
            onAccept = { onAcceptProposal(message.id) },
            onAcceptAndCommit = { onAcceptAndCommitProposal(message.id) },
            onReject = { onRejectProposal(message.id) },
        )
        is AddMessage.DraftConfirmed -> DraftConfirmedRow(text = "✓ 已采用 · ${message.fieldCount} 个字段")
        is AddMessage.Committed -> DraftConfirmedRow(text = "✦ 已收入图鉴")
        is AddMessage.SystemNote -> SystemNoteRow(text = message.text, tone = message.tone)
    }
}

@Composable
private fun DraftConfirmedRow(text: String) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = colors.terra,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
            ),
        )
    }
}

@Composable
private fun SystemNoteRow(text: String, tone: NoteTone) {
    val colors = LocalTreasureColors.current
    val accent = when (tone) {
        NoteTone.Working -> colors.sub
        NoteTone.Success -> Color(0xFF3E8E45)
        NoteTone.Warning -> Color(0xFFD89B23)
        NoteTone.Error -> Color(0xFFC5392E)
        NoteTone.Info -> colors.sub
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = accent,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Cormorant,
                fontStyle = FontStyle.Italic,
            ),
        )
    }
}

@Composable
private fun AssistantBubble(text: String) {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 4.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                color = colors.ink,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Cormorant,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                ),
            )
        }
    }
}

@Composable
private fun UserTextBubble(text: String) {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 4.dp, bottomStart = 14.dp))
                .background(colors.ink)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                color = colors.paper,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun UserPhotoBubble(uri: android.net.Uri, onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun UserVoiceBubble(text: String, duration: String, audioPath: String?) {
    val colors = LocalTreasureColors.current
    // Cycle 0034 v2：bubble 点击 toggle 播放本地 m4a；播放中波形给彩色提示。
    val player = com.treasure.audio.rememberVoicePlayer()
    val playing = audioPath != null && player.playingPath == audioPath
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 4.dp, bottomStart = 14.dp))
                .background(colors.ink)
                .clickable(enabled = audioPath != null) { audioPath?.let { player.toggle(it) } }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 播放/暂停 icon
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.paper.copy(alpha = if (playing) 1f else 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (playing) "❚❚" else "▶",
                        color = colors.ink,
                        fontSize = 10.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Waveform(
                    color = if (playing) colors.terra.copy(alpha = 0.9f) else colors.paper,
                    bars = listOf(6, 9, 12, 7, 11, 5, 10, 13, 8, 6, 11, 9),
                    modifier = Modifier.height(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = duration,
                    color = colors.paper.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            // Cycle 0034 v2：text 仅在老消息（旧 STT 路径）非空时显示；新音频
            // 流程是空字符串就不渲染这一行。
            if (text.isNotBlank()) {
                Text(
                    text = "\"$text\"",
                    color = colors.paper.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Cormorant,
                        fontStyle = FontStyle.Italic,
                        fontSize = 15.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DraftCtaCard(
    message: AddMessage.DraftCta,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onAcceptAndCommit: () -> Unit,
    onReject: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val template = remember(message.draft) {
        com.treasure.core.domain.Category.entries
            .firstOrNull { it.id == message.draft.category }
            ?.let { CategoryTemplates.forCategory(it) }
            ?: CategoryTemplates.forCategory(com.treasure.core.domain.Category.TECH)
    }
    // Cycle 0034 v3：采用前先用 AI 分配的"准头像"做预览 — 优先取一条标了
    // isAvatar=true 的 photo_assignment，没有就取第一条；用它的 sourceUri 当
    // 卡片上的小图。注意：这里还没真正落盘裁好，所以展示的是源图全幅；点
    // 采用后 applyAcceptedCta 会把按 crop 裁好的副本写进 draft.photos /
    // avatarPhotoPath，工作集 / 图鉴里看到的就是裁过的版。
    // Cycle 0034 v4：从 photo_assignments 里挑一条做卡片头像（标了 isAvatar
    // 的优先，没有就第一条），并把它的 crop rect 也带上 — HeroAvatar 会读
    // photoCrops[avatarPhotoPath] 应用裁剪，所以卡片直接显示"裁剪后的效果"。
    val previewAssignment = remember(message.photoAssignments) {
        message.photoAssignments.firstOrNull { it.isAvatar }
            ?: message.photoAssignments.firstOrNull()
    }
    val previewAvatar = previewAssignment?.sourceUri
    val previewCropMap = remember(previewAssignment) {
        if (previewAssignment != null && previewAvatar != null) {
            mapOf(
                previewAvatar to com.treasure.core.domain.PhotoCrop(
                    x = previewAssignment.cropX,
                    y = previewAssignment.cropY,
                    w = previewAssignment.cropW,
                    h = previewAssignment.cropH,
                ),
            )
        } else emptyMap()
    }
    val previewItem = remember(message.draft, template, previewAvatar, previewCropMap) {
        com.treasure.core.domain.Item(
            id = "preview",
            category = template.category.id,
            brand = message.draft.brand,
            model = message.draft.model,
            nickname = "",
            acquired = "",
            parted = null,
            status = com.treasure.core.domain.ItemStatus.OWNED,
            palette = template.palette,
            oneLiner = "",
            heroVector = template.heroVector,
            specs = emptyList(),
            history = emptyList(),
            photos = listOfNotNull(previewAvatar),
            avatarPhotoPath = previewAvatar,
            photoCrops = previewCropMap,
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
    val isPending = message.status == com.treasure.core.repo.DraftCtaStatus.Pending
    val isRejected = message.status == com.treasure.core.repo.DraftCtaStatus.Rejected
    val cardAlpha = if (isPending) 1f else 0.55f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(colors.card.copy(alpha = cardAlpha))
            .border(0.5.dp, colors.line),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.paper)
                    .border(0.5.dp, colors.line)
                    .padding(5.dp),
            ) {
                // Cycle 0034 v3：用 HeroAvatar，有 avatarPhotoPath 时显示真照片
                HeroAvatar(item = previewItem, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Cycle 0032 v2：tag 前缀加 "新增" / "修改" — 用户一眼看清这张
                // 卡是要新建一个物品还是改某个已有行。
                val kindPrefix = when (message.actionKind) {
                    com.treasure.core.repo.DraftCtaActionKind.Modify -> "修改 · "
                    com.treasure.core.repo.DraftCtaActionKind.Create -> ""
                }
                val tag = when (message.status) {
                    com.treasure.core.repo.DraftCtaStatus.Pending -> "${kindPrefix}AI 提案 · ${message.fieldCount} 字段"
                    com.treasure.core.repo.DraftCtaStatus.Accepted -> "${kindPrefix}已采用 · ${message.fieldCount} 字段"
                    com.treasure.core.repo.DraftCtaStatus.Rejected -> "${kindPrefix}已拒绝 · ${message.fieldCount} 字段"
                }
                Text(
                    text = tag,
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.height(2.dp))
                val title = listOf(message.draft.brand, message.draft.model)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "草稿" }
                Text(
                    text = if (isRejected) "✗ $title" else title,
                    color = colors.ink,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isRejected) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                )
                if (message.draft.oneLiner.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = message.draft.oneLiner,
                        color = colors.sub,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
            if (isPending) {
                Text(text = "→", color = colors.ink, style = MaterialTheme.typography.bodyLarge)
            }
        }
        // Cycle 0034 v8：卡片不再展示影集缩略图条 — 影集管理留给 Proposal
        // 预览页（点卡片进入），跟 Refine 页用同一套 HeroAvatarPicker。
        if (isPending) {
            // Cycle 0034 v7：按钮三态 — [不要] (灰) / [保存草稿] (terra 12% 底)
            // / [直接录入] (terra 实底，强调"一锤子录入")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "不要",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(onClick = onReject)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "保存草稿",
                    color = colors.terra,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.terra.copy(alpha = 0.12f))
                        .clickable(onClick = onAccept)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "直接录入",
                    color = colors.paper,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.terra)
                        .clickable(onClick = onAcceptAndCommit)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Cycle 0031：思考气泡 — 左侧旋转 spinner + "正在思考…" + 实时秒表。
 * spinner 用 [androidx.compose.foundation.Canvas] 手画一圈渐变描边 + infinite
 * 旋转 animation，避免 Material3 CircularProgressIndicator 默认调色板太重。
 * 秒表用 LaunchedEffect + delay 每 100ms 重算 elapsed。
 */
@Composable
private fun TypingIndicator(startedAt: Long?) {
    val colors = LocalTreasureColors.current
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "spin")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 900, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "rotation",
    )
    var elapsedMs by remember { mutableStateOf(0L) }
    androidx.compose.runtime.LaunchedEffect(startedAt) {
        val start = startedAt ?: System.currentTimeMillis()
        while (true) {
            elapsedMs = System.currentTimeMillis() - start
            kotlinx.coroutines.delay(100)
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 4.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val sw = 1.6.dp.toPx()
                val r = size.minDimension / 2f - sw / 2f
                rotate(rotation) {
                    drawArc(
                        color = colors.terra,
                        startAngle = 0f,
                        sweepAngle = 280f,
                        useCenter = false,
                        topLeft = Offset(sw / 2f, sw / 2f),
                        size = Size(r * 2, r * 2),
                        style = Stroke(width = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "正在思考…",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatElapsed(elapsedMs),
                color = colors.sub.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ElapsedHint(elapsedMs: Long) {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Text(
            text = "耗时 ${formatElapsed(elapsedMs)}",
            color = colors.sub.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Cycle 0034 v3：上轮 AI 调用失败 → 跟在错误信息后面挂个 "重试 ↻" 小胶囊。 */
@Composable
private fun RetryRow(onRetry: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(colors.terra.copy(alpha = 0.12f))
                .border(0.5.dp, colors.terra.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "↻", color = colors.terra, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            Text(text = "重试", color = colors.terra, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000.0
    return if (totalSec < 10) String.format("%.1f s", totalSec)
    else "${totalSec.toInt()} s"
}

@Composable
private fun Waveform(
    color: Color,
    bars: List<Int>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = (bars.size * 3).dp, height = 14.dp)) {
        val unit = 3.dp.toPx()
        val maxBar = bars.max().toFloat()
        bars.forEachIndexed { idx, h ->
            val barHeight = (h / maxBar) * size.height
            val left = idx * unit
            drawRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(left, size.height - barHeight),
                size = Size(width = 1.5.dp.toPx(), height = barHeight),
            )
        }
    }
}

// ─── composer (cycle 0035 redesign) ───────────────────────────────────

/**
 * Cycle 0035 — 新版 chatbar:
 *  - 两个 chip 在最上 (附件 / 模型)
 *  - 输入栏内部从左到右：mic / text / emoji；外侧右边独立圆形 send 键
 *  - 抽屉在 chip + input 下方，按需展开 0 → fit-content；整个 Column 锚在底部
 *    所以抽屉撑高时输入栏会被顶上去
 *  - 抽屉三种：附件 (3-grid 图/文件/物品) · 模型 (单选) · emoji (grid)
 */
private enum class ChatDrawer { None, Attach, Model, Emoji, ItemPicker }

private val EMOJI_PALETTE = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
    "😊", "😇", "🙂", "😉", "😌", "😍", "🥰", "😘",
    "😎", "🤩", "🥳", "😏", "😴", "🤔", "🤨", "🧐",
    "😱", "😡", "🥺", "😭", "😢", "🤯", "🥵", "🥶",
    "👍", "👎", "👌", "✌️", "🤝", "🙏", "👏", "🫶",
    "🔥", "✨", "⭐", "🌟", "💫", "💥", "❤️", "💔",
    "🎉", "🎊", "🎁", "🎂", "🍰", "☕", "🍺", "🍷",
    "📷", "🎵", "📚", "📝", "✅", "❌", "⚡", "🌈",
)

@Composable
private fun Composer(
    input: String,
    onInputChange: (String) -> Unit,
    busy: Boolean,
    saved: Boolean,
    pendingPhotos: List<android.net.Uri>,
    onRemovePending: (android.net.Uri) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onTakePhoto: () -> Unit,
    onStartVoice: () -> Unit = {},
    onPreviewPending: (Int) -> Unit = {},
    aiProfiles: List<com.treasure.data.AiProfile> = emptyList(),
    selectedProfileId: String? = null,
    onSelectProfile: (String) -> Unit = {},
    onPickFile: () -> Unit = {},
    allItems: List<com.treasure.core.domain.Item> = emptyList(),
    onAddExistingItem: (String) -> Unit = {},
    /** Cycle 0035 v2：新的语音流程 — 点击麦克风进语音态，长按输入框直接录音、松手发送。
     *  这两个回调取代之前 mic 长按入 RecordingOverlay 的全屏流程。 */
    onPressVoiceStart: () -> Unit = {},
    onPressVoiceSend: () -> Unit = {},
    onPressVoiceChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    var drawer by remember { mutableStateOf(ChatDrawer.None) }
    // Cycle 0035 v2：点麦克风进入语音态（输入框变成"长按输入语音"按钮）。
    var voiceMode by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val selectedProfile = remember(aiProfiles, selectedProfileId) {
        aiProfiles.firstOrNull { it.id == selectedProfileId } ?: aiProfiles.firstOrNull()
    }

    // Cycle 0035 v2：点 chip 打开抽屉时同步收输入法（保持画面整洁，且抽屉
    // 不会被键盘往上挤）。
    LaunchedEffect(drawer) {
        if (drawer != ChatDrawer.None) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        }
    }
    // Cycle 0035 v2：抽屉开着时 back 键先关抽屉，不要 pop 到 Main。
    androidx.activity.compose.BackHandler(enabled = drawer != ChatDrawer.None) {
        drawer = ChatDrawer.None
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // pending photos strip (kept above chip row)
        if (pendingPhotos.isNotEmpty()) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(pendingPhotos.size) { idx ->
                        val uri = pendingPhotos[idx]
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.paper)
                                .border(0.5.dp, colors.line, RoundedCornerShape(8.dp))
                                .clickable { onPreviewPending(idx) },
                        ) {
                            coil.compose.AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colors.ink.copy(alpha = 0.75f))
                                    .clickable { onRemovePending(uri) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✕", color = colors.paper, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (pendingPhotos.size == 1) "可以配一句话一起发" else "${pendingPhotos.size} 张图 · 可再加 / 配一句话一起发",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // chip row (附件 / 模型)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            ChatChip(
                active = drawer == ChatDrawer.Attach,
                enabled = !saved,
                onClick = {
                    drawer = if (drawer == ChatDrawer.Attach) ChatDrawer.None else ChatDrawer.Attach
                },
                leading = { Text("＋", style = MaterialTheme.typography.labelSmall) },
            ) { Text("附件", style = MaterialTheme.typography.labelMedium) }
            ChatChip(
                active = drawer == ChatDrawer.Model,
                enabled = !saved,
                onClick = {
                    drawer = if (drawer == ChatDrawer.Model) ChatDrawer.None else ChatDrawer.Model
                },
                leading = { Text("✦", style = MaterialTheme.typography.labelSmall) },
            ) {
                Text(
                    text = selectedProfile?.shortLabel() ?: "未配置",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(4.dp))
                Text("▾", style = MaterialTheme.typography.labelSmall)
            }
        }

        // input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // input pill with mic-inside-left + emoji-inside-right
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.card)
                    .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Cycle 0035 v2：mic 改成点击切换 voiceMode，glyph 用声波而非
                // 麦克风图标 — 老 MicGlyph 偏丑。
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !saved && !busy) {
                            voiceMode = !voiceMode
                            // 进 voice 态时收键盘；退出态保持原状
                            if (voiceMode) {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    SoundwaveGlyph(
                        color = when {
                            saved || busy -> colors.sub.copy(alpha = 0.4f)
                            voiceMode -> colors.terra
                            else -> colors.sub
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 28.dp, max = 96.dp)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (voiceMode) {
                        // Cycle 0035 v2：voice 态下输入框变成"长按输入语音"
                        // 按钮 — pointerInput 用 awaitPointerEventScope 把按下/
                        // 抬起拆开做，press 启动录音、release 自动发送。
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 28.dp)
                                .pointerInput(saved, busy) {
                                    if (saved || busy) return@pointerInput
                                    awaitPointerEventScope {
                                        while (true) {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            down.consume()
                                            onPressVoiceChange(true)
                                            onPressVoiceStart()
                                            try {
                                                waitForUpOrCancellation()?.consume()
                                            } finally {
                                                onPressVoiceChange(false)
                                                onPressVoiceSend()
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "长按 · 录音",
                                color = colors.ink.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        if (input.isEmpty()) {
                            Text(
                                text = when {
                                    saved -> "会话已封存 · 已收入图鉴"
                                    pendingPhotos.isNotEmpty() -> "配一句话…（可留空）"
                                    else -> "说说这件东西…"
                                },
                                color = colors.sub.copy(alpha = if (saved) 0.5f else 1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = if (saved) FontStyle.Italic else FontStyle.Normal,
                            )
                        }
                        BasicTextField(
                            value = input,
                            onValueChange = onInputChange,
                            enabled = !saved,
                            cursorBrush = SolidColor(colors.terra),
                            maxLines = 4,
                            textStyle = LocalTextStyle.current.copy(
                                color = if (saved) colors.sub.copy(alpha = 0.5f) else colors.ink,
                                fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !saved) {
                            drawer = if (drawer == ChatDrawer.Emoji) ChatDrawer.None else ChatDrawer.Emoji
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    EmojiSmileGlyph(
                        color = if (drawer == ChatDrawer.Emoji) colors.terra else colors.sub,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // send button (outside, circle)
            val hasContent = input.isNotBlank() || pendingPhotos.isNotEmpty()
            when {
                saved -> Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.line),
                    contentAlignment = Alignment.Center,
                ) { ArrowUpGlyph(colors.sub) }
                busy && !hasContent -> Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.terra)
                        .clickable(onClick = onStop),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.paper),
                    )
                }
                busy -> {
                    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "send-spin")
                    val rotation by infinite.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.tween(
                                durationMillis = 900,
                                easing = androidx.compose.animation.core.LinearEasing,
                            ),
                        ),
                        label = "send-spin-rot",
                    )
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.line),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            val sw = 1.8.dp.toPx()
                            val r = size.minDimension / 2f - sw / 2f
                            rotate(rotation) {
                                drawArc(
                                    color = colors.terra,
                                    startAngle = 0f,
                                    sweepAngle = 280f,
                                    useCenter = false,
                                    topLeft = Offset(sw / 2f, sw / 2f),
                                    size = Size(r * 2, r * 2),
                                    style = Stroke(
                                        width = sw,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    ),
                                )
                            }
                        }
                    }
                }
                else -> {
                    val canSend = hasContent
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (canSend) colors.ink else colors.card)
                            .border(0.5.dp, if (canSend) colors.ink else colors.line, CircleShape)
                            .clickable(enabled = canSend, onClick = onSend),
                        contentAlignment = Alignment.Center,
                    ) { ArrowUpGlyph(if (canSend) colors.paper else colors.sub) }
                }
            }
        }

        // drawer slot — animated 0 → wrap. Pushes the input upward when open.
        androidx.compose.animation.AnimatedVisibility(
            visible = drawer != ChatDrawer.None,
            enter = androidx.compose.animation.expandVertically(
                expandFrom = Alignment.Top,
            ) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically(
                shrinkTowards = Alignment.Top,
            ) + androidx.compose.animation.fadeOut(),
        ) {
            ChatDrawerBody(
                drawer = drawer,
                onClose = { drawer = ChatDrawer.None },
                onPickImage = {
                    drawer = ChatDrawer.None
                    onTakePhoto()
                },
                onPickFile = {
                    drawer = ChatDrawer.None
                    onPickFile()
                },
                onSwitchToItemPicker = { drawer = ChatDrawer.ItemPicker },
                aiProfiles = aiProfiles,
                selectedProfileId = selectedProfile?.id,
                onSelectProfile = { id ->
                    onSelectProfile(id)
                    drawer = ChatDrawer.None
                },
                onPickEmoji = { e -> onInputChange(input + e) },
                allItems = allItems,
                onPickExistingItem = { id ->
                    onAddExistingItem(id)
                    drawer = ChatDrawer.None
                },
            )
        }
    }
}

@Composable
private fun ChatChip(
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) colors.ink else colors.card)
            .border(0.5.dp, if (active) colors.ink else colors.line, RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides
                if (active) colors.paper else if (enabled) colors.ink else colors.sub,
        ) {
            if (leading != null) leading()
            content()
        }
    }
}

@Composable
private fun ChatDrawerBody(
    drawer: ChatDrawer,
    onClose: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onSwitchToItemPicker: () -> Unit,
    aiProfiles: List<com.treasure.data.AiProfile>,
    selectedProfileId: String?,
    onSelectProfile: (String) -> Unit,
    onPickEmoji: (String) -> Unit,
    allItems: List<com.treasure.core.domain.Item>,
    onPickExistingItem: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.paper)
            .border(0.5.dp, colors.line, RoundedCornerShape(18.dp)),
    ) {
        // header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.line),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (drawer) {
                    ChatDrawer.Attach -> "添加附件"
                    ChatDrawer.Model -> "选择模型"
                    ChatDrawer.Emoji -> "表情"
                    ChatDrawer.ItemPicker -> "添加已有物品"
                    ChatDrawer.None -> ""
                },
                color = colors.ink,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = colors.sub, style = MaterialTheme.typography.labelMedium)
            }
        }
        when (drawer) {
            ChatDrawer.Attach -> AttachGrid(
                onPickImage = onPickImage,
                onPickFile = onPickFile,
                onPickItem = onSwitchToItemPicker,
            )
            ChatDrawer.Model -> ModelList(
                profiles = aiProfiles,
                selectedId = selectedProfileId,
                onPick = onSelectProfile,
            )
            ChatDrawer.Emoji -> EmojiGrid(onPick = onPickEmoji)
            ChatDrawer.ItemPicker -> ItemPickerList(
                items = allItems,
                onPick = onPickExistingItem,
            )
            ChatDrawer.None -> Unit
        }
    }
}

@Composable
private fun AttachGrid(
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onPickItem: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Cycle 0035 v2：换成线描 glyph，跟全局画风对齐（emoji 字符在各家
        // ROM 上长得差距太大、又破坏 serif/mono 的克制感）。
        AttachTile(label = "图片", sub = "拍照或相册",
            onClick = onPickImage, modifier = Modifier.weight(1f),
        ) { c -> PictureGlyph(c) }
        AttachTile(label = "文件", sub = "PDF · 文本",
            onClick = onPickFile, modifier = Modifier.weight(1f),
        ) { c -> FileGlyph(c) }
        AttachTile(label = "物品", sub = "已收入图鉴",
            onClick = onPickItem, modifier = Modifier.weight(1f),
        ) { c -> CubeGlyph(c) }
    }
}

@Composable
private fun AttachTile(
    label: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: @Composable (color: Color) -> Unit,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.paper),
            contentAlignment = Alignment.Center,
        ) {
            glyph(colors.ink)
        }
        Text(label, color = colors.ink, style = MaterialTheme.typography.labelLarge)
        Text(sub, color = colors.sub, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ModelList(
    profiles: List<com.treasure.data.AiProfile>,
    selectedId: String?,
    onPick: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    if (profiles.isEmpty()) {
        Text(
            text = "去 [设置] 添加 AI 服务",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )
        return
    }
    Column(
        modifier = Modifier
            .heightIn(max = 220.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
    ) {
        profiles.forEach { p ->
            val on = p.id == selectedId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) colors.ink else Color.Transparent)
                    .clickable { onPick(p.id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = p.preset.display,
                        color = if (on) colors.paper else colors.ink,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = p.effectiveModel,
                        color = if (on) colors.paper.copy(alpha = 0.7f) else colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (on) colors.paper else colors.line,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (on) Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.paper),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiGrid(onPick: (String) -> Unit) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState()),
    ) {
        EMOJI_PALETTE.chunked(8).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEach { e ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(e) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemPickerList(
    items: List<com.treasure.core.domain.Item>,
    onPick: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    if (items.isEmpty()) {
        Text(
            text = "图鉴里还没有物品",
            color = colors.sub,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )
        return
    }
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val filtered = remember(items, q) {
        if (q.isEmpty()) items else items.filter {
            "${it.brand} ${it.model}".lowercase().contains(q) ||
                it.nickname.lowercase().contains(q) ||
                it.oneLiner.lowercase().contains(q)
        }
    }
    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        // Cycle 0035 v2：搜索条 — 图鉴大了之后没法翻
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (query.isEmpty()) {
                Text(
                    text = "搜物品（品牌 / 型号 / 备注）",
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                cursorBrush = SolidColor(colors.terra),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.ink,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Column(
            modifier = Modifier
                .heightIn(max = 200.dp)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
        ) {
            if (filtered.isEmpty()) {
                Text(
                    text = "没找到匹配的物品",
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                )
            } else {
                filtered.forEach { item ->
                    val itemTitle = "${item.brand} ${item.model}".trim()
                        .ifBlank { item.nickname }
                        .ifBlank { "（无标题）" }
                    val itemSub = item.oneLiner.ifBlank { item.nickname }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPick(item.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = itemTitle,
                                color = colors.ink,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            if (itemSub.isNotBlank() && itemSub != itemTitle) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = itemSub,
                                    color = colors.sub,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        Text("+", color = colors.terra, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}


@Composable
private fun CameraGlyph(color: Color) {
    Canvas(modifier = Modifier.size(17.dp)) {
        val sw = 1.3.dp.toPx()
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.30f),
            size = Size(w * 0.70f, h * 0.55f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(sw),
        )
        drawCircle(
            color = color,
            radius = h * 0.15f,
            center = Offset(w * 0.50f, h * 0.55f),
            style = Stroke(sw),
        )
        drawLine(color, Offset(w * 0.35f, h * 0.30f), Offset(w * 0.40f, h * 0.20f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.40f, h * 0.20f), Offset(w * 0.60f, h * 0.20f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.60f, h * 0.20f), Offset(w * 0.65f, h * 0.30f), strokeWidth = sw)
    }
}

@Composable
private fun MicGlyph(color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val sw = 1.4.dp.toPx()
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.36f, h * 0.14f),
            size = Size(w * 0.28f, h * 0.50f),
            cornerRadius = CornerRadius(w * 0.14f),
            style = Stroke(sw),
        )
        drawLine(color, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.20f, h * 0.55f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.80f, h * 0.50f), Offset(w * 0.80f, h * 0.55f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.20f, h * 0.55f), Offset(w * 0.50f, h * 0.78f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.80f, h * 0.55f), Offset(w * 0.50f, h * 0.78f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.50f, h * 0.78f), Offset(w * 0.50f, h * 0.92f), strokeWidth = sw)
    }
}

@Composable
private fun ArrowUpGlyph(color: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val sw = 1.6.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(color, Offset(w / 2f, h * 0.20f), Offset(w / 2f, h * 0.80f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.25f, h * 0.45f), Offset(w / 2f, h * 0.20f), strokeWidth = sw)
        drawLine(color, Offset(w * 0.75f, h * 0.45f), Offset(w / 2f, h * 0.20f), strokeWidth = sw)
    }
}

// ─── Cycle 0035 v2 chatbar glyphs ─────────────────────────────────────

/** 五条对称声波线 — 取代 mic 图标，跟"声音"语义更直接。 */
@Composable
private fun SoundwaveGlyph(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val sw = 1.5.dp.toPx()
        val w = size.width
        val h = size.height
        // 中间 3 条 + 两边各 1 条；高度按距离中心递减。
        val xs = listOf(0.18f, 0.34f, 0.50f, 0.66f, 0.82f)
        val heights = listOf(0.32f, 0.62f, 0.86f, 0.62f, 0.32f)
        xs.forEachIndexed { i, xRatio ->
            val x = w * xRatio
            val hb = h * heights[i]
            drawLine(
                color = color,
                start = Offset(x, (h - hb) / 2f),
                end = Offset(x, (h + hb) / 2f),
                strokeWidth = sw,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

/** 微笑表情线描 — 比 unicode ☺ 在不同 ROM 上稳定。 */
@Composable
private fun EmojiSmileGlyph(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val sw = 1.4.dp.toPx()
        val w = size.width
        val h = size.height
        drawCircle(color = color, radius = w * 0.45f, center = Offset(w / 2f, h / 2f), style = Stroke(sw))
        // 两眼
        drawCircle(color = color, radius = sw * 0.8f, center = Offset(w * 0.36f, h * 0.42f))
        drawCircle(color = color, radius = sw * 0.8f, center = Offset(w * 0.64f, h * 0.42f))
        // 嘴 — 用一段曲线近似（drawArc 直接画半圆）
        drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.40f),
            size = Size(w * 0.40f, h * 0.35f),
            style = Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

/** 风景照线描 — 矩形 + 太阳 + 远山。 */
@Composable
private fun PictureGlyph(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val sw = 1.4.dp.toPx()
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.15f),
            size = Size(w * 0.80f, h * 0.70f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(sw),
        )
        drawCircle(color = color, radius = w * 0.07f, center = Offset(w * 0.30f, h * 0.34f), style = Stroke(sw))
        drawLine(color, Offset(w * 0.18f, h * 0.78f), Offset(w * 0.50f, h * 0.50f), strokeWidth = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, Offset(w * 0.50f, h * 0.50f), Offset(w * 0.82f, h * 0.78f), strokeWidth = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** 文件线描 — 带折角的纸张。 */
@Composable
private fun FileGlyph(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val sw = 1.4.dp.toPx()
        val w = size.width
        val h = size.height
        val left = w * 0.22f
        val right = w * 0.78f
        val top = h * 0.14f
        val bottom = h * 0.86f
        val corner = w * 0.20f
        // 主体（去掉右上角后的轮廓）
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(left, top)
            lineTo(right - corner, top)
            lineTo(right, top + corner)
            lineTo(right, bottom)
            lineTo(left, bottom)
            close()
        }
        drawPath(path, color, style = Stroke(sw))
        // 折角小三角
        drawLine(color, Offset(right - corner, top), Offset(right - corner, top + corner), strokeWidth = sw)
        drawLine(color, Offset(right - corner, top + corner), Offset(right, top + corner), strokeWidth = sw)
        // 内部三道文字线
        drawLine(color, Offset(left + w * 0.10f, h * 0.50f), Offset(right - w * 0.10f, h * 0.50f), strokeWidth = sw * 0.7f)
        drawLine(color, Offset(left + w * 0.10f, h * 0.62f), Offset(right - w * 0.10f, h * 0.62f), strokeWidth = sw * 0.7f)
        drawLine(color, Offset(left + w * 0.10f, h * 0.74f), Offset(left + w * 0.50f, h * 0.74f), strokeWidth = sw * 0.7f)
    }
}

/** 立方体线描 — 已收入图鉴里的物品。 */
@Composable
private fun CubeGlyph(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val sw = 1.4.dp.toPx()
        val w = size.width
        val h = size.height
        // 简单 2D 盒子轮廓 + 上面 / 侧面分割线
        val l = w * 0.18f
        val r = w * 0.82f
        val t = h * 0.22f
        val b = h * 0.82f
        drawRoundRect(
            color = color,
            topLeft = Offset(l, t),
            size = Size(r - l, b - t),
            cornerRadius = CornerRadius(1.dp.toPx()),
            style = Stroke(sw),
        )
        // 上盖分割
        drawLine(color, Offset(l, t + (b - t) * 0.30f), Offset(r, t + (b - t) * 0.30f), strokeWidth = sw)
        // 上盖中线（开盖 hint）
        drawLine(color, Offset((l + r) / 2f, t), Offset((l + r) / 2f, t + (b - t) * 0.30f), strokeWidth = sw)
    }
}

/** Cycle 0035 v2：press-hold 录音中的视觉反馈 — 9 条声波，按 sin 节奏起伏。 */
@Composable
private fun AnimatedSoundwave(color: Color) {
    val infinite = androidx.compose.animation.core.rememberInfiniteTransition(label = "soundwave")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 900,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
        ),
        label = "soundwave-phase",
    )
    Canvas(
        modifier = Modifier
            .size(width = 140.dp, height = 64.dp),
    ) {
        val barCount = 9
        val sw = 4.dp.toPx()
        val gap = (size.width - barCount * sw) / (barCount - 1)
        val cx = size.height / 2f
        for (i in 0 until barCount) {
            val offset = (i - (barCount - 1) / 2.0).toFloat()
            val ampRatio = (kotlin.math.sin(phase + offset * 0.7).toFloat() + 1f) / 2f
            val h = (size.height * 0.20f) + ampRatio * (size.height * 0.70f)
            val x = sw / 2f + i * (sw + gap)
            drawLine(
                color = color,
                start = Offset(x, cx - h / 2f),
                end = Offset(x, cx + h / 2f),
                strokeWidth = sw,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}

// ─── not configured banner ────────────────────────────────────────────

/**
 * 一行小字提示，藏在头部分割线下方。没有方框、没有按钮 — 把
 * 这件事当成博物馆图鉴边角的一句脚注，而不是 toast。
 */
@Composable
private fun NotConfiguredBanner(onGoSettings: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onGoSettings)
            .padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(colors.terra),
        )
        Text(
            text = "尚未配置 AI",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "·",
            color = colors.sub.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "前往设置",
            color = colors.terra,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = com.treasure.theme.Cormorant,
                fontSize = 13.sp,
            ),
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "→",
            color = colors.terra,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

// ─── 工作集 drawer (Cycle 0031 redesign) ────────────────────────────────

/**
 * 一段会话的"工作集"抽屉。每行 = 一件候选物品；右侧状态胶囊承载主交互：
 *  - PENDING (红)：草稿待录入 → 进 Refine 页编辑 / 提交
 *  - SAVED (绿)：已收入图鉴 → 跳到 Detail 页
 *  - MODIFIED (黄)：原物品有了新版草稿 → 进 proposal-preview 决定是否覆盖
 *
 * 顶部右侧 [+] 打开 [ItemPickerSheet]，从图鉴里挑一件直接 SAVED 进工作集（让
 *  AI 能"看到"它，再让用户问 AI 帮忙补充 / 修改）。
 *
 * 行长按弹删除确认 — PENDING / MODIFIED 整行删（草稿丢弃），SAVED 仅移出工作
 * 集，不动图鉴里的真物品。
 */
private val StatusPendingColor = Color(0xFFA63A1F) // 锈红 — 待录入
private val StatusSavedColor   = Color(0xFF3F6B4A) // 苔绿 — 已录入
private val StatusModifiedColor = Color(0xFFB07A1F) // 黄铜 — 新修改

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ItemListDrawer(
    items: List<com.treasure.core.repo.ConversationItem>,
    itemsById: Map<String, com.treasure.core.domain.Item>,
    allItems: List<com.treasure.core.domain.Item>,
    onDismiss: () -> Unit,
    onPickItem: (com.treasure.core.repo.ConversationItem) -> Unit,
    onAddExistingItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onCommitAllPending: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    var pickerOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<com.treasure.core.repo.ConversationItem?>(null) }
    // Cycle 0034 v5：一键录入二次确认。
    var confirmingCommit by remember { mutableStateOf(false) }
    val pendingCount = items.count {
        it.status == com.treasure.core.repo.ConversationItemStatus.PENDING ||
            it.status == com.treasure.core.repo.ConversationItemStatus.MODIFIED
    }

    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            // 抽屉头：标题 + 右侧 plus
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 14.dp, top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "工作集",
                        color = colors.ink,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "这次会话攒下的物品",
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                    )
                }
                // Cycle 0034 v5：一键录入 — 把所有 PENDING / MODIFIED 一并提交
                // 到图鉴。pendingCount=0 时整颗按钮置灰不可点。
                if (pendingCount > 0) {
                    Text(
                        text = "一键录入 ($pendingCount)",
                        color = colors.terra,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(colors.terra.copy(alpha = 0.12f))
                            .clickable { confirmingCommit = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                IconCircleButton(onClick = { pickerOpen = true }) { PlusGlyph(colors.ink) }
            }
            HistoryDivider(colors.line)
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
            ) {
                if (items.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "（还没有物品 — 给 AI 发条消息，或点右上 + 从图鉴里挑）",
                                color = colors.sub,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                } else {
                    items(items.size) { idx ->
                        val ci = items[idx]
                        WorkingItemRow(
                            ci = ci,
                            savedItem = ci.itemRef?.let { itemsById[it] },
                            onTap = { onPickItem(ci) },
                            onLongPress = { deleting = ci },
                        )
                        if (idx < items.size - 1) HistoryDivider(colors.line)
                    }
                }
            }
        }
    }

    if (pickerOpen) {
        ItemPickerSheet(
            allItems = allItems,
            alreadyInWorkingSet = items.mapNotNull { it.itemRef }.toSet(),
            onDismiss = { pickerOpen = false },
            onPick = { itemId ->
                onAddExistingItem(itemId)
                pickerOpen = false
            },
        )
    }
    deleting?.let { ci ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = {
                Text(
                    text = when (ci.status) {
                        com.treasure.core.repo.ConversationItemStatus.SAVED -> "从工作集移除？"
                        else -> "丢弃这份草稿？"
                    },
                )
            },
            text = {
                Text(
                    text = when (ci.status) {
                        com.treasure.core.repo.ConversationItemStatus.SAVED ->
                            "只是从这次会话里移除，图鉴里的物品仍然在。"
                        com.treasure.core.repo.ConversationItemStatus.MODIFIED ->
                            "丢弃 AI 提的新版草稿；图鉴里的物品保持原样。"
                        else -> "草稿会被丢弃，无法恢复。"
                    },
                    color = colors.sub,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onRemoveItem(ci.id)
                    deleting = null
                }) { Text("移除") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleting = null }) {
                    Text("取消")
                }
            },
            containerColor = colors.card,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
    // Cycle 0034 v5：一键录入二次确认。
    if (confirmingCommit) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmingCommit = false },
            title = { Text(text = "一键录入 $pendingCount 件物品？") },
            text = {
                Text(
                    text = "把工作集里所有待录入 / 新修改的物品一次性收进图鉴。" +
                        "录入后会话仍可继续；要再改就到图鉴里编辑。",
                    color = colors.sub,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmingCommit = false
                    onCommitAllPending()
                }) { Text("录入", color = colors.terra) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmingCommit = false }) {
                    Text("取消")
                }
            },
            containerColor = colors.card,
            titleContentColor = colors.ink,
            textContentColor = colors.sub,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WorkingItemRow(
    ci: com.treasure.core.repo.ConversationItem,
    savedItem: com.treasure.core.domain.Item?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    // 拼 preview-item：SAVED 行用图鉴里真物品；PENDING / MODIFIED 用 draft 套
    // 模板的 heroVector / palette。
    val previewItem = remember(ci, savedItem) {
        savedItem ?: ci.draft?.let { d -> draftToPreviewItem(d) }
    }
    val draft = ci.draft
    val title = when {
        savedItem != null -> {
            val brand = savedItem.brand
            val model = savedItem.model
            val nick = savedItem.nickname
            listOf(brand, model).filter { it.isNotBlank() }
                .joinToString(" ").ifBlank { nick }.ifBlank { "无名物品" }
        }
        draft != null -> {
            listOf(draft.brand, draft.model).filter { it.isNotBlank() }
                .joinToString(" ").ifBlank { draft.nickname }.ifBlank { "草稿" }
        }
        else -> "（空）"
    }
    val subtitle = when {
        savedItem != null && savedItem.oneLiner.isNotBlank() -> savedItem.oneLiner
        draft != null && draft.oneLiner.isNotBlank() -> draft.oneLiner
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.paper)
                .border(0.5.dp, colors.line)
                .padding(4.dp),
        ) {
            // Cycle 0034 v3：HeroAvatar — avatarPhotoPath 非空时显示真照片
            HeroAvatar(item = previewItem, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        StatusPill(status = ci.status)
    }
}

@Composable
private fun StatusPill(status: com.treasure.core.repo.ConversationItemStatus) {
    val colors = LocalTreasureColors.current
    val (label, accent) = when (status) {
        com.treasure.core.repo.ConversationItemStatus.PENDING -> "待录入" to StatusPendingColor
        com.treasure.core.repo.ConversationItemStatus.SAVED -> "已录入" to StatusSavedColor
        com.treasure.core.repo.ConversationItemStatus.MODIFIED -> "新修改" to StatusModifiedColor
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(0.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(accent),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = accent,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private fun draftToPreviewItem(draft: com.treasure.core.ai.ItemDraft): com.treasure.core.domain.Item {
    // 复用 DraftCtaCard 里的同款 stub：用 category 找模板拿 palette / heroVector。
    val template = com.treasure.core.domain.Category.entries
        .firstOrNull { it.id == draft.category }
        ?.let { CategoryTemplates.forCategory(it) }
        ?: CategoryTemplates.forCategory(com.treasure.core.domain.Category.TECH)
    return com.treasure.core.domain.Item(
        id = "preview",
        category = template.category.id,
        brand = draft.brand,
        model = draft.model,
        nickname = "",
        acquired = "",
        parted = null,
        status = com.treasure.core.domain.ItemStatus.OWNED,
        palette = template.palette,
        oneLiner = "",
        heroVector = template.heroVector,
        specs = emptyList(),
        history = emptyList(),
        // Cycle 0034 v3：把 draft 上的 photos / avatarPhotoPath 透传给 stub
        // Item，让 HeroAvatar 优先走 AsyncImage(avatarPhoto) — 之前都丢成空，
        // 工作集胶囊还是显示线描。
        photos = draft.photos,
        avatarPhotoPath = draft.avatarPhotoPath,
        createdAt = 0L,
        updatedAt = 0L,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ItemPickerSheet(
    allItems: List<com.treasure.core.domain.Item>,
    alreadyInWorkingSet: Set<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val colors = LocalTreasureColors.current
    var query by remember { mutableStateOf("") }
    val candidates = remember(allItems, query, alreadyInWorkingSet) {
        val q = query.trim().lowercase()
        allItems
            .filter { it.id !in alreadyInWorkingSet }
            .filter {
                q.isBlank() ||
                    it.brand.lowercase().contains(q) ||
                    it.model.lowercase().contains(q) ||
                    it.nickname.lowercase().contains(q)
            }
            .sortedByDescending { it.updatedAt }
    }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.paper,
        contentColor = colors.ink,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
                Text(
                    text = "从图鉴里挑一件",
                    color = colors.ink,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "加入这次会话，让 AI 看到它",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.card)
                        .border(0.5.dp, colors.line, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = colors.ink),
                        cursorBrush = SolidColor(colors.ink),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    text = "搜品牌 / 型号 / 昵称",
                                    color = colors.sub.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            HistoryDivider(colors.line)
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
            ) {
                if (candidates.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (allItems.isEmpty()) "（图鉴里还没有物品）"
                                else "没找到匹配 — 试试其他关键词",
                                color = colors.sub,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                } else {
                    items(candidates.size) { idx ->
                        val it = candidates[idx]
                        ItemPickerRow(item = it, onClick = { onPick(it.id) })
                        if (idx < candidates.size - 1) HistoryDivider(colors.line)
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemPickerRow(
    item: com.treasure.core.domain.Item,
    onClick: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(colors.paper)
                .border(0.5.dp, colors.line)
                .padding(4.dp),
        ) {
            HeroAvatar(item = item, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            val title = listOf(item.brand, item.model).filter { it.isNotBlank() }
                .joinToString(" ").ifBlank { item.nickname }.ifBlank { "无名" }
            Text(
                text = title,
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
            )
            if (item.oneLiner.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.oneLiner,
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
        Text(
            text = "+",
            color = colors.terra,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
