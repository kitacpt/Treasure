package com.treasure.data

import com.treasure.core.ai.AnthropicClient
import com.treasure.core.ai.OpenAiClient
import com.treasure.core.ai.Provider

/**
 * UI-level preset for the BYO AI configuration. Each preset binds a
 * vendor (display name, key hint) to the underlying [Provider] code path
 * + a sensible default base URL + model name.
 *
 * Storage still keeps [Provider], [SettingsStore.baseUrl], etc. as the
 * source of truth — the preset id is just remembered so we can
 * re-highlight the right entry in the dropdown.
 */
enum class AiProviderPreset(
    val id: String,
    val display: String,
    val provider: Provider,
    val baseUrl: String?,
    val defaultModel: String,
    val keyHint: String,
    /** True when the preset is a generic OpenAI-compatible "fill it in
     *  yourself" entry — base URL becomes mandatory. */
    val baseUrlMandatory: Boolean = false,
) {
    Anthropic(
        id = "anthropic",
        display = "Anthropic",
        provider = Provider.Anthropic,
        baseUrl = "https://api.anthropic.com",
        defaultModel = AnthropicClient.DEFAULT_MODEL,
        keyHint = "sk-ant-...",
    ),
    OpenAi(
        id = "openai",
        display = "OpenAI",
        provider = Provider.OpenAi,
        baseUrl = "https://api.openai.com",
        defaultModel = OpenAiClient.DEFAULT_MODEL,
        keyHint = "sk-...",
    ),
    Moonshot(
        id = "moonshot",
        display = "Kimi · Moonshot",
        provider = Provider.OpenAiCompatible,
        baseUrl = "https://api.moonshot.cn/v1",
        defaultModel = "moonshot-v1-8k",
        keyHint = "sk-...",
    ),
    DeepSeek(
        id = "deepseek",
        display = "DeepSeek",
        provider = Provider.OpenAiCompatible,
        baseUrl = "https://api.deepseek.com",
        defaultModel = "deepseek-chat",
        keyHint = "sk-...",
    ),
    Qwen(
        id = "qwen",
        display = "通义千问 · Qwen",
        provider = Provider.OpenAiCompatible,
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        defaultModel = "qwen-plus",
        keyHint = "sk-...",
    ),
    Zhipu(
        id = "zhipu",
        display = "智谱 · GLM",
        provider = Provider.OpenAiCompatible,
        baseUrl = "https://open.bigmodel.cn/api/paas/v4",
        defaultModel = "glm-4-flash",
        keyHint = "your-api-key",
    ),
    Xiaomi(
        id = "xiaomi",
        display = "Xiaomi · MiLM",
        provider = Provider.OpenAiCompatible,
        baseUrl = "https://api.xiaomi.com/v1",
        defaultModel = "mimo-7b",
        keyHint = "your-api-key",
        baseUrlMandatory = true,
    ),
    Custom(
        id = "custom",
        display = "自定义 · OpenAI 兼容",
        provider = Provider.OpenAiCompatible,
        baseUrl = null,
        defaultModel = OpenAiClient.DEFAULT_MODEL,
        keyHint = "your-api-key",
        baseUrlMandatory = true,
    );

    companion object {
        fun fromId(id: String?): AiProviderPreset? =
            entries.firstOrNull { it.id == id }

        /**
         * Best-effort match for legacy state that only stored a [Provider] +
         * baseUrl. Used to seed the dropdown selection on first read after
         * upgrading from cycle 0008.
         */
        fun forLegacy(provider: Provider, baseUrl: String?): AiProviderPreset {
            if (baseUrl != null) {
                entries.firstOrNull { it.baseUrl == baseUrl }?.let { return it }
            }
            return when (provider) {
                Provider.Anthropic -> Anthropic
                Provider.OpenAi -> OpenAi
                Provider.OpenAiCompatible -> Custom
            }
        }
    }
}
