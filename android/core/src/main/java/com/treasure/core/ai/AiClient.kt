package com.treasure.core.ai

import com.treasure.core.domain.HeroSpec
import kotlinx.serialization.Serializable

/**
 * Per [ADR-0004], AI services are user-supplied (BYO key, device-direct,
 * no proxy). This interface is what the UI talks to; concrete impls live
 * in this package — `AnthropicClient` is the only one for now.
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
 * Fields that AI tries to fill. Optional — [confidence] is the model's
 * own commentary, not used yet but useful when we want to gate "auto save"
 * behaviour later.
 */
@Serializable
data class ItemDraft(
    val category: String? = null, // "badminton" / "photo" / "cars" / "tech"
    val brand: String = "",
    val model: String = "",
    val nickname: String = "",
    val oneLiner: String = "",
    val heroSpecs: List<HeroSpec> = emptyList(),
)
