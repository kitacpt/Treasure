# Cycle 0014 · notes

## 文件改动一览

主要修改：

- `core/.../ai/AiClient.kt` — 加 `extractFirstJsonObject` 共享 helper
- `core/.../ai/AnthropicClient.kt` — 构造加 temperature / thinkingEnabled；buildPayload 处理 thinking + tool_choice 切换；parseDraft 加文本回退
- `core/.../ai/OpenAiClient.kt` — 同款；额外发 `enable_thinking: true`
- `core/.../ai/Prompts.kt` — 6 品类 + 新 hero spec 标签 + enum
- `app/.../data/SettingsStore.kt` — `temperature` / `thinkingEnabled` 两个 prefs 键
- `app/.../TreasureApp.kt` — `aiClient()` 把两参穿进构造
- `app/.../ui/settings/SettingsViewModel.kt` — saved / draft 两份新字段；setters；save / testConnection 都消费
- `app/.../ui/settings/SettingsScreen.kt` — EditorDrawer / EditorSheet 多两个回调；UI 多 “高阶” 分段 + ToggleRow + temperature FormField；加 SectionLabel + ToggleRow 两个 helper；EditorSheet 加 imePadding
- `app/src/main/AndroidManifest.xml` — MainActivity `windowSoftInputMode="adjustResize"`
- `app/.../ui/add/AddChat.kt` — LazyColumn bottom 取 max(navBars, ime)；composer 改 imePadding；IME 起时 bottom 收到 8dp
- `app/.../ui/edit/EditScreen.kt` — root Box 加 imePadding
- `app/.../ui/add/CategoryForm.kt` — root Column 加 imePadding

## 设计取舍

### temperature 用文本框而不是 slider

试过 slider，但 0.0–2.0 这种连续区间用 slider 步进调起来不准；用户更可能想填 "0.20" 或 "0.70" 这种特定值。文本框 + decimal 键盘 + 空 = 默认，是最少惊讶的姿势。clamp 在 save 时做，UI 不打断输入。

### thinking 开关到底干啥

不同 provider 对 "thinking" 的语义不一样，但用户视角应该是同一个开关：

- **Anthropic** Claude 4 / 4.5 系列支持 `thinking: { type: enabled, budget_tokens: ... }` block，会先输出一段思考再给最终答案。代价：响应变长。开启时 `tool_choice` 不能强制点名，必须 `auto`。
- **OpenAI** o1 / o3 系列内置思考，没有显式 toggle 字段；但它们的 API 也只允许 `tool_choice: auto`。我们发 `enable_thinking: true` 给它，OpenAI 直接忽略；只需改 tool_choice 行为。
- **Kimi (Moonshot)** k2-thinking / kimi-k2-thinking 模型走 `enable_thinking: true` + `tool_choice: auto`，不然就是用户看到的那条报错 `tool_choice 'specified' is incompatible with thinking enabled`。
- **Qwen / Zhipu** 都吃 `enable_thinking: true`。

所以 toggle 的 effect 在我们的客户端层面统一成两件事：
1. payload 加 `enable_thinking: true`（Anthropic 用 thinking block，OpenAI 兼容侧用顶层字段）
2. `tool_choice` 不再点名，让模型自由选

回退解析：thinking 模式下 model 可能直接把 JSON 写在 content 里（不调 tool）。`extractFirstJsonObject` 简单括号配对从文字里捞出第一段平衡的 `{...}` 给 ItemDraft 反序列化。这个函数小但能稳稳处理 "前面思考一通然后扔个 JSON" 的常见形态。

### tool_choice "auto" 的副作用

非 thinking 模式我们仍强制点名（`tool` / `function`），保证模型一定调那个 schema-checked 的工具。thinking 模式下让 auto，模型有可能跑题输出无 tool 的 text。回退路径就是兜底 — 但不保证 100% 成功。如果实在失败，error message 会带 200 字 preview，用户能看到。

### imePadding vs adjustResize

两个加一起看似冗余，其实互补：

- `windowSoftInputMode="adjustResize"` 让 Android window 自身在键盘弹起时 *收缩高度*，配合 edge-to-edge 时这一行 manifest 不写就失效。
- `Modifier.imePadding()` 在 Compose 层给某个容器加上等于 IME inset 的 bottom padding，让浮在底部的 composer / sheet 跟着上移。

只有 adjustResize 时，一些悬浮元素（composer）不会动；只有 imePadding 时，window 不收缩，键盘可能盖一整块 LazyColumn。两个一起：window 收缩 + 关键浮元素 imePadding，体感就对了。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测要点

- Settings → 调整 → 抽屉滚到底，能看到 "高阶" 区段；输入 0.30 / 0.70 保存，再次打开应预填回去
- 切到 Kimi · Moonshot preset，把 thinking 开关打开，[测试连接] 应跑过（之前那条报错应该不再出现）
- Anthropic 也开 thinking 测一下：响应可能稍慢，但能拿到 ItemDraft
- 录入页点输入框，键盘弹起：composer 跟着上浮，最近的消息能露出来
- 编辑屏滚到底部任意 LabeledField 点击：键盘起后该字段不被挡
- Settings 抽屉里点 API Key 输入：抽屉随键盘上推，[保存] / [测试连接] 不被挡
