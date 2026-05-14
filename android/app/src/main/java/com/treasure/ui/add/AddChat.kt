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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.treasure.illust.HeroIllustration
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
    onGoSettings: () -> Unit,
    onPreviewPhoto: (android.net.Uri) -> Unit,
    onAcceptProposal: (String) -> Unit,
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
) {
    val colors = LocalTreasureColors.current
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            // The composer floats over the bottom of this column at
            // navigationBarsPadding + 100dp (roughly 150–170dp tall once
            // the composer + control island stack). Add equivalent bottom
            // contentPadding so messages can scroll fully into view above
            // the composer instead of disappearing under it.
            //
            // Cycle 0014：键盘弹起时也要让消息往上腾出位置 — bottom inset 取
            // max(navigationBars, ime)。Compose 的 imePadding 在 IME 上来时
            // 自动是包含 navBar 的高度。
            val bottomNavInset = WindowInsets.navigationBars
                .asPaddingValues().calculateBottomPadding()
            val bottomImeInset = WindowInsets.ime
                .asPaddingValues().calculateBottomPadding()
            val effectiveBottom = maxOf(bottomNavInset, bottomImeInset)
            // Cycle 0021：包一层 SelectionContainer 让用户能长按 → 复制 / 选择
            // 任意一段文字（助手 / 用户 / 语音转写 / 草稿副标都行）。
            // Composer 那边的 BasicTextField 自带 paste，无需多动。
            androidx.compose.foundation.text.selection.SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 22.dp,
                        end = 22.dp,
                        top = 18.dp,
                        bottom = 18.dp + effectiveBottom + 160.dp,
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
                            onRejectProposal = onRejectProposal,
                        )
                    }
                    if (state.busy) {
                        item { TypingIndicator(startedAt = state.busyStartedAt) }
                    } else if (state.lastElapsedMs != null && state.messages.lastOrNull() !is AddMessage.User && state.messages.lastOrNull() !is AddMessage.UserPhoto) {
                        // Cycle 0031：上一轮 AI 调用耗时小字 — 仅当最后一条不是用户消息
                        // 时显示（用户已经发新一句话 / 图，旧耗时就别再喧宾夺主）
                        item { ElapsedHint(state.lastElapsedMs) }
                    }
                }
            }
        }

        // Composer floats above the global control island. Control island
        // sits at navigationBarsPadding + 18dp + ~50dp tall; we sit at
        // 100dp so we're always at least 32dp clear of its top edge.
        //
        // Cycle 0014：用 imePadding 替代 navigationBarsPadding — 没键盘时
        // imePadding 等价 navigationBars，键盘升起时 composer 自动跟着上浮。
        // bottom 也按是否在用键盘做判断：键盘起时不再给 100dp 控制岛缓冲，
        // 因为控制岛在键盘下面被挡住了。
        val imeOpen = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > 0.dp
        Composer(
            input = input,
            onInputChange = { input = it },
            busy = state.busy,
            saved = state.saved,
            pendingPhotos = pendingPhotos,
            onRemovePending = { uri -> pendingPhotos = pendingPhotos - uri },
            onSend = {
                if (pendingPhotos.isNotEmpty()) {
                    // 一组图片 + 一段文字一起送给 AI；AI 据图分配给物品影集。
                    onSendPhotos(pendingPhotos, input)
                } else {
                    onSendText(input)
                }
                input = ""
                pendingPhotos = emptyList()
            },
            onStop = onStopExtract,
            onTakePhoto = ::launchPhotoFlow,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .padding(
                    bottom = if (imeOpen) 8.dp else 100.dp,
                    start = 14.dp,
                    end = 14.dp,
                ),
        )

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
    onRejectProposal: (String) -> Unit,
) {
    when (message) {
        is AddMessage.Assistant -> AssistantBubble(text = message.text)
        is AddMessage.User -> UserTextBubble(text = message.text)
        is AddMessage.UserPhoto -> UserPhotoBubble(
            uri = message.uri,
            onClick = { onPreviewPhoto(message.uri) },
        )
        is AddMessage.UserVoice -> UserVoiceBubble(text = message.text, duration = message.duration)
        is AddMessage.DraftCta -> DraftCtaCard(
            message = message,
            onOpen = { onPreviewProposal(message) },
            onAccept = { onAcceptProposal(message.id) },
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
private fun UserVoiceBubble(text: String, duration: String) {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 4.dp, bottomStart = 14.dp))
                .background(colors.ink)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Waveform(
                    color = colors.paper,
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

@Composable
private fun DraftCtaCard(
    message: AddMessage.DraftCta,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val template = remember(message.draft) {
        com.treasure.core.domain.Category.entries
            .firstOrNull { it.id == message.draft.category }
            ?.let { CategoryTemplates.forCategory(it) }
            ?: CategoryTemplates.forCategory(com.treasure.core.domain.Category.TECH)
    }
    val previewItem = remember(message.draft, template) {
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
            photos = emptyList(),
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
                HeroIllustration(item = previewItem, modifier = Modifier.fillMaxSize())
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
        // Cycle 0034：AI 给这张卡分了哪些图 — 缩略图条让用户在采用前看一眼
        // 是否分配正确。⭐ 标 avatar；crop 比例 != 整图时角落带一个剪刀提示。
        if (message.photoAssignments.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(message.photoAssignments.size) { idx ->
                    val pa = message.photoAssignments[idx]
                    val cropped = pa.cropW < 0.999f || pa.cropH < 0.999f ||
                        pa.cropX > 0.001f || pa.cropY > 0.001f
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.paper)
                            .border(0.5.dp, colors.line, RoundedCornerShape(6.dp)),
                    ) {
                        AsyncImage(
                            model = pa.sourceUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        if (pa.isAvatar) {
                            Text(
                                text = "★",
                                color = colors.terra,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(2.dp),
                            )
                        }
                        if (cropped) {
                            Text(
                                text = "✂",
                                color = colors.paper,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(colors.ink.copy(alpha = 0.6f))
                                    .padding(horizontal = 3.dp),
                            )
                        }
                    }
                }
            }
        }
        if (isPending) {
            // 采用 / 不要 按钮：右下角，与 cycle 0021 起的 footer 一致风格
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
                    text = "采用",
                    color = colors.terra,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.terra.copy(alpha = 0.12f))
                        .clickable(onClick = onAccept)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
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

// ─── composer ─────────────────────────────────────────────────────────

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
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        // Cycle 0034：pending photos strip — 多张图横滑预览，单张右上 × 删。
        // 用户可再点 📷 追加更多。AI 见到 N 张图后会按 source_index 分配给草稿。
        if (pendingPhotos.isNotEmpty()) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp)
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
                                .border(0.5.dp, colors.line, RoundedCornerShape(8.dp)),
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
                    text = if (pendingPhotos.size == 1) "可以配一句话一起发" else "${pendingPhotos.size} 张图 · 可再加 📷 / 配一句话一起发",
                    color = colors.sub,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clickable(enabled = !saved, onClick = onTakePhoto),
                contentAlignment = Alignment.Center,
            ) { CameraGlyph(if (saved) colors.sub.copy(alpha = 0.4f) else colors.sub) }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 28.dp, max = 96.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
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
            Spacer(Modifier.width(8.dp))
            // Cycle 0031：发送键三态 —
            //   busy + 输入空：⬛ 停止键（terra 底 + paper 方块）→ 掐请求
            //   busy + 输入非空：转圈 ring（terra 描边）置灰、不可点
            //   非 busy：箭头键，有内容才点亮（ink 底）
            // saved 模式 disable 整个 composer（外层已 disable 文本框、加号、
            // 这里也要禁用）。
            val hasContent = input.isNotBlank() || pendingPhotos.isNotEmpty()
            when {
                saved -> {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.line),
                        contentAlignment = Alignment.Center,
                    ) { ArrowUpGlyph(colors.sub) }
                }
                busy && !hasContent -> {
                    // 停止键
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.terra)
                            .clickable(onClick = onStop),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.paper),
                        )
                    }
                }
                busy -> {
                    // 旋转圈圈占位 — 已经有想发的内容，等当前请求结束
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.line),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (canSend) colors.ink else colors.line)
                            .clickable(enabled = canSend, onClick = onSend),
                        contentAlignment = Alignment.Center,
                    ) { ArrowUpGlyph(if (canSend) colors.paper else colors.sub) }
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
) {
    val colors = LocalTreasureColors.current
    var pickerOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<com.treasure.core.repo.ConversationItem?>(null) }

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
            HeroIllustration(item = previewItem, modifier = Modifier.fillMaxSize())
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
        photos = emptyList(),
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
            HeroIllustration(item = item, modifier = Modifier.fillMaxSize())
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
