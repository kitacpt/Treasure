package com.treasure.core.domain

/**
 * Domain model. Persistence-agnostic — no Room annotations on this side.
 * The Room entity layer translates to/from this shape.
 */
data class Item(
    val id: String,
    val category: Category,
    val brand: String,
    val model: String,
    val nickname: String,
    val acquired: String, // YYYY-MM-DD
    val parted: String?,
    val status: ItemStatus,
    val palette: List<String>, // 4 hex strings, e.g. ["#1a1a1a", ...]
    val oneLiner: String,
    val heroVector: HeroVector,
    val heroSpecs: List<HeroSpec>,
    val specs: Map<String, String>,
    val history: List<HistoryEvent>,
    val photos: List<String>, // absolute file paths in app's private storage
    val createdAt: Long,
    val updatedAt: Long,
)
