package com.treasure.core.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migration registry — 见 ADR-0006。
 *
 * Cycle 0001-0009 一直用 destructive fallback，所以历史版本（v1-v4）
 * 没有真正可读的 schema JSON 留下来。从 cycle 0010 起：
 *
 * - Room 写出 v5 / v6 等 schema JSON，作为新 baseline
 * - 任何对 entity / column 的改动都必须 bump version + 在这里追加
 *   `MIGRATION_5_6` / `MIGRATION_6_7` …
 * - 配套写 `:core` 的 `MigrationTest`
 */
internal object Migrations {

    /** Cycle 0010: 加上录入页对话历史两张表。 */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `add_conversations` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `add_messages` (
                    `id` TEXT NOT NULL,
                    `conversation_id` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `text` TEXT,
                    `photo_uri` TEXT,
                    `voice_duration` TEXT,
                    `draft_json` TEXT,
                    `field_count` INTEGER,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_add_messages_conversation_id` " +
                    "ON `add_messages` (`conversation_id`)",
            )
        }
    }

    /** Cycle 0010: items 表加 callouts_json (照片文字标注的 JSON map)。 */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `items` ADD COLUMN `callouts_json` TEXT NOT NULL DEFAULT '{}'",
            )
        }
    }

    /** Cycle 0016: items 表加 avatar_photo_path（用户从影集选作头像的照片）。 */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `items` ADD COLUMN `avatar_photo_path` TEXT")
        }
    }

    /**
     * Cycle 0026: 加 category_prefs 表（管 6 个内建分类的显示/隐藏/排序
     * + 用户自定义分类）。Migration 同时种子插入 6 个内建行，保证 v8 → v9
     * 后 manager 抽屉一打开就能看到全部分类。
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `category_prefs` (
                    `id` TEXT NOT NULL,
                    `built_in` INTEGER NOT NULL,
                    `name_zh` TEXT NOT NULL,
                    `name_en` TEXT NOT NULL,
                    `hero_vector` TEXT NOT NULL,
                    `hidden` INTEGER NOT NULL,
                    `sort_order` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            // 种子 6 个内建分类。顺序按 Category enum 自带的，跟 cycle 0001
            // 起的视觉次序一致。hero_vector 默认 GENERIC（用户能在 manager
            // 抽屉里挑别的）。
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

    /**
     * Cycle 0030：category_prefs 加 hero_photo_path 列。用户在分类编辑页从
     * 相册挑一张图当分类代表图（取代 cycle 0026 的 hero_vector enum 选择）。
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `category_prefs` ADD COLUMN `hero_photo_path` TEXT")
        }
    }

    /**
     * Cycle 0031：一段录入会话不再只盯一个物品。新表 `conversation_items`
     * 记录这段会话当前的"工作集"：每行一个候选物品，含 draft（待录入 / 新
     * 修改）和 item_ref（已录入 / 新修改的 item id）+ status 三态。
     */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `conversation_items` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `conversation_id` TEXT NOT NULL,
                    `draft_json` TEXT,
                    `item_ref` TEXT,
                    `status` TEXT NOT NULL,
                    `sort_order` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_conversation_items_conversation_id` " +
                    "ON `conversation_items` (`conversation_id`)",
            )
        }
    }

    /**
     * Cycle 0032：DraftCta 行带上 action 元数据 — `action_kind`(create/modify)
     * 和 `target_id`（modify 时指向 conversation_items 里某行 ciId）。让会话
     * 里"一次 AI 提议 N 条" 的每张卡片都能独立采用 / 不要，并在采用时知道
     * 是新增还是改在哪一行。旧 draft_cta 行没这两列 → 当 create 处理。
     */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `add_messages` ADD COLUMN `action_kind` TEXT")
            db.execSQL("ALTER TABLE `add_messages` ADD COLUMN `target_id` TEXT")
        }
    }

    /**
     * Cycle 0033：items 加 sort_order。回填 -created_at 让默认顺序 ASC = newest
     * first；新物品 commit 时 VM 取"当前最小 sort_order - 1"，自然前置；用户
     * 长按拖动后改写。
     */
    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `items` ADD COLUMN `sort_order` INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE `items` SET `sort_order` = -`created_at`")
        }
    }

    /**
     * Cycle 0034：DraftCta 行存 AI 给出的 photo_assignments — 每张已解析为
     * `(sourceUri, crop, isAvatar)`。一段 JSON 串落到 add_messages 新列里，
     * 让用户重启后还能采用某张卡（包括把图拷贝到 draft 影集）。
     */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `add_messages` ADD COLUMN `photo_assignments_json` TEXT")
        }
    }

    /**
     * Cycle 0034 v2：UserVoice 行存原始音频 file path（filesDir/voice-cache/
     * <convo>/<uuid>.m4a）。voice_duration 列继续存"分:秒"字符串。重启 / 进
     * 历史会话都能回放。
     */
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `add_messages` ADD COLUMN `voice_path` TEXT")
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
        MIGRATION_13_14, MIGRATION_14_15,
    )
}
