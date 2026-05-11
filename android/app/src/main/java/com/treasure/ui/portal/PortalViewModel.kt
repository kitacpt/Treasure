package com.treasure.ui.portal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.repo.CategoryRepository
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PortalUiState(
    val items: List<Item> = emptyList(),
    val totalItems: Int = 0,
    val ownedCount: Int = 0,
    val visibleCategories: List<CategoryInfo> = emptyList(),
    /** 按 categoryId 聚合 — 同时支持内建和自定义。 */
    val countByCategoryId: Map<String, Int> = emptyMap(),
    val latestByCategoryId: Map<String, Item?> = emptyMap(),
    val latestOverall: Item? = null,
)

class PortalViewModel(
    repo: ItemRepository,
    categories: CategoryRepository,
) : ViewModel() {

    val state: StateFlow<PortalUiState> = combine(
        repo.items,
        categories.observeAll(),
    ) { items, allCats ->
        val visible = allCats.filter { !it.hidden }
        PortalUiState(
            items = items,
            totalItems = items.size,
            ownedCount = items.count { it.status == ItemStatus.OWNED },
            visibleCategories = visible,
            countByCategoryId = items.groupBy { it.category }
                .mapValues { it.value.size },
            latestByCategoryId = visible.associate { c ->
                c.id to items.filter { it.category == c.id }.maxByOrNull { it.acquired }
            },
            latestOverall = items.maxByOrNull { it.acquired },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PortalUiState(),
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                PortalViewModel(app.repository, app.categoryRepository)
            }
        }
    }
}
