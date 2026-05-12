# Cycle 0031 · 返回栈优先级 · 拖动数学复修 · 分类插画选择肌肉记忆统一

- **状态：** done
- **完成：** 2026-05-11

## 用户反馈 3 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 很多抽屉会直接返回到首页而不是调用方。检查所有页面和抽屉的返回逻辑，是否符合优先返回到调用方；只有 Grid / Add / Settings 三个页面在没抽屉时才返回首页 | MainScreen 顶层 BackHandler 之外，给 3 个之前"漏接"的层加局部 BackHandler，让它们在 LIFO 顺序上覆盖全局：(a) `AddRoute` — `photoPreview != null` 时关 viewer，`historyOpen` 时收抽屉，`mode == AddMode.Preview` 时退回 Chat；(b) `SettingsScreen` — `state.editorOpen` 时关 AI 配置抽屉（它是 AnimatedVisibility 自渲染层，不像 ModalBottomSheet 自带 back）；(c) `DetailScreen` — `fullscreenIndex != null` 时关全屏图 viewer。这三层 BackHandler 都加在对应 state 真为 true 时才 `enabled = true`，闭合状态让出来给 NavHost / MainScreen 兜底。`CategoryManager` 抽屉早已在 MainScreen 顶层有专用 BackHandler，保留 |
| 2 | 分类管理页的拖动还是没法拖动到分割线以下，请你想办法 | cycle 0030 把 divider 改成占一个 visualSlot 后高度对了，但 `commitDrag` 的 `adjustedTarget = if (origin < target) target - 1 else target` + `hiddenIdxOffset = adjustedTarget - newVis - 1` 这套补偿在跨段时多减了 1，结果用户拖到最底 hidden 位时落到倒数第二格。cycle 0031 重写 `commitDrag`：基于"指尖 Y 与 visualSlot 一一对应"这一不变量，直接 `newCombinedIdx = if (targetIsVisible) targetVisualSlot else targetVisualSlot - 1`（只减 1 次给 divider 占的那格），origin 移除带来的 idx 偏移由 List.add 自动吸收。同时把渲染期间所有非拖动行 + divider 也按"预览终态布局" (`previewNewVis` / `previewNewCombinedIdx`) 摆位 — 拖动期间视觉与松手结果同款，再没有视觉空格 / divider 重叠 hidden 行 |
| 3 | 分类新增页和管理页的插画管理根本和物品编辑页的插画管理逻辑不一样，必须调整，点着难受 | cycle 0030 删掉了 HeroVectorRow，用两个按钮 `[+ 从相册选 / 换一张]` `[清除]` 操作，跟物品 Edit 页 `HeroAvatarPicker`（点头像展开候选行）的肌肉记忆不一致。cycle 0031 直接复用 `HeroAvatarPicker`：点头像即展开，候选行里包含 `[+ 选照片]` 动作 chip + 当前 photo 小圆 + 一道竖分隔 + 线描插画小圆。内建分类的"线描"段只放它的 `defaultHeroVector`，自定义编辑放该分类的 `heroVectorOptionsForId(id)` 整套（重新启用 cycle 0030 留作"死代码"的 `saveHeroVectorOnly`），自定义新建放 `HeroVector.entries` 全集。挑线描自动清照片，自定义编辑 vector 写入 eager。canSave 也跟着放宽：自定义新建只要 `nameZh.isNotBlank()`，photo 可有可无（没 photo 时存当前选的 vector） |

## 设计取舍

### BackHandler 优先级靠"composition 顺序" 而不是"优先级数字"

Compose 的 OnBackPressedDispatcher 用 LIFO：最后 register 的 BackHandler 最先吃 back 事件。MainScreen 在顶层 register 全局 handler，子页面 (AddRoute / SettingsScreen / DetailScreen) 在自己 composition 里 register — 自然比全局后，所以子页的 enabled handler 拿到 back 事件后处理掉，全局不会触发。

每个子页 handler 的 `enabled` 跟自己内部 state 绑定（`photoPreview != null` 等），不为 true 时它根本不参与 dispatcher 链，back 自动落到全局 handler。等价于"if 这个抽屉 / 模式开着就拦下，否则让出"。

### "拖到分割线下面" — 不变量重述

`combined = workVisible + workHidden`，`V = visibleCount`，总 visualSlot 数 = `combined.size + 1`（含 divider 占一格）。

- 用户指尖中心 Y / rowPx = `targetVisualSlot t ∈ [0, totalSlots-1]`
- 这 Y 在新布局里对应的 visualSlot 也是 `t`（rowPx 一致）
- 终态布局的 visualSlot 编号规则：combined idx k → if k < newVis then k else k+1（k+1 跳过 divider）
- 反推拖动行的终态 combined idx：
  - 落 visible 段：`t < V` → `idx = t`
  - 落 hidden 段：`t > V` → `idx = t - 1`（用户指尖在 divider 下方，combined idx 比 visualSlot 少 1，因为 visualSlot V 是 divider）
  - 落 divider 槽：`t == V` → 按拖动方向 snap（来自 visible → hidden 首位 = `t - 1 = V - 1`；来自 hidden → visible 末位 = `t = V`）

这套"`idx = t (if visible) else t - 1`"是无论 origin 在哪段都正确的。origin 在哪段只影响 `newVis`：
- visible → visible：newVis = V
- visible → hidden：newVis = V - 1
- hidden → visible：newVis = V + 1
- hidden → hidden：newVis = V

origin 移除导致的 idx 偏移由 `withoutDragged.add(idx, draggingRow)` 处理 — `add(idx, ...)` 把 dragging 行插入到指定位置，其它行后移，符合直觉。

### 拖动期间所有行都按终态布局摆位

cycle 0030 用一个 "make-room shift" 公式给非拖动行加位移；divider 是浮层不动。这导致跨分割线拖动时：(a) divider 视觉静止但 hidden 行被 shift 上来覆盖了它的位置；(b) origin 段被空出一格（除非 shift 公式精确补偿）。

cycle 0031 改成"计算这行在终态布局里的 visualSlot，translateY = newVisualSlot * rowPx"。divider 同款 — 它的终态 visualSlot = previewNewVis（用户拖动时已经预演新 newVis）。一套公式覆盖渲染 + commit 两端。

### 分类 photo 不再强制

cycle 0030 的限制 "自定义新建必须有 photo" 是因为没有 HeroVectorRow 时用户没法选 vector。cycle 0031 既然给了完整 vector 选择，photo 自然变成"可选"。canSave 放宽到 nameZh.isNotBlank()。

如果用户既不挑 vector 也不挑 photo，落到 default heroVector = GENERIC。可接受 — 跟之前 cycle 0029 之前的体验一致。

### HeroAvatarPicker 复用的兼容性

物品 Edit 那边对 HeroAvatarPicker 的需求：多张 photoOptions、tap 切换 / long-press 删、HeroVector 任意挑、有/无 onTakePhoto。

分类编辑的需求：单张 photo、单一 vector 选项（内建） / 全集（自定义新建） / 该分类的 options（自定义编辑）。

HeroAvatarPicker 已经支持：`onTakePhoto = null` 时不展示拍照 chip、`photoOptions = listOf(theOnePhoto)` 单张正常展示。所以零修改复用即可。`selectedPhoto` 给当前 photo 路径，picker 一致地展示 photo on top；点 vector 触发 onSelect 由 caller 决定是否清 photo。

## 不在这一刀

- 删除 cycle 0030 的 tmp 目录孤儿照片
- 死代码清理：CategoryForm.kt / ManualCategoryPicker / AddViewModel.saveManual / repo.setHeroVector (现在又有人用) — 继续推迟到 cycle 0032+
- 撤销采用 / headless WebView / 流式输出
- Detail 页 BottomSheetScaffold 抽屉的 back 处理（半展开 / 全展开切换）— 用户没提

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
