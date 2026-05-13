package com.treasure.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ConversationItemDao {

    @Query(
        "SELECT * FROM conversation_items WHERE conversation_id = :conversationId " +
            "ORDER BY sort_order ASC, created_at ASC",
    )
    fun observeForConversation(conversationId: String): Flow<List<ConversationItemEntity>>

    @Query(
        "SELECT * FROM conversation_items WHERE conversation_id = :conversationId " +
            "ORDER BY sort_order ASC, created_at ASC",
    )
    suspend fun loadForConversation(conversationId: String): List<ConversationItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ConversationItemEntity)

    @Query("DELETE FROM conversation_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM conversation_items WHERE conversation_id = :conversationId")
    suspend fun clearConversation(conversationId: String)

    @Query(
        "SELECT COALESCE(MAX(sort_order), -1) FROM conversation_items WHERE conversation_id = :conversationId",
    )
    suspend fun maxSortOrder(conversationId: String): Int
}
