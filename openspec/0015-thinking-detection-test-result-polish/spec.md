# Cycle 0015 · 验收

## ToggleRow

- 抽屉里的 thinking 那一行只有一行字 "Enable thinking" + 右侧 terra 圆点滑动开关，不再有副标解释

## OpenAiClient thinking 模式

- 触发 thinking-mode payload 的两个条件取或：
  1. 显式 `thinkingEnabled = true`（用户在抽屉里打开了开关）
  2. 模型名命中 `thinking` 子串（`kimi-thinking-preview`, `kimi-k2-thinking-*`）或以 `o1` / `o3` 起头（OpenAI o-series）
- 命中后：
  - `tool_choice` = `"auto"`（不再强制点名工具）
  - 仅当 baseUrl 是 `dashscope.aliyuncs.com` / `open.bigmodel.cn` 才额外发 `enable_thinking: true`；OpenAI / Kimi / DeepSeek 不再发这个字段
- 不命中时跟以前一样：`tool_choice: { type: "function", function: { name: ... } }`

## 测试连接结果显示

- 失败态 UI：terra 6% 透明 fill + 0.5dp terra 描边 + 圆角 2dp 的小卡片
  - 第一行：`× <kind>` (terra labelMedium)
  - 第二行：`<detail>` (ink bodyMedium)
- `kind` 取值：
  - `HTTP NNN`（NNN 是状态码；detail 是 `error.message` 或前 220 字 raw body）
  - `网络`（UnknownHostException / ConnectException / SocketTimeoutException 都归这）
  - `TLS`（SSLHandshakeException）
  - `配置`（API key 空 / 必填 base URL 空）
  - `错误`（兜底）

## 自动滚动

- testStatus 从 Running 转 Ok / Failed 时，EditorSheet 的 verticalScroll `animateScrollTo(maxValue)`，状态行立刻可见
- 用户在抽屉中部点击 [测试连接]，结果出来后无需自己再下滑

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
