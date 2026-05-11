# Cycle 0030 · spec

## 1. Portal GrandTitle 还原 cycle 0023

```kotlin
@Composable
private fun GrandTitle() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Treasure", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "a private cabinet of things owned, used, & remembered",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
        )
    }
}
```

底部 Ornament 也补回（PortalScreen LazyColumn 最后 `Spacer(28.dp) + Ornament(padding 24dp)`）。

## 2. 隐藏过滤穿透 Search + Grid 边角

### SearchRoute
```kotlin
val categories by app.categoryRepository.observeAll()
    .collectAsState(initial = emptyList())
val visibleIds = remember(categories) {
    categories.filter { !it.hidden }.map { it.id }.toSet()
}
val results = items.filter { it.category in visibleIds && it.matches(q) }
```

### GridViewModel — 当前 chip 的分类被隐藏后 fallback null
```kotlin
val effectiveSelectedId = if (selectedId != null && selectedId !in visibleIds) null
                          else selectedId
val filteredItems = if (effectiveSelectedId == null) visibleItems
                    else items.filter { it.category == effectiveSelectedId }
GridUiState(currentCategoryId = effectiveSelectedId, ...)
```

## 3. Manager 拖动 divider 当 row-height 块

### 坐标域
- `combined = workVisible + workHidden`（N 项）
- `visualSlot ∈ [0..N]`，N+1 个 slot 含 divider
- divider 占 `visualSlot = workVisible.size`
- `combinedToVisual(i): if i < visibleCount then i else i + 1`
- 容器总高 = `ROW_HEIGHT * (N + 1)`

### Drag end
```kotlin
val originVisual = combinedToVisual(dragIndex)
val draggedCenterY = originVisual * rowPx + dragOffsetY + rowPx / 2f
val targetVisualSlot = (draggedCenterY / rowPx).toInt().coerceIn(0, N)
// 落在 divider slot 时按拖动方向 snap (commitDrag 内部处理)
```

### `commitDrag`
1. 去掉 dragging 行：`withoutDragged`, `newVisibleCountWithoutDragged`
2. `adjustedTarget`：origin < target 时减 1 补偿
3. 根据 adjustedTarget vs newVisibleCount 决定 newCombinedIdx + newIsVisible
4. divider slot：origin < target → 落 hidden 首位；反向 → 落 visible 末位
5. 插回 + 算 newVisibleCount

### Make-room shift
- 同段拖动：介于 origin/target 的非 divider 行让 ±rowPx
- 跨段：divider 视为占位，不让

## 4. Schema v10 + heroPhotoPath

### Migration
```sql
ALTER TABLE category_prefs ADD COLUMN hero_photo_path TEXT
```

### Domain
```kotlin
data class CategoryInfo(
    ...
    val heroPhotoPath: String? = null,  // 新
)
```

### Repository
```kotlin
suspend fun setHeroPhotoPath(id: String, path: String?)
```

### VM 新方法
- `pickHeroPhoto(targetId: String?, uri: Uri, onSaved: (String) -> Unit)`
  - 复制图到 `filesDir/category-photos/<id|"tmp">/<uuid>.jpg`
  - targetId 非空 → 直接 `repo.setHeroPhotoPath`
  - targetId null → 暂存到 `pendingPhotoForNew`（新建模式）
- `clearHeroPhoto(targetId)` — 删本地图 + 清字段
- `addCustomWithPhoto(nameZh, nameEn, photoPath, onCreated)` — 创建 row + 把 tmp 图迁到正式目录 + 写 hero_photo_path

## 5. Editor UI

### 头像 (`AvatarHero`)
- photoPath 非空 → AsyncImage 占满圆形头像
- photoPath 空 + fallbackHeroVector 非空 (内建) → HeroIllustration
- 都空 (自定义新建) → italic "+ 从相册选" 占位

### Picker
```
代表图 · 必选 / 代表图
[+ 从相册选 / 换一张]   [清除]
```
- [+ 从相册选 / 换一张]：`PickVisualMedia` launcher
- [清除]：仅 photoPath 非空时显示；自定义清完之后头像变 italic 占位

### canSave
| 模式 | canSave |
|---|---|
| 内建编辑 | `true`（光改 hidden 也行） |
| 自定义编辑 | `nameZh.isNotBlank()` |
| 自定义新建 | `nameZh.isNotBlank() && currentPhoto != null` |

### onSaveBuiltIn / onSaveCustom 签名
之前内建也传 heroVector → 现在内建只回 hidden（heroVector 不再可改）；自定义回 `(nameZh, nameEn, hidden, pendingPhotoPath)`，photoPath 用 VM 内部状态在新建 mode 配 `addCustomWithPhoto`。

## 6. Portal doorway 用 photo 覆盖线描

```kotlin
private fun stubItemFor(info: CategoryInfo): Item = Item(
    ...
    heroVector = info.heroVector,
    avatarPhotoPath = info.heroPhotoPath,  // 新加
    ...
)
```

HeroAvatar 已经在 cycle 0016 起优先用 avatarPhotoPath。

## 7. Out of scope

- 新建 tmp 文件 GC
- 内建 enum 编辑器（仍走 enum.defaultHeroVector，没用户编辑入口）
- 死代码清理 / 撤销采用 / WebView headless / 流式输出
