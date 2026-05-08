# Cycle 0004 · 录入页 + Detail 全字段编辑

- **状态：** done
- **完成：** 2026-05-07

## 这一刀切什么

cycle 0003 留的两个尾巴：

- 录入页是 stub，没真正接通
- Detail 编辑只 3 个字段（昵称 / 一句话 / 状态），太薄

本轮一并补齐。

## 录入页

替换 `AddStubScreen`。两种录入并存，顶部 chips 切换：

- **手动录入**：4 个气泡（badminton / photo / cars / tech），错峰浮动（infinite transition + tween，2.2-2.9s 一个周期）。点气泡 → ModalBottomSheet 弹品类模板表单。
  - 每个品类有自己的 `CategoryTemplate`：`heroVector` 默认值、4 个 hero spec 标签预填、品类调色板默认。羽毛球：重量/平衡点/中杆/握把；摄影：传感器/像素/防抖/快门；汽车：动力/马力/0-100/驱动；电子：CPU/内存/存储/屏幕。
  - 表单字段：品牌 / 型号 / 昵称 / 购入日期 / 一句话 / 状态 chips / 4 行 hero specs（标签固定，值用户填）
  - 顶部预览：HeroIllustration 用模板的 heroVector + palette
  - 保存 → 写 Room（自动生成"购入"事件 + photos = 空 + specs = 空）→ 跳到新 Detail
- **AI 录入**：当前是占位。气泡聊天的视觉骨架（一个白底 / 一个 ink 底气泡），下面写明 "AI 录入 — coming · 需要先在「设置」里配置 API key"，提供"去设置"按钮。

## Detail 编辑（全字段）

抽屉 4 tabs 重洗：之前 `历史 / 参数 / 影集 / 设置` → 现在 `基础 / 参数 / 历史 / 影集`（按用户要求的顺序，"设置"消失）。

- **基础 tab**：完整编辑 + DANGER ZONE
  - 文本：品牌 / 型号 / 昵称 / 一句话 / 购入日期 / 出手日期
  - chips：状态（3）/ 品类（4）
  - heroVector：14 个 enum 全部以 chip 列出（每行 4 个，3-4 行铺开）
  - 保存修改按钮（dirty 启用）
  - DANGER ZONE：删除（保留二次确认 AlertDialog）
- **参数 tab**：完整 hero specs + specs map
  - 4 行 hero specs（标签 + 值都可改）
  - specs map：每行 key+value+`−` 删除按钮；底部 `+ 加一行` 按钮
  - 保存修改按钮
- **历史 tab**：增删改全有
  - 顶部 `+ 加一条历史` 按钮
  - 列表保留时间轴样式（kind 字形 + 圆角 badge + 连接线）
  - 点一行 → AlertDialog 编辑（日期 / kind chips / 标题 / 备注）
  - 长按一行 → AlertDialog 删除二次确认
  - 保存时自动按日期排序
  - 提示行 "tap 编辑 · 长按删除"
- **影集 tab**：保持 cycle 0003 实现

DetailViewModel 暴露统一的 `update(item: Item)`，每个 tab 自己 build 改动后的 Item 副本调它。比之前每字段一个 setter 简洁。

## 不在这一轮（cycle 0005+）

- AI 录入实际接通（需要 [ADR-0004](../../docs/adr/0004-byo-ai-key.md) 的 AiClient 实现）
- 设置页 AI 服务（包含上一项前置）
- DatePicker UI（仍纯文本）
- callout 文字标注 / 全屏看图浏览器 / 真 schema migration

## ADR 相关

- 不动 [ADR-0004](../../docs/adr/0004-byo-ai-key.md)（推迟）
- 不动 [ADR-0003](../../docs/adr/0003-local-first-with-optional-sync.md)（仍 destructive）

## 验收

详见 [`spec.md`](spec.md)。
