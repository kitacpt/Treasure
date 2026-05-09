# Cycle 0021 · 验收

## 对话框选择 / 复制 / 粘贴

- 在录入页，长按任意一段对话气泡里的文字 → 起选择手柄 + 系统菜单（复制 / 全选 / 分享）
- 系统菜单的 "复制" 把选中文本写进剪贴板，可粘贴到任意文本框
- Composer 的输入框长按 → 弹粘贴菜单（BasicTextField 自带）
- 选中文字滚出可视区时选择会自动清掉（LazyColumn 回收行为）— 这是预期

## URL fetch 三态

- `PageFetcher.fetchText` 不再返回 `String?`，返回 `FetchResult`：
  - `Success(text)` — 页面拉到 + strip 后字数 ≥ 200 + 没命中防爬关键词
  - `Blocked(host, reason)` — 字数 < 200 或命中 "请登录 / captcha / 打开拼多多 / 打开淘宝 / 打开京东" 等关键词且总长 < 1000
  - `Failed(host, message)` — HTTP 4xx/5xx 或网络异常

## AI prompt 分支

- Success → `[页面摘要]\n<text>` 拼进 prompt，正常识别
- Blocked → prompt 明确说 "客户端拉过被防爬挡了，**不要**回 '我无法访问外部链接'，仅依据分享原文 + 调 fill_item_draft 把能填的填上"
- Failed → 同款，把网络错误信息也带上

## 拼多多 / JD / 淘宝行为

- 拼多多分享链接绝大多数被识别为 Blocked（壳页）→ AI 仅从分享原文里抽
- JD / 淘宝商品页有 SSR 的能正常 Success；登录墙的会被识别 Blocked
- 任何 host 都不被偏好，detect 是按 strip 后内容做的

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
