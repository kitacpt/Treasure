# ADR-0001 · Android 原生（Kotlin）

- **状态：** Accepted
- **日期：** 2026-05-06
- **决定人：** 项目所有者

## 背景

Treasure 的交互设计稿（[`../../prototype/project/Treasure.html`](../../prototype/project/Treasure.html)）从设计阶段就明确写在 Android frame 里。设计对话（[`../../prototype/chats/chat1.md`](../../prototype/chats/chat1.md)）开头就把 platform 钉成 Android。

## 决定

**用 Kotlin 写原生 Android app**（minSdk 26，targetSdk 最新稳定版）。不做 iOS 版，不做 web 版。

## 候选方案与拒绝原因

| 方案 | 拒绝原因 |
|---|---|
| **Flutter** | 跨端能力溢出：当前用不到 iOS。博物馆这种重度自定义视觉（自绘矢量、纸面色 + 衬线 + 罗马数字标注）在 Compose 比 Flutter Widget 顺手；Compose Canvas 直接对应原型里的 SVG。 |
| **React Native** | JSX 原型迁移路径短，但 RN 在重度自定义视觉/动效上不如原生；社区方向也在收缩。 |
| **保留 Web 原型** | 原型是 desktop browser 里的 design canvas，不是手机 web app；要做成生产 web 还得改一版响应式 + 路由 + 持久化。投入相当，但用户拿到手的是个网页（不能装、不能离线、不能拍照接 AI）—— 与"私人收藏柜"的定位不符。 |

## 结果

- 仓库 `android/` 下是 Gradle 工程
- 模块拆分 `:app` + `:core`（见 [ADR-0002](0002-jetpack-compose.md)）
- 不为跨端额外抽象 —— 如果将来真要 iOS 版，那时再开 KMP 或 SwiftUI 平行实现

## 相关

- [ADR-0002 · Jetpack Compose](0002-jetpack-compose.md)
- 参考布局：[github.com/kryiea/drinking](https://github.com/kryiea/drinking) 的 `ios/`（SwiftUI），思路类似（声明式 + 原生重度定制）
