# Cycle 0019 · 验收

## Grid 选择持久化

- 在 "全部" tab 点物品 → Detail → 返回 → 仍在 "全部" tab，不会自动跳到其它品类
- 同样：在任意品类 tab 点物品 → Detail → 返回 → chip 高亮的还是那个品类
- 从门厅 doorway 点 → 落到对应品类（行为不变）

## AI 聊天 fallback

- 用户向 AI 发 "你好" 等无物品信息的话：
  - 模型回普通文字 → UI 显示成普通助手消息（白色气泡，italic Cormorant）
  - 不再前缀 "出错了：content had no JSON object"
- 模型确实出错（HTTP 4xx / 网络）→ 仍以 "出错了：…" 显示

## 连接状态灯

- 测试通过 → 绿灯 (`#3E8E45`)
- save() 不再清绿灯
- 改任意配置字段（preset / baseUrl / model / apiKey / temperature / thinking）→ 立即变黄灯 (`#D89B23`)
- 清掉 API key → 红灯 (`#C5392E`)

## 全屏看图滑动

- 点 Detail 影集任一缩略图 → 全屏 viewer
- 单指左/右滑 → HorizontalPager 翻到上/下张照片
- 双指 pinch → 1×–5× 缩放
- zoom > 1 时单指拖 → 平移图片
- 双击 → 1× ↔ 2.5× 切换
- 长按图空白处 → 加 callout
- 长按已有 callout dot → 改 / 删

## 分享接收

- AndroidManifest：MainActivity 加两个 intent-filter
  - `ACTION_SEND` + `text/plain` → 京东 / 淘宝 / 浏览器分享文字落到 Treasure
  - `ACTION_VIEW` + http(s) BROWSABLE → 用户可在 "用 Treasure 打开" 菜单里选
- MainActivity 单实例 (`launchMode="singleTask"`) + `onNewIntent` 接老实例
- 文字到 `TreasureApp.shareIntake` → MainScreen 切到录入 tab → AddRoute `vm.sendText` 派给 AI → 消费后清空

## 草稿预览页（AddPreview）

- 顶部 EditPageHeader：左 [取消]（回到对话）/ 主标题 "Refine" / 副标 = 中文品类名 / 右 [确认收入] terra
- HeroAvatarPicker 显示推断的 hero（read-only，候选行不响应）
- 一行 ConfidenceLegend：● 确定 / ● 可能 / ● 需补充
- SectionDivider "基础"：品牌 / 型号 / 昵称 / 一句话 — 每行 confidence dot + label + value 或 inline TextField
- SectionDivider "其他信息"：颜色 / 入手日期 / 入手价格 / 入手渠道
- 点任一行 → inline edit；右侧出现 [确认] [取消]；按键盘 Done 也提交

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
