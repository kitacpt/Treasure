# Cycle 0012 · 验收

## Callout 编辑 / 删除

- 全屏 viewer 长按图空白处 → 弹 `CalloutEditDialog(allowDelete = false)` → 输入文字 → 保存 → 新 callout 落库
- 全屏 viewer 长按已有 callout dot（terra 圆 + paper 气泡）→ 弹 `CalloutEditDialog(allowDelete = true)`：
  - 文本框预填当前文字
  - 下方一行 terra 描边按钮 “删除这条标注 ✕”
  - 顶部 [取消] / [保存]
  - 改完保存 → 该 callout 文字更新
  - 点删除按钮 → 该条立刻从列表里被抹掉，dialog 关闭
- 删完最后一条标注后：照片在 `Item.callouts` map 里那条 path 整个被移除，不留空数组
- 底部提示文案改成 "长按图任意处加注 · 长按已有标注可改 / 删"

## 立体魔戒图标

- adaptive icon foreground 仍是 108×108 viewBox，外径 26 / 内径 20
- 环身用 evenOdd fill + 5 段 linear gradient（左上亮金 → 主金 → 右下深棕）
- 外缘有 2 条 stroke arc（亮金 highlight + 深棕 shadow），内缘同款 bevel
- 4 道暗色 hairline 撒在环身做錾刻质感
- 中心宝石用 radial gradient（亮橙→terra→深棕）+ 一圈 22% terra halo
- 顶 / 底两条 paper-coloured rune（4 顶点锯齿），左右 2 道 paper tick
- 环底椭圆 cast shadow 给悬浮感

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
- APK 仍 13 MB（图标 vector 增加约 1KB，可忽略）

## 兼容性

- Schema 不变，仍是 v7
- `callouts: Map<String, List<PhotoCallout>>` 字段语义不变；写入路径从 `addCallout(path, callout)` 改成 `setCallouts(path, list)`
- 旧已存的 callout 数据不受影响
