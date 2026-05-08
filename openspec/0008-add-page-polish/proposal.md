# Cycle 0008 · 录入页 polish + 真 STT + app 图标

- **状态：** done
- **完成：** 2026-05-07

## 用户反馈 6 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 语音是假的，要真 | `core/voice/VoiceCapture.kt` 包装 Android `SpeechRecognizer`，含 RECORD_AUDIO 权限请求；不可用时自动回退到 stub |
| 2 | 发照片要权限 | 加 `READ_MEDIA_IMAGES` (API 33+) / `READ_EXTERNAL_STORAGE` (≤32) 到 manifest；点 📷 前请求权限（PickVisualMedia 不严格需要，但 vivo 等 OEM 可能门槛更高，加上更稳） |
| 3 | App 图标要类似魔戒风格 | adaptive icon：paper 背景 + ink 重笔粗环 + paper-color 顶/底 rune 刻入 + terra 中心 dot + 两侧小 dot |
| 4 | "录入" 中文太丑、去 New entry | header 标题改 `RECORD`（labelSmall 22sp 4sp letter-spacing，等宽 mono 风），副标题"Fujifilm X-T5 ▾"仅在 conversationTitle ≠ "New entry" 时显示 |
| 5 | History 弹窗太方正 | 圆角 14dp + soft shadow 12dp + 上下 ✦ ornament + RECENT CONVERSATIONS caps 标题 + 行 8dp 圆角 + 当前 conv 用 terra 小圆点而非整行 card 背景 + "新对话" 改 terra 色突出 |
| 6 | composer 不能压到胶囊 | composer bottom 88dp → 100dp（胶囊高 ~50dp + 18dp = 68dp，留 32dp 缓冲）；BasicTextField `maxLines = 4` 配 `heightIn(max=96dp)` 防止超长输入向下溢出 |

## 真 STT 实现

`com.treasure.voice.VoiceCapture`：

- `@Composable VoiceCapture(onResult, onCancel, onUnavailable)`
- 进入时检查 `Manifest.permission.RECORD_AUDIO`，缺则启动权限请求
- 用 `SpeechRecognizer.createSpeechRecognizer(context)` + `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`
- `EXTRA_LANGUAGE_MODEL = LANGUAGE_MODEL_FREE_FORM`，`EXTRA_LANGUAGE = "zh-CN"`，`EXTRA_PARTIAL_RESULTS = true`
- 监听 `onPartialResults` → 实时更新 overlay 内 italic 转写
- 监听 `onRmsChanged` → 把音量 dB 映射成波形 bar 高度（实时跳动）
- 用户 tap 蒙层 → `recognizer.stopListening()` → 触发 `onResults` → 拿最终文本 → `onResult(text)`
- `onError` → 有 partial 就用 partial 否则 `onCancel`
- `DisposableEffect.onDispose { recognizer.destroy() }` 释放

回退机制：
- `SpeechRecognizer.isRecognitionAvailable(context) == false` → 调 `onUnavailable`
- AddRoute 收到 onUnavailable → `vm.sendVoice("（设备未提供语音识别 · 已记录占位语音）")` 让用户至少看到反馈

## App 图标设计

`drawable/ic_launcher_background.xml` — paper #F4F1EA fill 108×108

`drawable/ic_launcher_foreground.xml`：
- 中心 (54,54) 半径 30 的 ink 粗环（stroke 6dp）—— 主体魔戒
- 半径 25 的细 ink 内圈（stroke 0.6dp）—— 暗示银带厚度
- 顶部 / 底部各一段小 paper-colour 锯齿型 rune（"刻"在 ink 环里）
- 左右 22/86 处各一道短水平 tick
- 中心 terra 实心 dot（半径 2.4）+ 左右各一颗 terra 小 dot —— "treasure 三粒宝石"

`mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` 都 reference 同一对 background/foreground。

manifest 加 `android:icon` + `android:roundIcon`。

## 不在这一轮

- 真 STT 流式响应优化 / 多语言切换
- 历史对话持久化（仍 stub）
- 拍照（直调相机）
- AI 生成博物馆插画
- 真 schema migration（cycle 0001-0008 已 destructive 8 次！）

## 验收

详见 [`spec.md`](spec.md)。
