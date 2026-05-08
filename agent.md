# Agent · 现状交接

每次工作结束更新这一份。新人 / 新一轮 agent 进来先读它。

## 今天 (2026-05-07)

**状态：cycle 0001 → 0006 全部落地**

最新 APK：`android/app/build/outputs/apk/debug/app-debug.apk` （v0.9.0，13 MB）

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
       ├─ Detail (详情)  纯只读 ★ cycle 0005 + 0006
       │      ┌ ← (back) ································ · (edit dot)
       │      │                                            ↑ 编辑入口已移到右上
       │      ▽ Hero 卡片：博物馆线描；点击翻面（有/无照片不同空状态）
       │      ▽ 4 行 hero specs（item.specs 前 4，只读）
       │      ▽ 底部 40dp 拉手 (peek 只露拉手)
       │      ▽ 上滑展开抽屉 (78% 屏高):  3 tabs 全只读
       │          历史  时间轴 (kind 字形 ★Δ↻+−)
       │          参数  完整 specs 列表，第 4 行后 terra 细线分隔 hero / tail
       │          影集  3 列网格（无添加 / 无删除 affordance）
       │
       ├─ Edit (编辑) ★ cycle 0005 新建 / 0006 重整
       │      ┌ ← (back) ··················· 保存 (dirty 时 terra)
       │      │
       │      EDIT
       │      ── 基础 ─── 品牌 / 型号 / 昵称 / 简介
       │      ── 时间 ─── 购入 / 出手
       │      ── 标签 ─── 状态 chips / 品类 chips
       │      ── 插画 ─── 14 个 HeroVector 横滚缩略，选中 terra
       │      ── 参数 ─── ★ cycle 0006 统一为单列表
       │                  每行 [label] [value] [≡ drag] [− del]
       │                  长按 ≡ 拖动重排；前 4 行作 hero
       │                  第 4 行后 terra 细线 + "↑ 关键 4 项"
       │      ── 历史 ─── 行排版 + tap 编辑 / 长按删 + 加一条
       │      ── 实拍 ─── 3 列 + tile + 长按删
       │      ── DANGER ZONE ─── 删除（二次确认）
       │
       ├─ 录入 (Add)  ★ cycle 0006 留空
       │      Header: Treasure / NEW ENTRY
       │      中间空 + italic "录入页交互重新设计中"
       │      底部 4 颗朴素品类 chip 临时入口（重设计后移除）
       │      （CategoryForm 仍存在；AiChatPanel 已删，等新设计接回 AI）
       │
       └─ 设置 (Settings) ★ cycle 0005 + 0006
              Provider chips: Anthropic / OpenAI / Custom (OpenAI-compatible)
              Model | Base URL（按 provider 显隐）| API Key (password)
              [保存] [测试连接]   DANGER ZONE: 清除所有设置
              EncryptedSharedPreferences 存 provider + key + model + baseUrl
       
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

- Room **v5** (cycle 0006 升级)，items 一张表，列含 palette / specs / history / photos 全部 JSON 序列化
- `Item.specs` 是单列表 `List<HeroSpec>`，前 4 项作 hero（计算属性 `heroSpecs` / `tailSpecs`）
- `Item` 域模型 + `ItemRepository` (Flow)
- 8 条种子物品（移植 prototype/data.jsx），覆盖 4 个品类，全部带 history 时间线，photos 默认空
- 真实照片存 `filesDir/photos/<itemId>/<uuid>.jpg`（app 私有目录）
- `fallbackToDestructiveMigration()`（已经 destructive 6 次了 — **cycle 0007 必须切真 migration**）

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

**中段 cycle 0003 上线**：

- 真实照片：Photo Picker（`PickVisualMedia`）→ `filesDir/photos/<itemId>/<uuid>.jpg` → Item.photos
- Coil 渲染缩略图（`AsyncImage`）
- 抽屉影集 tab 重写：3 列 + tile + 真实照片，长按删（二次确认）
- 翻面背面：≥1 张显示前 3 张预览，0 张保留空相框设计
- 正面 hero "0 PHOTOS" → "${N} PHOTOS"
- 抽屉设置 tab 加 EDIT 区：昵称 / 一句话 / 状态 + 保存按钮（concise 版）
- DetailViewModel 改 AndroidViewModel；加 `addPhoto` / `removePhoto` / `saveEdits`
- Schema v3 → v4：Item 加 `photos: List<String>`，`photos_json` 列
- 加 coil-compose 2.7.0 依赖

**后段 cycle 0004 上线**：

- 录入页换真实现（替换 AddStubScreen）：
  - 手动模式：4 气泡 + 点开 ModalBottomSheet 弹品类模板表单
  - AI 模式：聊天骨架 + "coming · 去设置" 占位
- `CategoryTemplate` 系统：4 个品类各自的 heroSpec 标签预填 + heroVector + palette
- `CategoryGlyph`: 4 个简单线条图标 (Canvas 自绘)
- Detail 抽屉 4 tabs 重洗 → 基础 / 参数 / 历史 / 影集（"设置"消失，删除下沉到基础 DANGER ZONE）
- 全字段编辑：基础 9 字段 / 参数（hero specs + 自由 specs map）/ 历史（增删改）
- DetailViewModel 简化为统一 `update(Item)` 入口

## 对接给下一个 agent / 新人

进来按这个顺序读：

1. [`README.md`](README.md) — 60 秒概览
2. **本文件** — 当前状态
3. [`docs/dev-loop.md`](docs/dev-loop.md) — 开发循环（构建 / 装机 / vivo 调试）
4. 浏览器开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html) — 视觉规格
5. [`docs/product.md`](docs/product.md) → [`docs/visual-language.md`](docs/visual-language.md) → [`docs/architecture.md`](docs/architecture.md)
6. [`docs/adr/`](docs/adr/) — 5 份决策记录
7. [`openspec/`](openspec/) — 各 cycle 提案 / 规格 / 笔记

## 下一刀候选（cycle 0005）

按优先级：

1. **AI 服务真接通**（最高优先级，cycle 0004 留下 AI 录入 stub 等着）：
   - 写 `core/ai/AiClient.kt` interface（`chat` / `visionExtract` / `generateIllustration`）
   - AnthropicClient 实现（设备直连，遵循 [ADR-0004](docs/adr/0004-byo-ai-key.md)）
   - 设置页 → 真页面：provider / model / API key 表单，EncryptedSharedPreferences 存 key
   - Add 的 AI tab 接通：拍照 / 文本 → vision extract → ItemDraft → 跳已预填的手动表单 review → 保存
2. **真 schema migration** —— cycle 0001-0004 全 destructive。cycle 0005 起必须停手；写 v1 → v4 的 Migration 对象 + MigrationTest；删 `fallbackToDestructiveMigration()`
3. **全屏看图浏览器** —— 影集 tap → fullscreen + 拖拽 dismiss + 左右翻
4. **AI 生成博物馆插画** —— 用户新增物品时调 AI 生成符合视觉规则的 SVG，cache 本地；`AiClient.generateIllustration`（依赖 1）
5. **callout 标注** —— 现有 11 个插画补 `i · pentaprism` / `ii · grip` 那种 Cormorant 斜体标注线（Compose Canvas 里走 `TextMeasurer`）

我建议 cycle 0005 = (1) + (2) 一并做：AI 接通把"录入 / 设置"屏闭环，schema migration 把数据安全立起来。两件都是债，越早还越便宜。

## 历史

| 日期 | 摘要 |
|---|---|
| 2026-05-07 | cycle 0004：录入页（气泡 + 模板表单 + AI 占位）+ Detail 全字段编辑 |
| 2026-05-07 | cycle 0003：真实照片 + 抽屉内嵌编辑（concise 版） |
| 2026-05-07 | 抽屉高度统一 + 全文档刷新 |
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
