# Cycle 0029 · spec

## 1. BackHandler

```kotlin
// MainScreen.kt
BackHandler(
    enabled = categoryManagerOpen || pagerState.currentPage != PAGE_PORTAL,
) {
    when {
        categoryManagerOpen -> categoryManagerOpen = false
        pagerState.currentPage != PAGE_PORTAL ->
            scope.launch { pagerState.animateScrollToPage(PAGE_PORTAL) }
    }
}
```

行为：
- Portal tab + Manager 关闭 → 默认行为（退出）
- 任何非 Portal tab → 返回到 Portal
- Manager 抽屉打开 → 收抽屉，留在当前 tab

`Detail` / `Edit` / `Search` / `CategoryEditor` 走 NavHost.popBackStack，不依赖 MainScreen 的 BackHandler。

## 2. Manager 简化

- 去掉底部 italic "长按 ≡ 拖动 ..."
- 内嵌的 `CategoryEditor` + helper 全删（移到 `CategoryEditorRoute.kt`）
- `CategoryManager` 签名加 `onAddCategory: () -> Unit` / `onEditCategory: (CategoryInfo) -> Unit`，分别在用户点 "+ 新增" / 行右侧小红点时 `onClose()` 收抽屉再 fire

## 3. CategoryEditor 全屏路由

新文件 `ui/category/CategoryEditorRoute.kt`：

```kotlin
@Composable
fun CategoryEditorRoute(
    categoryId: String?,  // null = 新建
    onBack: () -> Unit,
    vm: CategoryManagerViewModel = viewModel(...),
)
```

布局：
- 状态栏 + ime 边距
- `EditPageHeader(title = "Edit"/"New", subtitle = nameZh/"新增分类", leading = BackArrow, trailing = [保存/新建])`
- italic 提示（内建锁定 / 自定义必填）
- AvatarHero 112dp 圆
- 插画 picker（内建 enabled=false）
- 中文 / 英文 LabeledField（内建 disabled）
- 显示 toggle（仅编辑模式）
- 删除（仅自定义编辑）

路由 + 入口：
- `Routes.CategoryNew = "category/new"`
- `Routes.CategoryEditPattern = "category/edit/{categoryId}"`
- NavHost 注册两条 composable，都用 `CategoryEditorRoute(categoryId, onBack)`
- `MainScreen(onAddCategory, onEditCategory)` 接 nav 回调

## 4. Search 路由

新文件 `ui/search/SearchRoute.kt`：

```kotlin
@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
)
```

布局：
- 状态栏 + ime 边距
- 顶行：[‹ BackArrow] [圆角搜索框 + ✕ 清除] — 搜索框 LaunchedEffect requestFocus 自动弹 IME
- italic 结果提示 "${results.size} 条结果" / "没找到匹配项" / "输入关键词..."
- LazyVerticalGrid 2 列同 GridScreen.ItemCard 排版
- 每个 SearchItemCard：HeroAvatar 大图 + 高亮标题 + 高亮副标题

匹配：
```kotlin
private fun Item.matches(q: String): Boolean {
    val needle = q.lowercase()
    return brand.lowercase().contains(needle) ||
        model.lowercase().contains(needle) ||
        nickname.lowercase().contains(needle)
}
```

高亮：
```kotlin
private fun highlight(haystack, needle, accent): AnnotatedString {
    // 遍历 lowercased haystack 找 needle 所有出现位置
    // 命中段套 SpanStyle(color = accent, fontWeight = SemiBold)
}
```

入口：
- `Routes.Search = "search"`
- NavHost 注册 composable
- `MainScreen(onOpenSearch)` 接 nav callback
- `GridRoute(onOpenSearch)` / `GridScreen(onOpenSearch)` 透传

## 5. Grid 右上工具栏

```
之前： [小红点]                       (Box, align TopEnd)
之后： [搜索 icon] [小红点]            (Row, align TopEnd)
```

两个 32dp 触控圆，padding(top=24dp, end=16dp)。

搜索 icon 用 Canvas 画简笔放大镜 — 圆环 + 短斜线手柄，颜色 colors.ink，stroke 宽度按尺寸 *0.10。

## 6. Out of scope

- Tab 历史栈
- 搜索 specs / oneLiner / category
- 拼音 / 模糊
- 结果分组 / 排序选项
