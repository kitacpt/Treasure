# Cycle 0023 · 草稿镜像 Edit · 聊天图预览 · vision pill 双状态

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 3 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 现在 AI 对草稿的可控性是写死的吗？草稿说了和 Edit 页一样，AI 要填什么就让他填，不要固定模板 | (a) Prompt 改：[`core/ai/Prompts.kt`](android/core/src/main/java/com/treasure/core/ai/Prompts.kt) 放开"hero specs MUST follow category-specific labels" 这条强约束，改成"为本物品挑 4 条最重要的 hero spec，用自然中文 label"，并用每品类几个示例做 hint（不再是必填表）。(b) 草稿页全面镜像 Edit：`AddPreview` 重写，排版与 Edit 一致 — `EditPageHeader` + `HeroAvatarPicker` + 基础（品牌/型号/昵称/简介，`LabeledField`）+ 标签（status `Chip` + 品类 `InlineDropdown`）+ 参数（`DraftSpecs` — 渲染 draft.specs 全部行，可改 label/value/删/加，不做拖动重排）。(c) 数据：`PreviewField` 砍到 5 个一级字段，删掉 Color/AcquiredDate/AcquiredPrice/AcquiredChannel 这 4 个写死的"购入信息"行 — 现在是 spec 列表里的普通行，AI 填了就出现，没填用户自己加。`AddViewModel` 加 `updateDraftSpec` / `addDraftSpec` / `removeDraftSpec`，`commitDraft(status, onSaved)` 接受 status 参数（草稿页 chip 选 OWNED/PARTED/RENTED）。 |
| 2 | 对话页的图片，也可以点开预览，复用之前影集那边的点开预览 | `UserPhotoBubble` 加 `onClick` → `AddRoute` 收到 `(uri)` 后从 `state.messages` 收集所有 `UserPhoto.uri`，找到 tap 的 index，把整套 photos 喂给 `FullscreenPhotoViewer`（与影集同一个 composable）。多张图自动横滑、双指缩放、letterbox 黑边都跟影集一致。聊天图不存 callout — 传空 map + no-op 写回。 |
| 3 | settings 页的多模态支持标记我没看到，而且文案只需要多模态/纯文本就行了，不要加那么多备注 | 之前的 pill 只在 vision-capable 时显示，纯文本模型没任何提示；摘要卡上小灰字也容易漏。**双状态**：vision → terra 色描边 + 浅 terra 底 + "🖼 多模态"；纯文本 → 灰描边 + "纯文本"。两个文案都删掉"录入页可发图给它认 / 不支持发图"备注。摘要卡 Model 行 + 编辑抽屉 Model 输入下方两处都换成同一个 `VisionChip`。 |

## 不在这一刀

- 草稿页拖动重排（Edit 有，草稿页留给细调时进 Item Detail 改）
- 历史时间轴编辑（草稿页只生 ACQUIRED 一条事件，与 cycle 0017 之前一致）
- WebView headless render（cycle 0023 候选）
- 流式输出（用户授权跳过，cycle 0022 已说明）

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
