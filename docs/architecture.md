# Architecture · Treasure

本文记录**模块划分、数据流、关键依赖**。具体决策的"为什么"在 [`adr/`](adr/) 里 —— 这里不重复，只链过去。

## 模块拆分

```
android/
├── app/        :app   —— Android 入口、Compose 屏幕、导航、主题
└── core/       :core  —— 领域模型 + Room + Repository + UseCase（纯 Kotlin/JVM）
```

**:core 不依赖 Android framework**，只依赖 Room（compile-time annotation processing 是允许的，运行时 KMP 化也可行但不强求）。这样 :core 可以在 JVM 上跑单元测试。

`:app` 依赖 `:core`，反过来不行。

## 关键依赖（Gradle / version catalog）

实际写在 [`../android/gradle/libs.versions.toml`](../android/gradle/libs.versions.toml)：

| 用途 | 依赖 | 当前版本 |
|---|---|---|
| UI | `androidx.compose:compose-bom`、`material3`、`activity-compose` | bom 2024.10.01, activity 1.9.3 |
| 导航 | `androidx.navigation:navigation-compose` | 2.8.4 |
| 持久化 | `androidx.room:room-runtime` + `room-ktx` + `room-compiler`（KSP） | 2.6.1 |
| 序列化 | `kotlinx-serialization-json`（palette / specs / history JSON 列） | 1.7.3 |
| 生命周期 | `androidx.lifecycle:lifecycle-{viewmodel,runtime}-compose` | 2.8.7 |
| Kotlin / AGP | Kotlin 2.0.21 / AGP 8.7.2 / KSP 2.0.21-1.0.27 | — |
| 图片加载 | `io.coil-kt:coil-compose`（cycle 0003 真实照片） | 2.7.0 |
| 网络 / AI | `com.squareup.okhttp3:okhttp`（手写 Anthropic / OpenAI client，cycle 0005 起） | 4.12.0 |
| 安全存储 | `androidx.security:security-crypto-ktx`（API key 存储，[ADR-0004](adr/0004-byo-ai-key.md)） | 1.1.0-alpha06 |
| DI | **不引** Hilt/Koin，手写 ServiceLocator（`TreasureApp`） | — |

参见 [ADR-0002](adr/0002-jetpack-compose.md) 关于 Compose / Material3 用法。

## 数据流

```
            Compose Screen (e.g. PortalScreen.kt)
                       │
                  collect StateFlow
                       │
                  ScreenViewModel
                       │
                       ▼
                 UseCase（可选，简单读取直接走 Repo）
                       │
                       ▼
              ItemRepository (interface)
                       │
                       ▼
            ┌──────────┴──────────┐
            ▼                     ▼
       LocalItemSource         RemoteItemSource
        (Room DAO)              (sync, cycle 0003+)
```

实际链路只到 Room — 同步层 `RemoteItemSource` 至今没接（backend/ 是空脚手架）。

参见 [ADR-0003](adr/0003-local-first-with-optional-sync.md) 关于同步协议；接通是 cycle 0031+ 候选。

## :core 内部结构（实际）

```
core/
├── domain/
│   ├── Item.kt                         # 领域模型；category 是 String id（cycle 0027 起）；
│   │                                   #   specs 单列表，前 4 项为 hero（计算属性 heroSpecs / tailSpecs）；
│   │                                   #   avatarPhotoPath（cycle 0016）、callouts（cycle 0010）
│   ├── Category.kt                     # 6 个 enum：BADMINTON / PHOTO / CARS / TECH / COFFEE / WINE，
│   │                                   #   每个带 defaultHeroVector（cycle 0028）。**只作 CategoryTemplates
│   │                                   #   的 map key**，不再决定 Item.category 的合法值
│   ├── CategoryInfo.kt                 # cycle 0026 起的统一分类模型（内建 + 自定义）
│   ├── ItemStatus.kt                   # OWNED / PARTED / RENTED
│   ├── HeroVector.kt                   # 19 个预置插画 enum
│   ├── HeroSpec.kt                     # @Serializable
│   ├── HistoryEvent.kt                 # @Serializable + HistoryKind enum
│   └── PhotoCallout.kt                 # cycle 0010：照片文字标注
├── ai/
│   ├── AiClient.kt                     # interface：extractItemDraft(text, imageJpegBytes?,
│   │                                   #   priorTurns, baseline, categoryHints) → Result<ItemDraft>；
│   │                                   #   + AiTurn / AiRole / Provider / CategoryHint / ItemDraft
│   ├── AnthropicClient.kt              # POST /v1/messages；强制 tool_use=fill_item_draft（thinking 模式 auto）
│   ├── OpenAiClient.kt                 # POST .../chat/completions；覆盖 OpenAI + OpenAI 兼容端点
│   └── Prompts.kt                      # SYSTEM_PROMPT + buildSystemWithBaseline(baseline, json,
│                                       #   categoryHints) + EXTRACT_TOOL_PARAMETERS
├── repo/
│   ├── ItemRepository.kt               # interface + RoomItemRepository 实现
│   ├── AddConversationRepository.kt    # cycle 0010 起；含 AddConversationMessage 5 种 + DraftCtaStatus
│   └── CategoryRepository.kt           # cycle 0026 起；含 setHidden / reorder / addCustom /
│                                       #   setHeroPhotoPath / deleteCustom（自动 reassign items）
├── room/
│   ├── TreasureDatabase.kt             # @Database version=10；4 个 entity；addMigrations(*Migrations.ALL)
│   ├── Migrations.kt                   # 5_6 / 6_7 / 7_8 / 8_9 / 9_10
│   ├── ItemEntity.kt                   # internal；含 toDomain/fromDomain；JsonCodec object
│   ├── ItemDao.kt                      # observeAll/observeById/count/upsert/deleteById
│   ├── ConversationEntity.kt           # cycle 0010：add_conversations + add_messages
│   ├── ConversationDao.kt
│   ├── CategoryPrefEntity.kt           # cycle 0026 起；含 hero_photo_path（cycle 0030 加）
│   └── CategoryPrefDao.kt
├── seed/
│   └── SeedItems.kt                    # 8 条 + 真实 history（category 都是 String id 了）
└── web/
    └── PageFetcher.kt                  # cycle 0020-0022：OkHttp + mobile UA + HTML strip +
                                        # meta charset 探测 + 防爬启发式（FetchResult 三态）
```

Schema 历史（[ADR-0006](adr/0006-schema-migrations.md)）：

| 版本 | cycle | 变化 |
|---|---|---|
| v5 | 0010 baseline | 起点（cycle 0001-0009 全 destructive） |
| v6 | 0010 | 加 `add_conversations` + `add_messages` |
| v7 | 0010 | items 加 `callouts_json` |
| v8 | 0016 | items 加 `avatar_photo_path` |
| v9 | 0026 | 加 `category_prefs` 表 + 种子 6 内建 |
| v10 | 0030 | category_prefs 加 `hero_photo_path` |

简化 vs. 早期设计稿：

- **没拆 source 子层** —— 直接 `RoomItemRepository` 持有 `TreasureDatabase`。同步层（[ADR-0003](adr/0003-local-first-with-optional-sync.md)）还没接，backend/ 是空脚手架
- **没拆 usecase 层** —— ViewModel 直接用 Repository
- **没拆 history_events 表** —— history 当作 JSON 列嵌在 items 里

## :app 内部结构（实际）

```
app/
├── MainActivity.kt                     # ComponentActivity + enableEdgeToEdge + Share intent consume → TreasureNavHost
├── TreasureApp.kt                      # Application：装 ItemRepository / AddConversationRepository /
│                                       #   CategoryRepository / SettingsStore / PageFetcher / shareIntake；
│                                       #   首启 seed
├── data/
│   ├── SettingsStore.kt                # EncryptedSharedPreferences：provider / model / baseUrl / apiKey /
│   │                                   #   temperature / thinkingEnabled / lastTestPassed / presetId
│   └── AiProviderPreset.kt             # 8 个 preset + modelSupportsVision 启发式（cycle 0022）
├── voice/
│   └── VoiceCapture.kt                 # SpeechRecognizer + RECORD_AUDIO 权限 + 不可用回退（cycle 0017
│                                       #   起 UI 没入口；保留实现供云端 STT 兜底用）
├── theme/                              # TreasureTheme / Color / Type / Cormorant
├── ui/
│   ├── nav/
│   │   ├── Routes.kt                   # Main / Detail / Edit / Search / CategoryNew / CategoryEditPattern
│   │   └── TreasureNavHost.kt          # NavHost + 6 routes + 300ms 滑动转场
│   ├── main/
│   │   └── MainScreen.kt               # HorizontalPager 4 tab + ControlIsland + BackHandler +
│   │                                   #   CategoryManager 抽屉顶层 mount
│   ├── portal/                         # PortalRoute + PortalScreen + PortalViewModel
│   ├── grid/                           # GridRoute + GridScreen + GridViewModel + 右上 [🔍][小红点]
│   ├── detail/                         # DetailScreen + DetailViewModel；BottomSheetScaffold + 翻面 + 3-tab
│   ├── edit/                           # EditScreen + 用 DetailViewModel 复用
│   ├── add/
│   │   ├── AddRoute.kt                 # 编排：Chat / Preview（Refine）模式 + photo preview overlay
│   │   ├── AddChat.kt                  # RECORD header + 浮动 composer + 历史 ModalBottomSheet
│   │   ├── AddPreview.kt               # 草稿页镜像 Edit：基础 / 标签 / 参数 + 确认收入二次确认
│   │   ├── AddViewModel.kt             # 状态机：confirmedDraft + proposedDraft + pendingCtaId；
│   │   │                               #   newConversation / openConversation / sendText/Photo /
│   │   │                               #   acceptProposal / rejectProposal / commitDraft
│   │   ├── CategoryTemplate.kt         # 6 内建模板 + heroVectorOptionsForId
│   │   └── CategoryForm.kt             # 旧手动表单（cycle 0024 退役但文件未删）
│   ├── settings/
│   │   ├── SettingsScreen.kt           # 摘要卡 + 编辑抽屉 + 多模态 pill + 重置确认
│   │   └── SettingsViewModel.kt
│   ├── search/
│   │   └── SearchRoute.kt              # cycle 0029：BackArrow + auto-focus 搜索框 + 实时过滤
│   │                                   #   brand/model/nickname + 命中段 terra 高亮 + 2 列结果
│   ├── category/
│   │   ├── CategoryManager.kt          # cycle 0026/0028/0030：ModalBottomSheet 拖动 list +
│   │   │                               #   divider 当 row-height 块 + 跨段 toggle hidden + 实时
│   │   │                               #   commit applyReorder。computeShift / commitDrag 纯函数
│   │   ├── CategoryEditorRoute.kt      # cycle 0029 拆出的全屏编辑：EditPageHeader + BackArrow +
│   │   │                               #   PickVisualMedia 相册 picker (cycle 0030)
│   │   └── CategoryManagerViewModel.kt # 代理 CategoryRepository + pendingPhotoForNew 暂存 +
│   │                                   #   addCustomWithPhoto / pickHeroPhoto / applyReorder
│   ├── photo/
│   │   └── FullscreenPhotoViewer.kt    # cycle 0010/0019/0020：横滑翻页 + 双指缩放 + callout
│   └── components/
│       ├── ControlIsland.kt            # 底部浮动控制岛（4 颗胶囊）
│       ├── BackArrow.kt                # 手绘加粗 ← 箭头
│       ├── EditPageHeader.kt           # 共用 EditScreen / AddPreview / CategoryEditor
│       ├── HeroAvatar.kt               # photo 优先 → HeroIllustration 兜底
│       ├── HeroAvatarPicker.kt         # 物品编辑头像选择器
│       ├── InlineDropdown.kt           # 仿原型的中性 dropdown
│       ├── Ornament.kt                 # Portal 顶部 / 底部罗盘装饰
│       └── SectionDivider.kt
└── illust/                             # 16 个 Compose Canvas 博物馆插画 + HeroIllustration dispatcher
```

`HeroIllustration.kt` 按 `item.heroVector` 分发到 `illust/` 下具体函数；多个 enum 值可共享一个
Composable（CAMERA_DSLR / CAMERA_RANGEFINDER → Camera()，CAR_SEDAN / CAR_SUV → Car()，
KINDLE → Tablet()）。

## 导航图（实际）

```
NavHost (start = "main")
├── "main"                      ─→  MainScreen
│                                   ├── PortalRoute (page 0)
│                                   ├── GridRoute (page 1)
│                                   ├── AddRoute (page 2)
│                                   └── SettingsRoute (page 3)
│                                   + CategoryManager (ModalBottomSheet 顶层 mount)
├── "detail/{itemId}"           ─→  DetailRoute             # cycle 0001
├── "edit/{itemId}"             ─→  EditRoute               # cycle 0005
├── "search"                    ─→  SearchRoute             # cycle 0029
├── "category/new"              ─→  CategoryEditorRoute(null)  # cycle 0029
└── "category/edit/{categoryId}" ─→ CategoryEditorRoute(id)  # cycle 0029
```

转场（NavHost 全局）：300ms tween 的 `slideIntoContainer(Start/End)`。

控制岛 4 颗胶囊：门厅 / 图鉴 / 录入 / 设置。Detail / Edit / Search / CategoryEditor 屏控制岛**隐藏**（这些是 NavHost push 路由，不在 pager 里）。

**BackHandler**（cycle 0029）：在 MainScreen 顶层。Manager 抽屉打开优先收抽屉；非 Portal tab 返回 Portal；Portal 默认行为 → 退出。

## AI / 录入数据流（cycle 0024 重构后）

```
AddRoute（chat-first）
   │
   ├─ 文本 / 照片 / URL 分享
   │       │
   │       ▼
   │   AddViewModel.sendText / sendPhoto / runExtract
   │       │                                  │
   │       │                                  ▼
   │       │                          baseline = confirmedDraft（cycle 0024）
   │       │                          categoryHints = visibleCategories（cycle 0027）
   │       │                                  │
   │       │                                  ▼
   │       └→ AiClient.extractItemDraft(text, image?, priorTurns, baseline, categoryHints)
   │                  │
   │                  └→ AnthropicClient / OpenAiClient（按 SettingsStore）
   │                       POST + tool_use(fill_item_draft) → ItemDraft
   │       │
   │       ▼
   │   DraftCta(status=Pending) 落到 messages（同时 supersede 旧 Pending）
   │
   ├─ 用户点 [采用] → acceptProposal()
   │       │
   │       ▼
   │   confirmedDraft = proposedDraft；append DraftConfirmed（落 Room）；
   │   旧 DraftCta status = Accepted
   │
   ├─ 用户点 [不要] → rejectProposal()
   │       │
   │       ▼
   │   proposedDraft = null；旧 DraftCta status = Rejected
   │
   └─ 用户点 [手动] → AddRoute push 进 Refine（AddPreview）
           │
           ▼
       AddPreview 编辑 confirmedDraft 各字段 + DraftSpecs
           │
           ▼
       [确认收入] → AlertDialog 二次确认（cycle 0025）→ commitDraft(status)
           │
           ▼
       ItemRepository.upsert(Item) → vm.newConversation() → 跳新对话
```

AI key 通过 `SettingsStore`（EncryptedSharedPreferences）注入到 `AiClient` 的工厂方法（`TreasureApp.aiClient()` 持有）。设备直连 provider，不走代理（[ADR-0004](adr/0004-byo-ai-key.md)）。

## 状态与事件

- 每个屏一个 `ScreenViewModel`，暴露 `StateFlow<UiState>`
- UI 事件用单向 `(action: Action) -> Unit` 传给 VM，不要双向 binding
- 数据库变更通过 Room 的 `Flow<List<…>>` 回到 VM，**不需要手动刷新** —— 写入立即触发 UI 更新

## 测试策略（cycle 0001 起步）

- `:core` 跑 JVM 单元测试，覆盖 Repository / UseCase / Converters
- Room 测试用 in-memory 数据库（`Room.inMemoryDatabaseBuilder`）
- `:app` 用 Compose UI 测试覆盖 Portal / Grid / Detail 的关键 happy path
- **暂不**做端到端、截图、benchmark

## 与 [ADR-0003](adr/0003-local-first-with-optional-sync.md) 的对接点（未来）

cycle 0003+ 接通同步时，触点限定在：

1. `core/repo/source/RemoteItemSource.kt` 的实现切换
2. `core/sync/SyncWorker.kt`（新建，用 WorkManager 周期触发）
3. `app/ui/settings/SettingsScreen.kt` 加同步开关 + 服务器地址

不会侵入 ViewModel / Screen 层 —— Repository 的 `Flow<…>` 接口对调用方屏蔽来源。
