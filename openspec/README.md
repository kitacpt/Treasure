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
| 0004 | done | [录入页（手动 + AI 占位）+ Detail 全字段编辑](0004-add-route-and-full-edit/proposal.md) |

## 后续 cycle 候选（按优先级，不一定按这个顺序做）

- 0005 · AI 服务真接通：设置页 BYO key 表单 + `core/ai/AiClient` 接口 + AnthropicClient 实现 + Add AI tab 对话流（拍照 / 文本 → vision extract → 预填表单）
- 0006 · 真 schema migration（v1→v4 全部）+ 全屏看图浏览器
- 0007 · AI 生成博物馆插画 + callout 文字标注
- 0008 · FastAPI 同步层 + 后端 admin + 真实照片云端备份
