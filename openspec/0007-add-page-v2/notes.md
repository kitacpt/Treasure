# Cycle 0007 · 工作笔记

## 流程

- 设计稿来源：Anthropic Design API（同源 cycle 0001 那次）
- 拿到的还是 gzipped tarball，extract 到 `/tmp/design-extract-v2/treasure/...`
- 跟 cycle 0001 的 v1 比对：只有 3 个文件不同（`direction-a.jsx`、`Treasure.html`、`chats/chat1.md`）；其它 5 个 jsx 字节相同。整包复制成 `prototype/add-page-v2/`，HANDOFF.md 写明差异，节省下次 grep 时间。

## 实现拆分

- `AddRoute.kt` — 仅做 mode 编排（Chat ↔ Preview）+ overlay 状态（voice / history / manualPicker）
- `AddChat.kt` — 整个聊天面板（header / dropdown / messages / composer / VoiceOverlay 内部 internal）
- `AddPreview.kt` — 草稿预览（hero / 字段 / footer）
- `AddViewModel.kt` — 状态 + extract / commit
- `CategoryForm.kt` — 维持 cycle 0006 不变（按用户要求）

## 几个坑

- **Modifier.width 阴影**：第一版 AddPreview 我写了一个 `private fun Modifier.width(dp)` 帮助函数想偷懒，结果跟标准 `androidx.compose.foundation.layout.width` 重名。删掉直接用标准 import。
- **`vm.save` → `vm.saveManual`**：把通用 `save` 改成给 manual 用的 `saveManual`，CategoryForm 里第 83 行还在调老名字，编译报"Unresolved reference 'save'"。改完就过。
- **`HistoryDropdown.onPick: (FakeConversation) -> Unit`** vs `onToggleHistory: () -> Unit`：函数签名不同，不能直接传。包装一下 `{ onToggleHistory() }`。
- **LazyListScope.items 嵌套定义**：写了私有 inline `items(count, content)`，在 AddChat 和 AddPreview 各一份。本来想避免 import dance，最后发现标准 `LazyListScope.items(count: Int, ..., itemContent: @Composable LazyItemScope.(Int) -> Unit)` 已经够用，但保留私有 wrapper 让 lambda 不带 `LazyItemScope` 也能用 — 是噪音，下个 cycle 删掉。

## 草稿预览的 9 字段映射

`PreviewField` enum 跟 `ItemDraft` 的对应：

| PreviewField | 来源 |
|---|---|
| Category | `draft.category` (id) → `Category.fromId().nameZh` 显示 |
| Brand | `draft.brand` |
| Model | `draft.model` |
| Nickname | `draft.nickname` |
| Color | `draft.specs.firstOrNull { it.label == "颜色" }?.value` |
| AcquiredDate | 同上 with label "入手日期" |
| AcquiredPrice | label "入手价格" |
| AcquiredChannel | label "入手渠道" |
| OneLiner | `draft.oneLiner` |

写回时反向：first-class 的写 copy(...)，specs 类的更新或追加 HeroSpec。

Confidence 用启发式判断：值非空且超过 2 字 → high；非空 ≤ 2 字 → med；空 → low。粗糙但够看。真接一个 confidence 字段在 ItemDraft 里下个 cycle 做。

## 给下一个 agent

- cycle 0008 候选（按用户优先级走，但参考一下）：
  - **真 schema migration** —— 强制做，已经 7 次 destructive 了。这是债，且只会越来越大
  - **真 STT** + 历史对话持久化（让录入页真活起来）
  - **多轮对话**（assistant 反问 refine draft）
  - **拍照**（直调相机）
  - **AI 生成博物馆插画**
- 历史对话的存储：建议建一张 `add_conversations` 表 + `add_messages` 表，跟 items 解耦（不同的领域）。Or 把对话当成草稿一部分，存到 ItemDraft 里。前者更干净。
