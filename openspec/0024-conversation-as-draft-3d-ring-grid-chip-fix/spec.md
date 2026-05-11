# Cycle 0024 · spec

## 1. App 图标改 3D 俯视戒指

Vector spec：
- viewport 108×108
- 外椭圆 rx=24 ry=14（之前是 23×23 圆）
- 内椭圆（hole）rx=13 ry=6/7，cx 与外圈相同，cy 比外圈低 4dp — 这个"内孔上偏"是透视的核心
- 主体填 5-stop 线性 gradient（#F7E29A → #2A1A06），从左上 (34,36) 拉到右下 (74,72)
- 单独一条"前侧壁带"：外圈底缘到一条略低的椭圆（ry=16.5）之间的窄月牙，渐变更暗 (#8C6028 → #1F1304)，明显比顶面暗，给厚度感
- 左上高光弧（亮金）：M32,51 a22,13 0 0,1 22,-13
- 右下暗弧（深棕）：M76,57 a22,13 0 0,1 -22,13
- 内孔顶半暗线（远端内壁遮天）
- 内孔底半亮线（近端内壁反顶光）
- 顶面雕刻 rune（纸色 zigzag）保留
- 投影：扁椭圆 (rx=26, ry=4.5)，y=84，alpha 26000000

## 2. 会话 = 草稿（AddViewModel/AddRoute/AddChat/AddPreview/AiClient/Prompts）

### 数据状态

```kotlin
data class AddUiState(
    val confirmedDraft: ItemDraft? = null,   // 用户已采用
    val proposedDraft: ItemDraft? = null,    // AI 最新提案，未采用
    val pendingCtaId: String? = null,        // 对应的 DraftCta 行 id
    // ... 其他字段同前
)
```

### AddMessage 改动

- `DraftCta(id, draft, fieldCount, status)` — `id` 字段持久化、`status` ∈ {Pending, Accepted, Rejected}
- 新 `DraftConfirmed(draft, fieldCount)` — "✓ 已采用 · N 个字段" 行

### AI 流程（runExtract）

1. 收集 priorTurns（messages 最后 20 条，DraftCta 摘成"已写出草稿 / 已被采用 / 被拒绝"三种摘要文字）
2. 把 `confirmedDraft` 当 baseline 一并传入 `client.extractItemDraft(..., baseline = ...)`
3. `Prompts.buildSystemWithBaseline(baseline)` 拼接：
   ```
   {SYSTEM_PROMPT}

   [CURRENT CONFIRMED DRAFT — the user has accepted this as the baseline
   for this conversation. Your job is to give the *next version* of this
   draft, not start from scratch. Keep fields you don't have evidence to
   change. Only add / refine / overwrite the parts the user's new message
   actually addresses.]

   {baseline JSON}
   ```
4. AI 返回 → 创建 `DraftCta(id=UUID, status=Pending)`，写到 `proposedDraft` + `pendingCtaId`
5. 同时把之前还在的 Pending CTA 自动改 Rejected

### 采用 / 不要

- `vm.acceptProposal(ctaId)` →
  - 该 DraftCta 状态 → Accepted（state + Room）
  - 追加 `DraftConfirmed(draft)` 消息（state + Room）
  - `confirmedDraft = proposedDraft`；`proposedDraft = null`；`pendingCtaId = null`
- `vm.rejectProposal(ctaId)` →
  - 该 DraftCta 状态 → Rejected（state + Room）
  - 若是当前 pending → `proposedDraft = null`；`pendingCtaId = null`

### 手动 / Refine 页

- "手动" 按钮 → `vm.ensureDraftForManual()`（confirmedDraft 为空则建空白），然后 `mode = Preview`
- Refine 页编辑的是 `confirmedDraft`，所有 inline edit 直接落上去
- "确认收入" → `vm.commitDraft(status) { id -> newConversation(); onSaved(id) }`

### Conversation reload

- 扫所有消息：最新 `DraftConfirmed` = confirmedDraft；之后最新 status=Pending 的 DraftCta = proposedDraft / pendingCtaId
- 如果整段对话都没采用过任何提案，confirmedDraft = null（手动按钮按 ensureDraftForManual 建空白）

### 持久化（无 schema migration）

- DraftCta status 复用现有 `text` 列存（"pending" / "accepted" / "rejected"）；老数据 text=null 当 Pending
- DraftConfirmed 新增 role `"draft_confirmed"`，复用 draftJson / fieldCount 列

## 3. Grid chip 不再点完就置首

`GridScreen.CategoryChips`：

```kotlin
LaunchedEffect(selectedIndex) {
    val visible = listState.layoutInfo.visibleItemsInfo
    val isVisible = visible.any { it.index == selectedIndex }
    if (!isVisible) listState.animateScrollToItem(selectedIndex)
}
```

行为：
- 用户在 Grid 内点 chip：那个 chip 一定在视野里（否则点不到），LaunchedEffect 触发但 isVisible=true，**不滚**
- 从门厅 doorway 跳过来：initialCategoryId 变化导致 selectedIndex 变；目标 chip 可能在屏外（"全部"陈列后第 N 个），**这时才 animateScrollToItem**
- 屏外目标也包括"目标恰好被边缘截断一半"的情况 — visibleItemsInfo.index 含义就是"正在显示的所有行 index"，半显示也算可见

## 4. Out of scope

- CategoryForm.kt / ManualCategoryPicker / saveManual 死代码清理
- 撤销采用
- DraftCta "先看全文再决定" 的预览
- 流式输出 / WebView headless render
