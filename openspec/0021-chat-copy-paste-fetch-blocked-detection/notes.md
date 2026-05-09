# Cycle 0021 · notes

## 文件改动

主要修改：

- `app/.../ui/add/AddChat.kt` — LazyColumn 包 SelectionContainer
- `core/.../web/PageFetcher.kt` — 返回 `FetchResult` 三态；加 `detectBlock(text, host)` 启发式
- `app/.../ui/add/AddViewModel.kt` — `sendText` 用 `when (result)` 拼三种 prompt（Success / Blocked / Failed）

## 设计取舍

### SelectionContainer 包 LazyColumn

Compose 的 SelectionContainer 是一个跨 Text 文本选择的 logical scope。包在 LazyColumn 外面：

- 优点：用户体验直白，每条气泡都能选；不用每个 Text 包一次
- 缺点：选中的文字滚出可视区时会丢（LazyColumn 回收行为），跨 page 选择不稳

可接受：用户复制时一般在一两条消息内选，不会拖很远。如果将来真有跨屏拖选需求，再换成自家 selection state。

### Composer paste

BasicTextField 默认就支持长按弹 paste 菜单 — 不用动。

### 防爬识别启发式

不试图理解每个站点的 DOM。两条规则：

1. **长度阈值**：strip 后 < 200 字 = 大概率壳页（正常商品页 SSR strip 后至少几百字）
2. **关键词 + 长度联合**：strip 后 < 1000 字 + 命中 "请登录 / captcha / 打开拼多多 / 打开淘宝 / 打开京东 / 访问受限" 等 → 高概率壳页

为什么联合：搜索结果页 / 介绍页可能字数中等但合法包含 "登录" 字眼；商品页多半字数大。1000 字阈值把这两者分开。

误判方向：
- 短的合法商品页（极少）会被当 blocked → AI 仅基于分享原文
- 长的登录壳页（也极少，登录页一般没那么多内容）会被当 success → AI 拿到一堆 "请登录" 文本，识别能力下降

两个误判方向都不致命，主流程仍能给草稿。

### 拼多多专门处理？

考虑过：识别 host 为 yangkeduo.com / pinduoduo.com / p.pinduoduo.com 直接走 Blocked 路径，跳过 fetch 节省 30 秒等待。

最后**没做**，因为：

1. detectBlock 已经能识别拼多多壳页（"打开拼多多" 关键词命中）
2. 万一拼多多以后开放了某些链接（活动页 / SEO 页）我们 hardcode skip 反而漏掉
3. 30 秒的等待是 callTimeout，对应数据通道可能更短，实际等不了那么久

如果 cycle 0022 用户反馈 "拼多多链接每次都要等很久才出错"，再加 host short-circuit。

### prompt 里 "不要回 '我无法访问外部链接'" 这句

Anthropic / OpenAI / Kimi 的对话模型在被问 URL 时有强烈倾向回 "I can't browse the web"，因为这是 training 时被强化的安全 / 准确性边界。我们的客户端实际上做了 fetching，AI 这种回复对用户是误导。直接在 prompt 里 "**不要**回 ..." 是最有效的纠偏 — 三家 provider 都能听这种 negative instruction。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测

1. 录入页随便发一条文字 → AI 回复 → 长按助手气泡里的文字 → 起选择手柄 → 选中 → 复制 → 粘贴到 Composer
2. 拼多多任一商品 → 系统分享 → "Treasure" → AI 应该不再回 "我无法访问外部链接"，而是基于分享原文里的商品名给草稿
3. 京东商品（公开 SSR 页）→ 同款分享 → AI 应该能拿到商品页摘要，识别更准确
4. 登录墙的页面（如某些会员价）→ 应被识别为 Blocked，AI 不再回退到 "我没能力"
