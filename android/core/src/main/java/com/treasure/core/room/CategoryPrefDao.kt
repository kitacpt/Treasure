package com.treasure.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    /** Cycle 0031：一次性写一批 sort_order — Room 把整个 @Transaction 方法
     *  包成单个 SQLite 事务，InvalidationTracker 只触发一次 emit，避免拖动
     *  commit 时 observeAll 中间态闪一下让 UI 错位。 */
    @Transaction
    suspend fun reorder(orderedIds: List<String>, hiddenIds: Set<String>) {
        orderedIds.forEachIndexed { idx, id -> setSortOrder(id, idx) }
        orderedIds.forEach { id ->
            setHidden(id, if (id in hiddenIds) 1 else 0)
        }
    }

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

    /** Cycle 0030：设/清分类代表图。传 null 清掉。 */
    @Query("UPDATE category_prefs SET hero_photo_path = :path WHERE id = :id")
    suspend fun setHeroPhotoPath(id: String, path: String?)

    /** Cycle 0037：备份恢复整体覆盖前清空。 */
    @Query("DELETE FROM category_prefs")
    suspend fun deleteAll()
}
