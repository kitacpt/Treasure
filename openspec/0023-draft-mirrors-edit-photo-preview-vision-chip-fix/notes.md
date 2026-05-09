# Cycle 0023 · notes

## 文件改动

- `core/.../ai/Prompts.kt`
  - SYSTEM_PROMPT：放开"hero specs MUST follow category-specific labels"，改成示例 + "为本物品挑最重要的 4 条"
- `app/.../ui/add/AddViewModel.kt`
  - `PreviewField` 砍到 5 个一级字段（Category/Brand/Model/Nickname/OneLiner）
  - 删除 `previewRowsFor` / `PreviewRow` / `Confidence`（草稿页不再用统一 row 模型，直接拉 draft 字段）
  - `applyFieldEdit` 简化（去掉 Color/AcquiredDate/AcquiredPrice/AcquiredChannel 分支）
  - 新增 `updateDraftSpec(idx, spec)` / `addDraftSpec()` / `removeDraftSpec(idx)`
  - `commitDraft(status: ItemStatus = OWNED, onSaved)` — 接 status 参数
  - `fieldCount` 改成 `count(brand/model/nickname/oneLiner/category 非空) + count(specs 非空)`
- `app/.../ui/add/AddPreview.kt`
  - 重写：EditPageHeader → HeroAvatarPicker → 基础 LabeledField × 4 → 标签 (Chip status + InlineDropdown 品类) → 参数 DraftSpecs
  - 删除 ConfidenceLegend / LegendItem / ConfidenceDot / DraftFieldRow
  - 新 `DraftSpecs` 私有 composable（编辑/添加/删除，不 reorder）
  - 引入 `com.treasure.ui.edit` 的 `LabeledField` / `FieldLabel` / `Chip` / `InlineField` / `DeleteIcon` / `AddRowButton`（这些已是 internal，同 `:app` module）
  - signature 加 `onUpdateSpec` / `onAddSpec` / `onRemoveSpec`，`onConfirm: (ItemStatus) -> Unit`
- `app/.../ui/add/AddRoute.kt`
  - 新 `ChatPhotoPreview(photos, initialIndex)` data class
  - 新 `var photoPreview by mutableStateOf<ChatPhotoPreview?>(null)`
  - AddChat 加 `onPreviewPhoto = { uri -> ...indexOf in messages... }`
  - AddPreview 接新签名（spec 编辑回调 + status 进 onConfirm）
  - 末尾加 photoPreview overlay 渲染 `FullscreenPhotoViewer(photos, initialIndex, emptyMap(), no-op, onClose)`
- `app/.../ui/add/AddChat.kt`
  - AddChat signature 加 `onPreviewPhoto: (Uri) -> Unit`
  - MessageRow signature 加 `onPreviewPhoto`
  - `UserPhotoBubble(uri, onClick)` — Box 加 `.clickable(onClick)`
- `app/.../ui/settings/SettingsScreen.kt`
  - VisionChip 改双状态版（vision = terra accent，纯文本 = 灰 outline）
  - ModelRow：model 非空就一定挂 chip（之前只在 vision 时挂）
  - ModelCapabilityHint 简化：直接 spawn 同一个 VisionChip，不再写一行小字"录入页可发图给它认"

## 设计取舍

### 为什么草稿页不做拖动重排

Edit 页里 ReorderableSpecs 是 91 行的代码（拖动 + hero/tail 分隔 + 重排 callback），逻辑相当独立。草稿页是"快速进入图鉴"的中转，用户在这一步主要校对 AI 填的内容；想细调 hero 顺序的可以确认收入后进 Detail → Edit 调。不重复 91 行代码、不分裂 UX 出两套不同体验，是这个 cycle 想优化的方向。

如果将来用户反馈"我经常在草稿页就想拖动重排"，再把 ReorderableSpecs 抽到 components/ 里两边共用。

### 为什么 chat 图不存 callout

影集照片是物品的永久收藏，长按加注是一种"为这件东西的某个细节做笔记"。聊天里发的图是一次性的——给 AI 看一眼，AI 提取完信息这张图就完成使命了。如果让用户在 chat 图上加 callout：(a) 存哪？conversations 表加列？(b) 用户预期不一致，影集 callout 跟物品挂钩，chat callout 跟对话挂钩，两套数据生命周期不同。

干脆共用 viewer 的预览/缩放能力，不开放编辑入口（传 emptyMap + no-op）。要保存细节就把这张图拍成物品的影集照片。

### Vision pill 默认 terra 色而不是 ink

pill 是给 vision-capable 这种"积极信号"用的——多模态相对稀缺、是用户该知道的好消息。terra 是 brand accent，色泽够温暖也够显眼，比中性 ink 更能引起注意。纯文本走 sub 灰色 + 透明底——明显的"非主角"。

### Prompt 改动幅度

之前的 hero spec 表是死的，AI 看到 badminton 就照搬 [重量, 平衡点, 中杆硬度, 穿线磅数] 4 字段，对羽毛球鞋 / 球本身就生硬（鞋子不需要"中杆硬度"）。现在改成"挑 4 条最重要的"，AI 会按物品本身决定——羽毛球鞋会挑 [鞋码, 鞋面材质, 鞋底, 重量]。已知风险：badminton 大类下不同子物品 hero spec 标签不再统一，Grid 卡片视觉一致性会差一点点；可接受。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测

1. 录入页发条文字 → AI 出草稿 → 点 DraftCta 进 Refine 页：
   - 应看见 EditPageHeader (取消 / Refine · {品类} / 确认收入)
   - 头像选择器（template 插画）
   - 基础 4 行 LabeledField，AI 填的 brand/model/nickname/oneLiner 都在
   - 标签：status chip 默认 Owned + 品类 dropdown
   - 参数：AI 填的所有 spec 行都在（不再是固定 4 行"颜色/入手日期/入手价格/入手渠道"）
   - 改 status → Parted → 确认收入 → 新 item 应是 PARTED 状态
2. 录入页发条带照片 → 单击聊天里那张照片缩略图 → 应弹全屏 viewer，黑底，← 可关闭，左下方写 1/N
3. 多次发图后再点其中一张 → 应能横滑切换所有 chat 图
4. Settings 摘要卡：
   - 当前 model 多模态 → 模型名下挂 terra 色 "🖼 多模态" pill
   - 改 model 成纯文本（如 deepseek-chat） → pill 变灰色 "纯文本"
   - 编辑抽屉 model 输入下面同步显示
5. 开新对话发"我有把 Yonex 4U 球拍" → AI 草稿 specs 应该不再死板地是 [重量, 平衡点, 中杆硬度, 穿线磅数]，而是 AI 自己挑最相关的 4 条 hero
