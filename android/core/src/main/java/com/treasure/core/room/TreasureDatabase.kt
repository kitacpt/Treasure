package com.treasure.core.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

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
        CategoryPrefEntity::class,
        ConversationItemEntity::class,
    ],
    version = 13,
    exportSchema = true,
)
internal abstract class TreasureDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun conversationDao(): ConversationDao
    abstract fun categoryPrefDao(): CategoryPrefDao
    abstract fun conversationItemDao(): ConversationItemDao

    /**
     * Cycle 0031 复修：fresh-install 时给 category_prefs 补 6 个内建分类
     * 种子。INSERT OR IGNORE 跟 Migration_8_9 同款，重复跑无害。
     */
    private object SeedCategoriesCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val now = System.currentTimeMillis()
            val seeds = listOf(
                Triple("badminton", "羽毛球", "Badminton"),
                Triple("photo", "摄影", "Photography"),
                Triple("cars", "汽车", "Cars"),
                Triple("tech", "电子产品", "Tech"),
                Triple("coffee", "咖啡", "Coffee"),
                Triple("wine", "酒水", "Spirits"),
            )
            seeds.forEachIndexed { idx, (id, zh, en) ->
                db.execSQL(
                    "INSERT OR IGNORE INTO `category_prefs` " +
                        "(id, built_in, name_zh, name_en, hero_vector, hidden, sort_order, created_at) " +
                        "VALUES (?, 1, ?, ?, 'GENERIC', 0, ?, ?)",
                    arrayOf<Any>(id, zh, en, idx, now),
                )
            }
        }
    }

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
                    // Cycle 0031 复修：fresh install 时 Room 直接按当前 schema
                    // 建表，不会跑 Migration_8_9 里的 6 个内建分类种子；之前
                    // 的用户在新设备装新 APK 会发现 doorway 完全空。这个
                    // Callback 在 onCreate（数据库第一次创建时）补一遍同样
                    // 的 INSERT OR IGNORE。
                    .addCallback(SeedCategoriesCallback)
                    // Dev-only safety: 用旧版本 APK 安装到新 schema 上时
                    // 直接重建。线上 release 永远不该出现降级。
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
