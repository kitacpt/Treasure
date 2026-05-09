# Cycle 0019 · notes

## 文件改动

主要：

- `app/.../ui/main/MainScreen.kt` — `onCategoryChanged` 回写 gridCategoryId；shareIntake 监听 → animateScrollToPage(PAGE_ADD)
- `app/.../ui/grid/GridScreen.kt` — `GridRoute` 多 `onCategoryChanged` 回调；chip 点击同时 `vm.selectCategory` + `onCategoryChanged`
- `app/.../ui/add/AddRoute.kt` — shareIntake 监听 → `vm.sendText` + 清空
- `app/.../ui/add/AddViewModel.kt` — `onFailure` 分支识别 `ChatOnlyResponseException`，surface 为普通 Assistant 消息
- `app/.../ui/settings/SettingsViewModel.kt` — 加 `invalidateTest()`；所有 setter 调它；`save()` 不再 reset
- `app/.../ui/photo/FullscreenPhotoViewer.kt` — `transformable + lockRotationOnZoomPan = true` 替代 `detectTransformGestures`；`scale > 1` 时叠 `detectDragGestures`
- `app/.../ui/add/AddPreview.kt` — 整页重写，复用 `EditPageHeader` / `HeroAvatarPicker` / `SectionDivider`
- `app/.../MainActivity.kt` — `consumeShareIntent` + `onNewIntent`
- `app/.../TreasureApp.kt` — `shareIntake: MutableStateFlow<String?>`
- `app/src/main/AndroidManifest.xml` — MainActivity `singleTask` + 两个 intent-filter
- `core/.../ai/AiClient.kt` — 加 `ChatOnlyResponseException(text)`
- `core/.../ai/OpenAiClient.kt` / `AnthropicClient.kt` — parseDraft 改抛 ChatOnlyResponseException

## 设计取舍

### Grid 选择回写 vs 单 source

之前我把 chip 状态藏在 GridViewModel 里、把 nav-level 的 gridCategoryId 当 hint。两个状态不同步，从 Detail 返回 Grid 重组时 hint 覆盖了 vm。

修法是显式让两个状态同步：chip 点击同时 dispatch 给 vm 并 callback 给 MainScreen 写回 gridCategoryId。Single source of truth 在 MainScreen，vm 是 derived state。

### ChatOnlyResponseException

之前所有 parse 失败都抛 IllegalStateException + ".message" 拼成 "出错了：…" 给用户看。但模型不调 tool 而走聊天回复，本质不是错误 — 是用户没提物品信息。

新增专门的 ChatOnlyResponseException 携带 text，VM 接到它直接当对话消息插进流。这让录入页也能闲聊，不必每句都强行让 AI 出 draft。

### 状态灯 invalidate 时机

cycle 0018 用 `save()` 一律 reset 是因为 "保存意味着配置变了"。但 90% 情况下用户先测试再保存，结果一保存就 reset，绿灯永远见不到。

新版：reset 不在保存时，而在编辑时。setter 一被调用就 invalidate。意思变成 "你只要碰了配置，灯就回黄；测试是给你刷绿灯的唯一动作"。逻辑清晰，状态可解释。

### transformable vs detectTransformGestures

`detectTransformGestures` 一上来就 `awaitTouchSlopOrCancellation`，单指 drag 也被吃掉。结果 HorizontalPager 收不到 swipe。

`Modifier.transformable(state, lockRotationOnZoomPan = true)` 只接管 *多指 + zoom* 类的 gesture，单指 drag 透传。zoom > 1 时再单独挂 `detectDragGestures` 处理放大后的平移。两种 gesture 互不干扰。

### Share intent 的实际效果

京东 / 淘宝的 "分享" 一般出 ACTION_SEND 带一段文字（可能是 "https://item.jd.com/12345.html【商品名】..."）。Treasure 收到后直接喂给 AI。AI **不会真 fetch URL**（设备直连 provider，没有 web 抓取层），它能做的是：

- 从文字里的商品名 / 描述提取 brand / model / 一句话
- 如果 AI 见过这个 URL 模式（训练数据里），可能记得对应商品
- 否则基于 URL 串的关键词（如 "iphone-16-pro"）猜

效果不会比手动粘贴文字好太多，但省了用户的几次切屏。

### 真正的 web fetch 留给 cycle 0020+

如果要从 URL 实打实拉商品页 → 解析 → 喂 AI，我们需要：

- 一个 HTTP fetch 层（OkHttp 已有）
- 京东 / 淘宝可能有反爬 / 登录墙，需特殊处理
- HTML → 文本提取
- 大概率还要加移动 UA 头

工作量大、稳定性差，先不做。

### AddPreview 视觉一致

之前 AddPreview 自家一套：HeroCard + Footer + 自家 PreviewHeader。视觉跟 Edit / 手动录入不一样。本刀复用 EditPageHeader / HeroAvatarPicker / SectionDivider，inline-edit 行做成 LabeledField 同款下划线 + confidence dot 在前。功能不变（per-field 编辑），样式统一。

confidence dot 留在每行最前面 — 它是 AI 草稿独有的信息（哪些字段 AI 不确定），Edit 页没有，但 visually integrated。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测

1. 图鉴页 → "全部" → 点物品 → Detail → 返回 → 仍是 "全部"
2. AI 录入页输入 "你好" → 出现普通 italic 助手回复，不再前缀 "出错了"
3. Settings → 调整 → 测试连接通过（变绿）→ 关抽屉 → 摘要卡是绿灯
4. 同上但保存后 → 仍是绿灯
5. 摘要卡开抽屉 → 改一下 model 字段一个字符 → 黄灯立即出现
6. Detail 影集 → 点任一缩略图 → 全屏 viewer → 单指左右滑切换照片 → ✓
7. 京东打开任一商品 → 系统分享 → "Treasure" 出现在分享列表 → 选中 → app 自动落到录入页 + 文字喂给 AI
8. AI 出 draft → 草稿预览页样式：顶部 EditPageHeader / 头像选择器 / "基础" / "其他信息" 两段 / inline edit
