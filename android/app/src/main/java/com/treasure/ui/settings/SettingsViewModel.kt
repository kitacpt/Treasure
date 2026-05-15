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
import com.treasure.data.AiProfile
import com.treasure.data.AiProviderPreset
import com.treasure.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * Cycle 0035：从单份配置改为多份 + 默认。屏幕状态：
 *  - [profiles]：当前所有 profiles
 *  - [defaultProfileId]：默认 profile（aiClient 走它）
 *  - [activeIndex]：pager 当前停在第几张（含末尾的"添加"卡）
 *  - [editorTarget]：当前在编辑哪一份（null = 抽屉关）
 *  - [draft]：编辑抽屉里的草稿
 */
data class SettingsUiState(
    val profiles: List<AiProfile> = emptyList(),
    val defaultProfileId: String? = null,
    val activeIndex: Int = 0,
    val editorTarget: String? = null,  // profile id; null = closed
    val draft: DraftConfig = DraftConfig(),
    val testStatus: TestStatus = TestStatus.Idle,
    val addSheetOpen: Boolean = false,
)

data class DraftConfig(
    val preset: AiProviderPreset = AiProviderPreset.Anthropic,
    val displayName: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val temperatureText: String = "",
    val thinkingEnabled: Boolean = false,
)

sealed interface TestStatus {
    data object Idle : TestStatus
    data object Running : TestStatus
    data object Ok : TestStatus
    data class Failed(val kind: String, val detail: String) : TestStatus
}

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun loadState(): SettingsUiState {
        val list = store.profiles
        return SettingsUiState(
            profiles = list,
            defaultProfileId = store.defaultProfileId,
        )
    }

    fun setActiveIndex(i: Int) {
        _state.update { it.copy(activeIndex = i) }
    }

    /** Pager 滑到某张 → activeIndex 更新。 */
    fun openAddSheet() = _state.update { it.copy(addSheetOpen = true) }
    fun closeAddSheet() = _state.update { it.copy(addSheetOpen = false) }

    fun addProfile(preset: AiProviderPreset) {
        val newProfile = AiProfile.fromPreset(preset)
        val next = store.addProfile(newProfile)
        _state.update {
            it.copy(
                profiles = next,
                defaultProfileId = store.defaultProfileId,
                addSheetOpen = false,
                activeIndex = next.size - 1,  // jump to new card
                editorTarget = newProfile.id,  // open editor for the new profile
                draft = DraftConfig(
                    preset = preset,
                    displayName = "",
                    baseUrl = newProfile.baseUrl,
                    model = newProfile.model,
                    apiKey = "",
                    temperatureText = "",
                    thinkingEnabled = false,
                ),
                testStatus = TestStatus.Idle,
            )
        }
    }

    fun removeProfile(id: String) {
        val next = store.removeProfile(id)
        _state.update {
            it.copy(
                profiles = next,
                defaultProfileId = store.defaultProfileId,
                editorTarget = if (it.editorTarget == id) null else it.editorTarget,
                activeIndex = it.activeIndex.coerceAtMost(maxOf(0, next.size)),
            )
        }
    }

    fun setAsDefault(id: String) {
        store.defaultProfileId = id
        _state.update { it.copy(defaultProfileId = id) }
    }

    fun openEditor(id: String) {
        val p = store.profileById(id) ?: return
        _state.update {
            it.copy(
                editorTarget = id,
                draft = DraftConfig(
                    preset = p.preset,
                    displayName = p.displayName,
                    baseUrl = p.baseUrl,
                    model = p.model,
                    apiKey = p.apiKey,
                    temperatureText = p.temperature?.let { v -> "%.2f".format(v) }.orEmpty(),
                    thinkingEnabled = p.thinkingEnabled,
                ),
                testStatus = TestStatus.Idle,
            )
        }
    }

    fun setDisplayName(s: String) {
        _state.update { it.copy(draft = it.draft.copy(displayName = s)) }
    }

    /** 在编辑抽屉里点"移除"时调用 — 关抽屉、删 profile。 */
    fun removeCurrentEditingProfile() {
        val target = _state.value.editorTarget ?: return
        val next = store.removeProfile(target)
        _state.update {
            it.copy(
                profiles = next,
                defaultProfileId = store.defaultProfileId,
                editorTarget = null,
                activeIndex = it.activeIndex.coerceAtMost(maxOf(0, next.size)),
            )
        }
    }

    fun closeEditor() = _state.update { it.copy(editorTarget = null) }

    private fun invalidateTest() {
        _state.update {
            // 编辑抽屉里的改动还没存盘 → 不去碰持久态；只把灯先回黄。
            it.copy(testStatus = TestStatus.Idle)
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
        val s = _state.value
        val targetId = s.editorTarget ?: return
        val existing = store.profileById(targetId) ?: return
        val draft = s.draft
        val parsedTemp = draft.temperatureText.trim().toDoubleOrNull()?.coerceIn(0.0, 2.0)
        val updated = existing.copy(
            presetId = draft.preset.id,
            displayName = draft.displayName.trim(),
            baseUrl = draft.baseUrl,
            model = draft.model.ifBlank { draft.preset.defaultModel },
            apiKey = draft.apiKey,
            temperature = parsedTemp,
            thinkingEnabled = draft.thinkingEnabled,
        )
        store.updateProfile(updated)
        _state.update {
            it.copy(
                profiles = store.profiles,
                editorTarget = null,
            )
        }
    }

    fun resetAll() {
        store.clear()
        _state.update { loadState() }
    }

    fun testConnection() {
        val draft = _state.value.draft
        val targetId = _state.value.editorTarget
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
            val result = client.extractItemDrafts(text = "测试连接：随便编一个 AirPods Pro 2")
            // 把 lastTestPassed 同步到这份 profile（如果在编辑中且还没 save，
            // 这次结果直接 patch 到磁盘上的 profile 上；下次 save 不会覆盖它）。
            if (targetId != null) {
                store.profileById(targetId)?.let { p ->
                    store.updateProfile(p.copy(lastTestPassed = result.isSuccess))
                }
            }
            _state.update {
                it.copy(
                    profiles = store.profiles,
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

private fun extractProviderErrorMessage(body: String): String? {
    if (!body.startsWith("{")) return null
    return runCatching {
        val obj = kotlinx.serialization.json.Json.parseToJsonElement(body)
            .jsonObject
        val err = obj["error"] ?: obj["err"] ?: return@runCatching null
        when (err) {
            is JsonObject ->
                (err["message"] ?: err["msg"])
                    ?.let { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive ->
                err.contentOrNull
            else -> null
        }
    }.getOrNull()
}
