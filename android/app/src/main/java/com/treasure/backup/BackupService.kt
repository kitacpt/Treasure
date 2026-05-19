package com.treasure.backup

import android.content.Context
import android.net.Uri
import com.treasure.TreasureApp
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.domain.PhotoCrop
import com.treasure.core.repo.AddConversation
import com.treasure.core.repo.AddConversationMessage
import com.treasure.core.repo.AddConversationRepository
import com.treasure.core.repo.CategoryRepository
import com.treasure.core.repo.ConversationItem
import com.treasure.core.repo.ConversationItemStatus
import com.treasure.core.repo.DraftCtaActionKind
import com.treasure.core.repo.DraftCtaStatus
import com.treasure.core.repo.ItemRepository
import com.treasure.core.repo.ResolvedPhotoAssignment
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Cycle 0037：数据备份 / 恢复服务。
 *
 * 导出：把 4 张 Room 表 + filesDir 下 photos / category-photos 目录全部打成
 * 一个 zip。zip 内部结构：
 * - manifest.json
 * - data/items.json
 * - data/categories.json
 * - data/conversations.json
 * - data/messages.json
 * - data/conversation_items.json
 * - photos/[itemId]/[uuid].jpg ...
 * - category-photos/[categoryId]/[uuid].jpg ...
 *
 * 导入：解析 manifest.json，校验版本兼容；清空 Room 三张表 + filesDir/photos
 * + filesDir/category-photos；写回 data 下 5 份 JSON 回 Room，photos 目录回
 * filesDir。失败时 [BackupProgress.Failed] 上报；本地可能处于半完成状态，UI
 * 应事先做"建议先导出当前数据作为安全副本"的二次确认。
 *
 * 不打包：EncryptedSharedPreferences（API key 换机后请重新填）、临时会话
 * 产物（conversation-photos / draft-photos / voice-cache / chat-files /
 * captures — 删会话本就清理）。
 *
 * 进度通过回调上报；调用方负责把它桥到 StateFlow。所有 I/O 应在
 * Dispatchers.IO 上调用。
 */
class BackupService(
    private val app: TreasureApp,
    private val itemRepo: ItemRepository,
    private val categoryRepo: CategoryRepository,
    private val convRepo: AddConversationRepository,
) {
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val filesDir: File get() = app.filesDir

    // ─── EXPORT ───────────────────────────────────────────────────────────

    /**
     * 把当前数据导到 [destUri]（来自 ActivityResultContracts.CreateDocument）。
     * [report] 同步回调；调用方应在 IO 调度器上调用。
     */
    suspend fun exportToZip(
        destUri: Uri,
        report: (BackupProgress) -> Unit,
    ): BackupProgress {
        try {
            report(BackupProgress.Running("收集物品 ...", percent = 5))
            val items = itemRepo.items.first()

            report(BackupProgress.Running("收集分类 ...", percent = 10))
            val categories = categoryRepo.loadAll()

            report(BackupProgress.Running("收集会话 ...", percent = 15))
            val conversations = convRepo.loadAllConversations()
            val allMessages = mutableListOf<BackupMessage>()
            val allConvItems = mutableListOf<BackupConversationItem>()
            conversations.forEachIndexed { idx, c ->
                val msgs = convRepo.loadMessages(c.id).map { it.toBackup(c.id) }
                allMessages.addAll(msgs)
                allConvItems.addAll(
                    convRepo.loadItems(c.id).map { it.toBackup() },
                )
                if (idx % 5 == 0) {
                    report(BackupProgress.Running(
                        "收集会话 ${idx + 1}/${conversations.size} ...",
                        percent = 15 + (10 * (idx + 1) / maxOf(1, conversations.size)),
                    ))
                }
            }

            // 收集照片清单：item 的 photos / avatarPhotoPath / callouts keys /
            // photoCrops keys 都可能引用文件；分类的 heroPhotoPath 也是。我们直
            // 接扫两个目录的全部 jpg，避免漏（数据库不一致时也能恢复）。
            report(BackupProgress.Running("扫描照片 ...", percent = 28))
            val photoEntries = collectPhotoFiles()

            // 整理 wire DTO（path 改为相对 filesDir 的）。
            val backupItems = items.map { it.toBackup() }
            val backupCategories = categories.map { it.toBackup() }
            val backupConversations = conversations.map { it.toBackup() }
            val data = BackupData(
                items = backupItems,
                categories = backupCategories,
                conversations = backupConversations,
                messages = allMessages,
                conversationItems = allConvItems,
            )

            val manifest = BackupManifest(
                roomSchemaVersion = ROOM_SCHEMA_VERSION,
                appVersionCode = appVersionCode(),
                appVersionName = appVersionName(),
                exportedAtMillis = System.currentTimeMillis(),
                counts = BackupCounts(
                    items = backupItems.size,
                    categories = backupCategories.size,
                    conversations = backupConversations.size,
                    messages = allMessages.size,
                    conversationItems = allConvItems.size,
                    photoFiles = photoEntries.size,
                ),
                photos = photoEntries,
            )

            report(BackupProgress.Running("打包写盘 ...", percent = 32))
            val written = app.contentResolver.openOutputStream(destUri, "w")
                ?: error("无法写入目标文件（应用没拿到 URI 写权限）")
            written.use { os ->
                ZipOutputStream(os.buffered()).use { zip ->
                    // 1) manifest.json
                    putJsonEntry(zip, "manifest.json", json.encodeToString(
                        BackupManifest.serializer(), manifest,
                    ))
                    // 2) data/*.json
                    putJsonEntry(zip, "data/items.json", json.encodeToString(
                        ListSerializer(BackupItem.serializer()), backupItems,
                    ))
                    putJsonEntry(zip, "data/categories.json", json.encodeToString(
                        ListSerializer(BackupCategory.serializer()), backupCategories,
                    ))
                    putJsonEntry(zip, "data/conversations.json", json.encodeToString(
                        ListSerializer(BackupConversation.serializer()), backupConversations,
                    ))
                    putJsonEntry(zip, "data/messages.json", json.encodeToString(
                        ListSerializer(BackupMessage.serializer()), allMessages,
                    ))
                    putJsonEntry(zip, "data/conversation_items.json", json.encodeToString(
                        ListSerializer(BackupConversationItem.serializer()), allConvItems,
                    ))
                    // 3) 照片
                    val total = photoEntries.size
                    photoEntries.forEachIndexed { idx, e ->
                        val src = File(filesDir, e.relPath)
                        if (src.exists()) {
                            zip.putNextEntry(ZipEntry(e.zipPath))
                            src.inputStream().buffered().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                        if (total > 0 && (idx % 4 == 0 || idx == total - 1)) {
                            val p = 35 + (60 * (idx + 1) / total)
                            report(BackupProgress.Running(
                                "写入照片 ${idx + 1}/$total ...",
                                percent = p.coerceAtMost(95),
                            ))
                        }
                    }
                }
            }

            val size = runCatching {
                app.contentResolver.openFileDescriptor(destUri, "r")?.use { it.statSize }
            }.getOrNull() ?: -1L

            val done = BackupProgress.ExportDone(
                sizeBytes = if (size > 0) size else 0,
                items = backupItems.size,
                photoFiles = photoEntries.size,
                fileName = displayName(destUri),
            )
            report(done)
            return done
        } catch (t: Throwable) {
            val failed = BackupProgress.Failed("导出", t.message ?: t::class.java.simpleName)
            report(failed)
            return failed
        }
    }

    // ─── IMPORT ───────────────────────────────────────────────────────────

    /**
     * 从 [srcUri] 读 zip 并整体覆盖当前数据。
     *
     * 失败时本地可能处于半完成状态 — UI 必须事先做"建议先导出当前数据作为安全
     * 副本"的二次确认弹窗。
     */
    suspend fun importFromZip(
        srcUri: Uri,
        report: (BackupProgress) -> Unit,
    ): BackupProgress {
        try {
            report(BackupProgress.Running("打开备份文件 ...", percent = 2))
            // 解到临时目录，先读 manifest 校验，再整体生效。
            val tmpDir = File(app.cacheDir, "backup-restore-${System.currentTimeMillis()}")
            tmpDir.mkdirs()
            try {
                val totalEntries = unzipToDir(srcUri, tmpDir) { idx ->
                    if (idx % 8 == 0) {
                        report(BackupProgress.Running("解压 $idx 项 ...", percent = -1))
                    }
                }

                report(BackupProgress.Running("校验 manifest ...", percent = 35))
                val manifestFile = File(tmpDir, "manifest.json")
                if (!manifestFile.exists()) error("zip 内未找到 manifest.json，可能不是 Treasure 备份")
                val manifest = json.decodeFromString(
                    BackupManifest.serializer(),
                    manifestFile.readText(),
                )
                if (manifest.schemaVersion > BackupManifest.SCHEMA_VERSION_CURRENT) {
                    error("备份格式版本 ${manifest.schemaVersion} 比当前 app（${BackupManifest.SCHEMA_VERSION_CURRENT}）新，请升级 app 后再恢复")
                }
                if (manifest.roomSchemaVersion > ROOM_SCHEMA_VERSION) {
                    error("备份来自更新版本的 app（Room v${manifest.roomSchemaVersion}），当前 app（v$ROOM_SCHEMA_VERSION）无法恢复")
                }

                report(BackupProgress.Running("读取数据 ...", percent = 42))
                val items = loadJsonList(File(tmpDir, "data/items.json"), BackupItem.serializer())
                val categories = loadJsonList(File(tmpDir, "data/categories.json"), BackupCategory.serializer())
                val conversations = loadJsonList(File(tmpDir, "data/conversations.json"), BackupConversation.serializer())
                val messages = loadJsonList(File(tmpDir, "data/messages.json"), BackupMessage.serializer())
                val convItems = loadJsonList(File(tmpDir, "data/conversation_items.json"), BackupConversationItem.serializer())

                // 清旧 — 这一步起本地数据就开始覆盖了。失败的话 catch 块只能把
                // 错误上报，UI 二次确认前会提示用户先导出做安全副本。
                report(BackupProgress.Running("清空当前数据 ...", percent = 50))
                itemRepo.deleteAll()
                convRepo.deleteAllConversations()
                clearPhotoDirs()

                report(BackupProgress.Running("写入分类 ...", percent = 55))
                categoryRepo.replaceAll(categories.map { it.toDomain() })

                report(BackupProgress.Running("写入物品 ${items.size} ...", percent = 60))
                items.forEach { itemRepo.upsert(it.toDomain(filesDir)) }

                report(BackupProgress.Running("写入会话 ...", percent = 68))
                conversations.forEach { convRepo.upsert(it.toDomain()) }
                // 消息按 conversation 分组逐个回写。
                val byConv = messages.groupBy { it.conversationId }
                byConv.forEach { (cid, msgs) ->
                    convRepo.replaceMessages(cid, msgs.map { it.toDomain() })
                }
                convItems.forEach { convRepo.upsertItem(it.toDomain()) }

                // 拷贝照片：tmp/photos/* 和 tmp/category-photos/* → filesDir/*。
                report(BackupProgress.Running("恢复照片 ...", percent = 78))
                val copiedCount = copyDirIntoFilesDir(File(tmpDir, "photos"), "photos") +
                    copyDirIntoFilesDir(File(tmpDir, "category-photos"), "category-photos")

                val done = BackupProgress.ImportDone(
                    items = items.size,
                    photoFiles = copiedCount,
                )
                report(done)
                return done
            } finally {
                tmpDir.deleteRecursively()
            }
        } catch (t: Throwable) {
            val failed = BackupProgress.Failed("导入", t.message ?: t::class.java.simpleName)
            report(failed)
            return failed
        }
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    /** 扫两个目录的全部文件，得到相对 filesDir 的 [BackupPhotoEntry] 列表。 */
    private fun collectPhotoFiles(): List<BackupPhotoEntry> {
        val out = mutableListOf<BackupPhotoEntry>()
        listOf("photos", "category-photos").forEach { topRel ->
            val top = File(filesDir, topRel)
            if (!top.isDirectory) return@forEach
            top.walkTopDown().filter { it.isFile }.forEach { f ->
                val rel = f.toRelativeString(filesDir).replace(File.separatorChar, '/')
                out.add(BackupPhotoEntry(zipPath = rel, relPath = rel, sizeBytes = f.length()))
            }
        }
        return out
    }

    private fun putJsonEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    /**
     * 把 zip 完整解压到 [destDir]。返回总条目数。回调 [onEntry] 用于驱动进度。
     * 防 path-traversal：拒绝任何 `..` 或绝对路径条目。
     */
    private fun unzipToDir(
        src: Uri,
        destDir: File,
        onEntry: (Int) -> Unit,
    ): Int {
        var count = 0
        val input = app.contentResolver.openInputStream(src)
            ?: error("无法读取备份文件（应用没拿到 URI 读权限）")
        input.use { ins ->
            ZipInputStream(ins.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    // path-traversal 防护：reject `..` 和绝对路径
                    if (name.contains("..") || name.startsWith("/")) {
                        zip.closeEntry(); continue
                    }
                    val target = File(destDir, name)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().buffered().use { os -> zip.copyTo(os) }
                    }
                    zip.closeEntry()
                    count += 1
                    onEntry(count)
                }
            }
        }
        return count
    }

    /** filesDir 下 photos / category-photos 整目录删除。 */
    private fun clearPhotoDirs() {
        listOf("photos", "category-photos").forEach { rel ->
            File(filesDir, rel).deleteRecursively()
        }
    }

    /**
     * 把 [srcRoot] 下整棵树拷到 `filesDir/<topRel>/`。返回写入的文件数。
     */
    private fun copyDirIntoFilesDir(srcRoot: File, topRel: String): Int {
        if (!srcRoot.isDirectory) return 0
        var n = 0
        srcRoot.walkTopDown().filter { it.isFile }.forEach { f ->
            val rel = f.toRelativeString(srcRoot)
            val dest = File(filesDir, "$topRel/$rel")
            dest.parentFile?.mkdirs()
            f.inputStream().buffered().use { input ->
                dest.outputStream().buffered().use { out -> input.copyTo(out) }
            }
            n += 1
        }
        return n
    }

    private fun <T> loadJsonList(
        file: File,
        elementSerializer: kotlinx.serialization.KSerializer<T>,
    ): List<T> {
        if (!file.exists()) return emptyList()
        return json.decodeFromString(ListSerializer(elementSerializer), file.readText())
    }

    private fun appVersionCode(): Int = runCatching {
        @Suppress("DEPRECATION")
        app.packageManager.getPackageInfo(app.packageName, 0).versionCode
    }.getOrDefault(0)

    private fun appVersionName(): String = runCatching {
        app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty()
    }.getOrDefault("")

    private fun displayName(uri: Uri): String =
        runCatching {
            app.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "backup.zip"

    companion object {
        /** Cycle 0037：当前 app 用的 Room schema。和 TreasureDatabase 里的 version 保持同步。 */
        const val ROOM_SCHEMA_VERSION: Int = 16

        /** 推荐的输出文件名（UI 调用 CreateDocument 时作默认值）。 */
        fun suggestedFileName(now: Long = System.currentTimeMillis()): String {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
            val stamp = "%04d%02d%02d-%02d%02d%02d".format(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH),
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE),
                cal.get(java.util.Calendar.SECOND),
            )
            return "treasure-backup-$stamp.zip"
        }
    }
}

// ─── domain ↔ wire conversion ───────────────────────────────────────────

private fun Item.toBackup(): BackupItem = BackupItem(
    id = id,
    category = category,
    brand = brand,
    model = model,
    nickname = nickname,
    acquired = acquired,
    parted = parted,
    status = status.name,
    palette = palette,
    oneLiner = oneLiner,
    heroVector = heroVector.name,
    specs = specs,
    history = history,
    photos = photos.map { it.toRelPath() },
    callouts = callouts.mapKeys { (k, _) -> k.toRelPath() },
    avatarPhotoPath = avatarPhotoPath?.toRelPath(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    sortOrder = sortOrder,
    photoCrops = photoCrops.mapKeys { (k, _) -> k.toRelPath() },
)

private fun BackupItem.toDomain(filesDir: File): Item = Item(
    id = id,
    category = category,
    brand = brand,
    model = model,
    nickname = nickname,
    acquired = acquired,
    parted = parted,
    status = runCatching { ItemStatus.valueOf(status) }.getOrDefault(ItemStatus.OWNED),
    palette = palette,
    oneLiner = oneLiner,
    heroVector = runCatching { HeroVector.valueOf(heroVector) }.getOrDefault(HeroVector.GENERIC),
    specs = specs,
    history = history,
    photos = photos.map { it.toAbsPath(filesDir) },
    callouts = callouts.mapKeys { (k, _) -> k.toAbsPath(filesDir) },
    avatarPhotoPath = avatarPhotoPath?.toAbsPath(filesDir),
    createdAt = createdAt,
    updatedAt = updatedAt,
    sortOrder = sortOrder,
    photoCrops = photoCrops.mapKeys { (k, _) -> k.toAbsPath(filesDir) },
)

private fun CategoryInfo.toBackup(): BackupCategory = BackupCategory(
    id = id,
    isBuiltIn = isBuiltIn,
    nameZh = nameZh,
    nameEn = nameEn,
    heroVector = heroVector.name,
    hidden = hidden,
    sortOrder = sortOrder,
    heroPhotoPath = heroPhotoPath,  // 已经是 filesDir 子路径
)

private fun BackupCategory.toDomain(): CategoryInfo = CategoryInfo(
    id = id,
    nameZh = nameZh,
    nameEn = nameEn,
    heroVector = runCatching { HeroVector.valueOf(heroVector) }.getOrDefault(HeroVector.GENERIC),
    hidden = hidden,
    sortOrder = sortOrder,
    isBuiltIn = isBuiltIn,
    heroPhotoPath = heroPhotoPath,
)

private fun AddConversation.toBackup() = BackupConversation(id, title, createdAt, updatedAt)
private fun BackupConversation.toDomain() = AddConversation(id, title, createdAt, updatedAt)

private fun AddConversationMessage.toBackup(conversationId: String): BackupMessage {
    // 这里直接重新 encode 一遍：所有 message 都走 RoomAddConversationRepository
    // 现有 toEntity 的路径，但 Repository 没暴露这个内部 helper。我们在导出端用
    // 等价逻辑（cycle 0037 跟 cycle 0034 的 entity mapping 对齐）。
    val jsonFmt = backupJson
    return when (this) {
        is AddConversationMessage.Assistant -> BackupMessage(
            id = id, conversationId = conversationId, role = "assistant",
            text = text, createdAt = createdAt,
        )
        is AddConversationMessage.User -> BackupMessage(
            id = id, conversationId = conversationId, role = "user",
            text = text, createdAt = createdAt,
        )
        is AddConversationMessage.UserPhoto -> BackupMessage(
            id = id, conversationId = conversationId, role = "user_photo",
            photoUri = uri, createdAt = createdAt,
        )
        is AddConversationMessage.UserVoice -> BackupMessage(
            id = id, conversationId = conversationId, role = "user_voice",
            text = text, voiceDuration = duration, voicePath = audioPath,
            createdAt = createdAt,
        )
        is AddConversationMessage.DraftCta -> BackupMessage(
            id = id, conversationId = conversationId, role = "draft_cta",
            text = status.name.lowercase(),
            draftJson = jsonFmt.encodeToString(
                com.treasure.core.ai.ItemDraft.serializer(), draft,
            ),
            fieldCount = fieldCount,
            createdAt = createdAt,
            actionKind = when (actionKind) {
                DraftCtaActionKind.Create -> "create"
                DraftCtaActionKind.Modify -> "modify"
            },
            targetId = targetCiId,
            photoAssignmentsJson = if (photoAssignments.isEmpty()) null
            else jsonFmt.encodeToString(
                ListSerializer(ResolvedPhotoAssignment.serializer()),
                photoAssignments,
            ),
        )
        is AddConversationMessage.DraftConfirmed -> BackupMessage(
            id = id, conversationId = conversationId, role = "draft_confirmed",
            draftJson = jsonFmt.encodeToString(
                com.treasure.core.ai.ItemDraft.serializer(), draft,
            ),
            fieldCount = fieldCount, createdAt = createdAt,
        )
        is AddConversationMessage.Committed -> BackupMessage(
            id = id, conversationId = conversationId, role = "committed",
            text = savedItemId, createdAt = createdAt,
        )
    }
}

private fun BackupMessage.toDomain(): AddConversationMessage {
    val jsonFmt = backupJson
    fun parseDraft(): com.treasure.core.ai.ItemDraft = draftJson?.let {
        runCatching { jsonFmt.decodeFromString(com.treasure.core.ai.ItemDraft.serializer(), it) }
            .getOrNull()
    } ?: com.treasure.core.ai.ItemDraft()
    return when (role) {
        "assistant" -> AddConversationMessage.Assistant(id, text.orEmpty(), createdAt)
        "user" -> AddConversationMessage.User(id, text.orEmpty(), createdAt)
        "user_photo" -> AddConversationMessage.UserPhoto(id, photoUri.orEmpty(), createdAt)
        "user_voice" -> AddConversationMessage.UserVoice(
            id, text.orEmpty(),
            voiceDuration.orEmpty().ifBlank { "0:04" },
            createdAt, audioPath = voicePath,
        )
        "draft_cta" -> {
            val status = when (text?.lowercase()) {
                "accepted" -> DraftCtaStatus.Accepted
                "rejected" -> DraftCtaStatus.Rejected
                else -> DraftCtaStatus.Pending
            }
            val kind = when (actionKind?.lowercase()) {
                "modify" -> DraftCtaActionKind.Modify
                else -> DraftCtaActionKind.Create
            }
            val assignments = photoAssignmentsJson?.let {
                runCatching {
                    jsonFmt.decodeFromString(
                        ListSerializer(ResolvedPhotoAssignment.serializer()), it,
                    )
                }.getOrNull()
            }.orEmpty()
            AddConversationMessage.DraftCta(
                id, parseDraft(), fieldCount ?: 0, status, createdAt,
                actionKind = kind, targetCiId = targetId,
                photoAssignments = assignments,
            )
        }
        "draft_confirmed" -> AddConversationMessage.DraftConfirmed(
            id, parseDraft(), fieldCount ?: 0, createdAt,
        )
        "committed" -> AddConversationMessage.Committed(id, text.orEmpty(), createdAt)
        else -> AddConversationMessage.Assistant(id, text.orEmpty(), createdAt)
    }
}

private fun ConversationItem.toBackup() = BackupConversationItem(
    id = id,
    conversationId = conversationId,
    draftJson = draft?.let { backupJson.encodeToString(com.treasure.core.ai.ItemDraft.serializer(), it) },
    itemRef = itemRef,
    status = status.name.lowercase(),
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun BackupConversationItem.toDomain() = ConversationItem(
    id = id,
    conversationId = conversationId,
    draft = draftJson?.let {
        runCatching { backupJson.decodeFromString(com.treasure.core.ai.ItemDraft.serializer(), it) }
            .getOrNull()
    },
    itemRef = itemRef,
    status = when (status.lowercase()) {
        "saved" -> ConversationItemStatus.SAVED
        "modified" -> ConversationItemStatus.MODIFIED
        else -> ConversationItemStatus.PENDING
    },
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

// 共享一个 json formatter 给 message draftJson encode / decode 用，
// 必须 ignoreUnknownKeys 兼容老 draft 字段。
private val backupJson: Json = Json { ignoreUnknownKeys = true }

// 路径转换 — 全部相对 filesDir。我们存进 zip 的就是相对路径；导入时用
// 当前进程的 filesDir 拼回 absolute。
private fun String.toRelPath(): String {
    if (this.isEmpty()) return this
    // 去掉 filesDir 前缀（若有）。剩下的就是 `photos/<itemId>/...` 形式。
    val markers = listOf("/photos/", "/category-photos/")
    for (m in markers) {
        val idx = this.indexOf(m)
        if (idx >= 0) return this.substring(idx + 1).trim('/')
    }
    // 已经是相对路径就原样回。
    return this
}

private fun String.toAbsPath(filesDir: File): String {
    if (this.isEmpty()) return this
    // 如果是绝对路径，并且能落在 filesDir 下，直接用；否则按相对路径拼。
    if (this.startsWith("/")) {
        // 已经是绝对路径 — 多半是同设备导入，重新映射到当前 filesDir。
        val markers = listOf("/photos/", "/category-photos/")
        for (m in markers) {
            val idx = this.indexOf(m)
            if (idx >= 0) return File(filesDir, this.substring(idx + 1).trim('/')).absolutePath
        }
        return this  // 兜底原样
    }
    return File(filesDir, this).absolutePath
}
