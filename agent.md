# Agent · 现状交接

每次工作结束更新这一份。新人 / 新一轮 agent 进来先读它。

## 今天 (2026-05-07)

**状态：cycle 0001 + 0002 已落地，准备交接**

最新 APK：`android/app/build/outputs/apk/debug/app-debug.apk` （v0.5.2，11 MB）

GitHub: <https://github.com/kitacpt/Treasure>（main 分支）

## 全部已实现的功能（端到端）

四屏导航，全部数据从 Room 来：

```
       ┌─ Portal (门厅 / 默认起点)
       │      ornament + Treasure 64sp + 三连计数 + 4 扇门 + Latest entry
       │      点 4 扇门 → Grid 该品类
       │      点 Latest entry → Detail
       │
       ├─ Grid (图鉴)                  Portal 4 扇门 / 控制岛 → 这里
       │      标题 "Treasure" + N ITEMS
       │      品类 chips 横滚
       │      2 列卡片网格，hero 缩略
       │      点卡片 → Detail
       │
       ├─ Detail (详情) ★ cycle 0002 主体
       │      back 加粗箭头 + 标题
       │      ▽ Hero 卡片：博物馆线描；点击翻面 → "尚未收录实拍" 占位 (3 张旋转空相框 + ×)
       │      ▽ 4 行 hero specs
       │      ▽ 底部 40dp 拉手 (peek 只露拉手)
       │      ▽ 上滑展开抽屉 (78% 屏高，tab 切换不变高):
       │          历史 (timeline + kind 字形 + ★Δ↻+−)
       │          参数 (specs key-value)
       │          影集 (3×3 空灰格 + "添加照片 — coming")
       │          设置 (删除这件物品，AlertDialog 二次确认；后续操作占位)
       │
       ├─ 录入 stub      "对话式录入 · 拍照 → AI 自动识别 — coming"
       └─ 设置 stub       "AI 服务 · BYO API key — coming"
       
                     +──────────────────────+
                     │  门厅 图鉴 录入 设置  │  ← 浮动控制岛 (Detail 屏隐藏)
                     +──────────────────────+
```

视觉系统：

- 字体 Cormorant Garamond + Italic / Space Grotesk / JetBrains Mono（都打包）
- 配色 paper / ink / terra / card / sub / line tokens，浅深双套
- edge-to-edge + statusBarsPadding，控制岛 navigationBarsPadding
- 页面转场左右滑（slideIntoContainer Start/End，300ms tween）
- 11 个博物馆线描插画（10 形状 + Generic 兜底）

数据：

- Room v3，items 一张表，列含 palette / hero_specs / specs / history 全部 JSON 序列化（kotlinx-serialization）
- `Item` 域模型 + `ItemRepository` (Flow)
- 8 条种子物品（移植 prototype/data.jsx），覆盖 4 个品类，全部带 history 时间线
- `fallbackToDestructiveMigration()`（cycle 0001 期未冻 schema）

## 整体工程

```
treasure/
├── android/                Kotlin + Jetpack Compose（Gradle 8.10.2 + AGP 8.7.2 + Kotlin 2.0.21）
│   ├── app/                :app — Compose 屏幕 / 导航 / 主题 / 插画
│   └── core/               :core — domain / Room / Repository / Seed
├── prototype/              Claude Design 原型（活的视觉规格，可双击 Treasure.html）
├── docs/                   长期指引（产品 / 架构 / 视觉 / dev-loop / 5 ADRs）
├── openspec/               变更周期（0001 done, 0002 done）
├── scripts/                bootstrap.sh / prototype-serve.sh / serve-apk.sh
├── backend/                FastAPI 同步占位（cycle 0003+）
├── README.md
└── agent.md                这一份
```

## 之前的 polish round（昨天 2026-05-06 末段）

7 项用户反馈一次过：

1. Add → stub（删 AddViewModel/AddScreen/edit 路由）
2. 顶部 statusBarsPadding，每屏自管 inset；控制岛 navigationBarsPadding
3. 滑动转场（slideIntoContainer Start/End + 300ms）
4. Detail 顶部去 delete，下沉到抽屉"设置" tab
5. 抽屉 peek 降到 40dp，只露拉手
6. 翻面背面重画（3 张空相框 + × + italic 文案 + 添加占位）
7. back 按钮换 Canvas 加粗箭头无文字
8. Grid 副标题简化为 `N ITEMS`
9. Portal 删 EST 日期条 + 三连计数边框

加 `git init` + push 到 GitHub。

## 今天的细修（2026-05-07）

- **抽屉高度统一**：固定 78% 屏高；不同 tab（短的 Settings vs 长的 History）切换不再变高；每个 tab 内部 verticalScroll
- 影集占位从 6 格改为 3×3 = 9 格，看着更像样

## 对接给下一个 agent / 新人

进来按这个顺序读：

1. [`README.md`](README.md) — 60 秒概览
2. **本文件** — 当前状态
3. [`docs/dev-loop.md`](docs/dev-loop.md) — 开发循环（构建 / 装机 / vivo 调试）
4. 浏览器开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html) — 视觉规格
5. [`docs/product.md`](docs/product.md) → [`docs/visual-language.md`](docs/visual-language.md) → [`docs/architecture.md`](docs/architecture.md)
6. [`docs/adr/`](docs/adr/) — 5 份决策记录
7. [`openspec/`](openspec/) — 各 cycle 提案 / 规格 / 笔记

## 下一刀候选（cycle 0003）

按优先级：

1. **真实照片上传** —— 翻面背面"添加照片"接通：相册选择器 / 相机 → 存到 `files/photos/<itemId>/<uuid>.jpg` → 抽屉影集真实显示。schema 不动（照片 path 列表存为 JSON，可以加 `photos_json` 字段，bump v4）。
2. **设置页 AI 服务** —— BYO key form：provider / model / API key → EncryptedSharedPreferences。先把 `core/ai/AiClient.kt` interface 写起来。
3. **对话式录入** —— Add stub 接通：拍照 / 选图 → AI vision extract → 字段自动填 → 用户调整 → 保存。需要 1 + 2 都做完。
4. **AI 生成博物馆插画** —— 用户新增物品时调 AI 生成符合视觉规则的 SVG，cache 本地。`generateIllustration` 在 AiClient 上。
5. **callout 标注** —— 现有插画补 `i · pentaprism` / `ii · grip` 那种 Cormorant 斜体标注线（Compose Canvas 里走 `TextMeasurer`）。

## 历史

| 日期 | 摘要 |
|---|---|
| 2026-05-07 | 抽屉高度统一 + 全文档刷新（本次） |
| 2026-05-06 | cycle 0002 + 7 项 polish + git push 到 GitHub |
| 2026-05-06 | cycle 0001：Chunk B 博物馆线描插画（10 形状 + Generic） |
| 2026-05-06 | cycle 0001：Grid + Nav + Detail + Add/Edit + serialization v2 schema |
| 2026-05-06 | cycle 0001 开工：工程脚手架 → Portal 视觉 → Room 数据层 |
| 2026-05-06 | 项目骨架搭建完成 |
| 2026-05-06 | Claude Design 导出原型；视觉方向锁定为博物馆图鉴风 |

## 给下一个 agent 的备忘

- 改视觉之前一定先打开 `prototype/project/Treasure.html` 对照
- ADR 是钉死的决策。要推翻某个 ADR，写新 ADR 来 supersede 它
- 一个 cycle 一个文件夹（`openspec/NNNN-*/`），一个 cycle 一个改动
- 任何"日期"在文档里写绝对日期（YYYY-MM-DD），不写"上周"
- schema 升级：cycle 0001-0002 期间用 `fallbackToDestructiveMigration()`。**cycle 0003 之后必须开始写真 migration**——会有真用户的数据
- 控制岛在 Detail / Edit 屏隐藏（视觉规格要求）；其它屏显示
- 字体 / SVG / 历史事件等数据移植参考 `prototype/project/{vectors,data}.jsx`
