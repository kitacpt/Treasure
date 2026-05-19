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

    /**
     * Cycle 0033：图鉴页编辑态点 [编辑] → 把选中的 itemIds 投递到这里，并切
     * 到 PAGE_ADD。AddRoute 监听 → 调 VM 建新会话 + 把这些物品作为 SAVED 行
     * 加进工作集 + 切到那段会话。消费后清空。
     */
    val gridIntake: kotlinx.coroutines.flow.MutableStateFlow<List<String>?> =
        kotlinx.coroutines.flow.MutableStateFlow(null)

    /** Cycle 0031：用户手动选的主题 — null 跟随系统，true / false 强制覆盖。
     *  MainActivity 读它 + isSystemInDarkTheme 推断实际 darkTheme。 */
    val darkModeOverride: kotlinx.coroutines.flow.MutableStateFlow<Boolean?> =
        kotlinx.coroutines.flow.MutableStateFlow(null)

    fun setDarkModeOverride(value: Boolean?) {
        settingsStore.darkMode = value
        darkModeOverride.value = value
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = RoomItemRepository.create(this)
        conversationRepository = RoomAddConversationRepository.create(this)
        categoryRepository = RoomCategoryRepository.create(this)
        settingsStore = SettingsStore(this)
        darkModeOverride.value = settingsStore.darkMode
        // Cycle 0036 v2：PdfBox-Android 用 asset 里的 cmaps 字体表做文字提取，
        // 在用前必须 init 一下。放 IO 线程做 — 它要解几个内置资源 jar。
        appScope.launch {
            runCatching {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(applicationContext)
            }
        }
        appScope.launch { repository.ensureSeeded() }
    }

    /**
     * Build a fresh [AiClient] from the current [SettingsStore] state.
     * Returns null when the user hasn't supplied a key yet — UI uses this
     * as the gate for AI features. Provider switch happens here.
     */
    fun aiClient(): AiClient? {
        // Cycle 0035：从 effective profile（per-conversation override 优先，
        // 否则默认）构造 client。空 key / 配置缺失 → null，沿用旧的"未配置"
        // gate 语义。
        val profile = settingsStore.effectiveProfile() ?: return null
        if (!profile.hasKey) return null
        val key = profile.apiKey
        val temperature = profile.temperature
        val thinking = profile.thinkingEnabled
        return when (profile.provider) {
            Provider.Anthropic -> AnthropicClient(
                apiKey = key,
                model = profile.effectiveModel,
                baseUrl = profile.effectiveBaseUrl.ifBlank { "https://api.anthropic.com" },
                temperature = temperature,
                thinkingEnabled = thinking,
            )
            Provider.OpenAi -> OpenAiClient(
                apiKey = key,
                model = profile.effectiveModel,
                baseUrl = profile.effectiveBaseUrl.ifBlank { "https://api.openai.com" },
                temperature = temperature,
                thinkingEnabled = thinking,
            )
            Provider.OpenAiCompatible -> {
                val url = profile.effectiveBaseUrl.takeIf { it.isNotBlank() } ?: return null
                OpenAiClient(
                    apiKey = key,
                    model = profile.effectiveModel,
                    baseUrl = url,
                    temperature = temperature,
                    thinkingEnabled = thinking,
                )
            }
        }
    }
}
