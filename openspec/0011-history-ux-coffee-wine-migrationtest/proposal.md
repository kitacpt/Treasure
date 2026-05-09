# Cycle 0011 · 历史对话 UX + Coffee/Wine 品类 + MigrationTest 自动化

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 6 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 手动录入页点不出来 | cycle 0010 把主屏改成 HorizontalPager 后，AddRoute 把 ManualCategoryPicker / ModalBottomSheet 平级 emit 在 root Box *之外*，导致它们落在 pager page slot 不可见的位置。这一刀把 root 改成 `Box(fillMaxSize)` + 内层 `Box(statusBarsPadding)` 装 chat content；ManualCategoryPicker / ModalBottomSheet / VoiceCapture 全部挂在外层 Box 内，z-order 自然叠在 chat 之上 |
| 2 | 历史对话感觉不到“新建了”，旧对话无限堆、不能删 | (a) 每段对话第一句 assistant 不再用固定 GREETING，append 一行 "（新对话 · HH:MM 开始）" 让用户看到时间戳标记；(b) HistoryDropdown 限高 520dp + 下方 120dp 余量 + 内嵌 LazyColumn 滚动；(c) 每条历史行末尾加 ✎ / ✕ 两个 28dp 圆形按钮：✎ 弹改名 dialog（文本框预填当前 title），✕ 弹删除确认 dialog；空列表时显示 "（暂无历史）"；(d) ViewModel 加 `renameConversation(id, newTitle)` / `deleteConversation(id)`：删的若是当前对话就自动 `newConversation()` |
| 3 | Edit 页插画放最顶部、像头像，点了再选 | 新增共享组件 `ui/components/HeroAvatarPicker`：默认只展示选中那一张大图（112dp 圆形 paper 底）+ 一行 caption "点头像 · 换插画"；点头像 → 下面 horizontalScroll 出候选小圆形 56dp，再点候选 → 收起；EditScreen 把原 "插画" Section + 横排 HeroVectorPicker 移除，改成 EditPageHeader 之后第一条；CategoryForm 同款替换上一刀的 IllustrationPicker，两屏视觉完全一致 |
| 4 | AI 配置抽屉被底部胶囊挡住 | EditorSheet 内层 padding `bottom = 18.dp → 96.dp`（控制岛胶囊 ~50dp + 自身 18dp + 缓冲），保存 / 测试连接两按钮都能完整露出在胶囊之上 |
| 5 | 新增 Coffee + Wine 品类 + 预置插画 | (a) `Category` enum 加 `COFFEE("coffee", "咖啡", "Coffee")` / `WINE("wine", "酒水", "Spirits")`；(b) `HeroVector` 加 `ESPRESSO_MACHINE` / `COFFEE_GRINDER` / `COFFEE_BEAN` / `WINE_BOTTLE` / `COCKTAIL_GLASS` 五个；(c) 五张博物馆线描风 Compose Canvas illustration（[EspressoMachine.kt](../../android/app/src/main/java/com/treasure/illust/EspressoMachine.kt) / [CoffeeGrinder.kt](../../android/app/src/main/java/com/treasure/illust/CoffeeGrinder.kt) / [CoffeeBean.kt](../../android/app/src/main/java/com/treasure/illust/CoffeeBean.kt) / [WineBottle.kt](../../android/app/src/main/java/com/treasure/illust/WineBottle.kt) / [CocktailGlass.kt](../../android/app/src/main/java/com/treasure/illust/CocktailGlass.kt)），跟现有 Racket / Camera / Lens 同 viewBox + 同 INK 描边风格；(d) `CategoryTemplate.byCategory` 加 Coffee / Wine 两份模板，带 tagline + heroSpecLabels + heroSpecHints；(e) `heroVectorOptionsFor` 给两个新品类返回各自插画候选；(f) Portal 的 DoorwaysGrid 从硬编码 4 格改成 `cats.chunked(2).forEachIndexed` 自适应 6 格 + 罗马数字 V/VI |
| 6 | MigrationTest 自动化 | `core/build.gradle.kts` 加 `testInstrumentationRunner` + androidTest assets srcDir 指向 `core/schemas/` + 三个 androidTest 依赖（room-testing 2.6.1 / runner 1.6.2 / junit 1.2.1）；新建 [`core/src/androidTest/.../MigrationTest.kt`](../../android/core/src/androidTest/java/com/treasure/core/room/MigrationTest.kt) 四个测试：5→6（旧 items 数据保留 + 新表 add_conversations / add_messages 空表存在）、6→7（旧行的 callouts_json 默认 '{}')、5→7 全链、Room runtime 打开 v5 库走完所有 Migration 不抛 |

## 顺手的连带改动

- HeroAvatarPicker 抽到 `ui/components/`，cycle 0009 的 IllustrationPicker 私有实现整段删掉
- HistoryDropdown 限宽放宽到 260-320dp，新加 ✎ / ✕ 之后还能容纳得下
- Portal DoorwaysGrid 现在能自适应任意数量品类（最多 10 个罗马数字）

## 不在这一刀

- Callout 编辑 / 删除（用户已确认是 cycle 0010 留下的尾巴 — 在全屏 viewer 里长按某条已存在的 callout dot 弹菜单 → 编辑 / 删除）
- 云端 STT 兜底（vivo 国行 SR 不可用）
- AI 生成博物馆插画
- Xiaomi MiLM preset base URL 校准
- 多轮 refine 的 image 上下文

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
