# Cycle 0029 · notes

## 文件改动

### 新建
- `app/.../ui/category/CategoryEditorRoute.kt` — 全屏分类编辑路由 + AvatarHero / HeroVectorRow / FieldRow / EditorTextField / PillChip 内置 helpers
- `app/.../ui/search/SearchRoute.kt` — 全屏搜索路由 + SearchItemCard + highlight() + Item.matches()

### 修改
- `app/.../ui/nav/Routes.kt` — 新增 `Search` / `CategoryNew` / `CategoryEditPattern` 常量
- `app/.../ui/nav/TreasureNavHost.kt` — 注册 3 条新 composable；MainScreen 接 4 个 nav 回调（detail / search / addCategory / editCategory）
- `app/.../ui/main/MainScreen.kt` — 4 个 nav 回调透传；新 BackHandler；Manager 抽屉接 `onAddCategory` / `onEditCategory`
- `app/.../ui/grid/GridScreen.kt` — `GridRoute` / `GridScreen` 加 `onOpenSearch`；右上工具栏从单一 Box 改 Row（搜索 icon + 小红点）；新 SearchGlyph Canvas
- `app/.../ui/category/CategoryManager.kt` — 删 inline Editor + Mode sealed；签名加 `onAddCategory` / `onEditCategory`；去掉底部 italic 提示

## 设计取舍

### BackHandler 在 MainScreen 而不是每个 tab

考虑过把 BackHandler 放在 PortalRoute / GridRoute / AddRoute / SettingsRoute 里各自处理。但：
- HorizontalPager 4 个 tab 是同时 attached 的（虽然只渲染当前页），每个里面注册 BackHandler 会出现"最近 onActive 的那个生效"的顺序问题
- 集中在 MainScreen 写一个，看 pagerState.currentPage 就清楚

`enabled = ...` 让 Portal tab 时 BackHandler 不拦截 — 默认行为生效（退出）。

### Manager 改成"List-only"后 Mode sealed 删了

Mode sealed 在 cycle 0028 里区分 List / Edit / Add 三态。cycle 0029 把 Edit / Add 都拆出去，Manager 只剩 List，sealed 没意义了。删 sealed + 把 `when (val m = mode)` 改回顺序代码。少了 ~30 行 boilerplate。

### CategoryEditorRoute 用 own VM instance

`viewModel(factory = CategoryManagerViewModel.Factory)` 在 NavHost.composable 里被调用，会拿到 *route-scoped* VM instance — 跟 MainScreen 里 Manager 用的不是同一个。这样 Editor 自己跑 `vm.all` 也能拿到完整 list（不依赖父页面的 state）。两个 VM instance 共享 Room 数据库（singleton），所以编辑保存 → 仓库 update → 父页面那个 VM 的 observeAll Flow 自动刷新。无需手动同步。

代价：两个 VM 同时存在。但 VM 很轻（只 wrap repo flow），没问题。

### 搜索框 auto focus 用 LaunchedEffect

```kotlin
val focus = remember { FocusRequester() }
LaunchedEffect(Unit) { focus.requestFocus() }
```

进入 SearchRoute 立刻 focus → IME 弹出 → 用户直接打字。返回时 IME 自动收（系统 back 处理）。`imePadding()` 让搜索框不被 IME 遮住。

### SearchGlyph Canvas 手画放大镜

没用 Material icons 库（依赖大、视觉风格跟博物馆调子不搭）。直接 Canvas 画 1 个圆环 + 1 条斜线，stroke 跟着 size 按比例缩放。32dp 触控、16dp 视觉直径。

### highlight 用 AnnotatedString + SpanStyle

最朴素实现：lowercase 找所有 occurrence，每段套 SpanStyle(color = terra, fontWeight = SemiBold)。不动 `Modifier.background` 因为加 background span 在小字号上视觉糙；font weight + 颜色已经足够 "醒目"。

### 搜索结果副标题用 oneLiner 优先

GridScreen.ItemCard 副标题写死 `oneLiner`。SearchItemCard 用 `oneLiner.ifBlank { nickname }` 兜底 — 因为有可能用户搜的是 nickname，但这个 item 没填 oneLiner，那就显示 nickname（且 nickname 高亮）。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（14 MB）

### 手测

1. 装新 APK
2. 在 Portal → 按物理 back → 退出（Portal tab 不拦）
3. 滑到 Grid → 按 back → 回 Portal（不退出）
4. 滑到 Settings → 按 back → 回 Portal
5. Grid 右上：应有 [搜索 icon] [小红点] 两个按钮
6. 点搜索 icon → push 上来 Search 页：搜索框自动 focus + IME 弹出
7. 输入 "yo" → 立刻显示所有 brand/model/nickname 含 "yo" 的 item，"yo" 在标题上 terra 色高亮
8. 输入清空 → 提示"输入关键词 — 立刻看结果"
9. 点 ✕ 清除 → 同上
10. 按 back → 回 Grid
11. 点 Grid 小红点 → Manager 抽屉
12. 点 "+ 新增分类" → Manager 抽屉收起 + push CategoryEditor 全屏页：顶部 EditPageHeader 跟物品 Edit 页同款 + BackArrow
13. 按 back / 左上 BackArrow → 回主屏（Manager 不会重新弹）
14. Manager 抽屉 → 点某行右侧小红点 → 抽屉收起 + push Editor 编辑页
15. Editor 页按 back → 回主屏
