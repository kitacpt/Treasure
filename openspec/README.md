# openspec/ · 变更周期

灵感来自 [github.com/kryiea/drinking](https://github.com/kryiea/drinking) 的 `openspec/`。每个**变更周期（cycle）**就是一次"我们打算改/加一件事"的全过程，从提案到落地。

## 一个 cycle 的形状

```
openspec/
└── NNNN-<short-slug>/
    ├── proposal.md      # 这个 cycle 在做什么、为什么、不做什么
    ├── spec.md          # "做完"的可验收条件（测试 / 用户故事 / 截图位）
    └── notes.md         # 工作笔记，可以随便写，做完了归档
```

编号四位数字，递增。slug 用连字符。

## 流程

1. **提案**：写 `proposal.md`。和（自己/团队）对齐范围。可能要新写或改 ADR。
2. **规格**：写 `spec.md`。把"完成"的标准钉死 —— 验收条件、UI 状态、不做的事项。
3. **实施**：边做边在 `notes.md` 记笔记 —— 卡点、决定、todo
4. **收尾**：把 `proposal.md` / `spec.md` 顶部的 status 改成 `done`，agent.md 同步更新

一次只开**一个** in-flight 的 cycle。要并行做事 → 拆成两个 cycle，按编号顺序。

## 跟 ADR 的关系

- ADR 是**为什么这么定**的钉子，长期生效
- cycle 的 proposal/spec 是**这一次具体做什么**
- 一个 cycle 可能产出新的 ADR；老 ADR 被推翻 → 写新 ADR 用 `Supersedes ADR-xxxx` 标注，不悄悄改老的

## 索引

| # | 状态 | 标题 |
|---|---|---|
| 0001 | done | [MVP · Portal + Grid + Detail + Add（手动 Add 后改 stub）](0001-mvp-portal-grid-detail-add/proposal.md) |
| 0002 | done | [抽屉 + 明信片翻面 + 视觉 polish](0002-drawer-flip-polish/proposal.md) |
| 0003 | done | [真实照片 + 抽屉内嵌编辑（concise 版）](0003-photos-and-inline-edit/proposal.md) |
| 0004 | done | [录入页（手动 + AI 占位）+ Detail 全字段编辑（被否后改）](0004-add-route-and-full-edit/proposal.md) |
| 0005 | done | [AI 服务接通 + 编辑屏重做（入口移到详情左上角点）](0005-ai-and-edit-screen/proposal.md) |
| 0006 | done | [OpenAI / 自定义 provider + 参数统一（拖动选前 4）+ 编辑点移右上 + 录入外层留空](0006-providers-and-spec-unify/proposal.md) |
| 0007 | done | [录入页 v2：chat-first + 草稿预览 + 历史抽屉 + 手动入口](0007-add-page-v2/proposal.md) |

## 后续 cycle 候选（按优先级，不一定按这个顺序做）

- 0008 · **真 schema migration**（v1→v5 全部，MigrationTest，删 fallbackToDestructiveMigration）—— 已 7 次 destructive，欠债最大
- 0009 · 真 STT + 历史对话持久化（录入页两个 stub）+ 多轮对话
- 0010 · 拍照（直调相机）+ AI 生成博物馆插画
- 0011 · 全屏看图浏览器 + callout 文字标注
- 0012 · FastAPI 同步层 + 后端 admin + 真实照片云端备份
