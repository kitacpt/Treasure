package com.treasure.ui.category

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.HeroVector
import com.treasure.core.repo.CategoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                CategoryManagerViewModel(app, app.categoryRepository)
            }
        }
    }
}
