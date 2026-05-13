# Agent · 现状交接

每次工作结束更新这一份。新人 / 新一轮 agent 进来先读它。

## 当前状态 · 2026-05-13

**cycle 0001 → 0033 全部落地。**

- APK：`android/app/build/outputs/apk/debug/app-debug.apk` （v0.33.0 候选，debug 签名）
- GitHub：<https://github.com/kitacpt/Treasure>（main 分支）
- Schema：**v13**（cycle 0033 加 `items.sort_order`；cycle 0032 加 `add_messages.action_kind/target_id`；cycle 0031 加 `conversation_items` 表；cycle 0030 加 `hero_photo_path`；v5→v13 全程 `addMigrations`，没再 destructive）
- Seeds：6 物品（每内建分类 1 条样例）+ 6 内建分类。fresh install 经 `SeedCategoriesCallback` 写分类、`ItemRepository.ensureSeeded` 写物品；老升级用户经 Migration_8_9。用户清掉后不会再生
- 测试设备：vivo X200 Pro mini（Android 15）

## 端到端能跑通的功能

```
Main (HorizontalPager)    左右滑切换 4 个 tab：门厅 / 图鉴 / 录入 / 设置
                          控制岛点击 = animateScrollToPage；左右滑 = 高亮跟随
                          BackHandler：非 Portal tab → 回 Portal；Manager 抽屉
                          打开时优先收抽屉（cycle 0029）

Portal (page 0)           门厅：底部 + 顶部 Ornament + 大字 Treasure (displayLarge)
                          + 英文 tagline + 三连计数 + ✦ The Rooms ✦ +
                          可见分类 doorway 网格 + ✦ Latest entry ✦
                          隐藏分类不出 doorway / 不参与 latestOverall（cycle 0028）
                          doorway 永远用 CategoryInfo.heroVector 或 heroPhotoPath
                          (cycle 0030)。所有分类隐藏 / 没物品 → italic 文案 +
                          "去分类管理 →" 链接（cycle 0028）
   ↓ 点扇门               (set gridCategoryId + 滑到 Grid)

Grid (page 1)             品类网格：标题 + N ITEMS + 横滚 chips + 2 列卡片
                          右上工具栏（cycle 0026/0029）：[🔍 搜索] [小红点 ·
                          分类管理]
                          chip 点击不再自动置首；只在目标离屏时滚（cycle 0024）
                          全部 chip 只算 visibleItems；当前 chip 分类被隐藏
                          时自动 fallback "全部"（cycle 0028/0030）
   ↓ 点 🔍                push SearchRoute：BackArrow + 圆角搜索框（auto focus
                          + IME 弹出）+ brand/model/nickname contains 实时过滤
                          + 命中段 terra/SemiBold 高亮 + 2 列结果（cycle 0029）
   ↓ 点小红点             弹 ModalBottomSheet CategoryManager（cycle 0026/0028/0030）：
                          [分类管理] / [+ 新增分类]
                          单一拖动 list：visible 段 / divider 块 / hidden 段
                          长按左侧 ≡ 握把 + 拖动 → 同段重排 sort_order；跨过
                          divider toggle hidden；松手实时提交
                          每行右侧小红点 → push CategoryEditor 全屏（cycle 0029）
   ↓ 点卡片 / Latest entry → push Detail (NavHost)

CategoryEditor (push)     EditPageHeader + BackArrow（跟物品 Edit 同款）
                          顶部 112dp 圆 Avatar：内建有默认线描兜底，自定义
                          必须从相册挑图（cycle 0030：PickVisualMedia + AsyncImage）
                          中文名 / 英文名 / 显示状态 toggle / 删除（仅自定义，
                          AlertDialog 二次确认；删后里面 items 自动 rehome 到
                          电子产品兜底 — cycle 0027）

Detail (push, 只读)       ←  · (右上 dot 进编辑)
                          Hero (点翻面，有/无照片不同) + 4 行 hero specs
                          底部 40dp 拉手 → 上滑 78% 抽屉
                          抽屉 3 tabs：历史 / 参数 / 影集
                              影集点缩略图 → 全屏 viewer (横滑/缩放/长按加注)
                          ↓ 点右上 ·
Edit (push)               ← 单页表单
                          基础 / 标签 (品类 InlineDropdown 拉仓库 visibleCategories
                          + 当前 id 不在仓库列表时给伪 CategoryInfo 占位)
                          / 插画 / 参数(拖动选前 4 hero) / 历史 (类型 InlineDropdown)
                          / 实拍 (📷 拍照 + + 多选) / DANGER ZONE 删除

Add (page 2, RECORD)      chat-first；对话已落 Room (add_conversations / add_messages)
                          Header: Cormorant `Record` + 副标 = 当前对话标题
                                  右侧 🕐 (历史抽屉) [手动]
                          Cycle 0022 起：进入默认续上次对话，不再每次新建空壳
                          未配置 AI 时顶部出 banner（点 → 滑到 Settings）
                          Composer 浮在控制岛上方：📷 / 文本 / →
                              📷 → 系统 picker（先请 READ_MEDIA_IMAGES）→ 上
                                  传图自动喂 AI；点聊天里的图 → 全屏 viewer
                                  (cycle 0023)
                              → → AiClient.extractItemDraft(text, image,
                                   priorTurns, baseline = confirmedDraft,
                                   categoryHints = visibleCategories) — cycle
                                   0024/0027
                              发 URL → PageFetcher 拉页面（mobile UA + HTML
                                  strip + meta charset 探测）+ SystemNote 显
                                  示 "正在抓取" / "✓ 已抓取" / "⚠ 防爬挡住"
                                  (cycle 0020/0021/0022)
                          ✦ 会话 = 草稿（cycle 0024）：
                              AI 跑出 DraftCta(status=Pending)，右下角
                              [采用]/[不要] 按钮。每段对话只有一个 pending；
                              新提案到来时把旧的标 Rejected
                              [采用] → confirmedDraft = proposedDraft；
                                       append 一行 ✓ 已采用 · N 字段（落 Room）
                              [不要] → proposedDraft 清掉
                              点 [手动] → push 进 Refine 页（之前是 CategoryForm
                                       弹层，cycle 0024 退役）
                                       Refine 页改的是 confirmedDraft
                              Refine 页 [确认收入] → AlertDialog 二次确认
                                       (cycle 0025) → 写 Room → 新会话
                          🕐 → 历史抽屉（ModalBottomSheet，cycle 0018）列出
                                最近 20 段对话；点旧的就 reload

Settings (page 3)         AI 摘要卡（provider + 已配置/未连通/已连通 三态 pill
                          + Model 行 + 🖼 多模态 / 纯文本 pill +
                          Base URL + 掩码 Key + "调整 →"，cycle 0018/0022/0023）
                          点卡片 → 底部抽屉编辑：
                              Provider 下拉（Anthropic / OpenAI / Kimi
                              · Moonshot / DeepSeek / 通义千问 / 智谱 GLM
                              / Xiaomi MiLM / 自定义）
                              Base URL · Model · API Key · 高阶 (temperature /
                              thinking) · 保存 · 测试
                          DANGER ZONE 重置设置（AlertDialog 二次确认）
                          EncryptedSharedPreferences 存
```

控制岛（4 颗胶囊：门厅 / 图鉴 / 录入 / 设置）浮于 pager 底部，Detail / Edit /
Search / CategoryEditor 屏因为是 push 路由所以自然不可见。

## 视觉系统

- 字体：Cormorant Garamond（含 italic）/ Space Grotesk / JetBrains Mono — 全部打包
- 颜色 token：paper / ink / terra / card / sub / line（浅 + 深双套）
- edge-to-edge + statusBarsPadding；控制岛 navigationBarsPadding
- 滑动转场：300ms `slideIntoContainer(Start/End)`
- 16 个博物馆线描插画（Racket / Shoes / Camera (DSLR + 旁轴共享) / Lens /
  Tripod / Car (轿车 + SUV 共享) / Laptop / Tablet (Kindle 也共享) /
  Earbuds / Watch / EspressoMachine / CoffeeGrinder / CoffeeBean /
  WineBottle / CocktailGlass / Generic）
- App icon：cycle 0026 回到 cycle 0013 平面版（外圈 23 内圈 17.5 圆环 +
  forged-gold gradient + 顶/底 paper-color rune + 两侧 tick + 内边 bevel 高光
  暗弧）。3D 几个版本（cycle 0024/0025）作废留 git 历史

## 数据 / AI

- Room **v10**（cycle 0010 起 `exportSchema = true`，schema JSON 在 `core/schemas/`）
- 四张表：
    - `items` — 单表 + JSON 列（specs / history / photos / **callouts**）；
      `Item.category` 是 String id（cycle 0027 起，自定义分类也能装；老存储列
      本来就 TEXT，不需要 migration）
    - `add_conversations` — 录入页对话主表（id / title / 时间戳）
    - `add_messages` — 对话单条（role + payload，按角色取舍字段）。Cycle 0024
      起 role 加了 `draft_confirmed`，DraftCta 的 status 复用 text 列存
    - `category_prefs` — cycle 0026 起的分类元数据：内建（built_in=1）+ 自定义
      (built_in=0) 同一张表，含 hidden / sort_order / hero_vector / hero_photo_path
      / name_zh / name_en
- `Item.specs: List<HeroSpec>` 单列表；前 4 项为 hero（计算属性 `heroSpecs` / `tailSpecs`）
- 真实照片存 `filesDir/photos/<itemId>/<uuid>.jpg`；相机直拍中转 `filesDir/captures/<uuid>.jpg`（FileProvider）。
  Cycle 0030：分类代表图存 `filesDir/category-photos/<categoryId|"tmp">/<uuid>.jpg`
- 8 条种子物品 + 6 条 category_prefs 种子（Migration_8_9 直接写）首启 / 升级时落地
- **Migration 制度（[ADR-0006](docs/adr/0006-schema-migrations.md)）**：从 cycle
  0010 起每次 schema 改动必须 bump version + 写 Migration + 提交 schema JSON。
  `Migrations.ALL` 当前：5_6（conversation 两表）/ 6_7（items+callouts_json）/
  7_8（items+avatar_photo_path）/ 8_9（category_prefs 表 + 种子）/ 9_10
  (category_prefs+hero_photo_path)

AI:
- `core/ai/AiClient` interface + `AnthropicClient` / `OpenAiClient`（OpenAI client 同时覆盖兼容端点）
- `extractItemDraft(text, imageJpegBytes?, priorTurns, baseline, categoryHints)`：
  - `priorTurns`：当前对话最后 20 条文字消息（cycle 0010 起）
  - `baseline`：上一版已采用的草稿，AI 在它基础上 propose 下一版（cycle 0024）
  - `categoryHints`：当前可用分类列表（内建 + 未隐藏自定义），让 AI 选对的
    category id（cycle 0027 起，包括 "custom-uuid"）
- 强制 tool-use 结构化输出（fill_item_draft），cycle 0027 把 category.enum 移
  到 description 自由文本，由 categoryHints 在 system prompt 末尾约束
- 思考模型自动嗅探（kimi-k* / o1-3* / 名字含 thinking）→ tool_choice 自动 auto
  + readTimeout/callTimeout 360s（cycle 0015/0018）
- vision：image base64 块；语言：zh-CN
- 用户 BYO key，存 `EncryptedSharedPreferences`
- AI **设备直连 provider**，不走代理（[ADR-0004](docs/adr/0004-byo-ai-key.md)）

## 分类系统（cycle 0026-0030）

- `Category` enum 留作 6 个内建模板的 map key（CategoryTemplates 还按 enum
  查 hero spec labels / palette / tagline / defaultHeroVector）— **不再决定**
  Item.category 的合法值
- 真相在 `core/repo/CategoryRepository`（Room 后端），返回 `CategoryInfo(id,
  nameZh, nameEn, heroVector, heroPhotoPath, hidden, sortOrder, isBuiltIn)`
- 内建 6 个的 hero_vector 在 toDomain 时被 override 成 `Category.defaultHeroVector`
  （cycle 0028 修了 cycle 0026 种子写 GENERIC 的 bug）
- Portal doorway / Grid chip / AddPreview / EditScreen 的品类下拉、AI prompt
  的 categoryHints — 都读这个仓库
- 删自定义分类：`reassignItemsToTech` 兜底，items 不丢（cycle 0027）

## 工程布局

```
treasure/
├── android/                   Kotlin + Jetpack Compose（Gradle 8.10.2 / AGP 8.7.2 / Kotlin 2.0.21）
│   ├── app/                   :app — 屏幕 / VM / 主题 / 插画 / voice / data
│   │   src/main/java/com/treasure/
│   │   ├── ui/
│   │   │   ├── nav/           Routes + TreasureNavHost (Main / Detail / Edit /
│   │   │   │                   Search / CategoryNew / CategoryEdit)
│   │   │   ├── main/          MainScreen + ControlIsland + BackHandler
│   │   │   ├── portal/        PortalScreen + PortalViewModel
│   │   │   ├── grid/          GridScreen + GridViewModel + 右上工具栏 + 搜索 icon
│   │   │   ├── detail/        DetailScreen + DetailViewModel
│   │   │   ├── edit/          EditScreen + (用 DetailViewModel 复用)
│   │   │   ├── add/           AddRoute + AddChat + AddPreview + AddViewModel
│   │   │   │                   + CategoryTemplate + CategoryForm (cycle 0024
│   │   │   │                   起退役但文件还在)
│   │   │   ├── settings/      SettingsRoute + SettingsViewModel
│   │   │   ├── search/        SearchRoute (cycle 0029 新加)
│   │   │   ├── category/      CategoryManager (cycle 0026 起的抽屉) +
│   │   │   │                   CategoryEditorRoute (cycle 0029 拆出) +
│   │   │   │                   CategoryManagerViewModel
│   │   │   ├── photo/         FullscreenPhotoViewer
│   │   │   └── components/    EditPageHeader / BackArrow / ControlIsland /
│   │   │                       HeroAvatar / HeroAvatarPicker / InlineDropdown
│   │   │                       / Ornament / SectionDivider
│   │   ├── illust/            16 个 Compose Canvas 博物馆插画
│   │   ├── voice/             SpeechRecognizer 封装
│   │   ├── data/              SettingsStore + AiProviderPreset
│   │   ├── theme/             Theme / Color / Type
│   │   ├── TreasureApp.kt     Application：ServiceLocator 装 repo + 仓库
│   │   └── MainActivity.kt    + Share intent consume
│   └── core/                  :core — 域模型 / Room / Repo / Seed / AI / Web
│       src/main/java/com/treasure/core/
│       ├── domain/            Item / Category / CategoryInfo / HeroVector /
│       │                       HeroSpec / HistoryEvent / ItemStatus / PhotoCallout
│       ├── ai/                AiClient + AnthropicClient + OpenAiClient +
│       │                       Prompts (CategoryHint / SYSTEM_PROMPT +
│       │                       buildSystemWithBaseline) + ItemDraft (data class)
│       ├── repo/              ItemRepository + AddConversationRepository +
│       │                       CategoryRepository
│       ├── room/              TreasureDatabase v10 + 4 entities + 3 daos +
│       │                       Migrations.ALL（5 个 migration）
│       ├── seed/              SeedItems：8 条种子物品
│       └── web/               PageFetcher（cycle 0020-0022）
│   schemas/com.treasure.core.room.TreasureDatabase/  5–10.json（exportSchema）
├── prototype/                 Claude Design 原型（活的视觉规格）
│   ├── project/               原版 8 画板（cycle 0001–0006）
│   └── add-page-v2/           录入页 v2 设计稿（cycle 0007，HANDOFF.md 解释差异）
├── docs/                      长期指引（product / architecture / visual-language / dev-loop / 6 ADRs）
├── openspec/                  变更周期（0001–0030，每个 1 文件夹 3 文档）
├── scripts/                   bootstrap.sh / prototype-serve.sh / serve-apk.sh
├── backend/                   FastAPI 占位（始终是空脚手架，本地没起，
│                              ADR-0003 留的同步接口未真接）
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
| 0025 | 戒指图标 v3：椭圆压扁到 ry/rx=0.36 + 删前侧壁带第二 path + 删 rune，回归"单一 evenOdd 圆环 + 顶亮底暗渐变"清晰 ring 观感 · 草稿页 [确认收入] 加 AlertDialog 二次确认（仿 Settings 重置设置同款样式） | done |
| 0026 | 图标回退到 cycle 0013 平面版（用户说 3D 几版越来越丑）· **分类管理大刀**：schema v9 加 category_prefs 表 + Migration 种子 6 内建；新 CategoryInfo 领域 + CategoryRepository；Grid 右上小红点 → ModalBottomSheet 管理器（显示中 / 已隐藏 + 分割线，每行编辑 + toggle，标题右侧 + 新增）；编辑子页改名 / 换插画 / 显示状态 / 删除（AlertDialog 二次确认，仅自定义）；Portal doorways + Grid chip 按 visibleCategories 渲染。自定义分类暂时不能装物品（cycle 0027 候选） | done |
| 0027 | 自定义分类能装物品 — `Item.category` 从 `Category` enum 改 `String`（无 schema migration，列本来就是 TEXT，cycle 0027 只改 domain 强转）· AI prompt 加动态 categoryHints（内建+未隐藏自定义都喂给 AI）· tool schema 去 enum 改 description · `AddPreview` / `EditScreen` 的品类 InlineDropdown 改读 categoryRepo 仓库 · HeroAvatarPicker 接 String id · 新 `heroVectorOptionsForId` 自定义分类回 HeroVector.entries 全集 · 删自定义分类时 `reassignItemsToTech` 兜底，物品不丢 | done |
| 0028 | 隐藏真生效（Grid/Portal "全部"/total/latest 都过 visibleItems）· Portal LATEST ENTRY 标签居中两边加 ✦ + 全空状态 + "去分类管理 →" 链接 · Portal doorway 永远用 `info.heroVector`（"基础图" 不跟物品） · Manager 重写：删副标题 + 删 [隐藏/显示] pill + 删 [完成]、"编辑"→小红点、左侧三横纹握把长按拖动 + 跨分割线 toggle hidden · Editor 顶部 112dp Avatar + 插画必填 + 内建锁定 · Category enum 加 `defaultHeroVector`，repo override 内建 heroVector（修 cycle 0026 种子的 'GENERIC' bug） | done |
| 0029 | BackHandler：非 Portal tab 返回回 Portal（之前各处直接退出应用）· Manager 抽屉只剩 List（删 inline Editor + 删底部 italic 提示）· CategoryEditor 拆全屏路由 `category/{new\|edit/{id}}`，复用 EditPageHeader + BackArrow 与物品 Edit 页同款 · 图鉴右上工具栏从 [小红点] 变 [🔍][小红点] · 新 SearchRoute：BackArrow + 圆角搜索框（自动 focus 弹 IME）+ 实时过滤 brand/model/nickname + 命中段 terra 高亮 + 2 列 grid 结果 | done |
| 0030 | Manager 拖动重写（divider 当 row-height 块占一个 visualSlot，"拖到最底"能跨过分割线了；新 computeShift / commitDrag 纯函数）· Schema v10 加 `hero_photo_path` 列 · CategoryEditor 插画改 PickVisualMedia 相册 picker（自定义新建必填、内建可覆盖默认插画）；AvatarHero 优先 AsyncImage 显示 photo；删 HeroVectorRow + heroLabel · Search 加 visibleIds 过滤 · GridViewModel：当前 chip 分类被隐藏后 effectiveSelectedId fallback null · Portal GrandTitle 还原 cycle 0023 大字 displayLarge + 英文 tagline + 底部 Ornament 补回（cycle 0026 重写时改坏了，本次还原） | done |
| 0033 | (a) 录入页编辑/草稿入口接入影集管理 — `ItemDraft.photos`/`avatarPhotoPath` + `addDraftPhoto/setDraftAvatar/removeDraftPhoto/persistDraftPhoto`，`AddPreview` 的 `HeroAvatarPicker` 接 photo callbacks；(b) 拍 / 选照片走新 `CropScreen` 做基础裁剪（free-form 矩形 + 4 角 / 4 边 drag）后落 `filesDir/draft-photos/<convoId>/<uuid>.jpg`；(c) commitDraft 把 draft.photos / avatar 带进 Item，MODIFIED 行 commit 时保留原 item id / createdAt / photos；(d) Drawer flash 修复 — SAVED 行点击 / Refine 进入时显式 `itemDrawerOpen = false`，跨导航用 `reopenDrawerOnResume` 配合 `Lifecycle.Event.ON_RESUME` 回来重开；(e) Grid 长按 → 编辑态：`GridViewModel.selecting/selectedIds/enterEditMode/exitEditMode/toggleSelection/deleteSelected/reorder`、`EditHeader` 替换 [Edit + 红点] 为 `[完成] · 已选 N · [删除(N)] [编辑(N)]`、1-列 `EditReorderableList` 长按拖把手调序；(f) Schema v13 — `items.sort_order`（默认回填 `-created_at`、`ItemDao` 排序改 `sort_order ASC, acquired DESC`、新物品 commit 取 `min - 1`、MODIFY commit 保持原 sortOrder）；(g) 新 `gridIntake: MutableStateFlow<List<String>?>` + `AddViewModel.startConversationFromItems` — 编辑态 [编辑] 把选中物品扔到 AddRoute 起新会话，自动开 drawer | done |
| 0032 | 多 action 录入协议（核心修复用户报的"4 件物品 AI 只录入一件 + 覆盖上一个草稿"）：(a) 协议从 `extractItemDraft` 改 `extractItemDrafts` 返 `List<DraftAction>`，tool 名 `submit_drafts`、actions[]、kind=create/modify、target_id；(b) system prompt 加 `[CONVERSATION WORKING SET]` 块（每行 id + status + 标题 + category + oneLiner + specs），AI 据此判断 create vs modify；(c) 中间形态 v1：AI 一回复就把所有 actions 落工作集（drawer 直接刷 N 件）；(d) v2 复修：用户要求"先逐张确认"——一个 action 对应一张 DraftCta 卡，accept 才落到工作集（[applyAcceptedCta](android/app/src/main/java/com/treasure/ui/add/AddViewModel.kt)）；(e) Schema v12 — `add_messages` 加 `action_kind` / `target_id` 两列（旧行 NULL → Create）；(f) DraftCta 卡片渲染 "修改 ·" / "新增" 标签；(g) max_tokens 1024 → 4096（thinking 8192），原 1024 在 4 件物品 × 8 specs 时 JSON 截断；(h) drawer `rememberSaveable` + drawer 状态点 SAVED 行不主动收（AddChat 整体隐藏 = drawer 跟着隐，回 Chat 自动复现）；(i) ItemListDrawer 长按胶囊弹删除二次确认 — SAVED 只从工作集移除（不动物品）/ PENDING+MODIFIED 丢草稿；(j) ListIcon 替换 Draft 胶囊 + 工作集计数小红点 | done |
| 0031 | 长 cycle，多轮迭代：(a) 返回栈优先级 — AddRoute / SettingsScreen / DetailScreen 各自局部 BackHandler 拦截子层；(b) 拖动数学复修 — CategoryManager 按"预览终态布局"渲染 + `rememberUpdatedState` 兜 stale lambda + `indexOfFirst(info.id)` + DAO `@Transaction reorder(orderedIds, hiddenIds)` 单事务避免中间态 emit；(c) HeroVector 去重 — `canonical()` + `uniqueHeroVectors`，picker 不再 3/4、7/8、10/12 重复；(d) 历史对话删 current 改 resume 上一段不再 spawn 空壳 + ✎/✕ 36dp visibility；(e) Theme 切换 — `darkModeOverride: MutableStateFlow` 在 Settings header `☀/☾` icon；(f) Portal 空态新 [Door](android/app/.../illust/Door.kt) 大门 + "点开大门，展示你的专属 treasure"；(g) Detail 抽屉 3 页 `HorizontalPager` + 影集 + tile + 长按多选 + 底部删除条 + 二次确认 + drag-handle hint 文字 + 抽屉删书签；(h) Grid 标题动态两行（`TextMeasurer` 同行同步）+ 搜索按钮挪到 chip 条最左、点击原地变 `SearchInputBar` 实时过滤 + Edit + 红点 跟 Treasure 标题 baseline 对齐；(i) Edit 页大美化 — "参数" / "操作" 区名，"+" 单字号按钮，spec 行卡片化（key+value 共框 + 中间竖分隔 + 握把/✕ 不带框），DANGER ZONE 注释删，divider 提示行去掉两侧横线；(j) 历史 add/edit 抽屉化 — `ModalBottomSheet` + 顶部 emoji icon picker（🛒🏆🔧⚙️👋）+ Material3 `DatePicker` + 中文长日期 `2026 年 5 月 12 日`；(k) `ItemDraft.history` + `setDraftHistory` + AddPreview 复用 `HistorySection`（提为 internal）；(l) AddChat "手动" → "Draft"；(m) Schema 不变；(n) `SeedCategoriesCallback` 补 fresh-install 分类种子（cycle 0026 那 migration 只覆盖升级用户）；(o) 物品种子 8 → 6（每个内建分类 1 条，新增咖啡 MaraX + 酒水 Margaux 2015） | done |

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
8. [`openspec/`](openspec/) — cycle 0001-0031 提案 / 规格 / 笔记

## 下一刀候选（cycle 0034）

1. **专用拍照 launcher**：cycle 0033 的 Refine 拍照 / 选图都退到 PickVisualMedia 一条路；vivo 系统 picker 多数已带拍照入口，但单拍意图用 `ActivityResultContracts.TakePicture` + `FileProvider` 走更顺
2. **裁剪页旋转 / aspect lock**：现在只 free-form 矩形；用户若反馈"旋转一下"再补
3. **Detail / EditScreen 的影集 add 也走裁剪**：cycle 0033 只接到了 Refine 一侧；Edit 页 add photo 还是裸 PickVisualMedia
4. **死代码清理**：CategoryForm.kt / ManualCategoryPicker / AddViewModel.saveManual 自 cycle 0024 起没人调
5. **撤销采用 / 撤销 commit**：DraftCta accept 后想反悔，目前要靠 drawer 长按删；想要在 chat 卡片侧加 "撤销" 也行
6. **AI prompt 的 hero spec 模板提示按自定义分类适配** — 现在只有内建 6 个 example；如用户反馈"图书的 hero specs 太离谱"再做
7. **PageFetcher headless 渲染**：被 detectBlock 拦下的拼多多 / 重 SPA 页面 fall back 到本地 WebView 真渲染一次拿 DOM
8. **流式输出**（如果 forced tool-use 也能拆 SSE delta；目前明确推迟）
8. **云端 STT (OpenAI Whisper) 兜底 + 麦克风按钮回归** — cycle 0017 暂去掉的麦克风
9. **多轮 refine 的图片 vision context**
10. **AI 生成博物馆插画**
11. **Settings preset 校准** — Xiaomi MiLM 没公开端点
12. **MigrationTest CI**

## 给下一个 agent 的备忘

- 改视觉之前一定先打开 `prototype/project/Treasure.html` + `prototype/add-page-v2/project/Treasure.html` 对照
- ADR 是钉死的决策。要推翻某个 ADR，写新 ADR 来 supersede 它
- 一个 cycle 一个文件夹（`openspec/NNNN-*/`），proposal → spec → notes 三件套
- 任何 "日期" 在文档里写绝对日期（YYYY-MM-DD），不写 "上周"
- ⚠️ **Schema 不再 destructive**（cycle 0010 起，[ADR-0006](docs/adr/0006-schema-migrations.md)）：每改 entity / column 必须 bump `@Database(version)` + 在 `core/room/Migrations.kt` 追加新 Migration + 让 KSP 写出新 schema JSON 一并提交。**绝对禁止** `fallbackToDestructiveMigration()`（dev 环境降级用 `fallbackToDestructiveMigrationOnDowngrade` 是 OK 的）
- 控制岛在 Detail / Edit / Search / CategoryEditor 屏自然隐藏（这些是 NavHost push 路由，不在 pager 里）
- 主屏是 `ui/main/MainScreen` 的 HorizontalPager；要在 tab 间跳，set pagerState 而不是 nav.navigate；要 push 详情用 nav.navigate
- Pager 的状态由 `rememberPagerState` + `rememberSaveable gridCategoryId` 维护；从 Detail / Edit 返回会自然落回原 tab
- 字体 / SVG / 历史事件等数据移植参考 `prototype/project/{vectors,data}.jsx`
- 录入页的具体交互参考 `prototype/add-page-v2/` 但**对话 = 草稿** 这套（confirmed / proposed / 采用 / 不要）是 cycle 0024 新增，原型里没有
- 录入页对话已落 Room — 千万别把 `add_conversations` / `add_messages` 表当 stub 删掉
- AiClient 当前签名是 `extractItemDraft(text, imageJpegBytes?, priorTurns, baseline, categoryHints)`；UserPhoto 的图不进 prior，仅当条 image block。baseline = confirmedDraft，会自动拼到 system prompt 让 AI 做增量修订
- 真 STT 在国行 ROM（华为 / vivo 部分机型）不可用 — 已做 `onUnavailable` fallback，不要去掉。麦克风按钮 cycle 0017 暂去掉了；云端 STT 仍是候选
- 拍照走 FileProvider + `${applicationId}.fileprovider`；如果改 package name 记得同步 manifest
- AI key 存 EncryptedSharedPreferences；切勿改成 PlainSharedPreferences
- **Item.category 是 String id 不是 Category enum**（cycle 0027 起）；要查显示名 / heroVector 等去 CategoryRepository，不要假设它在 6 个内建里
- **Category enum 仍保留**作为 CategoryTemplates 的 map key（hero spec labels / palette / tagline / defaultHeroVector）— 但 enum 不再决定 Item.category 的合法值
- 分类管理抽屉（CategoryManager）和分类编辑（CategoryEditorRoute）拆开：抽屉是 ModalBottomSheet 在 MainScreen 顶层 mount；编辑是 NavHost push 全屏路由。改了一边不要忘了对应 API 调整另一边
- Manager 拖动是 cycle 0030 重写的：divider 当 row-height 块占独立 visualSlot，所有 drag 数学都在 `combinedToVisual` / `computeShift` / `commitDrag` 三个纯函数里。改其中之一前先看 cycle 0030 notes.md
- Xiaomi MiLM preset 的 base URL 还是占位（`https://api.xiaomi.com/v1`），用户提示要填正确才能 test，列为 cycle 0031+ 候选

## 历史

| 日期 | 摘要 |
|---|---|
| 2026-05-13 | cycle 0033：Refine 页接入影集管理（`ItemDraft.photos`/`avatarPhotoPath` + HeroAvatarPicker photo callbacks，picker→CropScreen→`filesDir/draft-photos/<convoId>/`，commit 把 photos/avatar 带进 Item，MODIFIED 行保留原 item id/createdAt/photos）· 新 `CropScreen`：4 角 / 4 边拖动 free-form 矩形 + 灰色蒙层 + 归一化 rect 出参 · Drawer flash 修复（SAVED 点击 / Refine 进入显式 `itemDrawerOpen=false`，`reopenDrawerOnResume` + `Lifecycle.Event.ON_RESUME` 回来重开）· Grid 长按 → 编辑态：`GridViewModel.selecting/selectedIds`、`EditHeader` 替换 [Edit + 红点] = `[完成] · 已选 N · [删除 N] [编辑 N]`、1-列 `EditReorderableList` 长按拖把手调序 · Schema v13 — `items.sort_order`（默认回填 `-created_at`，ItemDao 按 sort_order ASC, acquired DESC，新物品 commit 取 `min - 1`，MODIFY commit 保持原 sortOrder） · 新 `gridIntake` + `AddViewModel.startConversationFromItems` — Grid [编辑] 把选中物品扔录入页起新会话，自动开 drawer |
| 2026-05-13 | cycle 0032：多 action 录入协议（修"4 件物品 AI 只录入 1 件 + 覆盖上一个草稿"两个核心 bug）· 协议从 `extractItemDraft` 改 `extractItemDrafts` 返 `List<DraftAction>`，tool 名 `submit_drafts` + actions[] + kind=create/modify + target_id · system prompt 加 `[CONVERSATION WORKING SET]` 块（id/status/title/category/specs） · v1 直接落工作集 → v2 复修：每个 action 一张 DraftCta，accept 才落工作集（`applyAcceptedCta`）· Schema v12 加 `add_messages.action_kind/target_id` 两列 · DraftCta 卡前缀 "修改 ·" / "新增" · max_tokens 1024 → 4096（thinking 8192，4 件物品 × 8 specs 不再 JSON 截断）· drawer `rememberSaveable` + 长按胶囊删除（SAVED 只从工作集移除不动物品 / PENDING+MODIFIED 丢草稿）· ListIcon + 工作集计数小红点替换 [Draft] 胶囊 |
| 2026-05-12 | cycle 0031 第 N 轮：历史 UI emoji + 长日期（🛒🏆🔧⚙️👋；"2026 年 5 月 12 日"）· Edit 参数 divider 提示行去两侧横线 |
| 2026-05-12 | cycle 0031：Detail 抽屉 3 页 `HorizontalPager` + 影集 + tile + 长按多选 + 底部删除条二次确认 · Grid 标题动态两行（TextMeasurer 同行同步）+ 搜索按钮挪 chip 条最左、点击原地输入框实时过滤 · Edit 参数行卡片化 + "+" 单字按钮 + DANGER ZONE→操作 · 历史 add/edit 改下弹 + Material DatePicker + 顶部 icon picker · `ItemDraft.history` 加历史栏，Draft 复用 `HistorySection` · `SeedCategoriesCallback` 修 fresh-install 分类种子 · 物品种子 8 → 6（每分类 1 条，新增咖啡 + 酒水） |
| 2026-05-11 | cycle 0031：返回栈优先级补齐（AddRoute photoPreview / 历史抽屉 / Preview 模式 + SettingsScreen AI 配置抽屉 + DetailScreen 全屏 viewer 各加局部 BackHandler，不再被 MainScreen 全局兜底推回首页）· 分类管理拖动数学重写（按"预览终态布局"实时摆所有行 + divider；commitDrag 与渲染同一套公式，cycle 0030 残留的跨分割线落点错一格修掉）· CategoryEditor 插画选择改用与物品 Edit 同款 HeroAvatarPicker；自定义新建 photo 不再强制 · `HeroVector.canonical()` 去重 picker · 历史抽屉 current 删除走 resume 上一段 · Settings 加暗黑 / 明亮切换 ☀/☾ · Portal 空态大门插画 + "点开大门" 引导 |
| 2026-05-11 | cycle 0030：分类管理拖动重写（divider 当 row-height 块，能拖到最底部跨进 hidden 段；computeShift / commitDrag 纯函数）· Schema v10 加 `hero_photo_path` 列 · 分类编辑插画改 PickVisualMedia 相册 picker（自定义新建必填，内建可覆盖默认线描）· AvatarHero 优先 AsyncImage · Search 补 visibleIds 过滤 · Grid 当前 chip 分类被隐藏后 effectiveSelectedId 回退 null · Portal GrandTitle 还原 cycle 0023 displayLarge + 英文 tagline + 底部 Ornament 补回（cycle 0026 改错了） |
| 2026-05-11 | cycle 0029：BackHandler 修非 Portal tab 返回退应用问题（改为先回 Portal）· 分类编辑器拆全屏路由 `category/{new\|edit/{id}}`，复用 EditPageHeader + BackArrow 跟物品 Edit 同款 · Manager 抽屉只剩 List + 删底部 italic 提示 · 图鉴右上加搜索 icon · 新 SearchRoute（auto-focus 搜索框 + 实时过滤 brand/model/nickname + 命中段 terra 高亮） |
| 2026-05-11 | cycle 0028：隐藏分类真生效（Grid/Portal 全部 chip + total + latest 全过 visibleItems）· Portal LATEST ENTRY 居中两边加 ✦ + 全空时显示"去分类管理 →"链接 · Portal doorway 永远用 info.heroVector（基础图不跟物品）· Manager 重写：删副标题 / [隐藏 显示] pill / [完成]，编辑入口改小红点，左侧三横纹握把长按拖动 + 跨分割线 toggle hidden 实时生效 · Editor 顶部 112dp Avatar + 插画必填 + 内建锁定 name+插画 · Category enum 加 defaultHeroVector，repo override 内建（修 cycle 0026 种子的 GENERIC bug） |
| 2026-05-11 | cycle 0027：自定义分类能装物品 — `Item.category` 从 `Category` enum 改 String（无 schema migration，列本来就是 TEXT，cycle 0027 只改 domain 强转）· AI prompt 加动态 categoryHints + tool schema 去 enum 改 description · AddPreview / EditScreen InlineDropdown 改读 categoryRepo · HeroAvatarPicker 接 categoryId: String · heroVectorOptionsForId 自定义分类返回 HeroVector.entries 全集 · 删自定义分类时 reassignItemsToTech 兜底（item 不丢） |
| 2026-05-11 | cycle 0026：图标回退 cycle 0013 平面版（3D 几版作废，留在 git 历史）· 分类管理：schema v9 加 category_prefs 表（Migration 种子 6 内建）· 新 CategoryInfo / CategoryRepository · Grid 右上小红点 → ModalBottomSheet 管理器（显示中 / 已隐藏 分割线 + 每行 [隐藏/显示] + [编辑 →]，标题右侧 [+ 新增分类]）· 编辑子页：改名 / 换插画（HeroVector 横滚 picker）/ 显示状态 / 删除（AlertDialog 二次确认，仅自定义）· Portal doorways + Grid chip 按 visibleCategories 渲染（隐藏的不出）· Item.category 仍是 enum，自定义分类暂时空容器（cycle 0027 候选） |
| 2026-05-11 | cycle 0025：戒指图标 v3（外圈压扁到 28×10，删前侧壁第二 path 和 rune，纯垂直顶亮底暗渐变，回到"单一 evenOdd 圆环 + 凸面金属光照"读法）· 草稿页 [确认收入] 加二次确认 AlertDialog（仿 Settings 重置设置） |
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
