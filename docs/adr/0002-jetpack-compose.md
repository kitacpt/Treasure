# ADR-0002 · Jetpack Compose（自定义主题）

- **状态：** Accepted
- **日期：** 2026-05-06

## 背景

[ADR-0001](0001-android-native.md) 已经定了 Kotlin 原生。Android 原生上 UI 工具链有两个：传统 Views/XML 和 Jetpack Compose。

## 决定

**用 Jetpack Compose**。Material3 只作为 baseline 引入，在 `TreasureTheme` 里整体覆盖配色和排版（参见 [`../visual-language.md`](../visual-language.md)）。

导航：`androidx.navigation.compose`。状态：`ViewModel + StateFlow`，不上任何重型状态管理框架。

## 理由

1. **声明式映射 JSX 原型**：原型是 React，UI 树是声明式。Compose 的 `@Composable` 函数和原型的 React 组件是一一对应关系，从 `direction-a.jsx::Portal` 翻译到 `PortalScreen.kt` 是平铺直叙的事。
2. **重度自定义友好**：博物馆视觉里到处是 0.5px 描边、自定义色 token、矢量 hero 图。Compose 的 `Modifier.drawBehind` / `Canvas` 对自绘开放；MaterialTheme 也允许整个 `Typography` / `ColorScheme` 替换。
3. **官方默认**：2026 年 Compose 是 Android 官方推荐的新项目首选。

## 实现守则

- **不用 Material3 的默认排版/色号**。`TreasureTheme` 自己定义 `paper / ink / terra / card / sub / line` 等 token（[`../visual-language.md`](../visual-language.md)）。
- 不引第三方设计系统库（不要 Accompanist 之外的 UI kit）。
- 字体打包进 `res/font/`：Cormorant Garamond（含 italic 500）、Space Grotesk、JetBrains Mono、Noto Sans/Serif SC。**不要靠系统字体回退** —— 视觉一致性是产品差异点。
- 矢量插画用 `Canvas` 自绘（`drawLine` / `drawCircle` / `drawRect`）而不是 VectorDrawable XML，因为原型本来就是参数化（按物品 palette 调色）的。

## 相关

- [ADR-0001 · Android 原生](0001-android-native.md)
- [`../visual-language.md`](../visual-language.md)
