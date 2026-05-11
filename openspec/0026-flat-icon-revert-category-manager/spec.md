# Cycle 0026 · spec

## 1. 图标回退

`ic_launcher_foreground.xml` = `git show 393b1ca:.../ic_launcher_foreground.xml`。具体内容是 cycle 0013 那版：外圈 23px 圆环（不再椭圆）+ 上下高光/暗弧 + 内壁 bevel + 顶/底 paper-color rune + 两侧 tick + 黑色 hairline。

## 2. Schema v9

新表：

```
CREATE TABLE category_prefs (
    id TEXT NOT NULL PRIMARY KEY,
    built_in INTEGER NOT NULL,
    name_zh TEXT NOT NULL,
    name_en TEXT NOT NULL,
    hero_vector TEXT NOT NULL,
    hidden INTEGER NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at INTEGER NOT NULL
);
```

Migration_8_9 同时种子 6 个内建行（id 用 Category.entries 的 id，sort_order = enum 顺序 0..5，hidden=0，hero_vector='GENERIC'，name_zh / name_en 是冗余缓存值方便 manager 抽屉一次读完）。

Room version `8 → 9`，schemas/9.json 已 export，Migration 已加进 Migrations.ALL。

## 3. CategoryRepository

```kotlin
interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryInfo>>     // 排序按 sort_order
    suspend fun loadAll(): List<CategoryInfo>
    suspend fun setHidden(id: String, hidden: Boolean)
    suspend fun setHeroVector(id: String, heroVector: HeroVector)
    suspend fun updateCustom(id: String, nameZh: String, nameEn: String, heroVector: HeroVector)
    suspend fun deleteCustom(id: String)
    suspend fun addCustom(nameZh: String, nameEn: String, heroVector: HeroVector): String  // id
    suspend fun reorder(orderedIds: List<String>)
}
```

`CategoryInfo`：

```kotlin
data class CategoryInfo(
    val id: String, val nameZh: String, val nameEn: String,
    val heroVector: HeroVector, val hidden: Boolean,
    val sortOrder: Int, val isBuiltIn: Boolean,
)
```

`TreasureApp.categoryRepository` 在 `onCreate` 里 wire 上。

## 4. Grid 入口 + Manager 抽屉

GridScreen 右上 28dp 透明圆触控区，中央 12dp 红点 `#C5392E`，距状态栏顶 28dp、右 22dp。点击 → `managerOpen = true`。

Manager（ModalBottomSheet，paper 背景）：
1. 头：左 "分类管理"，右 "+ 新增分类"
2. italic 灰小字 "隐藏只是把分类从首页 / 图鉴入口里挪走，不会删数据"
3. SectionLabel "显示中 · N"
4. 每行：[左] nameZh / italic nameEn（"自定义" terra pill 仅自定义）/ [右] [隐藏] outline pill (一键) + [编辑 →] terra
5. DividerHidden（0.5dp line）
6. SectionLabel "已隐藏 · N"
7. 每行同上，toggle 文案改 "显示"
8. 底部 "完成" 居中

抽屉内部 mode 切换（List / Edit / Add），用 ModalBottomSheet 上 sealed `Mode` state；不嵌套二级 sheet。

## 5. Editor 子页

进入：点行的"编辑 →" → `Mode.Edit(info)`；点"+ 新增分类" → `Mode.Add`。

布局：
- 头：左 "‹ 返回" → 回 List；右 "保存" / "新建" terra
- 标题 "编辑 X" / "新增分类"
- 内建提示 italic：内建分类只能换插画 / 改显示状态
- 中文名 FieldRow（LabeledField 同款样式，内建 disabled = sub 灰）
- 英文名 同上
- 插画 picker：横滚 LazyRow，每个 HeroVector 一个 PillChip + 中文 label
- 显示状态（仅编辑模式）：[显示] [隐藏] 双 PillChip
- 删除按钮（仅自定义 + 编辑模式）：terra outline 全宽按钮 → AlertDialog 二次确认

AlertDialog：标题 "删除 {nameZh}？" + 文案 "这只是删掉这个分类本身。已经收在这个分类下的物品不会被删，但它们会归到一个空 id（要重新指派分类）。" + [删除] [取消]，沿用 cycle 0025 确认对话框样式。

## 6. Portal + Grid 改造

### GridUiState

```kotlin
data class GridUiState(
    val currentCategoryId: String? = null,     // 改 String，可指内建或自定义
    val itemsInCategory: List<Item>,
    val countByCategoryId: Map<String, Int>,
    val totalCount: Int,
    val visibleCategories: List<CategoryInfo>, // 渲染 chip 用
)
```

GridScreen.CategoryChips：iterate `state.visibleCategories`，"全部" + 每个 visible 分类一个 chip。

### PortalUiState

```kotlin
data class PortalUiState(
    val visibleCategories: List<CategoryInfo>,
    val countByCategoryId: Map<String, Int>,
    val latestByCategoryId: Map<String, Item?>,
    // ... 其他原有字段
)
```

DoorwaysGrid 现在 chunk(2) `state.visibleCategories`。每张 DoorwayCard 接 CategoryInfo，stub item 用 CategoryInfo.heroVector + 内建模板 palette（若 id 命中内建 enum）或一份兜底 generic palette。

### MainScreen

`onEnterCategory: (String) -> Unit` — 直接传 id，不再 Category enum。

## 7. Out of scope

- 自定义分类装物品（要 Item.category 全域改 String，cycle 0027 候选）
- 拖动重排 manager 抽屉内行（用户未明说）
- 自定义分类的 palette 调色（先继承 generic）
- cycle 0024 已记的死代码 / 撤销采用 / WebView headless
