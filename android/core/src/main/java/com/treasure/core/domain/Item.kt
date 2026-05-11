package com.treasure.core.domain

import androidx.compose.runtime.Immutable

/**
 * Domain model. Persistence-agnostic — no Room annotations on this side.
 *
 * As of cycle 0006, [specs] is a single ordered list. The first
 * [HERO_SPEC_COUNT] items are treated as "hero" specs (shown on the Detail
 * front face); the rest live in the drawer's 参数 tab. Users reorder the
 * list to choose which 4 are hero.
 *
 * Cycle 0013：标 @Immutable，Compose 把它当作 stable parameter，LazyColumn
 * 滚动时同一 Item 不再触发整张卡片的重组。
 */
@Immutable
data class Item(
    val id: String,
    /**
     * Cycle 0027：分类用 String id 而不是 [Category] enum，让用户自定义
     * 分类（cycle 0026 加的 category_prefs 表里 built_in=0 的行）也能挂
     * 物品。Display name / hero vector 等去 [com.treasure.core.repo
     * .CategoryRepository] 查 — domain 层不再绑死 6 个内建。
     *
     * 历史 v8 之前的数据库里 category 列就一直是字符串 id，只是 toDomain
     * 时被强行 `Category.fromId(...)` 转回 enum；本 cycle 把那层转换拆掉，
     * domain 看到的就是原值（"badminton" / "tech" / "custom-uuid-..."）。
     */
    val category: String,
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
    /**
     * 每张照片的文字标注：path → 标注列表。
     * Cycle 0010 加入；先以全照片为单位存（map），不需要单独表。
     */
    val callouts: Map<String, List<PhotoCallout>> = emptyMap(),
    /**
     * Cycle 0016：可选 — 用户从影集里挑一张照片当头像。null 时回退到
     * [heroVector] 的线描插画。值是 [photos] 列表里的某条 path（或者
     * 还没收进 photos 的临时 path，由 ViewModel 保证一致性）。
     */
    val avatarPhotoPath: String? = null,
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
