package com.treasure.backup

import kotlinx.serialization.Serializable

/**
 * Cycle 0037：备份 zip 顶层 manifest。
 *
 * 写在 zip 根 `manifest.json`，导入时先解这一份；不兼容直接拒绝。
 *
 *  - [schemaVersion]：备份格式本身的版本号（不是 Room schema）。从 1 起。
 *  - [roomSchemaVersion]：导出时 Room 的版本（v16）；导入若发现 backup 比当前
 *    Room 新，认为是从未来回退，拒绝。同版本 / 旧版本都允许（旧版会被当前
 *    JSON decoder 用 ignoreUnknownKeys 模式吃掉）。
 *  - [appVersionCode] / [appVersionName]：纯展示用，issue 反馈时方便对账。
 *  - [exportedAtMillis]：System.currentTimeMillis()。
 *  - [counts]：清单。导入时拿来给 UI 显示 "将恢复 N 物品 / M 分类 / ..."。
 *  - [photos]：照片清单 — 每条是 `(zipPath, fileSize, relPath)`。relPath 是
 *    解压目标相对 filesDir 的位置；导入时直接按它写回 filesDir。
 *
 * 不打包：API key / EncryptedSharedPreferences / 临时会话产物（conversation-photos
 * / draft-photos / voice-cache / chat-files / captures）。
 */
@Serializable
data class BackupManifest(
    val schemaVersion: Int = SCHEMA_VERSION_CURRENT,
    val roomSchemaVersion: Int,
    val appVersionCode: Int,
    val appVersionName: String,
    val exportedAtMillis: Long,
    val counts: BackupCounts,
    val photos: List<BackupPhotoEntry> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION_CURRENT: Int = 1
    }
}

@Serializable
data class BackupCounts(
    val items: Int = 0,
    val categories: Int = 0,
    val conversations: Int = 0,
    val messages: Int = 0,
    val conversationItems: Int = 0,
    val photoFiles: Int = 0,
)

@Serializable
data class BackupPhotoEntry(
    /** zip 内的相对路径，比如 `photos/<itemId>/<uuid>.jpg`。 */
    val zipPath: String,
    /** 导入时落到 filesDir 下的目标相对路径（一般等于 zipPath）。 */
    val relPath: String,
    val sizeBytes: Long,
)
