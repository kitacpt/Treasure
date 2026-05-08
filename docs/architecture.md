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

cycle 0001 的实际链路只到 LocalItemSource。`RemoteItemSource` 在 :core 里**只有 interface 定义和一个 NoOp 实现**，等接通时再写真实现。

参见 [ADR-0003](adr/0003-local-first-with-optional-sync.md) 关于同步协议。

## :core 内部结构（实际）

```
core/
├── domain/
│   ├── Item.kt                         # 领域模型；specs 单列表，前 4 项为 hero（计算属性 heroSpecs / tailSpecs）
│   ├── Category.kt                     # 4 个 enum：BADMINTON / PHOTO / CARS / TECH
│   ├── ItemStatus.kt                   # OWNED / PARTED / RENTED
│   ├── HeroVector.kt                   # 预置插画 enum
│   ├── HeroSpec.kt                     # @Serializable
│   └── HistoryEvent.kt                 # @Serializable + HistoryKind enum
├── ai/
│   ├── AiClient.kt                     # interface：extractItemDraft(transcript, photo?, category?) → ItemDraft
│   ├── AnthropicClient.kt              # POST /v1/messages，强制 tool_use=fill_item_draft
│   ├── OpenAiClient.kt                 # POST /v1/chat/completions，同时覆盖 OpenAI 兼容端点
│   ├── Prompts.kt                      # 共享 system prompt + tool schema（fill_item_draft）
│   └── ItemDraft.kt                    # 9 字段草稿 + per-field confidence
├── repo/
│   └── ItemRepository.kt               # interface + RoomItemRepository 实现
├── room/
│   ├── TreasureDatabase.kt             # @Database version=5, fallbackToDestructiveMigration ⚠️
│   ├── ItemEntity.kt                   # internal；含 toDomain/fromDomain；JsonCodec object
│   └── ItemDao.kt                      # observeAll/observeById/count/upsert/deleteById
└── seed/
    └── SeedItems.kt                    # 8 条 + 真实 history（移植自 prototype/project/data.jsx）
```

⚠️ Schema 已 destructive 迁移过 8 次（v1 → v5）。cycle 0009 必须切真 migration（见 agent.md 候选清单）。

简化 vs. 早期设计稿：

- **没拆 source 子层**（之前画的 `LocalItemSource` / `RemoteItemSource` interface）—— 直接 `RoomItemRepository` 持有 `TreasureDatabase`。等 cycle 0003+ 真要接同步层时再拆。
- **没拆 usecase 层** —— ViewModel 直接用 Repository。屏数 < 6 个，过早抽象没收益。
- **没拆 history_events 表** —— history 当作 JSON 列嵌在 items 里。等需要"按 kind 跨物品查"时再正规化。

## :app 内部结构（实际）

```
app/
├── MainActivity.kt                     # ComponentActivity + enableEdgeToEdge → TreasureNavHost
├── TreasureApp.kt                      # Application：构造 ItemRepository + SettingsStore + AiClient + 首启 seed
├── data/
│   └── SettingsStore.kt                # EncryptedSharedPreferences：provider / model / baseUrl / apiKey
├── voice/
│   └── VoiceCapture.kt                 # @Composable 包装 SpeechRecognizer + RECORD_AUDIO 权限 + 不可用回退
├── theme/
│   ├── Theme.kt                        # TreasureTheme + LocalTreasureColors
│   ├── Color.kt                        # paper / ink / terra / card / sub / line（浅深双套）
│   └── Type.kt                         # Cormorant / Space Grotesk / JetBrains Mono FontFamily
├── ui/
│   ├── nav/
│   │   ├── Routes.kt                   # 路由常量（Portal / Grid / Detail / Edit / Add / Settings）
│   │   └── TreasureNavHost.kt          # NavHost + 滑动转场 + 全局 ControlIsland（Detail/Edit 屏隐藏）
│   ├── portal/                         # PortalRoute + PortalScreen + PortalViewModel
│   ├── grid/                           # GridScreen + GridViewModel
│   ├── detail/
│   │   ├── DetailScreen.kt             # BottomSheetScaffold + 翻面 + 3-tab 抽屉（只读）+ 右上 · 入 Edit
│   │   └── DetailViewModel.kt          # observeById + delete
│   ├── edit/
│   │   ├── EditRoute.kt                # 单页编辑：基础 / 时间 / 标签 / 插画 / 参数(拖动) / 历史 / 实拍 / 删除
│   │   └── EditViewModel.kt            # 加载 Item → 内存表单 → 保存 / 删除 / 上传照片
│   ├── add/
│   │   ├── AddRoute.kt                 # 编排：Chat / Preview 模式 + voice / history / manual 浮层
│   │   ├── AddChat.kt                  # RECORD header + 浮动 composer + 历史 dropdown + 权限封装
│   │   ├── AddPreview.kt               # 草稿预览 9 字段 + confidence dots + 确认入库
│   │   ├── CategoryForm.kt             # 旧手动表单（[手动] 入口仍保留）
│   │   └── AddViewModel.kt             # 消息流 + sendText / sendPhoto / sendVoice → AiClient → ItemDraft
│   ├── settings/
│   │   ├── SettingsRoute.kt            # Provider chips / Model / Base URL / API Key (mask) / 测试连接 / 清除
│   │   └── SettingsViewModel.kt
│   └── components/
│       ├── ControlIsland.kt            # 底部浮动控制岛（4 颗胶囊）
│       ├── BackArrow.kt                # 手绘加粗 ← 箭头（无文字）
│       └── Ornament.kt                 # Portal 顶部罗盘装饰
└── illust/                             # 11 个 Compose Canvas 博物馆插画
    ├── IllustHelpers.kt                # drawInViewBox / palette4 / parseHex / INK
    ├── HeroIllustration.kt             # 按 HeroVector enum 分发的 dispatcher
    ├── Racket.kt                       # 球拍
    ├── Camera.kt                       # 相机（DSLR + 旁轴共享）
    ├── Lens.kt                         # 镜头
    ├── Tripod.kt                       # 三脚架
    ├── Shoes.kt                        # 球鞋
    ├── Car.kt                          # 汽车（轿车 + SUV 共享）
    ├── Laptop.kt                       # 笔电
    ├── Earbuds.kt                      # TWS 耳机
    ├── Tablet.kt                       # 平板（含 Kindle）
    ├── Watch.kt                        # 智能手表
    └── Generic.kt                      # 兜底空白博物馆牌
```

差异 vs. 早期设计稿：插画 dispatcher 没有按 enum 一一拆文件，而是把多个 enum 值映射到同一个 Composable（CAMERA_DSLR / CAMERA_RANGEFINDER → Camera()，CAR_SEDAN / CAR_SUV → Car()，KINDLE → Tablet()）。

`HeroIllustration.kt` 按 `item.heroVector` 分发到 `illust/` 下具体函数，所有具体函数都接受 `IllustrationStyle`（含 ink、palette、showCallouts）。

## 导航图（实际）

```
NavHost (start = "portal")
├── "portal"            ─→  PortalRoute
├── "grid/{categoryId}" ─→  GridRoute
├── "detail/{itemId}"   ─→  DetailRoute
├── "edit/{itemId}"     ─→  EditRoute              # cycle 0005，从 Detail 右上 · 进入
├── "add"               ─→  AddRoute               # cycle 0007 chat-first 重做
└── "settings"          ─→  SettingsRoute          # cycle 0005 起接 AI 配置
```

转场（NavHost 全局）：

| 方向 | 进 | 出 |
|---|---|---|
| 前进 (push) | `slideIntoContainer(Start)` 从右进 | `slideOutOfContainer(Start)` 向左推 |
| 回退 (pop)  | `slideIntoContainer(End)` 从左进  | `slideOutOfContainer(End)` 向右推  |

均 300ms tween。

控制岛 4 颗胶囊：门厅 / 图鉴 / 录入 / 设置。"图鉴"按当前路由：在 grid/X 时停留；不在时跳 `grid/photo` 默认。Detail / Edit 屏控制岛**隐藏**（视觉规格要求 — 这两屏右上角有自己的入口）。

## AI / 录入数据流

```
AddRoute（chat-first）
   │
   ├─ 文本 / 真照片 / 真 STT 转写
   │       │
   │       ▼
   │   AddViewModel.runExtract()
   │       │
   │       ▼
   │   AiClient.extractItemDraft(transcript, photoBytes?, category?)
   │       │            │
   │       │            └→ AnthropicClient / OpenAiClient（按 SettingsStore.provider）
   │       │                  POST + tool_use(fill_item_draft) → JSON 草稿
   │       ▼
   │   DraftCta 卡片（chat 内）
   │
   ├─ 点 DraftCta → AddPreview（9 字段 + confidence dots）
   │       │
   │       ▼
   │   AddViewModel.commitDraft()
   │       │
   │       ▼
   │   ItemRepository.upsert(Item) → 跳 Detail
   │
   └─ 点 [手动] → CategoryForm（4 品类模板）
           │
           ▼
       AddViewModel.saveManual() → ItemRepository.upsert
```

AI key 通过 `SettingsStore`（EncryptedSharedPreferences）注入到 `AiClient` 的工厂方法（`TreasureApp` 持有）。设备直连 provider，不走代理（[ADR-0004](adr/0004-byo-ai-key.md)）。

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
