package com.treasure

import android.app.Application
import com.treasure.core.ai.AiClient
import com.treasure.core.ai.AnthropicClient
import com.treasure.core.ai.OpenAiClient
import com.treasure.core.ai.Provider
import com.treasure.core.repo.ItemRepository
import com.treasure.core.repo.RoomItemRepository
import com.treasure.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application that wires up the dependency graph and runs first-launch
 * seeding. Hand-rolled ServiceLocator — no Hilt/Koin until it earns its keep.
 */
class TreasureApp : Application() {
    lateinit var repository: ItemRepository
        private set

    lateinit var settingsStore: SettingsStore
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = RoomItemRepository.create(this)
        settingsStore = SettingsStore(this)
        appScope.launch { repository.ensureSeeded() }
    }

    /**
     * Build a fresh [AiClient] from the current [SettingsStore] state.
     * Returns null when the user hasn't supplied a key yet — UI uses this
     * as the gate for AI features. Provider switch happens here.
     */
    fun aiClient(): AiClient? {
        val key = settingsStore.apiKey ?: return null
        val model = settingsStore.model
        val provider = settingsStore.provider
        return when (provider) {
            Provider.Anthropic -> AnthropicClient(
                apiKey = key,
                model = model.ifBlank { AnthropicClient.DEFAULT_MODEL },
                baseUrl = settingsStore.baseUrl ?: "https://api.anthropic.com",
            )
            Provider.OpenAi -> OpenAiClient(
                apiKey = key,
                model = model.ifBlank { OpenAiClient.DEFAULT_MODEL },
                baseUrl = settingsStore.baseUrl ?: "https://api.openai.com",
            )
            Provider.OpenAiCompatible -> {
                val url = settingsStore.baseUrl ?: return null
                OpenAiClient(
                    apiKey = key,
                    model = model.ifBlank { OpenAiClient.DEFAULT_MODEL },
                    baseUrl = url,
                )
            }
        }
    }
}
