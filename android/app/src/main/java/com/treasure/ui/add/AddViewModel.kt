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
import kotlinx.coroutines.flow.collectLatest
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
    data class UserVoice(
        val text: String,
        val duration: String = "0:04",
        /** Cycle 0034 v2：本地音频文件路径 — 气泡点击播放 / VM 送给 AI 用。
         *  老消息（cycle 0008-0033 转写流）audioPath = null，仅展示文本。 */
        val audioPath: String? = null,
    ) : AddMessage

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
        /** Cycle 0032：[com.treasure.core.repo.DraftCtaActionKind.Create] = 新增；
         *  Modify = 修改 [targetCiId] 指向的工作集行。 */
        val actionKind: com.treasure.core.repo.DraftCtaActionKind = com.treasure.core.repo.DraftCtaActionKind.Create,
        val targetCiId: String? = null,
        /** Cycle 0034：AI 给本卡分配的图。每条 (sourceUri, crop?, isAvatar)。
         *  采用时按这个列表拷贝 / 裁剪到 draft.photos / avatarPhotoPath。
         *  存在 Room 里（draft_json 同字段编码）让重启后采用仍能找到原图。 */
        val photoAssignments: List<ResolvedPhotoAssignment> = emptyList(),
    ) : AddMessage

    /** Cycle 0024：用户已"采用"某次 AI 提案后留下的草稿快照行。
     *  Cycle 0031：加 id，让 Draft 页编辑能 upsert 同一行（实时保存）。 */
    data class DraftConfirmed(
        val id: String,
        val draft: ItemDraft,
        val fieldCount: Int,
    ) : AddMessage

    /** Cycle 0031：点 [确认收入] 后的封存标记 — 这条会话从此只读。 */
    data class Committed(val id: String, val savedItemId: String) : AddMessage

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

/** Cycle 0034：UI 这边复用 core repo 的 [com.treasure.core.repo.ResolvedPhotoAssignment]。 */
typealias ResolvedPhotoAssignment = com.treasure.core.repo.ResolvedPhotoAssignment

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
    val busyStartedAt: Long? = null,
    val lastElapsedMs: Long? = null,
    /** Cycle 0031：会话工作集 — 这次会话里 user / AI 攒下的所有候选物品，
     *  PENDING / SAVED / MODIFIED 三态。drawer 渲染、AI baseline 都看这个。 */
    val items: List<com.treasure.core.repo.ConversationItem> = emptyList(),
    /** Cycle 0031：用户在 drawer 里点开某行 PENDING / MODIFIED 草稿编辑 →
     *  这里记下来，commit / 增量 upsert 都 prefer 这个 ciId（而不是"最近一条
     *  PENDING / MODIFIED"的启发式）。null 表示没显式选中，走启发式。 */
    val editingCiId: String? = null,
) {
    val refineDraft: ItemDraft get() = confirmedDraft ?: ItemDraft()
    /** Cycle 0031：commit 后封存 — 现在 commit 是 per-item，整个 conversation
     *  不再锁，所以这两个永远是 false / null。保留以兼容 UI。 */
    val saved: Boolean get() = false
    val savedItemId: String? get() = null
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

    // Cycle 0031：用户按"停止"时拿这两个 ref 掐请求 — viewModelScope 里的
    // coroutine + OkHttp client 都要 cancel 才彻底断。
    private var currentExtractJob: kotlinx.coroutines.Job? = null
    private var currentClient: com.treasure.core.ai.AiClient? = null
    // Cycle 0034：最近一轮 user-turn 发出的 photo URIs（持久化后的 file://）。
    // AI 返回的 photo_assignments 里 source_index 就对应这里的下标。Accept 一
    // 张 cta 卡时按 index 抄到 draft.photos。
    private var lastTurnPhotoUris: List<android.net.Uri> = emptyList()

    /** Cycle 0031：用户在思考态点 [⬛] 停止 — 只 cancel OkHttp 不 cancel
     *  coroutine。OkHttp 抛 IOException("Canceled") → runCatching 捕获 →
     *  onFailure 走 cancelled 分支清 busy。如果同时 cancel coroutine，
     *  CancellationException 绕过 runCatching，cleanup 不执行 busy 卡住。 */
    fun stopExtract() {
        currentClient?.cancel()
    }

    // ─── Cycle 0031：会话工作集操作 ─────────────────────────────────────

    /** 从图鉴里挑一件已有物品加入会话工作集 — 直接 SAVED。 */
    fun addExistingItem(itemId: String) {
        val convoId = _state.value.conversationId.ifBlank { return }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val order = conversations.nextSortOrder(convoId)
            conversations.upsertItem(
                com.treasure.core.repo.ConversationItem(
                    id = UUID.randomUUID().toString(),
                    conversationId = convoId,
                    draft = null,
                    itemRef = itemId,
                    status = com.treasure.core.repo.ConversationItemStatus.SAVED,
                    sortOrder = order,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    /** Cycle 0031：用户在 drawer 里点开某行 PENDING / MODIFIED 草稿编辑 →
     *  把该行的 draft 灌到 confirmedDraft（让 AddPreview 渲染），并记下
     *  editingCiId 锁定后续 commit / 增量 upsert 走这一行。 */
    fun openWorkingItem(ciId: String) {
        val ci = _state.value.items.firstOrNull { it.id == ciId } ?: return
        val baseDraft = ci.draft ?: ItemDraft()
        _state.update { it.copy(confirmedDraft = baseDraft, editingCiId = ciId) }
        // Cycle 0033：MODIFIED 行底下挂着真物品；photos / avatar 还在 Item 上，
        // 不在 draft 里。Refine 页要让用户也能直接管这部分影集，所以异步把
        // Item 的 photos / avatar 预填进 confirmedDraft（仅当 draft 自己没填
        // 时 — 避免覆盖用户已有的草稿改动）。
        val ref = ci.itemRef
        if (ref.isNullOrBlank()) return
        viewModelScope.launch {
            val item = runCatching { repo.observeById(ref).first() }.getOrNull() ?: return@launch
            _state.update { s ->
                val cur = s.confirmedDraft ?: baseDraft
                if (cur.photos.isNotEmpty()) s
                else s.copy(
                    confirmedDraft = cur.copy(
                        photos = item.photos,
                        avatarPhotoPath = item.avatarPhotoPath,
                    ),
                )
            }
        }
    }

    /** Cycle 0031：从 Refine 页退出时清掉锁定，下次 AI 走"最近一条 PENDING /
     *  MODIFIED"的启发式。 */
    fun clearEditing() {
        _state.update { it.copy(editingCiId = null) }
    }

    /** 长按删除 — Pending/Modified 整行删掉草稿；Saved 仅从工作集移除（不删
     *  图鉴里的实际物品）。 */
    fun removeWorkingItem(ciId: String) {
        viewModelScope.launch {
            // PENDING 直接 delete；MODIFIED 需要变回 SAVED（保留 itemRef 清掉
            // draft）；SAVED 直接 delete。
            val item = _state.value.items.firstOrNull { it.id == ciId } ?: return@launch
            when (item.status) {
                com.treasure.core.repo.ConversationItemStatus.PENDING,
                com.treasure.core.repo.ConversationItemStatus.SAVED -> {
                    conversations.deleteItem(ciId)
                }
                com.treasure.core.repo.ConversationItemStatus.MODIFIED -> {
                    // 用户语义："删除新修改 = 废除草稿"，回到 SAVED 状态保留
                    // 物品 ref。
                    conversations.upsertItem(
                        item.copy(
                            draft = null,
                            status = com.treasure.core.repo.ConversationItemStatus.SAVED,
                            updatedAt = System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
    }

    /** AI 提了一份新草稿（或 acceptProposalWithEdits 用户微改后采用）→ 落进
     *  工作集。规则：
     *  - 若 convo 里已有 PENDING / MODIFIED 行 → 更新该行 draft，保留 status
     *    与 itemRef（MODIFIED 行 itemRef 非空，代表"对已收入物品的下一版"）。
     *  - 否则 → 新建 PENDING 行。
     *
     *  这是单 action AI 流（一次回复 → 一份草稿）的简化映射。多 action AI
     *  会绕开此方法，按 action 数组各自 upsert。 */
    private suspend fun upsertWorkingDraft(draft: ItemDraft): String {
        val convoId = _state.value.conversationId.ifBlank { return "" }
        val now = System.currentTimeMillis()
        // 直接查 Room 而不是 _state.value.items，避免 observed Flow 还没回灌
        // 时的 race（AI 接连两次返回 → 两条 PENDING 并存）。
        val existing = conversations.loadItems(convoId)
        val editing = _state.value.editingCiId
        val active = if (editing != null) existing.firstOrNull { it.id == editing }
            else existing.lastOrNull {
                it.status == com.treasure.core.repo.ConversationItemStatus.PENDING ||
                    it.status == com.treasure.core.repo.ConversationItemStatus.MODIFIED
            }
        return if (active != null) {
            conversations.upsertItem(active.copy(draft = draft, updatedAt = now))
            active.id
        } else {
            val id = UUID.randomUUID().toString()
            val order = conversations.nextSortOrder(convoId)
            conversations.upsertItem(
                com.treasure.core.repo.ConversationItem(
                    id = id,
                    conversationId = convoId,
                    draft = draft,
                    itemRef = null,
                    status = com.treasure.core.repo.ConversationItemStatus.PENDING,
                    sortOrder = order,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            id
        }
    }

    // ─── Cycle 0032：多 action 喂给 AI 的工作集摘要 ─────────────────────

    /** 工作集喂给 AI 的简要摘要 — 每行只含识别核心字段，控住 token。 */
    private suspend fun buildWorkingSetSummary(
        items: List<com.treasure.core.repo.ConversationItem>,
    ): List<com.treasure.core.ai.WorkingItemSummary> {
        if (items.isEmpty()) return emptyList()
        // 拉一份 items 快照给 SAVED 行做摘要。flow.first() 取当前值就走。
        val byId = if (items.any { it.status == com.treasure.core.repo.ConversationItemStatus.SAVED }) {
            repo.items.first().associateBy { it.id }
        } else emptyMap()
        return items.map { ci ->
            val saved = ci.itemRef?.let { byId[it] }
            val draft = ci.draft
            val title = when {
                saved != null -> listOf(saved.brand, saved.model)
                    .filter { it.isNotBlank() }.joinToString(" ")
                    .ifBlank { saved.nickname }.ifBlank { "(无名)" }
                draft != null -> listOf(draft.brand, draft.model)
                    .filter { it.isNotBlank() }.joinToString(" ")
                    .ifBlank { draft.nickname }.ifBlank { "(草稿)" }
                else -> "(空)"
            }
            com.treasure.core.ai.WorkingItemSummary(
                id = ci.id,
                status = ci.status.name,
                title = title,
                category = saved?.category ?: draft?.category,
                oneLiner = saved?.oneLiner ?: draft?.oneLiner.orEmpty(),
                specs = saved?.specs ?: draft?.specs.orEmpty(),
            )
        }
    }

    /** Chat 里给用户看的多 action 摘要 — 引导用户逐张采用 / 不要。 */
    private fun buildAssistantSummary(actions: List<com.treasure.core.ai.DraftAction>): String {
        val creates = actions.count { it.kind == com.treasure.core.ai.ActionKind.CREATE }
        val modifies = actions.count { it.kind == com.treasure.core.ai.ActionKind.MODIFY }
        val parts = mutableListOf<String>()
        if (creates > 0) parts += "新增 ${creates} 件"
        if (modifies > 0) parts += "修改 ${modifies} 件"
        val body = parts.joinToString(" · ")
        return if (body.isEmpty()) "嗯。"
        else "好。给你提了 ${actions.size} 张草稿（$body）— 下方逐张采用或不要。采用后会进右上工作集，再点进去逐一录入。"
    }

    /** commit 之后把当前活跃 ciId（最近 PENDING / MODIFIED）升格为 SAVED。
     *  返回升格的 ciId 让上层知道是哪一行。 */
    private suspend fun markActiveItemSaved(itemId: String): String? {
        val convoId = _state.value.conversationId.ifBlank { return null }
        val existing = conversations.loadItems(convoId)
        val editing = _state.value.editingCiId
        val active = if (editing != null) existing.firstOrNull { it.id == editing }
            else existing.lastOrNull {
                it.status == com.treasure.core.repo.ConversationItemStatus.PENDING ||
                    it.status == com.treasure.core.repo.ConversationItemStatus.MODIFIED
            }
        if (active == null) return null
        conversations.upsertItem(
            active.copy(
                draft = null,
                itemRef = itemId,
                status = com.treasure.core.repo.ConversationItemStatus.SAVED,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return active.id
    }

    override fun onCleared() {
        com.treasure.background.AiKeepAliveService.stop(getApplication())
        super.onCleared()
    }

    init {
        // Cycle 0031：会话切换时同步订阅那段会话的工作集（ConversationItem 列表）。
        // collectLatest 切到新 id 时取消旧 inner flow，避免两份 items 同时往
        // _state 灌。
        viewModelScope.launch {
            _state.map { it.conversationId }
                .distinctUntilChanged()
                .collectLatest { id ->
                    if (id.isBlank()) {
                        _state.update { it.copy(items = emptyList()) }
                    } else {
                        conversations.observeItems(id).collect { itemsList ->
                            _state.update { it.copy(items = itemsList) }
                        }
                    }
                }
        }
    }

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

    /**
     * Cycle 0033：从图鉴编辑态点 [编辑] 进来 — 起一段新会话，把选中的物品
     * 全部作为 SAVED 行预填进工作集，让 AI 看见它们后用户可以批量做 modify
     * （比如"给这几只都换个外包装"）。会话标题取第一件物品的"品牌 型号"。
     */
    fun startConversationFromItems(itemIds: List<String>) {
        if (itemIds.isEmpty()) {
            newConversation()
            return
        }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val key = getApplication<TreasureApp>().settingsStore.hasKey()
        val opener = AddMessage.Assistant(GREETING)
        _state.value = AddUiState(
            conversationId = id,
            messages = listOf(opener),
            conversationTitle = "Batch · ${formatTime(now)}",
            aiAvailable = key,
        )
        viewModelScope.launch {
            conversations.upsert(
                AddConversation(id = id, title = "Batch · ${formatTime(now)}", createdAt = now, updatedAt = now),
            )
            persist(opener)
            // 把每个 itemId 当 SAVED 行加进工作集，sortOrder 顺次递增。
            val snapshot = runCatching { repo.items.first() }.getOrDefault(emptyList())
            val map = snapshot.associateBy { it.id }
            itemIds.forEachIndexed { idx, itemId ->
                if (itemId !in map) return@forEachIndexed
                conversations.upsertItem(
                    com.treasure.core.repo.ConversationItem(
                        id = UUID.randomUUID().toString(),
                        conversationId = id,
                        draft = null,
                        itemRef = itemId,
                        status = com.treasure.core.repo.ConversationItemStatus.SAVED,
                        sortOrder = idx,
                        createdAt = now + idx,
                        updatedAt = now + idx,
                    ),
                )
            }
            // 用第一件物品给会话起个有意义的标题
            val firstItem = itemIds.firstNotNullOfOrNull { map[it] }
            if (firstItem != null) {
                val title = listOf(firstItem.brand, firstItem.model)
                    .filter { it.isNotBlank() }.joinToString(" ")
                    .ifBlank { firstItem.nickname }
                    .ifBlank { "Batch · ${formatTime(now)}" }
                val fullTitle = if (itemIds.size > 1) "$title 等 ${itemIds.size} 件" else title
                conversations.upsert(
                    AddConversation(id = id, title = fullTitle, createdAt = now, updatedAt = now),
                )
                _state.update { it.copy(conversationTitle = fullTitle) }
            }
        }
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
        // Cycle 0031 redesign：会话不再封存（一会话支持多物品），允许继续发。
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

    /** Cycle 0034：一次发多张图。每张分别 persist 到 conversation-photos，
     *  作为单独 UserPhoto 消息入库；source_index 对应 photos 顺序，供 AI 在
     *  photo_assignments 里引用。 */
    fun sendPhotos(uris: List<Uri>, caption: String = "") {
        if (_state.value.busy || uris.isEmpty()) return
        val app = getApplication<TreasureApp>()
        viewModelScope.launch {
            val persisted = withContext(Dispatchers.IO) {
                uris.map { u -> runCatching { persistChatPhoto(app, u) }.getOrNull() ?: u }
            }
            persisted.forEach { appendMessage(AddMessage.UserPhoto(it)) }
            val trimmed = caption.trim()
            if (trimmed.isNotEmpty()) appendMessage(AddMessage.User(trimmed))
            val effectiveText = trimmed.ifBlank {
                if (persisted.size == 1) "（用户附了一张照片，请识别）"
                else "（用户附了 ${persisted.size} 张照片，请按物品逐张分配 / 按需切 crop）"
            }
            runExtract(text = effectiveText, imageUris = persisted)
        }
    }

    /** Cycle 0031 compat：单图 wrapper — 部分代码（旧 callsite）仍 sendPhoto。 */
    fun sendPhoto(uri: Uri, caption: String = "") = sendPhotos(listOf(uri), caption)

    /** Cycle 0034 v2：用户长按麦克风录的音频。[audioPath] 是 m4a 完整路径
     *  （已在 filesDir/voice-cache/<convo>/），duration 形如 "0:03"。把音频
     *  字节作为 input_audio block 送给 AI；provider 不支持会返错 surface 出去。 */
    fun sendVoiceAudio(audioPath: String, duration: String) {
        if (_state.value.busy || audioPath.isBlank()) return
        // text 字段留空 — 历史会话里把它视作 "（语音消息）"；不做转写。
        appendMessage(AddMessage.UserVoice(text = "", duration = duration, audioPath = audioPath))
        runExtract(
            text = "（用户发了一段语音，时长 $duration，请直接听音内容回应）",
            imageUris = emptyList(),
            audioPath = audioPath,
        )
    }

    /** Cycle 0008 兼容：旧 STT 路径（暂留供历史 callsite，新流不用）。 */
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

    private fun runExtract(text: String, imageUri: Uri?) =
        runExtract(text = text, imageUris = listOfNotNull(imageUri))

    private fun runExtract(text: String, imageUris: List<Uri>, audioPath: String? = null) {
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
        val startedAt = System.currentTimeMillis()
        _state.update { it.copy(busy = true, busyStartedAt = startedAt, lastElapsedMs = null) }
        // Cycle 0031：AI 调用期间起一发前台保活 service — vivo / 华为 / OPPO
        // 这种激进省电系统熄屏 / 切后台几秒就杀普通进程，OkHttp 直接断。前
        // 台 service + PARTIAL_WAKE_LOCK 给 OS 一个"我在干活"信号，撑过这
        // 段时间。runExtract 结束（成功 / 失败 / cancel）都会 stop。
        com.treasure.background.AiKeepAliveService.start(app)
        // Cycle 0031：记一下 client 引用，给 stopExtract 用。
        currentClient = client
        val priorTurns = buildPriorTurns(_state.value.messages)
        currentExtractJob = viewModelScope.launch {
            // Cycle 0027：把 manager 里能选的分类喂给 AI（内建 + 未隐藏自定义）
            val hints = categoryRepo.loadAll()
                .filter { !it.hidden }
                .map { com.treasure.core.ai.CategoryHint(it.id, it.nameZh, it.nameEn) }
            // Cycle 0034：一组 jpegs，按 source_index 顺序压缩。AI 据
            // photo_assignments 把对应 index 的图分配给草稿。
            val bytesList = imageUris.mapNotNull { uri ->
                runCatching {
                    withContext(Dispatchers.IO) { compressForAi(app, uri) }
                }.getOrNull()
            }
            // Cycle 0034：记下这一轮 user-turn 的 photo 集（按持久化后的 Uri
            // 顺序），accept 时按 source_index 反查。这个 state 只活在本次
            // VM 内存，不入 Room — 用户重启应用前还没 accept，那批 photo 索
            // 引就丢了；按 cycle 0034 设计，这是可接受的。
            lastTurnPhotoUris = imageUris
            // Cycle 0032：构造工作集摘要喂给 AI。SAVED 行从 itemsRepo 拉真物
            // 品摘要；PENDING / MODIFIED 直接用 ConversationItem.draft 的字段。
            val workingSetSummary = buildWorkingSetSummary(_state.value.items)
            // Cycle 0034 v2：把音频字节加载进来，作为 input_audio block 送 AI。
            val audioBytes = audioPath?.let { path ->
                runCatching {
                    withContext(Dispatchers.IO) { java.io.File(path).readBytes() }
                }.getOrNull()
            }
            client.extractItemDrafts(
                text = text,
                imagesJpegBytes = bytesList,
                priorTurns = priorTurns,
                workingSet = workingSetSummary,
                categoryHints = hints,
                audioBytes = audioBytes,
                audioFormat = "m4a",
            )
                .onSuccess { actions ->
                    if (actions.isEmpty()) {
                        // 模型返回了空 actions（理论上 minItems=1 拦着，但稳健起见）
                        val msg = AddMessage.SystemNote("AI 没识别到新物品", NoteTone.Warning)
                        _state.update {
                            val started = it.busyStartedAt ?: startedAt
                            it.copy(
                                messages = it.messages + msg,
                                busy = false,
                                busyStartedAt = null,
                                lastElapsedMs = System.currentTimeMillis() - started,
                            )
                        }
                        com.treasure.background.AiKeepAliveService.stop(app)
                        return@onSuccess
                    }
                    // Cycle 0032 v2：不再直接落工作集 —— AI 返回的每个 action
                    // 都先以一张 DraftCta 卡片摆在聊天里，用户逐个 [采用] / [不要]，
                    // 只有采用的那刻才上 conversation_items。这样用户能在采用前
                    // 看完整草稿、不喜欢就直接拒，未确认的不污染工作集。
                    val assistantText = buildAssistantSummary(actions)
                    val assistant = AddMessage.Assistant(assistantText)
                    // Cycle 0034：resolve photo_assignments — 把 source_index
                    // 反查到 lastTurnPhotoUris 拿到本地 file:// path。AI 给出
                    // 的 crop 已是归一化 0..1 矩形；UI / accept 时直接拿。
                    val turnUris = lastTurnPhotoUris
                    val ctas = actions.map { a ->
                        val resolved = a.photoAssignments.mapNotNull { pa ->
                            val srcUri = turnUris.getOrNull(pa.sourceIndex) ?: return@mapNotNull null
                            val crop = pa.crop
                            com.treasure.core.repo.ResolvedPhotoAssignment(
                                sourceUri = srcUri.toString(),
                                cropX = crop?.x ?: 0f,
                                cropY = crop?.y ?: 0f,
                                cropW = crop?.w ?: 1f,
                                cropH = crop?.h ?: 1f,
                                isAvatar = pa.setAsAvatar,
                            )
                        }
                        AddMessage.DraftCta(
                            id = UUID.randomUUID().toString(),
                            draft = a.draft,
                            fieldCount = fieldCount(a.draft),
                            status = DraftCtaStatus.Pending,
                            actionKind = when (a.kind) {
                                com.treasure.core.ai.ActionKind.CREATE ->
                                    com.treasure.core.repo.DraftCtaActionKind.Create
                                com.treasure.core.ai.ActionKind.MODIFY ->
                                    com.treasure.core.repo.DraftCtaActionKind.Modify
                            },
                            targetCiId = a.targetId.takeIf {
                                a.kind == com.treasure.core.ai.ActionKind.MODIFY
                            },
                            photoAssignments = resolved,
                        )
                    }
                    // 会话标题：以最近一条 create action 的 brand+model 命名（modify
                    // 的目标本来就在工作集里有标题了，没必要再覆盖）。
                    val latestCreate = actions.lastOrNull {
                        it.kind == com.treasure.core.ai.ActionKind.CREATE
                    }?.draft
                    val title = latestCreate?.let { d ->
                        listOf(d.brand, d.model).filter { it.isNotBlank() }
                            .joinToString(" ").ifBlank { null }
                    } ?: _state.value.conversationTitle
                    _state.update {
                        val started = it.busyStartedAt ?: startedAt
                        it.copy(
                            messages = it.messages + assistant + ctas,
                            conversationTitle = title,
                            busy = false,
                            busyStartedAt = null,
                            lastElapsedMs = System.currentTimeMillis() - started,
                        )
                    }
                    viewModelScope.launch {
                        persist(assistant)
                        ctas.forEach { persistDraftCtaWithId(it) }
                    }
                    com.treasure.background.AiKeepAliveService.stop(app)
                }
                .onFailure { err ->
                    // 用户随口聊天（"你好"），模型不调 tool 而回普通文字 — 这
                    // 不是错误，把那段文字直接 surface 成助手消息即可。
                    // Cycle 0031：用户主动 [停止] → OkHttp 抛 IOException("Canceled")
                    // 或 coroutine CancellationException；不当错误 surface，
                    // 只 append 一条 SystemNote。
                    val cancelled =
                        err is kotlinx.coroutines.CancellationException ||
                            err.message?.contains("Canceled", ignoreCase = true) == true ||
                            err.message?.contains("Socket closed", ignoreCase = true) == true
                    val msg = when {
                        cancelled -> AddMessage.SystemNote("已停止")
                        err is com.treasure.core.ai.ChatOnlyResponseException ->
                            AddMessage.Assistant(err.text.ifBlank { "嗯。" })
                        else -> AddMessage.Assistant("出错了：${err.message ?: "未知错误"}")
                    }
                    _state.update {
                        val started = it.busyStartedAt ?: startedAt
                        it.copy(
                            messages = it.messages + msg,
                            busy = false,
                            busyStartedAt = null,
                            lastElapsedMs = System.currentTimeMillis() - started,
                            errorMessage = when {
                                cancelled -> null
                                err is com.treasure.core.ai.ChatOnlyResponseException -> null
                                else -> err.message
                            },
                        )
                    }
                    if (msg is AddMessage.SystemNote) {
                        // SystemNote 不入 Room（per toDomainMessage 返回 null）
                    } else {
                        viewModelScope.launch { persist(msg) }
                    }
                    com.treasure.background.AiKeepAliveService.stop(app)
                }
            currentClient = null
            currentExtractJob = null
        }
    }

    /**
     * Cycle 0032 v2：用户点 DraftCta 上的 [采用]。把卡片置 Accepted，并把对
     * 应 action 应用到工作集：
     *  - kind=Create：新建一行 PENDING
     *  - kind=Modify + target 存在：覆盖 draft；原 SAVED → MODIFIED，否则保持
     *  - kind=Modify + target 不存在（被删了 / 旧数据）：兜底当 Create
     */
    fun acceptProposal(ctaId: String) {
        val current = _state.value
        val cta = current.messages.firstOrNull {
            it is AddMessage.DraftCta && it.id == ctaId
        } as AddMessage.DraftCta? ?: return
        val newMessages = current.messages.map { m ->
            if (m is AddMessage.DraftCta && m.id == ctaId) {
                m.copy(status = DraftCtaStatus.Accepted)
            } else m
        }
        _state.update { it.copy(messages = newMessages) }
        viewModelScope.launch {
            val accepted = newMessages.firstOrNull {
                it is AddMessage.DraftCta && it.id == ctaId
            } as AddMessage.DraftCta?
            accepted?.let { upsertCtaStatus(it) }
            applyAcceptedCta(cta, cta.draft)
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
     *  不需要走"提案 / 采用"循环 — 用户改的就是最终态。
     *  Cycle 0031：每次编辑都调度一次 debounced auto-save 到 Room，没有
     *  "保存"按钮，用户改完直接背景退出也不丢。 */
    fun updateDraftField(field: PreviewField, value: String) {
        val current = _state.value.confirmedDraft ?: ItemDraft()
        _state.value = _state.value.copy(confirmedDraft = applyFieldEdit(current, field, value))
        scheduleDraftAutoSave()
    }

    fun updateDraftSpec(idx: Int, spec: HeroSpec) {
        val current = _state.value.confirmedDraft ?: return
        if (idx < 0 || idx >= current.specs.size) return
        val newSpecs = current.specs.toMutableList().also { it[idx] = spec }
        _state.value = _state.value.copy(confirmedDraft = current.copy(specs = newSpecs))
        scheduleDraftAutoSave()
    }

    fun addDraftSpec() {
        val current = _state.value.confirmedDraft ?: ItemDraft()
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(specs = current.specs + HeroSpec("", "")),
        )
        scheduleDraftAutoSave()
    }

    fun removeDraftSpec(idx: Int) {
        val current = _state.value.confirmedDraft ?: return
        if (idx < 0 || idx >= current.specs.size) return
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(specs = current.specs.toMutableList().also { it.removeAt(idx) }),
        )
        scheduleDraftAutoSave()
    }

    /** Cycle 0031：Refine 页参数行换成 Edit 同款 ReorderableSpecs 后需要的 move 出口。 */
    fun moveDraftSpec(from: Int, to: Int) {
        val current = _state.value.confirmedDraft ?: return
        if (from < 0 || from >= current.specs.size) return
        val target = to.coerceIn(0, current.specs.size - 1)
        if (from == target) return
        val newSpecs = current.specs.toMutableList().also {
            val moved = it.removeAt(from)
            it.add(target, moved)
        }
        _state.value = _state.value.copy(confirmedDraft = current.copy(specs = newSpecs))
        scheduleDraftAutoSave()
    }

    /** Cycle 0032 v2：proposal-preview 微改后采用 — 同 acceptProposal，但把
     *  用户编辑过的 draft 落到工作集（而不是 cta 里 AI 的原版）。 */
    fun acceptProposalWithEdits(ctaId: String, editedDraft: ItemDraft) {
        val current = _state.value
        val cta = current.messages.firstOrNull {
            it is AddMessage.DraftCta && it.id == ctaId
        } as AddMessage.DraftCta? ?: return
        val newMessages = current.messages.map { m ->
            if (m is AddMessage.DraftCta && m.id == ctaId) {
                m.copy(status = DraftCtaStatus.Accepted, draft = editedDraft)
            } else m
        }
        _state.update { it.copy(messages = newMessages) }
        viewModelScope.launch {
            val accepted = newMessages.firstOrNull {
                it is AddMessage.DraftCta && it.id == ctaId
            } as AddMessage.DraftCta?
            accepted?.let { upsertCtaStatus(it) }
            applyAcceptedCta(cta, editedDraft)
        }
    }

    /**
     * Cycle 0032 v2：把"已采用"的 DraftCta 落到 conversation_items 工作集。
     * 这是 [acceptProposal] / [acceptProposalWithEdits] 的公共后半段。
     *
     * - Create → 新建一行 PENDING（递增 sortOrder）
     * - Modify + target 存在：覆盖 draft；SAVED → MODIFIED，否则保持 status
     * - Modify + target 缺失：兜底当 Create，免得用户点了采用却什么都没发生
     */
    private suspend fun applyAcceptedCta(
        cta: AddMessage.DraftCta,
        draft: ItemDraft,
    ) {
        val convoId = _state.value.conversationId.ifBlank { return }
        val now = System.currentTimeMillis()
        // Cycle 0034：先把 AI 分配的图按 crop 抄到 filesDir/draft-photos/<convo>/，
        // 拿到本端 file:// 路径，merge 进 draft.photos / avatarPhotoPath。这步
        // 在 upsertItem 之前做完，让落到工作集的就是终态。
        val draftWithPhotos = if (cta.photoAssignments.isEmpty()) draft
        else applyPhotoAssignmentsToDraft(draft, cta.photoAssignments)
        val existing = conversations.loadItems(convoId).associateBy { it.id }
        val target = cta.targetCiId?.let { existing[it] }
        if (cta.actionKind == com.treasure.core.repo.DraftCtaActionKind.Modify && target != null) {
            val newStatus = when (target.status) {
                com.treasure.core.repo.ConversationItemStatus.SAVED ->
                    com.treasure.core.repo.ConversationItemStatus.MODIFIED
                com.treasure.core.repo.ConversationItemStatus.MODIFIED ->
                    com.treasure.core.repo.ConversationItemStatus.MODIFIED
                com.treasure.core.repo.ConversationItemStatus.PENDING ->
                    com.treasure.core.repo.ConversationItemStatus.PENDING
            }
            conversations.upsertItem(
                target.copy(draft = draftWithPhotos, status = newStatus, updatedAt = now),
            )
        } else {
            val order = conversations.nextSortOrder(convoId)
            conversations.upsertItem(
                com.treasure.core.repo.ConversationItem(
                    id = UUID.randomUUID().toString(),
                    conversationId = convoId,
                    draft = draftWithPhotos,
                    itemRef = null,
                    status = com.treasure.core.repo.ConversationItemStatus.PENDING,
                    sortOrder = order,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    /**
     * Cycle 0034：把已 resolve 的 photo 分配应用到 draft。
     *  - 每条分配解码 sourceUri，按归一化 crop 切出子 bitmap，落 jpg 到
     *    filesDir/draft-photos/<convoId>/<uuid>.jpg
     *  - append 到 draft.photos
     *  - 第一条 isAvatar=true 的设为 draft.avatarPhotoPath
     */
    private suspend fun applyPhotoAssignmentsToDraft(
        draft: ItemDraft,
        assignments: List<com.treasure.core.repo.ResolvedPhotoAssignment>,
    ): ItemDraft = withContext(Dispatchers.IO) {
        val convoId = _state.value.conversationId.ifBlank { return@withContext draft }
        val app = getApplication<TreasureApp>()
        val dir = java.io.File(app.filesDir, "draft-photos/$convoId").apply { mkdirs() }
        val addedPhotos = mutableListOf<String>()
        var newAvatar: String? = null
        for (a in assignments) {
            val savedPath = runCatching {
                val src = app.contentResolver.openInputStream(android.net.Uri.parse(a.sourceUri))?.use {
                    android.graphics.BitmapFactory.decodeStream(it)
                } ?: return@runCatching null
                val w = src.width
                val h = src.height
                val left = (a.cropX * w).toInt().coerceIn(0, w - 1)
                val top = (a.cropY * h).toInt().coerceIn(0, h - 1)
                val right = ((a.cropX + a.cropW) * w).toInt().coerceIn(left + 1, w)
                val bottom = ((a.cropY + a.cropH) * h).toInt().coerceIn(top + 1, h)
                val cropped = android.graphics.Bitmap.createBitmap(src, left, top, right - left, bottom - top)
                if (cropped !== src) src.recycle()
                val dest = java.io.File(dir, "${UUID.randomUUID()}.jpg")
                dest.outputStream().use { out ->
                    cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
                }
                cropped.recycle()
                if (dest.exists() && dest.length() > 0) dest.absolutePath else null
            }.getOrNull() ?: continue
            addedPhotos += savedPath
            if (a.isAvatar && newAvatar == null) newAvatar = savedPath
        }
        draft.copy(
            photos = draft.photos + addedPhotos,
            avatarPhotoPath = newAvatar ?: draft.avatarPhotoPath,
        )
    }

    /**
     * Cycle 0034：commit 时把会话域路径下的图复制到 photos/<itemId>/，
     * 让物品的影集独立于会话。已经在 photos/<itemId>/ 下面的（MODIFY 物品
     * 原 photos）原样保留。返回新的 (photos, avatar) 列表 / 路径。
     *
     * 不删源文件 — 会话删除时一并清理（暂未实现），或留在磁盘做软备份。
     */
    private suspend fun migratePhotosToItemOwned(
        itemId: String,
        draftPhotos: List<String>,
        draftAvatar: String?,
    ): Pair<List<String>, String?> = withContext(Dispatchers.IO) {
        val app = getApplication<TreasureApp>()
        val targetDir = java.io.File(app.filesDir, "photos/$itemId").apply { mkdirs() }
        val targetPath = targetDir.absolutePath + java.io.File.separator
        val mapping = mutableMapOf<String, String>()
        for (src in draftPhotos) {
            if (src.startsWith(targetPath)) {
                mapping[src] = src // 已经是 item-owned
                continue
            }
            val srcFile = java.io.File(src)
            if (!srcFile.exists() || srcFile.length() == 0L) continue
            val dest = java.io.File(targetDir, "${UUID.randomUUID()}.jpg")
            runCatching {
                srcFile.inputStream().use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                }
            }
            if (dest.exists() && dest.length() > 0L) mapping[src] = dest.absolutePath
        }
        val migratedPhotos = draftPhotos.mapNotNull { mapping[it] }
        val migratedAvatar = draftAvatar?.let { mapping[it] ?: if (it.startsWith(targetPath)) it else null }
        migratedPhotos to migratedAvatar
    }

    /** Cycle 0031：草稿页历史时间轴整段替换。AddPreview 加 / 改 / 删一行
     *  都通过这一个出口。 */
    fun setDraftHistory(history: List<HistoryEvent>) {
        val current = _state.value.confirmedDraft ?: ItemDraft()
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(history = history.sortedBy { it.date }),
        )
        scheduleDraftAutoSave()
    }

    // ─── Cycle 0033：Refine 页影集管理 ───────────────────────────────────

    /** 把一张已写到磁盘的照片 path 加到草稿的 photos。HeroAvatarPicker 调
     *  + 选照片 / 📷 拍照后，UI 先做 crop（可选），裁好的 jpg 落到
     *  filesDir/draft-photos/<convoId>/，把绝对路径传进来。 */
    fun addDraftPhoto(path: String) {
        val current = _state.value.confirmedDraft ?: ItemDraft()
        if (path.isBlank() || path in current.photos) return
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(photos = current.photos + path),
        )
        scheduleDraftAutoSave()
    }

    /** 草稿头像 — 必须是 photos 里的某张；传 null = 用回线描插画。 */
    fun setDraftAvatar(path: String?) {
        val current = _state.value.confirmedDraft ?: return
        val newAvatar = path?.takeIf { it in current.photos }
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(avatarPhotoPath = newAvatar),
        )
        scheduleDraftAutoSave()
    }

    /** 移除草稿里的某张照片，并尝试删磁盘文件（如果是 draft-photos 目录下）。 */
    fun removeDraftPhoto(path: String) {
        val current = _state.value.confirmedDraft ?: return
        if (path !in current.photos) return
        val newAvatar = current.avatarPhotoPath?.takeIf { it != path }
        _state.value = _state.value.copy(
            confirmedDraft = current.copy(
                photos = current.photos - path,
                avatarPhotoPath = newAvatar,
            ),
        )
        scheduleDraftAutoSave()
        // 只删 draft-photos 目录下的；如果是 MODIFY 进来的 item.photos 路径
        // （存在于 photos/<itemId>/），不能盲删 — Refine 取消后还得回去。
        viewModelScope.launch(Dispatchers.IO) {
            val f = java.io.File(path)
            if (f.exists() && f.parentFile?.parentFile?.name == "draft-photos") {
                runCatching { f.delete() }
            }
        }
    }

    /**
     * Refine 页拍照 / picker 拿到 content:// URI 后，把它复制到 app 私有目录
     * 拿到稳定 file://path。如果 [cropRect] 非空（CropScreen 已给出归一化裁
     * 剪矩形 [0..1]），按矩形裁剪后再保存。
     *
     * Returns absolute file path on success, null on failure.
     */
    suspend fun persistDraftPhoto(
        sourceUri: Uri,
        cropRect: androidx.compose.ui.geometry.Rect? = null,
    ): String? = withContext(Dispatchers.IO) {
        val convoId = _state.value.conversationId.ifBlank { return@withContext null }
        val app = getApplication<TreasureApp>()
        val dir = java.io.File(app.filesDir, "draft-photos/$convoId").apply { mkdirs() }
        val dest = java.io.File(dir, "${UUID.randomUUID()}.jpg")
        runCatching {
            val bitmap = app.contentResolver.openInputStream(sourceUri)?.use {
                android.graphics.BitmapFactory.decodeStream(it)
            } ?: return@runCatching null
            val final = if (cropRect != null) {
                val w = bitmap.width
                val h = bitmap.height
                val left = (cropRect.left * w).toInt().coerceIn(0, w - 1)
                val top = (cropRect.top * h).toInt().coerceIn(0, h - 1)
                val right = (cropRect.right * w).toInt().coerceIn(left + 1, w)
                val bottom = (cropRect.bottom * h).toInt().coerceIn(top + 1, h)
                android.graphics.Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
                    .also { if (it !== bitmap) bitmap.recycle() }
            } else bitmap
            dest.outputStream().use { out ->
                final.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, out)
            }
            final.recycle()
            dest.absolutePath
        }.getOrNull()?.also { addDraftPhoto(it) }
    }

    /** Cycle 0031：Draft 编辑实时保存 — 500ms debounce 后把当前 confirmedDraft
     *  写回最近一条 DraftConfirmed 消息（同 id 走 upsertMessage 更新 payload）。
     *  如果当前还没有 DraftConfirmed（例如 ensureDraftForManual 刚建的空草
     *  稿），就 append 一条新的并把 id 记下来下次复用。 */
    private var autoSaveJob: kotlinx.coroutines.Job? = null
    private fun scheduleDraftAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            persistDraftSnapshot()
        }
    }

    private suspend fun persistDraftSnapshot() {
        val state = _state.value
        val draft = state.confirmedDraft ?: return
        val convoId = state.conversationId.ifBlank { return }
        val now = System.currentTimeMillis()
        // Cycle 0032：用户在 Refine 页编辑实时同步回 conversation_items 的对
        // 应行 — 这是新流程下"工作集"的真实状态，drawer 渲染 / AI working-set
        // 摘要都看它。editingCiId 指向当前编辑的 ciId；没设（极少见的退化
        // 路径）就跳过。
        val editing = state.editingCiId
        if (editing != null) {
            val ci = state.items.firstOrNull { it.id == editing }
            if (ci != null && ci.draft != draft) {
                conversations.upsertItem(ci.copy(draft = draft, updatedAt = now))
            }
        }
        // 兼容：DraftConfirmed 消息保留作为聊天历史里的"已确认草稿"快照。新
        // 流程不依赖它当 baseline，但若已存在就 upsert 同 id（不再 append 新）。
        val latest = state.messages.indexOfLast { it is AddMessage.DraftConfirmed }
        if (latest >= 0) {
            val old = state.messages[latest] as AddMessage.DraftConfirmed
            val updated = old.copy(draft = draft, fieldCount = fieldCount(draft))
            val newMessages = state.messages.toMutableList().also { it[latest] = updated }
            _state.update { it.copy(messages = newMessages) }
            conversations.appendMessage(
                convoId,
                AddConversationMessage.DraftConfirmed(updated.id, draft, fieldCount(draft), now),
            )
        }
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
            // Cycle 0032：commit 前先看一下我们是不是在 MODIFY 一个已有物品
            // （editingCiId 指向的 ConversationItem 是 MODIFIED 状态，itemRef
            // 非空）。是的话保留原 item id / createdAt / photos，避免在图鉴
            // 里产生重复行。
            val editing = _state.value.editingCiId
            val workingItem = editing?.let { ed ->
                _state.value.items.firstOrNull { it.id == ed }
            }
            val refItem: Item? = workingItem?.itemRef?.let { ref ->
                runCatching { repo.observeById(ref).first() }.getOrNull()
            }
            // Cycle 0027：category 现在是 String id。优先用 draft 里的；空就
            // 默认 "tech"（最泛的内建分类）。如果命中内建 enum，用对应模板
            // 拿 palette / heroVector；不命中（自定义分类）就走 GENERIC 套
            // generic palette。
            val categoryId = draft.category?.takeIf { it.isNotBlank() } ?: "tech"
            val builtIn = Category.entries.firstOrNull { it.id == categoryId }
            val template = builtIn?.let { CategoryTemplates.forCategory(it) }
            val palette = refItem?.palette ?: template?.palette
                ?: listOf("#0e0e0e", "#a47836", "#e8e2d4", "#5a5a5a")
            val heroVector = refItem?.heroVector ?: template?.heroVector ?: HeroVector.GENERIC
            val id = refItem?.id ?: makeId(categoryId, draft.brand, draft.model, now)
            // 还是优先看 AI 填没填 "入手日期" spec；没填就今天。手动改的也会
            // 体现在 spec 列表里，于是这里能拿到。
            val acquired = readPurchaseField(draft, "入手日期")
                .ifBlank { refItem?.acquired }
                ?.ifBlank { LocalDate.now().toString() }
                ?: LocalDate.now().toString()
            // Cycle 0034：commit 时把 draft 影集里所有"会话域"路径（conversation-
            // photos / draft-photos）COPY 到 photos/<itemId>/，让它们脱离会话
            // 寿命独立存在 — 用户删除会话不再丢图。已在 photos/<itemId>/ 下
            // 的（MODIFY 情况下从原 item 继承的）原样保留。
            val draftPhotosRaw = draft.photos.ifEmpty { refItem?.photos ?: emptyList() }
            val (itemPhotos, itemAvatar) = migratePhotosToItemOwned(
                itemId = id,
                draftPhotos = draftPhotosRaw,
                draftAvatar = draft.avatarPhotoPath ?: refItem?.avatarPhotoPath,
            )
            val item = Item(
                id = id,
                category = categoryId,
                brand = draft.brand.trim(),
                model = draft.model.trim(),
                nickname = draft.nickname.trim(),
                acquired = acquired,
                parted = refItem?.parted,
                status = status,
                palette = palette,
                oneLiner = draft.oneLiner.trim(),
                heroVector = heroVector,
                specs = draft.specs.filter { it.label.isNotBlank() || it.value.isNotBlank() },
                // Cycle 0031：草稿页用户能编辑 history 时间轴；用户填了就直接
                // 用，没填就走老逻辑默认一条 ACQUIRED。
                history = draft.history.ifEmpty {
                    refItem?.history ?: listOf(HistoryEvent(acquired, HistoryKind.ACQUIRED, "购入", ""))
                },
                photos = itemPhotos,
                avatarPhotoPath = itemAvatar,
                createdAt = refItem?.createdAt ?: now,
                updatedAt = now,
                // Cycle 0033：新物品 sortOrder = 当前最小 - 1 → 默认浮到图鉴
                // 列表最前。MODIFY commit 保持原 sortOrder 不动（防止编辑后
                // 物品被弹到前面，符合"后续改动不会影响排序"）。
                sortOrder = refItem?.sortOrder ?: (repo.minSortOrder() - 1L),
            )
            repo.upsert(item)
            // Cycle 0031 redesign：一个会话支持多物品 — commit 现在只是把活跃
            // PENDING / MODIFIED 行升格为 SAVED，会话不再封存。下次 AI 提案
            // 自动起一份新的 PENDING 行（upsertWorkingDraft 找不到活跃行就 new）。
            //
            // confirmedDraft / proposedDraft 是单草稿时期的会话级 baseline；
            // commit 一次就清掉，让下个物品从干净起手开始（AI 也看不到上一个
            // 物品的字段当 baseline，避免污染）。Committed 标记仍 append 一行，
            // 让聊天历史有迹可循（"✦ 已收入图鉴"），但不锁 composer。
            markActiveItemSaved(id)
            val committed = AddMessage.Committed(
                id = UUID.randomUUID().toString(),
                savedItemId = id,
            )
            _state.update {
                it.copy(
                    messages = it.messages + committed,
                    confirmedDraft = null,
                    proposedDraft = null,
                    pendingCtaId = null,
                    editingCiId = null,
                )
            }
            persist(committed)
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
                sortOrder = repo.minSortOrder() - 1L,
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
            is AddMessage.UserVoice -> AddConversationMessage.UserVoice(
                id, msg.text, msg.duration, now,
                audioPath = msg.audioPath,
            )
            is AddMessage.DraftCta -> AddConversationMessage.DraftCta(
                id = msg.id, // 保留 UI 已分配的 id，让后续 status 更新能找到同一行
                draft = msg.draft,
                fieldCount = msg.fieldCount,
                status = msg.status,
                createdAt = now,
                actionKind = msg.actionKind,
                targetCiId = msg.targetCiId,
                photoAssignments = msg.photoAssignments,
            )
            is AddMessage.DraftConfirmed -> AddConversationMessage.DraftConfirmed(
                id = msg.id, draft = msg.draft, fieldCount = msg.fieldCount, createdAt = now,
            )
            is AddMessage.Committed -> AddConversationMessage.Committed(
                id = msg.id, savedItemId = msg.savedItemId, createdAt = now,
            )
            is AddMessage.SystemNote -> null
        }
    }

    private fun toUiMessage(domain: AddConversationMessage): AddMessage = when (domain) {
        is AddConversationMessage.Assistant -> AddMessage.Assistant(domain.text)
        is AddConversationMessage.User -> AddMessage.User(domain.text)
        is AddConversationMessage.UserPhoto -> AddMessage.UserPhoto(Uri.parse(domain.uri))
        is AddConversationMessage.UserVoice -> AddMessage.UserVoice(
            text = domain.text,
            duration = domain.duration,
            audioPath = domain.audioPath,
        )
        is AddConversationMessage.DraftCta -> AddMessage.DraftCta(
            id = domain.id,
            draft = domain.draft,
            fieldCount = domain.fieldCount,
            status = domain.status,
            actionKind = domain.actionKind,
            targetCiId = domain.targetCiId,
            photoAssignments = domain.photoAssignments,
        )
        is AddConversationMessage.DraftConfirmed -> AddMessage.DraftConfirmed(
            id = domain.id, draft = domain.draft, fieldCount = domain.fieldCount,
        )
        is AddConversationMessage.Committed -> AddMessage.Committed(
            id = domain.id, savedItemId = domain.savedItemId,
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
                actionKind = cta.actionKind,
                targetCiId = cta.targetCiId,
                photoAssignments = cta.photoAssignments,
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
                actionKind = cta.actionKind,
                targetCiId = cta.targetCiId,
                photoAssignments = cta.photoAssignments,
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
                is AddMessage.Committed -> null // 封存标记，不喂回 AI
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

/** Cycle 0031：proposal-preview 模式 AddRoute 也需要按字段名 patch local
 *  draft，提到 internal。 */
internal fun applyPreviewFieldEdit(draft: ItemDraft, field: PreviewField, value: String): ItemDraft =
    applyFieldEdit(draft, field, value)

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

/**
 * Cycle 0031 复修：把聊天里附的图固化到 `filesDir/conversation-photos/`，
 * 拿到永久的 file://path 存进 Room — picker 给的 content:// 离开当前
 * Activity 上下文就失效，下次再 reload 对话时 AsyncImage 拉不到内容显示
 * 空白。
 */
private fun persistChatPhoto(app: TreasureApp, src: android.net.Uri): android.net.Uri {
    val dir = java.io.File(app.filesDir, "conversation-photos").apply { mkdirs() }
    val dest = java.io.File(dir, "${UUID.randomUUID()}.jpg")
    app.contentResolver.openInputStream(src)?.use { input ->
        dest.outputStream().use { out -> input.copyTo(out) }
    }
    if (!dest.exists() || dest.length() == 0L) return src
    return android.net.Uri.fromFile(dest)
}

/**
 * Cycle 0031 复修：发给 AI 前压一下图。
 *
 * 原始相机出图常 4-6 MB（4032×3024 JPEG），base64 后撑到 5-8 MB，慢网传不
 * 完 60-120s 就被 writeTimeout / 服务端 abort 掉，用户看见的就是
 * "Software caused connection abort"。
 *
 * 这里用两遍 BitmapFactory：第一遍只读尺寸算 inSampleSize，第二遍真正解码
 * 时直接降采样（不会先把原图整个塞进 Bitmap）。再按需 createScaledBitmap
 * 把最长边压到 [MAX_DIM]，最后 JPEG q=80 重编。一般能把 4-6 MB 的原图压到
 * 150-400 KB，base64 + JSON wrapping 后 ≈ 250-550 KB 上传，4G 也是 1-2s 的
 * 事。
 *
 * AI 识别 1280px 的图足够给出 brand/model（多模态模型大多内部又会再缩到
 * 768/1024）—— 不损失信息。
 */
private const val MAX_DIM = 1280
private const val JPEG_QUALITY = 80

private fun compressForAi(app: TreasureApp, uri: android.net.Uri): ByteArray? {
    val resolver = app.contentResolver
    // 第一遍：拿原图尺寸算 inSampleSize（不解码像素，省 OOM）
    val bounds = android.graphics.BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    resolver.openInputStream(uri)?.use {
        android.graphics.BitmapFactory.decodeStream(it, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > MAX_DIM * 2 || bounds.outHeight / sample > MAX_DIM * 2) {
        sample *= 2
    }
    val opts = android.graphics.BitmapFactory.Options().apply {
        inSampleSize = sample
    }
    val decoded = resolver.openInputStream(uri)?.use {
        android.graphics.BitmapFactory.decodeStream(it, null, opts)
    } ?: return null
    // 精修一次到 MAX_DIM 以内
    val scaled = run {
        val w = decoded.width
        val h = decoded.height
        val maxSide = maxOf(w, h)
        if (maxSide <= MAX_DIM) decoded
        else {
            val ratio = MAX_DIM.toFloat() / maxSide
            val nw = (w * ratio).toInt().coerceAtLeast(1)
            val nh = (h * ratio).toInt().coerceAtLeast(1)
            android.graphics.Bitmap.createScaledBitmap(decoded, nw, nh, true).also {
                if (it !== decoded) decoded.recycle()
            }
        }
    }
    return try {
        ByteArrayOutputStream().use { out ->
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
    } finally {
        scaled.recycle()
    }
}
