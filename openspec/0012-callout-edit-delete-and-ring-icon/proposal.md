# Cycle 0012 · Callout 编辑 / 删除 + 立体魔戒图标

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 2 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | Callout 只能加，不能改 / 删 | `FullscreenPhotoViewer` 的回调从 `onSaveCallout(path, PhotoCallout)` 改成 `onSetCallouts(path, List<PhotoCallout>)`：UI 层算好新列表 → 一次性写回。CalloutPin 自带 `pointerInput.detectTapGestures(onLongPress)`，长按 dot → 弹 `CalloutEditDialog(allowDelete = true)`，文本框预填 + 红色 "删除这条标注" 按钮；长按图空白处仍走原来的 add 流程（同一个 dialog 但 `allowDelete = false`）。`DetailViewModel` 加 `setCallouts(path, list)`，list 为空时把这条 path 整个从 map 抹掉避免空数组留库 |
| 2 | 当前图标不够像戒指、要立体一点 | `ic_launcher_foreground.xml` 重画：annulus path（外径 26 / 内径 20）+ evenOdd fill 镂空中孔；填色用 5 段 linear gradient（左上亮金 #F3DD8D → 主金 #B78648 → 右下深棕 #2C1D08）模拟金属反光；外缘双弧（左上亮金 highlight + 右下深棕 shadow）+ 内缘双弧 bevel，给环面带出 “顶亮底暗” 的立体光照；环身上加 4 道 0.25dp 暗色 hairline 当錾刻细节；中心 terra 宝石升级为 radial gradient（亮橙心 → terra → 深棕）+ 一圈 22% terra 光晕；环底加一条椭圆灰色阴影暗示 “戒指悬浮在纸面上”；顶 / 底 paper-coloured 锯齿 rune 重新对齐到新的内/外径，加宽到 4 顶点更像精灵铭文；side ticks 重置到 (30, 78) |

## 数据迁移

无 schema 变化。callouts 存储格式（cycle 0010 的 `Map<path, List<PhotoCallout>>`）原地兼容。

## 视觉细节

- gradient 起止点 (32, 28) → (78, 80)，对角线方向 ~225°，跟假想顶左光源一致
- 中心宝石半径从 2.4 涨到 2.6，外加 22% 透明 terra 光晕（半径 3.4）；radial gradient 中心点 (53.4, 53.4)、半径 3.0，刚好让宝石顶端有一小撮暖白高光
- 环底 cast shadow：22dp × 3.6dp 椭圆，10% 黑，y=82（adaptive crop 之外，但落到 paper background 上一道淡灰）

## 不在这一刀

- AI 生成博物馆插画
- 云端 STT 兜底
- 多轮 refine 的 image 上下文
- Settings preset (Xiaomi MiLM) URL 校准
- MigrationTest CI 接入

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
