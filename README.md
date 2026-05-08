# Treasure

> *a private cabinet of things owned, used, & remembered*

一个 Android 应用，用来记录个人爱好装备：羽毛球拍、相机镜头、租过的车、电子设备 …… 每件东西有它的样貌（一张博物馆线描风的矢量图）、参数表、和一条用过的故事线。

视觉灵感是 19 世纪自然博物馆的图鉴版画 —— 细线勾勒、淡彩平涂、罗马数字标注线。

## 从这里开始

新人 / 新一轮 agent 进来按这个顺序读：

1. [`agent.md`](agent.md) —— 当前状态、做了什么、接下来做啥（**先读这个**）
2. 浏览器开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html) —— v1 视觉规格（主体设计）
3. 浏览器开 [`prototype/add-page-v2/project/Treasure.html`](prototype/add-page-v2/project/Treasure.html) —— v2 录入页设计稿
4. [`docs/dev-loop.md`](docs/dev-loop.md) —— 构建 / 装机 / vivo 调试 / 内循环 / 权限调试
5. [`docs/product.md`](docs/product.md) → [`docs/visual-language.md`](docs/visual-language.md) → [`docs/architecture.md`](docs/architecture.md)
6. [`docs/adr/`](docs/adr/) —— 5 份决策记录
7. [`openspec/`](openspec/) —— cycle 0001-0008 的 proposal / spec / notes

## 当前状态（2026-05-07）

**cycle 0001 → 0008 全部落地**。v0.11.0 APK（13 MB，debug 签名），装到 vivo X200 Pro mini 上端到端跑通：

- Portal · Grid · Detail · Edit · Add (RECORD) · Settings 全屏导航
- Detail 抽屉（历史 / 参数 / 影集，全只读）+ 明信片翻面（看实拍）
- Detail 右上点 → Edit 单页表单（基础 / 时间 / 标签 / 插画 / 参数 / 历史 / 实拍 / 删除）
- Add (RECORD) chat-first：📷 真照片 + 文本 + 🎙 真 STT (zh-CN) + → AI 解析 → 草稿预览 → 确认入库
- AI：Anthropic / OpenAI / Custom provider，BYO key 存 EncryptedSharedPreferences
- 真实照片存 `filesDir/photos/<itemId>/<uuid>.jpg`（cycle 0003）
- Schema v5（Room，单表 items + JSON 列）；⚠️ 仍 `fallbackToDestructiveMigration()`
- 11 个博物馆线描插画 + 自家 LotR 风 app 图标
- 8 条种子数据（首启写入），edge-to-edge，控制岛在 Detail/Edit 屏隐藏

GitHub：<https://github.com/kitacpt/Treasure>

## 仓库布局

```
treasure/
├── prototype/      Claude Design 导出的可点击 HTML 原型，作为活的视觉规格
├── android/        Android app（Kotlin + Jetpack Compose；:app + :core）
├── backend/        FastAPI 同步服务（占位，cycle 0003+ 才会接通）
├── docs/           长期指引 —— product / architecture / visual-language / dev-loop / ADRs
├── openspec/       变更周期提案（一个 cycle 一个文件夹）
├── scripts/        bootstrap.sh / prototype-serve.sh / serve-apk.sh
└── agent.md        滚动更新的现状交接
```

## 关键决策（一句话版）

- **平台**：Android 原生，Kotlin + Jetpack Compose（[ADR-0001](docs/adr/0001-android-native.md)、[ADR-0002](docs/adr/0002-jetpack-compose.md)）
- **数据**：Local-first，Room 为权威源；FastAPI 同步层可选，先搭脚手架（[ADR-0003](docs/adr/0003-local-first-with-optional-sync.md)）
- **AI**：用户自带 API key（Anthropic / 模型 / Key），设备直连 provider，不走代理（[ADR-0004](docs/adr/0004-byo-ai-key.md)）
- **插画**：种子物品的 SVG 打包进 app；用户新增物品的插画由配置好的 AI 生成（[ADR-0005](docs/adr/0005-museum-illustration.md)）
