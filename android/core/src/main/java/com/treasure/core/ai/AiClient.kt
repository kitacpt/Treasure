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
     * Given a user's text description and (optionally) a JPEG photo,
     * return a structured [ItemDraft] the UI can pre-fill the manual form
     * with.
     */
    suspend fun extractItemDraft(
        text: String,
        imageJpegBytes: ByteArray? = null,
    ): Result<ItemDraft>
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
)
