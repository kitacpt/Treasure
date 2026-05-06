# docs/ · 长期指引

这一目录里的文档是**长期生效**的，不跟随某一次 cycle 起落。短期/进行中的改动放在 [`../openspec/`](../openspec/)。

## 内容

- [`product.md`](product.md) —— Treasure 是什么、给谁、为什么这么定义
- [`visual-language.md`](visual-language.md) —— 配色、字体、插画规则、控制岛规格 —— 来自 [`../prototype/`](../prototype/)
- [`architecture.md`](architecture.md) —— 模块划分、数据流、持久化、同步、AI 抽象
- [`adr/`](adr/) —— 架构决策记录。一份 ADR = 一个钉死的"为什么这么选"

## ADR 索引

- [0001 · Android 原生](adr/0001-android-native.md)
- [0002 · Jetpack Compose](adr/0002-jetpack-compose.md)
- [0003 · Local-first + 可选同步](adr/0003-local-first-with-optional-sync.md)
- [0004 · 用户自带 AI key](adr/0004-byo-ai-key.md)
- [0005 · 博物馆插画策略](adr/0005-museum-illustration.md)

## 对外引用一份参考

仓库布局参考了 [github.com/kryiea/drinking](https://github.com/kryiea/drinking)（Yinzhi）的组织方式：`docs/` + `openspec/` + `agent.md` + ADR。技术栈不一样（Yinzhi 是 SwiftUI/iOS，Treasure 是 Compose/Android），但项目管理的形状是一样的。
