# Cycle 0001 · MVP · Portal + Grid + Detail + 手动 Add

- **状态：** 进行中
- **开始：** 2026-05-06
- **预期完成：** TBD（首个里程碑，约 2 周一人工作量）

## 这一刀切什么

把博物馆视觉**完整搬到一个能跑的 Compose app**，并把 local-first 数据流（Room → ViewModel → Compose）打通。

具体覆盖 4 个屏：

1. **Portal** —— 仪式感门厅
2. **Grid** —— 某品类下物品列表
3. **Detail** —— 物品详情（hero 插画 + 关键参数；**没有抽屉，没有翻面**）
4. **Add / Edit** —— 朴素表单（**没有 AI，没有拍照**），从打包好的预置插画里挑一张当 hero
5. **Settings (stub)** —— 一句话占位 "AI integration coming"

## 为什么是这一刀

- 想在 Compose 里证两件事：**视觉能搬**、**数据能存能读**
- AI / 抽屉 / 翻面 / 真实照片 / 同步 全都更复杂 —— 先把骨架建好，骨架立得住再往上长
- 范围小 → 一两周完成 → 第二个 cycle 拿到工作中的 app 再雕

## 不做（明确）

- ❌ 抽屉（历史 / 参数 / 影集）
- ❌ 详情翻面、真实照片
- ❌ AI（对话录入、视觉识别、插画生成）
- ❌ Settings 实际功能（只放占位）
- ❌ 后端、同步、登录
- ❌ Tweaks 面板（运行时切配色 / 字体 / 深浅 —— 只支持系统深浅模式跟随）
- ❌ 多语言（中文 hardcoded）

## 涉及的 ADR

- 落地 [ADR-0001](../../docs/adr/0001-android-native.md) / [ADR-0002](../../docs/adr/0002-jetpack-compose.md)：起 Gradle 工程、Compose 主题
- 落地 [ADR-0003](../../docs/adr/0003-local-first-with-optional-sync.md) 阶段 1：Room schema + 种子数据 + Repository 接口（`RemoteItemSource` 留 NoOp）
- [ADR-0005](../../docs/adr/0005-museum-illustration.md) 阶段 A：预置插画（10 个 Compose Canvas 函数，按 `HeroVector` enum 分发）
- 不动 [ADR-0004](../../docs/adr/0004-byo-ai-key.md)（推迟）

## 接下来怎么做

详见 [`spec.md`](spec.md)。
