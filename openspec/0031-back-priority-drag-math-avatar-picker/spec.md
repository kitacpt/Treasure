# Cycle 0031 · spec

## 1. 返回栈优先级

### 1.1 全局规则（保留）

`MainScreen` 顶层 `BackHandler`（cycle 0029）：

```
enabled = categoryManagerOpen || pagerState.currentPage != PAGE_PORTAL
when {
    categoryManagerOpen -> categoryManagerOpen = false
    pagerState.currentPage != PAGE_PORTAL ->
        scope.launch { pagerState.animateScrollToPage(PAGE_PORTAL) }
}
```

`CategoryManager` 抽屉打开时一律由这里收。Grid / Add / Settings 在没局部 handler 接管时回首页。Portal 让默认行为接管（= 退出应用）。

### 1.2 录入页 [AddRoute]

进入条件：composition 内（pager 滑到 Add tab 时）。

```
BackHandler(enabled = photoPreview != null || historyOpen || mode == AddMode.Preview) {
    when {
        photoPreview != null -> photoPreview = null
        historyOpen -> historyOpen = false
        mode == AddMode.Preview -> mode = AddMode.Chat
    }
}
```

LIFO 注册比 MainScreen 后 → 优先吃。enabled 跟着内部 state 联动，全闭合时 back 落到 MainScreen → 回首页。

### 1.3 设置页 [SettingsScreen]

```
BackHandler(enabled = state.editorOpen) { onCloseEditor() }
```

AI 配置抽屉是 `AnimatedVisibility` 渲染层（不是 `ModalBottomSheet`），没有 dispatcher 自动处理。开着时 back 关抽屉，闭合时让 MainScreen 接管。

### 1.4 物品详情页 [DetailScreen]

```
fullscreenIndex?.let { idx ->
    BackHandler { fullscreenIndex = null }
    FullscreenPhotoViewer(...)
}
```

在全屏 viewer 开启的分支内才 register handler。viewer 关掉后 handler 自然 disposal，back 落回 NavHost → 弹回 Main。

### 1.5 NavHost push routes

`Detail` / `Edit` / `Search` / `CategoryNew` / `CategoryEdit` 五条 push 路由都用 `nav.popBackStack()` 作为 onBack。NavHost 默认 back 行为已经是 popBackStack — 不需要额外 BackHandler。

## 2. 分类管理拖动数学

### 2.1 状态

```kotlin
var workVisible by remember(all) { mutableStateOf(all.filter { !it.hidden }) }
var workHidden  by remember(all) { mutableStateOf(all.filter { it.hidden }) }
var dragIndex by remember { mutableStateOf(-1) }
var dragOffsetY by remember { mutableStateOf(0f) }

val combined = workVisible + workHidden
val visibleCount = workVisible.size
val totalSlots = combined.size + 1  // +1 给 divider
fun combinedToVisual(i: Int): Int = if (i < visibleCount) i else i + 1
```

### 2.2 用户指尖中心 → targetVisualSlot

```kotlin
val originVisual = if (dragIndex < 0) -1 else combinedToVisual(dragIndex)
val targetVisualSlot = if (dragIndex < 0) -1 else {
    val centerY = originVisual * rowPx + dragOffsetY + rowPx / 2f
    (centerY / rowPx).toInt().coerceIn(0, totalSlots - 1)
}
```

### 2.3 预览终态布局

```kotlin
val originIsVisible = dragIndex in 0 until visibleCount
val previewTargetIsVisible = when {
    dragIndex < 0 -> originIsVisible
    targetVisualSlot < visibleCount -> true
    targetVisualSlot > visibleCount -> false
    else -> !originIsVisible  // 拖到 divider 槽 → 跨段去对侧
}
val previewNewVis = when {
    dragIndex < 0 -> visibleCount
    originIsVisible && previewTargetIsVisible -> visibleCount
    originIsVisible && !previewTargetIsVisible -> visibleCount - 1
    !originIsVisible && previewTargetIsVisible -> visibleCount + 1
    else -> visibleCount
}
val previewNewCombinedIdx = if (dragIndex < 0) -1 else {
    val raw = if (previewTargetIsVisible) targetVisualSlot else targetVisualSlot - 1
    raw.coerceIn(0, combined.size - 1)
}
```

### 2.4 每行的 translateY（拖动期间）

```kotlin
val translateY = when {
    isDragging -> combinedToVisual(idx) * rowPx + dragOffsetY  // 跟指
    dragIndex < 0 -> combinedToVisual(idx) * rowPx              // 静态
    else -> {
        val newI = if (idx < dragIndex) idx else idx - 1                       // combined' idx
        val finalI = if (newI < previewNewCombinedIdx) newI else newI + 1       // newCombined idx
        val newVisual = if (finalI < previewNewVis) finalI else finalI + 1      // 跳过 divider
        newVisual * rowPx
    }
}
```

### 2.5 Divider 的 translateY

```kotlin
val dividerTranslate = (if (dragIndex < 0) visibleCount else previewNewVis) * rowPx
```

跨段拖动时 divider 视觉滑到新位置，保证它始终在 visible / hidden 段交界处。

### 2.6 commitDrag

```kotlin
private fun commitDrag(
    combined: List<CategoryInfo>,
    visibleCount: Int,
    dragIndex: Int,
    targetVisualSlot: Int,
    onApply: (List<CategoryInfo>, Int) -> Unit,
) {
    if (dragIndex < 0 || targetVisualSlot < 0) return
    val originVisual = if (dragIndex < visibleCount) dragIndex else dragIndex + 1
    if (targetVisualSlot == originVisual) return

    val originIsVisible = dragIndex < visibleCount
    val targetIsVisible = when {
        targetVisualSlot < visibleCount -> true
        targetVisualSlot > visibleCount -> false
        else -> !originIsVisible
    }
    val newVisibleCount = when {
        originIsVisible && targetIsVisible -> visibleCount
        originIsVisible && !targetIsVisible -> visibleCount - 1
        !originIsVisible && targetIsVisible -> visibleCount + 1
        else -> visibleCount
    }
    val newCombinedIdx = (if (targetIsVisible) targetVisualSlot else targetVisualSlot - 1)
        .coerceIn(0, combined.size - 1)

    val withoutDragged = combined.toMutableList().also { it.removeAt(dragIndex) }
    val newCombined = withoutDragged.toMutableList().also {
        it.add(newCombinedIdx, combined[dragIndex])
    }
    onApply(newCombined, newVisibleCount)
}
```

渲染和 commit 都用同一套 `targetIsVisible / newVis / newCombinedIdx` 公式 → 拖动手感 = 松手结果。

## 3. 分类编辑插画选择

### 3.1 复用 `HeroAvatarPicker`

```kotlin
HeroAvatarPicker(
    categoryId = initial?.id ?: "category-new",
    palette = DefaultPalette,            // listOf("#0e0e0e", "#a47836", "#e8e2d4", "#5a5a5a")
    options = vectorOptions,
    selected = heroVector,
    onSelect = { v ->
        heroVector = v
        if (currentPhoto != null) {
            vm.clearHeroPhoto(initial?.id)
            photoTick++
        }
        if (!isBuiltIn && !isAdd) {
            vm.saveHeroVectorOnly(initial!!.id, v)
        }
    },
    photoOptions = listOfNotNull(currentPhoto),
    selectedPhoto = currentPhoto,
    onSelectPhoto = { /* 单张，no-op */ },
    onTakePhoto = null,
    onPickPhotos = { pickPhoto.launch(...) },
    onRemovePhoto = { vm.clearHeroPhoto(initial?.id); photoTick++ },
)
```

`vectorOptions` 的来源：

| 模式 | options |
|---|---|
| 内建编辑 | `listOf(initial!!.heroVector)`（= `Category.defaultHeroVector`） |
| 自定义新建 | `HeroVector.entries`（全集） |
| 自定义编辑 | `heroVectorOptionsForId(initial!!.id)` |

### 3.2 canSave

```
isBuiltIn  → true
otherwise  → nameZh.isNotBlank()
```

Photo 不再强制（cycle 0030 加的"自定义新建必须 photo"约束放宽）。

### 3.3 commitSave

```
isBuiltIn:    if hidden 变了 → setHidden; onBack
isAdd:        if photo → addCustomWithPhoto(name, nameEn, photo);
              else      → addCustom(name, nameEn, heroVector)
otherwise:    saveCustom(id, name, nameEn, heroVector);
              if hidden 变了 → setHidden;
              onBack
```

### 3.4 Photo 写入仍 eager

`pickHeroPhoto` 和 `clearHeroPhoto` 在编辑模式都立刻写 DB（cycle 0030 行为保留）。新建模式仍走 `pendingPhotoForNew` 暂存。

## 4. 文件改动

| 文件 | 改动 |
|---|---|
| `app/.../ui/main/MainScreen.kt` | 无（保留 cycle 0029 BackHandler） |
| `app/.../ui/add/AddRoute.kt` | 加 BackHandler（photoPreview / historyOpen / mode == Preview） |
| `app/.../ui/settings/SettingsScreen.kt` | 加 BackHandler（state.editorOpen） |
| `app/.../ui/detail/DetailScreen.kt` | 在 fullscreenIndex?.let 块内加 BackHandler |
| `app/.../ui/category/CategoryManager.kt` | 重写 `CategoryList` 渲染与 `commitDrag`；删除 `computeShift` 函数 |
| `app/.../ui/category/CategoryEditorRoute.kt` | 整体重写：用 HeroAvatarPicker 替换 AvatarHero + 按钮组；放宽 canSave；新增 saveHeroVectorOnly 调用 |

无 schema 改动。

## 5. 不在范围

- 撤销 AI 提案
- 死代码清理
- Settings 的 thinking model 重排
- Detail 抽屉 back 处理（拖动 BottomSheet）
