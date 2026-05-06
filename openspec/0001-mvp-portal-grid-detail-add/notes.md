# Cycle 0001 · 工作笔记

随时记。卡点、决定、临时 todo —— 都来这里。完成后挑一段写复盘归档。

## 待开工 (2026-05-06)

骨架就位。第一刀挑哪儿动手？

候选起手式（按建议顺序）：

1. **生成 Gradle 工程** —— `gradle init` + 改成 :app/:core 多模块
2. **Color + Type** —— 先把视觉 token 写进 Compose，光秃 `Surface` 都得是纸面色
3. **Hello Portal（不带数据）** —— 静态 hardcode 一个 Portal 屏，验证字体加载、ornament 自绘、控制岛 blur
4. **Room schema + Seed** —— 数据层站起来；用 `Log` 验证数据流
5. **PortalScreen 接 ViewModel + Repository**
6. **GridScreen / DetailScreen / AddScreen**

挑这个顺序的理由：第 3 步先于数据层 —— 视觉能不能落地是这个 cycle 风险最大的事，先排雷。

## 已知卡点 / 需要查的事

- Cormorant Garamond 的 italic 500 在 Google Fonts 上是不是真有？打包到 `res/font/` 时怎么取
- Compose Canvas 自绘 SVG 路径能不能直接吃 SVG `path d="…"` 字符串？还是要手翻
- minSdk=26 上 `EncryptedSharedPreferences` API 行为（这个是 ADR-0004 的事，cycle 0001 暂不用）

## 临时 todo

（开工后随时更新）

- [ ] 跑 `gradle init`
- [ ] 写第一份 `TreasureTheme.kt`
- [ ] 把 `prototype/project/vectors.jsx` 里 `VRacket` 翻译成 Compose Canvas，验证一根头发丝描边出来对不对
