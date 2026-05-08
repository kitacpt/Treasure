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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors

private const val DEFAULT_TITLE = "New entry"

@Composable
fun AddChat(
    state: AddUiState,
    conversations: List<FakeConversation>,
    historyOpen: Boolean,
    onToggleHistory: () -> Unit,
    onOpenManual: () -> Unit,
    onNewChat: () -> Unit,
    onSendText: (String) -> Unit,
    onSendPhoto: (android.net.Uri) -> Unit,
    onStartVoice: () -> Unit,
    onOpenDraft: () -> Unit,
    onGoSettings: () -> Unit,
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

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) onSendPhoto(uri) }

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
                onTapTitle = onToggleHistory,
                onTapHistory = onToggleHistory,
                onTapNewChat = onNewChat,
                onTapManual = onOpenManual,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                    MessageRow(message = message, onOpenDraft = onOpenDraft)
                }
                if (state.busy) item { TypingIndicator() }
            }

            if (!state.aiAvailable) {
                NotConfiguredBanner(onGoSettings = onGoSettings)
            }
        }

        // Composer floats above the global control island. Control island
        // sits at navigationBarsPadding + 18dp + ~50dp tall; we sit at
        // 100dp so we're always at least 32dp clear of its top edge.
        Composer(
            input = input,
            onInputChange = { input = it },
            busy = state.busy,
            onSend = {
                onSendText(input)
                input = ""
            },
            onTakePhoto = ::launchPhotoFlow,
            onStartVoice = onStartVoice,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 100.dp, start = 14.dp, end = 14.dp),
        )

        if (historyOpen) {
            HistoryDropdown(
                conversations = conversations,
                onDismiss = onToggleHistory,
                onPick = { onToggleHistory() }, // noop until real persistence lands
                onNewChat = {
                    onToggleHistory()
                    onNewChat()
                },
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
    onTapTitle: () -> Unit,
    onTapHistory: () -> Unit,
    onTapNewChat: () -> Unit,
    onTapManual: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    val showSubtitle = title.isNotBlank() && title != DEFAULT_TITLE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 14.dp)
            .border(0.dp, Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RECORD",
                color = colors.ink,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MaterialTheme.typography.labelSmall.fontFamily,
                    letterSpacing = 4.sp,
                    fontSize = 22.sp,
                ),
            )
            if (showSubtitle) {
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onTapTitle)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
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
        }
        IconCircleButton(onClick = onTapHistory) { ClockGlyph(colors.ink) }
        IconCircleButton(onClick = onTapNewChat) { PlusGlyph(colors.ink) }
        ManualPill(onClick = onTapManual)
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
private fun ManualPill(onClick: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Transparent)
            .border(0.5.dp, colors.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ManualGlyph(colors.ink)
        Spacer(Modifier.width(5.dp))
        Text(
            text = "手动",
            color = colors.ink,
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
@Composable
private fun HistoryDropdown(
    conversations: List<FakeConversation>,
    onDismiss: () -> Unit,
    onPick: (FakeConversation) -> Unit,
    onNewChat: () -> Unit,
) {
    val colors = LocalTreasureColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink.copy(alpha = 0.18f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 60.dp, end = 14.dp, start = 14.dp)
                .widthIn(min = 240.dp, max = 290.dp)
                .shadow(12.dp, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.paper)
                .border(0.5.dp, colors.line, RoundedCornerShape(14.dp))
                .padding(vertical = 14.dp)
                .clickable(enabled = false) {},
        ) {
            HistoryHeader()
            Spacer(Modifier.height(10.dp))
            HistoryDivider(colors.line)
            conversations.forEach { c ->
                Spacer(Modifier.height(4.dp))
                HistoryRow(
                    conv = c,
                    onClick = { onPick(c) },
                )
                Spacer(Modifier.height(4.dp))
                HistoryDivider(colors.line)
            }
            Spacer(Modifier.height(6.dp))
            NewChatRow(onClick = onNewChat)
        }
    }
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
) {
    val colors = LocalTreasureColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (conv.current) colors.card else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun MessageRow(message: AddMessage, onOpenDraft: () -> Unit) {
    when (message) {
        is AddMessage.Assistant -> AssistantBubble(text = message.text)
        is AddMessage.User -> UserTextBubble(text = message.text)
        is AddMessage.UserPhoto -> UserPhotoBubble(uri = message.uri)
        is AddMessage.UserVoice -> UserVoiceBubble(text = message.text, duration = message.duration)
        is AddMessage.DraftCta -> DraftCtaCard(message = message, onOpen = onOpenDraft)
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
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
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
private fun UserPhotoBubble(uri: android.net.Uri) {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line),
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
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun DraftCtaCard(message: AddMessage.DraftCta, onOpen: () -> Unit) {
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
            category = template.category,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line)
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
            Text(
                text = "DRAFT · ${message.fieldCount} FIELDS",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "草稿已就绪",
                color = colors.ink,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "轻点过目，确认后收入图鉴",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )
        }
        Text(
            text = "→",
            color = colors.ink,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun TypingIndicator() {
    val colors = LocalTreasureColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 4.dp))
                .background(colors.card)
                .border(0.5.dp, colors.line)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = "正在思考…",
                color = colors.sub,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
            )
        }
    }
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
    onSend: () -> Unit,
    onTakePhoto: () -> Unit,
    onStartVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line, RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onTakePhoto),
            contentAlignment = Alignment.Center,
        ) { CameraGlyph(colors.sub) }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 28.dp, max = 96.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (input.isEmpty()) {
                Text(
                    text = "说说这件东西…",
                    color = colors.sub,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                cursorBrush = SolidColor(colors.terra),
                maxLines = 4,
                textStyle = LocalTextStyle.current.copy(
                    color = colors.ink,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(0.5.dp, colors.ink, CircleShape)
                .clickable(onClick = onStartVoice),
            contentAlignment = Alignment.Center,
        ) { MicGlyph(colors.ink) }
        Spacer(Modifier.width(6.dp))
        val canSend = input.isNotBlank() && !busy
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

@Composable
private fun NotConfiguredBanner(onGoSettings: () -> Unit) {
    val colors = LocalTreasureColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.card)
            .border(0.5.dp, colors.line)
            .clickable(onClick = onGoSettings)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "尚未配置 AI · ",
            color = colors.sub,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = "去设置",
            color = colors.terra,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "→",
            color = colors.terra,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
