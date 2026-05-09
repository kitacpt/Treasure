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

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
}
