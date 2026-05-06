package com.treasure.ui.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.Category
import com.treasure.core.domain.Item
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GridUiState(
    val currentCategory: Category = Category.PHOTO,
    val itemsInCategory: List<Item> = emptyList(),
    val countByCategory: Map<Category, Int> = emptyMap(),
    val totalCount: Int = 0,
)

class GridViewModel(
    private val repo: ItemRepository,
    initialCategory: Category,
) : ViewModel() {

    private val selected = MutableStateFlow(initialCategory)

    fun selectCategory(category: Category) {
        selected.value = category
    }

    val state: StateFlow<GridUiState> = combine(repo.items, selected) { items, cat ->
        GridUiState(
            currentCategory = cat,
            itemsInCategory = items.filter { it.category == cat },
            countByCategory = items.groupBy { it.category }.mapValues { it.value.size },
            totalCount = items.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GridUiState(currentCategory = initialCategory),
    )

    companion object {
        const val ARG_CATEGORY_ID = "categoryId"

        fun factory(initialCategoryId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                    GridViewModel(app.repository, Category.fromId(initialCategoryId))
                }
            }
    }
}
