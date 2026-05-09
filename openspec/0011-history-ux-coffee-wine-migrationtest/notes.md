# Cycle 0011 · notes

## 文件改动一览

新建：

- `app/.../ui/components/HeroAvatarPicker.kt` — 共享头像式插画选择器
- `app/.../illust/EspressoMachine.kt` / `CoffeeGrinder.kt` / `CoffeeBean.kt` / `WineBottle.kt` / `CocktailGlass.kt` — 5 张博物馆线描风插画
- `core/src/androidTest/java/.../room/MigrationTest.kt` — 4 个 Room 迁移测试

主要修改：

- `app/.../ui/add/AddRoute.kt` — root 改双层 Box，弹层全部内嵌
- `app/.../ui/add/AddViewModel.kt` — `renameConversation` / `deleteConversation` / `buildOpener`
- `app/.../ui/add/AddChat.kt` — HistoryDropdown 加 ✎/✕、改名 dialog、删除确认 dialog；HistoryRow 重写；空态文案
- `app/.../ui/add/CategoryForm.kt` — 用 HeroAvatarPicker；删除内嵌 IllustrationPicker
- `app/.../ui/add/CategoryTemplate.kt` — Coffee / Wine 两份模板 + heroVectorOptionsFor 两组
- `app/.../ui/edit/EditScreen.kt` — 顶部插入 HeroAvatarPicker；"插画" Section 移除
- `app/.../ui/portal/PortalScreen.kt` — DoorwaysGrid 自适应（chunked(2) + 罗马数字数组）
- `app/.../ui/settings/SettingsScreen.kt` — EditorSheet 内 padding bottom 96.dp
- `app/.../illust/HeroIllustration.kt` — 5 个新 HeroVector 的 dispatch
- `core/.../domain/Category.kt` — COFFEE / WINE
- `core/.../domain/HeroVector.kt` — 5 个新 vector
- `core/build.gradle.kts` — testInstrumentationRunner / androidTest assets / room-testing deps

## 设计取舍

### Pager + AddRoute 的弹层 bug

cycle 0010 把主屏改成 HorizontalPager，AddRoute 进了 page slot。原 AddRoute 把 ManualCategoryPicker 平级 emit 在 root Box 之外 — NavHost 给的 entry slot 是 AnimatedContent / Box，多 child emit 会自然叠层；但 HorizontalPager 给的 page lambda 用 `SubcomposeLayout` 测量 + 单 LayoutNode 摆放，平级 emit 的 Box(fillMaxSize) 在某些情况下被裁到 page bound 之外 / z-order 错乱。

修复：把所有弹层（manualPicker / manualSession sheet / VoiceCapture）都挂回 root Box 内。代价是 ManualCategoryPicker 的全屏蒙层不能盖到状态栏区域 —— 但视觉上影响很小。

### HistoryDropdown 限高

之前会无限往下堆。新结构：

- 外框 `heightIn(max = 520dp)` + 顶部底部 padding 给 status bar / 控制岛 让位
- 内嵌 `LazyColumn(weight(1f, fill = false))` 滚动 — 用 `fill = false` 让它在内容少时不抢空间
- LazyColumn 上下夹 fixed 头部（HistoryHeader）+ 尾部 NewChatRow
- 每行末尾两个 28dp 圆形 IconGlyphButton

### 新对话反馈

考虑过：toast、snackbar、闪一下高亮、弹 dialog。最终选了 *把时间戳塞进 assistant 第一句*：

```
你好。把新东西的照片发给我，或者直接说说它是什么。
（新对话 · 15:32 开始）
```

零额外 UI 代码、信息持久落库、用户翻历史时也能自然分段。

### HeroAvatarPicker 的 disclosure 模式

之前两屏的插画选择器思路不一样：

- 手动录入页（cycle 0010）顶部 124dp 大方框 + 横滚 56dp 候选 — 永远展开
- Edit 屏（cycle 0001-0009）一行横滚 72dp 候选 — 没大图

新组件：默认只展示选中的（112dp 圆形 paper 底，像头像）；点头像 → disclosure 展开候选。两个屏共用，视觉鼓点一致；候选默认折叠节省屏幕空间。

### Coffee / Wine 模板字段

设计时主要考虑：用户场景是 "我有什么 + 它的关键参数"。咖啡 / 酒水跟羽毛球摄影不同，单一物品维度模糊（一颗咖啡豆 ≠ 一台咖啡机）。所以两个模板的第一个字段都是 *品类自指*（"意式机 / 磨豆机 / 单品豆" / "红酒 / 威士忌 / 杜松子 / 调酒工具"），让用户先给自己分类再填具体参数。

### 插画风格

仿造 Racket / Camera 的：

- 单一 viewBox，所有坐标硬编码（同 prototype/vectors.jsx）
- `INK` 描边 + palette 色 wash + 几条灰色细节线
- 颜色不饱和、形态偏图鉴版画
- 不使用图片资源、不依赖 SVG drawable，全部 Compose Canvas

### MigrationTest 设计

每个 Migration 必须配套至少一个测试：

1. 写一行旧 schema 数据
2. 跑 migration（`runMigrationsAndValidate`）
3. 校验旧数据保留 + 新结构生效

`runMigrationsAndValidate` 内部会比对当前 schema 和 JSON baseline，任何 ALTER 漏掉 / 类型不对都会抛。

加了第 4 个 sanity 测试 `room_can_open_migrated_database`：用 Room runtime（含完整 Migrations.ALL）打开一次，让 Room 自己的 identity hash 校验把所有列 / 索引 / FK 一并查一遍。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL

cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :core:compileDebugAndroidTestKotlin
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### MigrationTest 真机跑

需要连一台真机或模拟器：

```
adb devices  # 确认设备在
ANDROID_HOME=$HOME/Android/Sdk ./gradlew :core:connectedDebugAndroidTest
```

### 手测要点

- 录入页右上 [手动] 应能弹出品类选择层
- 历史抽屉滚动 / 改名 / 删除 / 删当前会话自动新建
- 新对话首行带 "（新对话 · HH:MM 开始）"
- Edit 顶部头像点击展开候选，再点收起
- 手动录入页同款头像
- Settings 抽屉里保存 / 测试按钮在胶囊之上可见
- Portal 6 个 doorway，Coffee / 酒水的 hero 显示对应新插画
- Coffee 手动录入 → 头像默认是 espresso machine，候选有 grinder / bean / generic
- Wine 手动录入 → 头像默认是 wine bottle，候选有 cocktail glass / generic
