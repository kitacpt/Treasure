# Cycle 0020 · notes

## 文件改动

新建：

- `core/.../web/PageFetcher.kt` — OkHttp + UA 装移动 Chrome + HTML strip + first-URL helper

主要修改：

- `app/.../ui/photo/FullscreenPhotoViewer.kt`
  - HorizontalPager 上下各 64dp 黑边
  - ZoomableImageWithCallouts 改用 `awaitEachGesture` 看 pointer count + scale 决定要不要 consume
- `app/.../TreasureApp.kt` — `pageFetcher: PageFetcher` 单例
- `app/.../ui/add/AddViewModel.kt` — `sendText` 内 URL detection + fetch + augment prompt

## 设计取舍

### awaitEachGesture 才是真修

试过两轮：

- cycle 0019 第一次：`Modifier.transformable(state, lockRotationOnZoomPan = true)` — 期待它只接 multi-touch。看了 Compose 源码后发现 `lockRotationOnZoomPan` 只是 "zoom 起来后不再监听 rotation"，**单指 pan 仍被消费**。pager swipe 收不到。
- cycle 0020：手写 `awaitEachGesture { do { awaitPointerEvent(); ... } while(...) }`，在每次 event 看 `event.changes.count { it.pressed }`：
  - 0 / 1 fingers + scale==1 → **不调** `it.consume()`，事件 bubble 上去给 pager
  - 1 finger + scale>1 → 消费 + 平移
  - ≥2 fingers → 消费 + zoom+pan

PointerEvent 的 consume 是按 InputEvent 维度而不是按 modifier 维度，所以一旦没 consume，外层 pager 收得到。验证过。

### 64dp 留白

不是越小越好。状态栏一般 24dp，加上 ← back 文字 14sp 高 + 上下 6dp padding，至少需要 ~50dp。预留 64 带 14dp 缓冲，不挤。底部同款。

加 padding 顺序：先 statusBarsPadding 再 padding(top=64) — 状态栏正下方留 64dp 黑色，再下面才是图。

### PageFetcher 不假装是浏览器

只装移动 Chrome UA。**不用 cookie / 不带登录 session / 不执行 JS**。结果：

- 京东 / 淘宝公开商品页（有 SSR + 移动版）能拿到结构化文字
- 需要登录的页面 → 拉到一坨登录提示
- 完全 SPA 的页面（极少数电商）→ 拉到空壳

最后一种情况 fetched 是空白或一坨 JS 加载提示，AI 拿不到有用信息，效果跟没 fetch 一样。Acceptable — 主要场景是 JD / 淘宝 / 拼多多 / 京东到家这种公开 SSR。

### strip 策略

不试图理解 DOM。简单四件事：

1. 整段 remove `<script>` / `<style>` / `<noscript>`（含里面的 JS / CSS 噪声）
2. 抓 `<title>` 当 "【标题】" 前缀
3. 扫所有 `<meta property|name="og:title|og:description|description|keywords">` 当 "【描述】" 前缀
4. 剩下所有 tag 替成空格 + 折叠空白 + 截到 4000 字符

AI 来挑里面的品牌 / 型号 / 价格。这比 hardcode 京东 vs 淘宝 vs 拼多多的特定 DOM 选择器要稳得多。

### prompt 拼接

```
用户在外部 app 分享了一条商品链接，原文：
<用户原文>

[页面摘要]
<strip 后的页面文字>

请基于摘要识别这件商品（品牌 / 型号 / 关键参数），不要把 URL 本身写进任何字段。
```

最后一句 "不要把 URL 写进字段" 很重要 — 之前 AI 经常把 `https://item.jd.com/123.html` 当成 brand 之类的字段塞进去。

### busy 指示

cycle 0020 我没另外 append "正在拉取页面" 占位消息。fetch 期间把 `busy=true` 拉起来，typing indicator 自然展示在最后 — 跟 AI 思考时是同一种 UI。fetch 完进 `runExtract`，busy 仍为 true，AI 思考期间继续转。AI 出 draft → busy=false。一气呵成。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测

1. 影集双指 pinch zoom 到 2× → 单指拖应该平移；松手 → 缩回 1× 后单指左右滑应能翻页
2. 单指拖 / 左右滑（scale = 1）→ 直接翻页，不再卡死
3. 上下各能看到约 64dp 黑色，← back / 计数 / 底部提示在黑边里清晰可读
4. 京东打开任一商品页 → 系统分享 → "Treasure"
5. 录入页应该看到 typing indicator（"正在思考…"）转一会儿（fetch + AI）
6. 然后助手出 "好。我已经替你写好了一份草稿——要不要先看看？" + DraftCta 卡片
7. 草稿里的 brand / model 应是从分享页 title / og 抽出来的真值，URL 不会出现在字段里
8. 拉页面失败的话（如登录墙）→ 还是只用原文喂 AI，不报错

## 已知限制

- 国内电商 SSR 越来越向 SPA 迁移；将来可能要加 minimal headless 渲染（webview 拉一次再读 DOM），但工程量大很多
- 不抓 mobile-only 短链（如 `https://3.cn/xxx`）的最终落地页 — followRedirects 会跟一次，但如果短链跳转链很长可能拉不到
