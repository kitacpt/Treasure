package com.treasure.core.repo

import android.content.Context
import com.treasure.core.ai.ItemDraft
import com.treasure.core.room.ConversationEntity
import com.treasure.core.room.ConversationMessageEntity
import com.treasure.core.room.TreasureDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Cycle 0024：DraftCta 不再是"AI 给了一份草稿、点击展开看"的固定卡片；
 * 它现在显式有三种状态（Pending / Accepted / Rejected），表示一次 AI 提案
 * 的命运。Accepted 同时会 append 一条 DraftConfirmed 快照入库 — 那是会话
 * 当前"已确认"的草稿基线，AI 下次跑时拿这个当 baseline 而不是从零开始。
 */
enum class DraftCtaStatus { Pending, Accepted, Rejected }

/** Cycle 0032：一张 DraftCta 卡片对应的 action — 新增 or 修改某行。 */
enum class DraftCtaActionKind { Create, Modify }

/**
 * Cycle 0034：AI 给 DraftCta 卡片做的图分配，已 resolve 到本地 file path。
 * source_index 已查表换成 sourceUri（filesDir/conversation-photos/<uuid>.jpg）。
 * crop = (x, y, w, h) 归一化矩形；整图就是 (0,0,1,1)。一个卡片里最多一条
 * [isAvatar]=true，会被 commit 时设为物品头像。
 */
@kotlinx.serialization.Serializable
data class ResolvedPhotoAssignment(
    val sourceUri: String,
    val cropX: Float = 0f,
    val cropY: Float = 0f,
    val cropW: Float = 1f,
    val cropH: Float = 1f,
    val isAvatar: Boolean = false,
)

/** 录入页对话的领域级 message —— 对应 UI 层 AddMessage。 */
sealed interface AddConversationMessage {
    val id: String
    val createdAt: Long

    data class Assistant(override val id: String, val text: String, override val createdAt: Long) : AddConversationMessage
    data class User(override val id: String, val text: String, override val createdAt: Long) : AddConversationMessage
    data class UserPhoto(override val id: String, val uri: String, override val createdAt: Long) : AddConversationMessage
    /** Cycle 0034 v2：[audioPath] 是 file system 路径 — 用户长按录的原始 AAC/M4A，
     *  历史会话回放靠它。[text] 旧版本可能装转写文本；新版本默认空。 */
    data class UserVoice(
        override val id: String,
        val text: String,
        val duration: String,
        override val createdAt: Long,
        val audioPath: String? = null,
    ) : AddConversationMessage
    data class DraftCta(
        override val id: String,
        val draft: ItemDraft,
        val fieldCount: Int,
        val status: DraftCtaStatus,
        override val createdAt: Long,
        /** Cycle 0032：[DraftCtaActionKind.Create] = 新增；[Modify] = 修改既有
         *  conversation_items 行（[targetCiId] 指向它）。 */
        val actionKind: DraftCtaActionKind = DraftCtaActionKind.Create,
        val targetCiId: String? = null,
        /** Cycle 0034：AI 给本卡分配的图（已 resolve source_index → 本地 path）。
         *  采用时按这个列表拷贝 / 裁剪到 draft.photos / avatarPhotoPath。 */
        val photoAssignments: List<ResolvedPhotoAssignment> = emptyList(),
    ) : AddConversationMessage
    /** Cycle 0024：用户 "采用" 后写下的快照，等价于"这一刻起，会话草稿是这样"。 */
    data class DraftConfirmed(
        override val id: String,
        val draft: ItemDraft,
        val fieldCount: Int,
        override val createdAt: Long,
    ) : AddConversationMessage
    /** Cycle 0031：用户点 "确认收入" 把草稿固化成 Item 后写的标记 — 会话从
     *  此封存。[savedItemId] 是落到图鉴里的 Item id，未来加"打开图鉴查看
     *  这条"按钮可以指过去。 */
    data class Committed(
        override val id: String,
        val savedItemId: String,
        override val createdAt: Long,
    ) : AddConversationMessage
}

data class AddConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Cycle 0031：一段会话的"工作集"里每条物品的三态。 */
enum class ConversationItemStatus { PENDING, SAVED, MODIFIED }

/**
 * Cycle 0031：会话工作集里的一行。
 *
 * - [draft] PENDING / MODIFIED 时是当前待审的草稿；SAVED 时为 null。
 * - [itemRef] SAVED / MODIFIED 时指向 items 表里的真物品；PENDING 时为 null。
 * - [status] 三态决定列表里胶囊按钮颜色（红/绿/黄）+ 点击行为。
 */
data class ConversationItem(
    val id: String,
    val conversationId: String,
    val draft: ItemDraft?,
    val itemRef: String?,
    val status: ConversationItemStatus,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

interface AddConversationRepository {
    fun observeRecent(limit: Int = 20): Flow<List<AddConversation>>
    suspend fun upsert(conversation: AddConversation)
    suspend fun delete(conversationId: String)
    suspend fun loadMessages(conversationId: String): List<AddConversationMessage>
    suspend fun appendMessage(conversationId: String, message: AddConversationMessage)
    suspend fun replaceMessages(conversationId: String, messages: List<AddConversationMessage>)

    /** Cycle 0031：会话工作集相关。 */
    fun observeItems(conversationId: String): Flow<List<ConversationItem>>
    suspend fun loadItems(conversationId: String): List<ConversationItem>
    suspend fun upsertItem(item: ConversationItem)
    suspend fun deleteItem(itemId: String)
    suspend fun nextSortOrder(conversationId: String): Int

    /** Cycle 0037：备份 / 恢复用 — 一次性拉全量会话（无 limit），不走 Flow。 */
    suspend fun loadAllConversations(): List<AddConversation>

    /** Cycle 0037：清空所有会话 + 消息 + 工作集（导入恢复时整体覆盖）。 */
    suspend fun deleteAllConversations()
}

class RoomAddConversationRepository internal constructor(
    db: TreasureDatabase,
) : AddConversationRepository {
    private val dao = db.conversationDao()
    private val itemDao = db.conversationItemDao()
    private val json = Json { ignoreUnknownKeys = true }

    override fun observeItems(conversationId: String): Flow<List<ConversationItem>> =
        itemDao.observeForConversation(conversationId).map { rows ->
            rows.map { it.toDomain(json) }
        }

    override suspend fun loadItems(conversationId: String): List<ConversationItem> =
        itemDao.loadForConversation(conversationId).map { it.toDomain(json) }

    override suspend fun upsertItem(item: ConversationItem) {
        itemDao.upsert(item.toEntity(json))
    }

    override suspend fun deleteItem(itemId: String) {
        itemDao.delete(itemId)
    }

    override suspend fun nextSortOrder(conversationId: String): Int =
        itemDao.maxSortOrder(conversationId) + 1

    override fun observeRecent(limit: Int): Flow<List<AddConversation>> =
        dao.observeConversations(limit).map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun upsert(conversation: AddConversation) {
        dao.upsert(
            ConversationEntity(
                id = conversation.id,
                title = conversation.title,
                createdAt = conversation.createdAt,
                updatedAt = conversation.updatedAt,
            ),
        )
    }

    override suspend fun delete(conversationId: String) {
        dao.clearMessages(conversationId)
        dao.deleteConversation(conversationId)
    }

    override suspend fun loadMessages(conversationId: String): List<AddConversationMessage> =
        dao.loadMessages(conversationId).map { it.toDomain(json) }

    override suspend fun appendMessage(conversationId: String, message: AddConversationMessage) {
        dao.upsertMessage(message.toEntity(conversationId, json))
    }

    override suspend fun replaceMessages(
        conversationId: String,
        messages: List<AddConversationMessage>,
    ) {
        dao.clearMessages(conversationId)
        messages.forEach { dao.upsertMessage(it.toEntity(conversationId, json)) }
    }

    override suspend fun loadAllConversations(): List<AddConversation> =
        dao.loadAllConversations().map { it.toDomain() }

    override suspend fun deleteAllConversations() {
        dao.deleteAllMessages()
        dao.deleteAllConversationItems()
        dao.deleteAllConversations()
    }

    companion object {
        fun create(context: Context): AddConversationRepository =
            RoomAddConversationRepository(TreasureDatabase.get(context))
    }
}

private fun ConversationEntity.toDomain() = AddConversation(
    id = id, title = title, createdAt = createdAt, updatedAt = updatedAt,
)

private fun ConversationMessageEntity.toDomain(json: Json): AddConversationMessage = when (role) {
    "assistant" -> AddConversationMessage.Assistant(id, text.orEmpty(), createdAt)
    "user" -> AddConversationMessage.User(id, text.orEmpty(), createdAt)
    "user_photo" -> AddConversationMessage.UserPhoto(id, photoUri.orEmpty(), createdAt)
    "user_voice" -> AddConversationMessage.UserVoice(
        id, text.orEmpty(), voiceDuration.orEmpty().ifBlank { "0:04" }, createdAt,
        audioPath = voicePath,
    )
    "draft_cta" -> {
        // Cycle 0024：DraftCta 的 status 复用 text 列存（"pending" / "accepted" /
        // "rejected"），免去 schema 迁移。老数据 text=null → 当 Pending。
        val draft = draftJson?.let {
            runCatching { json.decodeFromString(ItemDraft.serializer(), it) }.getOrNull()
        } ?: ItemDraft()
        val status = when (text?.lowercase()) {
            "accepted" -> DraftCtaStatus.Accepted
            "rejected" -> DraftCtaStatus.Rejected
            else -> DraftCtaStatus.Pending
        }
        // Cycle 0032：旧 draft_cta 行 actionKind = null → 退化为 Create。
        val kind = when (actionKind?.lowercase()) {
            "modify" -> DraftCtaActionKind.Modify
            else -> DraftCtaActionKind.Create
        }
        // Cycle 0034：解析 JSON 列里的 photoAssignments；老行（v13 前）NULL → []。
        val assignments: List<ResolvedPhotoAssignment> = photoAssignmentsJson?.let {
            runCatching {
                json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(ResolvedPhotoAssignment.serializer()),
                    it,
                )
            }.getOrNull()
        }.orEmpty()
        AddConversationMessage.DraftCta(
            id, draft, fieldCount ?: 0, status, createdAt,
            actionKind = kind,
            targetCiId = targetId,
            photoAssignments = assignments,
        )
    }
    "draft_confirmed" -> {
        val draft = draftJson?.let {
            runCatching { json.decodeFromString(ItemDraft.serializer(), it) }.getOrNull()
        } ?: ItemDraft()
        AddConversationMessage.DraftConfirmed(id, draft, fieldCount ?: 0, createdAt)
    }
    // Cycle 0031：commit 把草稿落到图鉴后写的封存标记 — saved item id 存 text。
    "committed" -> AddConversationMessage.Committed(id, text.orEmpty(), createdAt)
    else -> AddConversationMessage.Assistant(id, text.orEmpty(), createdAt)
}

private fun AddConversationMessage.toEntity(
    conversationId: String,
    json: Json,
): ConversationMessageEntity = when (this) {
    is AddConversationMessage.Assistant -> ConversationMessageEntity(
        id = id, conversationId = conversationId, role = "assistant",
        text = text, photoUri = null, voiceDuration = null,
        draftJson = null, fieldCount = null, createdAt = createdAt,
    )
    is AddConversationMessage.User -> ConversationMessageEntity(
        id = id, conversationId = conversationId, role = "user",
        text = text, photoUri = null, voiceDuration = null,
        draftJson = null, fieldCount = null, createdAt = createdAt,
    )
    is AddConversationMessage.UserPhoto -> ConversationMessageEntity(
        id = id, conversationId = conversationId, role = "user_photo",
        text = null, photoUri = uri, voiceDuration = null,
        draftJson = null, fieldCount = null, createdAt = createdAt,
    )
    is AddConversationMessage.UserVoice -> ConversationMessageEntity(
        id = id, conversationId = conversationId, role = "user_voice",
        text = text, photoUri = null, voiceDuration = duration,
        draftJson = null, fieldCount = null, createdAt = createdAt,
        voicePath = audioPath,
    )
    is AddConversationMessage.DraftCta -> ConversationMessageEntity(
        id = id, conversationId = conversationId, role = "draft_cta",
        text = status.name.lowercase(), // pending / accepted / rejected
        photoUri = null, voiceDuration = null,
        draftJson = json.encodeToString(draft),
        fieldCount = fieldCount, createdAt = createdAt,
        actionKind = when (actionKind) {
            DraftCtaActionKind.Create -> "create"
            DraftCtaActionKind.Modify -> "modify"
        },
        targetId = targetCiId,
        photoAssignmentsJson = if (photoAssignments.isEmpty()) null
        else json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(ResolvedPhotoAssignment.serializer()),
            photoAssignments,
        ),
    )
    is AddConversationMessage.DraftConfirmed -> ConversationMessageEntity(
        id = id, conversationId = conversationId, role = "draft_confirmed",
        text = null, photoUri = null, voiceDuration = null,
        draftJson = json.encodeToString(draft),
        fieldCount = fieldCount, createdAt = createdAt,
    )
    is AddConversationMessage.Committed -> ConversationMessageEntity(
        id = id, conversationId = conversationId, role = "committed",
        text = savedItemId, photoUri = null, voiceDuration = null,
        draftJson = null, fieldCount = null, createdAt = createdAt,
    )
}

private fun com.treasure.core.room.ConversationItemEntity.toDomain(json: Json): ConversationItem {
    val draft = draftJson?.let {
        runCatching { json.decodeFromString(ItemDraft.serializer(), it) }.getOrNull()
    }
    val parsedStatus = when (status.lowercase()) {
        "saved" -> ConversationItemStatus.SAVED
        "modified" -> ConversationItemStatus.MODIFIED
        else -> ConversationItemStatus.PENDING
    }
    return ConversationItem(
        id = id,
        conversationId = conversationId,
        draft = draft,
        itemRef = itemRef,
        status = parsedStatus,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}

private fun ConversationItem.toEntity(json: Json): com.treasure.core.room.ConversationItemEntity =
    com.treasure.core.room.ConversationItemEntity(
        id = id,
        conversationId = conversationId,
        draftJson = draft?.let { json.encodeToString(ItemDraft.serializer(), it) },
        itemRef = itemRef,
        status = status.name.lowercase(),
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
