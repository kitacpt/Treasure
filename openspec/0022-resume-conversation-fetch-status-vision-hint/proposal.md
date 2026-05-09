# Cycle 0022 · 续上次会话 · fetch 状态可见 · 多模态能力提示

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 4 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 点开 Record 页自动打开的是上次的历史记录，而不是创建一个新记录，不然每次都创建一个新纪录很费解 | `AddViewModel.init` 不再无脑 `newConversation()`，改为 `conversations.observeRecent(1).first()` — 有就 `resumeConversation(latest)` 加载消息；空才 newConversation。从此进 Record tab 直接续聊，历史抽屉里也不再堆一堆空壳 "New entry · HH:MM" |
| 2 | 好像还是不能抓取网页 | (a) UI 可见：发 URL 时立刻在聊天里加一行小字 "正在抓取 jd.com…"，fetch 完成后替成 "✓ 已抓取 jd.com · 1.2K 字" / "⚠ 防爬挡住" / "⚠ 抓取失败 · message"。新增 `AddMessage.SystemNote(text, NoteTone)`，五种 tone（Info/Working/Success/Warning/Error），不入 Room、不喂 AI priorTurns，纯诊断行。(b) charset 稳固：`PageFetcher.decodeWithBestCharset` 当 Content-Type 不带 charset 时，从 body 头部 grep `<meta charset="X">` / `<meta http-equiv="Content-Type" content=...; charset=X>`，命中再重解 — 之前老 GBK 站点直解 UTF-8 出来一堆乱码 |
| 3 | AI 不能进行照片识别，是不是没支持上多模态？能否根据模型信息是否支持多模态显示在 AI 配置页上？ | 加 `data/AiProviderPreset.kt::modelSupportsVision(model: String): Boolean` 启发式判定：含 "vision" / "vl" / "v\d" / glm-?[0-9]v / claude-3* / claude-{sonnet,opus,haiku}* / gpt-4o* / gpt-4-turbo* / gpt-4-vision* / o4* → true；deepseek-* / o1* / o3* / moonshot-v1-{8k,32k,128k} → false。Settings 摘要卡 Model 行下面挂个 "🖼 多模态" pill；编辑抽屉里 Model 输入框下面挂一行小字 "🖼 多模态 · 录入页可发图给它认" / "纯文本模型 · 不支持发图" — 用户改一字、提示就跟着变 |
| 4 | AI 的返回结果能不能流式返回？也是根据模型信息显示在 AI 配置页？ | **不做**。当前所有 extract 走 forced tool-use（`tool_choice="specified"`），整段 thinking + tool-call 一次到位返回；流式拆出来用户看到的依然是先一片空白几秒再 "好。我已经替你..." 出现，跟非流式无差。复杂度（增量 SSE 解析 + 多 provider 适配 + tool-call delta 拼接）不抵收益。用户也讲了 "3 和 4 我不太确定，你自己定夺" |

## 三件事的取舍

### 续上次会话 vs 进入页就新建

新建对话的好处：每次 Record 进入是干净起点。坏处：每按一次底部 RECORD tab 都生一段空壳。用户痛点很明确 — 不想被废空壳淹没。续上次后，新建走「历史抽屉 → 新对话」按钮，主动而非默认。

### Fetch 状态显示成 SystemNote 而不是 Assistant 气泡

如果当成 Assistant 文本写进 Room：(a) 永久挂在历史里，重读杂；(b) AI 看到 "我已经替用户抓了 jd.com" 又会被 priorTurns 误导，下一轮可能复读。SystemNote 故意短命 + 不入持久层 + 不喂 AI，纯给人看。重开历史时它就消失了，没问题。

### 多模态 chip 用启发式而不是 provider catalog

理论上最稳是：每个 provider 拉个 catalog API 列出 model -> capabilities。但 provider 没统一 catalog 接口，OpenAI 有 /v1/models 但不带 capability 字段；Anthropic 干脆没 list；国产几家全 ad-hoc。用户还可能填一个 BYO endpoint 上的私有 finetune。靠 model 名字符串模式匹配是工程上最务实的：99% 命中、错了无非少显示一个图标。

## 不在这一刀

- 流式输出（见上）
- WebView headless render fallback（拼多多真页面）
- vision 真做了图片发送之后的 UX（已经在 cycle 0007 做完，AddChat.UserPhoto + sendPhoto 走 `extractItemDraft(imageJpegBytes=...)`，本轮只做能力提示）

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
