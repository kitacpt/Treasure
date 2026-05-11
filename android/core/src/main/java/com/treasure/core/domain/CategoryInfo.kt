package com.treasure.core.domain

/**
 * Cycle 0026：统一的"分类"概念，把 6 个内建 [Category] 和用户自定义分类
 * 拍在同一个模型下。UI 层 (Portal / Grid / Manager) 渲染时只看 CategoryInfo，
 * 不直接依赖 enum。
 *
 * [isBuiltIn] 决定能不能改 name_zh / name_en（内建锁名）和能不能删（内建
 * 不可删，只能 hide）。
 */
data class CategoryInfo(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val heroVector: HeroVector,
    val hidden: Boolean,
    val sortOrder: Int,
    val isBuiltIn: Boolean,
)
