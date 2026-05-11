# Cycle 0030 · 拖动 divider 当块 · 隐藏过滤补全 · 相册选图 · 主页标题回归

- **状态：** done
- **完成：** 2026-05-11

## 用户反馈 4 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 分类管理的拖动问题很大，那个分割线不知道飞到哪个位置了，而且他到最底部后，就拖不到他下面去了。分割线占用的空间和某一行占用的空间一样，作为一个"块"进行占位 | 重写 `CategoryList` 拖动数学：divider 改成跟 row 同高 (`ROW_HEIGHT`) 占一个独立 visualSlot，不再是 36dp 浮层。坐标域统一在 `visualSlot ∈ [0..combined.size]`（含 divider 这一格），可拖范围 = `combined.size + 1` 个 slot 高度，所以拖到最底部还有"divider 后面那段"可以放下去。把 `combinedToVisual(idx)` / `computeShift(...)` / `commitDrag(...)` 抽成 3 个纯函数，drop 时算 `effectiveTargetSlot` 跟原 `originVisual` 比较确定是否跨段 → toggle hidden + reorder 一次性 commit |
| 2 | 新增分类和分类编辑里的插画选择，怎么是选品类的，就做成和物品页选影集一样就行，从相册选择，不提供默认插画 | (a) Schema v10：`category_prefs` 加 `hero_photo_path TEXT` 列（Migration_9_10 ALTER）；(b) `CategoryInfo.heroPhotoPath: String?`；(c) `CategoryRepository.setHeroPhotoPath` + `CategoryManagerViewModel.pickHeroPhoto / clearHeroPhoto / addCustomWithPhoto`，复用 cycle 0003 那套 `filesDir/category-photos/<id>/<uuid>.jpg` 路径规则；(d) `CategoryEditor` 删 HeroVectorRow + heroLabel，换成 `[+ 从相册选 / 换一张]` + `[清除]` 双按钮 + `PickVisualMedia` launcher；(e) `AvatarHero` 优先显示 photo，没图时内建 fall back 到 `Category.defaultHeroVector` 线描，自定义 italic "+ 从相册选" 占位；(f) `canSave` 自定义新建必须有 photo，内建始终能存（没图走默认插画）；(g) `Portal.stubItemFor` 把 `info.heroPhotoPath` 写到 stub Item 的 `avatarPhotoPath` — `HeroAvatar` 一直就会优先用 photo 覆盖线描 |
| 3 | 全部页和搜索页，还是能看到被隐藏分类里的物品项 | (a) `SearchRoute` 漏了过滤 — 之前直接用 `repository.items`，cycle 0030 加 `categoryRepository.observeAll()` collect 后算 `visibleIds`，搜索时同时要求 `it.category in visibleIds`；(b) `GridViewModel` 还有个 edge case：用户在某分类 chip 上时把那个分类隐藏掉，chip 行虽然消失但 `selectedId` 还指着它，items 就还是按 selectedId 过滤显示。修：`effectiveSelectedId = if (selectedId !in visibleIds) null else selectedId`，再写到 `GridUiState.currentCategoryId` 上 — 自动 fall back 到"全部" |
| 4 | 没让你动的地方尽量别动，发现你好像动了主页的标题样式 | cycle 0026 重写 `PortalScreen` 时把 `GrandTitle` 偷偷动了：title 从 `displayLarge` 改成 `titleLarge`，spacer 10dp → 4dp，副标从英文 "a private cabinet of things owned, used, & remembered" (`displayMedium`) 改成中文 "私人博物馆 · 图鉴" (`labelSmall`)，外层去掉了 24dp horizontal padding。还顺手丢了底部 Ornament。这个 cycle 全部还原回 393b1ca 当时的样子 |

## 设计取舍

### 拖动 divider 作"块"占位 = 数学统一

之前 (cycle 0028) divider 是 graphicsLayer 浮层（36dp 高），不参与 row 的 baseY 计算 — 结果两套坐标系：(a) `combined[idx]` 的 baseY = `idx * rowPx + (if idx >= visibleCount then dividerPx else 0)`；(b) 目标 index 反算还要再处理 divider 偏移。两个分支多了 `if/else`，写错就出 bug — 用户反馈的"分割线不知道飞到哪个位置"，本质是这套混杂坐标系下视觉位置和拖动算法的 target slot 不一致。

cycle 0030：divider 也占一个 ROW_HEIGHT slot。`visualSlot` 是唯一坐标域。combined[idx] 通过 `combinedToVisual(i)` 算它的 visualSlot（跳过 divider slot），dragOffset / target 都直接 in visualSlot 域里跑。Make-room shift 也用 visualSlot，跳过 divider 时不计 row。`commitDrag` 把 visualSlot 还原成 (combined index + hidden flag)。

代价：抽屉总高 = (combined.size + 1) × 60dp，比之前的 combined.size × 60dp + 36dp 略高（多 24dp）。可忽略。

### 拖到最底部能下去 = 容器高包含 divider slot

总高 `ROW_HEIGHT * totalSlots` 里 `totalSlots = combined.size + 1`，所以 6 visible + 0 hidden 时容器高 = 7 × 60dp = 420dp，比 visible 实际占的 6 × 60dp = 360dp 多出 60dp。这 60dp 给 divider 占一个 slot，用户拖一个 visible 行到容器底部就刚好把它推到"divider 下方"的 visualSlot = visibleCount = 6，触发 `originVisual < targetVisualSlot` → drop 进 hidden 段。

### 自定义分类禁默认插画 vs 内建保留

用户原话"不提供默认插画"。我读成"对自定义分类不提供默认"。内建分类有现成的代表插画（cycle 0028 加的 `Category.defaultHeroVector`），用户可能想要 — 所以内建保留 fallback。逻辑：

- 自定义新建：`canSave = nameZh.isNotBlank() && currentPhoto != null` — 必须从相册挑图
- 自定义编辑：`canSave = nameZh.isNotBlank()` — 可以清掉图（但 AvatarHero 就显示 italic 占位，Portal doorway 也会回到一个空头像；可接受）
- 内建：`canSave = true` — 没图走默认插画，挑图就覆盖

这给了用户灵活性 — 内建分类如果想用一张更精美的家庭照片代表"摄影"也可以。

### `pendingPhotoForNew` 在 VM 里暂存

新建分类时还没有 row id，photo 没法 `setHeroPhotoPath`。把它暂存到 `vm.pendingPhotoForNew` (String 路径)，文件先落在 `filesDir/category-photos/tmp/`。用户点 [新建] 时 `addCustomWithPhoto` 创建 row 拿到 id，把 tmp 目录的图复制到 `category-photos/<id>/`，写 DB 字段。

如果用户没点 [新建] 直接退出 editor：tmp 文件留在那里。空间小，下次 GC 也行；当前先不清。

### Manager `setHeroVector` / `saveHeroVectorOnly` / `setHeroVectorOnly` 还能用吗

cycle 0030 把 editor 里的 HeroVectorRow 删了，所以这些 API 没人调。但 repository 接口里还在 — 给老消费者保留。下个 cycle 真的没人用了再删。

### Schema v10 + `defaultValue = ""`

ALTER TABLE 加 nullable TEXT 列，老 row 默认 NULL。schemas/10.json 自动 export。Migration_9_10 写在 `Migrations.ALL` 末尾。Room version 9 → 10。

## 不在这一刀

- 删除 tmp 目录的孤儿图片（用户在新建模式选了图但没点 [新建] 就退出时）
- 删除 hidden category 相关的"删除"动作（manager 里只能 toggle，不能删内建）
- 死代码清理（CategoryForm.kt / setHeroVector / saveHeroVectorOnly 等）
- 撤销采用 / WebView headless / 流式输出

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
