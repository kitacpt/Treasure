package com.treasure.core.repo

import android.content.Context
import com.treasure.core.domain.Item
import com.treasure.core.room.ItemEntity
import com.treasure.core.room.TreasureDatabase
import com.treasure.core.seed.SeedItems
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ItemRepository {
    val items: Flow<List<Item>>
    fun observeById(id: String): Flow<Item?>
    suspend fun ensureSeeded()
    suspend fun upsert(item: Item)
    suspend fun delete(id: String)
    /** Cycle 0033：返回当前最小 sort_order，commit 新物品时取此值 - 1 让它前置。 */
    suspend fun minSortOrder(): Long
    /** Cycle 0033：批量重排 — orderedIds 按用户拖完的次序，从某个起点开始
     *  顺序递增写 sort_order。同一批一次性生效（避免中途观察到中间态）。 */
    suspend fun reorder(orderedIds: List<String>)
    /** Cycle 0037：备份恢复整体覆盖前先清空。 */
    suspend fun deleteAll()
}

class RoomItemRepository internal constructor(
    private val db: TreasureDatabase,
) : ItemRepository {
    private val dao = db.itemDao()

    override val items: Flow<List<Item>> =
        dao.observeAll().map { rows -> rows.map(ItemEntity::toDomain) }

    override fun observeById(id: String): Flow<Item?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun ensureSeeded() {
        // Cycle 0031 复修：完全空 app 太冷清，恢复每个内建分类一条样例物品
        // （6 条），用户随时可以删。只在 items 表完全为空时种 — 用户清掉
        // 后不会再回来。
        if (dao.count() == 0) {
            dao.insertAll(SeedItems.all().map(ItemEntity::fromDomain))
        }
    }

    override suspend fun upsert(item: Item) {
        dao.upsert(ItemEntity.fromDomain(item))
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun minSortOrder(): Long = dao.minSortOrder() ?: 0L

    override suspend fun reorder(orderedIds: List<String>) {
        if (orderedIds.isEmpty()) return
        // 起点从当前最小往下 — 让新拍板的顺序整体落在原排序之上（newest-first 风格）。
        var order = (dao.minSortOrder() ?: 0L) - 1L
        orderedIds.forEach { id ->
            dao.setSortOrder(id, order)
            order += 1L
        }
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    companion object {
        fun create(context: Context): ItemRepository =
            RoomItemRepository(TreasureDatabase.get(context))
    }
}
