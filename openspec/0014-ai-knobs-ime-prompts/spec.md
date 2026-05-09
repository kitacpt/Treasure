# Cycle 0014 · 验收

## AI 配置抽屉

- 摘要卡 → 调整 → 抽屉，滚动到底部能看到一个 “高阶” 分段（细线 + label 风）：
  - **Temperature** 输入框：decimal 键盘，留空 = 走 provider 默认，超出 0.0–2.0 自动 clamp
  - **开启 thinking** ToggleRow：点击行任意处切换；副标说明各 provider 对这个开关的解读
- [测试连接] / [保存] 两个按钮都会用当前 draft 的 temperature + thinking 跑 / 落库

## AI 客户端行为

### `AnthropicClient(thinkingEnabled = true)`

- payload 多 `thinking: { type: "enabled", budget_tokens: 2048 }`
- `tool_choice` 从 `{ type: "tool", name: ... }` 改成 `{ type: "auto" }`
- `max_tokens` 从 1024 调到 4096（thinking 要更多 buffer）
- 解析时若没 tool_use block，回退抓 text block 里的 JSON

### `OpenAiClient(thinkingEnabled = true)`

- payload 多 `enable_thinking: true`（Kimi / Qwen / Zhipu 识别；OpenAI 自家无此字段，忽略）
- `tool_choice` 从 `{ type: "function", function: { name: ... } }` 改成 `"auto"`（这是 Kimi 报错的那行）
- 解析时 tool_calls 没来就从 `message.content` 抓第一段 `{...}` 反序列化

### temperature

两端都接 `temperature: Double?` 构造参数，null 时不发字段（Provider 默认）。Settings UI 限制在 0.0–2.0。

## IME 遵循

- 录入页：键盘弹起 → composer 自动上浮（imePadding）；消息列表 bottom contentPadding 跟着扩展，最后一条消息能完整露出在 composer 上方
- 编辑屏 / 手动录入屏：根容器加 `imePadding()`，所有 LabeledField 在键盘起时不会被挡（adjustResize + imePadding 双保险）
- Settings 抽屉：`imePadding()` 让整张抽屉随键盘上推
- AlertDialog 自带 IME 处理，无需改

## Prompts

- system prompt 列 6 个品类 + 每类的 hero spec 标签
- JSON schema `category.enum` 同步加 coffee / wine

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
