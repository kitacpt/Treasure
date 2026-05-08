package com.treasure.core.domain

/**
 * Domain model. Persistence-agnostic — no Room annotations on this side.
 *
 * As of cycle 0006, [specs] is a single ordered list. The first
 * [HERO_SPEC_COUNT] items are treated as "hero" specs (shown on the Detail
 * front face); the rest live in the drawer's 参数 tab. Users reorder the
 * list to choose which 4 are hero.
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
    val specs: List<HeroSpec>,
    val history: List<HistoryEvent>,
    val photos: List<String>, // absolute file paths in app's private storage
    val createdAt: Long,
    val updatedAt: Long,
) {
    /** Convenience views — not persisted, computed from [specs]. */
    val heroSpecs: List<HeroSpec> get() = specs.take(HERO_SPEC_COUNT)
    val tailSpecs: List<HeroSpec> get() = specs.drop(HERO_SPEC_COUNT)

    companion object {
        const val HERO_SPEC_COUNT: Int = 4
    }
}
