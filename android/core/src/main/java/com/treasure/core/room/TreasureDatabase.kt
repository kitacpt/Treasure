package com.treasure.core.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ItemEntity::class], version = 4, exportSchema = false)
internal abstract class TreasureDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile private var instance: TreasureDatabase? = null

        fun get(context: Context): TreasureDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TreasureDatabase::class.java,
                    "treasure.db",
                )
                    // Cycle 0001: schema is still settling, no real users yet —
                    // wipe on incompatible upgrade rather than write migrations
                    // we'll throw away. Replace with proper Migrations after MVP.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
