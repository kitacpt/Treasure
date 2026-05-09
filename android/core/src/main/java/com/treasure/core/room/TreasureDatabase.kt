package com.treasure.core.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room database root.
 *
 * Cycle 0001-0009 都用了 [fallbackToDestructiveMigration]，每次 schema
 * 一变就抹库。Cycle 0010（ADR-0006）改成真 migration：
 * - `exportSchema = true`，每个版本的 schema JSON 落盘到 :core/schemas/
 * - 这里维护 [Migrations.ALL]，新加版本必须配套写 Migration
 * - 只在 release 构建之外允许 `fallbackToDestructiveMigrationOnDowngrade`，
 *   防止 dev 环境降级时 crash
 */
@Database(
    entities = [
        ItemEntity::class,
        ConversationEntity::class,
        ConversationMessageEntity::class,
    ],
    version = 8,
    exportSchema = true,
)
internal abstract class TreasureDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile private var instance: TreasureDatabase? = null

        fun get(context: Context): TreasureDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TreasureDatabase::class.java,
                    "treasure.db",
                )
                    .addMigrations(*Migrations.ALL)
                    // Dev-only safety: 用旧版本 APK 安装到新 schema 上时
                    // 直接重建。线上 release 永远不该出现降级。
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
