# Cycle 0025 · 戒指图标重画 · 确认收入二次确认

- **状态：** done
- **完成：** 2026-05-11

## 用户反馈 2 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 3D 戒指看着有点怪，不像个戒指 | 重画 [`ic_launcher_foreground.xml`](android/app/src/main/res/drawable/ic_launcher_foreground.xml)。诊断 cycle 0024 那版"怪"的几个可能因子：(a) 椭圆 ry/rx=0.58 太接近圆，俯视感不足；(b) "前侧壁渐变带" 单独画成第二个 path，导致看着像"几个圈叠在一起"而不是一枚戒指；(c) 顶面 rune 雕刻在 launcher 那么小看不清反而像污点。Cycle 0025 修：把椭圆压扁到 ry/rx=0.36（28×10 外圈 + 13×4 内孔，强烈俯视感），删掉前侧壁第二个 path，主体改回单一 evenOdd 椭圆环，渐变改纯垂直顶亮底暗（光从天而降照在凸面金属上的最直读法），rune 去掉。装饰物只留 4 条 stroke：外圈左上高光、外圈右下暗弧、内孔顶半暗（远端内壁）、内孔底半亮（近端内壁朝天反光） |
| 2 | 草稿页的"确认收入"需要二次确认，容易误触 | `AddPreview` 的"确认收入"点击不再直接调 `onConfirm(status)`；先把 `confirming=true`，弹一个 `AlertDialog` 标题 "收入 {Brand Model}？" 文案 "确认后会作为一件 {品类} 入图鉴，再改就要从图鉴里点进去编辑。"，[收入] [取消] 两个按钮。这个 dialog 沿用 `SettingsScreen` 重置设置同款样式（cycle 0017 起的 paper 背景 + terra 文字 + sub-text 描述） |

## 不在这一刀

- 完全重建 3D 戒指图标（如真渲染一个 torus PNG 包进 res/ — 太重）
- "确认收入" 之外其它草稿页操作的二次确认（删除提案 / 删除参数行等）
- cycle 0024 已记的下一刀候选（死代码清理 / 撤销采用 / WebView headless / etc）

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
