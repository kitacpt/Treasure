# ADR-0004 · 用户自带 AI key（BYO Key）

- **状态：** Accepted（实现推迟）
- **日期：** 2026-05-06
- **生效 cycle：** 0001 之后某个 cycle（具体编号待定）

## 背景

原型的设置页（参见 [`../../prototype/project/Treasure.html`](../../prototype/project/Treasure.html) 的 `a-settings` 画板）已经把 AI 集成的形态钉死了：

> AI 服务（Anthropic / 模型 / API Key）+ 默认插画风格

设计对话也明确："录入页：对话式 LLM 输入"、"AI 统一图风"。

## 决定

**用户自带 API key（Bring-Your-Own-Key, BYOK）**。Treasure 不代理 AI 流量，不做我们这边的"赠送 quota"，不收费。

具体形态：

- 设置页让用户填 `provider + model + api_key`
- provider 至少支持：**Anthropic Claude**（默认）、OpenAI、自定义 OpenAI-兼容 endpoint
- key 存在 [EncryptedSharedPreferences](https://developer.android.com/topic/security/data) 里
- 设备**直连** provider（HTTPS），不经过我们的服务器
- 没有 key → AI 相关功能（对话录入、AI 生成插画）静默灰掉，**不影响**主流程（浏览/手动录入）

## 为什么不代理

- Treasure 是 local-first，加一层服务器代理违背了 ADR-0003 的精神
- 代理意味着我们要承担 AI 调用的费用、speed-limit、抗滥用 …… 一个个人爱好工具不该背这些
- 用户自己的 key 直接调，开销透明、记账透明

## 安全

- key 永远不离开本机（除 HTTPS 调 provider 外）
- 同步给后端的数据不包含 key
- 错误日志/崩溃报告里 redact 掉 `Authorization` 头
- 设置页提供"清除 key"的按钮

## 实现门面

`core/ai/AiClient.kt`：

```kotlin
interface AiClient {
  suspend fun chat(messages: List<Message>): Result<String>
  suspend fun visionExtract(image: ByteArray, prompt: String): Result<ItemDraft>
  suspend fun generateIllustration(prompt: String, style: IllustrationStyle): Result<ByteArray>
}
```

cycle 0001 不实现这个接口。先把接口钉好，配合 ADR-0005 的"用户新增的物品先用占位"。

## 相关

- [ADR-0003 · Local-first](0003-local-first-with-optional-sync.md)
- [ADR-0005 · 博物馆插画策略](0005-museum-illustration.md)
