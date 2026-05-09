package com.treasure.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.ai.AnthropicClient
import com.treasure.core.ai.OpenAiClient
import com.treasure.core.ai.Provider
import com.treasure.data.AiProviderPreset
import com.treasure.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.launch

/** Persisted, on-screen state. The [draft] mirrors persisted fields while
 *  the editor drawer is open, so cancelling discards changes. */
data class SettingsUiState(
    val saved: SavedConfig = SavedConfig(),
    val draft: DraftConfig = DraftConfig(),
    val editorOpen: Boolean = false,
    val testStatus: TestStatus = TestStatus.Idle,
)

data class SavedConfig(
    val preset: AiProviderPreset = AiProviderPreset.Anthropic,
    val provider: Provider = Provider.Anthropic,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val keyConfigured: Boolean = false,
    val temperature: Double? = null,
    val thinkingEnabled: Boolean = false,
    /** 上次 [testConnection] 是否成功；用来在摘要卡上画绿灯。改配置后 save() 会重置回 false → 黄灯。 */
    val lastTestPassed: Boolean = false,
)

data class DraftConfig(
    val preset: AiProviderPreset = AiProviderPreset.Anthropic,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    /** 文本框直接编辑；空串 = 走默认 */
    val temperatureText: String = "",
    val thinkingEnabled: Boolean = false,
)

sealed interface TestStatus {
    data object Idle : TestStatus
    data object Running : TestStatus
    data object Ok : TestStatus
    /**
     * 测试失败 — `kind` 是简短的错误类别（HTTP 400 / 网络 / 配置 / 未知），
     * `detail` 是带细节的人类可读 message。UI 渲染成 "× kind · detail"。
     */
    data class Failed(val kind: String, val detail: String) : TestStatus
}

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun loadState(): SettingsUiState {
        val provider = store.provider
        val baseUrl = store.baseUrl.orEmpty()
        val preset = AiProviderPreset.fromId(store.presetId)
            ?: AiProviderPreset.forLegacy(provider, store.baseUrl)
        val saved = SavedConfig(
            preset = preset,
            provider = provider,
            baseUrl = baseUrl.ifBlank { preset.baseUrl.orEmpty() },
            model = store.model,
            apiKey = store.apiKey.orEmpty(),
            keyConfigured = store.hasKey(),
            temperature = store.temperature,
            thinkingEnabled = store.thinkingEnabled,
            lastTestPassed = store.lastTestPassed,
        )
        return SettingsUiState(
            saved = saved,
            draft = DraftConfig(
                preset = saved.preset,
                baseUrl = saved.baseUrl,
                model = saved.model,
                apiKey = saved.apiKey,
                temperatureText = saved.temperature?.let { "%.2f".format(it) }.orEmpty(),
                thinkingEnabled = saved.thinkingEnabled,
            ),
        )
    }

    /** Open the editor drawer, seeded with the currently saved values. */
    fun openEditor() = _state.update {
        it.copy(
            editorOpen = true,
            draft = DraftConfig(
                preset = it.saved.preset,
                baseUrl = it.saved.baseUrl,
                model = it.saved.model,
                apiKey = it.saved.apiKey,
                temperatureText = it.saved.temperature?.let { v -> "%.2f".format(v) }.orEmpty(),
                thinkingEnabled = it.saved.thinkingEnabled,
            ),
            testStatus = TestStatus.Idle,
        )
    }

    fun closeEditor() = _state.update { it.copy(editorOpen = false) }

    /**
     * Cycle 0019：用户在抽屉里改任何配置都意味着 "上次测试" 不再算数 →
     * 立刻把状态灯回到黄。save() 不再 reset；它信任此刻的 lastTestPassed。
     */
    private fun invalidateTest() {
        store.lastTestPassed = false
        _state.update {
            it.copy(
                saved = it.saved.copy(lastTestPassed = false),
                testStatus = TestStatus.Idle,
            )
        }
    }

    fun setPreset(p: AiProviderPreset) {
        invalidateTest()
        _state.update {
            it.copy(
                draft = it.draft.copy(
                    preset = p,
                    baseUrl = p.baseUrl.orEmpty(),
                    model = p.defaultModel,
                ),
            )
        }
    }
    fun setBaseUrl(s: String) {
        invalidateTest()
        _state.update { it.copy(draft = it.draft.copy(baseUrl = s)) }
    }
    fun setModel(s: String) {
        invalidateTest()
        _state.update { it.copy(draft = it.draft.copy(model = s)) }
    }
    fun setApiKey(s: String) {
        invalidateTest()
        _state.update { it.copy(draft = it.draft.copy(apiKey = s)) }
    }
    fun setTemperatureText(s: String) {
        invalidateTest()
        _state.update { it.copy(draft = it.draft.copy(temperatureText = s)) }
    }
    fun setThinkingEnabled(b: Boolean) {
        invalidateTest()
        _state.update { it.copy(draft = it.draft.copy(thinkingEnabled = b)) }
    }

    fun save() {
        val draft = _state.value.draft
        store.presetId = draft.preset.id
        store.provider = draft.preset.provider
        store.apiKey = draft.apiKey
        store.model = draft.model.ifBlank { draft.preset.defaultModel }
        store.baseUrl = draft.baseUrl.takeIf { it.isNotBlank() }
        val parsedTemp = draft.temperatureText.trim().toDoubleOrNull()?.coerceIn(0.0, 2.0)
        store.temperature = parsedTemp
        store.thinkingEnabled = draft.thinkingEnabled
        // Cycle 0019：save() 信任此刻的 lastTestPassed — 任何改动都已经在
        // setter 里把它打回 false 了。这里没改 → 仍然是上次测试的状态。
        val keepGreen = store.lastTestPassed
        _state.update {
            val saved = SavedConfig(
                preset = draft.preset,
                provider = draft.preset.provider,
                baseUrl = draft.baseUrl,
                model = draft.model.ifBlank { draft.preset.defaultModel },
                apiKey = draft.apiKey,
                keyConfigured = store.hasKey(),
                temperature = parsedTemp,
                thinkingEnabled = draft.thinkingEnabled,
                lastTestPassed = keepGreen,
            )
            it.copy(saved = saved, editorOpen = false)
        }
    }

    fun clear() {
        store.clear()
        _state.update { loadState() }
    }

    fun testConnection() {
        val draft = _state.value.draft
        if (draft.apiKey.isBlank()) {
            _state.update { it.copy(testStatus = TestStatus.Failed("配置", "先填 API key")) }
            return
        }
        if (draft.preset.baseUrlMandatory && draft.baseUrl.isBlank()) {
            _state.update { it.copy(testStatus = TestStatus.Failed("配置", "此 provider 必须填 Base URL")) }
            return
        }
        _state.update { it.copy(testStatus = TestStatus.Running) }
        viewModelScope.launch {
            val effectiveBase = draft.baseUrl.ifBlank { draft.preset.baseUrl.orEmpty() }
            val effectiveModel = draft.model.ifBlank { draft.preset.defaultModel }
            val effectiveTemperature = draft.temperatureText.trim()
                .toDoubleOrNull()?.coerceIn(0.0, 2.0)
            val client = when (draft.preset.provider) {
                Provider.Anthropic -> AnthropicClient(
                    apiKey = draft.apiKey,
                    model = effectiveModel,
                    baseUrl = effectiveBase.ifBlank { "https://api.anthropic.com" },
                    temperature = effectiveTemperature,
                    thinkingEnabled = draft.thinkingEnabled,
                )
                Provider.OpenAi -> OpenAiClient(
                    apiKey = draft.apiKey,
                    model = effectiveModel,
                    baseUrl = effectiveBase.ifBlank { "https://api.openai.com" },
                    temperature = effectiveTemperature,
                    thinkingEnabled = draft.thinkingEnabled,
                )
                Provider.OpenAiCompatible -> OpenAiClient(
                    apiKey = draft.apiKey,
                    model = effectiveModel,
                    baseUrl = effectiveBase,
                    temperature = effectiveTemperature,
                    thinkingEnabled = draft.thinkingEnabled,
                )
            }
            val result = client.extractItemDraft(text = "测试连接：随便编一个 AirPods Pro 2")
            // 持久化：成功 → 绿灯；失败 → 不动（多半已经从 false 起步，
            // 但若之前是 true 这次失败，也降回 false）
            store.lastTestPassed = result.isSuccess
            _state.update {
                it.copy(
                    saved = it.saved.copy(lastTestPassed = result.isSuccess),
                    testStatus = if (result.isSuccess) TestStatus.Ok
                    else result.exceptionOrNull().toTestFailed(),
                )
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TreasureApp
                SettingsViewModel(app.settingsStore)
            }
        }
    }
}

/**
 * 把 AiClient 抛出的各种异常归到一个简短的类别 + 一行人话。
 *
 * - `HTTP 400: { ... json ... }` → 解出 provider 报的 `error.message`，
 *   类别用 "HTTP 400"
 * - 网络异常 (UnknownHost / Connect / SocketTimeout) → 类别 "网络"
 * - 其他 → 类别 "错误"，全文当 detail
 */
private fun Throwable?.toTestFailed(): TestStatus.Failed {
    val raw = this?.message?.trim().orEmpty()
    if (raw.isEmpty()) return TestStatus.Failed("错误", "未知异常 (${this?.javaClass?.simpleName ?: "?"})")

    val httpMatch = Regex("^HTTP (\\d+):\\s*(.*)", RegexOption.DOT_MATCHES_ALL).find(raw)
    if (httpMatch != null) {
        val code = httpMatch.groupValues[1]
        val rest = httpMatch.groupValues[2].trim()
        val pretty = extractProviderErrorMessage(rest) ?: rest.take(220)
        return TestStatus.Failed("HTTP $code", pretty)
    }

    val cls = this?.javaClass?.simpleName.orEmpty()
    val networkKind = when {
        cls == "UnknownHostException" || raw.contains("UnknownHostException") -> "网络"
        cls == "ConnectException" || raw.contains("ConnectException") -> "网络"
        cls == "SocketTimeoutException" || raw.contains("SocketTimeoutException") -> "网络"
        cls == "SSLHandshakeException" || raw.contains("SSLHandshakeException") -> "TLS"
        else -> null
    }
    if (networkKind != null) return TestStatus.Failed(networkKind, raw)

    return TestStatus.Failed("错误", raw.take(280))
}

/** 从 provider 的 JSON error 体里抓 `error.message` / `error.msg`。 */
private fun extractProviderErrorMessage(body: String): String? {
    if (!body.startsWith("{")) return null
    return runCatching {
        val obj = kotlinx.serialization.json.Json.parseToJsonElement(body)
            .jsonObject
        val err = obj["error"] ?: obj["err"] ?: return@runCatching null
        when (err) {
            is kotlinx.serialization.json.JsonObject ->
                (err["message"] ?: err["msg"])
                    ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
            is kotlinx.serialization.json.JsonPrimitive ->
                err.contentOrNull
            else -> null
        }
    }.getOrNull()
}
