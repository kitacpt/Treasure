# Cycle 0014 · AI 配置加旋钮 · 修 Kimi tool_choice · IME 遵循输入框

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 3 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | AI 配置应该能调 temperature / thinking | (a) `SettingsStore` 多两个键：`temperature: Double?`（null = 走 provider 默认）+ `thinkingEnabled: Boolean`；(b) Settings 抽屉底部新增 “高阶” 分段：一个 decimal-keyboard FormField 让用户填 0.0–2.0 的温度，留空就走默认；一行 ToggleRow 控制 thinking（terra 圆点滑动开关）+ 副标解释 “Anthropic thinking block · OpenAI o1 · Kimi Thinking · Qwen / Zhipu enable_thinking”；(c) `TreasureApp.aiClient()` 把这俩参数喂给 client 构造；(d) Settings 的 [测试连接] 也按当前 draft 的两参跑 |
| 2 | 接 Kimi 报 `tool_choice 'specified' is incompatible with thinking enabled` | thinking on 时不能再强制点名 tool —— `OpenAiClient.buildPayload` 改成发 `tool_choice = "auto"`，同时透传 `enable_thinking: true`（Kimi / Qwen / Zhipu 都吃这个字段，OpenAI o1 系列直接忽略）；`AnthropicClient` 同样处理 — thinking 开时改 `tool_choice = { type: "auto" }` + 加 `thinking: { type: "enabled", budget_tokens: 2048 }` block；两个 client 的 `parseDraft` 都加文本回退路径：tool_calls 没出来就从 message.content / text block 里抓第一段平衡的 `{ ... }` 反序列化 ItemDraft |
| 3 | 输入法弹起来时遮住对应的输入框 | (a) Manifest 给 MainActivity 加 `android:windowSoftInputMode="adjustResize"`；(b) `AddChat` 的 LazyColumn bottom contentPadding 取 max(navigationBars, ime)；composer 用 `imePadding()` 替代 `navigationBarsPadding()`，IME 起时 bottom padding 从 100dp 收到 8dp（控制岛被键盘盖了，缓冲多余）；(c) `EditScreen` 根 Box 加 `imePadding()`；(d) `CategoryForm` 根 Column 加 `imePadding()`；(e) `SettingsScreen` 的 EditorSheet 加 `imePadding()` 让整张抽屉随键盘上推 |

## 顺手的连带改动

- `AiClient.kt` 加共享 helper `extractFirstJsonObject(text: String): String?` —— 简单括号配对捞出第一段 `{...}` JSON，给两个 client 的回退路径共用
- Prompts.kt 的 system prompt 现在列上 6 个品类（之前漏了 cycle 0011 加的 coffee / wine），spec 标签提示也跟着更新到 cycle 0011 那批（"重量 (g)" / "ISO 范围" / "马力 (PS)" 等）；JSON schema 的 `category.enum` 同步加上 `coffee` / `wine`
- `Item / HeroSpec / HistoryEvent / PhotoCallout`（cycle 0013 标了 @Immutable）继续保留，不动

## 不在这一刀

- 云端 STT (OpenAI Whisper) 兜底
- 多轮 refine 的图片 vision context
- AI 生成博物馆插画
- Settings preset (Xiaomi MiLM) URL 校准
- MigrationTest CI 接入
- Modifier.drawWithCache 给插画录 Picture（cycle 0013 的 @Immutable 已经够好）

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
