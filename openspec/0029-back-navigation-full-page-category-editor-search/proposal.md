# Cycle 0029 · BackHandler 修返回 · 分类编辑器全屏 · 图鉴搜索

- **状态：** done
- **完成：** 2026-05-11

## 用户反馈 4 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 很多页面的返回后会退出应用，返回的逻辑应该是返回到拉起该页面的原页面，而最开始时的页面返回要先返回到首页 | `MainScreen` 加 `androidx.activity.compose.BackHandler`：(a) 分类管理抽屉打开时优先收起抽屉；(b) 不在 Portal tab 时返回回 Portal（HorizontalPager animateScrollToPage(PAGE_PORTAL)）；(c) 在 Portal 时默认行为（= 退出）。Detail / Edit 已经走 NavHost.popBackStack 不动；新增的 Search / CategoryEditor 路由也都走 popBackStack |
| 2 | 分类管理的"没有现实中的分类——从下方滑回来一个"这种提示词可以去掉，简洁为主 | 拆出 Editor 到全屏路由（item 3）顺手把 Manager 底部 italic 提示 "长按 ≡ 拖动 — 跨过分割线即可隐藏 / 显示..." 也去了。整个 Manager 现在只有 Header + 拖动 list + 分割线 italic 标签 "↑ 显示中 · ↓ 已隐藏"。前一轮 cycle 0028 重写时已经没了 "没有显示中..." 等空 row 提示 — 用户看到的那段文字其实是 cycle 0026 老版本残留的记忆，这次彻底干净 |
| 3 | 新增分类页和编辑分类页都用单独的完整页面，返回用和物品返回一样的返回键（保持 UI 风格一致） | 拆 `CategoryManager.kt` 里的内嵌 Editor，独立到 `ui/category/CategoryEditorRoute.kt` 全屏路由，复用 `EditPageHeader` + `BackArrow` 与物品 Edit 页同款。新路由 `category/new` / `category/edit/{categoryId}`。Manager 抽屉里点 "+ 新增分类" 或行右侧小红点时先 `onClose()` 收抽屉，再 nav 过去；返回 popBackStack 回到主屏（不会顺手把 Manager 重新弹开 — Manager state 在 Main 层是关闭状态） |
| 4 | 图鉴页的小红点左边加一个搜索 icon，点击后进入搜索页，每输入一个字就立刻返回搜索结果，只对品牌型号昵称进行搜索即可。搜索到在标题上要高亮，展示就和图鉴页的展示方式一样即可 | 新 `ui/search/SearchRoute.kt` 全屏路由，前端 `BackArrow` + 圆角搜索框（自动 focus + IME 弹出）+ 实时筛 `Item.brand/model/nickname` 大小写不敏感 contains；结果展示 2 列同 `GridScreen.ItemCard` 排版。标题 `brand + model` 命中段用 terra 色 + SemiBold 高亮（`AnnotatedString.SpanStyle`）；副标题 `oneLiner` 或 fallback `nickname` 也同样高亮。Grid 右上工具栏由 `[小红点]` 改成 `[🔍] [小红点]` 两个 32dp 触控区，Canvas 画了简笔放大镜 |

## 设计取舍

### BackHandler 简单方案 vs tab 历史栈

考虑过维护"用户访问过的 tab 列表"做精确"返回到上一 tab"。最终选择简单的"非 Portal → Portal"二阶模型：

- 优点：实现 5 行，行为可预期，符合 Android 标准 "home is root" 范式
- 缺点：用户 Grid → Add → Settings 时，按 back 直接回 Portal，不是 Add

用户的反馈 "返回到拉起该页面的原页面" 大概率指的是 Detail / Edit / Search 这种 push 上来的子页面 — 这些走 NavHost 的 popBackStack 早就工作。Tab 之间互相切换不是 "push" 关系，没有"原页面"概念。所以简单方案就够了。

如果将来用户反馈"我从 Settings 返回想回到刚才的 Grid"，再加 tab 历史栈。

### 全屏 Editor vs sheet 内 Mode 切换

cycle 0026 引入分类编辑时，为了少 wire 路由用了 ModalBottomSheet 内部 Mode sealed 切换 (List / Edit / Add)。cycle 0029 用户明确要求"和物品编辑页同款返回键"，那必须全屏路由（item Edit 是 NavHost push 上来的）。

代价：CategoryEditorRoute.kt 复用了 cycle 0028 Editor 那段 ~250 行代码（AvatarHero / HeroVectorRow / FieldRow / EditorTextField / PillChip）。原始 CategoryManager.kt 里的 Editor 那段全删了（cycle 0028 写的那版没人 mount 了）。文件量增加 ~250 行，但 ItemEdit 与 CategoryEdit 视觉对齐。

### 搜索粒度：3 字段 contains，不分词

用户原话："只对品牌、型号、昵称进行搜索即可"。所以 specs / oneLiner / category 都不参与匹配。Contains 是最朴素的策略 — 用 lowercase 做 case-insensitive；不做分词 / 拼音模糊 / 编辑距离。这对 brand 类（"Yonex" / "Sony"）和 model 类（数字 + 字母组合）已经够用。

如果将来用户报"我打 'yn' 想搜 Yonex 但 ynex 也命中是不是不准" — 加更智能的 ranking。目前先简单。

### 标题高亮 vs 全文高亮

标题（brand + model）和副标题（oneLiner 或 nickname）都跑高亮。nickname 命中但标题没命中的卡片，标题不高亮但卡片还是出现 — 用户看见副标题里 nickname 那块高亮就明白为什么命中。

### Empty 分类管理 hint 是 cycle 0028 已经移除

用户说"分类管理的'没有现实中的分类'..."。实际 cycle 0028 重写 manager 时已经没有这种 empty row 了。但底部还残留 italic "长按 ≡ 拖动 — 跨过分割线..."，这次也删了。Manager 现在零废话。

## 不在这一刀

- Tab 历史栈（"从 Settings 返回回 Add"）
- 搜索的 specs / oneLiner / category 字段匹配
- 拼音 / 模糊匹配
- 搜索结果分组（如"按品类聚合"）
- 死代码清理 / 撤销采用 / WebView headless / 流式输出

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
