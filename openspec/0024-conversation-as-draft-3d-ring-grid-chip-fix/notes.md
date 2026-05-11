# Cycle 0024 · notes

## 文件改动

- `res/drawable/ic_launcher_foreground.xml` — 全部重写为透视椭圆 3D 戒指（内孔上偏 + 前侧壁渐变带 + 内壁明暗）
- `app/.../ui/grid/GridScreen.kt`
  - `CategoryChips` LaunchedEffect 改成"target 不在 visibleItemsInfo 才 animate"
- `core/.../repo/AddConversationRepository.kt`
  - 新 enum `DraftCtaStatus { Pending, Accepted, Rejected }`
  - `AddConversationMessage.DraftCta` 加 `status` 字段
  - 新 `AddConversationMessage.DraftConfirmed`
  - `toDomain` / `toEntity` 改：DraftCta 状态复用 text 列存、`draft_confirmed` 新 role 复用 draftJson + fieldCount
- `core/.../ai/AiClient.kt`
  - `extractItemDraft` 加 `baseline: ItemDraft? = null` 参数
- `core/.../ai/Prompts.kt`
  - 新 `buildSystemWithBaseline(baseline, json)` — baseline 非空时拼一段 "[CURRENT CONFIRMED DRAFT — give *next version*, not from scratch]" + baseline JSON 到 SYSTEM_PROMPT 末尾
- `core/.../ai/AnthropicClient.kt` / `core/.../ai/OpenAiClient.kt`
  - 各自 `extractItemDraft` / `buildPayload` 多接一个 baseline，把 SYSTEM_PROMPT 替换成 `buildSystemWithBaseline(baseline, json)`
- `app/.../ui/add/AddViewModel.kt`
  - `AddUiState` 拆出 `confirmedDraft` / `proposedDraft` / `pendingCtaId`，保留 `refineDraft: ItemDraft` computed 属性
  - `AddMessage.DraftCta` 加 `id` + `status`；新 `DraftConfirmed`
  - 新 `deriveDraftsFromMessages(msgs): DerivedDrafts` — reload 时推导 confirmed/proposed/pending
  - `runExtract` 把 baseline = confirmedDraft 传给 client，supersede 之前的 pending CTA 为 Rejected
  - 新 `acceptProposal(ctaId)` / `rejectProposal(ctaId)` / `ensureDraftForManual()`
  - 新 `upsertCtaStatus(cta)` / `persistDraftCtaWithId(cta)` 持久化 helper
  - 现有 `updateDraftField` / `updateDraftSpec` / `addDraftSpec` / `removeDraftSpec` / `commitDraft` 全部改成操作 `confirmedDraft`
  - `buildPriorTurns` 多了 DraftConfirmed 分支（跳过 — baseline 已经在 system prompt）
- `app/.../ui/add/AddRoute.kt`
  - 全部重写：移除 `manualPickerOpen` / `manualSession` state；不再 mount `ManualCategoryPicker` / `ModalBottomSheet` / `CategoryForm`
  - "手动" 按钮和 DraftCta 卡片点击都走 `ensureDraftForManual()` + `mode = Preview`
  - AddChat 新增 onAcceptProposal / onRejectProposal 回调
- `app/.../ui/add/AddChat.kt`
  - AddChat signature 加 `onAcceptProposal: (String) -> Unit` / `onRejectProposal: (String) -> Unit`
  - `MessageRow` 加 DraftConfirmed 分支 + 透传 accept/reject 回调
  - `DraftConfirmedRow` — 居中 italic terra "✓ 已采用 · N 个字段"
  - `DraftCtaCard` 重写：根据 status 决定 alpha、tag 文案、是否显示 [采用]/[不要] 按钮、是否给标题加 strike-through
- (dead code 暂留) `app/.../ui/add/CategoryForm.kt` + `AddViewModel.saveManual` — 没人调用了

## 设计取舍

### 为什么 confirmedDraft 不持久化为 conversation 的列

我考虑过给 `ConversationEntity` 加一列 `confirmed_draft_json`。但这要 Room schema bump → Migration → 新 JSON schema export，相对工程量大。

而 conversations 表本身已经存了 messages，messages 里"用户采用过什么草稿"是完整可推导的信息：扫一遍消息找 `DraftConfirmed` 中最新那个就行。把 derivation 放进 `deriveDraftsFromMessages` 一个函数里，逻辑集中、可测试，不需要 schema 改动 — 完美避开了 [ADR-0006](docs/adr/0006-schema-migrations.md) 的全套迁移流程。

代价：每次 reload 一遍 messages。但 messages 单段对话最多几十条，O(N) 扫一遍可以忽略。

### 为什么 supersede 旧 pending 为 Rejected 而不是 Accepted

新 AI 提案到了意味着用户没在旧 pending 上点采用就开始新轮对话。如果改成 Accepted 等于替用户做主，不符合"需要确认"原则。改成 Rejected 是"语义上自动 dismiss 旧的，强迫用户对新版做选择"——更安全的默认。

如果将来用户反馈"我打字打到一半 AI 又出新版，前面那版我其实想要"，可以加"撤销 Reject"或者改 supersede 策略。目前先 Rejected。

### baseline 拼在 system prompt 末尾而不是 user message 里

两条理由：
1. system 是稳定语境，user 是"这一轮的输入"。baseline 是上一版的产物，本质属于稳定语境，放 system 更符合 OAI/Anthropic 的语义边界
2. priorTurns 已经把对话历史串好了。如果 baseline JSON 也塞到 user 末尾，会和 priorTurns 里"已经替用户写出草稿"的 assistant 摘要冲突 — 模型可能混淆"这个 baseline 是上一条 assistant 输出的吗？" system 末尾就清楚多了

### Grid chip "不滚" 在什么情况下还会自动滚

只在 `LaunchedEffect(selectedIndex)` 触发时 + 目标不在 visibleItemsInfo 里才滚。两种触发场景：
1. 用户点 chip：selectedIndex 变了，但 chip 必然在 visible（点得到）→ 不滚
2. 门厅 doorway 点过来：`initialCategoryId` → `selectCategory` → currentCategory → selectedIndex 变；若目标 chip 不在视野（如选了第 6 个品类而当前 LazyRow 显示前 4 个）→ 滚

第二种是用户原本想要的 "从首页跳过来焦点要在视野里" 的合理行为。

### 3D 戒指还是 2D 戒指

之前的 cycle 0012 / 0013 已经在 2D 圆环上加了很多视觉细节（gradient 渐变 + 高光弧 + rune 雕刻），但外轮廓还是个完美圆，看起来仍像贴纸。这次的 3 个透视改动（外圈椭圆化 + 内孔上偏 + 前侧壁带）是**很轻量的几何变化**但能立刻把"贴纸感"换成"实物感"。

试过更激进的方案（画一个完整的 3D 渲染戒指）：在 108×108 viewport 里细节糊掉，不如克制。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（14 MB）

### 手测

1. 装新 APK → 主屏 / Launcher 看新图标：应该是椭圆形戒指 + 前下缘有一条更暗的"侧壁"渐变带 + 上面薄一些下面厚一些（透视）
2. 录入页：
   - 新对话发"我有把 Yonex 4U 球拍" → AI 出草稿，DraftCta 右下角 [采用] [不要] 按钮
   - 点 [采用] → 卡片置灰，下方出现 "✓ 已采用 · N 个字段" 行
   - 继续发"颜色是红色" → AI 应该基于已采用的 Yonex 草稿，只在 specs 里加颜色，不重写品牌型号
   - 再点 [采用] → 第二份草稿采用
   - 点 "手动" 按钮 → 进入 Refine 页，可以看到 brand=Yonex / model 等都已经填好，可以继续编辑
   - 点 "确认收入" → 物品保存到 Room，新建一段对话
3. 反向：发"yonex"出草稿，但点 [不要] → 卡片置灰、strike-through，没有新草稿
4. 历史抽屉切到别的对话再切回来：草稿状态（confirmed / proposed / accepted CTA 列表）应该正确恢复
5. 图鉴页：
   - 点 chip 切品类：滑动条**不动**，只是选中态变（前一个 chip 退、新 chip 进）
   - 回门厅 → 点最后一个品类的 doorway（如"酒水"）→ 跳回图鉴：如果"酒水" chip 不在视野，应该 animate 滚到它；如果在视野，不动
