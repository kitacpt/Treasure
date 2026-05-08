package com.treasure.ui.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate

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
    val current: Boolean = false,
)

data class AddUiState(
    val messages: List<AddMessage> = emptyList(),
    val conversationTitle: String = "New entry",
    val draft: ItemDraft? = null,
    val busy: Boolean = false,
    val errorMessage: String? = null,
    val aiAvailable: Boolean = false,
)

class AddViewModel(
    application: Application,
    private val repo: ItemRepository,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(initial())
    val state: StateFlow<AddUiState> = _state.asStateFlow()

    /**
     * Pretend conversation history — wired to a real persistence layer in a
     * later cycle. Today these strings live in memory only.
     */
    val recentConversations: List<FakeConversation> = listOf(
        FakeConversation("c1", "New entry", "今天", current = true),
    )

    private fun initial(): AddUiState {
        val app = getApplication<TreasureApp>()
        val key = app.settingsStore.hasKey()
        return AddUiState(
            messages = listOf(AddMessage.Assistant(GREETING)),
            aiAvailable = key,
        )
    }

    fun refreshAiAvailability() {
        val available = getApplication<TreasureApp>().settingsStore.hasKey()
        _state.update { it.copy(aiAvailable = available) }
    }

    fun newConversation() {
        _state.value = initial()
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.busy) return
        _state.update {
            it.copy(
                messages = it.messages + AddMessage.User(trimmed),
                errorMessage = null,
            )
        }
        runExtract(text = trimmed, imageUri = null)
    }

    fun sendPhoto(uri: Uri) {
        if (_state.value.busy) return
        _state.update {
            it.copy(
                messages = it.messages + AddMessage.UserPhoto(uri),
                errorMessage = null,
            )
        }
        runExtract(text = "（用户附了一张照片，请识别）", imageUri = uri)
    }

    /**
     * Add a voice message — transcript comes from the real
     * [SpeechRecognizer] now. Falls back to a stub line if the device
     * has no recognition service or the user simply tapped to dismiss.
     */
    fun sendVoice(transcript: String) {
        if (_state.value.busy) return
        val text = transcript.trim().ifBlank { "（无识别结果）" }
        _state.update {
            it.copy(messages = it.messages + AddMessage.UserVoice(text))
        }
        runExtract(text = "用户语音：$text", imageUri = null)
    }

    private fun runExtract(text: String, imageUri: Uri?) {
        val app = getApplication<TreasureApp>()
        val client = app.aiClient()
        if (client == null) {
            _state.update {
                it.copy(
                    messages = it.messages + AddMessage.Assistant(
                        "（还没配 API key — 在右下角的设置里填一下，再试。）",
                    ),
                    aiAvailable = false,
                )
            }
            return
        }
        _state.update { it.copy(busy = true) }
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
            client.extractItemDraft(text = text, imageJpegBytes = bytes)
                .onSuccess { draft ->
                    val title = listOf(draft.brand, draft.model)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { "新草稿" }
                    _state.update {
                        it.copy(
                            messages = it.messages +
                                AddMessage.Assistant("好。我已经替你写好了一份草稿——要不要先看看？") +
                                AddMessage.DraftCta(draft, fieldCount(draft)),
                            draft = draft,
                            conversationTitle = title,
                            busy = false,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            messages = it.messages + AddMessage.Assistant("出错了：${err.message ?: "未知错误"}"),
                            busy = false,
                            errorMessage = err.message,
                        )
                    }
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
                heroVector = template.heroVector,
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

    companion object {
        const val GREETING = "你好。把新东西的照片发给我，或者直接说说它是什么。"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                AddViewModel(app, app.repository)
            }
        }
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
