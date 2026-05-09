# Cycle 0023 · spec

## 1. 草稿页镜像 Edit

排版（自上而下）：

1. EditPageHeader：左 [取消]，中 "Refine · {品类中文}"，右 [确认收入]
2. HeroAvatarPicker（read-only — 录入时还没真照片，只展示模板插画）
3. SectionDivider "基础" → 4 行 LabeledField（品牌 / 型号 / 昵称 / 简介）
4. SectionDivider "标签" → 状态 chip（Owned / Parted / Rented）+ 品类 InlineDropdown
5. SectionDivider "参数 · AI 填的字段" → DraftSpecs：每行两个 InlineField（label / value）+ 删除按钮，最后是 "+ 加一行参数"

具体行为：

- AI 填的每条 spec 直接显示，没有"颜色/入手日期/入手价格/入手渠道"4 行写死的占位
- 用户改 label / value 实时回写到 `vm.state.draft.specs`
- 删行立即移除
- 加新行 push 一条 `HeroSpec("", "")` 到末尾
- status 默认 OWNED，用户改了在 `[确认收入]` 时传给 `commitDraft(status)`
- 品类改了走 `vm.updateDraftField(Category, nameZh)`，会重算 hero 模板和插画

## 2. AI prompt 放开

`SYSTEM_PROMPT`：

- 旧："hero specs MUST follow category-specific labels: {6 个品类的固定 4-tuple}"
- 新："为本物品挑 4 条最重要的 hero spec，用自然中文 label。下面只是示例不是必填表"
- 仍保留：specs 总数 4-10，第一组 4 条作为 hero，剩下 tail；以及 brand/model/oneLiner/category 的指导

## 3. 聊天图全屏预览

- 长按某 UserPhoto 气泡 → 起 SelectionContainer 文本菜单（与 cycle 0021 一致）
- **单击** UserPhoto 气泡 → 打开 `FullscreenPhotoViewer`
- viewer 接收 `photos = state.messages.filterIsInstance<UserPhoto>().map { uri.toString() }`，`initialIndex = indexOf(tappedUri)`
- 多张图可横滑切换，双指缩放，letterbox 黑边 64dp（与影集一致）
- 不存 callout（聊天图不持久化标注）— 传 `emptyMap()` + no-op `onSetCallouts`
- ← 关闭

## 4. Vision pill 双状态

|  | Vision-capable | Text-only |
|---|---|---|
| 文案 | "🖼 多模态" | "纯文本" |
| 描边色 | terra 0.55α | line |
| 底色 | terra 0.10α | 透明 |
| 文字色 | terra | sub |

显示位置：
- Settings 摘要卡 Model 行：在 model 名下方挂 chip（model 非空时一定显示）
- Settings 编辑抽屉 Model 输入下方：实时显示 chip（输入空 → 沿用 preset.defaultModel 判定）

`modelSupportsVision(model)` 启发式不变（cycle 0022 加的）。

## 5. Out of scope

- 草稿页拖动重排（Edit 有，草稿页留给细调时进 Detail 改）
- chat 图存 callout（聊天图是临时 hint，影集才是永久收藏）
- 流式输出 / WebView headless
