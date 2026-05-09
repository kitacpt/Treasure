# Cycle 0021 · 对话框复制粘贴 · fetch 防爬识别

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 2 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 对话框需要能复制粘贴 | AddChat 的 LazyColumn 外面包一层 `androidx.compose.foundation.text.selection.SelectionContainer`：长按任意助手 / 用户 / 语音转写 / 草稿副标气泡里的文字 → 起选择手柄 → 系统菜单复制 / 全选；Composer 那边 BasicTextField 自带 paste（长按弹粘贴菜单），不动 |
| 2 | AI 说他无法访问外部链接；拼多多链接被防爬挡了 | `PageFetcher.fetchText` 从返回 `String?` 改返回 `FetchResult` 三态：`Success(text)` / `Blocked(host, reason)` / `Failed(host, message)`。`detectBlock(text, host)` 启发式识别防爬：(a) strip 后字数 < 200 直接当壳页；(b) 命中 "请登录 / 验证 / captcha / 打开拼多多 / 打开淘宝 / 打开京东 / 在 app 内打开 / 访问受限 / 活动已结束" 等关键词且总长 < 1000 字 → 当 app gate / login wall。`AddViewModel.sendText` 三种结果分别拼三种 prompt：成功 = `[页面摘要]`；blocked / failed = 系统提示明确告诉 AI "客户端尝试过，被网站挡了 / 网络错了，**不要**回 '我无法访问外部链接'，仅基于分享文字判断 + 调 fill_item_draft 把能填的填上"，杜绝 AI 模板回复式 "I cannot browse" |

## 关于拼多多专门的解法

拼多多分享链接（`mobile.yangkeduo.com` / 短链 `p.pinduoduo.com`）几乎一定会落到 "请打开拼多多 App" 的壳页 — 它专门反爬。我们识别到这种壳页后，**不再尝试 fetch 内容**，转而：

- 提示 AI "壳页拦住了，仅基于分享文字判断"
- 用户分享拼多多商品时，原文里通常有 "9.9包邮 XXX商品 复制此消息打开拼多多" 这种自带的商品名描述。AI 直接从那段抽。

如果用户希望真拿到拼多多商品页，唯一稳妥的路是 mini headless WebView（用本地 WebView 真渲染一次）— 工程量 + 启动延迟都很大，cycle 0022+ 候选。

## 不在这一刀

- WebView headless 渲染 fallback
- AI 生成插画 / Whisper 兜底 / preset 校准 / MigrationTest CI

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
