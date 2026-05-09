package com.treasure.core.room

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ADR-0006 钉死的：每次 schema bump 必须配套 Migration + 这个 test。
 *
 * 设备上跑：`./gradlew :core:connectedDebugAndroidTest`
 * Schema JSON 通过 `core/schemas/` → androidTest assets srcDir 打进 test APK。
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TreasureDatabase::class.java,
    )

    /** v5 → v6：新增 add_conversations / add_messages 两张空表，原 items 数据保留。 */
    @Test
    fun migrate_5_to_6_keeps_items_and_creates_conversation_tables() {
        // 先在 v5 schema 上建库，写一行 item
        helper.createDatabase(DB, 5).use { db ->
            db.execSQL(
                """
                INSERT INTO items VALUES(
                    'tech-laptop-1', 'tech', 'Apple', 'M3 Pro', '老伙计',
                    '2026-01-01', NULL, 'OWNED',
                    '[]', 'fast machine', 'LAPTOP',
                    '[]', '[]', '[]',
                    1700000000000, 1700000000000
                )
                """.trimIndent(),
            )
        }
        // 跑 5→6 迁移并校验 schema
        helper.runMigrationsAndValidate(
            DB, 6, /* validateDroppedTables = */ true, Migrations.MIGRATION_5_6,
        ).use { db ->
            // items 表里的旧数据还在
            db.query("SELECT id, brand FROM items WHERE id = 'tech-laptop-1'").use { c ->
                assertTrue("旧 item 行应该被保留", c.moveToFirst())
                assertEquals("tech-laptop-1", c.getString(0))
                assertEquals("Apple", c.getString(1))
            }
            // 新表 add_conversations 存在且为空
            db.query("SELECT count(*) FROM add_conversations").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
            db.query("SELECT count(*) FROM add_messages").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        }
    }

    /** v6 → v7：items 表 ALTER TABLE 加 callouts_json 列，默认 '{}'。 */
    @Test
    fun migrate_6_to_7_adds_callouts_column_with_default() {
        helper.createDatabase(DB, 6).use { db ->
            db.execSQL(
                """
                INSERT INTO items VALUES(
                    'photo-x100v', 'photo', 'Fuji', 'X100V', '',
                    '2025-08-01', NULL, 'OWNED',
                    '[]', '', 'CAMERA_RANGEFINDER',
                    '[]', '[]', '[]',
                    1700000000000, 1700000000000
                )
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(
            DB, 7, true, Migrations.MIGRATION_6_7,
        ).use { db ->
            db.query("SELECT callouts_json FROM items WHERE id = 'photo-x100v'").use { c ->
                assertTrue(c.moveToFirst())
                // 旧行的新列默认 '{}'
                assertEquals("{}", c.getString(0))
            }
        }
    }

    /** v5 → v7 一连串：迁移链加起来跑通。 */
    @Test
    fun migrate_5_to_7_runs_full_chain() {
        helper.createDatabase(DB, 5).use { db ->
            db.execSQL(
                """
                INSERT INTO items VALUES(
                    'cars-suv-1', 'cars', 'Toyota', 'Land Cruiser 80', '',
                    '2024-12-01', NULL, 'OWNED',
                    '[]', '', 'CAR_SUV',
                    '[]', '[]', '[]',
                    1700000000000, 1700000000000
                )
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(
            DB, 7, true, Migrations.MIGRATION_5_6, Migrations.MIGRATION_6_7,
        ).use { db ->
            db.query("SELECT id, callouts_json FROM items WHERE id = 'cars-suv-1'").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("cars-suv-1", c.getString(0))
                assertEquals("{}", c.getString(1))
            }
        }
    }

    /** v7 → v8：items 表 ALTER TABLE 加 avatar_photo_path 列（nullable）。 */
    @Test
    fun migrate_7_to_8_adds_avatar_column_nullable() {
        helper.createDatabase(DB, 7).use { db ->
            db.execSQL(
                """
                INSERT INTO items VALUES(
                    'tech-watch-1', 'tech', 'Garmin', 'Fenix 7', '',
                    '2025-03-01', NULL, 'OWNED',
                    '[]', '', 'WATCH',
                    '[]', '[]', '[]', '{}',
                    1700000000000, 1700000000000
                )
                """.trimIndent(),
            )
        }
        helper.runMigrationsAndValidate(
            DB, 8, true, Migrations.MIGRATION_7_8,
        ).use { db ->
            db.query("SELECT avatar_photo_path FROM items WHERE id = 'tech-watch-1'").use { c ->
                assertTrue(c.moveToFirst())
                // 旧行的新列没填值 → null
                assertTrue(c.isNull(0))
            }
        }
    }

    /**
     * Sanity: 用 Room runtime（含 Migrations.ALL）真打开一次升级后的库，
     * Room 的 identity hash 校验会替我们把 column 类型 / index 一并查一遍。
     * 任何 schema 跟 v7 baseline 不匹配的迁移都会在这里抛 IllegalStateException。
     */
    @Test
    fun room_can_open_migrated_database() {
        helper.createDatabase(DB, 5).close()
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Room.databaseBuilder(context, TreasureDatabase::class.java, DB)
            .addMigrations(*Migrations.ALL)
            .build().apply {
                openHelper.writableDatabase
                assertNotNull(itemDao())
                close()
            }
    }
}
