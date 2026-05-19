package com.treasure.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMessage(message: ConversationMessageEntity)

    @Query("DELETE FROM add_messages WHERE conversation_id = :conversationId")
    suspend fun clearMessages(conversationId: String)

    @Query("DELETE FROM add_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("SELECT * FROM add_conversations ORDER BY updated_at DESC LIMIT :limit")
    fun observeConversations(limit: Int = 20): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM add_messages WHERE conversation_id = :conversationId ORDER BY created_at ASC")
    suspend fun loadMessages(conversationId: String): List<ConversationMessageEntity>

    /** Cycle 0037：备份导出 — 一次拉全量，不分页。 */
    @Query("SELECT * FROM add_conversations ORDER BY updated_at DESC")
    suspend fun loadAllConversations(): List<ConversationEntity>

    /** Cycle 0037：导入前整体清空。 */
    @Query("DELETE FROM add_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM add_conversations")
    suspend fun deleteAllConversations()

    @Query("DELETE FROM conversation_items")
    suspend fun deleteAllConversationItems()
}
