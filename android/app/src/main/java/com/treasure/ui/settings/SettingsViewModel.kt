package com.treasure.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.treasure.TreasureApp
import com.treasure.core.ai.AnthropicClient
import com.treasure.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val model: String = AnthropicClient.DEFAULT_MODEL,
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

    private val _state = MutableStateFlow(
        SettingsUiState(
            model = store.model,
            apiKey = store.apiKey.orEmpty(),
            keyConfigured = store.hasKey(),
        ),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setModel(s: String) = _state.update { it.copy(model = s) }
    fun setApiKey(s: String) = _state.update { it.copy(apiKey = s, testStatus = TestStatus.Idle) }

    fun save() {
        val st = _state.value
        store.apiKey = st.apiKey
        store.model = st.model.ifBlank { AnthropicClient.DEFAULT_MODEL }
        _state.update { it.copy(keyConfigured = store.hasKey()) }
    }

    fun clear() {
        store.clear()
        _state.update {
            SettingsUiState(model = AnthropicClient.DEFAULT_MODEL, apiKey = "", keyConfigured = false)
        }
    }

    fun testConnection() {
        val st = _state.value
        if (st.apiKey.isBlank()) {
            _state.update { it.copy(testStatus = TestStatus.Failed("先填 API key")) }
            return
        }
        _state.update { it.copy(testStatus = TestStatus.Running) }
        viewModelScope.launch {
            val client = AnthropicClient(apiKey = st.apiKey, model = st.model)
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
