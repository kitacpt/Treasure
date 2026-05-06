# ADR-0005 · 博物馆插画策略

- **状态：** Accepted（cycle 0001 阶段策略；AI 生成推迟）
- **日期：** 2026-05-06

## 背景

Treasure 视觉的差异点是**统一风格**的博物馆线描插画。这件事和"用户能录入任意物品"天然冲突 —— 用户加一个新东西，从哪儿来一张匹配风格的图？

原型对话里曾尝试过像素风（被否），最终落到："19 世纪百科全书的版画 —— 细线 + 单色淡彩 + 罗马数字标注线"（[`../visual-language.md`](../visual-language.md) 已记录视觉规则）。

## 决定（分两阶段）

### 阶段 A · cycle 0001 + 短期内

- **种子物品**：参照原型 `vectors.jsx`，预先在 Compose 里写好若干个 `@Composable HeroIllustration_<kind>(palette)` 函数，按品类/物品 kind 区分（`racket / camera / lens / car / tripod / laptop / earbuds / kindle / watch / tablet`）
- **用户新增**：录入表单里要求用户从这些预置插画里**挑一张作 hero**，并选一个 4 色 palette（默认从物品类别来）
- 没有"用户上传 SVG"功能 —— 那会破坏统一风格
- 真实照片**不**作 hero，cycle 0002 才在翻面/影集里出现

### 阶段 B · AI cycle 之后

- 用户填完物品基本信息 → 设备上调用配置好的 AI（[ADR-0004](0004-byo-ai-key.md)）→ 生成符合 [`../visual-language.md`](../visual-language.md) 规则的 SVG/PNG
- 生成结果先 cache 到本地（`files/illustrations/<item_id>.svg`）
- 失败 / 没配 AI → 退回到阶段 A 的预置插画
- prompt 模板放在 `core/ai/IllustrationPrompt.kt`，模板内容是博物馆风格的描述 + 物品参数 + palette

### 阶段 B 拒绝的方案

- ❌ **服务端预生成插画库**：违背 local-first；维护成本高
- ❌ **让用户上传任意图**：风格秒崩
- ❌ **接 stable diffusion 之类模型本地跑**：模型大小、设备发热都不合适

## Compose 里的插画形态

定义统一接口：

```kotlin
data class IllustrationStyle(
  val ink: Color,
  val palette: List<Color>,  // 4 色
  val showCallouts: Boolean = true
)

@Composable
fun HeroIllustration(item: Item, style: IllustrationStyle, modifier: Modifier = Modifier)
```

实现里按 `item.heroVector`（一个 enum 值如 `RACKET / CAMERA_DSLR / LENS_PRIME / CAR_SEDAN / …`）分发到具体的 `Canvas` 绘制函数。所有绘制函数共享 callout/标签的 helpers，对应原型里的 `Callout` / `PlateLabel`。

## 相关

- [`../visual-language.md`](../visual-language.md) —— 视觉规则
- [`../../prototype/project/vectors.jsx`](../../prototype/project/vectors.jsx) —— 原型实现，移植参照
- [ADR-0004 · BYO AI key](0004-byo-ai-key.md) —— 阶段 B 的依赖
