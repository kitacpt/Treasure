# Cycle 0024 · 会话 = 草稿 · 3D 戒指图标 · Grid chip 滚动修正

- **状态：** done
- **完成：** 2026-05-11

## 用户反馈 3 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 图标还是平面图，要立体的戒指，比较像从上方俯视，但又不是最上方的那种 | [`ic_launcher_foreground.xml`](android/app/src/main/res/drawable/ic_launcher_foreground.xml) 重画：3 个透视手法 — (a) 外轮廓 / 内孔从圆变椭圆（rx > ry），(b) 内孔相对外圈往上偏 4dp 模拟"背面内壁离我们远，看起来更高"，(c) 外圈下半侧加一条窄的"侧壁渐变带"给出厚度。光源左上：左上高光弧 + 右下暗弧。内孔顶半暗（远端内壁遮天）、底半亮（近端内壁反顶光）。投影也改成扁椭圆软影，符合俯视角度 |
| 2 | AI 录入页：一个会话就是一个草稿，会话里的手动录入是修改这份草稿，AI 在这次对话里的每次修改也都是基于这份草稿做修改。但不能让 AI 直接对这份草稿完成修改，需要有确认的过程。所以说，AI 只是基于上一个确认的版本提供下一个版本，而不是每次都提供一个全新版本 | 这是大改。把 AI 录入页从"每次重新生成一份独立草稿"重做成"会话级演进式草稿"：(a) `AddUiState` 分裂出 `confirmedDraft`（用户已采用的最新版）和 `proposedDraft`（AI 最新提案，未采用）；(b) AI 调用时把 `confirmedDraft` 当 baseline 拼到 system prompt 末尾，要求 AI **基于它给下一版**而不是从零开始，避免每轮换一套字段；(c) `DraftCta` 卡片三状态（Pending / Accepted / Rejected），Pending 右下角配 [采用] / [不要] 按钮；(d) "采用" → confirmed = proposed，在聊天里 append 一行 `✓ 已采用 · N 个字段`（也入库当 baseline 锚点），如果之前还有 pending CTA 自动标 Rejected；(e) "不要" → 仅 dismiss，proposed 清空；(f) "手动" 按钮不再弹 CategoryForm 4 品类选择，改成直接进 Refine 页编辑 `confirmedDraft`（没有就建空白）；(g) Refine 页的 "确认收入" 把 confirmedDraft 落到 Room 当 Item。整个会话只有一个草稿在演化，AI 的每次修改都是 patch 而非重写 |
| 3 | 图鉴页的每个分类按钮，不用点击后就把它放在第一个位置去聚焦，这个是之前做了从首页跳转后聚焦导致的新问题。需要一种更自然的操作（用户点击后只聚焦但滑动条不会自己动，除非从首页点过来时发现焦点不在当前可见范围内） | `GridScreen.CategoryChips` 把 `LaunchedEffect(selectedIndex) { animateScrollToItem(selectedIndex) }` 改成 "先看 `layoutInfo.visibleItemsInfo` 里有没有目标 chip — 在的就不滚，离屏才 animateScrollToItem"。自然得很：用户能点中的 chip 一定可见，所以点了不动；从门厅跳过来的目标如果在屏外才被拉进视野 |

## 关于"会话 = 草稿"的几个 design decision

### 持久化用什么 schema 表示

不加新列。`DraftCta` 的三态（Pending / Accepted / Rejected）复用现有 `ConversationMessageEntity.text` 列存（原本 DraftCta 行 text 是 null）。新增一个 `draft_confirmed` role 表示"用户采用后的草稿快照"，复用 `draftJson` + `fieldCount` 两列。重载会话时按时间顺序扫消息推导：
- 最新 `draft_confirmed` = `confirmedDraft`
- 在它之后（或全程）的最新 Pending DraftCta = `proposedDraft`

不存额外 baseline 列省去 schema migration（[ADR-0006](docs/adr/0006-schema-migrations.md) 严格意义还是要写 Migration 才能改字段，复用旧字段避开了这一刀）。

### 多个 pending CTA 并存怎么办

不会并存。每次 AI 跑出新提案时，扫一遍 messages 把所有 status=Pending 的旧 DraftCta 一律改 Rejected — 等同于"用户没采用就来了新的，自动判旧的失效"。这样语义清晰：任何时刻最多只有一个 pending CTA，避免用户被多份提案困扰。

### "手动" 按钮还要不要 CategoryForm

不要了。CategoryForm + ManualCategoryPicker 沿用了 cycle 0006 的"4 品类弹层 + 模板表单"流程，本来是 AI 之外的独立保存路径。本 cycle 的新模型里手动录入是**修改会话草稿**而不是**绕过 AI 直接存**，所以 CategoryForm 就退役。`AddRoute` 不再 mount 这两个 composable；文件本身留在 tree 里（dead code）由下个 cycle 清理。`AddViewModel.saveManual` 同理。

### "采用" 按钮和 "确认收入" 按钮的区别

- **采用**（在聊天 DraftCta 卡片上）：把 AI 这一版升格成 `confirmedDraft`，但**还在会话里**，没存入 Room
- **确认收入**（在 Refine 页头部）：把当前 `confirmedDraft` 写到 Room 当一个 Item，并 `newConversation()` 开启下一段对话

用户可以在采用之后继续聊天叠加修改（每次 AI 都基于新的 baseline），直到去 Refine 页点确认收入才真正"收藏"。这是关键的分层 — AI 的演进 vs 物品的固化。

## 不在这一刀

- 删 CategoryForm.kt / saveManual 的死代码（下个 cycle 清）
- DraftCta 卡片支持"先打开看草稿全文，再决定采用/不要"（当前卡片只 surface brand+model+oneLiner+fieldCount）
- "撤销采用"（用户采用后想反悔）— 现在只能拒绝新提案 + 等下次 AI 重新建议
- 流式输出 / WebView headless（之前 cycle 推迟的）

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
