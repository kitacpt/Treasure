# Visual Language · Treasure

视觉规格的**唯一权威**是 [`../prototype/project/Treasure.html`](../prototype/project/Treasure.html)（双击在浏览器打开）。这份文档是它的浓缩文字版，方便在 Compose 里查色号/字号。**遇到分歧以原型为准。**

## 调色板

来自 `prototype/project/Treasure.html` 的 `TWEAK_DEFAULTS.paletteA`：

| Token | Hex | 用途 |
|---|---|---|
| `terra` | `#8a3a1f` | 赤陶强调色，仅用于点睛（光标、被选中态、品类色等） |
| `ink` | `#1a1815` | 主文字、线描描边 |
| `paper` | `#f4f1ea` | 全局背景（暖纸面） |

衍生 token（来自 `direction-a.jsx` 顶部的 `card / sub / line` 三个变量）：

| Token | Light | Dark | 用途 |
|---|---|---|---|
| `bg` | `#f4f1ea` | `#1a1815` | 背景 |
| `card` | `#fbf9f4` | `#27241f` | 卡片表面 |
| `ink` | `#1a1815` | `#eae5d8` | 主文字 |
| `sub` | `rgba(26,24,21,0.55)` | `rgba(234,229,216,0.55)` | 副文字 / 标签 |
| `line` | `rgba(26,24,21,0.10)` | `rgba(234,229,216,0.12)` | 分隔线、卡片描边 |

阴影：light 模式下卡片有极轻的 `0 0.5px 0 rgba(26,24,21,0.06), 0 1px 3px rgba(26,24,21,0.04)`。dark 下没有阴影。

控制岛背景：`rgba(26,24,21,0.85)`（light）/ `rgba(40,36,30,0.78)`（dark），叠 `backdrop-filter: blur(20px) saturate(1.3)`。

## 字体

| Token | Family | 用法 |
|---|---|---|
| `serif` | `Cormorant Garamond, Noto Serif SC, serif` | 标题、Portal 大标题、详情品名 |
| `sans` | `Space Grotesk, Noto Sans SC, sans-serif` | 正文、按钮、控制岛 |
| `display` | 同 serif | 仪式感的大字号 |
| `mono` | `JetBrains Mono, ui-monospace, monospace` | 日期 strip、罗马数字、标签 caps |

可切换的 fallback（在 Tweaks 面板里）：

- 衬线：Cormorant Garamond / Fraunces / Noto Serif SC
- 正文：Inter / Space Grotesk / Noto Sans SC

**Compose 实现要点**：把这几个家族打包进 app 的 `res/font/`（不要靠系统字体）。Cormorant 的斜体（italic 500）是博物馆插画标注线必备，确保打包了 italic 字重。

## 排版尺度（单位 sp）

| 用途 | size | weight | letter-spacing | line-height |
|---|---|---|---|---|
| Portal 大标题 | 64 | 500 (serif) | -0.03em | 1.0 |
| Portal 副标题（italic） | 14 | 400 (serif italic) | 0.02em | — |
| Home 标题 "Treasure" | 36 | 500 (serif) | -0.02em | 1.05 |
| 详情 hero 品名 | 28–32 | 500 (serif) | -0.02em | 1.1 |
| 卡片标题 | 18 | 500 (serif) | — | 1.1 |
| 正文/按钮 | 12.5 | 500 (sans) | — | — |
| 副标签（小 caps） | 9.5–11.5 | 400 (sans) | 0.18–0.20em（uppercase） | — |
| 罗马数字角标 | 9 | 400 (mono) | 0.10em | — |
| 数字（计数等） | 22 | 500 (serif tabular-nums) | — | — |

## 插画语言（来自 `prototype/project/vectors.jsx`）

**核心规则：**

- 描边：`#1a1815`（ink）发丝级，`stroke-width` 在 `0.4`（结构内的细线）到 `1.0`（外轮廓）之间
- 色块：用物品自己的 4 色 palette 做平涂，**所有色块 opacity 0.18–0.55**（永远不是 100% 实色，呼吸感）
- **不允许**：渐变、阴影、伪 3D、glow、freehand 高难度路径
- 形状只用 SVG 原语：circle / rect / ellipse / polygon / line
- 每张插画带 2–3 条 callout 引线 —— 一个端点上的小圆点 + 直线 + 末端的标注文字
- 标注文字：Cormorant 斜体 9px，前缀小写罗马数字 + 中点，例如 `i · pentaprism`、`ii · grip`、`iii · rubber foot`
- callout 颜色：`stroke=#1a1815, opacity=0.45`；标注 `fill=#1a1815, opacity=0.75`

**Compose 实现要点**：原型用 SVG。Compose 里两条路：

1. 直接把 SVG 当 `ImageVector` 加载（`androidx.compose.ui.res.vectorResource` + Android Vector Drawable，`drawable/`）—— SVG 必须先转 VectorDrawable XML（用 Android Studio 的 `New > Vector Asset`，或 `svg2android` CLI）
2. 用 Compose Canvas 重绘 —— 给每个品类一个 `@Composable HeroIllustration(item)` 函数，参数化 palette。**这条路更贴近原型**（原型本来就是参数化绘的），cycle 0001 起用这一条，[`../android/app/src/main/java/com/treasure/illust/`](../android/app/src/main/java/com/treasure/illust/) 下 16 张插画都是 Compose Canvas 实现。

## 控制岛（Bottom Floating Island）

来自 `direction-a.jsx` 的 `Island` 组件：

- 浮于屏幕底部 `bottom: 18px`，水平居中，**不撑满**
- 形态：药丸（`borderRadius: 999`）
- 内容：4 颗胶囊按钮 —— **门厅 / 图鉴 / 录入 / 设置**
- 按钮内部：`<icon> <label>`，gap 7px，padding `9px 16px`
- 选中态：按钮背景 = 全局 `bg`，文字色 = `ink`
- 未选中态：透明背景，文字色 `#f4f1ea`（不分浅深模式）
- 容器外框：`0.5px solid rgba(255,255,255,0.08)`
- 阴影：`0 12px 40px rgba(0,0,0,0.18), 0 1px 0 rgba(255,255,255,0.05) inset`
- 切换动画：`background 180ms ease`

## Portal 屏的几个特殊元素

来自 `direction-a.jsx::Portal`：

- 顶部日期 strip：等宽小字，左 `EST. 2020`、右 `MAY VI · MMXXVI`，letter-spacing `0.18em`，size 9.5px
- ornament（罗盘装饰）：两段细线 + 同心圆 + 上下钻石，颜色 `ink`/`sub`，居中
- 三连计数：`items / owned / rooms`，数字 22px serif tabular-nums，标签 9px sans uppercase
- "The Rooms" 节标题：9.5px sans uppercase，前后各一颗 `✦`
- 4 扇门（2×2 网格）：每扇门内含——
  - 右上角罗马数字（I / II / III / IV）
  - 中部最近收入物的矢量缩略
  - 底部细线分隔后：中文名（serif 18）+ `count pcs · english_name`（sub）

## 抽屉行为（cycle 0002 起实现，cycle 0031 / 0034 调整）

来自原型对话："参考网易云的层叠卡片"：

- 默认半隐藏，详情屏底部留一条 ~52px 的提示条（"↑ 上拖看详情"）
- 上滑展开到 ~78% 屏高
- 顶部三 tab：**参数 / 历史 / 影集**（cycle 0034 v9 改成参数在前）
- 抽屉内卡片是各 tab 的内容
- 再下滑收起；cycle 0035 起抽屉展开时 back 键先收回 `partialExpand()` 不 pop 到 Main

## 翻面（cycle 0002 起实现）

来自原型："明信片翻面"：

- 详情 hero 矢量图右下角小角标 `N PHOTOS`
- 点击 → 整张卡 600ms Y 轴 3D 翻转
- 背面：用户真实照片网格 + "添加照片" 按钮
- 这是博物馆插画统一风格 vs. 真实照片杂乱的折中点 —— 默认看到的是统一线描，想看真实可主动翻面

## 暗模式

切换全局 `dark: true/false`（Tweaks 面板）。变更点：

- `bg`: `#f4f1ea` → `#1a1815`
- `card`: `#fbf9f4` → `#27241f`
- `ink`: `#1a1815` → `#eae5d8`
- `sub` / `line`: 反相 alpha
- 控制岛：背景透明度略加深、blur 不变
- 卡片阴影：消失

矢量插画的 ink 在 dark 模式下不直接换成白 —— 而是换成 `#eae5d8`（"暗纸面上的旧墨"），保持暖色调一致。
