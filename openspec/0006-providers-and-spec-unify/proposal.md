# Cycle 0006 · Providers + 参数统一 + 编辑入口/空白录入

- **状态：** done
- **完成：** 2026-05-07

## 这一刀切什么

四件用户反馈：

1. **OpenAI / 自定义 provider 支持**
2. **编辑入口（点）从详情左上角移到右上角**
3. **关键参数 / 完整参数统一为同一种结构 + 拖动重排选前 4 作 hero**
4. **手动录入表单与 EditScreen 排版保持一致**；外层录入页（手动 / AI 切换那个）**留空**等用户重画

## 1. Provider 抽象

- `core/ai/Provider` enum：`Anthropic` / `OpenAi` / `OpenAiCompatible`
- `core/ai/AnthropicClient`（已有）+ `core/ai/OpenAiClient`（新；同时覆盖 OpenAI 官方 + 兼容端点）
- `core/ai/Prompts.kt` 抽出共用 system prompt + tool schema
- `SettingsStore` 加 `provider` + `baseUrl` 字段
- `TreasureApp.aiClient()` 按 provider 路由
- `SettingsViewModel` + `SettingsScreen` 加 provider chips、按 provider 切换默认 model + baseUrl、必填校验（custom 必须填 baseUrl）
- 默认 model 改为 provider-specific：Anthropic = `claude-haiku-4-5-20251001`、OpenAI = `gpt-4o-mini`

## 2. 编辑点移到右上

`DetailScreen` 顶部 bar `BackArrow ··· DotButton` → `BackArrow ··· (weight) ··· DotButton`。Detail 屏所有空状态文案的"点左上 · 编辑添加"也跟着改成"点右上 · 编辑添加"。

## 3. 参数统一 + 拖动重排

数据层：

- `Item.heroSpecs: List<HeroSpec>` + `Item.specs: Map<String, String>` → 单个 `Item.specs: List<HeroSpec>`，**前 4 项**作为 hero 显示
- 在 `Item` 里加计算视图：`heroSpecs` (= `specs.take(4)`) 和 `tailSpecs`（不持久化，只是 getter）
- `ItemEntity` 列名 `specs_json`（JSON List），删 `hero_specs_json` + `specs_json` Map
- Schema **bump v4 → v5**，仍 `fallbackToDestructiveMigration`
- `SeedItems` 8 条物品全部把原 `heroSpecs + specs` 拼成单 list 重写
- `ItemDraft.heroSpecs` → `ItemDraft.specs`；prompt 同步约定"前 4 跟模板顺序，后面任意"

UI：

- `EditScreen` "关键参数 / 完整参数" 两 section 合并成一段 `参数 · 拖动选前 4 作关键参数`
- 单列表，每行：`[label] [value] [≡ drag] [− delete]`
- 拖动重排：手动实现的 `pointerInput { detectDragGesturesAfterLongPress(...) }`，长按 `≡` 启动拖动；非拖动行用 `graphicsLayer.translationY` 计算 "make-room" 位移
- 第 4 行下方加 terra 色细线 + "↑ 关键 4 项"，告诉用户哪条是分界线
- `DetailScreen` `HeroSpecsTable` 仍展示 `item.heroSpecs`（计算属性）；抽屉"参数" tab 展示完整 `item.specs`，第 4 行下面也画一条 terra 细线

## 4. CategoryForm 对齐 EditScreen + 外层留空

`CategoryForm`：

- 改用 EditScreen 的 Section + LabeledField 行排版（label 56dp 左 + 下划线值右）
- 删掉之前的"big hero preview"卡片
- Sections 顺序：基础 / 时间 / 状态 / 关键参数（按品类模板预填 4 个标签）
- 接受 `initial: ItemDraft?` 时，`specs.getOrNull(i)?.value` 映射到 4 个 hero 字段

`AddRoute` 外层（之前的 4 气泡 + AI 模式 chip-toggle 那一屏）：

- 删除气泡浮动 + 模式切换 + AiChatPanel
- 仅保留：Header（"Treasure / NEW ENTRY"）+ 中间一行 italic "录入页交互重新设计中"
- 底部留 **临时入口**：4 颗朴素品类 chip，点击直接弹 CategoryForm — 仅为保留可测路径，重设计后会移除
- 删掉 `AiChatPanel.kt` 和 `CategoryGlyph.kt`（外层已不用）

## 不做（cycle 0007+ 候选）

- AI 录入入口（用户重画完之后接通）
- 拍照（直接调相机）/ 多选照片
- AI 生成博物馆插画
- 真 schema migration（cycle 0001-0006 已经 6 次 destructive，必须收手）
- 全屏看图浏览器

## 验收

详见 [`spec.md`](spec.md)。
