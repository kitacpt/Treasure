package com.treasure.backup

import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.PhotoCallout
import com.treasure.core.domain.PhotoCrop
import kotlinx.serialization.Serializable

/**
 * Cycle 0037：备份导出 / 恢复用的"线协议"DTOs。和 Room 实体一一对应，
 * 但都是纯 @Serializable Kotlin 值类型，不依赖 Room / android 任何东西。
 *
 * 设计原则：
 *  - **存 JSON 列的原文**（specs_json / history_json / photos_json 之类），
 *    避免 domain 模型某天加字段、备份 schema 不破裂 — decode 时用 `ignoreUnknownKeys`
 *  - **照片相对路径**：导出时把 photos / avatarPhotoPath / callouts keys 都
 *    转成相对 filesDir 的路径（`photos/<itemId>/<uuid>.jpg`），导入时再拼
 *    成 absolute path。这样换设备 / OS 重装路径前缀变也不影响。
 *  - **不存** API key / encrypted prefs / 临时会话产物。
 */
@Serializable
data class BackupItem(
    val id: String,
    val category: String,
    val brand: String,
    val model: String,
    val nickname: String,
    val acquired: String,
    val parted: String? = null,
    val status: String,             // ItemStatus.name
    val palette: List<String> = emptyList(),
    val oneLiner: String = "",
    val heroVector: String,         // HeroVector.name
    val specs: List<HeroSpec> = emptyList(),
    val history: List<HistoryEvent> = emptyList(),
    /** 相对 filesDir 的路径。 */
    val photos: List<String> = emptyList(),
    /** key 也是相对路径。 */
    val callouts: Map<String, List<PhotoCallout>> = emptyMap(),
    val avatarPhotoPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Long = 0L,
    /** key 是相对路径。 */
    val photoCrops: Map<String, PhotoCrop> = emptyMap(),
)

@Serializable
data class BackupCategory(
    val id: String,
    val isBuiltIn: Boolean,
    val nameZh: String,
    val nameEn: String,
    val heroVector: String,
    val hidden: Boolean,
    val sortOrder: Int,
    /** 相对 filesDir 的路径。 */
    val heroPhotoPath: String? = null,
)

@Serializable
data class BackupConversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Cycle 0037：备份不解释 add_messages 的语义 — 直接存列原文（包括 JSON 列）。
 * 这样未来 message 协议怎么改都不影响存档兼容性（导入时新版 app 用自己的
 * decode 路径解读 JSON）。
 */
@Serializable
data class BackupMessage(
    val id: String,
    val conversationId: String,
    val role: String,
    val text: String? = null,
    val photoUri: String? = null,
    val voiceDuration: String? = null,
    val draftJson: String? = null,
    val fieldCount: Int? = null,
    val createdAt: Long,
    val actionKind: String? = null,
    val targetId: String? = null,
    val photoAssignmentsJson: String? = null,
    val voicePath: String? = null,
)

@Serializable
data class BackupConversationItem(
    val id: String,
    val conversationId: String,
    val draftJson: String? = null,
    val itemRef: String? = null,
    val status: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/** 顶层 wrap，方便一份 json 一刀切。 */
@Serializable
data class BackupData(
    val items: List<BackupItem> = emptyList(),
    val categories: List<BackupCategory> = emptyList(),
    val conversations: List<BackupConversation> = emptyList(),
    val messages: List<BackupMessage> = emptyList(),
    val conversationItems: List<BackupConversationItem> = emptyList(),
)
