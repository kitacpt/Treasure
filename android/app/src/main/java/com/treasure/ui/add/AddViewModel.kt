package com.treasure.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddViewModel(private val repo: ItemRepository) : ViewModel() {

    /**
     * Build an Item from form values + the selected category template,
     * write it to Room, and return the new id via [onSaved] so the caller
     * can navigate straight to its Detail screen.
     */
    fun save(
        template: CategoryTemplate,
        brand: String,
        model: String,
        nickname: String,
        acquired: String,
        oneLiner: String,
        status: ItemStatus,
        heroSpecValues: List<String>,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = makeId(template.category, brand, model, now)
            val acquiredOrToday = acquired.ifBlank { LocalDate.now().toString() }
            val item = Item(
                id = id,
                category = template.category,
                brand = brand.trim(),
                model = model.trim(),
                nickname = nickname.trim(),
                acquired = acquiredOrToday,
                parted = null,
                status = status,
                palette = template.palette,
                oneLiner = oneLiner.trim(),
                heroVector = template.heroVector,
                heroSpecs = template.heroSpecLabels.zip(heroSpecValues) { l, v ->
                    HeroSpec(l, v.trim())
                },
                specs = emptyMap(),
                history = listOf(
                    HistoryEvent(
                        date = acquiredOrToday,
                        kind = HistoryKind.ACQUIRED,
                        title = "购入",
                        note = "",
                    ),
                ),
                photos = emptyList(),
                createdAt = now,
                updatedAt = now,
            )
            repo.upsert(item)
            onSaved(id)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                AddViewModel(app.repository)
            }
        }
    }
}

private fun makeId(category: Category, brand: String, model: String, now: Long): String {
    val slug = "$brand-$model"
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "item" }
        .take(40)
    return "${category.id}-$slug-${(now / 1000) % 100000}"
}
