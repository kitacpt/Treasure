package com.treasure.core.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cycle 0026：分类管理表。同时容纳两种东西：
 *
 * 1. 6 个内建分类的"用户偏好"（hidden / sort_order）— migration 时一次性
 *    种子插入，built_in = 1，name_zh / name_en / hero_vector 字段是冗余
 *    缓存（让 manager 抽屉一次性读全部分类，不用回头查 Category enum）
 * 2. 用户新建的自定义分类 — built_in = 0，name_zh / name_en / hero_vector
 *    由用户填写，id = UUID
 *
 * 一张表搞定查询、排序、显示/隐藏；不再需要拆 "prefs + custom" 两张表。
 */
@Entity(tableName = "category_prefs")
internal data class CategoryPrefEntity(
    @PrimaryKey val id: String,
    /** 1 = 内建（badminton / photo / cars / tech / coffee / wine），0 = 用户自定义 */
    val built_in: Int,
    val name_zh: String,
    val name_en: String,
    /** 存 [com.treasure.core.domain.HeroVector] 的 `.name`。"GENERIC" 是兜底。 */
    val hero_vector: String,
    val hidden: Int,
    val sort_order: Int,
    val created_at: Long,
    /** Cycle 0030：用户从相册挑的分类代表图，复用 item.avatarPhotoPath 路径
     *  规则（`filesDir/category-photos/<id>/<uuid>.jpg`）。null 时 Portal
     *  doorway 用 hero_vector 的线描兜底。 */
    val hero_photo_path: String? = null,
)
