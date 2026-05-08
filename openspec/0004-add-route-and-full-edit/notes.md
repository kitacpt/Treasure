# Cycle 0004 · 工作笔记

## 设计决策

- **气泡浮动**：`rememberInfiniteTransition` + 4 个不同周期的 `tween` (FastOutSlowInEasing) + `Reverse`，`graphicsLayer.translationY`。错峰避免节拍化。
- **品类模板写死在 `CategoryTemplates`**：4 个 hero spec 标签 / heroVector / palette 预填。这是产品决策（不让用户在录入时面对空白表格），不是临时偷懒。
- **手动表单字段限定**：只暴露品牌 / 型号 / 昵称 / 购入 / 一句话 / 状态 + 4 hero specs。`specs`、`history`、`photos` 留空，用户进 Detail 后再补。理由：录入流要快，不能让用户面对一屏 N 个空字段。
- **AI 录入是 stub**：架子先搭，让"AI 录入"在 UI 上有真实存在感（不只是文字"coming"）。等 cycle 0005 真 AI 接通时填进去。
- **Detail 4 tabs 顺序**：用户指定 `基础 / 参数 / 历史 / 影集`。"设置"消失，删除按钮下沉到基础 tab 的 DANGER ZONE（视觉上 + 心理上都和编辑动作隔开）。
- **统一的 `update(item: Item)`** 替代每字段 setter：每个 tab build 自己的 Item 副本，传去。VM 不知道改了什么字段。简洁，并且未来加字段不用动 VM。
- **HeroVector 全部以 chip 列出**：14 个 enum 值在基础 tab 用 4 列 chip 铺开。视觉上不那么精致，但 14 个用 horizontal scroll 反而难找。等 cycle 0005 polish 改成图标 grid。

## 坑

- **`InfiniteTransition.animateFloat` import**：要 `androidx.compose.animation.core.animateFloat`，不是 `animateFloatAsState`。两个名字像，不一样。
- **`Modifier.size(Dp)` import**：从 `androidx.compose.foundation.layout.size` 来，加规格 specs 时缺这个 import。
- **`mutableStateListOf` 双向绑定**：`Pair<String, String>` 这种 immutable 元素，要替换整个元素 (`specRows[i] = newPair`)，不能改字段——Pair 没有 var。OK。
- **`detectTapGestures` 同时支持 onTap + onLongPress**：替代 `combinedClickable`，更直接。
- **历史 dialog 的 chips 5 个一行**：`HistoryKind.entries.forEach { FormChip(...) }` 在 narrow dialog 里会超宽。以后可能要换成 horizontalScroll，目前 vivo X200 屏宽够用。
- **空 list 时 `mutableStateListOf<Pair<String,String>>()` 的初始化**：需要 `apply { addAll(item.specs.toList()) }`。

## 给下一个 agent

- cycle 0005 要做的两件事按优先级：
  1. **AI 服务接通**：真写 `core/ai/AiClient.kt` interface + AnthropicClient 实现；设置页提供 BYO key 表单（EncryptedSharedPreferences）；Add 的 AI tab 接通对话流（拍照 / 文本 → vision extract → ItemDraft → 跳已预填的手动表单 review → 保存）
  2. **真 schema migration**：cycle 0001-0004 全 destructive，cycle 0005 起停手；把 v1 → v4 的 migration object 都写出来，添加 MigrationTest
- 模板系统可扩展：用户应能自定义品类（cycle 0006+）
- HeroIllustration grid picker（替代 chip）应该能复用 Add 的 hero 预览 widget
