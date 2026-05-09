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

/** 录入页对话的领域级 message —— 对应 UI 层 AddMessage。 */
sealed interface AddConversationMessage {
    val id: String
    val createdAt: Long

    data class Assistant(override val id: String, val text: String, override val createdAt: Long) : AddConversationMessage
    data class User(override val id: String, val text: String, override val createdAt: Long) : AddConversationMessage
    data class UserPhoto(override val id: String, val uri: String, override val createdAt: Long) : AddConversationMessage
    data class UserVoice(override val id: String, val text: String, val duration: String, override val createdAt: Long) : AddConversationMessage
    data class DraftCta(override val id: String, val draft: ItemDraft, val fieldCount: Int, override val createdAt: Long) : AddConversationMessage
}

data class AddConversation(
    val id: String,
    val title: String,
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
}

class RoomAddConversationRepository internal constructor(
    db: TreasureDatabase,
) : AddConversationRepository {
    private val dao = db.conversationDao()
    private val json = Json { ignoreUnknownKeys = true }

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
    )
    "draft_cta" -> {
        val draft = draftJson?.let {
            runCatching { json.decodeFromString(ItemDraft.serializer(), it) }.getOrNull()
        } ?: ItemDraft()
        AddConversationMessage.DraftCta(id, draft, fieldCount ?: 0, createdAt)
    }
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
    )
    is AddConversationMessage.DraftCta -> ConversationMessageEntity(
        id = id, conversationId = conversationId, role = "draft_cta",
        text = null, photoUri = null, voiceDuration = null,
        draftJson = json.encodeToString(draft),
        fieldCount = fieldCount, createdAt = createdAt,
    )
}
