# Cycle 0020 · 影集放大真能滑 + 留白 · 分享链接真 fetch 页面

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 3 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 影集放大预览还是不能左右滑 | cycle 0019 我换的 `Modifier.transformable(lockRotationOnZoomPan = true)` 实际上还是吃单指 drag。`lockRotationOnZoomPan` 只是 "zoom 起来后再不接收 rotation"，不影响单指 pan。本刀换成手写 `awaitEachGesture { ... }` 在每次 pointerEvent 看：pressed pointer count == 1 + scale == 1 时**完全不消费**，让外层 HorizontalPager 自然吃 swipe；pressed >= 2 → 处理 zoom + pan；pressed == 1 + scale > 1 → 平移 |
| 2 | 影集放大上下要留白 | 之前 HorizontalPager `.fillMaxSize()`，竖图把 ← back / 计数 / 底部提示全压在图上，浅色照片上看不见文字。本刀给 pager 上下各留 64dp 黑边（`.statusBarsPadding().navigationBarsPadding().padding(top = 64.dp, bottom = 64.dp)`），图片 letterbox 在中间，控件单独躺在黑背景上 |
| 3 | 真去 fetch URL 抓京东 / 淘宝商品页 | (a) 新建 `core/.../web/PageFetcher.kt`：OkHttp 装移动 Chrome UA，最多读 96KB，剥 `<script>` / `<style>` / `<noscript>` 后整段 strip tag；提取 `<title>` / `og:*` / `description` meta 当摘要前缀；body 折叠空白后截到 4000 字符；(b) helper `firstUrlIn(text)` 从自由文本里抓 http(s) URL；(c) `TreasureApp.pageFetcher` 单例；(d) `AddViewModel.sendText` 检测到 URL 时先 `pageFetcher.fetchText(url)`，把 "原文 + [页面摘要] + 提示" 拼成新 prompt 喂 AI；fetch 期间 `busy=true` 让 typing indicator 自然显示；fetch 失败就回退原文 |

## 数据 / Schema

- 无 schema 变化。`PageFetcher` 是无状态 IO，不持久化任何东西

## 不在这一刀

- AI 生成插画 / Whisper 兜底 / preset 校准 / MigrationTest CI
- JD / 淘宝特定 DOM 抽取（不同站结构差异大 + 反爬，不值得 hardcode）— 当前是通用 strip + AI 智能识别

## 安全考量

- PageFetcher 走的是设备直连 web，不经过任何 proxy。这跟 [ADR-0004](../../docs/adr/0004-byo-ai-key.md) 的 "AI 设备直连 provider" 立场一致 — 用户的浏览数据不进我们的服务器
- fetch 不带 cookie / 不带任何已登录 session，纯匿名抓取；因此抓不到登录墙背后的页面（这是预期，安全 > 完整）
- 任何 fetch 错误都吞回 null，回退到只用分享文本，主流程不受影响

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
