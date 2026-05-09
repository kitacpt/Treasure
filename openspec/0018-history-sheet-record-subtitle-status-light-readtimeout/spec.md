# Cycle 0018 · 验收

## 历史抽屉

- 点 ChatHeader 上的 🕐 / 副标 → 历史从底部上滑（material3 ModalBottomSheet，跟手动录入屏一致）
- 顶部带半透 scrim + 拖把手 + 圆角
- 列表区内嵌 LazyColumn，限高 560dp，滚动正常
- 点对话行 / 点 "新对话" 不自动收
- 滑下去 / 点 scrim 才收

## RECORD 副标

- 永远显示当前对话标题（带 italic Cormorant + ▾ 触发抽屉）
- 默认对话标题是 "New entry · HH:MM"（创建即唯一）
- AI 出草稿后改名为 "Brand Model"
- 历史抽屉里同一段对话显示一字不差的标题
- 切到另一段对话，副标立刻同步成那段的标题

## AI 状态灯（Settings 摘要卡）

- 红 `#C5392E` "未配置"：API key 空
- 黄 `#D89B23` "未连通"：key 已填但 lastTestPassed = false
- 绿 `#3E8E45` "已连通"：key 已填且最后一次 testConnection 成功
- 任何 `save()` 都会 reset lastTestPassed → false → 黄灯（配置变了得重测）
- testConnection 成功 → 绿灯；失败 → 黄灯（覆盖之前的绿）

## OkHttp 超时

- `connectTimeout = 30s`（不变）
- `readTimeout = callTimeoutSec`（thinking 模式 360s，普通 120s）
- `writeTimeout = 60s`（够上传 base64 图片）
- `callTimeout = callTimeoutSec`（同 read）

之前 readTimeout 用 OkHttp 默认 10s — reasoning 模型必死。

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
