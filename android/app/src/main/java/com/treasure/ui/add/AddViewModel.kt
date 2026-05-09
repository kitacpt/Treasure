package com.treasure.ui.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.ai.AiRole
import com.treasure.core.ai.AiTurn
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.repo.AddConversation
import com.treasure.core.repo.AddConversationMessage
import com.treasure.core.repo.AddConversationRepository
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.UUID

/**
 * One side of the chat — assistant prose, user text, attached photo,
 * voice playback, or the special "draft is ready" CTA card. Mirrors
 * `prototype/add-page-v2/project/direction-a.jsx::AddChat`.
 */
sealed interface AddMessage {
    data class Assistant(val text: String) : AddMessage
    data class User(val text: String) : AddMessage
    data class UserPhoto(val uri: Uri, val caption: String = "1 张照片") : AddMessage
    data class UserVoice(val text: String, val duration: String = "0:04") : AddMessage
    data class DraftCta(val draft: ItemDraft, val fieldCount: Int) : AddMessage
}

data class FakeConversation(
    val id: String,
    val title: String,
    val date: String,
    /** "HH:MM"，给标题做时间后缀用 — 多段 "New entry" 不能都长得一样。 */
    val time: String,
    val current: Boolean = false,
)

data class AddUiState(
    val conversationId: String = "",
    val messages: List<AddMessage> = emptyList(),
    val conversationTitle: String = DEFAULT_TITLE,
    val draft: ItemDraft? = null,
    val busy: Boolean = false,
    val errorMessage: String? = null,
    val aiAvailable: Boolean = false,
)

private const val DEFAULT_TITLE = "New entry"

class AddViewModel(
    application: Application,
    private val repo: ItemRepository,
    private val conversations: AddConversationRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AddUiState())
    val state: StateFlow<AddUiState> = _state.asStateFlow()

    /**
     * 历史抽屉里展示的最近 N 条对话。Cycle 0010 起从 Room 读，
     * cycle 0013 改成 combine — 之前用嵌套 `let { MutableStateFlow().also { collect } }`
     * 等于把同一份数据走两遍 collector，多了一层不必要的开销。
     */
    val recentConversations: StateFlow<List<FakeConversation>> =
        kotlinx.coroutines.flow.combine(
            conversations.observeRecent(limit = 20),
            _state.map { it.conversationId }.distinctUntilChanged(),
        ) { rows, currentId ->
            rows.map { c ->
                FakeConversation(
                    id = c.id,
                    title = c.title,
                    date = formatDate(c.updatedAt),
                    time = formatTime(c.updatedAt),
                    current = c.id == currentId,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        // 启动时新开一段对话，旧对话仍可从历史抽屉里翻出来。
        newConversation()
    }

    fun refreshAiAvailability() {
        val available = getApplication<TreasureApp>().settingsStore.hasKey()
        _state.update { it.copy(aiAvailable = available) }
    }

    fun newConversation() {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val key = getApplication<TreasureApp>().settingsStore.hasKey()
        // Cycle 0018：默认标题直接含 HH:MM 后缀，每段对话一进 db 就是
        // "New entry · 15:32" 唯一的；ChatHeader 副标和历史抽屉里的标题
        // 完全一致，用户切换时一眼就能看出。
        val title = "$DEFAULT_TITLE · ${formatTime(now)}"
        val opener = AddMessage.Assistant(GREETING)
        _state.value = AddUiState(
            conversationId = id,
            messages = listOf(opener),
            conversationTitle = title,
            aiAvailable = key,
        )
        viewModelScope.launch {
            conversations.upsert(
                AddConversation(id = id, title = title, createdAt = now, updatedAt = now),
            )
            persist(opener)
        }
    }

    /** 改对话名（历史抽屉里 ✎ 按钮）。 */
    fun renameConversation(id: String, newTitle: String) {
        val title = newTitle.trim().ifBlank { DEFAULT_TITLE }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            conversations.upsert(
                AddConversation(id = id, title = title, createdAt = now, updatedAt = now),
            )
            if (id == _state.value.conversationId) {
                _state.update { it.copy(conversationTitle = title) }
            }
        }
    }

    /** 删一段对话（历史抽屉里 ✕ 按钮）。如果删的是当前会话，就开一段新的。 */
    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversations.delete(id)
            if (id == _state.value.conversationId) {
                newConversation()
            }
        }
    }

    /**
     * 切到 history 抽屉里点的某段对话，从 Room 把它的消息全 reload。
     * `storedTitle` 来自 history row（已含 HH:MM 后缀），用作回退；如果消息里
     * 已经有 AI 生成的草稿，标题以草稿的 "Brand Model" 为准。
     */
    fun openConversation(id: String, storedTitle: String) {
        if (id == _state.value.conversationId) return
        viewModelScope.launch {
            val msgs = conversations.loadMessages(id).map(::toUiMessage)
            val draftTitle = msgs.firstNotNullOfOrNull {
                if (it is AddMessage.DraftCta) {
                    listOf(it.draft.brand, it.draft.model)
                        .filter { s -> s.isNotBlank() }
                        .joinToString(" ").ifBlank { null }
                } else null
            }
            _state.update {
                it.copy(
                    conversationId = id,
                    messages = msgs,
                    conversationTitle = draftTitle ?: storedTitle,
                    draft = msgs.lastOrNull { it is AddMessage.DraftCta }
                        ?.let { (it as AddMessage.DraftCta).draft },
                )
            }
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.busy) return
        appendMessage(AddMessage.User(trimmed))
        // Cycle 0020：如果文本里带 http(s) URL（多半是京东 / 淘宝分享过来），
        // 先把页面拉一下，给 AI 当 context；拉失败就只用原文。fetch 期间把
        // busy 拉起来，typing indicator 自然出现，不再 append 临时占位消息。
        val url = com.treasure.core.web.firstUrlIn(trimmed)
        if (url != null) {
            _state.update { it.copy(busy = true) }
            viewModelScope.launch {
                val app = getApplication<TreasureApp>()
                val fetched = app.pageFetcher.fetchText(url)
                val augmented = if (!fetched.isNullOrBlank()) {
                    """
                    用户在外部 app 分享了一条商品链接，原文：
                    $trimmed

                    [页面摘要]
                    $fetched

                    请基于摘要识别这件商品（品牌 / 型号 / 关键参数），不要把
                    URL 本身写进任何字段。
                    """.trimIndent()
                } else {
                    trimmed
                }
                // runExtract 会重新 set busy=true 并在结束时 set false，所以
                // 这里不必手动还原。
                runExtract(text = augmented, imageUri = null)
            }
        } else {
            runExtract(text = trimmed, imageUri = null)
        }
    }

    fun sendPhoto(uri: Uri) {
        if (_state.value.busy) return
        appendMessage(AddMessage.UserPhoto(uri))
        runExtract(text = "（用户附了一张照片，请识别）", imageUri = uri)
    }

    fun sendVoice(transcript: String) {
        if (_state.value.busy) return
        val text = transcript.trim().ifBlank { "（无识别结果）" }
        appendMessage(AddMessage.UserVoice(text))
        runExtract(text = "用户语音：$text", imageUri = null)
    }

    private fun appendMessage(msg: AddMessage) {
        _state.update {
            it.copy(messages = it.messages + msg, errorMessage = null)
        }
        viewModelScope.launch { persist(msg) }
    }

    private suspend fun persist(msg: AddMessage) {
        val convoId = _state.value.conversationId.ifBlank { return }
        val now = System.currentTimeMillis()
        val domain = toDomainMessage(msg, now)
        conversations.appendMessage(convoId, domain)
        conversations.upsert(
            AddConversation(
                id = convoId,
                title = _state.value.conversationTitle,
                createdAt = now, // upsert REPLACEs — title/updated 重写到最新
                updatedAt = now,
            ),
        )
    }

    private fun runExtract(text: String, imageUri: Uri?) {
        val app = getApplication<TreasureApp>()
        val client = app.aiClient()
        if (client == null) {
            appendMessage(
                AddMessage.Assistant(
                    "（还没配 API key — 在右下角的设置里填一下，再试。）",
                ),
            )
            _state.update { it.copy(aiAvailable = false) }
            return
        }
        _state.update { it.copy(busy = true) }
        val priorTurns = buildPriorTurns(_state.value.messages)
        viewModelScope.launch {
            val bytes = imageUri?.let { uri ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        app.contentResolver.openInputStream(uri)?.use { input ->
                            ByteArrayOutputStream().also { out -> input.copyTo(out) }.toByteArray()
                        }
                    }
                }.getOrNull()
            }
            client.extractItemDraft(
                text = text,
                imageJpegBytes = bytes,
                priorTurns = priorTurns,
            )
                .onSuccess { draft ->
                    val title = listOf(draft.brand, draft.model)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { _state.value.conversationTitle }
                    val assistantText = if (_state.value.draft == null) {
                        "好。我已经替你写好了一份草稿——要不要先看看？"
                    } else {
                        "我把刚才的修改更新到草稿里了，再看一眼？"
                    }
                    val assistant = AddMessage.Assistant(assistantText)
                    val cta = AddMessage.DraftCta(draft, fieldCount(draft))
                    _state.update {
                        it.copy(
                            messages = it.messages + assistant + cta,
                            draft = draft,
                            conversationTitle = title,
                            busy = false,
                        )
                    }
                    viewModelScope.launch {
                        persist(assistant)
                        persist(cta)
                    }
                }
                .onFailure { err ->
                    // 用户随口聊天（"你好"），模型不调 tool 而回普通文字 — 这
                    // 不是错误，把那段文字直接 surface 成助手消息即可。
                    val msg = if (err is com.treasure.core.ai.ChatOnlyResponseException) {
                        AddMessage.Assistant(err.text.ifBlank { "嗯。" })
                    } else {
                        AddMessage.Assistant("出错了：${err.message ?: "未知错误"}")
                    }
                    _state.update {
                        it.copy(
                            messages = it.messages + msg,
                            busy = false,
                            errorMessage = if (err is com.treasure.core.ai.ChatOnlyResponseException) null else err.message,
                        )
                    }
                    viewModelScope.launch { persist(msg) }
                }
        }
    }

    /** Inline edit on the preview screen. Promotes confidence to "high". */
    fun updateDraftField(field: PreviewField, value: String) {
        val current = _state.value.draft ?: return
        _state.value = _state.value.copy(draft = applyFieldEdit(current, field, value))
    }

    /** Persist the current draft as a real Item; returns id via callback. */
    fun commitDraft(onSaved: (String) -> Unit) {
        val draft = _state.value.draft ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val category = Category.fromId(draft.category ?: Category.TECH.id)
            val template = CategoryTemplates.forCategory(category)
            val id = makeId(category, draft.brand, draft.model, now)
            val acquired = readPurchaseField(draft, "入手日期").ifBlank { LocalDate.now().toString() }
            val item = Item(
                id = id,
                category = category,
                brand = draft.brand.trim(),
                model = draft.model.trim(),
                nickname = draft.nickname.trim(),
                acquired = acquired,
                parted = null,
                status = ItemStatus.OWNED,
                palette = template.palette,
                oneLiner = draft.oneLiner.trim(),
                heroVector = template.heroVector,
                specs = draft.specs.filter { it.label.isNotBlank() || it.value.isNotBlank() },
                history = listOf(
                    HistoryEvent(acquired, HistoryKind.ACQUIRED, "购入", ""),
                ),
                photos = emptyList(),
                createdAt = now,
                updatedAt = now,
            )
            repo.upsert(item)
            onSaved(id)
        }
    }

    /** Used by the manual entry pop-up — same shape as cycle 0006. */
    fun saveManual(
        template: CategoryTemplate,
        brand: String,
        model: String,
        nickname: String,
        acquired: String,
        oneLiner: String,
        status: ItemStatus,
        heroSpecValues: List<String>,
        heroVector: com.treasure.core.domain.HeroVector = template.heroVector,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val acquiredOrToday = acquired.ifBlank { LocalDate.now().toString() }
            val id = makeId(template.category, brand, model, now)
            val specs = template.heroSpecLabels.zip(heroSpecValues) { l, v ->
                HeroSpec(l, v.trim())
            }
            val item = Item(
                id = id,
                category = template.category,
                brand = brand.trim(),
                model = model.trim(),
                nickname = nickname.trim(),
                acquired = acquiredOrToday,
                parted = null,
                status = status,
                palette = template.palette,
                oneLiner = oneLiner.trim(),
                heroVector = heroVector,
                specs = specs,
                history = listOf(
                    HistoryEvent(acquiredOrToday, HistoryKind.ACQUIRED, "购入", ""),
                ),
                photos = emptyList(),
                createdAt = now,
                updatedAt = now,
            )
            repo.upsert(item)
            onSaved(id)
        }
    }

    private fun toDomainMessage(msg: AddMessage, now: Long): AddConversationMessage {
        val id = UUID.randomUUID().toString()
        return when (msg) {
            is AddMessage.Assistant -> AddConversationMessage.Assistant(id, msg.text, now)
            is AddMessage.User -> AddConversationMessage.User(id, msg.text, now)
            is AddMessage.UserPhoto -> AddConversationMessage.UserPhoto(id, msg.uri.toString(), now)
            is AddMessage.UserVoice -> AddConversationMessage.UserVoice(id, msg.text, msg.duration, now)
            is AddMessage.DraftCta -> AddConversationMessage.DraftCta(id, msg.draft, msg.fieldCount, now)
        }
    }

    private fun toUiMessage(domain: AddConversationMessage): AddMessage = when (domain) {
        is AddConversationMessage.Assistant -> AddMessage.Assistant(domain.text)
        is AddConversationMessage.User -> AddMessage.User(domain.text)
        is AddConversationMessage.UserPhoto -> AddMessage.UserPhoto(Uri.parse(domain.uri))
        is AddConversationMessage.UserVoice -> AddMessage.UserVoice(domain.text, domain.duration)
        is AddConversationMessage.DraftCta -> AddMessage.DraftCta(domain.draft, domain.fieldCount)
    }

    private fun buildPriorTurns(messages: List<AddMessage>): List<AiTurn> = messages
        .mapNotNull { msg ->
            when (msg) {
                is AddMessage.Assistant -> AiTurn(AiRole.ASSISTANT, msg.text)
                is AddMessage.User -> AiTurn(AiRole.USER, msg.text)
                is AddMessage.UserVoice -> AiTurn(AiRole.USER, "（语音）${msg.text}")
                is AddMessage.UserPhoto -> null // 图片单独走 image block，不放到上下文里
                is AddMessage.DraftCta -> AiTurn(
                    AiRole.ASSISTANT,
                    "已经替用户写出一份草稿，包含 ${msg.fieldCount} 个字段。",
                )
            }
        }
        .takeLast(20) // 控制 token 上限

    private fun buildOpener(now: Long): String {
        val time = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
        val hh = time.hour.toString().padStart(2, '0')
        val mm = time.minute.toString().padStart(2, '0')
        return "$GREETING\n（新对话 · $hh:$mm 开始）"
    }

    companion object {
        const val GREETING = "你好。把新东西的照片发给我，或者直接说说它是什么。"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                AddViewModel(app, app.repository, app.conversationRepository)
            }
        }
    }
}

private fun formatTime(epochMillis: Long): String {
    val zoned = java.time.Instant.ofEpochMilli(epochMillis)
        .atZone(java.time.ZoneId.systemDefault())
    val hh = zoned.hour.toString().padStart(2, '0')
    val mm = zoned.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}

private fun formatDate(epochMillis: Long): String {
    val instant = java.time.Instant.ofEpochMilli(epochMillis)
    val zoned = instant.atZone(java.time.ZoneId.systemDefault())
    val today = java.time.LocalDate.now()
    val that = zoned.toLocalDate()
    return when {
        that == today -> "今天"
        that == today.minusDays(1) -> "昨天"
        else -> that.toString()
    }
}

/**
 * The 9 preview fields the prototype enumerates. Map cleanly onto our
 * domain — first-class fields where they exist, [HeroSpec] entries
 * (looked up by label) for "purchase context" extras.
 */
enum class PreviewField(val label: String) {
    Category("品类"),
    Brand("品牌"),
    Model("型号"),
    Nickname("昵称"),
    Color("颜色"),
    AcquiredDate("入手日期"),
    AcquiredPrice("入手价格"),
    AcquiredChannel("入手渠道"),
    OneLiner("一句话"),
}

enum class Confidence { High, Medium, Low }

data class PreviewRow(val field: PreviewField, val value: String, val confidence: Confidence)

/** Pull a stable list of preview rows out of an [ItemDraft]. */
fun previewRowsFor(draft: ItemDraft): List<PreviewRow> {
    fun confOf(value: String): Confidence =
        if (value.isBlank()) Confidence.Low
        else if (value.length <= 2) Confidence.Medium
        else Confidence.High
    val color    = readPurchaseField(draft, "颜色")
    val date     = readPurchaseField(draft, "入手日期")
    val price    = readPurchaseField(draft, "入手价格")
    val channel  = readPurchaseField(draft, "入手渠道")
    val categoryDisplay = draft.category?.let {
        runCatching { Category.fromId(it).nameZh }.getOrNull()
    }.orEmpty()
    return listOf(
        PreviewRow(PreviewField.Category,        categoryDisplay,    confOf(categoryDisplay)),
        PreviewRow(PreviewField.Brand,           draft.brand,        confOf(draft.brand)),
        PreviewRow(PreviewField.Model,           draft.model,        confOf(draft.model)),
        PreviewRow(PreviewField.Nickname,        draft.nickname,     if (draft.nickname.isBlank()) Confidence.Low else Confidence.Medium),
        PreviewRow(PreviewField.Color,           color,              confOf(color)),
        PreviewRow(PreviewField.AcquiredDate,    date,               confOf(date)),
        PreviewRow(PreviewField.AcquiredPrice,   price,              confOf(price)),
        PreviewRow(PreviewField.AcquiredChannel, channel,            confOf(channel)),
        PreviewRow(PreviewField.OneLiner,        draft.oneLiner,     confOf(draft.oneLiner)),
    )
}

private fun readPurchaseField(draft: ItemDraft, label: String): String =
    draft.specs.firstOrNull { it.label == label }?.value.orEmpty()

private fun applyFieldEdit(draft: ItemDraft, field: PreviewField, value: String): ItemDraft = when (field) {
    PreviewField.Category -> {
        val matched = Category.entries.firstOrNull { it.nameZh == value || it.nameEn.equals(value, ignoreCase = true) }
        if (matched != null) draft.copy(category = matched.id)
        else draft
    }
    PreviewField.Brand    -> draft.copy(brand = value)
    PreviewField.Model    -> draft.copy(model = value)
    PreviewField.Nickname -> draft.copy(nickname = value)
    PreviewField.OneLiner -> draft.copy(oneLiner = value)
    PreviewField.Color, PreviewField.AcquiredDate, PreviewField.AcquiredPrice, PreviewField.AcquiredChannel -> {
        val newSpecs = draft.specs.toMutableList()
        val idx = newSpecs.indexOfFirst { it.label == field.label }
        if (idx >= 0) newSpecs[idx] = newSpecs[idx].copy(value = value)
        else newSpecs.add(HeroSpec(field.label, value))
        draft.copy(specs = newSpecs)
    }
}

private fun fieldCount(draft: ItemDraft): Int =
    previewRowsFor(draft).count { it.value.isNotBlank() }

private fun makeId(category: Category, brand: String, model: String, now: Long): String {
    val slug = "$brand-$model"
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
        .take(40)
    return "${category.id}-$slug-${(now / 1000) % 100000}"
}
