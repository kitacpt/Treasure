# Cycle 0002 · 工作笔记

## 抓的几个坑

- **抽屉高度随 tab 内容浮动**（用户最先反馈）：BottomSheetScaffold 的 sheetContent 默认按 wrap-content 计高，短 tab（设置）贴底、长 tab（历史 6 条）大半屏。修法：`Column(modifier = Modifier.fillMaxWidth().height(screenHeight * 0.78f))`，里面 `Box.weight(1f)` 装 tab 内容，每个 tab 内部 `verticalScroll`。修完后切换无突变。
- **BottomSheetScaffold 是 experimental** —— 编译报警告级错。`@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)`。
- **edge-to-edge 后内容压在状态栏**：`enableEdgeToEdge()` 必须配合每屏自己的 `statusBarsPadding()`；`navigationBarsPadding()` 给控制岛。
- **gradle wrapper 不能完全离线生成**：第一次 `gradle wrapper` 需要联网下 distribution；后续 `./gradlew` 自给自足。
- **ItemEntity 自定义 JSON 编码格式**坑爹：cycle 0001 我用了 CSV 风的分隔逗号（`encodePalette`/`decodePalette`），cycle 0002 加 history/specs 时切到 kotlinx-serialization。统一成 `JsonCodec` object，老 palette 也走 JSON 数组。schema bump 顺手把这个清掉。

## 折中

- **影集 / 添加照片 / settings"更多操作"** 全是 stub 文案。给视觉信号"这里以后有东西"，比假装没规划好。
- **flip = 600ms** —— 比设计稿默认快一点。原型注释写的是 600ms。500ms 太急、800ms 拖。
- **drawer peek = 40dp** —— Material3 DragHandle 默认 22dp 上下 padding + 4dp 圆条 = 48dp 总高，但 peek=48 会在某些屏幕上把拖拽柄完全卡进圆角。40dp 略短一点更稳。

## 没做的，明确留账

- 真 migration（cycle 0001-0002 全程靠 `fallbackToDestructiveMigration`，cycle 0003 之后必须收手）
- 抽屉里"添加历史"按钮 / 表单
- callout 引线 + 罗马数字标注（用 Compose `TextMeasurer`，至少要测半天）
- UI test 套件（`./gradlew :app:connectedDebugAndroidTest`）
- 暗模式真上手测过（仅理论上 OK）

## 给下一个 agent

- 推 cycle 0003：先做"真实照片上传"，理由 = 翻面背面"+ 添加照片"按钮已经在那等着，UX 闭环最值
- 如果先做 AI（设置 + 对话录入）：注意 ADR-0004 里钉死的"设备直连 provider，不走代理"，key 用 EncryptedSharedPreferences
- schema 加字段时千万别忘了同时改 `ItemEntity` + `Item` + `JsonCodec` + `SeedItems`，少一个就崩
- AlertDialog 的 `containerColor / titleContentColor / textContentColor` 要手设，否则 Material3 默认色和我们的 paper/ink 不搭

## 复盘

cycle 0002 把 cycle 0001 暴露的 7 个用户反馈一并扫掉，加上抽屉 + 翻面，一天打住。三件意外收获：

1. drawer height 用 `screenHeightDp * 0.78f` 计算，比硬编 `640.dp` 跨屏更稳
2. `graphicsLayer { rotationY = 180f }` 倒回背面读法 —— 比手动 mirror 内容简单得多
3. `kindGlyph` 函数纯字符（`+ ★ ↻ Δ −`），不引图标库，省一个 dep
