# Cycle 0009 · 验收

## 视觉统一

- 录入页 `AddChat` 的 header：左侧 Cormorant `Record` (titleLarge) + 下方 mono `RECORD` (labelSmall)；当 `conversationTitle` 不是默认值时改显示 italic Cormorant 副标题 + ▾
- `CategoryForm` 与 `EditScreen` 顶部统一：utility 行（取消 / ←  ............  保存）→ Cormorant titleLarge → mono caption (`NEW · 羽毛球` / `EDIT · 摄影`)
- `CategoryForm` 与 `EditScreen` 的分组标题统一为 `SectionDivider`：两侧 0.6dp 细线居中夹一行 ink 11sp 2.4 letter-spacing 文本

## 录入页

- ChatHeader 的 0.5dp divider 之下、消息列表之上是 “尚未配置 AI · 去设置 →” 横幅（仅在 `state.aiAvailable == false` 时出现）
- LazyColumn 底部 contentPadding 包含 `navigationBars` 系统 inset + 额外 160dp，向上滑能把最后一条消息完全暴露出来
- 助理 / 语音 / 草稿 italic 文本走 Cormorant 字体，不再触发 SpaceGrotesk 的合成斜体

## 手动录入

- 顶部 italic tagline（来自 `CategoryTemplate.tagline`）
- 四个 hero 字段都有 hint（来自 `CategoryTemplate.heroSpecHints`）：举例 “重量 (g)” → hint `如 84-89 (3U / 4U)`
- 基础区四行均带 hint

## Settings

- 主页只展示：Header (Settings + AI SERVICE 副标) → 摘要卡 → DANGER ZONE
- 摘要卡：Provider 名称 + 连通 pill (`已配置` terra dot · `未配置` sub dot) → 0.5dp 内分隔线 → Model / Base URL / API Key (掩码) 三行 → “调整 →”
- 摘要卡可点 → 弹底部抽屉：拖把手 → `AI 配置` 标题 + 取消 → Provider 下拉（默认折叠，点开列出全部 preset 含 base URL 副字） → Base URL（可空 / 必填依 preset） → Model → API Key (显示 / 隐藏) → 保存 + 测试连接 → 测试状态行
- 抽屉外有半透明 ink 32% 蒙层，点蒙层关闭抽屉

## App 图标

- adaptive 前景：ink 主环外径 22 / stroke 5；内侧 0.5dp 细圈；顶/底 paper-color 锯齿型 rune；左右 tick；中心 + 两侧 terra dot
- adaptive 18dp crop 后还剩 ~10dp paper 留白

## 数据层

- `SettingsStore.presetId` 字段；首次打开旧版数据按 (provider, baseUrl) best-effort 映射
- `OpenAiClient.buildUrl()` 兼容 `…/chat/completions` `…/v\d+` `…/v\d+beta` 三种形态

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` clean
- APK 13 MB，debug 签名
