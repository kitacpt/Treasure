# docs/ · 长期指引

这一目录里的文档是**长期生效**的，不跟随某一次 cycle 起落。短期 / 进行中的改动放在 [`../openspec/`](../openspec/)（cycle 0001-0031）或者 [`../agent.md`](../agent.md) 的 cycle 一览（0032+）。

## 给 AI agent 的推荐阅读顺序

进来读完 [`../README.md`](../README.md) 和 [`../agent.md`](../agent.md) 之后：

1. [`dev-loop.md`](dev-loop.md) —— 构建 / 装机 / vivo 调试 / 内循环 / smoke test 步骤 / adb 速查
2. [`architecture.md`](architecture.md) —— 模块划分 / 数据流 / Schema v5→v16 演化 / `:core` 与 `:app` 完整目录结构 / AI 录入流水线 / 多 profile 数据流 / 导航图
3. [`product.md`](product.md) —— Treasure 是什么、给谁、不做什么
4. [`visual-language.md`](visual-language.md) —— 配色 / 字体 / 插画规则 / 控制岛规格（视觉规格的唯一权威仍是 [`../prototype/`](../prototype/) 下的 HTML 原型）
5. [`adr/`](adr/) —— 6 份钉死的决策记录

## 文档列表

- [`product.md`](product.md) — 产品定位、用户、核心流程、不做什么
- [`visual-language.md`](visual-language.md) — 配色、字体、插画规则、控制岛规格
- [`architecture.md`](architecture.md) — 模块 / 数据流 / Schema / AI / 文件清单
- [`dev-loop.md`](dev-loop.md) — 构建 / 装机 / 内循环 / smoke test / 常见踩坑
- [`adr/`](adr/) — Architecture Decision Records

## ADR 索引

钉死的决策，长期生效。**推翻**某条 ADR → 写一份新 ADR，开头标注 `Supersedes ADR-NNNN`；老的不改。

- [0001 · Android 原生](adr/0001-android-native.md)
- [0002 · Jetpack Compose](adr/0002-jetpack-compose.md)
- [0003 · Local-first + 可选同步](adr/0003-local-first-with-optional-sync.md)
- [0004 · 用户自带 AI key (BYO Key)](adr/0004-byo-ai-key.md)
- [0005 · 博物馆插画策略](adr/0005-museum-illustration.md)
- [0006 · Schema migrations 硬规矩](adr/0006-schema-migrations.md)

## 与 openspec/ 的分工

| 维度 | docs/ | openspec/ + agent.md |
|---|---|---|
| 时效 | 长期 | 一个 cycle 的过程记录 |
| 修改频率 | 偶尔（架构 / 决策变了才动） | 每 cycle 一次（proposal → spec → notes） |
| 内容 | "整体长什么样" | "这一次具体改什么 / 改完了什么" |
| 权威性 | docs 与代码冲突 → 改 docs；ADR 与代码冲突 → 视为代码 bug | 历史记录，不再修改 |
