# Agent · 现状交接

每次工作结束更新这一份。新人 / 新一轮 agent 进来先读它。

## 当前状态 · 2026-05-07

**cycle 0001 → 0008 全部落地。准备交接。**

- APK：`android/app/build/outputs/apk/debug/app-debug.apk` （v0.11.0，13 MB，debug 签名）
- GitHub：<https://github.com/kitacpt/Treasure>（main 分支）
- 上次 commit：`9e75cc0` Cycle 0008
- 测试设备：vivo X200 Pro mini（Android 15）

## 端到端能跑通的功能

```
Portal (默认起点)         门厅：ornament + 大字 Treasure + 三连计数 + 4 扇门 + Latest entry
   ↓ 点扇门 / 控制岛
Grid                      品类网格：标题 + N ITEMS + 横滚 chips + 2 列卡片
   ↓ 点卡片 / Latest entry
Detail (只读)             ←  · (右上 dot 进编辑)
                          Hero (点翻面，有/无照片不同) + 4 行 hero specs
                          底部 40dp 拉手 → 上滑 78% 抽屉
                          抽屉 3 tabs：历史 / 参数 / 影集 (全只读)
                          ↓ 点右上 ·
Edit                      ← 单页表单
                          基础 / 时间 / 标签 / 插画 / 参数(拖动选前 4 hero)
                          / 历史 / 实拍 / DANGER ZONE 删除

Add (RECORD)              chat-first
                          Header: RECORD  [conv ▾]   🕐  ⊕  [手动]
                          Composer 浮在控制岛上方：📷 / 文本 / 🎙 / →
                              📷 → 系统 picker（先请 READ_MEDIA_IMAGES）
                              🎙 → SpeechRecognizer 真转写（先请 RECORD_AUDIO）
                              → → AnthropicClient/OpenAiClient.extractItemDraft
                          AI 出草稿 → DraftCta 卡片
                              点 → Preview 屏 9 字段 + confidence dots → 确认
                              确认 → 写 Room → 跳新 Detail
                          点 [手动] → 4 品类选择弹层 → CategoryForm

Settings                  Provider chips: Anthropic / OpenAI / Custom
                          Model | Base URL（按 provider 显隐）| API Key (mask)
                          [保存] [测试连接]   DANGER ZONE: 清除
                          EncryptedSharedPreferences 存
```

控制岛（4 颗胶囊：门厅 / 图鉴 / 录入 / 设置）浮于底部，Detail / Edit 屏隐藏。

## 视觉系统

- 字体：Cormorant Garamond（含 italic）/ Space Grotesk / JetBrains Mono — 全部打包
- 颜色 token：paper / ink / terra / card / sub / line（浅 + 深双套）
- edge-to-edge + statusBarsPadding；控制岛 navigationBarsPadding
- 滑动转场：300ms `slideIntoContainer(Start/End)`
- 11 个博物馆线描插画（Racket / Camera / Lens / Tripod / Shoes / Car / Laptop / Earbuds / Tablet / Watch / Generic）
- App icon：纸面背景 + 重笔粗黑环 + 顶/底 paper-color rune + terra 中心 dot 三粒（cycle 0008）

## 数据 / AI

- Room **v5**，单表 `items`，所有结构化字段 JSON 列（kotlinx-serialization）
- `Item.specs: List<HeroSpec>` 单列表；前 4 项为 hero（计算属性 `heroSpecs` / `tailSpecs`）
- 真实照片存 `filesDir/photos/<itemId>/<uuid>.jpg`
- 8 条种子物品（移植自 prototype/data.jsx）首启写入
- ⚠️ **Schema 仍 `fallbackToDestructiveMigration()`** — 已 destructive 8 次，cycle 0009 必须切真 migration

AI:
- `core/ai/AiClient` interface + `AnthropicClient` / `OpenAiClient`（OpenAI client 同时覆盖兼容端点）
- 强制 tool-use 结构化输出（fill_item_draft）
- vision：image base64 块；语言：zh-CN
- 用户 BYO key，存 `EncryptedSharedPreferences`
- AI **设备直连 provider**，不走代理（[ADR-0004](docs/adr/0004-byo-ai-key.md)）

## 工程布局

```
treasure/
├── android/                   Kotlin + Jetpack Compose（Gradle 8.10.2 / AGP 8.7.2 / Kotlin 2.0.21）
│   ├── app/                   :app — 屏幕 / VM / 主题 / 插画 / voice / data
│   └── core/                  :core — 域模型 / Room / Repo / Seed / AI clients
├── prototype/                 Claude Design 原型（活的视觉规格）
│   ├── project/               原版 8 画板（cycle 0001–0006）
│   └── add-page-v2/           录入页 v2 设计稿（cycle 0007，HANDOFF.md 解释差异）
├── docs/                      长期指引（product / architecture / visual-language / dev-loop / 5 ADRs）
├── openspec/                  变更周期（0001–0008，每个 1 文件夹 3 文档）
├── scripts/                   bootstrap.sh / prototype-serve.sh / serve-apk.sh
├── backend/                   FastAPI 占位（cycle 0011+ 才接通）
├── README.md
└── agent.md                   这一份
```

## Cycle 一览

| # | 主要内容 | 状态 |
|---|---|---|
| 0001 | MVP：Portal / Grid / Detail，Room 数据层，11 博物馆插画 | done |
| 0002 | 抽屉（历史/参数/影集）+ 明信片翻面 + 7 项视觉 polish | done |
| 0003 | 真实照片上传（Photo Picker + Coil + filesDir）+ 抽屉内嵌编辑（concise 版） | done |
| 0004 | 录入页（4 气泡 + 模板表单 + AI 占位）+ Detail 全字段编辑 | done |
| 0005 | AI 服务接通（Anthropic + OkHttp）+ 设置页 + 编辑屏重做（→ 单页 Edit） | done |
| 0006 | OpenAI / Custom provider + specs 统一为单列表（拖动选前 4）+ 编辑点移右上 + 录入外层留空 | done |
| 0007 | 录入页 v2：chat-first + 草稿预览 + 历史抽屉 + 手动入口 | done |
| 0008 | 录入页 polish + 真 STT + 权限请求 + app 图标 + 6 项反馈修复 | done |

详见 [`openspec/`](openspec/) 各 cycle 的 proposal / spec / notes。

## 对接给下一个 agent

进来按这个顺序读：

1. **本文件** — 当前状态（你在看）
2. [`README.md`](README.md) — 60 秒概览 + 关键决策
3. [`docs/dev-loop.md`](docs/dev-loop.md) — 构建 / 装机 / vivo 调试 / 内循环 / 权限调试
4. 浏览器开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html) — 视觉规格（v1，主体设计）
5. 浏览器开 [`prototype/add-page-v2/project/Treasure.html`](prototype/add-page-v2/project/Treasure.html) — 录入页 v2 设计稿
6. [`docs/product.md`](docs/product.md) → [`docs/visual-language.md`](docs/visual-language.md) → [`docs/architecture.md`](docs/architecture.md)
7. [`docs/adr/`](docs/adr/) — 5 份决策记录
8. [`openspec/`](openspec/) — cycle 0001-0008 提案 / 规格 / 笔记

## 下一刀候选（cycle 0009）

按优先级：

1. **真 schema migration**（最高优先级 · 最大欠债）— 已 destructive 8 次，再不做就要丢用户数据
   - `exportSchema = true`，`schemaLocation = core/schemas/`
   - 把 v1-v5 schema JSON commit
   - 写 `MIGRATION_1_2 / 2_3 / 3_4 / 4_5` Migration 对象
   - 加 `MigrationTest`（Room 提供 `MigrationTestHelper`）
   - 删 `fallbackToDestructiveMigration()`
   - 写 ADR-0006 钉死规矩
2. **历史对话持久化** + **多轮对话**（assistant refine draft）— 录入页两个 stub 接通
   - 新表 `add_conversations` / `add_messages`（或扩展 items？建议独立表）
   - 多轮 send：把消息历史一起喂给 AiClient
3. **拍照**（直调相机）+ **多选照片** — cycle 0003 留下的尾巴
4. **AI 生成博物馆插画** — `AiClient.generateIllustration` + cache `filesDir/illustrations/<id>.svg`
5. **全屏看图浏览器** + **callout 文字标注**

强烈建议 cycle 0009 单独做 (1) — migration 是基础设施，混在功能里容易出错。

## 给下一个 agent 的备忘

- 改视觉之前一定先打开 `prototype/project/Treasure.html` + `prototype/add-page-v2/project/Treasure.html` 对照
- ADR 是钉死的决策。要推翻某个 ADR，写新 ADR 来 supersede 它（cycle 0009 会写 ADR-0006 schema migrations）
- 一个 cycle 一个文件夹（`openspec/NNNN-*/`），proposal → spec → notes 三件套
- 任何"日期"在文档里写绝对日期（YYYY-MM-DD），不写"上周"
- ⚠️ **Schema migration**：cycle 0001-0008 全 `fallbackToDestructiveMigration`。**cycle 0009 起必须切真 migration** —— 用户的录入物品 / 编辑过的 hero specs / 添加的照片都会丢
- 控制岛在 Detail / Edit 屏隐藏（视觉规格要求）；其它屏显示
- 字体 / SVG / 历史事件等数据移植参考 `prototype/project/{vectors,data}.jsx`
- 录入页的具体交互（chat / preview / voice / history）参考 `prototype/add-page-v2/`
- 真 STT 在国行 ROM（华为 / vivo 部分机型）可能不可用 — 已做 `onUnavailable` fallback，不要去掉
- AI key 存 EncryptedSharedPreferences；切勿改成 PlainSharedPreferences

## 历史

| 日期 | 摘要 |
|---|---|
| 2026-05-07 | cycle 0008：录入页 polish + 真 STT + 权限请求 + app 图标 + 6 项反馈修复 |
| 2026-05-07 | cycle 0007：录入页 v2 — chat-first + 草稿预览 + 历史抽屉 + 手动入口 |
| 2026-05-07 | cycle 0006：OpenAI/Custom provider + specs 统一为单列表 + 编辑点移右上 + 录入外层留空 |
| 2026-05-07 | cycle 0005：AI 服务接通 + 编辑屏重做（先在左上点，后改右上） |
| 2026-05-07 | cycle 0004：录入页（4 气泡 + 模板 + AI 占位）+ Detail 全字段编辑 |
| 2026-05-07 | cycle 0003：真实照片 + 抽屉内嵌编辑（concise 版） |
| 2026-05-07 | 抽屉高度统一 + 全文档刷新 |
| 2026-05-06 | cycle 0002 + 7 项 polish + git push 到 GitHub |
| 2026-05-06 | cycle 0001：博物馆插画 + Grid + Nav + Detail + Add/Edit + Schema v2 |
| 2026-05-06 | cycle 0001：工程脚手架 → Portal 视觉 → Room 数据层 |
| 2026-05-06 | 项目骨架搭建完成 |
| 2026-05-06 | Claude Design 导出原型；视觉方向锁定为博物馆图鉴风 |
