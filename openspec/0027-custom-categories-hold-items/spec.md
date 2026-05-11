# Cycle 0027 · spec

## 1. Domain：Item.category 从 enum 改 String

```kotlin
// Before
data class Item(val category: Category, ...)

// After
data class Item(val category: String, ...)  // "badminton" / "tech" / "custom-uuid-..."
```

`ItemEntity.toDomain` 直接透传 String，不再 `Category.fromId(...)`。`ItemEntity.fromDomain` 也直接 `item.category` 不再 `.id`。`Category` enum 仍然存在（CategoryTemplates / heroVectorOptionsFor 还用它当 key），但只对内建 6 个分类有意义。

## 2. AI: 动态 category 列表

新数据类 `CategoryHint(id: String, nameZh: String, nameEn: String)` 在 `:core` ai 包。

`AiClient.extractItemDraft` 签名加：
```kotlin
suspend fun extractItemDraft(
    text: String,
    imageJpegBytes: ByteArray? = null,
    priorTurns: List<AiTurn> = emptyList(),
    baseline: ItemDraft? = null,
    categoryHints: List<CategoryHint> = emptyList(),  // 新
): Result<ItemDraft>
```

`Prompts.buildSystemWithBaseline(baseline, json, categoryHints)`：
- categoryHints 非空时拼到 system prompt 末尾，格式：
  ```
  [AVAILABLE CATEGORIES — these are the actual ids the user has set up in this app right now. Pick the `category` value from THIS list (id), not the hardcoded six above.]
    badminton  (羽毛球 / Badminton)
    photo      (摄影 / Photography)
    ...
    custom-7f3a  (图书 / Books)
  ```
- 跟在 baseline 段前面（如果 baseline 也非空，两个段都拼）

JSON 工具 schema：
```diff
- "category": { "type": "string", "enum": [...固定6个...] }
+ "category": { "type": "string", "description": "Category id; pick from the list in the system prompt (built-in or user-added)." }
```

AddViewModel.runExtract 调用前：
```kotlin
val hints = categoryRepo.loadAll()
    .filter { !it.hidden }
    .map { CategoryHint(it.id, it.nameZh, it.nameEn) }
```

## 3. UI dropdowns 用 CategoryInfo

`AddPreview(draft, categories, ...)`、`EditScreen(item, categories, ...)` 各自多接一个 `categories: List<CategoryInfo>` 参数。其 InlineDropdown：
- options = categories.filter { !it.hidden }
- selected = categories.firstOrNull { it.id == draft.category / item.category } ?: 伪 CategoryInfo(id=...) 占位
- visibleOptions = selected 在列表里 → list；不在 → list + selected（孤儿 id 也能显示）
- onSelect = { it.id }（不是 it.nameZh）

`applyFieldEdit(PreviewField.Category, value)` 同时容忍 id / nameZh / nameEn 三种匹配方式，匹配不上的 value 也直接当 id 写入。

## 4. HeroAvatarPicker 接 String

`fun HeroAvatarPicker(categoryId: String, ...)` 替换 `category: Category`。内部 `previewItem(v, palette, categoryId)` 把 Item.category 设成 categoryId。

新 helper `heroVectorOptionsForId(id: String): List<HeroVector>` — 命中内建走原 `heroVectorOptionsFor(category)`，未命中（自定义）返回 `HeroVector.entries` 全集。

## 5. 删自定义分类时把 items 重指 tech

`CategoryPrefDao.reassignItemsToTech(id: String)`：
```sql
UPDATE items SET category = 'tech' WHERE category = :id
```

`CategoryRepository.deleteCustom(id)` 先 `reassignItemsToTech(id)`，再 `dao.deleteCustom(id)`。manager 编辑页删除 dialog 文案改：
> 这只是删掉这个分类本身。原本收在这里的物品不会被删 — 它们会被自动重新归到"电子产品"分类下，进图鉴后可手动改类别。

## 6. 不需要 schema 迁移

`ItemEntity.category` 始终是 TEXT 列存字符串 id；cycle 0027 只动了 domain 层的强转逻辑。Room version 仍 v9（cycle 0026 加 category_prefs 那次 bump）。历史 item 升级后照样可读。

## 7. Out of scope

- AI prompt 里 hero spec 示例还是按内建 6 个品类写
- manager 抽屉拖动重排
- CategoryForm.kt / saveManual 死代码清理
- 撤销采用 / WebView headless / 流式输出
