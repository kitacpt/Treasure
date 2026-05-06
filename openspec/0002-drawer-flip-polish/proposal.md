# Cycle 0002 · 抽屉 + 翻面 + 视觉 polish

- **状态：** done
- **完成：** 2026-05-06

## 这一刀切什么

cycle 0001 把骨架立起来了，cycle 0002 把"详情页深一层"补上，并修了 cycle 0001 暴露的三处用户反馈。

## 三处反馈修复

1. **Add 屏改为 stub**：手工填表没意义（没图片上传、没 AI 识别），改成跟 Settings 一样的 "coming"。删 `AddViewModel` / `AddScreen`，Detail 顶部的 edit 按钮也一起去掉。
2. **顶部 status bar 留白**：之前内容和系统栏冲突。改用 edge-to-edge 模式（`enableEdgeToEdge()`），每个屏自己 `statusBarsPadding()`；控制岛 `navigationBarsPadding()`。
3. **页面转场改为左右推**：默认 fade 闪烁。`NavHost` 设置 `slideIntoContainer(Start)` 入 + `slideOutOfContainer(Start)` 出（前进），`End` 方向用于回退；300ms tween。

## cycle 0002 主体

### 抽屉（历史 / 参数 / 影集）

- `BottomSheetScaffold` 半隐藏，96dp peek 显示拖拽柄 + tabs；上滑展开
- 3 tabs：
  - **历史**：垂直时间轴；每条事件有 kind glyph（`+ ★ ↻ Δ −`）+ 日期 + 标题 + 备注
  - **参数**：key-value 行
  - **影集**：3 列网格，6 个空灰格 + "添加照片 — coming"

### 明信片翻面

- Detail 的 hero 卡片点击 → 600ms `rotationY` 翻转
- 正面：博物馆线描 + "0 PHOTOS · TAP TO FLIP" 小角标
- 背面：纸面色，"暂无实拍" + 物品名 + "添加照片 — coming" 胶囊按钮

### Schema v3

- `Item` 加 `history: List<HistoryEvent>` 字段
- `HistoryEvent(date, kind, title, note)`，`HistoryKind` enum：ACQUIRED / MILESTONE / MAINTAIN / MOD / PARTED
- 列名 `history_json`，序列化用 kotlinx-serialization
- `fallbackToDestructiveMigration()` 处理 v2 → v3
- 8 条种子物品全都补了真实历史事件（移植自 `prototype/project/data.jsx`）

## 不做（cycle 0003+）

- 真实照片上传：相机 / 相册选择 / 本地文件存储
- AI：设置页接通 + 对话录入 + 插画生成
- 后端同步
- 抽屉里"添加历史"功能（cycle 0003 跟 AI 一起做）
- callout 引线 + 罗马数字标注（在 Compose Canvas 里加文字标注）
