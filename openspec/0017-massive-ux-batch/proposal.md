# Cycle 0017 · 一刀十改 · UX 大批量

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 10 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | Kimi k2.5 测试老 timeout | 没法在 agent 这边 curl 用户的 key（凭据外发风险），但很可能是 callTimeout 太短：之前固定 120s，reasoning 模型可能要 1-3 min。`OpenAiClient` / `AnthropicClient` 的 `defaultHttpClient` 现在按 thinking 状态切档：thinking on（toggle 或模型名隐式命中）→ 360s；非 thinking → 120s |
| 2 | Portal 统计去掉 OWNED | `Tally` 现在只剩 `items` + `rooms` 两列 |
| 3 | 主页跳到 Grid 后 chips 没聚焦到对应品类 | `CategoryChips` 从 `Row + horizontalScroll` 换成 `LazyRow + LaunchedEffect(selectedIndex) { animateScrollToItem }`，从门厅过来时所选 chip 自动滚到可见区 |
| 4 | Edit 副标去 EDIT 前缀 / 头像处直接管理影集 / 删时间区段 / 拖动跨分割线遮挡 | (a) `EditPageHeader` subtitle 直接用 `item.category.nameZh`；(b) `HeroAvatarPicker` 加了 `onTakePhoto` / `onPickPhotos` / `onRemovePhoto` 三个回调，展开区顶部多两个 terra-描边动作 chip（📷 拍照 / + 选照片）+ 影集照片小圆 long-press 弹删除确认；EditScreen 的 "实拍" Section + 整个 PhotoSection composable 都删了；activity-result launchers 上提到 EditScreen body 顶层；(c) "时间" Section + 两个 LabeledField 整段删；acquired/parted 字段保留但通过历史 section 编辑；(d) ReorderableSpecs 拖动期间把 HeroDivider 替换成同高 Spacer，避免 make-room shift 错位 + 被拖行覆盖分割线 |
| 5 | 图鉴页第一个 tab 是 "全部" | `GridUiState.currentCategory: Category?` 变 nullable；null = 全部聚合。`CategoryChips` 在 LazyRow 第一个 item 渲染 `AllChip`；`EmptyHint(category: Category?)` 适配 null；`MainScreen.gridCategoryId` 默认仍是 `Category.PHOTO.id`（用户进 Grid 默认看摄影），用户想要全部点头号 chip。`GridViewModel.factory` 多 `ALL_FILTER_ID = "all"` sentinel 备未来用 |
| 6 | 录入页历史改抽屉 / New entry 后缀 / 去⊕ / 点后不自动收 | (a) `HistoryDropdown` 从 top-right 小卡片改成左侧全高侧边抽屉（`align(CenterStart) + fillMaxHeight + widthIn 280-320dp + statusBarsPadding/navigationBarsPadding`）；(b) `FakeConversation` 多 `time: String`（HH:MM）；HistoryRow 显示时若 title 是默认 "New entry" 就拼上 "· HH:MM" 后缀；(c) ChatHeader 的 `onTapNewChat` 参数 + ⊕ IconCircleButton 都删了；新增对话只通过抽屉里的 "新对话" 行；(d) 抽屉内 onPick / onNewChat 不再调 onToggleHistory()，让用户可以连续切几段 |
| 7 | 手动录入弹层去标题副标 | `ManualCategoryPicker` 删掉 "手动录入 · 选品类" titleMedium + "TAP A ROOM TO BEGIN" 副标，直接展示 6 行品类选项 |
| 8 | 手动录入完全复用 Edit 排版 | `CategoryForm` 的 sections 重新组织成跟 Edit 一致：基础 / 标签（状态 chips 横排，跟 Edit 的 chip 行一致） / 参数 · ${category}。删了独立 "状态" + "时间" section + tagline 中央 italic 文案。subtitle 直接用 `template.category.nameZh`（不再有 "NEW · BADMINTON" 那种全大写英文标签） |
| 9 | 暂时去掉语音输入按钮 | `Composer` 里 MicGlyph 圆按钮 + onStartVoice 参数都删；AddChat / AddRoute 链路上的 `onStartVoice`、`voiceOn` state、`VoiceCapture` 调用、`com.treasure.voice.VoiceCapture` import 整层清掉。云端 STT 兜底正式上来再加回 |
| 10 | Settings "清除所有设置" → "重置设置" + 二次确认 | `DangerZone` 文案改 "重置设置"；点行不立刻清，先 `confirming = true` → 弹 AlertDialog（标题 "重置设置？" + 解释 "API key、provider、temperature、thinking 等所有 AI 配置都会被清空…不可撤销" + 取消 / 重置 双按钮），点重置才真 onClear |

## 不在这一刀

- 云端 STT 兜底（语音按钮已临时去除，等云端 STT 接进来再加回）
- 多轮 refine 的图片 vision context
- AI 生成博物馆插画
- Settings preset (Xiaomi MiLM) URL 校准
- MigrationTest CI 接入

## 注意

- 用户在聊天里贴了 Moonshot API key 让我帮验证。harness 阻止了我把 key 外发到 api.moonshot.cn（防止 transcript 泄露），所以没真 curl。强烈建议用户重置 / 旋转那把 key。

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
