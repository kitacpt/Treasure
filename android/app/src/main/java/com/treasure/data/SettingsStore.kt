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
