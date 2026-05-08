# Cycle 0008 · spec

- **状态：** done
- **完成：** 2026-05-07

## Manifest

- [x] `RECORD_AUDIO` 添加
- [x] `READ_MEDIA_IMAGES` 添加（API 33+）
- [x] `READ_EXTERNAL_STORAGE` 添加（maxSdkVersion=32）
- [x] `android:icon` + `android:roundIcon` 设置 `@mipmap/ic_launcher`

## App icon

- [x] `res/drawable/ic_launcher_background.xml` — paper fill
- [x] `res/drawable/ic_launcher_foreground.xml` — ring + 内圈 + 顶/底 rune + 左右 tick + 中心 terra dot + 两侧小 dot
- [x] `res/mipmap-anydpi-v26/ic_launcher.xml`
- [x] `res/mipmap-anydpi-v26/ic_launcher_round.xml`
- [ ] 装机：桌面图标显示纸面 + 黑环 + 红色中心点（待用户验证）

## 真 STT

- [x] `com.treasure.voice.VoiceCapture` Composable 新增
- [x] 权限：缺 `RECORD_AUDIO` 时启动 `RequestPermission`，拒绝则自动 onCancel
- [x] `SpeechRecognizer.createSpeechRecognizer` + `RecognitionListener`
- [x] 中文识别 (`EXTRA_LANGUAGE = "zh-CN"`)
- [x] `EXTRA_PARTIAL_RESULTS = true` 实时部分结果
- [x] `onRmsChanged` → 动态波形条高度
- [x] `onResults` → onResult(text)；`onError` → 有 partial 用 partial，否则 onCancel
- [x] tap 蒙层 → `stopListening()` → 走 onResults
- [x] `DisposableEffect.onDispose` 销毁 recognizer
- [x] `SpeechRecognizer.isRecognitionAvailable` false → onUnavailable 回退

## Photo permission

- [x] `rememberPhotoPermissionName()` 按 SDK 选 `READ_MEDIA_IMAGES` (33+) / `READ_EXTERNAL_STORAGE` (M-32) / null (<M)
- [x] 点 📷 → 检查 `ContextCompat.checkSelfPermission` → 已授权直接 launch picker，否则 `RequestPermission` → 不论结果之后都 launch picker

## Header

- [x] "录入" → "RECORD"（22sp + 4sp letter-spacing）
- [x] 副标题（conversation title + ▾）只在 title 不是 "New entry" 时出现
- [x] 默认进入 → 看不到 "New entry"，只有大字 "RECORD"

## Composer

- [x] `bottom = 100dp`（之前 88dp，避免压控制岛）
- [x] BasicTextField `maxLines = 4`
- [x] 文字框 `heightIn(min=28dp, max=96dp)`，向上增长不溢出

## History dropdown

- [x] 圆角 14dp（之前 2dp）
- [x] `Modifier.shadow(12.dp)` 软阴影
- [x] 头部 ✦ ornament + RECENT CONVERSATIONS caps（letter-spacing 2sp）
- [x] 行内 8dp 圆角；当前对话标记改成右侧 terra 小圆点（不是整行 card 背景）
- [x] 日期"-"换"·"
- [x] "新对话" 改 terra 色 + 大 padding

## 验证

- [x] `./gradlew :app:assembleDebug` 通过（v0.11.0，13 MB）
- [ ] 装机：桌面新图标渲染（环+rune+terra dots）
- [ ] AddRoute → header 显示 "RECORD"，无副标题
- [ ] 点 🎙 → 弹权限请求 → 同意 → 蒙层 + 波形 + partial 转写 → tap → 用户语音气泡显示真转写 → AI 解析
- [ ] 拒绝麦克风权限 → 蒙层立即 dismiss，无错误
- [ ] 没 SpeechRecognizer 服务的设备 → 蒙层秒退 + 占位语音消息
- [ ] 点 📷 → 弹权限请求 → 不论同意/拒绝 → 之后弹相册 picker
- [ ] composer 多行输入 → 4 行后停止增长，不下沉到胶囊位置
- [ ] 点 🕐 → 历史 dropdown 弹出 → 看到 ✦ 头 / 圆角 / 软阴影 / 当前对话右侧小 terra dot

## 不在这一轮

- 真历史持久化
- 拍照 / 多选
- AI 生成博物馆插画
- 真 schema migration（**cycle 0009 必做**）
