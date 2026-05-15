package com.treasure.data

import com.treasure.core.ai.Provider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Cycle 0035：单个 AI 配置实体。
 *
 * 之前一台机只能存一份 AI 配置（顶层 [SettingsStore] 上的 provider / apiKey /
 * model / baseUrl 等字段）；本 cycle 起改成"多份配置 + 其中一份是默认"：
 *  - 用户在 Settings 页里横滑切换；点 "设为默认" 把这份钉为下一次 [AiClient] 构造的来源
 *  - 录入页可以点模型 chip 临时切换"本会话用哪份"，不影响默认
 *
 * 老字段在 [SettingsStore] 里保留：第一次升级后被读出来变成第一份 profile，
 * 之后写在 KEY_PROFILES_JSON 里。
 */
@Serializable
data class AiProfile(
    val id: String,
    val presetId: String,
    val baseUrl: String,
    val model: String,
    val apiKey: String,
    val temperature: Double? = null,
    val thinkingEnabled: Boolean = false,
    val lastTestPassed: Boolean = false,
    /** Cycle 0035 v2：用户自定义显示名。空 = 回退到 [AiProviderPreset.display]。
     *  之前卡片直接展示 "Anthropic / OpenAI" 等 provider 名，不方便区分多份同 provider
     *  的配置（"工作密钥" vs "测试密钥"）；这里允许用户改一个有意义的标题。 */
    val displayName: String = "",
) {

    /** 卡片头部 / chatbar chip 上展示的标题。 */
    val title: String get() = displayName.ifBlank { preset.display }
    val preset: AiProviderPreset
        get() = AiProviderPreset.fromId(presetId) ?: AiProviderPreset.Anthropic

    val provider: Provider
        get() = preset.provider

    val effectiveBaseUrl: String
        get() = baseUrl.ifBlank { preset.baseUrl.orEmpty() }

    val effectiveModel: String
        get() = model.ifBlank { preset.defaultModel }

    val hasKey: Boolean get() = apiKey.isNotBlank()

    /** Settings 页右上"已连通/未连通/未配置"的实时状态。 */
    val connectivity: Connectivity
        get() = when {
            !hasKey -> Connectivity.Unconfigured
            !lastTestPassed -> Connectivity.Idle
            else -> Connectivity.Connected
        }

    enum class Connectivity { Unconfigured, Idle, Connected }

    /** 给录入页 model chip 显示的短名 — 用户改过 [displayName] 就用它，
     *  否则回退到 model 字段。 */
    fun shortLabel(): String {
        if (displayName.isNotBlank()) return displayName
        val m = effectiveModel
        return when {
            m.isBlank() -> preset.display
            else -> m
        }
    }

    companion object {
        fun newId(): String = "p-" + UUID.randomUUID().toString().take(8)

        fun fromPreset(preset: AiProviderPreset, id: String = newId()): AiProfile =
            AiProfile(
                id = id,
                presetId = preset.id,
                baseUrl = preset.baseUrl.orEmpty(),
                model = preset.defaultModel,
                apiKey = "",
            )

        internal val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}
