# Cycle 0028 · spec

## 1. 可见性过滤穿透到 total / latest / "全部"

### GridViewModel
```kotlin
val visibleIds = visible.map { it.id }.toSet()
val visibleItems = items.filter { it.category in visibleIds }
val filteredItems = if (selectedId == null) visibleItems   // "全部" 只算可见
                    else items.filter { it.category == selectedId }
GridUiState(
    ...
    totalCount = visibleItems.size,   // "全部" chip 数字 + Header N ITEMS
)
```

### PortalViewModel
```kotlin
val visibleIds = visible.map { it.id }.toSet()
val visibleItems = items.filter { it.category in visibleIds }
PortalUiState(
    items = visibleItems,
    totalItems = visibleItems.size,
    ownedCount = visibleItems.count { it.status == OWNED },
    countByCategoryId = visibleItems.groupBy { it.category }.mapValues { it.value.size },
    latestByCategoryId = visible.associate { c ->
        c.id to visibleItems.filter { it.category == c.id }.maxByOrNull { it.acquired }
    },
    latestOverall = visibleItems.maxByOrNull { it.acquired },
)
```

## 2. Portal 视觉调整

### Latest entry section label
- 之前：`✦ Latest entry`，左对齐
- 现在：`✦ Latest entry ✦`，centered = true（跟 `✦ The Rooms ✦` 视觉一致）

### 全空状态
- `visibleCategories.isEmpty()`：替换 DoorwaysGrid 为 `EmptyRoomsHint(onOpenCategoryManager)` — italic "所有分类都被隐藏了 — 没有房间可以走进去。" + terra "去分类管理 →"
- `latestOverall == null`：替换 LatestEntryCard 为 `EmptyLatestHint(onOpenCategoryManager)` — italic "暂时没有可展示的物品" + "去分类管理 →"

### Doorway 渲染
- DoorwayCard 不再接 `latest: Item?`；hero 区永远 `HeroAvatar(item = stubItemFor(info))` 用 `info.heroVector`
- 物品多少不影响 doorway 那张图（仅影响下方 `N pcs` 计数）

## 3. Category.defaultHeroVector

```kotlin
enum class Category(..., val defaultHeroVector: HeroVector) {
    BADMINTON(..., HeroVector.RACKET),
    PHOTO(..., HeroVector.CAMERA_DSLR),
    CARS(..., HeroVector.CAR_SEDAN),
    TECH(..., HeroVector.LAPTOP),
    COFFEE(..., HeroVector.ESPRESSO_MACHINE),
    WINE(..., HeroVector.WINE_BOTTLE);
}
```

`RoomCategoryRepository.toDomain` 内建行 override：
```kotlin
val hv = if (isBuiltIn) {
    Category.entries.firstOrNull { it.id == id }?.defaultHeroVector ?: storedHv
} else storedHv
```

老的 stored 'GENERIC' 不动 — domain 永远见正确值。

## 4. Manager 拖动重设

### Header
- 标题 "分类管理" + 右侧 "+ 新增分类" — 不变
- **删** italic 副标题 "隐藏只是把分类从首页 / 图鉴入口里挪走 ..."
- **删** 底部 [完成] 按钮

### Row
- 左：3 条横纹拖动握把（36dp 触控区，长按 + 拖触发 `detectDragGesturesAfterLongPress`）
- 中：分类中文名 / 英文名小字 / "自定义" terra pill（仅自定义行）
- 右：28dp 触控区 + 10dp 实心红圆点（点击进编辑页），**不再有 "编辑 →" 文字**
- **删** [隐藏 / 显示] outline pill

### 拖动逻辑
- 列表布局：`workVisible`（top）+ DIVIDER（36dp 高，italic "↑ 显示中 · ↓ 已隐藏"）+ `workHidden`
- 每行用 `graphicsLayer.translationY` 定位；被拖动的行加 dragOffsetY，其他行做 make-room shift
- `targetIndex` 由 `(baseY + dragOffsetY)` 除以 rowPx 反算；落在 divider 区域内则二分判断靠哪侧
- 松手时：把 dragIndex 行 move 到 targetIndex；若跨过 divider 同步 toggle hidden
- 一次 `applyReorder(orderedIds, hiddenIds)` 提交给 VM

### VM
```kotlin
fun applyReorder(orderedIds: List<String>, hiddenIds: Set<String>) {
    repo.reorder(orderedIds)
    val current = repo.loadAll().associate { it.id to it.hidden }
    orderedIds.forEach { id ->
        val want = id in hiddenIds
        if (current[id] != want) repo.setHidden(id, want)
    }
}
```

## 5. Editor 顶部插画 + 必填

### Layout
```
‹ 返回     [新建 / 保存]
新增分类 / 编辑 X
italic 提示
─────────────────
       ⬤ 112dp AvatarHero ⬤
─────────────────
"插画 · 必选" / "插画（内建已固定）"
[Hero pill row]   ← 内建时整行半透明、enabled=false
─────────────────
中文名 [____________]   ← 内建 disabled
英文名 [____________]   ← 内建 disabled
─────────────────
显示  [显示] [隐藏]       ← 仅编辑模式
─────────────────
[删除分类]                  ← 仅自定义编辑
```

### State + 校验
```kotlin
var heroVector by remember { mutableStateOf<HeroVector?>(...) }
val canSave = (isBuiltIn || nameZh.isNotBlank()) && heroVector != null
```

- 新增模式（isAdd=true）：heroVector 初始 null → AvatarHero 画 "+ 选张插画" 占位，[新建] 灰；用户选了之后才点亮
- 编辑内建：heroVector 来自 `Category.entries.defaultHeroVector`，picker 行 disabled
- 编辑自定义：heroVector 来自 stored，picker 行 enabled

### AvatarHero composable
- 112dp 圆，paper 背景 + line 边
- heroVector == null → italic placeholder
- 否则用一份兜底 palette 合成 stub Item 喂 `HeroIllustration`

## 6. MainScreen 提升 Manager 状态

```kotlin
var categoryManagerOpen by remember { mutableStateOf(false) }
// ...
PortalRoute(onOpenCategoryManager = { categoryManagerOpen = true })
GridRoute(onOpenCategoryManager = { categoryManagerOpen = true })
// ...
if (categoryManagerOpen) {
    CategoryManager(onClose = { categoryManagerOpen = false })
}
```

## 7. Out of scope

- AI prompt 自定义分类的 hero spec 模板提示
- 死代码清理 / 撤销采用 / WebView headless / 流式输出
- 拖动到 sheet 边缘时自动滚动整个 Manager 抽屉
