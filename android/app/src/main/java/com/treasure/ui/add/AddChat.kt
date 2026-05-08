package com.treasure.ui.add

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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.treasure.illust.HeroIllustration
import com.treasure.theme.LocalTreasureColors

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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader(
                title = state.conversationTitle,
                onTapTitle = onToggleHistory,
                onTapHistory = onToggleHistory,
                onTapNewChat = onNewChat,
                onTapManual = onOpenManual,
            )

            // Messages list — bottom padding accounts for the composer (about
            // 64dp tall) + control island room below.
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    horizontal = 22.dp,
                    vertical = 18.dp,
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

        // Composer — sits ABOVE the global control island. Control island
        // lives in TreasureNavHost with `bottom = nav bar + 18dp`; composer
        // adds another ~64dp on top of that.
        Composer(
            input = input,
            onInputChange = { input = it },
            busy = state.busy,
            onSend = {
                onSendText(input)
                input = ""
            },
            onTakePhoto = {
                pickPhoto.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            },
            onStartVoice = onStartVoice,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 88.dp, start = 14.dp, end = 14.dp),
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 14.dp, top = 18.dp, bottom = 14.dp)
            .border(0.dp, Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "录入",
                    color = colors.ink,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onTapTitle)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
        drawCircle(color, radius = r, center = Offset(cx, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()))
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
                .padding(top = 60.dp, end = 14.dp)
                .widthIn(min = 220.dp, max = 260.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.paper)
                .border(0.5.dp, colors.line)
                .clickable(enabled = false) {},
        ) {
            Text(
                text = "历史对话",
                color = colors.sub,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
            conversations.forEach { c ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (c.current) colors.card else Color.Transparent)
                        .clickable { onPick(c) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = c.title,
                        color = colors.ink,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = c.date + (if (c.current) " · 当前" else ""),
                        color = colors.sub,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(colors.line))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNewChat)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlusGlyph(colors.ink)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "新对话",
                    color = colors.ink,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
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

// ─── composer + voice overlay ─────────────────────────────────────────

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
        Box(modifier = Modifier.weight(1f).heightIn(min = 28.dp), contentAlignment = Alignment.CenterStart) {
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
        // body
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(w * 0.15f, h * 0.30f),
            size = Size(w * 0.70f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(sw),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.30f),
            size = Size(w * 0.70f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(sw),
        )
        // lens
        drawCircle(
            color = color,
            radius = h * 0.15f,
            center = Offset(w * 0.50f, h * 0.55f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(sw),
        )
        // viewfinder bump
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
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(sw),
        )
        // arc
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

// ─── voice overlay ────────────────────────────────────────────────────

@Composable
internal fun VoiceOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC1A1815))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 130.dp),
        ) {
            Waveform(
                color = Color(0xFFF4F1EA),
                bars = listOf(18, 32, 46, 28, 54, 40, 22, 50, 36, 26, 42, 30, 48, 34, 20),
                modifier = Modifier
                    .height(60.dp)
                    .width(140.dp),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "\"二零二三年情人节，一万二千五…\"",
                color = Color(0xFFF4F1EA),
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "松开发送 · TAP TO STOP",
                color = Color(0xFFF4F1EA).copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
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
