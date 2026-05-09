# Cycle 0019 · Grid 同步 · 聊天回退 · 状态灯 · 全屏滑动 · 分享接收 · 草稿页样式

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 6 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 在 "全部" 里点东西，返回后跳到酒水 | `MainScreen.gridCategoryId` 从来不接 chip 点击的回写。Detail pop 回来时 GridRoute 重组，`LaunchedEffect(initialCategoryId)` 用旧的 gridCategoryId 把 chip 选择覆盖掉。修法：GridRoute 增加 `onCategoryChanged: (String) -> Unit` 回调，chip 点击时把 `cat?.id ?: GridViewModel.ALL_FILTER_ID` 喂给它；MainScreen 收到后写回 gridCategoryId — single source of truth |
| 2 | "你好" 先报 `content had no JSON object` 再正常回复 | 那不是两条消息：是 parseDraft 抛异常时把 model 的纯文本回复包进 message 里，UI 显示成 "出错了：content had no JSON object: 你好！我是…"，看起来像先报错再回复。本刀新增 `ChatOnlyResponseException(text)`，OpenAi/AnthropicClient 在没找到工具调用 + 没 JSON 时改抛这个；AddViewModel 接到它就把 `text` surface 成普通助手聊天消息（不再是 "出错了" 前缀），自然对话流 |
| 3 | 明明连接成功了还是 "未连通" | cycle 0018 我让 `save()` 一律 reset `lastTestPassed = false`。结果用户测试通过 → 保存 → 状态灯立即变黄。本刀反过来：`save()` 不再 reset，**信任此刻的 lastTestPassed**；改在 `setApiKey` / `setBaseUrl` / `setModel` / `setPreset` / `setTemperatureText` / `setThinkingEnabled` 这些 setter 里 invalidate（开了一个 `invalidateTest()` helper 统一调）。意思是 "改任何字段就把灯打回黄"，不改就保持上次测试的颜色 |
| 4 | 影集照片之间没法左右滑 | `FullscreenPhotoViewer.ZoomableImageWithCallouts` 之前用 `detectTransformGestures` 把单指拖动也吃了（事件 consume），HorizontalPager 收不到 swipe → 没法翻页。改成 `Modifier.transformable(state, lockRotationOnZoomPan = true)` — 单指拖不被消费，pager 能正常翻；双指 pinch 仍走我们的 zoom；`scale > 1` 时再单独挂一个 `detectDragGestures` 处理放大后的平移 |
| 5 | 京东 / 淘宝 等分享商品链接进 Treasure 自动录入 | (a) Manifest 加 `<intent-filter>` ACTION_SEND text/plain + ACTION_VIEW http(s) BROWSABLE；MainActivity `singleTask` + `onNewIntent` 处理；(b) `TreasureApp.shareIntake: MutableStateFlow<String?>` 中转；MainActivity 写入；(c) MainScreen 监听到非空就 `pagerState.animateScrollToPage(PAGE_ADD)`；AddRoute 监听到就 `vm.sendText(text)` + 清空。AI 对带 URL 的文本会做提取尝试（不会真去 fetch URL，那是 cycle 0020 候选） |
| 6 | 草稿页样式跟 Edit 页保持一致 | `AddPreview` 整页重写：用 `EditPageHeader`（左 [取消] + 主标题 "Refine" + 副标 = 中文品类名 + 右 [确认收入] terra）、`HeroAvatarPicker` 只读预览、`SectionDivider` 分两段 "基础" / "其他信息"、每个字段一行 `LabeledField` 风格的结构（confidence dot + label + value/textfield + 下划线）；点行进 inline edit；之前的卡片式 HeroCard / Footer / 自家 Header 全删 |

## 关于 #4 跟 Kimi 的关系

这一刀和 Kimi 不直接相关 —— readTimeout 在 cycle 0018 已经修了。但这次的 ChatOnlyResponseException 改造也覆盖了一种 Kimi 用户可能遇到的现象：模型不调 tool 而走纯聊天回复时，UI 会自然走聊天流，不再误报 "content had no JSON object"。

## 不在这一刀

- 真去 fetch 京东 / 淘宝商品页，解析 HTML 抽 spec — 大工作量，cycle 0020+ 候选
- 云端 STT
- AI 生成插画
- preset URL 校准
- MigrationTest CI

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
