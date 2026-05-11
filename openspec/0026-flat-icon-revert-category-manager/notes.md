# Cycle 0026 · notes

## 文件改动

### Schema / 数据层
- `core/.../room/CategoryPrefEntity.kt` 新建
- `core/.../room/CategoryPrefDao.kt` 新建
- `core/.../room/Migrations.kt` 加 `MIGRATION_8_9`，含种子 6 行 INSERT
- `core/.../room/TreasureDatabase.kt` version 8 → 9，注册新 entity / DAO
- `core/schemas/.../9.json` 自动生成
- `core/.../domain/CategoryInfo.kt` 新建
- `core/.../repo/CategoryRepository.kt` 新建（接口 + Room 实现）

### App wiring
- `app/.../TreasureApp.kt` 加 `categoryRepository: CategoryRepository`

### UI 新文件
- `app/.../ui/category/CategoryManagerViewModel.kt` 新建
- `app/.../ui/category/CategoryManager.kt` 新建（含 CategoryList / CategoryEditor / HeroVectorRow）

### UI 改动
- `app/.../ui/grid/GridViewModel.kt` — `currentCategoryId: String?` 替换 `currentCategory: Category?`，加 `visibleCategories`
- `app/.../ui/grid/GridScreen.kt` — chip iterate visibleCategories，右上小红点入口，集成 CategoryManager
- `app/.../ui/portal/PortalViewModel.kt` — `visibleCategories` 替换 `roomsCount` / `countByCategory` / `latestByCategory`（都改 ById）
- `app/.../ui/portal/PortalScreen.kt` — DoorwaysGrid iterate visibleCategories；stubItemFor 接 CategoryInfo + 兜底 palette；`onEnterCategory: (String) -> Unit`
- `app/.../ui/main/MainScreen.kt` — PortalRoute 的 onEnterCategory 改字符串

### 资源
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — 恢复 cycle 0013 平面版

## 设计取舍

### 一张 `category_prefs` 表 vs 两张

我考虑过拆成 `category_prefs(id, hidden, sort_order)` + `custom_categories(id, name_zh, name_en, hero_vector, created_at)`。但内建 + 自定义在 UI 上是同一种东西，repository 要不停 join / merge。一张表带 `built_in` flag 让 dao 一句 `SELECT * ORDER BY sort_order` 全搞定，schema 也更简单。

代价：`name_zh / name_en` 对内建行是冗余的（Category enum 已经有）。但冗余在这里是 OK 的 — manager 抽屉一次性读全部就用得着；将来内建分类要本地化也方便（直接改种子值）。

### 自定义分类暂时不能装物品

这是本 cycle 最大的妥协。`Item.category: Category` 是 enum，全域使用。把它改 String 是一刀很广的 refactor，影响：

- ItemEntity / ItemTypeConverters
- AI prompt（enum 列表 → dynamic）
- AddPreview / Edit 的 InlineDropdown 数据源
- previewRowsFor / applyFieldEdit 那一坨

要做就做对，所以拆出来给 cycle 0027。本 cycle 先让 visibility 和 manager UI 落地 — 用户能立刻拿来"藏掉不关心的内建分类"，已经是大改进。

### "小红点" UI

用户原话"右上角一个小红点"。我读成 visual affordance — 一个红色小圆点，提醒"这里有功能"。但 28dp 整圈是触控区（人指头 ≥ 44dp 友好，但右上角太挤所以 28dp 折中）；视觉上只有中央 12dp 实心圆是 visible。位置：Header 上方留 28dp 距状态栏（与 GridScreen Header 同 top padding），距右边 22dp（同 Grid 的 horizontal padding）。

颜色 `#C5392E` 是 Settings 里"未配置"用的红，同一色系。不另设新 token。

### Editor sub-screen 走 sheet 内 mode 切换

最初想用嵌套 ModalBottomSheet（点编辑弹出第二层 sheet）。问题是 Material 3 ModalBottomSheet 不易嵌套，且视觉上有两个 scrim 叠加。最后用 sealed `Mode` 在抽屉内部 swap content — 单层 sheet，"‹ 返回"按钮回 list。"完成"按钮收 sheet 整体。

### Editor 内不放"取消改动"按钮

考虑过 Editor 头部加 [取消]（不保存改动直接返回）。但用户改动量小（一个 model 一个 hidden toggle 顶天了），不保存就退出的诉求不强。简化成头部 "‹ 返回" = 保存的逆操作 = 直接丢弃改动。"保存" / "新建" 才真把变化下沉到 repository。

如果将来用户报"我改完忘记保存就返回了"，再加确认。

### 自定义分类 Item.category 数据完整性

如果 cycle 0027 真把 Item.category 改 String，那么自定义分类"删除"操作要考虑遗留 item 怎么处理。本 cycle 内自定义分类还没有任何 item 引用它（因为 AI / Add 都还用 enum），所以删 = 直接 DELETE 就可以；删除 dialog 文案里仍提了"已经收在这个分类下的物品不会被删，会归到一个空 id" 是为了未来当真有遗留 item 时不打脸。

### Migration 写在 SQL 字符串里 vs 用 Room Migration Helper

`MIGRATION_8_9` 里 6 个 INSERT 是用 `execSQL` + `?` 占位符。Room 有 `MigrationContainer` 但不提供"种子数据"的 DSL，所以原生 SQL 是标准做法。`INSERT OR IGNORE` 让 migration 幂等 — 万一某种原因 migration 跑两遍，已存在的 id 不会冲突。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

schemas/9.json 已落盘，Migration 注册到 Migrations.ALL。APK 14 MB。

### 手测

1. 装 APK：图标看起来跟 cycle 0013 / 0023 那版完全一致（平面圆环 + rune）
2. 进图鉴页：右上应有一个小红圆点
3. 点小红点 → 弹底部抽屉 "分类管理"
4. 应看到 6 条 "显示中" 内建分类 + 1 条 "已隐藏" 段（空）
5. 点某个分类的 [隐藏] pill → 那行立刻迁到下面 "已隐藏" 段；同时回主屏，Portal 的对应 doorway 消失、Grid 的 chip 消失
6. 点 [显示] → 重新加回 "显示中" 段 + Portal/Grid 同步显示
7. 点 + 新增分类 → 编辑页 → 中文名填 "图书"、英文名 "Books"、插画选"通用" → [新建] → 列表多一行 "图书"（带 "自定义" pill）
8. Portal 应看到 "图书" doorway（0 pcs）；Grid chip 行也应看到 "图书 0"
9. 点新增的 "图书" 那行 → 编辑页可改名、改插画、显示/隐藏 toggle、底部 [删除分类] terra outline 按钮
10. 点 [删除分类] → AlertDialog "删除 图书？" + [删除] [取消] → 点 [删除] → 列表里 "图书" 消失 + Portal/Grid 同步消失
11. 内建分类的编辑页：中文名 / 英文名 输入框 disabled（灰色不可改），底部没有 [删除分类] 按钮
12. (升级路径) 把上次 v8 的 APK 装一份再升级到 v9：进 manager 能看到种子的 6 条；不重复也不丢
