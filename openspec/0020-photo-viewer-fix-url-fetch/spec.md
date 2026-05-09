# Cycle 0020 · 验收

## 全屏看图

- pager 上下各让 64dp 黑边，竖图不再压住 ← back / 计数 / 底部提示
- 单指左右滑（scale = 1 时）→ 翻到上 / 下张照片
- 双指 pinch → 1× ~ 5× 缩放
- zoom > 1 时 1 指拖 → 平移已放大图片
- 双击 → 1× ↔ 2.5× 切换
- 长按图空白 → 加 callout
- 长按已有 dot → 编辑 / 删

## 分享链接 → 自动抓页面

- 用户从京东 / 淘宝 / 浏览器分享一段含 URL 的文字到 Treasure
- 录入页落地后 `vm.sendText(...)` 检测到 URL，先把页面拉一下：
  - PageFetcher 走 OkHttp 装移动 Chrome UA，最多 96KB raw body
  - HTML strip 后取 `<title>` + `og:*` / `description` meta + 全文摘要 ≤ 4000 字符
- 拼成新 prompt：用户原文 + `[页面摘要]` + 一句提示让 AI 不要把 URL 写进字段
- AI 基于摘要识别商品；草稿 CTA 出现
- 页面拉不动（404 / 反爬 / 网络）时回退到只用分享文本，主流程不挂

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿

## 不变

- Schema 仍 v8
- Migration 没新增
- 其他屏未动
