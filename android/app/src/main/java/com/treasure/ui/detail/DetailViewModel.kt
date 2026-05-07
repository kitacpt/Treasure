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

    /** Save inline edits from the drawer's settings tab. */
    fun saveEdits(nickname: String, oneLiner: String, status: ItemStatus) {
        viewModelScope.launch {
            val item = state.value.item ?: return@launch
            repo.upsert(item.copy(
                nickname = nickname.trim(),
                oneLiner = oneLiner.trim(),
                status = status,
                updatedAt = System.currentTimeMillis(),
            ))
        }
    }

    /**
     * Copy the picked photo from a content:// URI into the app's private
     * storage and append its absolute path to the item.
     */
    fun addPhoto(uri: Uri) {
        viewModelScope.launch {
            val item = state.value.item ?: return@launch
            val app = getApplication<Application>()
            val savedPath = withContext(Dispatchers.IO) {
                val dir = File(app.filesDir, "photos/${item.id}").apply { mkdirs() }
                val dest = File(dir, "${UUID.randomUUID()}.jpg")
                app.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                }
                if (dest.exists() && dest.length() > 0) dest.absolutePath else null
            } ?: return@launch
            repo.upsert(item.copy(
                photos = item.photos + savedPath,
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
