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
import com.treasure.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val provider: Provider = Provider.Anthropic,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val keyConfigured: Boolean = false,
    val testStatus: TestStatus = TestStatus.Idle,
)

sealed interface TestStatus {
    data object Idle : TestStatus
    data object Running : TestStatus
    data object Ok : TestStatus
    data class Failed(val message: String) : TestStatus
}

class SettingsViewModel(private val store: SettingsStore) : ViewModel() {

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private fun loadState(): SettingsUiState {
        val provider = store.provider
        return SettingsUiState(
            provider = provider,
            baseUrl = store.baseUrl ?: SettingsStore.defaultBaseUrlFor(provider).orEmpty(),
            model = store.model,
            apiKey = store.apiKey.orEmpty(),
            keyConfigured = store.hasKey(),
        )
    }

    fun setProvider(p: Provider) = _state.update {
        // Reset model + baseUrl to that provider's defaults when switching
        it.copy(
            provider = p,
            model = SettingsStore.defaultModelFor(p),
            baseUrl = SettingsStore.defaultBaseUrlFor(p).orEmpty(),
            testStatus = TestStatus.Idle,
        )
    }
    fun setModel(s: String) = _state.update { it.copy(model = s) }
    fun setBaseUrl(s: String) = _state.update { it.copy(baseUrl = s, testStatus = TestStatus.Idle) }
    fun setApiKey(s: String) = _state.update { it.copy(apiKey = s, testStatus = TestStatus.Idle) }

    fun save() {
        val st = _state.value
        store.provider = st.provider
        store.apiKey = st.apiKey
        store.model = st.model.ifBlank { SettingsStore.defaultModelFor(st.provider) }
        store.baseUrl = st.baseUrl.takeIf { it.isNotBlank() }
        _state.update { it.copy(keyConfigured = store.hasKey()) }
    }

    fun clear() {
        store.clear()
        _state.update { loadState() }
    }

    fun testConnection() {
        val st = _state.value
        if (st.apiKey.isBlank()) {
            _state.update { it.copy(testStatus = TestStatus.Failed("先填 API key")) }
            return
        }
        if (st.provider == Provider.OpenAiCompatible && st.baseUrl.isBlank()) {
            _state.update { it.copy(testStatus = TestStatus.Failed("自定义 provider 必须填 base URL")) }
            return
        }
        _state.update { it.copy(testStatus = TestStatus.Running) }
        viewModelScope.launch {
            val client = when (st.provider) {
                Provider.Anthropic -> AnthropicClient(
                    apiKey = st.apiKey,
                    model = st.model.ifBlank { AnthropicClient.DEFAULT_MODEL },
                    baseUrl = st.baseUrl.ifBlank { "https://api.anthropic.com" },
                )
                Provider.OpenAi -> OpenAiClient(
                    apiKey = st.apiKey,
                    model = st.model.ifBlank { OpenAiClient.DEFAULT_MODEL },
                    baseUrl = st.baseUrl.ifBlank { "https://api.openai.com" },
                )
                Provider.OpenAiCompatible -> OpenAiClient(
                    apiKey = st.apiKey,
                    model = st.model.ifBlank { OpenAiClient.DEFAULT_MODEL },
                    baseUrl = st.baseUrl,
                )
            }
            val result = client.extractItemDraft(text = "测试连接：随便编一个 AirPods Pro 2")
            _state.update {
                it.copy(
                    testStatus = if (result.isSuccess) TestStatus.Ok
                    else TestStatus.Failed(result.exceptionOrNull()?.message ?: "未知错误"),
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
