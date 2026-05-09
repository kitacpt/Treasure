# Cycle 0018 · 历史改下弹 · RECORD 副标同步 · 三档状态灯 · readTimeout 修复

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 4 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 历史抽屉跟其他抽屉风格统一 — 从下面弹出来 | `HistoryDropdown` 从左侧 fillMaxHeight panel 改成 `ModalBottomSheet`（material3，跟手动录入屏 / Settings 抽屉一致），自带圆角顶 + 拖把手 + scrim；`heightIn(max = 560.dp)` 限高，列表 LazyColumn 内部滚 |
| 2 | RECORD 副标 = 当前对话主标题，切换时一眼能看出 | (a) `newConversation()` 创建对话时 title 直接拼上 "· HH:MM" 后缀（"New entry · 15:32"），入库就唯一；(b) `openConversation(id, storedTitle)` 接 history row 的 title 一并带过去；(c) `ChatHeader` 副标永远直接显示 `state.conversationTitle`（不再有 "title==DEFAULT_TITLE 时显示 RECORD mono caption" 的分支）；(d) `HistoryRow` 也是直接显示 `conv.title`，display-time 后缀的 hack 删了 — 一处生成、各处一致 |
| 3 | AI 接入状态分三档 红/黄/绿 | (a) `SettingsStore.lastTestPassed: Boolean` 持久化上次 testConnection 结果；(b) `SettingsViewModel.testConnection` 成功 / 失败都会写回；(c) `save()` 永远 reset 为 false（配置变了 → 旧成功不算数）；(d) `ConnectivityPill` 三档：未配置 = `#C5392E` 红 / 未连通 = `#D89B23` 黄 / 已连通 = `#3E8E45` 绿 |
| 4 | Kimi 还是不通；不止是 timeout | 关键诊断：cycle 0017 把 `callTimeout` 抬到 360s，但 OkHttp 默认 `readTimeout = 10s`，没动。reasoning 模型在第一个 byte 出来之前可能要 30-180s 思考，readTimeout 会先在 10s 把连接干掉 — 用户看到的就是 "timeout"，跟 callTimeout 多大无关。本刀两个 client 的 `defaultHttpClient` 都加上 `readTimeout(callTimeoutSec)` + `writeTimeout(60s)`，跟 callTimeout 同档生效 |

## 关于直接 curl 验证 key

harness 第二次拒绝把用户 sk- 凭据外发到 api.moonshot.cn。policy 一致：transcript 永久保留 key 等于泄露，即便用户主动给。强烈建议用户立即在 Moonshot 控制台 revoke / rotate 那把 key（已经在聊天里出现过两次）。

诊断思路给到用户：装新 APK → Settings → 调整 → 测试连接 → 结果卡片里看 kind / detail。
- `× HTTP 4xx · ...` 表示请求送到了，服务端拒绝；message 里有具体原因
- `× 网络 · SocketTimeoutException` 表示读不到字节（**这一刀修了，应该不再出现**）
- `× 网络 · UnknownHostException` 表示 DNS / 路由问题

## 不在这一刀

- 云端 STT（cycle 0017 暂去掉的麦克风）
- image vision 多轮 / AI 生成插画 / preset 校准 / MigrationTest CI

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
