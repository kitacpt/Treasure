package com.treasure.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.treasure.core.ai.AnthropicClient
import com.treasure.core.ai.OpenAiClient
import com.treasure.core.ai.Provider
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString

/**
 * Persisted user settings — currently the BYO AI configuration.
 * Stored in [EncryptedSharedPreferences] so the API key isn't readable
 * even if the device is rooted later.
 *
 * Cycle 0035：从"单份 AI 配置"重构成"多份配置 + 默认"。
 *  - 旧的 KEY_PROVIDER / KEY_API_KEY 等字段被读出来兼容老安装，迁移成
 *    profiles[0]，并保留作为单份 view 给老代码读（[provider] / [apiKey] 等
 *    getter 现在转发到 [defaultProfile]）。
 *  - 新增 [profiles] / [defaultProfileId] 是真正的源；[setProfiles] /
 *    [setDefaultProfileId] 写回。
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

    // ─── multi-profile core ──────────────────────────────────────────

    /**
     * 当前所有 AI 配置。getter 在首次访问时会做一次升级：
     *  - 已经有 KEY_PROFILES_JSON → 直接解；
     *  - 没有但旧的单份字段有内容 → 把旧字段当 profiles[0] 并写回；
     *  - 都没 → 返回空列表（用户首次安装）。
     */
    var profiles: List<AiProfile>
        get() {
            val cached = profilesCache
            if (cached != null) return cached
            val raw = prefs.getString(KEY_PROFILES_JSON, null)
            val list = if (!raw.isNullOrBlank()) {
                runCatching {
                    AiProfile.json.decodeFromString(
                        ListSerializer(AiProfile.serializer()),
                        raw,
                    )
                }.getOrDefault(emptyList())
            } else {
                migrateLegacy().also { migrated ->
                    if (migrated.isNotEmpty()) writeProfiles(migrated)
                }
            }
            profilesCache = list
            return list
        }
        set(value) { writeProfiles(value) }

    private var profilesCache: List<AiProfile>? = null

    /** 默认 profile 的 id；返回的 id 必定在 [profiles] 里（被删了就回退到第一个）。 */
    var defaultProfileId: String?
        get() {
            val ps = profiles
            if (ps.isEmpty()) return null
            val raw = prefs.getString(KEY_DEFAULT_PROFILE_ID, null)
            return ps.firstOrNull { it.id == raw }?.id ?: ps.firstOrNull()?.id
        }
        set(value) {
            prefs.edit().run {
                if (value.isNullOrBlank()) remove(KEY_DEFAULT_PROFILE_ID)
                else putString(KEY_DEFAULT_PROFILE_ID, value)
                apply()
            }
        }

    fun defaultProfile(): AiProfile? {
        val id = defaultProfileId ?: return null
        return profiles.firstOrNull { it.id == id }
    }

    fun profileById(id: String): AiProfile? = profiles.firstOrNull { it.id == id }

    /** 覆盖一份配置（id 必须已存在）。 */
    fun updateProfile(updated: AiProfile) {
        val next = profiles.map { if (it.id == updated.id) updated else it }
        writeProfiles(next)
    }

    fun addProfile(p: AiProfile): List<AiProfile> {
        val next = profiles + p
        writeProfiles(next)
        if (defaultProfileId == null) defaultProfileId = p.id
        return next
    }

    fun removeProfile(id: String): List<AiProfile> {
        val next = profiles.filterNot { it.id == id }
        writeProfiles(next)
        if (defaultProfileId == id) defaultProfileId = next.firstOrNull()?.id
        return next
    }

    private fun writeProfiles(list: List<AiProfile>) {
        profilesCache = list
        val json = AiProfile.json.encodeToString(
            ListSerializer(AiProfile.serializer()), list,
        )
        prefs.edit().putString(KEY_PROFILES_JSON, json).apply()
    }

    private fun migrateLegacy(): List<AiProfile> {
        val legacyKey = prefs.getString(KEY_API_KEY, null)
        // 没 key 也没 model 就当作首装，不造一份空 profile（按设计：用户先看到
        // "添加 AI 服务" 的幽灵卡，再选 provider）
        val legacyProvider = prefs.getString(KEY_PROVIDER, null)
        if (legacyKey.isNullOrBlank() && legacyProvider.isNullOrBlank()) return emptyList()

        val provider = legacyProvider
            ?.let { runCatching { Provider.valueOf(it) }.getOrNull() }
            ?: Provider.Anthropic
        val preset = AiProviderPreset.fromId(prefs.getString(KEY_PRESET_ID, null))
            ?: AiProviderPreset.forLegacy(provider, prefs.getString(KEY_BASE_URL, null))
        return listOf(
            AiProfile(
                id = AiProfile.newId(),
                presetId = preset.id,
                baseUrl = prefs.getString(KEY_BASE_URL, null).orEmpty()
                    .ifBlank { preset.baseUrl.orEmpty() },
                model = prefs.getString(KEY_MODEL, null).orEmpty()
                    .ifBlank { preset.defaultModel },
                apiKey = legacyKey.orEmpty(),
                temperature = if (prefs.contains(KEY_TEMPERATURE))
                    prefs.getFloat(KEY_TEMPERATURE, -1f).toDouble() else null,
                thinkingEnabled = prefs.getBoolean(KEY_THINKING_ENABLED, false),
                lastTestPassed = prefs.getBoolean(KEY_LAST_TEST_PASSED, false),
            ),
        )
    }

    // ─── per-conversation override (volatile, not persisted) ─────────

    /**
     * Cycle 0035：录入页 model chip 可以临时切换"本会话用哪份配置"，不影响
     * 默认。这里只在内存里挂一下，进程死了就回退到默认。
     */
    @Volatile
    var conversationOverrideProfileId: String? = null

    fun effectiveProfile(): AiProfile? {
        val override = conversationOverrideProfileId
        if (override != null) profiles.firstOrNull { it.id == override }?.let { return it }
        return defaultProfile()
    }

    // ─── single-profile compat shims (kept for legacy callers) ────────

    /** True when the (effective) profile has an API key set. */
    fun hasKey(): Boolean = effectiveProfile()?.hasKey == true

    val provider: Provider
        get() = effectiveProfile()?.provider ?: Provider.Anthropic

    val apiKey: String?
        get() = effectiveProfile()?.apiKey?.takeIf { it.isNotBlank() }

    val model: String
        get() = effectiveProfile()?.effectiveModel
            ?: AnthropicClient.DEFAULT_MODEL

    val baseUrl: String?
        get() = effectiveProfile()?.effectiveBaseUrl?.takeIf { it.isNotBlank() }

    val temperature: Double?
        get() = effectiveProfile()?.temperature

    val thinkingEnabled: Boolean
        get() = effectiveProfile()?.thinkingEnabled == true

    // ─── orthogonal app prefs ─────────────────────────────────────────

    /** Cycle 0031：用户手动选的主题。null = 跟随系统；true = 强制暗黑；
     *  false = 强制明亮。 */
    var darkMode: Boolean?
        get() = when (prefs.getString(KEY_DARK_MODE, null)) {
            "dark" -> true
            "light" -> false
            else -> null
        }
        set(value) {
            prefs.edit().run {
                when (value) {
                    true -> putString(KEY_DARK_MODE, "dark")
                    false -> putString(KEY_DARK_MODE, "light")
                    null -> remove(KEY_DARK_MODE)
                }
                apply()
            }
        }

    fun clear() {
        prefs.edit().clear().apply()
        profilesCache = null
    }

    companion object {
        private const val FILE_NAME = "treasure_settings"

        // legacy single-profile keys — only read for one-time migration
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_PRESET_ID = "preset_id"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_THINKING_ENABLED = "thinking_enabled"
        private const val KEY_LAST_TEST_PASSED = "last_test_passed"

        private const val KEY_PROFILES_JSON = "profiles_json"
        private const val KEY_DEFAULT_PROFILE_ID = "default_profile_id"
        private const val KEY_DARK_MODE = "dark_mode"

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
