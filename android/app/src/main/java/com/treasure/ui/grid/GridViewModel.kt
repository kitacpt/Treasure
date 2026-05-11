package com.treasure.ui.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.Item
import com.treasure.core.repo.CategoryRepository
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Cycle 0026：[currentCategoryId] 改成 String? 通用 id（"badminton" / "tech" /
 * "custom-xxx"），不再绑死 [com.treasure.core.domain.Category] enum，从而能
 * 选中用户自建的分类。[visibleCategories] 是显示在 chip 行里的全部分类 —
 * 内建 + 自定义且未 hidden 的。"全部" 仍是 null。
 */
data class GridUiState(
    val currentCategoryId: String? = null,
    val itemsInCategory: List<Item> = emptyList(),
    /** 各分类（含自定义）的物品数量，给 chip 显示 N items。 */
    val countByCategoryId: Map<String, Int> = emptyMap(),
    val totalCount: Int = 0,
    /** 当前未隐藏的分类列表，按 sort_order 排好。空时回退到 emptyList。 */
    val visibleCategories: List<CategoryInfo> = emptyList(),
)

class GridViewModel(
    private val repo: ItemRepository,
    private val categories: CategoryRepository,
    initialCategoryId: String?,
) : ViewModel() {

    private val selected = MutableStateFlow<String?>(initialCategoryId)

    /** 传 null 切到 "全部" 聚合视图 */
    fun selectCategory(id: String?) {
        selected.value = id
    }

    val state: StateFlow<GridUiState> = combine(
        repo.items,
        selected,
        categories.observeAll(),
    ) { items, selectedId, allCats ->
        val visible = allCats.filter { !it.hidden }
        val filteredItems = if (selectedId == null) items
        else items.filter { it.category.id == selectedId }
        GridUiState(
            currentCategoryId = selectedId,
            itemsInCategory = filteredItems,
            countByCategoryId = items.groupBy { it.category.id }
                .mapValues { it.value.size },
            totalCount = items.size,
            visibleCategories = visible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GridUiState(currentCategoryId = initialCategoryId),
    )

    companion object {
        const val ARG_CATEGORY_ID = "categoryId"

        const val ALL_FILTER_ID = "all"

        fun factory(initialCategoryId: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                    val initial = if (initialCategoryId == ALL_FILTER_ID || initialCategoryId.isBlank()) null
                                  else initialCategoryId
                    GridViewModel(app.repository, app.categoryRepository, initial)
                }
            }
    }
}
