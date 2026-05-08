# Cycle 0008 · 工作笔记

## 真 STT 几个坑

- **不能在 LaunchedEffect 里直接 startListening**：必须在 main thread，且 `SpeechRecognizer` 是 stateful 的。用 `DisposableEffect` 一并管 lifecycle（创建 + onDispose 销毁）。
- **`onResults` 可能晚到**：用户 tap 蒙层 → 调 stopListening → 大概 100-300ms 后 onResults 触发。这中间用 `done` flag 防止重复回调。
- **`isRecognitionAvailable` 仅检查服务存在**，不保证识别质量。某些 vivo / 华为 ROM 没装 Google App，返回 false → 我们走 onUnavailable → 走老 stub。
- **EXTRA_LANGUAGE = "zh-CN"** 在国行 ROM 上效果好；EXTRA_PREFER_OFFLINE=true 可以离线识别但精度差。
- **波形条**：`onRmsChanged` 给的 dB 值噪声大，做了 `0.6 * old + 0.4 * target` 平滑，并加随机抖动防止视觉死板。

## 图标 viewport 注意

- adaptive icon 视口 108×108，但**安全圆形区** 只有内 72dp 直径
- 我用半径 30 ring（直径 66dp），加 6dp stroke 粗 → 总外径约 70dp，刚好在安全区内
- 如果用 PIE shape 启动器，外圈会被裁；圆形更明显
- 顶/底 rune 在 y=22-30 / y=82-86，在大半径内

## composer 不再下沉

- 控制岛在 NavHost 层，bottom = navigationBarsPadding + 18dp + 大约 50dp 高
- composer 在 AddChat 层，bottom = navigationBarsPadding + 100dp
- 数学：composer 底边 = 100dp 高于 nav bar；岛顶边 = 18 + 50 = 68dp
- 净空 32dp，足以用户长按打字而不混叠
- 多行输入 maxLines=4 + heightIn(max=96dp) 让上半部分撑开，下半部分锚死

## 给下一个 agent

cycle 0009 候选（按用户优先级，但请优先做 schema）：

1. **真 schema migration**（已 destructive 8 次，债越欠越多）
2. **历史对话持久化** + **多轮对话**（assistant refine draft）
3. **拍照** 直调相机 + **多选照片**
4. **AI 生成博物馆插画** —— `AiClient.generateIllustration` + 本地缓存
5. **全屏看图浏览器** + **callout 文字标注**
