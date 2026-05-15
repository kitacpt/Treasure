# prototype/ · Claude Design 导出原型

这一目录里的 HTML/CSS/JS 文件是 [Claude Design](https://claude.ai/design) 在 2026-05 期间为 Treasure 产出的可点击原型。**视觉规格的唯一权威**就在这些 HTML 里 —— 改视觉前先打开它们对照。

## 怎么用

浏览器开 [`project/Treasure.html`](project/Treasure.html)（双击即可），看 cycle 0001–0006 的主体设计；开 [`add-page-v2/project/Treasure.html`](add-page-v2/project/Treasure.html) 看录入页 v2（cycle 0007）。

颜色 token / 字号 / 间距 / 插画规则 / 控制岛规格都在源文件里可读 —— 不要靠截图，直接读 HTML / JSX 里的 props。

## 当前状态（cycle 0035）

- **色板 / 字体 / 控制岛 / 插画规则**：仍以原型为准
- **录入页 chatbar**：cycle 0035 重新设计（chip + drawer），跟原型已经偏离 —— 实现以 [Record.html design handoff](https://api.anthropic.com/v1/design/h/i9EiWaZrLqIPq9bSmnktGw)（已落到代码里）和 [`../docs/architecture.md`](../docs/architecture.md) 描述为准
- **Settings 多 profile pager**：cycle 0035 新加，原型不含

## 文件分布

- [`README.md`](README.md) — 这一份
- [`chats/`](chats/) — 用户与 Claude Design 助手的对话记录（"用户原本想要什么"的来源）
- [`project/`](project/) — v1 8 张画板（cycle 0001–0006）
- [`add-page-v2/`](add-page-v2/) — 录入页 v2 设计稿（cycle 0007）；同目录 `HANDOFF.md` 详述与 v1 的差异

## 这一目录不再演进

视觉规格演进改写在 [`../agent.md`](../agent.md) 和具体代码里（不会再回过头改这一目录的 HTML）。原型保留作历史快照。
