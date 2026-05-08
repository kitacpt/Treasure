package com.treasure.ui.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.ai.ItemDraft
import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import com.treasure.core.repo.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.LocalDate

class AddViewModel(
    application: Application,
    private val repo: ItemRepository,
) : AndroidViewModel(application) {

    /**
     * Build an Item from form values + the selected category template,
     * write it to Room, and return the new id via [onSaved].
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
            val specs = template.heroSpecLabels.zip(heroSpecValues) { l, v ->
                HeroSpec(l, v.trim())
            }
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
                specs = specs,
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

    /**
     * Ask the configured AI service to fill out an [ItemDraft] from the
     * user's text + optional photo.
     */
    fun extractDraft(
        text: String,
        imageUri: Uri?,
        onDraft: (ItemDraft) -> Unit,
        onError: (String) -> Unit,
    ) {
        val app = getApplication<TreasureApp>()
        val client = app.aiClient()
        if (client == null) {
            onError("尚未配置 API key · 去设置")
            return
        }
        viewModelScope.launch {
            val bytes = imageUri?.let { uri ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        app.contentResolver.openInputStream(uri)?.use { input ->
                            ByteArrayOutputStream().also { out -> input.copyTo(out) }.toByteArray()
                        }
                    }
                }.getOrNull()
            }
            client.extractItemDraft(text = text, imageJpegBytes = bytes)
                .onSuccess(onDraft)
                .onFailure { onError(it.message ?: "未知错误") }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                AddViewModel(app, app.repository)
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
