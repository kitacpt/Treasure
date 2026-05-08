# Cycle 0007 · spec

- **状态：** done
- **完成：** 2026-05-07

## 设计源

- [x] 拉取 Anthropic Design API 第二版包，extract 到 `/tmp/design-extract-v2/`
- [x] 与 v1 对比：只 3 个文件不同（Treasure.html、direction-a.jsx、chats/chat1.md）
- [x] 整包复制到 `prototype/add-page-v2/`，加 `HANDOFF.md` 说明差异和实施约束

## AddRoute orchestrator

- [x] 默认进 chat 模式，无 chooser 起步页
- [x] 状态：mode (Chat / Preview) + voiceOn + historyOpen + manualPickerOpen + manualSession
- [x] 控制岛在 NavHost 层，AddRoute 不重画
- [x] 草稿 CTA 点击 → mode = Preview；预览返回 → mode = Chat
- [x] 确认收入图鉴 → commitDraft → newConversation → mode = Chat → onSaved(id) → 跳 Detail

## AddChat 面板

- [x] Header："录入 + 当前对话名 ▾" 左 + [🕐][⊕][手动] 右
- [x] 标题旁的对话名 + ▾ 全可点，开历史抽屉
- [x] 历史抽屉：从右上角下垂，最大 260dp 宽，列出 conversations + 底部"+ 新对话"
- [x] 消息列表：LazyColumn + auto-scroll 到底部
- [x] 5 种气泡：Assistant / UserText / UserPhoto / UserVoice / DraftCta
- [x] 助手气泡：italic serif，card 背景，4/14 圆角
- [x] 用户文本气泡：ink 背景，paper 文字，14/4 圆角
- [x] 用户照片气泡：120dp 方块，Coil 渲染
- [x] 用户语音气泡：ink 背景 + Canvas 波形 + 时长 + italic 转写
- [x] DraftCta：缩略 hero + DRAFT · N FIELDS + "草稿已就绪" + italic 副本 + → 箭头
- [x] busy 时显示"正在思考…"占位
- [x] 没配 AI key 时底部显示"尚未配置 AI · 去设置"提示条
- [x] Composer：浮于控制岛上方（`bottom = navBar + 88dp`）
- [x] Composer：📷 / 文本输入 / 🎙 圆形按钮 / → 发送圆形按钮
- [x] 发送按钮：dirty 时 ink，clean 时 line 灰
- [x] 输入框 placeholder："说说这件东西…"

## VoiceOverlay

- [x] 全屏 ink 半透明蒙层（`#CC1A1815`）
- [x] 中央：波形 + italic 转写（"二零二三年情人节，一万二千五…"） + "松开发送 · TAP TO STOP"
- [x] Tap 蒙层 dismiss → 自动 sendVoiceStub（喂入预写转写）

## AddPreview 屏

- [x] Header："草稿预览" 左 + sub "REVIEW · EDIT · CONFIRM" + 右上"← 换一种"返回
- [x] Hero 卡片：BRAND · YEAR caps + 大字 model + italic 一句话 + thumbnail（HeroIllustration 按 category 模板）
- [x] 角标：DRAFT №ddd（hash 自 brand+model，3 位数字）+ UNCONFIRMED
- [x] Confidence 图例：· 确定 (ink) / · 可能 (terra) / · 需补充 (sub 50%)
- [x] 9 字段（`PreviewField` enum）：Category / Brand / Model / Nickname / Color / AcquiredDate / AcquiredPrice / AcquiredChannel / OneLiner
- [x] 每行：confidence dot + label 72dp + value（空时斜体 "（点击补充）"）+ ✎
- [x] Tap 行 → 切 BasicTextField + 确认 / 取消按钮
- [x] 确认 → 写回 draft（map 到 first-class 字段或 specs 列表）+ confidence 升 high
- [x] Footer 浮在控制岛上方（`navigationBarsPadding + 78dp`）
- [x] Footer："继续修改"（描边）+ "✓ 确认收入图鉴"（ink 实色，weight 1f）

## AddViewModel

- [x] `messages` / `conversationTitle` / `draft` / `busy` / `aiAvailable` / `errorMessage` 状态
- [x] `recentConversations` stub（含当前一条）
- [x] `newConversation()` — 重置消息为打招呼
- [x] `sendText(text)` — 加 user 气泡 + 调 extract
- [x] `sendPhoto(uri)` — 加 photo 气泡 + 调 extract（image bytes）
- [x] `sendVoiceStub()` — 加 voice 气泡 + 调 extract（stub 转写）
- [x] `updateDraftField(field, value)` — 编辑 draft 单字段
- [x] `commitDraft(onSaved)` — 构 Item → upsert
- [x] `saveManual(...)` — cycle 0006 同款，给 CategoryForm 调用
- [x] `refreshAiAvailability()` — 从 Settings 回来时更新 aiAvailable

## 手动入口

- [x] 顶部"手动"按钮 → ManualCategoryPicker（蒙层卡片 + 4 品类行）
- [x] Pick 品类 → ModalBottomSheet 弹 CategoryForm（cycle 0006 不变）

## 文档

- [x] `prototype/add-page-v2/` + `HANDOFF.md`（差异 + 关键交互 + Compose 实施注意）
- [x] `openspec/0007-add-page-v2/{proposal,spec,notes}.md`
- [ ] `agent.md` 更新（下一个 commit 之前做）
- [ ] `openspec/README.md` 索引（同上）

## 验证

- [x] `./gradlew :app:assembleDebug` 通过（v0.10.0，13 MB）
- [ ] 装机：进 Add 看到聊天首屏 + 助手打招呼 + composer 浮在控制岛上方
- [ ] 输文字 → 发送 → 看到用户气泡 → 助手"正在思考"→ 出 DraftCta（如果配了 AI key）
- [ ] 没配 AI 时：底部显示"尚未配置 AI · 去设置"提示条 + 发送进入"还没配 API key"占位回复
- [ ] 点 🎙 → 全屏蒙层 + 波形 → 点 dismiss → 自动 stub 一条语音消息 + 调 AI
- [ ] 点 📷 → photo picker → 选 → 用户照片气泡 + 调 AI（视觉 extract）
- [ ] 点 DraftCta → 切 Preview 屏 → 改昵称 → 确认 → 标题更新 → 点确认收入图鉴 → 跳新 Detail
- [ ] 点 🕐 / ▾ / 标题 → 历史抽屉打开
- [ ] 点 ⊕ 新对话 → 消息清空回到打招呼
- [ ] 点"手动" → 蒙层选品类 → 选 → 弹 CategoryForm（cycle 0006 原状）→ 保存 → 跳 Detail

## 不在这一轮

- 真 STT
- 历史持久化
- 多轮对话
- 拍照（直调相机）
- AI 生成博物馆插画
- 真 schema migration
