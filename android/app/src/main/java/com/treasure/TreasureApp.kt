package com.treasure

import android.app.Application
import com.treasure.core.ai.AiClient
import com.treasure.core.ai.AnthropicClient
import com.treasure.core.ai.OpenAiClient
import com.treasure.core.ai.Provider
import com.treasure.core.repo.AddConversationRepository
import com.treasure.core.repo.CategoryRepository
import com.treasure.core.repo.ItemRepository
import com.treasure.core.repo.RoomAddConversationRepository
import com.treasure.core.repo.RoomCategoryRepository
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

    lateinit var conversationRepository: AddConversationRepository
        private set

    lateinit var categoryRepository: CategoryRepository
        private set

    lateinit var settingsStore: SettingsStore
        private set

    /** Cycle 0020：把分享进来的链接拉一下页面给 AI 当 context。 */
    val pageFetcher: com.treasure.core.web.PageFetcher = com.treasure.core.web.PageFetcher()

    /**
     * Cycle 0019：从外部 app（京东 / 淘宝 / 浏览器）通过 ACTION_SEND /
     * ACTION_VIEW 分享过来的文字（多半含商品链接）。MainActivity 写入；
     * MainScreen 监听后切到录入 tab + 把文字喂给 AI。消费后清掉。
     */
    val shareIntake: kotlinx.coroutines.flow.MutableStateFlow<String?> =
        kotlinx.coroutines.flow.MutableStateFlow(null)

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = RoomItemRepository.create(this)
        conversationRepository = RoomAddConversationRepository.create(this)
        categoryRepository = RoomCategoryRepository.create(this)
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
        val temperature = settingsStore.temperature
        val thinking = settingsStore.thinkingEnabled
        return when (provider) {
            Provider.Anthropic -> AnthropicClient(
                apiKey = key,
                model = model.ifBlank { AnthropicClient.DEFAULT_MODEL },
                baseUrl = settingsStore.baseUrl ?: "https://api.anthropic.com",
                temperature = temperature,
                thinkingEnabled = thinking,
            )
            Provider.OpenAi -> OpenAiClient(
                apiKey = key,
                model = model.ifBlank { OpenAiClient.DEFAULT_MODEL },
                baseUrl = settingsStore.baseUrl ?: "https://api.openai.com",
                temperature = temperature,
                thinkingEnabled = thinking,
            )
            Provider.OpenAiCompatible -> {
                val url = settingsStore.baseUrl ?: return null
                OpenAiClient(
                    apiKey = key,
                    model = model.ifBlank { OpenAiClient.DEFAULT_MODEL },
                    baseUrl = url,
                    temperature = temperature,
                    thinkingEnabled = thinking,
                )
            }
        }
    }
}
