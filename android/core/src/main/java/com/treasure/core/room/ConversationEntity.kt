package com.treasure.core.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cycle 0010：录入页对话历史。一行一段对话，标题在 AI 出第一份草稿
 * 后写为 "<品牌> <型号>"，否则为 "New entry"。
 */
@Entity(tableName = "add_conversations")
internal data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * Cycle 0010：单条对话消息。角色用字符串而不是枚举，方便后续扩展
 * 不写迁移；payload 字段按角色取舍。
 */
@Entity(
    tableName = "add_messages",
    indices = [Index(value = ["conversation_id"], name = "index_add_messages_conversation_id")],
)
internal data class ConversationMessageEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    /** 'assistant' | 'user' | 'user_photo' | 'user_voice' | 'draft_cta' */
    val role: String,
    val text: String?,
    @ColumnInfo(name = "photo_uri") val photoUri: String?,
    @ColumnInfo(name = "voice_duration") val voiceDuration: String?,
    @ColumnInfo(name = "draft_json") val draftJson: String?,
    @ColumnInfo(name = "field_count") val fieldCount: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
