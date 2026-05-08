# Cycle 0006 · spec

- **状态：** done
- **完成：** 2026-05-07

## Provider

- [x] `core/ai/Provider` enum (Anthropic / OpenAi / OpenAiCompatible)
- [x] `core/ai/AnthropicClient` 重构使用 `Prompts.kt` 共用 prompt
- [x] `core/ai/OpenAiClient` 新增；OpenAI 官方 + 兼容端点都用它
- [x] `core/ai/Prompts.kt`：`SYSTEM_PROMPT`、`EXTRACT_TOOL_NAME`、`EXTRACT_TOOL_PARAMETERS`
- [x] `SettingsStore` 加 `provider`、`baseUrl`、`defaultModelFor()`、`defaultBaseUrlFor()`
- [x] `TreasureApp.aiClient()` 按 provider 路由
- [x] `SettingsScreen` 加 provider chips；切换时自动换默认 model + baseUrl
- [x] `SettingsScreen` 显示 "Base URL" 字段（Anthropic 隐藏；OpenAI 可选；Custom 必填）
- [x] `SettingsViewModel.testConnection()` 按当前 provider 构造对应 client

## 编辑点位置

- [x] `DetailScreen.TopBar`：BackArrow 左、DotButton 右

## 参数统一

数据层：

- [x] `Item.specs: List<HeroSpec>` 单列表；`heroSpecs` / `tailSpecs` 计算属性
- [x] `Item.HERO_SPEC_COUNT = 4`
- [x] `ItemEntity.specsJson` 单列；删 hero_specs_json + 旧 specs_json Map
- [x] Schema v5（destructive）
- [x] `SeedItems` 8 条全改为单 list（heroSpecs + 旧 specs map 合并到单 list）
- [x] `ItemDraft.specs: List<HeroSpec>`（重命名 from heroSpecs）
- [x] `Prompts.SYSTEM_PROMPT` + tool schema 用单 specs 字段

UI：

- [x] `EditScreen` "参数 · 拖动选前 4 作关键参数" 单 section
- [x] 行排版：`label / value / drag-handle / delete`
- [x] `pointerInput { detectDragGesturesAfterLongPress(...) }`：长按 ≡ 启动拖动
- [x] 拖动中行用 graphicsLayer translationY 跟手；其它行计算 "make-room" 位移
- [x] 第 4 行下方 terra 色细线 + "↑ 关键 4 项"
- [x] `+ 加一行 参数` 在底部
- [x] `DetailScreen.SpecsList`（抽屉参数 tab）展示完整 `item.specs`，第 4 行后画一条 terra 细线提示
- [x] `DetailScreen.HeroSpecsTable`（详情正面）展示 `item.heroSpecs`（前 4）

## CategoryForm 对齐 EditScreen

- [x] 同 Section / LabeledField / Chip 组件样式
- [x] Sections：基础 / 时间 / 状态 / 关键参数（按 template 标签预填）
- [x] 接受 `initial: ItemDraft?` 预填 brand / model / nickname / oneLiner / 4 hero spec values
- [x] 删除之前的大 hero 预览卡片

## AddRoute 外层留空

- [x] 删除气泡浮动 + 模式 chips + AI panel
- [x] Header 保留（"Treasure / NEW ENTRY"）
- [x] 中间一行 italic "录入页交互重新设计中"
- [x] 底部 4 颗朴素品类 chip 作临时入口（重设计后移除）
- [x] 删 `AiChatPanel.kt` 和 `CategoryGlyph.kt`

## 验证

- [x] `./gradlew :app:assembleDebug` 通过（v0.9.0，13 MB）
- [ ] 装机：Settings → Provider 切到 OpenAI → 自动改 model 默认 → 填 sk-... → 保存测试
- [ ] Settings → Provider 切到 Custom → 必填 base URL → 没填测试按钮禁用
- [ ] Detail → 右上角点 → 进 EditScreen
- [ ] EditScreen 参数 section → 长按 ≡ → 拖动到顶 → 释放 → 该行变成 hero（顶部 4 行内）
- [ ] EditScreen 参数 section → 第 4 行下方有 terra 细线 + "↑ 关键 4 项"
- [ ] CategoryForm → 排版与 EditScreen 一致（label 左、下划线值右）
- [ ] AddRoute 外层 → 中间空 + 底部 4 颗品类 chip
- [ ] 抽屉参数 tab → 第 4 行下方有 terra 细线区分 hero / tail
- [ ] AI 录入 → 因为外层留空所以暂时无入口（按用户意图）

## 不在这一轮

- AI 录入入口（用户要重新画交互）
- 拍照 / 多选照片
- 真 schema migration
- 全屏看图
- AI 生成博物馆插画
