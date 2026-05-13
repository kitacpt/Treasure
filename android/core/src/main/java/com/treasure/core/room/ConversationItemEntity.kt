package com.treasure.core.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cycle 0031：一段录入会话的"工作集"。每行一个候选物品，三态：
 *
 * - PENDING：只有 [draftJson]，[itemRef] 为空 — AI 提出 / 用户手动起的新草稿。
 * - SAVED：只有 [itemRef]（指向 items 表里的真物品），[draftJson] 为空 —
 *   已落到图鉴。
 * - MODIFIED：[itemRef] + [draftJson] 都有 — 之前已经入图鉴的物品，现在用户
 *   或 AI 提了一份"下一版"草稿，等用户 confirm 才会覆盖原物品。
 */
@Entity(
    tableName = "conversation_items",
    indices = [
        Index(value = ["conversation_id"], name = "index_conversation_items_conversation_id"),
    ],
)
internal data class ConversationItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    /** [com.treasure.core.ai.ItemDraft] 的 JSON。SAVED 行可为空。 */
    @ColumnInfo(name = "draft_json") val draftJson: String?,
    /** 落到图鉴里的 Item id。PENDING 行为空。 */
    @ColumnInfo(name = "item_ref") val itemRef: String?,
    /** "pending" / "saved" / "modified" */
    val status: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
