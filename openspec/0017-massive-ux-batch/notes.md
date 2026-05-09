# Cycle 0017 · notes

## 文件改动

主要修改：

- `core/.../ai/OpenAiClient.kt` / `AnthropicClient.kt` — `defaultHttpClient(callTimeoutSec)` 参数化，thinking 模式走 360s
- `app/.../ui/portal/PortalScreen.kt` — Tally 去 OWNED
- `app/.../ui/grid/GridViewModel.kt` — `currentCategory` 变 nullable；`selectCategory(Category?)`；ALL_FILTER_ID sentinel
- `app/.../ui/grid/GridScreen.kt` — LazyRow CategoryChips + AllChip + LaunchedEffect auto-scroll；EmptyHint 适配 null
- `app/.../ui/components/HeroAvatarPicker.kt` — 加 `onTakePhoto` / `onPickPhotos` / `onRemovePhoto` 回调；展开区动作 chip 行；photo 长按删除确认
- `app/.../ui/edit/EditScreen.kt` — subtitle 去前缀；删 "时间" Section；删 "实拍" Section + 整个 PhotoSection / PhotoActionButton；launchers 上提到 EditScreen body；HeroAvatarPicker 接全套回调；ReorderableSpecs 拖动期间 HeroDivider → HeroDividerSpacer
- `app/.../ui/add/CategoryForm.kt` — subtitle = 中文品类名；删 tagline；删 "时间" / 独立 "状态" Section；新 "标签" Section 横排状态 chips
- `app/.../ui/add/AddRoute.kt` — `ManualCategoryPicker` 删标题副标；删 voice flow 全部（state / VoiceCapture / import）
- `app/.../ui/add/AddChat.kt` — 删 ChatHeader 的 ⊕ 按钮 + onTapNewChat 参数；Composer 删 MicGlyph + onStartVoice；HistoryDropdown 改左侧全高抽屉，状态 / 导航栏 padding；HistoryRow 显示 "New entry · HH:MM"；onPick / onNewChat 不再自动收
- `app/.../ui/add/AddViewModel.kt` — `FakeConversation.time` 字段；`formatTime` helper
- `app/.../ui/settings/SettingsScreen.kt` — DangerZone 改 "重置设置" + AlertDialog 二次确认

## 设计取舍

### 头像 + 影集合二为一

之前 Edit 屏有头像选择器（顶部）+ 实拍 Section（中下）两个独立模块。用户反馈 "头像处直接管理影集"。融合后：

- 闭合状态：112dp 圆形头像（照片 OR 线描，看 `avatarPhotoPath` 是否非空）
- 展开状态（点头像）：📷 / + 动作 chip 行 + 影集照片圆（tap=换头像 / long-press=删除）+ 0.5dp 竖分隔 + 品类线描圆

整页少一段 Section，视觉密度更舒服；用户对 "影集" 的认知和 "头像源" 一致 — 你可能拍的照片 = 你可能用的头像 = 你的影集。

代价：一个组件管多种交互，Picker 的代码膨胀（约 200 行）。但放在一处比拆两段更易维护。

### 时间 Section 删除

`Item.acquired` / `parted` 字段保留，仍参与 commit。仅 UI 不再有专门的输入框。"购入" / "出手" 时间由历史事件 (`HistoryEvent` with kind=ACQUIRED / PARTED) 表达，跟 cycle 0007 的设计意图回归一致 — 历史是时间的真相。

如果用户硬要直接改顶层 `acquired`，目前只能通过历史事件间接触发（add ACQUIRED event）。后续如有反馈，再考虑给历史 dialog 一个 "影响顶层时间" 的开关。

### Spec 拖动跨分割线

原代码假设所有行高度都是 `ROW_HEIGHT`，"make-room" 平移用 `rowHeightPx` 算偏移。但 `HeroDivider` 是异型 Composable（高度 ≈ 16.5dp，跟 56dp 行高对不上），跨过分割线的相邻行偏移不到位 + 拖动行又有 `zIndex(1f)` 把分割线盖住。

最便宜的修法：拖动期间用同高 Spacer 替换 HeroDivider —— 行高假设重新成立，拖完再恢复分割线。视觉上相当于 "拖的时候分割线让路"，符合直觉。

### 历史抽屉

之前是 top-right 280-320dp × 520dp 小卡片，用户称之为 "弹窗"。新版：左侧 280-320dp 全高 panel，明显更 "drawer" 感。点 scrim 仍能关，对话行 / 新对话点击不再自动 onDismiss —— 让用户可以连点几下试不同对话不打断。

`statusBarsPadding + navigationBarsPadding` 让抽屉在状态栏和导航栏之间安全展示，不被遮。

`shadow(16.dp, clip = false)` 给个看得见的边缘。

### New entry 后缀

UI 层后缀（display-time），不入库。如果 title 已经被 AI 改名成 "Brand Model"，就不加后缀。多段同日 "New entry" 的辨识度从 "全是今天" 升级到 "新对话 · 15:32 / 16:08 / 18:21"。

### Voice 暂时去掉

`com.treasure.voice.VoiceCapture` 文件保留（云端 STT 上来要复用 / 取代），import 全部摘除。`AddViewModel.sendVoice` 方法保留，没人调用。等 cycle 0018 接 Whisper 时复活。

### Kimi 360s 超时

之前 callTimeout = 120s 是经典生成模型的合理值；reasoning 模型（k2-thinking-preview / k2-0905-preview / o1）思考链长，可能 1-3 min 才出第一个 token。简单按状态切档比把所有模型都拉到 360s 风险小（普通模型继续 120s 快速失败）。

如果 360s 还不够，可能是用户网络问题，UI 层会显示 "× 网络 · SocketTimeoutException"，区分起来更容易。

### 没真 curl 验证 Kimi key

harness 阻止了我把用户提供的 sk- 凭据外发到 api.moonshot.cn。这是对的：transcript 会保留 key，外发 = 泄露。即便用户主动给，也不应做。已经在 proposal 里强烈建议用户重置那把 key。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测要点

1. Portal Tally 只剩 items / rooms 两列
2. 进 Grid 后 chips 第一个是 "全部"，从门厅过来的 chip 自动滚到当前位置
3. 录入页头部右侧只有 🕐 + [手动]，没有 ⊕
4. 点 🕐 → 左侧抽屉滑出，全高
5. 多个 "New entry" 显示成 "New entry · HH:MM"
6. 点抽屉里某段 / 点新对话 → 抽屉不关，可继续操作；点 scrim 才关
7. 点 [手动] → 弹层只有 6 行品类，没有上方标题
8. 选品类 → CategoryForm 顶部 subtitle 是中文品类名；sections 跟 Edit 一样：基础 / 标签 / 参数
9. Composer 没有麦克风按钮
10. Edit 页 subtitle 不带 "EDIT ·"；头像点开有 📷 / + 两个 chip 行；影集照片可 tap 换头像、长按删除
11. Edit 页没有 "时间" / "实拍" 两个独立 Section
12. 拖动 spec 跨过 hero/tail 分割线，分割线让路（暂时消失），不再被覆盖
13. Settings DANGER ZONE 文案 "重置设置"，点行弹 AlertDialog
14. Kimi · Moonshot + 模型 `kimi-k2-0905-preview`：测试连接给到的时间预算从 120s 涨到 360s
