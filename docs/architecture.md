# Architecture · Treasure

本文记录**模块划分、数据流、关键依赖、Schema 演化**。具体决策的"为什么"在 [`adr/`](adr/) 里 —— 这里只链过去。

最后大改：2026-05-15（cycle 0035 后整体复刷）。

## 模块拆分

```
android/
├── app/        :app   —— Android 入口、Compose 屏幕、导航、主题、插画、音频、保活 service
└── core/       :core   —— 领域模型 + Room + Repository + AI 客户端 + Web fetcher（纯 Kotlin/JVM）
```

**`:core` 不依赖 Android framework**（除了 Room runtime —— compile-time annotation processing 允许；运行时 KMP 化是后续选项）。这样 `:core` 可以在 JVM 上跑单元测试。

`:app` 依赖 `:core`，反过来不行。

## 关键依赖（Gradle / version catalog）

实际写在 [`../android/gradle/libs.versions.toml`](../android/gradle/libs.versions.toml)：

| 用途 | 依赖 | 版本（cycle 0035） |
|---|---|---|
| 平台 | Kotlin / AGP / KSP | 2.0.21 / 8.7.2 / 2.0.21-1.0.27 |
| UI | `androidx.compose:compose-bom`、`material3`、`activity-compose`、`navigation-compose` | BOM 2024.10.01 / activity 1.9.3 / nav 2.8.4 |
| 持久化 | `androidx.room:room-{runtime,ktx,compiler}`（KSP） | 2.6.1 |
| 序列化 | `kotlinx-serialization-json`（多个 JSON 列 + AI profiles） | 1.7.3 |
| 生命周期 | `androidx.lifecycle:lifecycle-{viewmodel,runtime}-compose` | 2.8.7 |
| 图片 | `io.coil-kt:coil-compose` | 2.7.0 |
| 网络 / AI | `com.squareup.okhttp3:okhttp`（手写 Anthropic / OpenAI client） | 4.12.0 |
| 加密存储 | `androidx.security:security-crypto-ktx`（API key + AI profiles JSON） | 1.1.0-alpha06 |
| DI | **不引** Hilt/Koin，手写 ServiceLocator（`TreasureApp`） | — |

参见 [ADR-0002](adr/0002-jetpack-compose.md) 关于 Compose / Material3 用法。

## 数据流

```
            Compose Screen (e.g. PortalScreen.kt)
                       │
                  collect StateFlow（lifecycle-aware）
                       │
                  ScreenViewModel
                       │
                       ▼
              Repository（ItemRepository / AddConversationRepository /
                          CategoryRepository / SettingsStore）
                       │
                       ▼
                  Room DAO  +  filesystem  +  EncryptedSharedPreferences
```

实际链路只到本地：同步层 `RemoteItemSource` 至今没接（`backend/` 是空脚手架）。
参见 [ADR-0003](adr/0003-local-first-with-optional-sync.md) 关于可选同步协议。

## `:core` 内部结构

```
core/src/main/java/com/treasure/core/
├── domain/                         # @Immutable 域模型，纯 Kotlin
│   ├── Item.kt                     #  - category: String id（cycle 0027 起，
│   │                               #    自定义分类也能装；列本身一直是 TEXT，
│   │                               #    没 migration）
│   │                               #  - specs 单列表，前 4 项为 hero
│   │                               #  - avatarPhotoPath（cycle 0016）
│   │                               #  - callouts: Map<path, List<{x,y,text}>>（cycle 0010）
│   │                               #  - photoCrops: Map<path, PhotoCrop>（cycle 0034，非破坏裁剪）
│   │                               #  - sortOrder（cycle 0033，图鉴里显式排序）
│   ├── Category.kt                 # 6 个 enum 留作 CategoryTemplates 的 map key
│   │                               # （hero spec labels / palette / tagline / defaultHeroVector）—
│   │                               # 不再决定 Item.category 的合法值
│   ├── CategoryInfo.kt             # cycle 0026 起的统一分类模型（内建 + 自定义同一张表）
│   ├── HeroVector.kt               # 19 个预置插画 enum
│   ├── HeroSpec.kt                 # @Serializable
│   ├── HistoryEvent.kt             # @Serializable + HistoryKind enum
│   ├── PhotoCallout.kt             # 照片文字标注
│   ├── PhotoCrop.kt                # cycle 0034：归一化裁剪矩形（x, y, w, h 都 0..1）
│   └── ItemStatus.kt               # OWNED / PARTED / RENTED
│
├── ai/
│   ├── AiClient.kt                 # interface：
│   │                               #   extractItemDrafts(text, images, audioM4a?,
│   │                               #     priorTurns, workingSet, categoryHints) → Result<List<DraftAction>>
│   │                               #   + AiTurn / AiRole / Provider / CategoryHint
│   │                               #   + ItemDraft（含 photos / photoCrops / avatarPhotoPath / heroVector）
│   │                               #   + DraftAction（kind=create/modify, targetId, photo_assignments）
│   ├── AnthropicClient.kt          # POST /v1/messages；强制 tool_use=submit_drafts
│   │                               # （audio block + image block；thinking 模式 auto；
│   │                               #  JsonNull-safe 解析）
│   ├── OpenAiClient.kt             # POST .../chat/completions；覆盖 OpenAI + 全部 OpenAI-兼容端点
│   │                               # （input_audio + image_url；同样 JsonNull-safe）
│   └── Prompts.kt                  # SYSTEM_PROMPT + buildSystemWithBaseline(...) +
│                                   #   [CONVERSATION WORKING SET] 块 + MODIFY = delta-only 协议 +
│                                   #   submit_drafts tool schema（actions[]）
│
├── repo/
│   ├── ItemRepository.kt           # interface + RoomItemRepository 实现；
│   │                               # observeAll / observeById / upsert / deleteById / count /
│   │                               # ensureSeeded
│   ├── AddConversationRepository.kt# 录入页对话主表 + 5 种 AddConversationMessage（含 DraftCta、
│   │                               # DraftConfirmed、SystemNote 等）+ DraftCtaStatus +
│   │                               # ConversationItem 表（cycle 0031：工作集）
│   └── CategoryRepository.kt       # CategoryInfo CRUD：setHidden / reorder /
│                                   # addCustomWithPhoto / setHeroPhotoPath / deleteCustom
│                                   # （删自定义时把 items rehome 到 TECH 兜底）
│
├── room/
│   ├── TreasureDatabase.kt         # @Database version=16；6 个 entity；addMigrations(*Migrations.ALL)
│   ├── Migrations.kt               # 5_6 → 6_7 → 7_8 → 8_9 → 9_10 → 10_11 → 11_12 → 12_13 → 13_14 → 14_15 → 15_16
│   ├── ItemEntity.kt               # internal；toDomain/fromDomain；JsonCodec object
│   ├── ItemDao.kt                  # observeAll/observeById/count/upsert/deleteById（按 sort_order 排）
│   ├── ConversationEntity.kt       # add_conversations + add_messages
│   ├── ConversationDao.kt          # @Transaction reorder / message upsert / setStatus
│   ├── ConversationItemEntity.kt   # cycle 0031：工作集行
│   ├── ConversationItemDao.kt
│   ├── CategoryPrefEntity.kt       # category_prefs（含 hero_photo_path）
│   └── CategoryPrefDao.kt          # @Transaction reorder(orderedIds, hiddenIds) 单事务
│
├── seed/
│   └── SeedItems.kt                # 6 条种子物品（每内建分类 1 条）
│
└── web/
    └── PageFetcher.kt              # OkHttp + mobile UA + HTML strip + meta charset 探测 +
                                    # 防爬启发式（FetchResult Success / Blocked / Failed）
```

**简化 vs. 早期设计稿**：
- 没拆 source 子层 —— `RoomItemRepository` 直接持有 `TreasureDatabase`；同步层未接
- 没拆 usecase 层 —— ViewModel 直接用 Repository
- 没拆 history_events 表 —— history 当作 JSON 列嵌在 items 里

## Schema 演化（[ADR-0006](adr/0006-schema-migrations.md)）

从 cycle 0010 起 `exportSchema = true`，schema JSON 在 `core/schemas/com.treasure.core.room.TreasureDatabase/`，**禁止 destructive**。`fallbackToDestructiveMigrationOnDowngrade()` 还开着，**只**在用户从更高版本降回老 APK 时清库（dev OK，线上几乎不发生）。

| 版本 | cycle | 变化 |
|---|---|---|
| v5 | 0010 baseline | 起点（v1-v4 全 destructive，cycle 0001-0009 阶段） |
| v6 | 0010 | 加 `add_conversations` + `add_messages` |
| v7 | 0010 | items 加 `callouts_json` |
| v8 | 0016 | items 加 `avatar_photo_path` |
| v9 | 0026 | 加 `category_prefs` 表 + 6 内建种子 |
| v10 | 0030 | category_prefs 加 `hero_photo_path` |
| v11 | 0031 | 加 `conversation_items` 表（工作集） |
| v12 | 0032 | add_messages 加 `action_kind` + `target_id`（多 action 协议） |
| v13 | 0033 | items 加 `sort_order`（图鉴显式排序） |
| v14 | 0034 | add_messages 加 `voice_path`（语音消息） |
| v15 | 0034 | add_messages 加 `photo_assignments_json` |
| v16 | 0034 | items 加 `photo_crops_json`（非破坏裁剪） |

## `:app` 内部结构

```
app/src/main/java/com/treasure/
├── MainActivity.kt                 # ComponentActivity + enableEdgeToEdge + Share intent
│                                   #   (ACTION_SEND/VIEW) consume → TreasureNavHost
├── TreasureApp.kt                  # Application：ServiceLocator —— 装 ItemRepository /
│                                   #   AddConversationRepository / CategoryRepository /
│                                   #   SettingsStore / PageFetcher / shareIntake / gridIntake /
│                                   #   darkModeOverride；首启 seed；aiClient() 工厂
│                                   #   按 effectiveProfile 构造
├── data/
│   ├── AiProfile.kt                # cycle 0035：单个 AI 配置实体（@Serializable）；
│   │                               #   含 displayName 用户可改
│   ├── AiProviderPreset.kt         # 8 个 preset + modelSupportsVision 启发式
│   └── SettingsStore.kt            # EncryptedSharedPreferences；多 profile 存 JSON 列表 +
│                                   #   defaultProfileId + 内存级 conversationOverrideProfileId
├── audio/
│   ├── VoiceRecorder.kt            # MediaRecorder 封装（AAC m4a）+ amplitude / elapsed
│   └── VoicePlayer.kt              # MediaPlayer 封装
├── background/
│   └── AiKeepAliveService.kt       # cycle 0031：AI 调用期间前台保活；API 34+
│                                   #   FOREGROUND_SERVICE_TYPE_DATA_SYNC + WakeLock + 通知
├── theme/                          # TreasureTheme / Color / Type / Cormorant
├── illust/                         # 16 个 Compose Canvas 博物馆插画 + HeroIllustration dispatcher
└── ui/
    ├── nav/
    │   ├── Routes.kt               # Main / Detail / Edit / Search / CategoryNew / CategoryEdit
    │   └── TreasureNavHost.kt      # NavHost + 6 routes + 300ms slideIntoContainer 转场
    ├── main/
    │   └── MainScreen.kt           # HorizontalPager 4 tab + ControlIsland +
    │                               #   BackHandler + CategoryManager 抽屉顶层 mount
    ├── portal/                     # PortalRoute + PortalScreen + PortalViewModel
    ├── grid/
    │   └── GridScreen.kt           # 2-列网格 + 标题动态两行（TextMeasurer 同行同步）+
    │                               #   右上 [🔍][🔧]；长按进编辑态 → 父层 pointerInput +
    │                               #   detectDragGesturesAfterLongPress（cycle 0035）；
    │                               #   GridDragState：bounds + liveOrder + translationFor +
    │                               #   hitTest + applyScrollDelta + insert-shift +
    │                               #   userScrollEnabled=false 期间禁用列表自身滚动；
    │                               #   边缘 96dp 内 auto-scroll
    ├── detail/                     # DetailScreen + DetailViewModel；BottomSheetScaffold +
    │                               #   翻面 + 3-tab（参数 / 历史 / 影集，cycle 0034 v9）+
    │                               #   抽屉 BackHandler 收回 partial（cycle 0035）
    ├── edit/                       # EditScreen + 用 DetailViewModel 复用
    ├── add/
    │   ├── AddRoute.kt             # 编排：Chat / Refine 双模 + photo preview + crop +
    │   │                           #   voice recorder + pickFileLauncher + aiProfiles 状态
    │   ├── AddChat.kt              # Cycle 0035 v3 架构：Header → LazyColumn(weight 1) →
    │   │                           #   Composer 串在 Column 里；imePadding 后 padding
    │   │                           #   bottom = max(IME, 72dp)，键盘弹起整列上抬；
    │   │                           #   LaunchedEffect(imeBottomDp) 滚到末尾。
    │   │                           #   Composer 内：附件 / 模型 chip + pill 输入框
    │   │                           #   (mic-inside-left / 文本 / emoji-inside-right) +
    │   │                           #   send 外侧；4 种 ChatDrawer（Attach / Model / Emoji /
    │   │                           #   ItemPicker），抽屉弹起同步收 IME + BackHandler 退；
    │   │                           #   ItemPicker 有搜索框
    │   ├── AddPreview.kt           # Refine 页（confirmedDraft + proposalDraft 共用）
    │   ├── AddViewModel.kt         # 状态机：confirmedDraft + proposalDraft + workingItems +
    │   │                           #   retryAvailable / lastFailedExtract +
    │   │                           #   acceptProposal / acceptAndCommitProposal /
    │   │                           #   commitAllPendingWorkingItems / sendVoiceAudio /
    │   │                           #   addExistingItem / removeWorkingItem /
    │   │                           #   addDraftPhoto / setDraftPhotoCrop /
    │   │                           #   mergeDraftOntoItem + mergeDraftOntoDraft
    │   ├── RecordingOverlay.kt     # 旧全屏录音 UI（cycle 0035 起停用；press-hold 半屏
    │   │                           #   毛玻璃直接在 AddChat 渲染）
    │   ├── CategoryTemplate.kt     # 6 内建模板
    │   └── CategoryForm.kt         # cycle 0024 退役，文件留作历史
    ├── settings/
    │   ├── SettingsScreen.kt       # cycle 0035：HorizontalPager 多份 AI 配置卡 +
    │   │                           #   末尾幽灵卡 → AddProfileSheet；每张卡
    │   │                           #   [设为默认/★默认] + [调整→]；编辑抽屉含
    │   │                           #   名称 / Provider / Base URL / Model / Key /
    │   │                           #   Temperature / Thinking / 保存 / 测试连接 /
    │   │                           #   移除（仅 size>1）；DANGER ZONE 重置全部
    │   └── SettingsViewModel.kt
    ├── search/
    │   └── SearchRoute.kt          # BackArrow + auto-focus + 实时过滤
    │                               #   brand/model/nickname + 命中段 terra 高亮 + 2 列结果
    ├── category/
    │   ├── CategoryManager.kt      # ModalBottomSheet 拖动 list；computeShift / commitDrag
    │   │                           #   纯函数；divider 当 row-height 块；跨段 toggle hidden
    │   ├── CategoryEditorRoute.kt  # 全屏 EditPageHeader + PickVisualMedia hero 图
    │   └── CategoryManagerViewModel.kt
    ├── photo/
    │   ├── FullscreenPhotoViewer.kt# 横滑翻页 + 双指缩放 + callout
    │   ├── CropScreen.kt           # cycle 0033/0034：基础裁剪（PointerEventPass.Initial
    │   │                           #   抢在 Pager 之前消费，桌面 / Pager 父级不会抢手势）
    │   └── CroppedPhoto.kt         # cycle 0034：等比缩放（Crop / Fit 双语义） +
    │                               #   graphicsLayer 应用 photoCrops rect
    └── components/
        ├── ControlIsland.kt        # 底部浮动控制岛（4 颗胶囊）
        ├── BackArrow.kt
        ├── EditPageHeader.kt
        ├── HeroAvatar.kt           # photo + crop 优先 → HeroIllustration 兜底
        ├── HeroAvatarPicker.kt
        ├── InlineDropdown.kt
        ├── Ornament.kt
        └── SectionDivider.kt
```

`HeroIllustration.kt` 按 `item.heroVector` 分发到 `illust/` 下具体 Composable；多个 enum 值可共享一个函数（CAMERA_DSLR / CAMERA_RANGEFINDER → `Camera()`，CAR_SEDAN / CAR_SUV → `Car()`，KINDLE → `Tablet()`）。

## 导航图

```
NavHost (start = "main")
├── "main"                          ─→  MainScreen
│                                       ├── PortalRoute (page 0)
│                                       ├── GridRoute (page 1)
│                                       ├── AddRoute (page 2)
│                                       └── SettingsRoute (page 3)
│                                       + CategoryManager (ModalBottomSheet 顶层 mount)
├── "detail/{itemId}"               ─→  DetailRoute               # cycle 0001
├── "edit/{itemId}"                 ─→  EditRoute                 # cycle 0005
├── "search"                        ─→  SearchRoute               # cycle 0029
├── "category/new"                  ─→  CategoryEditorRoute(null) # cycle 0029
└── "category/edit/{categoryId}"    ─→  CategoryEditorRoute(id)
```

转场（NavHost 全局）：300ms tween 的 `slideIntoContainer(Start/End)`。

控制岛 4 颗胶囊：门厅 / 图鉴 / 录入 / 设置。Detail / Edit / Search / CategoryEditor / Crop 屏控制岛**隐藏**（这些是 NavHost push 路由，不在 pager 里）。

**BackHandler 栈**（cycle 0029 + 0035）：
- AddRoute：photoPreview → historyOpen → itemDrawerOpen → Preview 模式 → 兜底 MainScreen
- DetailScreen：抽屉展开时 back 走 `partialExpand()`
- AddChat (Composer)：ChatDrawer 打开时 back 关抽屉
- SettingsScreen：editorTarget / addSheetOpen 打开时 back 关
- MainScreen 顶层：Manager 抽屉打开优先收；非 Portal tab → 回 Portal；Portal 默认 → 退出

## AI / 录入数据流（cycle 0032 多 action + cycle 0034 多模态 + cycle 0035 多 profile）

```
AddRoute (chat-first)
   │
   ├─ 输入：文本 / 多张图片 / 语音录音（press-and-hold）/ URL 分享 / 已有物品（picker）
   │       │
   │       ▼
   │   AddViewModel.sendText / sendPhotos / sendVoiceAudio / addExistingItem
   │       │
   │       ▼
   │   runExtract(systemHints, priorTurns, workingSet)
   │       │
   │       ▼
   │   TreasureApp.aiClient()  ─→  SettingsStore.effectiveProfile()
   │       │                       （= conversationOverrideProfileId ?: defaultProfileId）
   │       │
   │       ▼
   │   AnthropicClient / OpenAiClient
   │       POST + tool_use(submit_drafts)
   │       → List<DraftAction>（每 action 含 kind=create/modify, draft, photo_assignments）
   │       │
   │       ▼
   │   每条 action 落一张 DraftCta(Pending) 到 messages
   │   MODIFY 类的 action 在 runExtract.onSuccess 时已经 mergeDraftOntoItem/Draft 算好
   │   "修改后完整状态" 灌进 cta.draft —— 卡片显示的是合并后的全貌
   │
   ├─ 用户对 DraftCta：
   │   [保存草稿] → acceptProposal()
   │            → 落 ConversationItem 工作集（PENDING 或 MODIFIED 状态）
   │            → 旧 Pending DraftCta status = Accepted
   │   [直接录入] → acceptAndCommitProposal()
   │            → 同上 + 立刻 commitDraft 落 Item，跳 Detail
   │   [不要]    → 旧 Pending DraftCta status = Rejected
   │
   ├─ 用户点 DraftCta 卡片 → proposal-preview（AddPreview proposalMode）
   │   │
   │   ▼
   │   编辑 proposalDraft（独立于 confirmedDraft）；可 + 添加照片走 CropScreen；
   │   保存 / 直接录入 / 返回
   │
   └─ Drawer 列工作集：[完成 N 件] → commitAllPendingWorkingItems
       → 逐个 commit → 完成后 newConversation
```

每条 commit 时 `migratePhotosToItemOwned` 把 `conversation-photos/` 或 `draft-photos/` 下的图拷到 `photos/<itemId>/`，让"删会话不丢影集"。

**MODIFY 协议**（cycle 0034 v7+）：prompt 明令 MODIFY action **只返回变更字段**，omit = keep baseline。代码侧 `mergeDraftOntoItem`/`mergeDraftOntoDraft` 把 delta 叠到原物品 / 原草稿；photo_assignments append 不覆盖。**全字段重述会导致影集被空覆盖**，已经在 prompt 里加了 warning。

**Photo assignments**：AI 多图录入时按 `{source_index, crop?, set_as_avatar}` 把 user 发的多张图分配给具体 action 的影集；commit 时按 assignments 落到对应 item。

## 多 AI 服务（cycle 0035）

```
SettingsStore（EncryptedSharedPreferences）
├── profiles: List<AiProfile>            # JSON 列；首次访问做 legacy 单 profile 迁移
├── defaultProfileId: String?            # 设置页"设为默认"或第一个 profile
└── conversationOverrideProfileId: String? # 内存 only；录入页模型 chip 临时切

AiProfile（@Serializable）
- id / displayName / presetId / baseUrl / model / apiKey /
  temperature / thinkingEnabled / lastTestPassed

TreasureApp.aiClient() = effectiveProfile() → AnthropicClient | OpenAiClient
```

录入页右下角的"模型" chip 切换 `conversationOverrideProfileId`；下一次 `aiClient()` 调用就用新 profile。设置页改 `defaultProfileId`，不影响正在跑的会话。

## 状态与事件

- 每个屏一个 `ScreenViewModel`，暴露 `StateFlow<UiState>`
- UI 事件用单向回调 `(action: Action) -> Unit` 传给 VM，不做双向 binding
- 数据库变更通过 Room 的 `Flow<List<…>>` 回到 VM，**不需要手动刷新** —— 写入立即触发 UI 更新

## 测试策略

- `:core` 跑 JVM 单元测试（目前主要覆盖 schema migration test —— `MigrationTest`）
- Room 测试用 in-memory 数据库（`Room.inMemoryDatabaseBuilder`）
- `:app` 用 Compose UI 测试覆盖 Portal / Grid / Detail 的关键 happy path（数量少）
- **暂不**做端到端、截图、benchmark
- 没接 CI；本地跑 `./gradlew :app:assembleDebug` + 端到端 smoke test（见 [`dev-loop.md`](dev-loop.md)）

## 与未来同步层的对接点

cycle 0035 后接通 `backend/` 时，触点限定在：

1. `core/repo/source/RemoteItemSource.kt`（新建）的实现
2. `core/sync/SyncWorker.kt`（新建，WorkManager 周期触发）
3. `app/ui/settings/` 里加同步开关 + 服务器地址

不会侵入 ViewModel / Screen 层 —— Repository 的 `Flow<…>` 接口对调用方屏蔽来源。

参见 [ADR-0003](adr/0003-local-first-with-optional-sync.md)。
