# Agent · 现状交接

每次工作结束更新这一份。新人 / 新一轮 agent 进来先读它。

## 今天 (2026-05-07)

**状态：cycle 0001 + 0002 + 0003 已落地**

最新 APK：`android/app/build/outputs/apk/debug/app-debug.apk` （v0.6.0，12 MB）

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
       ├─ Detail (详情) ★ cycle 0002 + 0003 主体
       │      back 加粗箭头 + 标题
       │      ▽ Hero 卡片：博物馆线描；点击翻面：
       │          0 张照片 → 3 张空相框 + × + "上滑抽屉 · 影集 tab 添加"
       │          ≥1 张  → 前 3 张缩略 + "N 张实拍 · 上滑抽屉看影集"
       │        正面右下角 "${N} PHOTOS · TAP TO FLIP" 角标
       │      ▽ 4 行 hero specs
       │      ▽ 底部 40dp 拉手 (peek 只露拉手)
       │      ▽ 上滑展开抽屉 (78% 屏高，tab 切换不变高):
       │          历史 (timeline + kind 字形 + ★Δ↻+−)
       │          参数 (specs key-value)
       │          影集 (3 列网格：+ tile + 真实照片，长按删) ★ cycle 0003
       │          设置:
       │            EDIT 区：昵称 / 一句话 / 状态 + 保存修改  ★ cycle 0003
       │            MANAGE 区：删除（二次确认）
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

- Room **v4**，items 一张表，列含 palette / hero_specs / specs / history / photos 全部 JSON 序列化（kotlinx-serialization）
- `Item` 域模型 + `ItemRepository` (Flow)
- 8 条种子物品（移植 prototype/data.jsx），覆盖 4 个品类，全部带 history 时间线，photos 默认空
- 真实照片存 `filesDir/photos/<itemId>/<uuid>.jpg`（app 私有目录）
- `fallbackToDestructiveMigration()`（cycle 0001-0003 期未冻 schema；cycle 0004 之后必须改）

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

## 今天的进度（2026-05-07）

**早段 polish**：

- 抽屉高度统一：固定 78% 屏高；tab 切换不再变高；每 tab 内 verticalScroll
- 全文档刷新（agent.md / openspec/0002 spec+notes / docs/dev-loop.md / README / architecture.md）
- 推 commit `05e8807` 到 GitHub

**后段 cycle 0003 上线**：

- 真实照片：Photo Picker（`PickVisualMedia`）→ `filesDir/photos/<itemId>/<uuid>.jpg` → Item.photos
- Coil 渲染缩略图（`AsyncImage`）
- 抽屉影集 tab 重写：3 列 + tile + 真实照片，长按删（二次确认）
- 翻面背面：≥1 张显示前 3 张预览，0 张保留空相框设计
- 正面 hero "0 PHOTOS" → "${N} PHOTOS"
- 抽屉设置 tab 加 EDIT 区：昵称 / 一句话 / 状态 + 保存按钮（dirty 启用）
- DetailViewModel 改 AndroidViewModel；加 `addPhoto` / `removePhoto` / `saveEdits`
- Schema v3 → v4：Item 加 `photos: List<String>`，`photos_json` 列
- 加 coil-compose 2.7.0 依赖

## 对接给下一个 agent / 新人

进来按这个顺序读：

1. [`README.md`](README.md) — 60 秒概览
2. **本文件** — 当前状态
3. [`docs/dev-loop.md`](docs/dev-loop.md) — 开发循环（构建 / 装机 / vivo 调试）
4. 浏览器开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html) — 视觉规格
5. [`docs/product.md`](docs/product.md) → [`docs/visual-language.md`](docs/visual-language.md) → [`docs/architecture.md`](docs/architecture.md)
6. [`docs/adr/`](docs/adr/) — 5 份决策记录
7. [`openspec/`](openspec/) — 各 cycle 提案 / 规格 / 笔记

## 下一刀候选（cycle 0004）

按优先级：

1. **设置页 AI 服务**（`SettingsStubScreen` → 真页面）+ **Add stub 接通**：BYO key 表单（Anthropic / OpenAI / 自定义 endpoint），EncryptedSharedPreferences；写 `core/ai/AiClient.kt` interface + 一个 Anthropic 实现；Add 屏拍照 / 选图 → AI vision extract → 字段自动填
2. **AI 生成博物馆插画** —— 用户新增物品时调 AI 生成符合视觉规则的 SVG，cache 本地；`AiClient.generateIllustration`
3. **全屏看图浏览器** —— 影集 tap → fullscreen + 拖拽 dismiss
4. **真 schema migration** —— cycle 0001-0003 全靠 `fallbackToDestructiveMigration`，cycle 0004 之后必须停手
5. **callout 标注** —— 现有 11 个插画补 `i · pentaprism` / `ii · grip` 那种 Cormorant 斜体标注线（Compose Canvas 里走 `TextMeasurer`）

## 历史

| 日期 | 摘要 |
|---|---|
| 2026-05-07 | cycle 0003：真实照片 + 抽屉内嵌编辑（本次后段） |
| 2026-05-07 | 抽屉高度统一 + 全文档刷新（本次前段） |
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
