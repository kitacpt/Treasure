package com.treasure.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.treasure.core.ai.AnthropicClient
import com.treasure.core.ai.OpenAiClient
import com.treasure.core.ai.Provider

/**
 * Persisted user settings — currently the BYO AI configuration.
 * Stored in [EncryptedSharedPreferences] so the API key isn't readable
 * even if the device is rooted later.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    var provider: Provider
        get() = prefs.getString(KEY_PROVIDER, null)
            ?.let { runCatching { Provider.valueOf(it) }.getOrNull() }
            ?: Provider.Anthropic
        set(value) { prefs.edit().putString(KEY_PROVIDER, value.name).apply() }

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().run {
                if (value.isNullOrBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, value)
                apply()
            }
        }

    var model: String
        get() = prefs.getString(KEY_MODEL, defaultModelFor(provider))
            ?: defaultModelFor(provider)
        set(value) {
            prefs.edit().putString(KEY_MODEL, value.ifBlank { defaultModelFor(provider) }).apply()
        }

    var baseUrl: String?
        get() = prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().run {
                if (value.isNullOrBlank()) remove(KEY_BASE_URL) else putString(KEY_BASE_URL, value)
                apply()
            }
        }

    var presetId: String?
        get() = prefs.getString(KEY_PRESET_ID, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().run {
                if (value.isNullOrBlank()) remove(KEY_PRESET_ID) else putString(KEY_PRESET_ID, value)
                apply()
            }
        }

    /** Cycle 0014：采样温度。null = 走 provider 默认。 */
    var temperature: Double?
        get() = if (prefs.contains(KEY_TEMPERATURE)) prefs.getFloat(KEY_TEMPERATURE, -1f).toDouble() else null
        set(value) {
            prefs.edit().run {
                if (value == null) remove(KEY_TEMPERATURE) else putFloat(KEY_TEMPERATURE, value.toFloat())
                apply()
            }
        }

    /** Cycle 0014：是否开启 thinking（Anthropic thinking block / OpenAI o1 / Kimi thinking 模型）。 */
    var thinkingEnabled: Boolean
        get() = prefs.getBoolean(KEY_THINKING_ENABLED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_THINKING_ENABLED, value).apply()
        }

    /**
     * Cycle 0018：上次 [SettingsViewModel.testConnection] 是否成功。
     * 用来在 Settings 摘要卡上区分 "未连通"（黄）和 "已连通"（绿）。
     * 任何 save() 都会重置回 false（配置变了，旧成功不算数）。
     */
    var lastTestPassed: Boolean
        get() = prefs.getBoolean(KEY_LAST_TEST_PASSED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LAST_TEST_PASSED, value).apply()
        }

    fun hasKey(): Boolean = !apiKey.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "treasure_settings"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_PRESET_ID = "preset_id"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_THINKING_ENABLED = "thinking_enabled"
        private const val KEY_LAST_TEST_PASSED = "last_test_passed"

        fun defaultModelFor(provider: Provider): String = when (provider) {
            Provider.Anthropic -> AnthropicClient.DEFAULT_MODEL
            Provider.OpenAi, Provider.OpenAiCompatible -> OpenAiClient.DEFAULT_MODEL
        }

        fun defaultBaseUrlFor(provider: Provider): String? = when (provider) {
            Provider.Anthropic -> "https://api.anthropic.com"
            Provider.OpenAi -> "https://api.openai.com"
            Provider.OpenAiCompatible -> null  // user must supply
        }
    }
}
