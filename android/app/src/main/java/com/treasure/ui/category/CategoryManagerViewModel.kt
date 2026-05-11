package com.treasure.ui.category

import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.HeroVector
import com.treasure.core.repo.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Cycle 0026：分类管理抽屉 + 编辑页背后的 VM。直接代理 [CategoryRepository]，
 * UI 层不需要再去 wire Room/DAO，专注 state ↔ user gesture 这一层。
 */
class CategoryManagerViewModel(
    app: TreasureApp,
    private val repo: CategoryRepository,
) : AndroidViewModel(app) {

    val all: StateFlow<List<CategoryInfo>> = repo.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun setHidden(id: String, hidden: Boolean) = viewModelScope.launch {
        repo.setHidden(id, hidden)
    }

    fun saveCustom(id: String, nameZh: String, nameEn: String, heroVector: HeroVector) =
        viewModelScope.launch {
            repo.updateCustom(id, nameZh, nameEn, heroVector)
        }

    fun saveHeroVectorOnly(id: String, heroVector: HeroVector) = viewModelScope.launch {
        repo.setHeroVector(id, heroVector)
    }

    fun deleteCustom(id: String) = viewModelScope.launch {
        repo.deleteCustom(id)
    }

    fun addCustom(
        nameZh: String,
        nameEn: String,
        heroVector: HeroVector,
        onCreated: (String) -> Unit = {},
    ) = viewModelScope.launch {
        val id = repo.addCustom(nameZh, nameEn, heroVector)
        onCreated(id)
    }

    /**
     * Cycle 0028：拖动结束一次性提交。先 reorder（按新顺序写 sort_order），
     * 再把每行的 hidden 同步成 [hiddenIds] 里的状态。
     */
    fun applyReorder(orderedIds: List<String>, hiddenIds: Set<String>) =
        viewModelScope.launch {
            repo.reorder(orderedIds)
            // 只对发生变化的行调用 setHidden，少写几条 SQL
            val current = repo.loadAll().associate { it.id to it.hidden }
            orderedIds.forEach { id ->
                val want = id in hiddenIds
                if (current[id] != want) repo.setHidden(id, want)
            }
        }

    /**
     * Cycle 0030：从相册挑了一张图，先复制到 `filesDir/category-photos/<id>/`
     * 再把绝对路径写到 hero_photo_path。
     *
     * 新建分类还没分配 id 时（[targetId] = null）— 把 photo 暂存进
     * [pendingPhotoForNew] 让 editor 回显，真正写入要等用户点 [新建]，那时
     * `addCustomWithPhoto` 一起做 add + setHeroPhotoPath。
     */
    private val app = app

    var pendingPhotoForNew: String? = null
        private set

    fun pickHeroPhoto(targetId: String?, uri: Uri, onSaved: (String) -> Unit = {}) =
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                copyPickedPhotoToCategoryDir(uri, targetId ?: "tmp")
            } ?: return@launch
            if (targetId != null) {
                repo.setHeroPhotoPath(targetId, path)
            } else {
                pendingPhotoForNew = path
            }
            onSaved(path)
        }

    fun clearHeroPhoto(targetId: String?) = viewModelScope.launch {
        if (targetId != null) {
            // 删本地图 + 清 DB 字段
            val old = repo.loadAll().firstOrNull { it.id == targetId }?.heroPhotoPath
            if (!old.isNullOrBlank()) withContext(Dispatchers.IO) {
                runCatching { File(old).delete() }
            }
            repo.setHeroPhotoPath(targetId, null)
        } else {
            pendingPhotoForNew?.let { p ->
                withContext(Dispatchers.IO) { runCatching { File(p).delete() } }
            }
            pendingPhotoForNew = null
        }
    }

    /** 新建模式：用户已经挑了 photo（落在 pendingPhotoForNew），点 [新建] 时
     *  一次性创建 row + 把 photo 移到正式目录 + 写 DB。 */
    fun addCustomWithPhoto(
        nameZh: String,
        nameEn: String,
        photoPath: String,
        onCreated: (String) -> Unit = {},
    ) = viewModelScope.launch {
        val id = repo.addCustom(nameZh, nameEn, HeroVector.GENERIC)
        // 把 tmp 目录的图移到 id 目录下
        val moved = withContext(Dispatchers.IO) {
            val src = File(photoPath)
            if (!src.exists()) return@withContext null
            val destDir = File(app.filesDir, "category-photos/$id").apply { mkdirs() }
            val dest = File(destDir, "${UUID.randomUUID()}.jpg")
            runCatching {
                src.inputStream().use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                }
                src.delete()
            }
            if (dest.exists() && dest.length() > 0) dest.absolutePath else null
        }
        if (moved != null) {
            repo.setHeroPhotoPath(id, moved)
        }
        pendingPhotoForNew = null
        onCreated(id)
    }

    private fun copyPickedPhotoToCategoryDir(uri: Uri, dirKey: String): String? {
        val dir = File(app.filesDir, "category-photos/$dirKey").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        return runCatching {
            app.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { out -> input.copyTo(out) }
            }
            if (dest.exists() && dest.length() > 0) dest.absolutePath else null
        }.getOrNull()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                CategoryManagerViewModel(app, app.categoryRepository)
            }
        }
    }
}
