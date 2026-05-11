package com.treasure.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CategoryPrefDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CategoryPrefEntity)

    @Query("SELECT * FROM category_prefs ORDER BY sort_order ASC, created_at ASC")
    fun observeAll(): Flow<List<CategoryPrefEntity>>

    @Query("SELECT * FROM category_prefs ORDER BY sort_order ASC, created_at ASC")
    suspend fun loadAll(): List<CategoryPrefEntity>

    @Query("SELECT * FROM category_prefs WHERE id = :id")
    suspend fun load(id: String): CategoryPrefEntity?

    @Query("UPDATE category_prefs SET hidden = :hidden WHERE id = :id")
    suspend fun setHidden(id: String, hidden: Int)

    @Query("UPDATE category_prefs SET sort_order = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Int)

    /** 内建：只能改 hero_vector（用户挑插画），name_* 保留 enum 自带的中文 / 英文。 */
    @Query("UPDATE category_prefs SET hero_vector = :heroVector WHERE id = :id")
    suspend fun setHeroVector(id: String, heroVector: String)

    /** 自定义：可改 name_zh / name_en / hero_vector。 */
    @Query(
        "UPDATE category_prefs SET name_zh = :nameZh, name_en = :nameEn, " +
            "hero_vector = :heroVector WHERE id = :id AND built_in = 0",
    )
    suspend fun updateCustom(id: String, nameZh: String, nameEn: String, heroVector: String)

    @Query("DELETE FROM category_prefs WHERE id = :id AND built_in = 0")
    suspend fun deleteCustom(id: String)

    /** Cycle 0027：删自定义分类时把里面的物品挪到 tech 兜底，避免出现孤儿 id。 */
    @Query("UPDATE items SET category = 'tech' WHERE category = :id")
    suspend fun reassignItemsToTech(id: String)
}
