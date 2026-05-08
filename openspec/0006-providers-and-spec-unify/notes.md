# Cycle 0006 · 工作笔记

## Provider 抽象

- **OpenAI vs Anthropic message API 差异**：
  - 路径：`/v1/chat/completions` vs `/v1/messages`
  - Auth：`Authorization: Bearer ...` vs `x-api-key: ...` + `anthropic-version`
  - System prompt：OpenAI 是 `messages[0]` role=system；Anthropic 是顶层 `system` 字段
  - Vision：OpenAI 用 `image_url` 类型（data URL）；Anthropic 用 `image` 类型（base64 source 对象）
  - Tool use：OpenAI 是 `tools[].type=function` + `tool_choice.type=function`；Anthropic 是 `tools[]` 直接 + `tool_choice.type=tool`
  - Tool call response：OpenAI `choices[0].message.tool_calls[0].function.arguments`（JSON 字符串）；Anthropic `content[].type==tool_use, .input`（已是 JSON 对象）
- 共用：tool schema (input_schema / parameters)、system prompt
- 自定义 provider = 用 OpenAI client 但换 baseUrl。OpenAI v1 schema 是事实标准，DeepSeek / Together / vLLM 等都支持。

## 参数统一

把 `heroSpecs: List` + `specs: Map` 合成一个 `specs: List`：

- 改动多：Item / ItemEntity / SeedItems / ItemDraft / AnthropicClient / OpenAiClient / EditScreen / DetailScreen / CategoryForm / AddViewModel
- Schema bump v4 → v5。这是第 4 次 destructive。**cycle 0007 必须切真 migration。**
- 计算属性 `Item.heroSpecs` (= `specs.take(4)`) 让现有 Detail 代码大部分不动

## 拖动重排

手动实现而非引库（`sh.calvin.reorderable`）：

- 每行 `≡` 句柄上挂 `pointerInput { detectDragGesturesAfterLongPress(...) }`
- 拖动行 `graphicsLayer.translationY = dragOffset`，加 `shadowElevation` 飘起来
- 其它行 `graphicsLayer.translationY = 计算出的 "make-room" 偏移`
- 落点用 `(originalIdx + dragOffset / rowHeightPx).roundToInt()`
- 不完美：跨多行拖动时，中间行的位移瞬间发生不连续（不是流畅过渡）。这个版本"够用"。
- 真要丝滑：换 `sh.calvin.reorderable:reorderable:2.4.0`，下一刀考虑

## CategoryForm 与 EditScreen 对齐

代码 DRY 没做 — 两个文件各自定义了一套 `Section / LabeledField / Chip / FormField`。**视觉一致是因为按相同的尺寸/颜色/字号写的**，不是因为代码共享。

下一次有第三个表单时，把这些抽到 `app/ui/forms/FormPrimitives.kt`。

## 外层 AddRoute 留空

按用户要求"留空，要重新画"。方案：

- Header 保留
- 中间整片留空，italic "录入页交互重新设计中"
- 底部"临时入口" 4 颗品类 chip，让 CategoryForm 还能测；新交互上线后这 4 颗删除

删了 `AiChatPanel.kt` 和 `CategoryGlyph.kt`（不再被引用）。AI 录入路径目前**暂时不可达**——用户拿到新设计后我们再接回去。`AiClient` / `AnthropicClient` / `OpenAiClient` 全部仍然在 `:core` 里活着、可工作。

## 给下一个 agent

cycle 0007 候选（按优先级）：

1. **真 schema migration**：v1→v5 全部，加 MigrationTest，删 `fallbackToDestructiveMigration()`。**这是最大的债。**
2. **新录入页交互**（等用户给设计稿）+ AI 入口接回
3. **拍照 / 多选照片**
4. AI 生成博物馆插画 / callout 标注 / 全屏看图
