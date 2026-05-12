package com.treasure.core.repo

import android.content.Context
import com.treasure.core.domain.Category
import com.treasure.core.domain.CategoryInfo
import com.treasure.core.domain.HeroVector
import com.treasure.core.room.CategoryPrefEntity
import com.treasure.core.room.TreasureDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Cycle 0026：分类显示/隐藏/排序 + 用户自定义分类的仓库。
 *
 * 真相只有一份 — `category_prefs` 表。Migration 时种子插了 6 个内建行；
 * 用户后续可以 toggle hidden / 改 hero_vector / 加新行 / 删自定义行。
 *
 * 内建行用 [Category] enum 的 id 当主键（"badminton" / "photo" / …），
 * UI 之外的地方（Item.category / AI prompt）继续靠 [Category] enum 自己
 * 走，因为 enum 保留了 6 个 id 的稳定字符串约定。
 */
interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryInfo>>
    suspend fun loadAll(): List<CategoryInfo>
    suspend fun setHidden(id: String, hidden: Boolean)
    suspend fun setHeroVector(id: String, heroVector: HeroVector)
    suspend fun updateCustom(id: String, nameZh: String, nameEn: String, heroVector: HeroVector)
    suspend fun deleteCustom(id: String)
    suspend fun addCustom(nameZh: String, nameEn: String, heroVector: HeroVector): String
    suspend fun reorder(orderedIds: List<String>)

    /** Cycle 0031：把 reorder + setHidden 合并成单事务一次性写，
     *  observeAll 只 emit 一次。避免拖动 commit 期间 UI 闪到中间态。 */
    suspend fun applyOrderAndHidden(orderedIds: List<String>, hiddenIds: Set<String>)

    /** Cycle 0030：设置 / 清除分类代表图。传 null 清除。 */
    suspend fun setHeroPhotoPath(id: String, path: String?)
}

class RoomCategoryRepository internal constructor(
    db: TreasureDatabase,
) : CategoryRepository {
    private val dao = db.categoryPrefDao()

    override fun observeAll(): Flow<List<CategoryInfo>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun loadAll(): List<CategoryInfo> =
        dao.loadAll().map { it.toDomain() }

    override suspend fun setHidden(id: String, hidden: Boolean) {
        dao.setHidden(id, if (hidden) 1 else 0)
    }

    override suspend fun setHeroVector(id: String, heroVector: HeroVector) {
        dao.setHeroVector(id, heroVector.name)
    }

    override suspend fun updateCustom(
        id: String,
        nameZh: String,
        nameEn: String,
        heroVector: HeroVector,
    ) {
        dao.updateCustom(id, nameZh.trim(), nameEn.trim(), heroVector.name)
    }

    override suspend fun deleteCustom(id: String) {
        // Cycle 0027：自定义分类删除前先把它名下的 item 重指给 tech（兜底），
        // 不让任何 item 落到孤儿 id 上。
        dao.reassignItemsToTech(id)
        dao.deleteCustom(id)
    }

    override suspend fun addCustom(
        nameZh: String,
        nameEn: String,
        heroVector: HeroVector,
    ): String {
        val id = "custom-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        // sortOrder 排在所有现有项之后
        val maxOrder = dao.loadAll().maxOfOrNull { it.sort_order } ?: -1
        dao.upsert(
            CategoryPrefEntity(
                id = id,
                built_in = 0,
                name_zh = nameZh.trim().ifBlank { "未命名" },
                name_en = nameEn.trim(),
                hero_vector = heroVector.name,
                hidden = 0,
                sort_order = maxOrder + 1,
                created_at = now,
            ),
        )
        return id
    }

    override suspend fun reorder(orderedIds: List<String>) {
        orderedIds.forEachIndexed { idx, id -> dao.setSortOrder(id, idx) }
    }

    override suspend fun applyOrderAndHidden(
        orderedIds: List<String>,
        hiddenIds: Set<String>,
    ) {
        dao.reorder(orderedIds, hiddenIds)
    }

    override suspend fun setHeroPhotoPath(id: String, path: String?) {
        dao.setHeroPhotoPath(id, path)
    }

    companion object {
        fun create(context: Context): CategoryRepository =
            RoomCategoryRepository(TreasureDatabase.get(context))
    }
}

private fun CategoryPrefEntity.toDomain(): CategoryInfo {
    val isBuiltIn = built_in != 0
    // Cycle 0028：内建分类的 heroVector 一律以 Category enum 的 defaultHeroVector
    // 为准 — 不读 row 里存的（cycle 0026 种子写的是 GENERIC，是个错）。自定义分
    // 类才用用户存的值。
    val storedHv = runCatching { HeroVector.valueOf(hero_vector) }
        .getOrDefault(HeroVector.GENERIC)
    val hv = if (isBuiltIn) {
        Category.entries.firstOrNull { it.id == id }?.defaultHeroVector ?: storedHv
    } else storedHv
    return CategoryInfo(
        id = id,
        nameZh = name_zh,
        nameEn = name_en,
        heroVector = hv,
        hidden = hidden != 0,
        sortOrder = sort_order,
        isBuiltIn = built_in != 0,
        heroPhotoPath = hero_photo_path,
    )
}
