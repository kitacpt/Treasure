package com.treasure.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.Item
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val item: Item? = null,
    val loaded: Boolean = false,
)

class DetailViewModel(
    private val repo: ItemRepository,
    itemId: String,
) : ViewModel() {

    val state: StateFlow<DetailUiState> = repo.observeById(itemId)
        .let { flow ->
            kotlinx.coroutines.flow.flow {
                flow.collect { emit(DetailUiState(item = it, loaded = true)) }
            }
        }
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

    companion object {
        fun factory(itemId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                    DetailViewModel(app.repository, itemId)
                }
            }
    }
}
