package com.treasure.core.ai

import com.treasure.core.domain.HeroSpec
import kotlinx.serialization.Serializable

/**
 * Per [ADR-0004], AI services are user-supplied (BYO key, device-direct,
 * no proxy). This interface is what the UI talks to; concrete impls live
 * in this package — [AnthropicClient] for Claude, [OpenAiClient] for
 * OpenAI and OpenAI-compatible endpoints (e.g. self-hosted vLLM,
 * DeepSeek, etc).
 */
interface AiClient {
    /**
     * Given a user's text description, optional JPEG photo, and any
     * earlier turns in the conversation, return a structured
     * [ItemDraft] the UI can pre-fill the manual form with.
     *
     * 多轮：[priorTurns] 按时间顺序传入；最早在前。
     */
    /**
     * @param baseline 上一次用户已"采用"的草稿。Cycle 0024：AI 不从零开
     *  始 propose，而是基于这份基线给出"下一版" — 给 AI 一种"修订"语境，
     *  避免每轮都生成完全不同的字段集。
     * @param categoryHints Cycle 0027：当前用户可用的分类列表（内建 +
     *  未隐藏的自定义）。空时退回 SYSTEM_PROMPT 里的内建 6 个。非空时
     *  会拼到 system prompt 让 AI 选自定义 id（比如"custom-xxx"）。
     */
    suspend fun extractItemDraft(
        text: String,
        imageJpegBytes: ByteArray? = null,
        priorTurns: List<AiTurn> = emptyList(),
        baseline: ItemDraft? = null,
        categoryHints: List<CategoryHint> = emptyList(),
    ): Result<ItemDraft>
}

enum class AiRole { USER, ASSISTANT }

data class AiTurn(val role: AiRole, val text: String)

/**
 * 当模型选择 *不* 调 fill_item_draft，而是回了一段普通对话文字时，
 * 客户端把文字封进这个异常。AddViewModel 接到它就直接把 `text` 作为
 * 助手聊天消息插进对话流，不显示为 "出错了：…"。
 */
class ChatOnlyResponseException(val text: String) : Exception("model returned chat text instead of tool call")

/**
 * 在自由文本里捞出第一段平衡的 `{ ... }`。给 thinking 模式 + tool_choice=auto
 * 的回退路径用 — 模型有时不调 tool 而是把 JSON 直接写在文字里，可能还包了
 * markdown 代码围栏。简单括号配对，足够应付 "前面一段思考 + 后面一坨 JSON" 的形态。
 */
internal fun extractFirstJsonObject(text: String): String? {
    val start = text.indexOf('{')
    if (start < 0) return null
    var depth = 0
    var inString = false
    var escape = false
    for (i in start until text.length) {
        val c = text[i]
        if (inString) {
            if (escape) { escape = false; continue }
            when (c) {
                '\\' -> escape = true
                '"' -> inString = false
            }
            continue
        }
        when (c) {
            '"' -> inString = true
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return text.substring(start, i + 1)
            }
        }
    }
    return null
}

/**
 * Provider taxonomy. [OpenAiCompatible] uses the OpenAI client with a
 * custom baseUrl — covers anything that speaks the OpenAI v1 schema.
 */
enum class Provider(val display: String) {
    Anthropic("Anthropic"),
    OpenAi("OpenAI"),
    OpenAiCompatible("Custom (OpenAI-compatible)"),
}

/**
 * Fields the AI tries to fill. [specs] is a single ordered list — the
 * first [com.treasure.core.domain.Item.HERO_SPEC_COUNT] entries are
 * treated as "hero" by the UI; anything after is the long-tail.
 */
@Serializable
data class ItemDraft(
    val category: String? = null, // "badminton" / "photo" / "cars" / "tech"
    val brand: String = "",
    val model: String = "",
    val nickname: String = "",
    val oneLiner: String = "",
    val specs: List<HeroSpec> = emptyList(),
    /** Cycle 0031：草稿也能编辑 history 时间轴 — 跟物品 Edit 页同款逻辑。
     *  AI 不直接填这个字段；用户在 Refine 页手动加。commitDraft 把它带进
     *  最终 Item。 */
    val history: List<com.treasure.core.domain.HistoryEvent> = emptyList(),
)
