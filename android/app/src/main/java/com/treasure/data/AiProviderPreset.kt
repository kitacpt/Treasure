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

/**
 * Cycle 0022：根据 model 名启发式判定是否支持图片输入（多模态 / vision）。
 * 用户经常自定义 model 字段，没法靠 preset 一刀切；这里靠模型命名约定。
 *
 * 已知支持 vision 的：
 *  - Anthropic：claude-3*、claude-sonnet-*、claude-opus-* 全系都吃图
 *  - OpenAI：gpt-4o*、gpt-4-turbo*、gpt-4-vision*、o4*（o4 是多模态）；
 *    o1 / o3 推理系列**不**吃图（仍是 text-only reasoning）
 *  - 名称含 "vision" / "vl" / "-v"（GLM-4V / Qwen-VL）的几乎都是多模态
 *
 * 不支持的：
 *  - moonshot-v1-8k / 32k / 128k（Kimi 文本模型；moonshot-v1-vision 才支持）
 *  - deepseek-chat / deepseek-coder / deepseek-reasoner 全系
 *  - 任何 reasoner / thinking-only
 *
 * 拿不准就 false — UI 只会少显示一个 "🖼" 图标，不会误导用户上传图片
 * 然后被 provider 报错。
 */
fun modelSupportsVision(model: String): Boolean {
    val m = model.trim().lowercase()
    if (m.isEmpty()) return false

    // 显式带 vision / vl 关键词
    if (m.contains("vision")) return true
    // -vl- / -vl<digit> / glm-4v / qwen2-vl 之类
    if (Regex("\\bvl\\b").containsMatchIn(m)) return true
    if (Regex("(^|[-_/])vl[-_]?").containsMatchIn(m)) return true
    if (Regex("glm-?\\dv\\b").containsMatchIn(m)) return true

    // Anthropic Claude 全系 3+ 都是多模态
    if (m.startsWith("claude-3") ||
        m.startsWith("claude-sonnet") ||
        m.startsWith("claude-opus") ||
        m.startsWith("claude-haiku")) return true

    // OpenAI：4o / 4-turbo / 4-vision；o1 / o3 是 reasoning text-only，
    // o4-mini 已经多模态；为了避免和 "1o" / "3o" 之类自定义混淆，要求是
    // 模型名开头。
    if (m.startsWith("gpt-4o")) return true
    if (m.startsWith("gpt-4-turbo")) return true
    if (m.startsWith("gpt-4-vision")) return true
    if (m.startsWith("o4")) return true

    // Cycle 0031：Moonshot kimi-k2.5 系列内置多模态（用户实测）。kimi-k2 /
    // kimi-k2-turbo 仍是 text-only，不要笼统覆盖。
    if (m == "kimi-k2.5" || m.startsWith("kimi-k2.5-")) return true

    // Qwen / 通义千问的 VL 系列已被上面的 vl 分支覆盖
    return false
}
