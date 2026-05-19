package com.treasure.core.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ItemDao {
    // Cycle 0033：默认排序 = sort_order ASC（小的在前）。新物品 sort_order =
    // 当前最小 - 1，自然落在最前。用 acquired DESC 当 tie-breaker 维持老体感。
    @Query("SELECT * FROM items ORDER BY sort_order ASC, acquired DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ItemEntity?>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ItemEntity)

    @Update
    suspend fun update(entity: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Cycle 0033：取当前最小 sort_order，给"新物品在最前"逻辑用。 */
    @Query("SELECT MIN(sort_order) FROM items")
    suspend fun minSortOrder(): Long?

    @Query("UPDATE items SET sort_order = :order WHERE id = :id")
    suspend fun setSortOrder(id: String, order: Long)

    /** Cycle 0037：备份恢复前整体清空。 */
    @Query("DELETE FROM items")
    suspend fun deleteAll()
}
