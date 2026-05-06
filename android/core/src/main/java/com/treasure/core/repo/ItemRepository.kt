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

    companion object {
        fun create(context: Context): ItemRepository =
            RoomItemRepository(TreasureDatabase.get(context))
    }
}
