# Cycle 0027 · 自定义分类可装物品

- **状态：** done
- **完成：** 2026-05-11

## 用户指令

> 那你继续吧 0027 做完

即落地 cycle 0026 文档里点名的下一刀候选 #1：**自定义分类能装物品**。

## 上下文

cycle 0026 把分类管理 UI（visibility + 自定义新增 + 编辑 + 删除）一刀做完，但 `Item.category` 还是 [Category](android/core/src/main/java/com/treasure/core/domain/Category.kt) enum 写死的 6 个内建值。结果：用户在 manager 里新建一个 "图书" 分类，能看见、能编辑、能拖、但 AI / 手动录入填不上去，"图书" 永远 0 items。

cycle 0027 的目标：让用户新建的分类真正能挂物品。

## 落地

| # | 改动 | 说明 |
|---|---|---|
| 1 | `Item.category: Category` → `Item.category: String` | 一刀全域改字段类型。enum 还在（CategoryTemplates / heroVectorOptionsFor 内建模板还用它当 key），但 domain 模型只看 String id。`ItemEntity.toDomain` 不再 `Category.fromId(...)` 强转，直接透传字符串 — 自定义 id "custom-uuid-..." 也能 round-trip |
| 2 | AI prompt 喂动态 category 列表 | 新 `CategoryHint(id, nameZh, nameEn)` 数据类，`AiClient.extractItemDraft` 多接一个 `categoryHints: List<CategoryHint>`。`Prompts.buildSystemWithBaseline(baseline, json, categoryHints)` 把 list 拼到 system prompt 末尾，明确告诉 AI 这些是当前用户可选的 id（覆盖前面写死的 6 个内建）。tool 的 JSON Schema 也把 `category.enum` 去掉，改成宽松 `description` 让 AI 自由选 |
| 3 | AddPreview / Edit 的 InlineDropdown 改 CategoryInfo 源 | 不再 `Category.entries`，改成读 `app.categoryRepository.observeAll()` 过掉 hidden 的。selected id 不在列表里（比如自定义分类被删了后留下的 item）→ 用伪 `CategoryInfo(id=item.category, nameZh=item.category)` 占位，dropdown 仍能显示当前值。`onSelect` 直接传 `it.id` |
| 4 | HeroAvatarPicker 接 `categoryId: String` | 之前是 `category: Category`，但内部 only 用来填 preview Item 的 category 字段。换 String 顺手把 import 也清理掉 |
| 5 | `heroVectorOptionsForId(id: String)` 新增 | 命中内建走 `heroVectorOptionsFor(category)`；未命中（自定义）回 `HeroVector.entries` 全套，让用户随便挑插画 |
| 6 | 删除自定义分类时 rehome items | 新 DAO `reassignItemsToTech(id)` — 删之前把名下 items.category 全 UPDATE 成 "tech"。AlertDialog 文案同步改成 "原本收在这里的物品会被自动重新归到 '电子产品' 分类下" |
| 7 | 调用点全域清理 | `item.category.id` → `item.category`；`item.category.nameZh` → repo 查；`item.category == Category.X` → `item.category == "x"`；SeedItems / commitDraft / saveManual / Portal stub item / makeId 全部跟着改 |

## 没做（cycle 0028 候选）

- **AI prompt 的 hero spec 模板提示**还是按内建 6 个品类写示例（如 "badminton racket: 重量 / 平衡点 / 中杆硬度 / 穿线磅数"）。自定义分类没有针对性的 hero spec 提示，AI 全靠自己根据 item 类型挑。如果用户反馈"图书的 hero specs 太离谱"，再补"自定义分类 hero spec 由用户首次入库后用户调整"的循环
- **manager 抽屉拖动重排** — 用户 cycle 0026 没明说要，留着
- **撤销采用** (cycle 0024 留下)
- **CategoryForm.kt / saveManual** 还在但没人调用，下个 cycle 一起清
- **WebView headless / 流式输出**

## 不需要的 schema 迁移

`ItemEntity.category` 一直都是 TEXT 列（cycle 0001 起），实际存的就是字符串 id（"badminton" / "tech" / ...）。**只有 `toDomain` 那一步强转 enum，是 cycle 0027 干掉的**。所以本 cycle 不需要 schema bump — Room version 仍是 v9 (cycle 0026 加 category_prefs 表那次)。历史 item 升级到 cycle 0027 的 APK 后 category 字段照样可读，不会丢数据。

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
