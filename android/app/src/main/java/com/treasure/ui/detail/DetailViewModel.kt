package com.treasure.ui.detail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class DetailUiState(
    val item: Item? = null,
    val loaded: Boolean = false,
)

class DetailViewModel(
    application: Application,
    private val repo: ItemRepository,
    itemId: String,
) : AndroidViewModel(application) {

    val state: StateFlow<DetailUiState> = repo.observeById(itemId)
        .map { DetailUiState(item = it, loaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailUiState(),
        )

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            state.value.item?.let { repo.delete(it.id) }
            onDone()
        }
    }

    /** Persist any user-edited copy of the current item back to Room. */
    fun update(updated: Item) {
        viewModelScope.launch {
            repo.upsert(updated.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /**
     * Copy the picked photo from a content:// URI into the app's private
     * storage and append its absolute path to the item.
     */
    fun addPhoto(uri: Uri) {
        addPhotos(listOf(uri))
    }

    /** Copy a batch of picked photos in one shot. */
    fun addPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val item = state.value.item ?: return@launch
            val app = getApplication<Application>()
            val savedPaths = withContext(Dispatchers.IO) {
                val dir = File(app.filesDir, "photos/${item.id}").apply { mkdirs() }
                uris.mapNotNull { uri ->
                    val dest = File(dir, "${UUID.randomUUID()}.jpg")
                    runCatching {
                        app.contentResolver.openInputStream(uri)?.use { input ->
                            dest.outputStream().use { out -> input.copyTo(out) }
                        }
                    }
                    if (dest.exists() && dest.length() > 0) dest.absolutePath else null
                }
            }
            if (savedPaths.isEmpty()) return@launch
            repo.upsert(item.copy(
                photos = item.photos + savedPaths,
                updatedAt = System.currentTimeMillis(),
            ))
        }
    }

    fun removePhoto(path: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { runCatching { File(path).delete() } }
            val item = state.value.item ?: return@launch
            repo.upsert(item.copy(
                photos = item.photos - path,
                callouts = item.callouts - path,
                // 删的若是当前头像照片，就把头像清空回到线描
                avatarPhotoPath = item.avatarPhotoPath?.takeIf { it != path },
                updatedAt = System.currentTimeMillis(),
            ))
        }
    }

    /**
     * Cycle 0012：用一份新的列表替换某张照片的全部 callout。空列表时把
     * 这条 path 整个从 map 里抹掉，免得 JSON 里堆空数组。
     */
    fun setCallouts(
        path: String,
        list: List<com.treasure.core.domain.PhotoCallout>,
    ) {
        viewModelScope.launch {
            val item = state.value.item ?: return@launch
            val newCallouts = if (list.isEmpty()) {
                item.callouts - path
            } else {
                item.callouts + (path to list)
            }
            repo.upsert(item.copy(
                callouts = newCallouts,
                updatedAt = System.currentTimeMillis(),
            ))
        }
    }

    companion object {
        fun factory(itemId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                    DetailViewModel(app, app.repository, itemId)
                }
            }
    }
}
