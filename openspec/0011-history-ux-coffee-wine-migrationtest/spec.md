# Cycle 0011 · 验收

## 录入页

- 点 [手动] 立刻弹出 4-行品类选择层（不再卡死）
- 点品类 → ModalBottomSheet 升起 CategoryForm
- 取消 / 保存 都能正常关闭
- 新对话第一句 assistant 文本带 "（新对话 · HH:MM 开始）" 后缀

## 历史抽屉

- 抽屉限高 520dp，超出在内嵌 LazyColumn 里滚
- 每行末尾两个圆形按钮：✎ 改名、✕ 删除
- 点 ✎ → 弹文本框 dialog，预填当前 title，输入新名 → 保存写回 Room + 当前会话同步刷新
- 点 ✕ → 弹确认 dialog，确认 → 从 Room 删；删的若是当前会话，自动新建一段
- 列表为空显示 "（暂无历史）"

## Edit / 手动录入：插画选择器

- 共享 `HeroAvatarPicker`（`ui/components/HeroAvatarPicker.kt`）
- 默认状态：112dp 圆形 paper 底里展示当前 hero；下面一行 caption "点头像 · 换插画"
- 点头像 → 下方一行 horizontalScroll 56dp 候选；点候选 → onSelect + 收起
- EditScreen 不再有 "插画" Section
- CategoryForm 用同一组件，两屏视觉一致

## Settings 抽屉

- 内 padding `bottom = 96.dp`，[保存] / [测试连接] 在控制岛之上完全可见

## 新品类 Coffee / Wine

- Portal DoorwaysGrid：3 行 × 2 列，Roman I-VI，6 个品类全展示
- Grid 屏 chips：6 个品类
- Edit 屏 "品类" InlineDropdown：6 项
- 手动录入 4-row 选择层：6 项
- 5 张新插画：espresso machine / coffee grinder / coffee bean / wine bottle / cocktail glass，全部 INK 描边 + palette wash 风格，符合 cycle 0001 视觉规范
- Coffee 模板默认 espresso machine + 4 个字段（品类 / 产地烘焙度 / 研磨度 / 用法）
- Wine 模板默认 wine bottle + 4 个字段（酒种 / 度数容量 / 产地年份 / 酒款）

## MigrationTest

- `:core:connectedDebugAndroidTest` 应跑过 4 个 case：
  - `migrate_5_to_6_keeps_items_and_creates_conversation_tables`
  - `migrate_6_to_7_adds_callouts_column_with_default`
  - `migrate_5_to_7_runs_full_chain`
  - `room_can_open_migrated_database`
- Schema JSON 通过 `core/build.gradle.kts` 的 `sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")` 打进 test APK assets

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
- `:core:compileDebugAndroidTestKotlin` 也通过；instrumentation 跑需要真机或模拟器
