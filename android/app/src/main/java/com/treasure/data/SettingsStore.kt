package com.treasure.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.treasure.core.ai.AnthropicClient

/**
 * Persisted user settings — currently just the BYO AI configuration.
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

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().run {
                if (value.isNullOrBlank()) remove(KEY_API_KEY) else putString(KEY_API_KEY, value)
                apply()
            }
        }

    var model: String
        get() = prefs.getString(KEY_MODEL, AnthropicClient.DEFAULT_MODEL)
            ?: AnthropicClient.DEFAULT_MODEL
        set(value) {
            prefs.edit().putString(KEY_MODEL, value.ifBlank { AnthropicClient.DEFAULT_MODEL }).apply()
        }

    fun hasKey(): Boolean = !apiKey.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "treasure_settings"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
    }
}
