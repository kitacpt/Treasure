# Cycle 0027 · notes

## 文件改动

### Domain
- `core/.../domain/Item.kt` — `category: Category` → `category: String`

### 数据层
- `core/.../room/ItemEntity.kt` — toDomain/fromDomain 透传 String，去 `Category.fromId`
- `core/.../room/CategoryPrefDao.kt` — 新增 `reassignItemsToTech(id)`
- `core/.../repo/CategoryRepository.kt` — `deleteCustom` 先 reassign 再 delete

### AI
- `core/.../ai/AiClient.kt` — `extractItemDraft` 加 `categoryHints` 参数；新 `CategoryHint` 数据类
- `core/.../ai/Prompts.kt` — `buildSystemWithBaseline` 加 `categoryHints` 参数，拼到 prompt；tool schema 去掉 enum 改 description
- `core/.../ai/AnthropicClient.kt` / `OpenAiClient.kt` — `extractItemDraft` 实现接收 categoryHints，buildPayload 透传

### UI
- `app/.../ui/components/HeroAvatarPicker.kt` — `category: Category` → `categoryId: String`，previewItem 内部跟着改
- `app/.../ui/add/CategoryTemplate.kt` — 新增 `heroVectorOptionsForId(id: String)`
- `app/.../ui/add/AddViewModel.kt` — 加 categoryRepo 字段；runExtract 收 hints 传 client；commitDraft 用 String id；applyFieldEdit Category 分支三模式匹配；makeId 接 String；HeroVector import；Factory 多传 categoryRepository
- `app/.../ui/add/AddRoute.kt` — 拉 `categories` flow，传给 AddPreview
- `app/.../ui/add/AddPreview.kt` — 加 `categories: List<CategoryInfo>` 参数；category 由 String 解；InlineDropdown 用 CategoryInfo + onSelect 传 id
- `app/.../ui/add/AddChat.kt` — DraftCtaCard 的 previewItem `category = template.category.id`
- `app/.../ui/edit/EditScreen.kt` — EditRoute 拉 categories；EditScreen 多接 categories；subtitle 查 repo；InlineDropdown 改 CategoryInfo 源；HeroAvatarPicker `categoryId = category`；`heroVectorOptionsForId(category)`
- `app/.../ui/grid/GridViewModel.kt` — `it.category.id == ...` → `it.category == ...`
- `app/.../ui/portal/PortalViewModel.kt` — 同上
- `app/.../ui/portal/PortalScreen.kt` — stubItemFor `category = info.id`

### Seed
- `core/.../seed/SeedItems.kt` — `category = Category.X` → `category = "x"`；删 Category import

## 设计取舍

### 为什么 Category enum 留着

最初想干脆把 `Category` enum 删了，全用 CategoryRepository。但：
1. `CategoryTemplates` 是按 enum 当 map key 的；6 个内建模板（hero spec labels / hints / palette）跟 enum 一对一。删 enum 就要把模板改成按字符串 id 索引 — 重构 surface 太大且没价值
2. `heroVectorOptionsFor(Category)` 同理 — 内建模板逻辑很自然按 enum 写
3. AI prompt 的 hero spec 示例（"badminton racket: 重量 / 平衡点 / 中杆硬度 / 穿线磅数"）也是按内建分类列的

结论：**enum 是"内建分类的模板表的 key"**，不是 domain 模型。Item.category 不绑 enum，而 CategoryTemplates / heroVectorOptionsFor 这种"模板查询"还按 enum 走 — 各司其职。

### AI prompt 既保留写死的 6 个 example，又拼动态 list

为什么不直接删掉 SYSTEM_PROMPT 里的 6 个 example？因为：
1. 6 个内建 example 解释了**怎么挑** id（语义提示），对模型友好
2. 动态 list 在尾部，确切告诉模型 **当前哪些 id 可选**

两者互补不冲突。模型能从 system prompt 后段看到 "ok 现在还有 custom-7f3a (图书) 可选"，于是合理把书类 item 落到图书分类下。如果只删 6 个 example，AI 缺少语义启发。

### deleteCustom 把 items 重指 tech 而不是不准删

考虑过：如果自定义分类下有 items，禁止删除（要求用户先迁移）。但增加摩擦：用户得自己去 Detail / Edit 一件件改 category。自动重指到 tech 兜底是更平滑的选择 — 用户在 dialog 里被告知"会归到电子产品"，可接受。

万一 tech 自己也被 hide 了？UI 上还能看见（Grid 的 visibleCategories 是过滤 hidden 的，但 chip "全部" 永远展示所有 items 不分 category，所以 item 不会消失，只是 chip 行里没有 tech）。等用户去 manager 取消 tech 的隐藏，item 又回来了。语义上 acceptable。

### 孤儿 id 的兜底渲染

如果 item.category 是个已经被删的 custom-id（理论上 cycle 0027 后不会发生，因为 deleteCustom 会 reassign；但万一升级路径有 bug，或者用户用工具直接改了数据库）：
- EditScreen / AddPreview 的 InlineDropdown：用伪 `CategoryInfo(id=item.category, nameZh=item.category)` 占位，可让用户再选一个合法 id
- Portal/Grid chips：visibleCategories 不含它，item 不会上 chip 但 "全部" 还看得见
- Detail / 卡片标题用 categoryRepo 查不到时 fallback 显示 raw id（如 "custom-7f3a"）

总之：孤儿 id 不会 crash，UI 退化但 item 不丢。

### EXTRACT_TOOL_PARAMETERS 去掉 enum 后 AI 会不会乱选

`enum: [...]` 是 JSON Schema 的严格约束，模型有时候 violates；但去掉后模型按 system prompt 提示选。已知模型（Claude Haiku / Opus, GPT-4o, Kimi）在 forced tool_choice + 明确 "pick from THIS list" 提示下都会按列表选。如果将来某模型乱传 id（比如自创 "books-001"），UI 层会把它当孤儿处理 — 不 crash 但需要用户在 Refine 页手动改 category。可接受。

### Migration 没改

Item.category 列一直是 TEXT，老数据存的是字符串 id ("badminton" / "photo" 等)。本 cycle 只改 domain 层的 enum 强转逻辑，没动 schema。所以 Room version 不动，schemas/9.json 不变。从 cycle 0026 升级到 0027 的用户：升级时不跑 migration，老数据 round-trip 没问题。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（14 MB）

### 手测

1. 装新 APK（如果之前装过 cycle 0026 的 APK，category_prefs 表的种子 6 行还在）
2. 进图鉴页 → 右上小红点 → manager → + 新增分类 → 填中文名"图书"、英文名"Books"、随便挑个插画 → [新建]
3. 回主屏：Portal 应看到 "图书" doorway，Grid chip 行应看到 "图书 0"
4. 切到录入页：跟 AI 说"我有一本《时间简史》霍金著"
5. AI 输出 DraftCta：category 应自动是 "custom-..." → 点 [采用]
6. 点 [手动] 进 Refine 页：subtitle 应显示"图书"，品类下拉里有 "图书" 选项
7. 点 [确认收入] → 弹 dialog "收入《时间简史》？" → 点 [收入]
8. Grid "图书" chip 应 +1，Portal "图书" doorway 应 +1 + 显示这本书的插画
9. 回 manager → 编辑 "图书" → [删除分类] → AlertDialog "删除 图书？..." → [删除]
10. Grid / Portal：图书 chip 消失；之前那本《时间简史》应该跑到 "电子产品" 分类里（数据没丢）
11. 进 Detail / Edit 那本书：subtitle 显示"电子产品"，品类下拉默认选中"电子产品"，可再改回任意其他分类
