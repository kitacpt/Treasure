# Cycle 0012 · notes

## 文件改动一览

主要修改：

- `app/src/main/res/drawable/ic_launcher_foreground.xml` — 整图重画，加 `xmlns:aapt` 命名空间 + `aapt:attr` 嵌 gradient
- `app/.../ui/photo/FullscreenPhotoViewer.kt` —
  - 回调签名从 `onSaveCallout(path, callout)` 改成 `onSetCallouts(path, list)`
  - 加 `editPending` state + `PendingEdit(path, index, callout)` data class
  - `CalloutPin` 接 `onLongPress` 闭包
  - 原 `CalloutInputDialog` 重命名为 `CalloutEditDialog`，多了 `allowDelete: Boolean` 参数 + 红色删除按钮 row
- `app/.../ui/detail/DetailViewModel.kt` — `addCallout` 替换为 `setCallouts(path, list)`，list 空时 `callouts - path`
- `app/.../ui/detail/DetailScreen.kt` — DetailRoute 传 `vm::setCallouts`；`onAddCallout` 参数改名 `onSetCallouts`，签名换成 `(String, List<PhotoCallout>) -> Unit`

## 设计取舍

### setCallouts vs add/update/delete 三个独立回调

最初想拆成 `onAdd / onUpdate / onDelete` 三个函数，跟 history dialog 那边类似。试了一下发现 viewer 内部已经有完整 callouts list 在手，组装新 list 比传 index 出去更直接 —— UI 层算好新 list，VM 一次写回，数据流单向。

代价：每次改 1 条都要重写整张 list 进 Room。但 callout 单条很小（一个 PhotoCallout < 50 字节），一张照片几十条已经很多，整体 JSON < 1KB，重写无所谓。

### 长按 dot vs 点击 dot 弹菜单

考虑过：

- 单击 dot → 高亮 + 弹菜单 — 容易误触（手指划过 photo 时手抖）
- 长按 dot — 跟 “长按图加注” 形成一致的 “长按 = 编辑/管理” 习惯

选了后者。代价：用户得发现这个交互。底部 hint 文案明确告诉了 “长按已有标注可改 / 删”。

### gradient 模拟立体感

vector drawable 不能做真正的 PBR / 法线贴图，只能假装：

- linear gradient 左上→右下 假装顶左光源
- 内/外缘 stroke arc 分上半和下半，做出 “顶部反光、底部阴影” 的环面 bevel
- 中心宝石 radial gradient 假装球面
- 环底椭圆 cast shadow 假装悬浮

效果在 launcher 各种 mask（圆形 / squircle / 仿苹果 squircle）里都能成立，因为环身本身在 safe zone (radius 36) 内、外径 26 + cast shadow 在 28..80 范围。adaptive crop 18dp 之外的地方都是 paper-color background，cast shadow 落在 paper 上仍可见。

### 颜色挑选

用 hex 精确写出 5 段：

- `#F3DD8D` 亮金（highlight peak）
- `#D8B05F` 主金 (bright cheek)
- `#A47836` 主金 (mid)
- `#5C3F12` 暗金 (shadow start)
- `#2C1D08` 深棕 (deep shadow)

这是“蜂蜜→焦糖→巧克力”的暖色串，不是冷金 / 银色 — 跟 LotR 的 “simple gold band, no jewels” 视觉对得上（除了我们留了一颗 terra 中心点向 “这是 Treasure 不是 ring 本身” 致敬）。

### 已知限制

- vector drawable gradient 在某些 OEM 启动器上栅格化精度差，可能看上去有色带（banding）；如果有用户反馈再考虑导出 mipmap PNG 兜底
- 中心宝石 radial gradient 中心点 (53.4, 53.4) 比几何中心略偏左上，刻意让 highlight 更明显；像素级看可能略不对称

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk` (13 MB)

### 手测要点

- 装机后 launcher 上看图标：金色 ring 应有明显 “顶亮底暗” 的立体感 + 中心暖红宝石带高光 + 微弱的 cast shadow
- Detail → 抽屉 影集 → 点照片 → 长按图空白处：弹 “加一条标注” dialog（无删除按钮）
- 长按已有 callout dot：弹 “改 / 删一条标注” dialog，文字预填 + 红色删除按钮可见
- 改完保存 → 退出 viewer 再进，标注文字更新
- 点删除 → dialog 关闭，标注从图上消失；删完所有标注后再次打开 viewer 应仍正常工作
