package com.treasure.ui.add

import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroVector

/**
 * Per-category form scaffolding. Pre-fills the four hero spec labels and
 * a sensible default heroVector / palette so the user only needs to fill
 * values, not figure out what to ask themselves.
 */
data class CategoryTemplate(
    val category: Category,
    val heroVector: HeroVector,
    val heroSpecLabels: List<String>,
    val palette: List<String>,
)

object CategoryTemplates {
    val byCategory: Map<Category, CategoryTemplate> = mapOf(
        Category.BADMINTON to CategoryTemplate(
            category = Category.BADMINTON,
            heroVector = HeroVector.RACKET,
            heroSpecLabels = listOf("重量", "平衡点", "中杆", "握把"),
            palette = listOf("#0e0e0e", "#c9362f", "#e8e2d4", "#5a5a5a"),
        ),
        Category.PHOTO to CategoryTemplate(
            category = Category.PHOTO,
            heroVector = HeroVector.CAMERA_DSLR,
            heroSpecLabels = listOf("传感器", "像素", "机身防抖", "快门"),
            palette = listOf("#1a1a1a", "#3a3530", "#d8d2c4", "#8a8378"),
        ),
        Category.CARS to CategoryTemplate(
            category = Category.CARS,
            heroVector = HeroVector.CAR_SEDAN,
            heroSpecLabels = listOf("动力", "马力", "0-100", "驱动"),
            palette = listOf("#1a1a1a", "#2a2a2a", "#c9362f", "#a8a8a8"),
        ),
        Category.TECH to CategoryTemplate(
            category = Category.TECH,
            heroVector = HeroVector.LAPTOP,
            heroSpecLabels = listOf("CPU", "内存", "存储", "屏幕"),
            palette = listOf("#3a3a3c", "#1a1a1c", "#d8d2c4", "#8a8378"),
        ),
    )

    fun forCategory(c: Category): CategoryTemplate =
        byCategory[c] ?: byCategory.values.first()
}
