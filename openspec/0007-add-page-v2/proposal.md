# Cycle 0007 · 录入页 v2（chat-first + 草稿预览）

- **状态：** done
- **完成：** 2026-05-07

## 这一刀切什么

按用户给的新设计稿（`prototype/add-page-v2/`，HANDOFF.md 里有原文链接），把外层"录入"页从 cycle 0006 的留空状态升级成 chat-first 录入流。

只动录入页，**其它屏全部保留**：

- Portal / Grid / Detail / Edit / Settings — 不动
- 手动录入面板（CategoryForm）— 不动；从新录入页右上角"手动"按钮进入

## 设计落地

### 默认进 AI 对话

无 chooser 起步页，进来就是聊天界面。

### 顶部 header

```
录入  Fujifilm X-T5 ▾                     🕐  ⊕  [📝 手动]
```

- 左：标题 "录入" + 当前对话名 + ▾，点击或点 🕐 都展开历史抽屉
- 右：3 颗按钮
  - 🕐 圆形 — 历史对话（dropdown 抽屉）
  - ⊕ 圆形 — 新对话（清掉当前消息列表）
  - "手动" 胶囊带图标 — 弹品类选择 → CategoryForm

### 历史抽屉

dropdown 自顶部右下垂，列出最近对话（暂时只有当前一条 stub），底部 "+ 新对话"。当前对话有 `card` 背景高亮。

### 消息样式

- **Assistant**：italic serif，card 背景，左下角小圆角的气泡（4dp / 14dp）
- **User text**：ink 背景 paper 文字，右下角小圆角
- **User photo**：120dp 方块，Coil AsyncImage 渲染
- **User voice**：ink 背景 + 波形条 + 时长 + italic 转写
- **Draft CTA**：横向卡片：左缩略 hero + DRAFT · N FIELDS + "草稿已就绪" + italic "轻点过目" + 右箭头

### 输入栏（composer）

挂在控制岛**上方**（`bottom = navigationBarsPadding + 88dp`），不被遮：

```
[📷] [说说这件东西…       ] [🎙] [→]
```

- 📷 → 系统 Photo Picker → user photo bubble + AI extract
- 文本 → enter / send → user text bubble + AI extract
- 🎙 → 全屏蒙层 + 波形 + italic "二零二三年情人节，一万二千五…" + "松开发送 · TAP TO STOP"。点蒙层 dismiss → user voice bubble + AI extract（stub 转写文本，未真接 STT）
- → 发送，dirty 时 ink 实色，clean 时 line 灰背景

### 草稿预览屏

draft CTA 点击 → 切到 Preview 屏（同 AddRoute 内部状态切换，不走 nav）：

```
草稿预览                            ← 换一种
REVIEW · EDIT · CONFIRM
─────────────────────────
DRAFT №024              UNCONFIRMED
[ hero  ]   FUJIFILM
[       ]   X-T5
[       ]   APS-C · 4020 万像素

· 确定  · 可能  · 需补充

· 品类      摄影              ✎
· 品牌      Fujifilm           ✎
· 型号      X-T5              ✎
· 昵称      （点击补充）       ✎
· 颜色      （点击补充）       ✎
· 入手日期  （点击补充）       ✎
· 入手价格  （点击补充）       ✎
· 入手渠道  （点击补充）       ✎
· 一句话    APS-C · 4020 万像素 ✎

[ 继续修改 ]  [ ✓ 确认收入图鉴      ]
```

- 9 个字段（`PreviewField` enum），每行 confidence dot + label 72dp + value + 编辑笔
- Tap 行 → 切 inline edit（BasicTextField + 确认 / 取消）→ 保存值 +  confidence 升 high
- 确认收入图鉴 → 构建 Item → repo.upsert → 跳新 Detail + 同时 newConversation 让 Add 屏复位

### 手动按钮

→ 弹"手动录入 · 选品类"小窗，4 个品类行 → 选 → 弹 CategoryForm（cycle 0006 那一版，未动）。

## 数据流

`AddViewModel`：

- `messages: List<AddMessage>` — 对话状态
- `conversationTitle` — 由最后一次 AI 草稿的 brand+model 决定
- `draft: ItemDraft?` — 当前草稿
- `busy` — 调 AI 中
- `aiAvailable` — `settingsStore.hasKey()` 决定
- `recentConversations` — 写死的 stub 列表（cycle 0008+ 接持久化）
- 方法：`sendText` / `sendPhoto` / `sendVoiceStub` / `updateDraftField` / `commitDraft` / `saveManual` / `newConversation` / `refreshAiAvailability`

## 约束 / 留账

- 语音是**视觉 stub**，没接真 STT；点蒙层 dismiss 自动喂入一条预写转写
- 历史对话是**视觉 stub**，没接持久化，切换不真切
- 手动表单（CategoryForm）按用户要求保持 cycle 0006 现状，不重写
- AI 提示词没专门改 — 草稿里"颜色 / 入手日期 / 入手价格 / 入手渠道"如果 AI 没主动放进 specs 里就显示空，由用户补
- 多轮对话**不实现** — 每条用户消息独立调一次 extract，覆盖之前的 draft

## 不在这一轮（cycle 0008+）

- 真 STT (Android `SpeechRecognizer` 或 server-side ASR)
- 历史对话持久化 + 切换
- 多轮对话（assistant 跟问下去、refine draft）
- 拍照（直调相机）
- AI 生成博物馆插画
- 真 schema migration（cycle 0001-0007 七次 destructive，**强烈建议下一刀就做**）

## 验收

详见 [`spec.md`](spec.md)。
