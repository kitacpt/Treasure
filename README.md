# Treasure

> *a private cabinet of things owned, used, & remembered*

一个 Android 应用，用来记录个人爱好装备：羽毛球拍、相机镜头、租过的车、电子设备 …… 每件东西有它的样貌（一张博物馆线描风的矢量图）、参数表、和一条用过的故事线。

视觉灵感是 19 世纪自然博物馆的图鉴版画 —— 细线勾勒、淡彩平涂、罗马数字标注线。

## 从这里开始

- **想看可点击的视觉规格** → 浏览器打开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html)
- **想看产品定义** → [`docs/product.md`](docs/product.md)
- **想看视觉系统** → [`docs/visual-language.md`](docs/visual-language.md)
- **想看技术架构** → [`docs/architecture.md`](docs/architecture.md)
- **想看为什么这么选** → [`docs/adr/`](docs/adr/)
- **想看现在做到哪儿了 / 接下来做啥** → [`agent.md`](agent.md)
- **想看正在进行的变更周期** → [`openspec/`](openspec/)

## 仓库布局

```
treasure/
├── prototype/      Claude Design 导出的可点击 HTML 原型，作为活的视觉规格
├── android/        Android app（Kotlin + Jetpack Compose）
├── backend/        FastAPI 同步服务（占位，cycle 0003+ 才会接通）
├── docs/           长期指引 —— product / architecture / visual-language / ADRs
├── openspec/       变更周期提案（一个 cycle 一个文件夹）
├── scripts/        开发/构建脚手架
└── agent.md        滚动更新的现状交接
```

## 关键决策（一句话版）

- **平台**：Android 原生，Kotlin + Jetpack Compose（[ADR-0001](docs/adr/0001-android-native.md)、[ADR-0002](docs/adr/0002-jetpack-compose.md)）
- **数据**：Local-first，Room 为权威源；FastAPI 同步层可选，先搭脚手架（[ADR-0003](docs/adr/0003-local-first-with-optional-sync.md)）
- **AI**：用户自带 API key（Anthropic / 模型 / Key），设备直连 provider，不走代理（[ADR-0004](docs/adr/0004-byo-ai-key.md)）
- **插画**：种子物品的 SVG 打包进 app；用户新增物品的插画由配置好的 AI 生成（[ADR-0005](docs/adr/0005-museum-illustration.md)）
