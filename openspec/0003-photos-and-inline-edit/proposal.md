# Cycle 0003 · 真实照片 + 抽屉内嵌编辑

- **状态：** 进行中
- **开始：** 2026-05-07

## 这一刀切什么

cycle 0002 的"+ 添加照片 — coming"占位 + Detail 没有改字段的入口，本轮一并做掉。

### 真实照片上传

- 用 Android 的 Photo Picker（API 26+ 兼容版）选图
- 单选，存到 app 私有目录 `filesDir/photos/<itemId>/<uuid>.jpg`
- `Item` 加 `photos: List<String>` 字段（绝对路径列表）
- Schema 升 v4，`fallbackToDestructiveMigration` 处理升级
- 用 Coil 渲染（file:// 路径 + crop scale + 自动 cache）
- 抽屉"影集" tab：3 列网格，第一格是 `+` tile（点击开 picker），其它是真实照片；长按一张照片 → 二次确认 → 删（同时删文件）
- Detail 翻面背面：≥1 张照片 → 前 3 张缩略；0 张 → 当前空状态保留
- Hero 正面 `0 PHOTOS · TAP TO FLIP` 角标 → `N PHOTOS · TAP TO FLIP`

### Detail 内嵌编辑（concise）

抽屉"设置" tab 加一段 EDIT 区，**仅 3 个字段**：

- 昵称 (text field)
- 一句话简介 (text field)
- 状态 (3 chips: Owned / Parted / Rented)

显式"保存修改"按钮，unchanged 时禁用。其它字段（brand / model / category / heroVector / acquired / specs）暂不在此处改 —— identity 类信息要改在 cycle 0004 单独做"重建物品"流程，避免简陋表单回归。

## 不在这一轮（留给后续）

- 拍照（直接调相机）—— 现在只走相册
- 多选 / 批量上传
- 全屏看图浏览器（点缩略图放大查看）
- 照片裁剪 / 滤镜
- 抽屉里"添加历史"
- 设置页 AI 服务（cycle 0004）
- 对话式录入（cycle 0004，需要 AI 先就绪）
- callout 文字标注
- 真 schema migration（仍 fallbackToDestructiveMigration）

## ADR 相关

- 落地 [ADR-0003](../../docs/adr/0003-local-first-with-optional-sync.md) 阶段 1：照片只本地，不同步
- 不动 [ADR-0004](../../docs/adr/0004-byo-ai-key.md)（推迟 cycle 0004）

## 验收标准

详见 [`spec.md`](spec.md)。
