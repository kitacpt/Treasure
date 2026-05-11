package com.treasure.ui.add

import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroVector

/**
 * Per-category illustration palette. Manual entry shows these as a
 * picker so the user can pick a closer-fitting silhouette than the
 * template default.
 */
fun heroVectorOptionsFor(category: Category): List<HeroVector> = when (category) {
    Category.BADMINTON -> listOf(HeroVector.RACKET, HeroVector.SHOES, HeroVector.GENERIC)
    Category.PHOTO -> listOf(
        HeroVector.CAMERA_DSLR, HeroVector.CAMERA_RANGEFINDER,
        HeroVector.LENS_PRIME, HeroVector.TRIPOD, HeroVector.GENERIC,
    )
    Category.CARS -> listOf(HeroVector.CAR_SEDAN, HeroVector.CAR_SUV, HeroVector.GENERIC)
    Category.TECH -> listOf(
        HeroVector.LAPTOP, HeroVector.TABLET, HeroVector.EARBUDS,
        HeroVector.KINDLE, HeroVector.WATCH, HeroVector.GENERIC,
    )
    Category.COFFEE -> listOf(
        HeroVector.ESPRESSO_MACHINE, HeroVector.COFFEE_GRINDER,
        HeroVector.COFFEE_BEAN, HeroVector.GENERIC,
    )
    Category.WINE -> listOf(
        HeroVector.WINE_BOTTLE, HeroVector.COCKTAIL_GLASS, HeroVector.GENERIC,
    )
}

/**
 * Cycle 0027：按 String id 查可选插画。命中内建走 [heroVectorOptionsFor]，
 * 未命中（自定义分类）回退到一份"所有插画 + GENERIC 兜底"列表，让用户随
 * 便挑。
 */
fun heroVectorOptionsForId(id: String): List<HeroVector> {
    val builtIn = Category.entries.firstOrNull { it.id == id }
    if (builtIn != null) return heroVectorOptionsFor(builtIn)
    // 自定义分类：全套可选 + GENERIC 在末尾兜底
    return HeroVector.entries
}

/**
 * Per-category form scaffolding. Pre-fills the four hero spec labels and
 * a sensible default heroVector / palette so the user only needs to fill
 * values, not figure out what to ask themselves.
 *
 * Cycle 0009 added [tagline] and [heroSpecHints] to give each room a
 * gentler "museum plaque" feel — a one-line italic intro at the top of
 * the form and unit/example hints under each spec field.
 */
data class CategoryTemplate(
    val category: Category,
    val heroVector: HeroVector,
    val tagline: String,
    val heroSpecLabels: List<String>,
    val heroSpecHints: List<String>,
    val palette: List<String>,
)

object CategoryTemplates {
    val byCategory: Map<Category, CategoryTemplate> = mapOf(
        Category.BADMINTON to CategoryTemplate(
            category = Category.BADMINTON,
            heroVector = HeroVector.RACKET,
            tagline = "握感、磅数、攻守 — 一支拍的脾气写在这四行里。",
            heroSpecLabels = listOf("重量 (g)", "平衡点 (mm)", "中杆硬度", "穿线磅数"),
            heroSpecHints = listOf(
                "如 84-89 (3U / 4U)",
                "如 295 (头轻) · 305 (均衡) · 320 (头重)",
                "如 偏硬 / 中等 / 软",
                "如 24-26 LBS",
            ),
            palette = listOf("#0e0e0e", "#c9362f", "#e8e2d4", "#5a5a5a"),
        ),
        Category.PHOTO to CategoryTemplate(
            category = Category.PHOTO,
            heroVector = HeroVector.CAMERA_DSLR,
            tagline = "一台机器替你按下时间 — 把它的样貌记下来。",
            heroSpecLabels = listOf("画幅", "像素", "ISO 范围", "连拍"),
            heroSpecHints = listOf(
                "如 全画幅 / APS-C / M4/3",
                "如 33 MP",
                "如 100-51200",
                "如 10 fps",
            ),
            palette = listOf("#1a1a1a", "#3a3530", "#d8d2c4", "#8a8378"),
        ),
        Category.CARS to CategoryTemplate(
            category = Category.CARS,
            heroVector = HeroVector.CAR_SEDAN,
            tagline = "脚下的那匹钢铁 — 留下它最骄傲的四个数字。",
            heroSpecLabels = listOf("动力", "马力 (PS)", "0-100 (s)", "驱动"),
            heroSpecHints = listOf(
                "如 2.0T 燃油 / 纯电 / 混动",
                "如 252",
                "如 6.5",
                "如 后驱 / 四驱",
            ),
            palette = listOf("#1a1a1a", "#2a2a2a", "#c9362f", "#a8a8a8"),
        ),
        Category.TECH to CategoryTemplate(
            category = Category.TECH,
            heroVector = HeroVector.LAPTOP,
            tagline = "工具不是消耗品 — 把这台陪你写字的机器收进图鉴。",
            heroSpecLabels = listOf("芯片", "内存", "存储", "屏幕"),
            heroSpecHints = listOf(
                "如 M3 Pro · Snapdragon 8 Gen 3",
                "如 16 GB",
                "如 512 GB / 1 TB",
                "如 14.2\" 3K 120Hz",
            ),
            palette = listOf("#3a3a3c", "#1a1a1c", "#d8d2c4", "#8a8378"),
        ),
        Category.COFFEE to CategoryTemplate(
            category = Category.COFFEE,
            heroVector = HeroVector.ESPRESSO_MACHINE,
            tagline = "豆子、机器、研磨度 — 把每天那杯的来历钉进图鉴。",
            heroSpecLabels = listOf("品类", "产地 / 烘焙度", "研磨度", "用法"),
            heroSpecHints = listOf(
                "如 意式机 · 磨豆机 · 单品豆",
                "如 埃塞俄比亚 · 中浅烘",
                "如 14 · 18 (espresso / pour over)",
                "如 9 bar 双份 / V60 圆锥",
            ),
            palette = listOf("#3d2818", "#7a4423", "#e8d5b7", "#1a1815"),
        ),
        Category.WINE to CategoryTemplate(
            category = Category.WINE,
            heroVector = HeroVector.WINE_BOTTLE,
            tagline = "一瓶一签 — 把这趟陈年的旅途留下名字。",
            heroSpecLabels = listOf("酒种", "酒精度 / 容量", "产地 / 年份", "酒款"),
            heroSpecHints = listOf(
                "如 红酒 · 威士忌 · 杜松子 · 调酒工具",
                "如 13.5% / 750 ml",
                "如 Bordeaux · 2018",
                "如 Macallan 12 · Hendrick's",
            ),
            palette = listOf("#3b1212", "#8a3a1f", "#e8e2d4", "#1a1815"),
        ),
    )

    fun forCategory(c: Category): CategoryTemplate =
        byCategory[c] ?: byCategory.values.first()
}
