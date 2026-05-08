# prototype / add-page-v2

> 来自 Claude Design 第二轮交付，2026-05-07。**只用来重做录入页**，其它页面保留 cycle 0006 的实现。

## 与 v1 (`prototype/`) 的差异

只有 3 个文件不同：

- `chats/chat1.md` — 新增 cycle 0007 这一段对话（之前 289 行 → 现在 405 行）
- `project/direction-a.jsx` — 新版 `AddScreen` / `AddChat` / `AddPreview` / `AddManual` 函数
- `project/Treasure.html` — 画板从 8 张扩成 13 张（新增 5 个录入相关画板）

`data.jsx` / `vectors.jsx` / `design-canvas.jsx` / `android-frame.jsx` / `tweaks-panel.jsx` 与 v1 字节相同 —— 这里复制只是为了双击 `Treasure.html` 还能直接渲染。

## 录入页画板（5 张）

浏览器开 `project/Treasure.html`，看 cycle 0007 关心的这 5 张：

| 画板 | seed | 状态 |
|---|---|---|
| `录入·对话（默认）` | `addMode: 'chat'` | AI 对话首屏 |
| `录入·历史对话` | `addMode: 'chat', historyOpen: true` | 顶部右侧的历史抽屉展开 |
| `录入·语音中` | `addMode: 'chat', voiceOn: true` | 半透明蒙层 + 波形 + 转写 |
| `录入·AI 草稿预览` | `addMode: 'preview'` | 9 字段 + 置信度小圆点 + 确认按钮 |
| `录入·手动表单` | `addMode: 'manual'` | 手动 9 字段 |

## 关键交互（用户在 chat1.md 里钉死的）

1. **默认进 AI 对话页**，没有 chooser 起步页（之前的方案被否）
2. **顶部右上 3 颗按钮**：🕐 历史对话 / + 新对话 / 手动（带文字的胶囊）
3. **标题旁显示当前对话名** "Fujifilm X-T5 ▾"，点击也开历史抽屉
4. **输入框抬到控制岛上方**（`bottom: 88`），不再被遮
5. **草稿入口** 改为卡片样式：左缩略 + DRAFT №024 + "草稿已就绪" + 箭头
6. **Manual 表单的"返回"** 退回到对话页 —— 整个录入流程在 AI 对话里闭环

## Cycle 0007 实现注意

- **手动表单（AddManual）这次不重写**：用户原话 "包括录入页点进去的手动录入面板也是保留现有的现状"。所以 cycle 0007 实现 Compose 时，"手动" 按钮跳转的还是 cycle 0004-0006 一路演进下来的 `CategoryForm`，而不是这里 `AddManual` 的样子。
- **其它页面（Portal / Grid / Detail / Edit / Settings）也都保留**，cycle 0007 只动 `AddRoute` 和它内部的状态机。

## 浏览器查看

```bash
cd /home/mi/workspace/treasure/prototype/add-page-v2
python3 -m http.server 7001    # 然后开 http://localhost:7001/Treasure.html
```

或者直接双击 `Treasure.html`（相对路径加载）。
