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

| 用途 | 依赖 |
|---|---|
| UI | `androidx.compose:compose-bom`、`androidx.compose.material3`、`androidx.activity:activity-compose` |
| 导航 | `androidx.navigation:navigation-compose` |
| 持久化 | `androidx.room:room-runtime`、`room-ktx`、`room-compiler`（KSP） |
| 偏好 | `androidx.datastore:datastore-preferences` |
| 安全存储 | `androidx.security:security-crypto`（[ADR-0004](adr/0004-byo-ai-key.md) 用） |
| 异步 | `kotlinx-coroutines-android`、`kotlinx-coroutines-core` |
| 序列化 | `kotlinx-serialization-json`（specs / hero_specs / palette 字段都是 JSON） |
| DI | **不引** Hilt/Koin，先用手写 ServiceLocator。等屏数 > 5 个再上 DI。 |
| 网络 | （cycle 0003+ 才引 `ktor-client` / `okhttp`） |
| AI client | （cycle 之后）`com.anthropic:anthropic-java`（或 OkHttp 直调） |

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

## :core 内部结构

```
core/
├── domain/
│   ├── Item.kt                         # 领域模型（不依赖 Room）
│   ├── HistoryEvent.kt
│   ├── Category.kt                     # 4 个 enum：BADMINTON / PHOTO / CARS / TECH
│   ├── ItemStatus.kt                   # owned / parted / rented
│   └── HeroVector.kt                   # 预置插画 enum：RACKET / CAMERA_DSLR / LENS_PRIME / …
├── repo/
│   ├── ItemRepository.kt               # interface
│   ├── DefaultItemRepository.kt        # 实现，用 LocalItemSource
│   └── source/
│       ├── LocalItemSource.kt          # interface
│       ├── RoomItemSource.kt           # 实现
│       └── RemoteItemSource.kt         # interface（cycle 0003+ 用）
├── room/
│   ├── TreasureDatabase.kt
│   ├── ItemEntity.kt
│   ├── HistoryEventEntity.kt
│   ├── ItemDao.kt
│   ├── HistoryEventDao.kt
│   └── Converters.kt                   # JSON ↔ List<HeroSpec> / Map<String,String> / List<String>
├── usecase/
│   ├── GetCategoryCounts.kt
│   ├── ListItemsByCategory.kt
│   ├── GetItemDetail.kt
│   ├── CreateItem.kt
│   ├── UpdateItem.kt
│   └── DeleteItem.kt
├── seed/
│   └── SeedItems.kt                    # 首次启动预置（移植自 prototype/project/data.jsx）
└── ai/                                 # cycle AI 之后才有具体实现
    └── AiClient.kt                     # interface（[ADR-0004]）
```

## :app 内部结构

```
app/
├── MainActivity.kt
├── TreasureApp.kt                      # NavHost + Theme
├── theme/
│   ├── TreasureTheme.kt                # 应用 Color/Typography 覆盖
│   ├── Color.kt                        # paper / ink / terra / sub / line / card
│   └── Type.kt                         # Cormorant / Space Grotesk / Mono / Noto SC
├── ui/
│   ├── portal/PortalScreen.kt
│   ├── grid/GridScreen.kt
│   ├── detail/DetailScreen.kt
│   ├── add/AddScreen.kt                # 表单（cycle 0001 不带 AI）
│   ├── settings/SettingsStubScreen.kt  # cycle 0001：占位 "AI integration coming"
│   └── components/
│       ├── ControlIsland.kt            # 底部浮动控制岛
│       ├── HeroIllustration.kt         # 分发到具体绘制
│       ├── CalloutLine.kt              # 标注线 helper（对应原型 Callout）
│       ├── PlateLabel.kt               # 罗马数字小标（对应原型 PlateLabel）
│       └── Ornament.kt                 # Portal 顶部的罗盘装饰
└── illust/                             # 各物品的 Canvas 绘制函数
    ├── HeroIllustration_Racket.kt
    ├── HeroIllustration_CameraDSLR.kt
    ├── HeroIllustration_LensPrime.kt
    ├── HeroIllustration_CarSedan.kt
    ├── HeroIllustration_Tripod.kt
    ├── HeroIllustration_Laptop.kt
    ├── HeroIllustration_Earbuds.kt
    ├── HeroIllustration_Kindle.kt
    ├── HeroIllustration_Watch.kt
    └── HeroIllustration_Tablet.kt
```

`HeroIllustration.kt` 按 `item.heroVector` 分发到 `illust/` 下具体函数，所有具体函数都接受 `IllustrationStyle`（含 ink、palette、showCallouts）。

## 导航图

```
NavHost (start = "portal")
├── "portal"
├── "grid/{categoryId}"
├── "detail/{itemId}"
├── "add"           （cycle 0001）
├── "edit/{itemId}" （cycle 0001）
└── "settings"      （cycle 0001：stub 屏）
```

控制岛 4 颗胶囊里"录入"和"设置"同样进 `add`/`settings` 路由。门厅与品类间的"4 扇门"点击 → `grid/<id>`。

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
