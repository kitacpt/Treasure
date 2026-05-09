# Cycle 0015 · notes

## 文件改动一览

主要修改：

- `core/.../ai/OpenAiClient.kt` — 加 `isImplicitThinkingModel` / `effectiveThinking` / `supportsEnableThinkingFlag` 三个 derived 属性；buildPayload 用 effectiveThinking 决策；enable_thinking 仅 Qwen / 智谱发
- `app/.../ui/settings/SettingsViewModel.kt`
  - `TestStatus.Failed(message)` → `Failed(kind, detail)`
  - 加 `Throwable?.toTestFailed()` 归类 + 解 `error.message`
  - 加 `extractProviderErrorMessage(body: String)` JSON 浅解
  - testConnection() / pre-validate 改用新 Failed 构造
- `app/.../ui/settings/SettingsScreen.kt`
  - ToggleRow 去掉 sub 参数 + 调用点同步
  - TestStatusLine 失败态改成 terra-tinted 小卡片，两行（kind / detail）
  - EditorSheet 自带 scrollState；LaunchedEffect(testStatus) 自动滚到 maxValue

## 设计取舍

### 隐式 thinking 嗅探 vs 显式开关

cycle 0014 我留了一个 `thinkingEnabled` 旋钮，假设用户会主动打开。但 Kimi 的痛点恰好是 *用户不知道* 自己装了个 thinking 模型 —— 直接踩坑。

cycle 0015 的策略：
- 旋钮还在（强制开启，对所有 provider 生效）
- 加一层 model-name 嗅探：模型名带 `thinking` 或前缀 `o1` / `o3` 自动按 thinking 模式构 payload

判定可叠加（OR）：toggle 开 ∨ model 名命中 = effectiveThinking。代价是用户给模型起了个意外含 `thinking` 的本地名字会 false-positive，但这种情况几乎只出现在自家服务，影响小。

### enable_thinking 不再撒网

cycle 0014 我对所有 OpenAI 兼容 provider 都发 `enable_thinking: true`。这有两个隐患：
- OpenAI / Kimi 不识别这字段，可能直接 400（部分 provider 严格拒绝未知字段）
- 即使 provider 容忍，也是冗余信息

cycle 0015 改成精确投放：仅 `dashscope.aliyuncs.com` (Qwen) 和 `open.bigmodel.cn` (智谱) 才发。Kimi / OpenAI / DeepSeek 用模型名隐式控制 thinking。

### 失败态 UI 升级

之前一行字 `× HTTP 400: {"error":{"message":...}}`，长得吓人 + 信息密度低。

新版：terra-tinted 小卡片，第一行类型，第二行人话。HTTP body 优先尝试解 `error.message`（OpenAI / Kimi / Anthropic 这一层都通用），解不开再回退原文截断。

`Failed(kind, detail)` 比 `Failed(message)` 多了一层结构，但 UI 更清晰。

### 自动滚动

LaunchedEffect 监听 testStatus，触发 `animateScrollTo(maxValue)`。简单稳。Idle 和 Running 不滚，避免点 [测试连接] 那一刻还没出结果就跳。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测

1. Settings → 调整 → Provider Kimi · Moonshot
2. Model 字段输入 `kimi-thinking-preview`（或 `kimi-k2-0905-preview`），不动 thinking 开关，[测试连接]
3. 应该不再报 `tool_choice 'specified'` —— 可能拿到 ✓ 连接成功，或者拿到一个其他 HTTP 400（被解出来的人话 message）
4. Model 改回 `moonshot-v1-8k`，关 thinking，[测试连接] 应正常 — 走老的 specified tool_choice 路径
5. 故意填错 API key，[测试连接] → 失败卡片显示 `× HTTP 401` + provider 给的 message
6. 拔网线，[测试连接] → `× 网络` + 异常信息
7. 测试完不论成功失败，结果应自动滚到底，无需手动下滑
