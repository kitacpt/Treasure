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
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.repo.AddConversation
import com.treasure.core.repo.AddConversationMessage
import com.treasure.core.repo.AddConversationRepository
import com.treasure.core.repo.DraftCtaStatus
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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

    /**
     * Cycle 0024：一次 AI 提案。[id] 是数据库 row id；UI 上根据它 mutate
     * status（采用 / 不要）时知道更新哪条。[status] 决定卡片的呈现：
     * Pending 给两个按钮；Accepted / Rejected 灰掉只显示历史。
     */
    data class DraftCta(
        val id: String,
        val draft: ItemDraft,
        val fieldCount: Int,
        val status: DraftCtaStatus,
    ) : AddMessage

    /** Cycle 0024：用户已"采用"某次 AI 提案后留下的草稿快照行。 */
    data class DraftConfirmed(val draft: ItemDraft, val fieldCount: Int) : AddMessage

    /**
     * 系统状态行 — 比如 "✓ 已抓取京东 · 1.2KB"、"⚠ 防爬挡住"。Cycle 0022
     * 加：用户报 URL fetch 没生效，但其实早静默执行了，没法定位是 fetch
     * 失败还是 AI 没用。把状态显式喷出来给用户看。
     *
     * 故意不持久化（不入 Room）也不喂回 AI 的 priorTurns —— 这是给人看的
     * 即时诊断，不是对话内容。重开历史时它就消失了，没问题。
     */
    data class SystemNote(val text: String, val tone: NoteTone = NoteTone.Info) : AddMessage
}

enum class NoteTone { Info, Working, Success, Warning, Error }

data class FakeConversation(
    val id: String,
    val title: String,
    val date: String,
    /** "HH:MM"，给标题做时间后缀用 — 多段 "New entry" 不能都长得一样。 */
    val time: String,
    val current: Boolean = false,
)

/**
 * Cycle 0024：一个会话 = 一个草稿。
 *   - [confirmedDraft]：用户已"采用"的最新版本，所有 AI 提案和"确认收入"
 *     都基于它做下一步。null 表示这段对话还没生成过任何被接受的草稿。
 *   - [proposedDraft]：AI 最新的提案，未被采用。用户点 [采用] 时它升格
 *     为 confirmedDraft；点 [不要] 时丢弃。同时 [pendingCtaId] 指向触发
 *     这次提案的 DraftCta，决定它什么时候置灰。
 */
data class AddUiState(
    val conversationId: String = "",
    val messages: List<AddMessage> = emptyList(),
    val conversationTitle: String = DEFAULT_TITLE,
    val confirmedDraft: ItemDraft? = null,
    val proposedDraft: ItemDraft? = null,
    val pendingCtaId: String? = null,
    val busy: Boolean = false,
    val errorMessage: String? = null,
    val aiAvailable: Boolean = false,
) {
    /** 在 Refine 页打开时实际编辑的草稿。优先 confirmed；都没有就给空白。 */
    val refineDraft: ItemDraft get() = confirmedDraft ?: ItemDraft()
}

private const val DEFAULT_TITLE = "New entry"

class AddViewModel(
    application: Application,
    private val repo: ItemRepository,
    private val conversations: AddConversationRepository,
    private val categoryRepo: com.treasure.core.repo.CategoryRepository,
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
        // Cycle 0022：默认续上最近一段对话，没有才新建。之前每次 VM 创建都
        // newConversation() 会在 history 抽屉里留一堆 "New entry · HH:MM" 空
        // 壳，用户反馈 "每次都创建一个新纪录很费解"。续上最后那条对话才是
        // 自然的入口。
        viewModelScope.launch {
            val recent = conversations.observeRecent(limit = 1).first()
            val latest = recent.firstOrNull()
            if (latest == null) {
                newConversation()
            } else {
                resumeConversation(latest)
            }
        }
    }

    private suspend fun resumeConversation(c: AddConversation) {
        val msgs = conversations.loadMessages(c.id).map(::toUiMessage)
        val effectiveMessages = if (msgs.isEmpty()) {
            listOf(AddMessage.Assistant(GREETING))
        } else {
            msgs
        }
        val drafts = deriveDraftsFromMessages(effectiveMessages)
        val key = getApplication<TreasureApp>().settingsStore.hasKey()
        _state.value = AddUiState(
            conversationId = c.id,
            messages = effectiveMessages,
            conversationTitle = c.title,
            confirmedDraft = drafts.confirmed,
            proposedDraft = drafts.proposed,
            pendingCtaId = drafts.pendingCtaId,
            aiAvailable = key,
        )
    }

    /**
     * 从消息流里推导出会话当前的 confirmed / proposed 状态。规则：
     *   - 最近一条 DraftConfirmed = confirmedDraft（用户接受过的最新版）
     *   - 在它之后 / 没有 DraftConfirmed 时整个历史里，最新一条 Pending 状态
     *     的 DraftCta = proposedDraft（待用户处理的 AI 提案）
     */
    private data class DerivedDrafts(
        val confirmed: ItemDraft?,
        val proposed: ItemDraft?,
        val pendingCtaId: String?,
    )

    private fun deriveDraftsFromMessages(msgs: List<AddMessage>): DerivedDrafts {
        var confirmed: ItemDraft? = null
        msgs.forEach { m ->
            if (m is AddMessage.DraftConfirmed) confirmed = m.draft
        }
        // pending = 最后一条 status=Pending 的 DraftCta（用户没采用 / 没拒绝）
        val pendingCta = msgs.lastOrNull {
            it is AddMessage.DraftCta && it.status == DraftCtaStatus.Pending
        } as AddMessage.DraftCta?
        return DerivedDrafts(
            confirmed = confirmed,
            proposed = pendingCta?.draft,
            pendingCtaId = pendingCta?.id,
        )
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

    /** 删一段对话（历史抽屉里 ✕ 按钮）。
     *  Cycle 0031：删的是当前会话时不再自动开"New entry · HH:MM"空壳 — 之
     *  前用户反馈"删了发现位置上又冒了一条新的，以为没删干净"。改成：把
     *  current 滑到剩下里最新一段；剩下都没了才 newConversation。 */
    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversations.delete(id)
            if (id == _state.value.conversationId) {
                val next = conversations.observeRecent(limit = 1).first().firstOrNull()
                if (next != null) resumeConversation(next) else newConversation()
            }
        }
    }

    /**
     * 切到 history 抽屉里点的某段对话，从 Room 把它的消息全 reload。
     * `storedTitle` 来自 history row，用作回退标题；如果消息里有 AI 草稿，
     * 以"品牌 型号"做标题更可读。
     */
    fun openConversation(id: String, storedTitle: String) {
        if (id == _state.value.conversationId) return
        viewModelScope.launch {
            val msgs = conversations.loadMessages(id).map(::toUiMessage)
            val drafts = deriveDraftsFromMessages(msgs)
            val titleFromDraft = (drafts.confirmed ?: drafts.proposed)?.let { d ->
                listOf(d.brand, d.model).filter { it.isNotBlank() }
                    .joinToString(" ").ifBlank { null }
            }
            _state.update {
                it.copy(
                    conversationId = id,
                    messages = msgs,
                    conversationTitle = titleFromDraft ?: storedTitle,
                    confirmedDraft = drafts.confirmed,
                    proposedDraft = drafts.proposed,
                    pendingCtaId = drafts.pendingCtaId,
                )
            }
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.busy) return
        appendMessage(AddMessage.User(trimmed))
        // Cycle 0020-0021：如果文本里带 http(s) URL（多半是京东 / 淘宝 / 拼
        // 多多分享过来），先把页面拉一下，给 AI 当 context；fetch 期间把 busy
        // 拉起来，typing indicator 自然出现。
        //
        // PageFetcher 返回三种结果：
        //   - Success：[页面摘要] 拼进 prompt
        //   - Blocked：拼多多 / 京东 等 app gate 或登录墙，明确告诉 AI 拉
        //     不到、别说自己 "无法访问外部 URL"，要它仅基于分享文字判断
        //   - Failed：网络 / HTTP 错误，同上
        val url = com.treasure.core.web.firstUrlIn(trimmed)
        if (url != null) {
            _state.update { it.copy(busy = true) }
            // Cycle 0022：用户报 fetch 没起效，其实早跑过了只是默不作声。把
            // "正在抓取" / 结果都喷成 SystemNote 给用户看，定位是 fetch 失
            // 败还是 AI 不识。
            val host = runCatching { java.net.URI(url).host.orEmpty() }
                .getOrDefault("")
                .ifBlank { "网页" }
            appendTransientNote(AddMessage.SystemNote("正在抓取 $host …", NoteTone.Working))
            viewModelScope.launch {
                val app = getApplication<TreasureApp>()
                val (augmented, note) = when (val result = app.pageFetcher.fetchText(url)) {
                    is com.treasure.core.web.FetchResult.Success -> {
                        val len = result.text.length
                        val sizeHint = if (len < 1000) "${len} 字"
                        else "${(len + 500) / 1000}K 字"
                        Pair(
                            buildPromptWithPage(trimmed, result.text),
                            AddMessage.SystemNote("✓ 已抓取 $host · $sizeHint", NoteTone.Success),
                        )
                    }
                    is com.treasure.core.web.FetchResult.Blocked ->
                        Pair(
                            buildPromptFetchBlocked(trimmed, result.host, result.reason),
                            AddMessage.SystemNote(
                                "⚠ ${result.host} 防爬挡住 · ${result.reason}",
                                NoteTone.Warning,
                            ),
                        )
                    is com.treasure.core.web.FetchResult.Failed ->
                        Pair(
                            buildPromptFetchFailed(trimmed, result.host, result.message),
                            AddMessage.SystemNote(
                                "⚠ ${result.host.ifBlank { "网页" }} 抓取失败 · ${result.message.take(60)}",
                                NoteTone.Error,
                            ),
                        )
                }
                replaceLastWorkingNote(note)
                // runExtract 会重新 set busy=true 并在结束时 set false。
                runExtract(text = augmented, imageUri = null)
            }
        } else {
            runExtract(text = trimmed, imageUri = null)
        }
    }

    /** SystemNote 不入 Room；只塞进 UI state。 */
    private fun appendTransientNote(note: AddMessage.SystemNote) {
        _state.update { it.copy(messages = it.messages + note) }
    }

    /** 把最后一条 Working 状态的 SystemNote 替成结果，避免堆一行 "正在抓取"
     *  + 一行 "✓ 抓到"。 */
    private fun replaceLastWorkingNote(replacement: AddMessage.SystemNote) {
        _state.update { state ->
            val lastIdx = state.messages.indexOfLast {
                it is AddMessage.SystemNote && it.tone == NoteTone.Working
            }
            if (lastIdx < 0) {
                state.copy(messages = state.messages + replacement)
            } else {
                val newList = state.messages.toMutableList().also {
                    it[lastIdx] = replacement
                }
                state.copy(messages = newList)
            }
        }
    }

    private fun buildPromptWithPage(userText: String, pageSummary: String) = """
        用户在外部 app 分享了一条商品链接，原文：
        $userText

        [页面摘要]
        $pageSummary

        请基于摘要识别这件商品（品牌 / 型号 / 关键参数），不要把 URL 本身
        写进任何字段。
    """.trimIndent()

    private fun buildPromptFetchBlocked(userText: String, host: String, reason: String) = """
        用户在外部 app 分享了一条商品链接，原文：
        $userText

        [系统提示] 客户端已经替你尝试拉取过这个 $host 页面，但被网站防爬挡
        住了 ($reason)。你**没有**实时访问外部 URL 的能力，但你有 Treasure
        客户端帮你抓的页面文字 — 这次没抓到。

        请：
        1. **不要**回复 "我无法访问外部链接" — 这是误导。客户端尝试过，是
           网站这边的限制，跟你的能力无关。
        2. 仅基于用户分享的原文里的关键词（商品名 / 价格 / 描述）判断商品。
        3. 如果信息不足，直接调用 fill_item_draft 把能填的字段填上、空字段
           留空，让用户在草稿里手动补。
    """.trimIndent()

    private fun buildPromptFetchFailed(userText: String, host: String, message: String) = """
        用户在外部 app 分享了一条商品链接，原文：
        $userText

        [系统提示] 客户端尝试拉取 $host 页面失败：$message。你**没有**实时
        访问外部 URL 的能力，请仅基于用户分享文字判断；如果信息不足，调用
        fill_item_draft 把能填的填上，空的留空。**不要**回 "无法访问外部
        链接"。
    """.trimIndent()

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
        // SystemNote 是给用户看的状态行，不存 Room、不喂 AI。
        val domain = toDomainMessage(msg, now) ?: return
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
        // Cycle 0024：baseline = 当前已确认的草稿；AI 在此基础上 propose 下一版
        val baseline = _state.value.confirmedDraft
        viewModelScope.launch {
            // Cycle 0027：把 manager 里能选的分类喂给 AI（内建 + 未隐藏自定义）
            val hints = categoryRepo.loadAll()
                .filter { !it.hidden }
                .map { com.treasure.core.ai.CategoryHint(it.id, it.nameZh, it.nameEn) }
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
                baseline = baseline,
                categoryHints = hints,
            )
                .onSuccess { draft ->
                    val title = listOf(draft.brand, draft.model)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .ifBlank { _state.value.conversationTitle }
                    val isFirst = _state.value.confirmedDraft == null
                    val assistantText = if (isFirst) {
                        "好。我替你写了一版草稿——要不要采用？"
                    } else {
                        "在你已确认的草稿上做了点修改——要不要采用？"
                    }
                    val assistant = AddMessage.Assistant(assistantText)
                    val ctaId = UUID.randomUUID().toString()
                    val cta = AddMessage.DraftCta(
                        id = ctaId,
                        draft = draft,
                        fieldCount = fieldCount(draft),
                        status = DraftCtaStatus.Pending,
                    )
                    _state.update {
                        // 之前的 pending CTA 如果还在，被新提案 supersede — 标
                        // 记为 Rejected（用户没采用就来了新的）。
                        val supersededMessages = it.messages.map { m ->
                            if (m is AddMessage.DraftCta && m.status == DraftCtaStatus.Pending) {
                                m.copy(status = DraftCtaStatus.Rejected)
                            } else m
                        }
                        it.copy(
                            messages = supersededMessages + assistant + cta,
                            proposedDraft = draft,
                            pendingCtaId = ctaId,
                            conversationTitle = title,
                            busy = false,
                        )
                    }
                    viewModelScope.launch {
                        // 先把被 supersede 的旧 pending CTA 状态 sync 回 Room
                        val current = _state.value.messages
                        current.forEach { m ->
                            if (m is AddMessage.DraftCta && m.status == DraftCtaStatus.Rejected) {
                                upsertCtaStatus(m)
                            }
                        }
                        persist(assistant)
                        persistDraftCtaWithId(cta)
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

    /**
     * Cycle 0024：用户点 DraftCta 上的 [采用]。把 proposed 升格为 confirmed，
     * 在 Room 里把那条 cta 状态置 Accepted，并 append 一行 DraftConfirmed
     * 快照（这样后续 AI 跑能看到正确 baseline）。
     */
    fun acceptProposal(ctaId: String) {
        val current = _state.value
        val cta = current.messages.firstOrNull {
            it is AddMessage.DraftCta && it.id == ctaId
        } as AddMessage.DraftCta? ?: return
        val confirmedSnapshot = AddMessage.DraftConfirmed(cta.draft, fieldCount(cta.draft))
        val newMessages = current.messages.map { m ->
            if (m is AddMessage.DraftCta && m.id == ctaId) {
                m.copy(status = DraftCtaStatus.Accepted)
            } else m
        } + confirmedSnapshot
        _state.update {
            it.copy(
                messages = newMessages,
                confirmedDraft = cta.draft,
                proposedDraft = null,
                pendingCtaId = null,
            )
        }
        viewModelScope.launch {
            val accepted = newMessages.firstOrNull {
                it is AddMessage.DraftCta && it.id == ctaId
            } as AddMessage.DraftCta?
            accepted?.let { upsertCtaStatus(it) }
            persist(confirmedSnapshot)
        }
    }

    /** Cycle 0024：用户点 [不要]。clear proposed，不写 DraftConfirmed 快照。 */
    fun rejectProposal(ctaId: String) {
        val current = _state.value
        val newMessages = current.messages.map { m ->
            if (m is AddMessage.DraftCta && m.id == ctaId) {
                m.copy(status = DraftCtaStatus.Rejected)
            } else m
        }
        _state.update {
            it.copy(
                messages = newMessages,
                proposedDraft = if (it.pendingCtaId == ctaId) null else it.proposedDraft,
                pendingCtaId = if (it.pendingCtaId == ctaId) null else it.pendingCtaId,
            )
        }
        viewModelScope.launch {
            val rejected = newMessages.firstOrNull {
                it is AddMessage.DraftCta && it.id == ctaId
            } as AddMessage.DraftCta?
            rejected?.let { upsertCtaStatus(it) }
        }
    }

    /** Cycle 0024：Refine 页 inline edit 直接落到 confirmedDraft；手动操作
     *  不需要走"提案 / 采用"循环 — 用户改的就是最终态。 */
    fun updateDraftField(field: PreviewField, value: String) {
        val current = _state.value.confirmedDraft ?: ItemDraft()
        _state.value = _state.value.copy(confirmedDraft = applyFieldEdit(current, field, value))
    }

    fun updateDraftSpec(idx: Int, spec: HeroSpec) {
        val current = _state.value.confirmedDraft ?: return
        if (idx < 0 || idx >= current.specs.size) return
        val newSpecs = current.specs.toMutableList().also { it[idx] = spec }
        _state.value = _state.value.copy(confirmedDraft = current.copy(specs = newSpecs))
    }

    fun addDraftSpec() {
        val current = _state.value.confirmedDraft ?: ItemDraft()
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(specs = current.specs + HeroSpec("", "")),
        )
    }

    fun removeDraftSpec(idx: Int) {
        val current = _state.value.confirmedDraft ?: return
        if (idx < 0 || idx >= current.specs.size) return
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(specs = current.specs.toMutableList().also { it.removeAt(idx) }),
        )
    }

    /** Cycle 0031：草稿页历史时间轴整段替换。AddPreview 加 / 改 / 删一行
     *  都通过这一个出口。 */
    fun setDraftHistory(history: List<HistoryEvent>) {
        val current = _state.value.confirmedDraft ?: ItemDraft()
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(history = history.sortedBy { it.date }),
        )
    }

    /** 进入手动 Refine 模式时调用：如果没有 confirmedDraft，建一个空草稿
     *  来让 UI 有东西可编。 */
    fun ensureDraftForManual() {
        if (_state.value.confirmedDraft == null) {
            _state.value = _state.value.copy(confirmedDraft = ItemDraft())
        }
    }

    /**
     * Persist the current draft as a real Item; returns id via callback.
     * Cycle 0023：[status] 由用户在草稿页选（默认 OWNED）；不再写死。
     * Cycle 0024：草稿是 [AddUiState.confirmedDraft]（用户已采用的最新版）。
     */
    fun commitDraft(
        status: ItemStatus = ItemStatus.OWNED,
        onSaved: (String) -> Unit,
    ) {
        val draft = _state.value.confirmedDraft ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Cycle 0027：category 现在是 String id。优先用 draft 里的；空就
            // 默认 "tech"（最泛的内建分类）。如果命中内建 enum，用对应模板
            // 拿 palette / heroVector；不命中（自定义分类）就走 GENERIC 套
            // generic palette。
            val categoryId = draft.category?.takeIf { it.isNotBlank() } ?: "tech"
            val builtIn = Category.entries.firstOrNull { it.id == categoryId }
            val template = builtIn?.let { CategoryTemplates.forCategory(it) }
            val palette = template?.palette
                ?: listOf("#0e0e0e", "#a47836", "#e8e2d4", "#5a5a5a")
            val heroVector = template?.heroVector ?: HeroVector.GENERIC
            val id = makeId(categoryId, draft.brand, draft.model, now)
            // 还是优先看 AI 填没填 "入手日期" spec；没填就今天。手动改的也会
            // 体现在 spec 列表里，于是这里能拿到。
            val acquired = readPurchaseField(draft, "入手日期").ifBlank { LocalDate.now().toString() }
            val item = Item(
                id = id,
                category = categoryId,
                brand = draft.brand.trim(),
                model = draft.model.trim(),
                nickname = draft.nickname.trim(),
                acquired = acquired,
                parted = null,
                status = status,
                palette = palette,
                oneLiner = draft.oneLiner.trim(),
                heroVector = heroVector,
                specs = draft.specs.filter { it.label.isNotBlank() || it.value.isNotBlank() },
                // Cycle 0031：草稿页用户能编辑 history 时间轴；用户填了就直接
                // 用，没填就走老逻辑默认一条 ACQUIRED。
                history = draft.history.ifEmpty {
                    listOf(HistoryEvent(acquired, HistoryKind.ACQUIRED, "购入", ""))
                },
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
            val id = makeId(template.category.id, brand, model, now)
            val specs = template.heroSpecLabels.zip(heroSpecValues) { l, v ->
                HeroSpec(l, v.trim())
            }
            val item = Item(
                id = id,
                category = template.category.id,
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

    private fun toDomainMessage(msg: AddMessage, now: Long): AddConversationMessage? {
        val id = UUID.randomUUID().toString()
        return when (msg) {
            is AddMessage.Assistant -> AddConversationMessage.Assistant(id, msg.text, now)
            is AddMessage.User -> AddConversationMessage.User(id, msg.text, now)
            is AddMessage.UserPhoto -> AddConversationMessage.UserPhoto(id, msg.uri.toString(), now)
            is AddMessage.UserVoice -> AddConversationMessage.UserVoice(id, msg.text, msg.duration, now)
            is AddMessage.DraftCta -> AddConversationMessage.DraftCta(
                id = msg.id, // 保留 UI 已分配的 id，让后续 status 更新能找到同一行
                draft = msg.draft,
                fieldCount = msg.fieldCount,
                status = msg.status,
                createdAt = now,
            )
            is AddMessage.DraftConfirmed -> AddConversationMessage.DraftConfirmed(
                id = id, draft = msg.draft, fieldCount = msg.fieldCount, createdAt = now,
            )
            is AddMessage.SystemNote -> null
        }
    }

    private fun toUiMessage(domain: AddConversationMessage): AddMessage = when (domain) {
        is AddConversationMessage.Assistant -> AddMessage.Assistant(domain.text)
        is AddConversationMessage.User -> AddMessage.User(domain.text)
        is AddConversationMessage.UserPhoto -> AddMessage.UserPhoto(Uri.parse(domain.uri))
        is AddConversationMessage.UserVoice -> AddMessage.UserVoice(domain.text, domain.duration)
        is AddConversationMessage.DraftCta -> AddMessage.DraftCta(
            id = domain.id,
            draft = domain.draft,
            fieldCount = domain.fieldCount,
            status = domain.status,
        )
        is AddConversationMessage.DraftConfirmed -> AddMessage.DraftConfirmed(
            draft = domain.draft, fieldCount = domain.fieldCount,
        )
    }

    /** 把已有 UI 行的 DraftCta 状态 sync 回 Room（upsert by id）。 */
    private suspend fun upsertCtaStatus(cta: AddMessage.DraftCta) {
        val convoId = _state.value.conversationId.ifBlank { return }
        val now = System.currentTimeMillis()
        conversations.appendMessage(
            convoId,
            AddConversationMessage.DraftCta(
                id = cta.id,
                draft = cta.draft,
                fieldCount = cta.fieldCount,
                status = cta.status,
                createdAt = now,
            ),
        )
    }

    /** 新建一条 DraftCta，使用 UI 提前生成的 id（让 [acceptProposal] /
     *  [rejectProposal] 后续能找到同一行做状态更新）。 */
    private suspend fun persistDraftCtaWithId(cta: AddMessage.DraftCta) {
        val convoId = _state.value.conversationId.ifBlank { return }
        val now = System.currentTimeMillis()
        conversations.appendMessage(
            convoId,
            AddConversationMessage.DraftCta(
                id = cta.id,
                draft = cta.draft,
                fieldCount = cta.fieldCount,
                status = cta.status,
                createdAt = now,
            ),
        )
        conversations.upsert(
            AddConversation(
                id = convoId,
                title = _state.value.conversationTitle,
                createdAt = now,
                updatedAt = now,
            ),
        )
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
                    when (msg.status) {
                        DraftCtaStatus.Pending -> "已经替用户写出一份草稿（含 ${msg.fieldCount} 个字段），等用户采用。"
                        DraftCtaStatus.Accepted -> "替用户写的草稿（${msg.fieldCount} 字段）已被采用。"
                        DraftCtaStatus.Rejected -> "替用户写的草稿（${msg.fieldCount} 字段）被用户拒绝了；下次注意。"
                    },
                )
                is AddMessage.DraftConfirmed -> null // baseline 已经在 system prompt 里
                is AddMessage.SystemNote -> null
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
                AddViewModel(app, app.repository, app.conversationRepository, app.categoryRepository)
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
 * Cycle 0023：草稿页改成 Edit 页同款排版，PreviewField 缩到只剩 5 个一级
 * 字段（其余 specs 都让 AI 自由发挥，AddPreview 直接渲染 draft.specs）。
 */
enum class PreviewField(val label: String) {
    Category("品类"),
    Brand("品牌"),
    Model("型号"),
    Nickname("昵称"),
    OneLiner("一句话"),
}

private fun applyFieldEdit(draft: ItemDraft, field: PreviewField, value: String): ItemDraft = when (field) {
    // Cycle 0027：Category 现在直接传 String id（dropdown 选项是 CategoryInfo，
    // 回调 onSelect 传 it.id）。兼容老调用方传中文 / 英文名也匹配一下内建 enum。
    PreviewField.Category -> {
        val matched = Category.entries.firstOrNull {
            it.id == value || it.nameZh == value || it.nameEn.equals(value, ignoreCase = true)
        }
        when {
            matched != null -> draft.copy(category = matched.id)
            value.isNotBlank() -> draft.copy(category = value)
            else -> draft
        }
    }
    PreviewField.Brand    -> draft.copy(brand = value)
    PreviewField.Model    -> draft.copy(model = value)
    PreviewField.Nickname -> draft.copy(nickname = value)
    PreviewField.OneLiner -> draft.copy(oneLiner = value)
}

private fun readPurchaseField(draft: ItemDraft, label: String): String =
    draft.specs.firstOrNull { it.label == label }?.value.orEmpty()

private fun fieldCount(draft: ItemDraft): Int {
    val first = listOf(draft.brand, draft.model, draft.nickname, draft.oneLiner, draft.category.orEmpty())
        .count { it.isNotBlank() }
    val specs = draft.specs.count { it.label.isNotBlank() && it.value.isNotBlank() }
    return first + specs
}

private fun makeId(categoryId: String, brand: String, model: String, now: Long): String {
    val slug = "$brand-$model"
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
        .take(40)
    // categoryId 已是 url-safe slug（内建 6 个固定 id；自定义是 "custom-uuid"）
    return "$categoryId-$slug-${(now / 1000) % 100000}"
}
