# openspec/ · 变更周期

每个**变更周期（cycle）**= 一次"我们打算改 / 加一件事"的全过程。0001-0031 在这里以"一文件夹三文档"的形式存档；0032 起改成在 [`../agent.md`](../agent.md) 的 "Cycle 一览 + 历史" 表里行内记录（更轻量，不再拆 proposal/spec/notes）。

## 一个 cycle 的形状（0001-0031 模式）

```
openspec/
└── NNNN-<short-slug>/
    ├── proposal.md      # 做什么、为什么、不做什么
    ├── spec.md          # "做完"的可验收条件
    └── notes.md         # 工作笔记
```

编号四位数字，递增；slug 用连字符。

## 流程（0001-0031 模式）

1. **提案**：写 `proposal.md`，对齐范围
2. **规格**：写 `spec.md`，钉死验收条件
3. **实施**：边做边在 `notes.md` 记笔记
4. **收尾**：proposal / spec 顶部 status 改 `done`，[`../agent.md`](../agent.md) 同步更新

一次只开**一个** in-flight cycle。要并行 → 拆成两个 cycle，按编号顺序。

## 0032+ 的新做法

cycle 0032 起不再单独建文件夹 / 三件套 docs；改成在 [`../agent.md`](../agent.md) 的 "Cycle 一览" 表里行内一两段写完，"历史" 表里按日期细记每次 patch。这样：

- 小迭代不用为了"开 cycle"额外建一堆文件
- 一个 agent 重启后只读 `agent.md` 就能完整复原近期工作
- ADR 仍单独写在 [`../docs/adr/`](../docs/adr/)，跟 cycle 解耦

## ADR vs cycle

- ADR 是"为什么这么定"的钉子，长期生效（[`../docs/adr/`](../docs/adr/)）
- cycle 是"这一次具体做什么"
- 一个 cycle 可能产出新的 ADR；老 ADR 被推翻 → 写新 ADR `Supersedes ADR-xxxx`，老的不悄悄改

## 全量 cycle 索引

| # | 状态 | 标题（点击 = openspec 文件夹 / 否则见 [`../agent.md`](../agent.md) 历史表） |
|---|---|---|
| 0001 | done | [MVP · Portal + Grid + Detail + Add (手动 stub)](0001-mvp-portal-grid-detail-add/proposal.md) |
| 0002 | done | [抽屉 + 明信片翻面 + 视觉 polish](0002-drawer-flip-polish/proposal.md) |
| 0003 | done | [真实照片 + 抽屉内嵌编辑](0003-photos-and-inline-edit/proposal.md) |
| 0004 | done | [录入页（手动 + AI 占位）+ Detail 全字段编辑](0004-add-route-and-full-edit/proposal.md) |
| 0005 | done | [AI 服务接通 + 编辑屏重做](0005-ai-and-edit-screen/proposal.md) |
| 0006 | done | [OpenAI / 自定义 provider + 参数统一](0006-providers-and-spec-unify/proposal.md) |
| 0007 | done | [录入页 v2：chat-first + 草稿预览](0007-add-page-v2/proposal.md) |
| 0008 | done | [录入页 polish + 真 STT + app 图标](0008-add-page-polish/proposal.md) |
| 0009 | done | [UI polish · Settings 改造 · 共享 SectionDivider](0009-ui-polish-and-settings-rework/proposal.md) |
| 0010 | done | [4 Tab 横滑 · 拍照 · 历史持久化 · 全屏看图 · 真 schema migration](0010-pager-camera-history-fullscreen-migration/proposal.md) |
| 0011 | done | [手动录入弹层 · 历史改名删除 · Coffee/Wine + 5 张插画 · MigrationTest](0011-history-ux-coffee-wine-migrationtest/proposal.md) |
| 0012 | done | [Callout 编辑 / 删除 · 立体魔戒图标](0012-callout-edit-delete-and-ring-icon/proposal.md) |
| 0013 | done | [性能（@Immutable + Pager lazy）· 修插画变白 · 图标精简](0013-perf-illust-fix-icon-trim/proposal.md) |
| 0014 | done | [AI 配置 temperature/thinking · Kimi tool_choice 修 · prompts 同步](0014-ai-knobs-ime-prompts/proposal.md) |
| 0015 | done | [Thinking 自动嗅探 · 测试结果规范化](0015-thinking-detection-test-result-polish/proposal.md) |
| 0016 | done | [Portal 空品类显示 · 头像影集 · Kimi k 嗅探](0016-portal-fixes-photo-avatar-kimi-k-detect/proposal.md) |
| 0017 | done | [一刀十改：thinking 360s · Grid 全部 tab · 拖动 fix](0017-massive-ux-batch/proposal.md) |
| 0018 | done | [历史改 ModalBottomSheet · AI 状态灯三档](0018-history-sheet-record-subtitle-status-light-readtimeout/proposal.md) |
| 0019 | done | [Grid 选择持久化 · AI 闲聊不报错 · 全屏单指翻页](0019-grid-sync-chat-fallback-status-light-pager-share-preview/proposal.md) |
| 0020 | done | [影集翻页 · URL 真 fetch（PageFetcher）](0020-photo-viewer-fix-url-fetch/proposal.md) |
| 0021 | done | [SelectionContainer · PageFetcher 三态 · 防爬启发](0021-chat-copy-paste-fetch-blocked-detection/proposal.md) |
| 0022 | done | [Record 续上次会话 · fetch 状态可见 · charset 探测](0022-resume-conversation-fetch-status-vision-hint/proposal.md) |
| 0023 | done | [草稿页镜像 Edit · 放开 hero spec · 聊天图单击预览](0023-draft-mirrors-edit-photo-preview-vision-chip-fix/proposal.md) |
| 0024 | done | [会话 = 草稿 大重构 · 3D 戒指图标 · Grid chip 不自动置首](0024-conversation-as-draft-3d-ring-grid-chip-fix/proposal.md) |
| 0025 | done | [戒指 v3 · 草稿二次确认 AlertDialog](0025-ring-icon-retry-confirm-dialog/proposal.md) |
| 0026 | done | [图标回退平面 · 分类管理大刀（v9 schema）](0026-flat-icon-revert-category-manager/proposal.md) |
| 0027 | done | [自定义分类能装物品 · Item.category 改 String](0027-custom-categories-hold-items/proposal.md) |
| 0028 | done | [隐藏真生效 · Portal 空态 · Manager 重写](0028-visibility-filtering-drag-manager-required-avatar/proposal.md) |
| 0029 | done | [BackHandler 栈 · CategoryEditor 全屏路由 · Search 路由](0029-back-navigation-full-page-category-editor-search/proposal.md) |
| 0030 | done | [Manager 拖动重写 · v10 hero_photo_path · Search 加 visibleIds](0030-drag-fix-hidden-filter-gallery-picker-title-revert/proposal.md) |
| 0031 | done | [返回栈 · 拖动数学 · Theme 切换 · Detail 抽屉重做 · 大杂烩](0031-back-priority-drag-math-avatar-picker/proposal.md) |
| 0032 | done | 多 action 录入协议（actions[] · create/modify · v12 schema · max_tokens 4096）— 见 [`../agent.md`](../agent.md) |
| 0033 | done | 草稿影集管理 · CropScreen 基础裁剪 · Grid 长按编辑态 · v13 sort_order — 见 [`../agent.md`](../agent.md) |
| 0034 | done | **v1.0**：多图 + 语音 + 非破坏裁剪 + MODIFY 增量合并（9 个 patch v1-v9，schema v14/v15/v16）— 见 [`../agent.md`](../agent.md) |
| 0035 | done | 多 AI 服务管理（HorizontalPager + displayName）· 录入页 chip+drawer 重做 · 半屏录音 · Grid 拖动重写（父层 + auto-scroll）· IME 跟随 · JsonNull-safe parser — 见 [`../agent.md`](../agent.md) |

## 候选 / 进行中

见 [`../agent.md`](../agent.md) 末尾的 "下一刀候选"。
