# Cycle 0015 · Thinking 自动嗅探 · 测试结果规范化

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 3 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | thinking 开关副标太啰嗦，就写 "Enable thinking" | `SettingsScreen.kt` 的 ToggleRow 删掉 sub 副标参数；`Enable thinking` 一行结束 |
| 2 | Kimi 还是报 `tool_choice 'specified' is incompatible with thinking enabled` | 之前 cycle 0014 只在用户手动 toggle 时才发 `tool_choice: "auto"`。但 Kimi 的 `kimi-thinking-preview` / `kimi-k2-*-thinking` / OpenAI 的 `o1` / `o3` 系列 *服务端* 就是 thinking 模式，跟我们客户端 toggle 没关系。新建 `OpenAiClient.isImplicitThinkingModel`，模型名包含 `thinking` / `o1` / `o3` 前缀就视作 thinking；effectiveThinking = toggle ∨ implicit。同时把 `enable_thinking: true` 限制只发给 dashscope / open.bigmodel.cn 这种顶层布尔字段的厂商（Qwen / 智谱），不再发给 Kimi / OpenAI，避免引发 "未知字段" 类的额外问题 |
| 3 | 测试错误信息要规范化（类型 · 详情）+ 出错后自动让用户看到 | (a) `TestStatus.Failed(message: String)` 拆成 `Failed(kind: String, detail: String)`；新建 `Throwable?.toTestFailed()` 把 raw exception 归到 4 类：`HTTP NNN` 解 `error.message` / `网络` (UnknownHost / Connect / SocketTimeout) / `TLS` / `配置`；(b) `TestStatusLine` 失败态从一行字升级成 terra 描边的小卡片：第一行 `× HTTP 400`，下面一行人话 detail；(c) EditorSheet 持有自己的 `rememberScrollState()`，testStatus 变 Ok / Failed 时 LaunchedEffect 自动 `animateScrollTo(maxValue)`，结果立刻可见 |

## 不在这一刀

- 云端 STT 兜底 / image vision 多轮 / AI 生成插画 / Xiaomi preset 校准 — 仍在 cycle 0016 候选清单
- thinking model 自动嗅探只看模型名子串。如果用户 model 字段填了完全 custom 的名字（比如 `private-thinking-7b`）也会触发；这是我们想要的行为

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
