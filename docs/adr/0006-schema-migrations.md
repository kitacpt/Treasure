# ADR-0006 · Schema migrations as a hard rule

- **状态：** accepted (cycle 0010 落地)
- **日期：** 2026-05-08

## 背景

Cycle 0001 → 0009 一直用 `Room.databaseBuilder(...).fallbackToDestructiveMigration()`：每次升数据库版本就把库抹掉重建。开发期还行（没真用户、种子数据也是首启写入），但任何接近 release 的 build 都受不了——用户手动录入的物品、编辑过的 hero specs、上传到 `filesDir/photos/<id>/*.jpg` 的实拍引用，都会因为一次 schema 升级就丢。

到 cycle 0009 我们已经 destructive 过 8 次。再不立规矩就出大事。

## 决策

从 cycle 0010 起：

1. `@Database(... exportSchema = true)`；KSP 配置 `room.schemaLocation = $projectDir/schemas`，每个版本的 schema JSON 提交到 `core/schemas/com.treasure.core.room.TreasureDatabase/<version>.json`，跟 git 走。
2. 任何对 entity / column / index 的改动都必须：
   - bump `@Database(version = N+1)`
   - 在 `core/room/Migrations.kt` 的 `ALL` 数组里追加 `MIGRATION_N_N+1`
   - 配套 `:core` 的 `MigrationTest`（用 `MigrationTestHelper`）跑通 N → N+1
3. 永远不再调 `.fallbackToDestructiveMigration()`。允许的只有 `.fallbackToDestructiveMigrationOnDowngrade()`，仅用于 dev 环境装回旧版 APK。
4. v5 是 cycle 0010 的 baseline schema —— 此前 v1-v4 因为 destructive 没留下可信记录，已经无法构造迁移测试。这一段历史就此定格。

## 后果

- 后续每次改 schema 都要多写：1 个 Migration + 1 个 test + 1 次 schema JSON 提交。开发负担可控，相比丢用户数据是绝对划算。
- `core/schemas/` 目录的 diff 评审是 PR 的强制项 —— review 时一旦看到只 bump 了 `@Database(version)` 但没新 schema JSON，就要打回。
- `MigrationTest` 跑在 `:core` 的 androidTest source set。CI 上跑 `./gradlew :core:connectedAndroidTest` 即可（cycle 0010 暂时手测，CI hookup 留给后续 cycle）。

## 相关

- 取代 cycle 0001 隐含的 “MVP 期间用 destructive” 临时决策
- 不影响 [ADR-0003](0003-local-first-with-optional-sync.md)：Room 仍是权威源
