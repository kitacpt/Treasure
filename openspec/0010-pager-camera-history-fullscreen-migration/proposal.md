# Cycle 0010 · 4 Tab 横滑 + 拍照 + 历史持久化 + 全屏看图 + schema migration

- **状态：** done
- **完成：** 2026-05-08

## 用户反馈 5 条 + cycle 0010 候选 5 条 一并处理

| # | 反馈 / 候选 | 实现 |
|---|---|---|
| 1 | Edit / 新增历史的 “类型” 行换两行；编辑页 “品类” 的 “电子产品” 也换行；改下拉避免兼容麻烦 | 新增 `ui/components/InlineDropdown`：折叠态 `当前值 ▾`，展开后内嵌列表；EditScreen 的 “品类” + HistoryDialog 的 “类型” 都换上；HistoryKind 顺手补了中文 label |
| 2 | “尚未配置 AI” 横幅画风突兀 | 横幅去掉方框 / 按钮，只剩 “● 尚未配置 AI · 前往设置 →” 一行字，贴在 ChatHeader 0.5dp divider 下 |
| 3 | 手动录入页顶部居中放插画选择器 | CategoryForm 顶部加了 124dp paper 大图（点切换） + 56dp 小缩略图横滚；按品类用 `heroVectorOptionsFor()` 过滤；新增 heroVector 状态由 `saveManual` 接收 |
| 4 | 语音怎么还是占位 | 用户跳过了这一项。SpeechRecognizer 在 vivo 国行 ROM 不可用是已知事项，cycle 0011+ 再补云端 STT |
| 5 | 四个页面左右滑切换 | NavHost 收成 Main / Detail / Edit 三条路由；新增 `ui/main/MainScreen`：HorizontalPager(4) 装 Portal / Grid / Add / Settings；ControlIsland 选中 = pagerState.currentPage；门厅点品类 / “去设置” 横幅都走同一个 pagerState |
| 6 | cycle 0010 候选 1：真 schema migration | `exportSchema = true`，KSP `room.schemaLocation = $projectDir/schemas`；`Migrations.ALL` 现在装 `MIGRATION_5_6` (加 add_conversations / add_messages 两表) + `MIGRATION_6_7` (items 加 callouts_json)；删了 `fallbackToDestructiveMigration()`，仅留 `OnDowngrade` 兜底；写了 [ADR-0006](../../docs/adr/0006-schema-migrations.md) 钉规矩 |
| 7 | cycle 0010 候选 2：历史对话持久化 + 多轮 refine | 新表 `add_conversations` / `add_messages`；新 `core/repo/AddConversationRepository`；AddViewModel 启动时建一段新对话，每次发消息都 append；history 抽屉点旧对话 → `openConversation(id)` 从 Room 重读；`AiClient.extractItemDraft` 接 `priorTurns: List<AiTurn>`，AddViewModel 跑前把当前对话的最后 20 条作为 prior 传过去，Anthropic / OpenAI 两端 buildPayload 都消费 |
| 8 | cycle 0010 候选 3：拍照 + 多选照片 | EditScreen 的 PhotoSection 顶部并排两个 terra-描边按钮：📷 拍照 / + 选照片；拍照走 `ActivityResultContracts.TakePicture()` + FileProvider（manifest 加 provider + xml/file_paths.xml）+ CAMERA 权限；选照片走 `PickMultipleVisualMedia(maxItems = 9)`；DetailViewModel 加 `addPhotos(List<Uri>)` 一次性写入 |
| 9 | cycle 0010 候选 4：AI 生成博物馆插画 | 用户跳过了这一项。当前依然走 11 个预置 HeroVector |
| 10 | cycle 0010 候选 5：全屏看图浏览器 + callout | 新建 `ui/photo/FullscreenPhotoViewer`：HorizontalPager 横滑、双指 1×–5× 缩放、双击切换、长按某点弹文字输入；callout 存 `Map<path, List<PhotoCallout>>`，画 terra dot + paper 气泡；DetailScreen 的影集 tab 点缩略图 → 全屏 viewer，传 `onSaveCallout = vm::addCallout` |

## 视觉 / 数据系统升级

- 共享 `InlineDropdown`：状态 + 历史类型 + 品类都用同一个；后续选项 ≥3 个就别用 chip-row
- Settings 已经在 cycle 0009 改成抽屉摘要，这一轮没动
- Schema baseline 从 v5 走到 v7；`core/schemas/com.treasure.core.room.TreasureDatabase/{5,6,7}.json` 入库
- `TreasureApp.conversationRepository` 加进 ServiceLocator
- ItemEntity 多了 `callouts_json` 列，`JsonCodec` 多了一对 `encode/decodeCallouts`
- `AiClient` 接口新加 `priorTurns: List<AiTurn> = emptyList()` 参数；`AnthropicClient` / `OpenAiClient` 都改了 buildPayload 拼 prior 历史

## 不在这一轮（用户主动跳过 + 时间预算）

- 云端 STT（OpenAI Whisper）— 用户说不做。SpeechRecognizer 在 vivo 国行还是回退到占位
- AI 生成博物馆插画 — 用户说继续用预置
- 多轮 “请把这个改一下” 的图片 vision context — prior_turns 只传文字，UserPhoto 还是只在当条 message 走 image block
- MigrationTest 自动化（依赖 androidTest source set + 模拟器 / 真机），cycle 0011 补
- 全屏看图的 callout 编辑 / 删除 / 批量管理 — 这一刀只做 “长按加” 一种 affordance

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
