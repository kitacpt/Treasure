# Agent · 现状交接

每次工作结束更新这一份。新人/新一轮 agent 进来先读它。

## 今天 (2026-05-06)

**状态：cycle 0001 进行中 —— Portal 屏 + Room 数据层已通**

今天的进度（按时间顺序）：

1. **工程脚手架**：Gradle 多模块工程立起来（`:app` + `:core`）；OpenJDK 17、Android SDK 35 在 `~/Android/Sdk`、Gradle 8.10.2 在 `~/.local/opt/`、wrapper 装在 `android/`
2. **首个 APK**：Hello-Treasure 占位屏，验证编译链 + 主题
3. **完整 Portal 视觉**：bundled 字体（Cormorant / Space Grotesk / JetBrains Mono）、罗盘装饰、三连计数、四扇门带罗马数字 + hero 占位、Latest entry 卡片、底部浮动控制岛（4 颗胶囊）
4. **Room 数据层**：`:core` 模块启动；`Item` 域模型、`ItemEntity` + `ItemDao` + `TreasureDatabase`、`ItemRepository` 暴露 `Flow<List<Item>>`
5. **种子数据**：8 条物品从 `prototype/project/data.jsx` 移植到 `core/seed/SeedItems.kt`，覆盖全部 4 个品类
6. **VM 接通**：`TreasureApp` 当 ServiceLocator 起 DB + repo；`PortalViewModel` 把 items 流聚合成 UiState；`PortalScreen` 通过 `viewModel()` + `collectAsStateWithLifecycle()` 反应式订阅
7. APK：`android/app/build/outputs/apk/debug/app-debug.apk` ~10MB，覆盖装到 vivo X200 验证过

**今天后半段：Chunk A（Grid + Nav）+ Chunk C（Detail + Add/Edit）**

- 加 navigation-compose，路由表 `Routes.kt`，控制岛上提到 NavHost 级别（按路由判可见性）
- Portal 重构成纯展示组件，吃 `onEnterCategory` / `onOpenItem` 回调
- **Grid 屏**：横向品类胶囊 + 2 列卡片网格，从门厅 4 扇门 / 控制岛"图鉴"都能进
- **Detail 屏**：hero + 标题 + 状态徽章 + 4 行 hero specs + 底部抽屉占位；右上 edit/delete
- **Add / Edit 屏** 同一个 `AddRoute(itemId)`，null = 新建 / 非空 = 编辑预填；写 Room 后跳到 Detail
- 引入 `kotlinx-serialization`，schema bump v2，`Item` 加 `heroSpecs: List<HeroSpec>` + `specs: Map`
- `fallbackToDestructiveMigration()` —— cycle 0001 还没真用户，schema 抖动期允许吹库

**接下来：cycle 0002 全量 + 三处用户反馈修复**

- 反馈 1: Add 屏取消手工录入，改为 "coming" stub（删了 AddScreen / AddViewModel；Detail 的 edit 按钮也一起去掉）
- 反馈 2: 顶部 status bar 留白 —— `enableEdgeToEdge()` + 每个屏 `Modifier.statusBarsPadding()`；控制岛 `navigationBarsPadding()`
- 反馈 3: 页面转场改为左右推 —— `slideIntoContainer(Start/End)` + 300ms tween，前进从右进左出，回退从左回右
- **抽屉**: `BottomSheetScaffold` + 96dp peek；3 tabs（历史 ★Δ↻+− 时间轴 / 参数 key-value 表 / 影集 3×2 占位）；schema 升 v3，`Item` 加 `history: List<HistoryEvent>`（kind: ACQUIRED/MILESTONE/MAINTAIN/MOD/PARTED）
- **明信片翻面**: hero 卡片点击 `graphicsLayer { rotationY }` 600ms tween；正面 hero illustration + "0 PHOTOS · TAP TO FLIP" 角标；背面 "暂无实拍" + 添加照片占位（cycle 0003 接通真照片）
- 8 条种子物品全都补了真实历史事件（移植自 prototype/data.jsx）

**今天前半段：Chunk B —— 真博物馆线描插画**

- 翻译 prototype/project/vectors.jsx 的 10 个 V-functions 到 Compose `Canvas`：Racket / Camera / Lens / Tripod / Shoes / Car / Laptop / Earbuds / Tablet / Watch
- 共享 helper `drawInViewBox(vbW, vbH)` 在 Canvas 里模拟 SVG viewBox 的 letterbox 行为
- `HeroIllustration(item)` dispatcher 按 `item.heroVector` 分发到具体函数；多个 enum 值可共享同一个画法（CAMERA_DSLR / CAMERA_RANGEFINDER 都走 Camera，CAR_SEDAN / CAR_SUV 都走 Car，KINDLE 走 Tablet）
- `Generic` 给没具体画法的 enum 兜底（带画框 + palette swatch 的"空白博物馆牌"）
- 删了 `HeroPlaceholder`；Portal / Grid / Detail / Add 都改用 `HeroIllustration`
- **没做**：callout 引线 + 罗马数字标注（i · pentaprism 那种）—— 加进去要在 Canvas 里调 `TextMeasurer`，下一刀

下一步候选（cycle 0001 收尾或开 cycle 0002）：

- cycle 0001 收尾：callout 引线 + 罗马数字标注；DatePicker；UI test；agent.md 复盘
- cycle 0002：抽屉（历史 / 参数 / 影集）+ 详情翻面 + 真实照片
- cycle 0003：设置 + AI 服务 + 对话式录入

不打算这一轮做的（cycle 0002+）：

- 抽屉（历史/参数/影集）、明信片翻面、DatePicker、AI、后端同步

## 历史

| 日期 | 摘要 |
|---|---|
| 2026-05-06 | cycle 0002：抽屉（历史/参数/影集）+ 明信片翻面 + status bar 留白 + 滑动转场 + Add stub（本次） |
| 2026-05-06 | cycle 0001：Chunk B 博物馆插画（10 形状 + Generic） |
| 2026-05-06 | cycle 0001：Grid + Nav + Detail + Add/Edit + serialization v2 schema |
| 2026-05-06 | cycle 0001 开工：工程脚手架 → Portal 视觉 → Room 数据层 |
| 2026-05-06 | 项目骨架搭建完成 |
| 2026-05-06 | Claude Design 导出原型；视觉方向锁定为博物馆图鉴风（参见 [`prototype/chats/chat1.md`](prototype/chats/chat1.md)） |

## 给下一个 agent 的备忘

- 改视觉之前一定先打开 `prototype/project/Treasure.html` 对照 —— 那是活的视觉规格，比文字描述准
- ADR 是钉死的决策。要推翻某个 ADR，写一份新的 ADR 来 supersede 它，不要悄悄改老的
- 一个 cycle 一个文件夹（`openspec/NNNN-*/`），一个 cycle 一个改动，做完再开下一个
- 任何"日期"在文档/code 里都写绝对日期（YYYY-MM-DD），不要写"上周"、"昨天"
