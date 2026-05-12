# Cycle 0031 · notes

## 文件改动汇总

```
app/.../ui/add/AddRoute.kt                      +13 行（局部 BackHandler）
app/.../ui/settings/SettingsScreen.kt           + 6 行（局部 BackHandler）
app/.../ui/detail/DetailScreen.kt               + 2 行（局部 BackHandler）
app/.../ui/category/CategoryManager.kt          重写 ~120 行（拖动渲染 + commit + 删 computeShift）
app/.../ui/category/CategoryEditorRoute.kt      整体重写 ~370 行
agent.md                                         加 cycle 0031 行 + 历史条目；状态行更新
openspec/0031-.../proposal.md|spec.md|notes.md  新增
```

## 调试要点

### Drag 数学，再来一次

Cycle 0030 的 `adjustedTarget = if (origin < target) target - 1` 看起来是补偿"removeAt 让后面 idx 减 1"，但实际上目标 visualSlot 的 reference frame 是 *最终新布局*，不是 *移除后中间态*。两者错位的根本：

- 中间态：combined' = combined - origin，size N-1，divider 仍在 V (origin visible) 或 V (origin hidden)
- 终态：combined + 移动 = newCombined，size N，divider 在 newVis

用户指尖在终态布局里的 visualSlot 才是他要的。所以正确做法是：

1. 决定 newVis（基于 origin / target 两段身份）
2. 推回拖动行的终态 combined idx：visualSlot t 反推 idx = if (t < newVis) t else t - 1
3. 把这个 idx 当作 `withoutDragged.add(idx, draggingRow)` 的参数

这一套不需要"补偿 origin removal"，因为 add(idx) 自然把其它行后移。

### 渲染期间的"newVisualSlot" 计算

非拖动行在终态 newCombined 里的 idx：

```
newI       = if (idx < dragIndex) idx else idx - 1        // combined' 里
finalI     = if (newI < previewNewCombinedIdx) newI else newI + 1  // newCombined 里
newVisual  = if (finalI < previewNewVis) finalI else finalI + 1    // 跳 divider
```

第二步的 `+1` 是因为 dragging 行被插回 combined' 时，所有 idx ≥ previewNewCombinedIdx 的行后移 1 位。第三步的 `+1` 是因为 newVisual 跳过 divider 这个 visualSlot。

### HeroAvatarPicker 复用零修改

`HeroAvatarPicker` 接受 `onTakePhoto: (() -> Unit)?` — null 时不渲染 "📷 拍照" chip。所以分类编辑只传 `onPickPhotos = picker.launch` 即可。`onSelectPhoto` 给一个 no-op lambda（单张 photo 时点击 == 已选）。`photoOptions = listOfNotNull(currentPhoto)` 保证仓库里没图时不渲染空圆。

`onRemovePhoto` 触发 picker 自己的删除确认 AlertDialog（长按 photo 小圆触发），不要 caller 额外做。

### 内建分类的"线描"选项

`heroVectorOptionsFor(builtInCategory)` 通常返回 2-5 个候选（cycle 0011 的 CategoryTemplate 里写的）。但是 cycle 0028 起内建的 heroVector 一律被 repo 强制写成 `Category.defaultHeroVector`，所以即使用户在 picker 里点其它 vector，DB 读回来还是 default。

为避免"用户点了 LENS_PRIME 但实际显示还是 CAMERA_DSLR"这种迷惑感，cycle 0031 内建分类的 `vectorOptions = listOf(initial.heroVector)` —— 只放 default 这一个 tile。用户能看到的就是当前显示的，没歧义。

### 自定义编辑的 vector 写入 eager

cycle 0030 删了 HeroVectorRow 时把 `saveHeroVectorOnly` 标成死代码。cycle 0031 重新启用它给自定义编辑的"挑 vector"动作用：

```kotlin
if (!isBuiltIn && !isAdd) {
    vm.saveHeroVectorOnly(initial!!.id, v)
}
```

这样用户在 picker 里点 vector → DB 立刻更新 → all StateFlow 推新 CategoryInfo 进来 → 头像 recompose。如果只 update 本地 state 不写 DB，用户离开页面不点 [保存]，挑的 vector 就丢了。Photo 也是同样 eager，行为对齐。

### canSave 放宽对老 row 兼容性

自定义新建的 row 现在可能没 photo 但有 vector（GENERIC 或用户挑的）。Portal `stubItemFor` 一直就是"if photo then photo else heroVector 线描"，所以新建无 photo 的 doorway 显示 vector 线描 — 没破坏现有展示。

### 死代码：CategoryEditorRoute 删除的 helper

旧版 `CategoryEditorRoute.kt` 里有：

- `AvatarHero(photoPath, fallbackHeroVector)` 私有 composable
- 旧的 `onSaveBuiltIn` / `onSaveCustom` 回调签名

cycle 0031 整体重写，这些都被 HeroAvatarPicker + 内联 `commitSave()` 取代。没有外部依赖。

## 设计取舍

### MainScreen 的 BackHandler 没改

`pagerState.currentPage != PAGE_PORTAL` 触发回首页这条逻辑不动。所有"应该在调用方"的需求都通过加局部 handler 实现 LIFO 优先级，更精确：

- 优势：每层 handler 跟自己的 state 联动，state 一变 enabled 自动跟着变 — 不需要在 MainScreen 顶层维护"哪些抽屉 / 模式开着"的全局表
- 代价：每加一个新的"非 push 但可暂态"的局部 UI（如未来加的某个 inline 抽屉），都得记得自己加 BackHandler。但这是 Compose 默认习惯，不算坏

### 拖动数学：渲染与 commit 共用一套公式

公式拆出来 `previewTargetIsVisible / previewNewVis / previewNewCombinedIdx` 三个值，渲染用它们摆每行位置，`commitDrag` 用同样输入再算一次最终 newCombined。一致性保证视觉不"骗"用户。

公式有点重复，但拆成 helper 会需要在 Composable 内调用并 hoist state — 没明显收益。保留行内。

### 自定义编辑 vector eager vs 保存时一次写

Photo 已经是 eager（pickHeroPhoto 写 DB），保持 vector 同款 eager 才一致。否则用户在 picker 里挑了 vector → 没点 [保存] → 退出 → 改动丢失（跟 photo 行为不一致，用户更困惑）。

代价：用户在 picker 里来回挑 vector → 多次 DB 写。但每次都是 single column update，可忽略。

### 自定义新建 vector 不 eager

新建模式还没 row id，没法 setHeroVector。所以 `addCustomWithPhoto` / `addCustom` 在点 [新建] 时一次性写 row 拿 id 同时写 photo 或 vector。这是 cycle 0030 已有的 pattern，cycle 0031 保留。

### Detail 抽屉的 back 没接

`DetailScreen` 用 `BottomSheetScaffold` — 抽屉可以从 PartiallyExpanded 拖到 Expanded。没加 BackHandler 让 back 把 Expanded 还原回 Partial。用户没提，先不动。如果以后用户希望，加个：

```kotlin
BackHandler(enabled = sheetState.currentValue == SheetValue.Expanded) {
    scope.launch { sheetState.partialExpand() }
}
```

## 验证

### 编译

```bash
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL in 8m 37s
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（14 MB）。

### 手测清单

1. 装新 APK
2. **返回栈**：
   - 录入页输入对话 → 点 🕐 历史 → back 应只关历史抽屉，仍在录入页
   - 录入页点聊天图 → 全屏 viewer → back 应只关 viewer，仍在录入页
   - 录入页有 confirmedDraft → 点 [手动] → Refine 页 → back 应回 Chat 模式，仍在录入 tab
   - 设置页点 AI 摘要卡 → 抽屉打开 → back 应只关抽屉，仍在设置页
   - 物品详情页点影集照片 → 全屏 viewer → back 应只关 viewer，仍在 Detail
3. **拖动**：
   - 图鉴 → 小红点 manager
   - 长按某 visible 行 → 拖到底部 hidden 段（视觉上能看到 divider 跟着滑）→ 松手该行真的进 hidden
   - 长按 hidden 末位 → 一路拖到 visible 顶 → 松手 → 该行在 visible 第 0 位
   - 同段拖动：长按 visible 中间一行往下拖到 visible 末位 → 排序正确
4. **分类插画 picker**：
   - 新建分类：点头像 → 候选行展开（[+ 选照片] + 一道竖分隔 + HeroVector 全集小圆）
   - 点某 HeroVector 小圆 → 头像变线描；点 [+ 选照片] → 系统相册；选张图 → 头像变照片，且左侧的 vector 小圆有 photo tile 出现可点回
   - 输入中文名 → [新建] 不灰，点 [新建] → 回主屏，新分类的 doorway 用选的 vector 或 photo
   - 内建编辑：点头像 → 候选行展开（只有该分类的 default vector + 当前 photo 如有）；点 [+ 选照片] 换图、点 vector 圆形回到默认线描
   - 自定义编辑（先用上面流程建一个）：点头像 → 看到 HeroVector 整套；点不同 vector → 头像跟着切换且立刻持久化（退出再进还是新挑的）

详见 [`proposal.md`](proposal.md) / [`spec.md`](spec.md)。

---

## 附 · cycle 0031 后续追加（2026-05-12）

cycle 0031 滚到 2026-05-12 又装了一大批用户反馈，单独写新 cycle 显得碎，统一附在这里。

### 拖动复修第二轮 — 真正解决"弹回"

第一轮 (5-11) 改了 commitDrag 公式 + LaunchedEffect-on-id-change，但用户仍报"位置弹回去"。第二轮 (5-11 晚) 定位到真根因：`pointerInput(info.id) { ... }` 块的 closure 只在 key 变化时重抓 callback，`info.id` 不变 → 块里抓住的永远是首次组合时那一份 onDragStart lambda，捕获的 idx 在后续重组里全错。

修复（[`CategoryManager.kt:371-374`](../../android/app/src/main/java/com/treasure/ui/category/CategoryManager.kt#L371-L374)）：

- `rememberUpdatedState(onDragStart/onDrag/onDragEnd/onDragCancel)` — gesture detector 每次触发都拿最新那份 lambda
- onDragStart 用 `combined.indexOfFirst { it.id == info.id }` 现查 idx，完全不依赖闭包捕获
- onDragEnd 在 lambda body 内现算 target，不读 outer composition 里捕获的 targetVisualSlot
- DAO 新增 `@Transaction reorder(orderedIds, hiddenIds)` 单事务，`observeAll` 只 emit 一次终态，中间态不闪

EditScreen specs 拖动同套写法复用（[`EditScreen.kt:506+`](../../android/app/src/main/java/com/treasure/ui/edit/EditScreen.kt#L506)），HERO/TAIL divider 跟着 row 同高一起 shift。

### HeroVector 去重

cycle 0030 删 HeroVectorRow 后，cycle 0031 把它换成 HeroAvatarPicker 复用，但渲染时共享同一线描的 enum 项（CAMERA_RANGEFINDER/DSLR、CAR_SUV/SEDAN、KINDLE/TABLET）会出现重复小圆。

[`CategoryTemplate.kt`](../../android/app/src/main/java/com/treasure/ui/add/CategoryTemplate.kt) 加 `HeroVector.canonical()` + `uniqueHeroVectors` + `heroVectorOptionsForId` 返回前 `distinctBy { canonical() }`。`CategoryEditorRoute` 也用 `selected.canonical()` 让已存的非 canonical 值也能高亮到对应 tile。

### 历史抽屉 current 删除

`AddViewModel.deleteConversation` 之前删的是当前会话就 `newConversation()`，UI 上看像"删完又冒一条 New entry"。改成：先尝试 `resumeConversation(observeRecent(1).first().firstOrNull())`；都没了才 newConversation。同时 `IconGlyphButton` 28dp → 36dp，current row 的 ✎/✕ 用 `colors.ink` 提对比度。

### Theme 切换

`SettingsStore.darkMode: Boolean?`（null = 跟系统、true/false = 强制）+ `TreasureApp.darkModeOverride: MutableStateFlow` + MainActivity collectAsState + Settings header ☀/☾ icon。换主题不重启 Activity。

### Portal 空态大门

`state.visibleCategories.isEmpty()` 时不出 The Rooms / Latest entry 两段，换成新 [`illust/Door.kt`](../../android/app/src/main/java/com/treasure/illust/Door.kt)（拱顶双开木门 + 圆窗 + 把手 + 地面线，240×240 viewbox 同款线描），下方 italic "点开大门，展示你的专属 treasure"，点门进 manager。

### Detail 抽屉 3 页横滑

`DrawerContent` 把 `when (selected) { ... }` 换成 `HorizontalPager(state = pagerState, pageCount = { 3 })`，3 tab 头点击 `animateScrollToPage`。

`AlbumList` 加 + tile（永远第一格、`PickMultipleVisualMedia` 多选），长按缩略图进编辑态，编辑态显示选中 ✓ + 底部 [完成] / [删除 N] 长条；删除走 AlertDialog 二次确认。

Drag handle 从 36dp 灰线把手换成居中小字 "↑ 上拖看详情" / "↓ 下拖收起"（高度 52dp）。

### Grid 标题动态两行

`LazyVerticalGrid` 换 `LazyColumn` + `chunked(2)`。新 `ItemPairRow` 用 `TextMeasurer` 算两张卡的 `lineCount`，pair 取 max → 传给两张 `ItemCard` 当 `minLines/maxLines`。同行任一两行 → 两张都两行，bottom 永远对齐。

搜索 icon 从右上工具栏挪到 chip 条最左 36dp 圆框，点击 → `searchActive` → chip 条整体换成 `SearchInputBar`（auto-focus + 实时 filter `state.allVisibleItems`），输入框边 "取消/清空" 二态按钮。

Header `Treasure` 标题与右侧 [Edit + 小红点] 用 `Row(verticalAlignment = Bottom)` 让底线对齐。

### Edit 页大美化

- "参数 · 拖动选前 4 作关键参数" → 仅 "参数"（hint 文案合并到 HERO/TAIL 分割线居中文字，最终把分割线左右两条横线也删了只保留文字）
- "+ 加一行参数" / "+ 加一条历史" → 单 "+"
- "长按 ≡ 拖动重排 · 顶部 4 行作为关键参数显示" 整段删除
- DANGER ZONE → "操作"；"删除这件物品" → "删除"，下方注释删
- spec 行 key+value 合一张圆角卡 + 中间 0.5dp 竖分隔 + 握把 / ✕ 不带框

### 历史 add/edit 抽屉化

`HistoryEditDialog` 从 `AlertDialog` 改成 `ModalBottomSheet`：顶部居中一排 5 个圆形 emoji icon picker，日期点击调出 Material `DatePicker`，「类型」行去掉。

历史 row 视觉重做：左 36/40dp 圆 + emoji（🛒🏆🔧⚙️👋）+ kindColor 淡填充 + 半透明边框；中间标题 + 中文长日期 `2026 年 5 月 12 日`（[`EditScreen.kt:1119`](../../android/app/src/main/java/com/treasure/ui/edit/EditScreen.kt#L1119) `formatHistoryDate`）；Detail 抽屉时间轴左轨改成"年(小) + 月/日(大)" 两行。

emoji 用系统字体直渲，不引入额外 library。

### Draft 加历史栏

`ItemDraft.history: List<HistoryEvent>`；`AddViewModel.setDraftHistory` setter；`AddPreview` 加 SectionDivider("历史") + 复用 `com.treasure.ui.edit.HistorySection`（从 private 提到 internal，签名改 `history: List<HistoryEvent>` 直接收）；`commitDraft` 用户填了就用，没填走老逻辑默认一条 ACQUIRED。

`AddChat` "手动" → "Draft"。

### 种子数据

- `ItemRepository.ensureSeeded()` 一度被改成 no-op 让 app 空，后又恢复但物品 8 → 6（每个内建分类 1 条，新加 `coffee-mara-x` + `wine-chateau-margaux-2015`）
- 发现 cycle 0026 那次分类种子是用 Migration_8_9 写的，**fresh install 跳过 migration 直接建 v10 表** → 新装无分类。补 `TreasureDatabase.SeedCategoriesCallback`（[`TreasureDatabase.kt:37+`](../../android/core/src/main/java/com/treasure/core/room/TreasureDatabase.kt#L37)）在 onCreate 跑同一套 `INSERT OR IGNORE`，新装 / 老升级双 path 都覆盖

### Detail / Grid Edit + 小红点视觉对齐

DotButton 退役，Detail / Grid 都改用 `Box(size=12.dp, bg=0xFFC5392E)` 红圆 + "Edit" terra labelMedium。Grid Header 改 Column + Row(verticalAlignment = Bottom)，标题与 Edit 行 baseline 对齐。
