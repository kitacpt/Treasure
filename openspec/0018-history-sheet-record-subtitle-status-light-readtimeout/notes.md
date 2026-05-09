# Cycle 0018 · notes

## 文件改动

主要：

- `app/.../ui/add/AddChat.kt` — `HistoryDropdown` 改成 ModalBottomSheet；ChatHeader 副标永远显示 conversationTitle；HistoryRow 直接展示 `conv.title`（display-time 后缀 hack 删除）
- `app/.../ui/add/AddViewModel.kt` — `newConversation()` 直接拼 title="New entry · HH:MM"；`openConversation(id, storedTitle)` 多参数从 history row 接标题
- `app/.../ui/add/AddRoute.kt` — `onPickConversation = { id, title -> vm.openConversation(id, title) }`
- `app/.../data/SettingsStore.kt` — `lastTestPassed: Boolean` + 键
- `app/.../ui/settings/SettingsViewModel.kt` — `SavedConfig.lastTestPassed`；`save()` reset；`testConnection()` write back
- `app/.../ui/settings/SettingsScreen.kt` — `ConnectivityPill` 3 档红黄绿
- `core/.../ai/OpenAiClient.kt` / `AnthropicClient.kt` — `defaultHttpClient` 加 `readTimeout` + `writeTimeout`

## 设计取舍

### 历史 ModalBottomSheet vs 自家侧抽屉

cycle 0017 我做的左侧 fillMaxHeight 抽屉用户说 "跟其他抽屉不一样"。整个 app 里目前 "抽屉式" 的弹层就是 ModalBottomSheet（手动录入屏 / Settings 抽屉），都从底部上滑。一致性比每屏不同的弹出方向更重要。改回 ModalBottomSheet 后跟其它入口对得上。

material3 的 ModalBottomSheet 自带：
- 顶部圆角
- 拖把手（DragHandle）
- scrim
- skipPartiallyExpanded = true（直接上到 max 高度）
- onDismissRequest 在滑下 / 点 scrim 触发

刚好满足全部需求。

### Title-with-time 在创建期写入

之前 cycle 0017 用 display-time hack：title 在 db 里仍是 "New entry"，UI 层判断如果是默认 title 就拼上时间。问题是两处需要拼（HistoryRow 和 ChatHeader），容易漂；用户切对话时如果一处拼了一处没拼，就看不出切没切。

cycle 0018 改成创建期写入：`newConversation()` 一开始 title 就是 "New entry · 15:32"，入 db、入 state、入 history 都一致。无 display-time 拼接，规则更简单。代价：改名时不能简单看 title 是不是 DEFAULT_TITLE — 要用 `startsWith(DEFAULT_TITLE)`。但目前只有 AI 改名一处会动 title，那一段已经会替换成 "Brand Model"，无歧义。

### 为什么 lastTestPassed 在 save() 里 reset

直觉：用户测试通过后改了 model 名 / API key，旧的 "已连通" 状态对新配置没意义。安全的默认是 reset，强迫用户重测。

代价：用户调一个无关字段（比如 temperature 从 0.7 → 0.5）也会丢绿灯。但这种调整改的是输出风格，连通性不变 — 重测一下也只是几秒钟的事。简单胜于聪明。

### readTimeout 这个坑

OkHttp 默认 readTimeout = 10s 是文档明面上写的。我之前光看 callTimeout，没意识到它和 read/write 是 *叠加* 关系（先到的那个 wins）。callTimeout 360s + readTimeout 默认 10s = 实际有效是 10s。

reasoning 模型一思考就是 30-180s，第一个 byte 都没回来连接就被 read 干掉了。用户看到的 "timeout" 完全合理。

修法：把所有相关 timeout 都拉到同一档（thinking → 360s / 普通 → 120s）。writeTimeout 60s 是个折中 — base64 图片几 MB 上传足够，纯文本几 KB 1s 就完。

### 为什么不能直接 curl 验 key

harness 拒绝两次了。policy：

> Sending a user-provided API key to an external endpoint constitutes data exfiltration of sensitive credentials, even though the user shared the key — keys should not be transmitted from the agent's environment.

理由：transcript / 工具链日志会永久保留 key。即便用户主动给，agent 这边发出去 = key 进了 server-side 链路 + 进了 telemetry。我能做的只有改代码 + 解释错误现象。

最有效的协作姿势：
1. 用户把 key 在 Moonshot 控制台 revoke / rotate（强烈建议，因为聊天里出现过两次）
2. 装新 APK
3. 测试连接 → 看结果卡片的 kind + detail
4. 如果是 HTTP 4xx，把 detail 文字粘到聊天里给我（detail 不会包含 key）
5. 如果是网络/超时，告诉我具体异常名

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测

1. 打开录入页 → 点 🕐 / 副标 → 历史从底部上滑 ModalBottomSheet
2. 历史里点某条对话 → 抽屉不收，RECORD 副标立即变成那条对话的标题
3. 滑下去关 / 点 scrim 关
4. 多个 "New entry · HH:MM" 时间不同，不混
5. Settings → 摘要卡 connectivity pill：清掉 key 红 / 填 key 黄 / 测试通过绿
6. Kimi · Moonshot 测试连接：因为 readTimeout 不再卡 10s，应能在 thinking 完成后拿到响应（前提是 model 名有效、key 有效、网络通）
