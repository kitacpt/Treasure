package com.treasure.ui.portal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.Category
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PortalUiState(
    val items: List<Item> = emptyList(),
    val totalItems: Int = 0,
    val ownedCount: Int = 0,
    val roomsCount: Int = Category.entries.size,
    val countByCategory: Map<Category, Int> = emptyMap(),
    val latestByCategory: Map<Category, Item?> = emptyMap(),
    val latestOverall: Item? = null,
)

class PortalViewModel(repo: ItemRepository) : ViewModel() {

    val state: StateFlow<PortalUiState> = repo.items
        .map { items ->
            PortalUiState(
                items = items,
                totalItems = items.size,
                ownedCount = items.count { it.status == ItemStatus.OWNED },
                roomsCount = Category.entries.size,
                countByCategory = items.groupBy { it.category }.mapValues { it.value.size },
                latestByCategory = Category.entries.associateWith { c ->
                    items.filter { it.category == c }.maxByOrNull { it.acquired }
                },
                latestOverall = items.maxByOrNull { it.acquired },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PortalUiState(),
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                PortalViewModel(app.repository)
            }
        }
    }
}
