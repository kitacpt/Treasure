# Agent · 现状交接

每次工作结束更新这一份。新人 / 新一轮 agent 进来先读它。

## 当前状态 · 2026-05-09

**cycle 0001 → 0024 全部落地。**

- APK：`android/app/build/outputs/apk/debug/app-debug.apk` （v0.24.0 候选，14 MB，debug 签名）
- GitHub：<https://github.com/kitacpt/Treasure>（main 分支）
- Schema：v8（cycle 0016 加 avatar_photo_path）
- 测试设备：vivo X200 Pro mini（Android 15）

## 端到端能跑通的功能

```
Main (HorizontalPager)    左右滑切换 4 个 tab：门厅 / 图鉴 / 录入 / 设置
                          控制岛点击 = animateScrollToPage；左右滑 = 高亮跟随

Portal (page 0)           门厅：ornament + 大字 Treasure + 三连计数 + 4 扇门 + Latest entry
   ↓ 点扇门               (set gridCategory + 滑到 Grid)
Grid (page 1)             品类网格：标题 + N ITEMS + 横滚 chips + 2 列卡片
   ↓ 点卡片 / Latest entry → push Detail (NavHost)
Detail (push, 只读)       ←  · (右上 dot 进编辑)
                          Hero (点翻面，有/无照片不同) + 4 行 hero specs
                          底部 40dp 拉手 → 上滑 78% 抽屉
                          抽屉 3 tabs：历史 / 参数 / 影集
                              影集点缩略图 → 全屏 viewer (横滑/缩放/长按加注)
                          ↓ 点右上 ·
Edit (push)               ← 单页表单
                          基础 / 时间 / 标签 (品类用 InlineDropdown) /
                          插画 / 参数(拖动选前 4 hero) / 历史 (类型用
                          InlineDropdown) / 实拍 (📷 拍照 + + 多选)  /
                          DANGER ZONE 删除

Add (page 2, RECORD)      chat-first；对话已落 Room (add_conversations / add_messages)
                          Header: Cormorant `Record` + mono `RECORD` caption
                                  右侧 🕐 (历史抽屉) ⊕ (新对话) [手动]
                          未配置 AI 时顶部出一行 banner（点 → 滑到 Settings）
                          Composer 浮在控制岛上方：📷 / 文本 / 🎙 / →
                              📷 → 系统 picker（先请 READ_MEDIA_IMAGES）
                              🎙 → SpeechRecognizer 真转写（先请 RECORD_AUDIO）
                              → → AiClient.extractItemDraft(text, image, priorTurns)
                                   priorTurns = 当前对话最后 20 条文字消息
                          AI 出草稿 → DraftCta 卡片（多轮可继续 refine）
                              点 → Preview 屏 9 字段 + confidence dots → 确认
                              确认 → 写 Room → 跳新 Detail
                          🕐 → 历史抽屉列出最近 20 段对话；点旧的就 reload
                          点 [手动] → 4 品类选择弹层 → CategoryForm
                              顶部 124dp 大插画 + 56dp 横滚选项（按品类过滤）
                              下面 italic tagline + 各字段 hint
                              EditPageHeader (cancel / 大字 / 保存)

Settings (page 3)         AI 摘要卡（provider + 已配置/未配置 pill +
                          Model / Base URL / 掩码 Key + “调整 →”）
                          点卡片 → 底部抽屉编辑：
                              Provider 下拉（Anthropic / OpenAI / Kimi
                              · Moonshot / DeepSeek / 通义千问 / 智谱 GLM
                              / Xiaomi MiLM / 自定义）
                              Base URL · Model · API Key · 保存 · 测试
                          DANGER ZONE 清除
                          EncryptedSharedPreferences 存
```

控制岛（4 颗胶囊：门厅 / 图鉴 / 录入 / 设置）浮于 pager 底部，Detail / Edit 屏因为是 push 路由所以自然不可见。

## 视觉系统

- 字体：Cormorant Garamond（含 italic）/ Space Grotesk / JetBrains Mono — 全部打包
- 颜色 token：paper / ink / terra / card / sub / line（浅 + 深双套）
- edge-to-edge + statusBarsPadding；控制岛 navigationBarsPadding
- 滑动转场：300ms `slideIntoContainer(Start/End)`
- 11 个博物馆线描插画（Racket / Camera / Lens / Tripod / Shoes / Car / Laptop / Earbuds / Tablet / Watch / Generic）
- App icon：纸面背景 + 重笔粗黑环 + 顶/底 paper-color rune + terra 中心 dot 三粒（cycle 0008）

## 数据 / AI

- Room **v7**（cycle 0010 起 `exportSchema = true`，schema JSON 在 `core/schemas/`）
- 三张表：
    - `items` — 单表 + JSON 列（specs / history / photos / **callouts**）；callouts 是 `Map<path, List<PhotoCallout(x, y, text)>>`
    - `add_conversations` — 录入页对话主表（id / title / 时间戳）
    - `add_messages` — 对话单条（role + payload，按角色取舍字段）
- `Item.specs: List<HeroSpec>` 单列表；前 4 项为 hero（计算属性 `heroSpecs` / `tailSpecs`）
- 真实照片存 `filesDir/photos/<itemId>/<uuid>.jpg`；相机直拍中转 `filesDir/captures/<uuid>.jpg`（FileProvider 暴露给系统相机）
- 8 条种子物品（移植自 prototype/data.jsx）首启写入
- **Migration 制度（[ADR-0006](docs/adr/0006-schema-migrations.md)）**：从 cycle 0010 起每次 schema 改动必须 bump version + 写 Migration + 提交 schema JSON。`Migrations.ALL` 现在装 `MIGRATION_5_6`（加 conversation 两表）+ `MIGRATION_6_7`（items 加 callouts_json）

AI:
- `core/ai/AiClient` interface + `AnthropicClient` / `OpenAiClient`（OpenAI client 同时覆盖兼容端点）
- `extractItemDraft(text, imageJpegBytes?, priorTurns: List<AiTurn>)` — 多轮：把当前对话最后 20 条文字消息按 user/assistant 顺序拼到 prior
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
| 0009 | UI polish · Settings 改造 · 品类模板升级 · 共享 SectionDivider/EditPageHeader | done |
| 0010 | 4 Tab 横滑 · 拍照+多选 · 历史对话持久化+多轮 · 全屏看图+callout · 真 schema migration | done |
| 0011 | 修手动录入弹层 · 历史对话改名删除 · Edit 头像式插画 · Coffee/Wine 品类 + 5 张插画 · MigrationTest | done |
| 0012 | Callout 编辑 / 删除 · 立体魔戒图标（gradient 环 + 高光阴影 + 宝石）  | done |
| 0013 | 性能（IllustPalette 稳定 + @Immutable domain + Pager 默认 lazy + recentConversations 单流）· 修插画变白 · 图标精简（缩小 + 去 3 dot）  | done |
| 0014 | AI 配置加 temperature / thinking · 修 Kimi tool_choice 报错 · IME 遵循输入框 · prompts 同步 6 品类 | done |
| 0015 | Thinking model 自动嗅探（按模型名）· enable_thinking 精确投放 · 测试结果规范化 + 自动滚动 · ToggleRow 简化 | done |
| 0016 | Portal 空品类显示模板插画 · 修品类点击错位（GridRoute LaunchedEffect）· 影集照片当头像（schema v7→v8）· Kimi k 系列嗅探扩展 | done |
| 0017 | 一刀十改：thinking 360s · Portal 去 OWNED · Grid 全部 tab + chip 自动滚 · 头像合并影集 · Edit 删时间区段 · 拖动跨分割线修复 · 录入历史改抽屉 · 手动录入复用 Edit 排版 · 删麦克风按钮 · Settings 重置设置二次确认 | done |
| 0018 | 历史改 ModalBottomSheet · RECORD 副标 = 当前对话主标题（创建即含 HH:MM）· AI 状态灯三档红黄绿 · OkHttp readTimeout 跟随 callTimeout（修 reasoning 模型 timeout 真因） | done |
| 0019 | Grid 选择持久化（修 "全部 → Detail → 返回成酒水"）· AI 闲聊不报错（ChatOnlyResponseException）· 状态灯改 setter invalidate（save 信任绿灯）· 全屏 viewer 单指能翻页（transformable 替代 detectTransformGestures）· 接收 ACTION_SEND/VIEW 分享 · AddPreview 接 Edit 页样式 | done |
| 0020 | 影集放大单指真能翻页（手写 awaitEachGesture）· 上下 64dp 黑边不遮控件 · 分享 URL 真去 fetch 页面（PageFetcher + 移动 UA + HTML strip + og 抽取）喂 AI | done |
| 0021 | 对话框 SelectionContainer 复制粘贴 · PageFetcher 三态返回（Success / Blocked / Failed）+ 拼多多/JD/淘宝壳页启发式识别 · prompt 里明确告诉 AI "客户端拉过被挡了，别回 '无法访问外部链接'" | done |
| 0022 | Record 默认续上次会话（不再每次新建）· fetch 状态对用户可见（SystemNote "正在抓取" / "✓ 已抓取" / "⚠ 防爬挡住"）· PageFetcher 加 charset 探测（GBK 老站点不再乱码）· AI 配置页显示模型多模态能力（启发式判 model 名）· 流式输出明确不做 | done |
| 0023 | 草稿页全面镜像 Edit（基础/标签/参数 三段，AI 填啥就显啥，状态 chip 自选）· 放开 prompt 的 hero spec 模板（AI 按物品挑最重要的 4 条）· 聊天图单击复用 FullscreenPhotoViewer 全屏预览 · Vision pill 双状态（terra "🖼 多模态" / 灰 "纯文本"）+ 删除备注文案 + 摘要卡总是显示 | done |
| 0024 | 会话 = 草稿 大重构（AddUiState.confirmedDraft + proposedDraft、Prompts.buildSystemWithBaseline、DraftCta 三态 Pending/Accepted/Rejected + [采用]/[不要] 按钮、新 DraftConfirmed 行；手动按钮改成进 Refine 编辑 confirmedDraft，CategoryForm 退役）· App 图标改 3D 俯视戒指（透视椭圆 + 内孔上偏 + 前侧壁渐变带）· Grid chip 点击不再自动置首（只在目标离屏时才 animateScrollToItem） | done |

详见 [`openspec/`](openspec/) 各 cycle 的 proposal / spec / notes。

## 对接给下一个 agent

进来按这个顺序读：

1. **本文件** — 当前状态（你在看）
2. [`README.md`](README.md) — 60 秒概览 + 关键决策
3. [`docs/dev-loop.md`](docs/dev-loop.md) — 构建 / 装机 / vivo 调试 / 内循环 / 权限调试
4. 浏览器开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html) — 视觉规格（v1，主体设计）
5. 浏览器开 [`prototype/add-page-v2/project/Treasure.html`](prototype/add-page-v2/project/Treasure.html) — 录入页 v2 设计稿
6. [`docs/product.md`](docs/product.md) → [`docs/visual-language.md`](docs/visual-language.md) → [`docs/architecture.md`](docs/architecture.md)
7. [`docs/adr/`](docs/adr/) — 6 份决策记录
8. [`openspec/`](openspec/) — cycle 0001-0024 提案 / 规格 / 笔记

## 下一刀候选（cycle 0025）

1. **死代码清理**：CategoryForm.kt / ManualCategoryPicker / AddViewModel.saveManual 都在 cycle 0024 没人 mount 了，删
2. **撤销采用**：用户采用 AI 提案后想反悔，目前没法回到上一版 confirmedDraft
3. **PageFetcher headless 渲染**：被 detectBlock 拦下的拼多多 / 重 SPA 页面 fall back 到本地 WebView 真渲染一次拿 DOM
4. **草稿页拖动重排 specs**：抽到 components/ 后 Edit / Refine 两边共用
5. **流式输出**（如果 forced tool-use 也能拆 SSE delta；目前明确推迟）
6. **云端 STT (OpenAI Whisper) 兜底 + 麦克风按钮回归** — cycle 0017 暂去掉的麦克风
7. **多轮 refine 的图片 vision context**
8. **AI 生成博物馆插画**
9. **Settings preset 校准** — Xiaomi MiLM 没公开端点
10. **MigrationTest CI**

## 给下一个 agent 的备忘

- 改视觉之前一定先打开 `prototype/project/Treasure.html` + `prototype/add-page-v2/project/Treasure.html` 对照
- ADR 是钉死的决策。要推翻某个 ADR，写新 ADR 来 supersede 它
- 一个 cycle 一个文件夹（`openspec/NNNN-*/`），proposal → spec → notes 三件套
- 任何 "日期" 在文档里写绝对日期（YYYY-MM-DD），不写 "上周"
- ⚠️ **Schema 不再 destructive**（cycle 0010 / [ADR-0006](docs/adr/0006-schema-migrations.md)）：每改 entity / column 必须 bump `@Database(version)` + 在 `core/room/Migrations.kt` 追加新 Migration + 让 KSP 写出新 schema JSON 一并提交
- 控制岛在 Detail / Edit 屏自然隐藏（这两个是 NavHost push 路由，不在 pager 里）
- 主屏是 `ui/main/MainScreen` 的 HorizontalPager；要在 tab 间跳，set pagerState 而不是 nav.navigate
- Pager 的状态由 `rememberPagerState` + `rememberSaveable gridCategoryId` 维护；从 Detail / Edit 返回会自然落回原 tab
- 字体 / SVG / 历史事件等数据移植参考 `prototype/project/{vectors,data}.jsx`
- 录入页的具体交互（chat / preview / voice / history）参考 `prototype/add-page-v2/`
- 录入页对话已落 Room — 千万别把 `add_conversations` / `add_messages` 表当 stub 删掉
- AiClient 现在接 `priorTurns: List<AiTurn>`，调用方需要构造历史；UserPhoto 的图不进 prior，仅当条 image block
- 真 STT 在国行 ROM（华为 / vivo 部分机型）不可用 — 已做 `onUnavailable` fallback，不要去掉。云端 STT 兜底是 cycle 0011 的活
- 拍照走 FileProvider + `${applicationId}.fileprovider`；如果改 package name 记得同步 manifest
- AI key 存 EncryptedSharedPreferences；切勿改成 PlainSharedPreferences
- Xiaomi MiLM preset 的 base URL 是占位，cycle 0011 要么校准要么删除该 preset

## 历史

| 日期 | 摘要 |
|---|---|
| 2026-05-11 | cycle 0024：会话 = 草稿 大重构 — AddUiState 拆 confirmedDraft + proposedDraft；AI 调用把 confirmedDraft 当 baseline 拼到 system prompt（buildSystemWithBaseline）让 AI 给"下一版"而不是从零；DraftCta 三态（Pending/Accepted/Rejected）+ 右下 [采用]/[不要] 按钮；采用 → append DraftConfirmed 行入 Room + state 升格；supersede 旧 pending 为 Rejected；"手动"按钮改成进 Refine 编辑 confirmedDraft，CategoryForm + ManualCategoryPicker 退役（暂留死代码）；持久化复用 text 列 + 新 draft_confirmed role 不动 schema · 应用图标改 3D 俯视戒指（外圈 24×14 椭圆 + 内孔上偏 4dp + 前侧壁渐变带） · Grid chip 点击不再自动置首（target 在 visibleItemsInfo 不滚） |
| 2026-05-09 | cycle 0023：草稿页全面镜像 Edit（基础 LabeledField × 4 / 标签 status chip + 品类 dropdown / 参数 DraftSpecs 直接渲染 draft.specs 全部行）· 放开 SYSTEM_PROMPT 的 hero spec 模板（AI 按物品挑最重要的，不再是固定 6 品类 4-tuple）· 聊天图单击复用 FullscreenPhotoViewer 全屏预览（多张可横滑，无 callout）· Vision pill 双状态 + 摘要卡总是显示（terra "🖼 多模态" / 灰 "纯文本"，删除"可发图给它认"备注） |
| 2026-05-09 | cycle 0022：Record 默认续上次会话（init 改 observeRecent(1).first()）· fetch 状态可见（AddMessage.SystemNote 5 tone，"正在抓取" → "✓ 已抓取/⚠ 防爬"）· PageFetcher charset 探测（meta charset/http-equiv，GBK 老站点不再乱码）· AI 配置页 model 多模态能力 chip + 编辑抽屉一行小字（启发式 modelSupportsVision）· 流式不做 |
| 2026-05-09 | cycle 0021：对话框复制粘贴（SelectionContainer）· PageFetcher 三态（Success / Blocked / Failed）+ 拼多多 / JD / 淘宝壳页识别 + prompt 引导 AI 不要回 "无法访问外部链接" |
| 2026-05-09 | cycle 0020：全屏看图单指真能翻页（手写 awaitEachGesture）· 64dp 黑边不遮控件 · 分享 URL 真去 fetch 页面给 AI 当 context（PageFetcher + 移动 UA + HTML strip） |
| 2026-05-09 | cycle 0019：Grid 选择持久化 · AI 闲聊回复不再报错 · 状态灯改 setter invalidate（保存信任绿灯）· 全屏 viewer 用 transformable 让单指翻页能用 · 京东/淘宝分享 ACTION_SEND 落到录入 · AddPreview 改 Edit 页样式 |
| 2026-05-09 | cycle 0018：历史改 ModalBottomSheet（与其他抽屉一致从下弹）· RECORD 副标永远 = 当前对话主标题（创建即拼 HH:MM）· AI 状态灯三档红黄绿 + 持久化 lastTestPassed · 修 OkHttp readTimeout 默认 10s 卡 reasoning 模型的真问题 |
| 2026-05-09 | cycle 0017：一刀十改 — thinking 360s · Portal 去 OWNED · Grid 加 "全部" chip + 自动滚到当前 · 头像合并影集（拍照/选/删/换都在头像处）· Edit 删时间区段 · spec 拖动跨分割线修复 · 录入历史改左侧抽屉 + New entry 后缀 · 手动录入完全复用 Edit 排版 · 暂去麦克风按钮 · Settings 重置设置 + 二次确认 |
| 2026-05-09 | cycle 0016：Portal Coffee/Wine 用品类模板插画 · 修品类跳转错位（VM 缓存导致永远停在第一次点的品类）· 影集照片可作头像（schema v8）· Kimi k 系列加入隐式 thinking |
| 2026-05-09 | cycle 0015：Thinking model 自动嗅探 · enable_thinking 精确投放 Qwen/智谱 · 测试结果规范化（kind · detail）+ 自动滚动 · ToggleRow 简化 |
| 2026-05-09 | cycle 0014：AI 配置加 temperature / thinking · 修 Kimi tool_choice 报错（thinking on 时改 auto + 文本回退）· IME 遵循输入框（windowSoftInputMode + imePadding 全屏渗透）· prompts 同步 6 品类 |
| 2026-05-09 | cycle 0013：性能（IllustPalette + @Immutable + Pager 默认 lazy + recentConversations 单流）· 修插画变白 · 图标精简（缩小 + 去三 dot） |
| 2026-05-09 | cycle 0012：Callout 编辑 / 删除 · 立体魔戒图标（gradient 环 + 高光阴影 + radial-gradient 宝石） |
| 2026-05-09 | cycle 0011：修手动录入弹层 · 历史对话改名删除 + 新对话时间戳 · Edit 头像式插画 · Coffee/Wine 品类 + 5 张插画 · MigrationTest 自动化 |
| 2026-05-08 | cycle 0010：4 Tab 横滑 · 拍照+多选 · 历史对话持久化+多轮 · 全屏看图+callout · 真 schema migration（v5→v7） |
| 2026-05-08 | cycle 0009：UI polish · Settings 重写为摘要 + 抽屉 · 共享 SectionDivider / EditPageHeader · 品类模板加 tagline + 字段 hint |
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
