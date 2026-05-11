# Cycle 0026 · 图标回退平面版 · 分类管理（显示/隐藏 + 自定义）

- **状态：** done
- **完成：** 2026-05-11

## 用户反馈 2 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 越来越丑了，还是用回原来的平面图吧 | `git show 393b1ca:android/app/src/main/res/drawable/ic_launcher_foreground.xml` 把 cycle 0013 那版（外圈 23px 圆 + 顶/底 paper-color rune + 中心 terra dot 已去掉、4 条 bevel + rune + side ticks 都保留）写回。cycle 0024/0025 那两版 3D 尝试作废，但作为参考留在 git 历史里 |
| 2 | 图鉴页新增自定义分类功能，入口是右上角一个小红点，点击后可以选择哪些分类展示哪些分类不展示，还可以新增自定义分类，新增页面类似编辑页（内容你看着填）。我的想法是用一个抽屉，每行一个分类，分割线一侧显示一侧隐藏，每行有编辑功能，编辑页放删除（二次确认），添加按钮位置你定 | 大块工作：(a) 新表 `category_prefs` (Schema v9，含 Migration_8_9，**种子插入** 6 个内建分类作为已有用户的起点)；(b) 新 [`CategoryInfo`](android/core/src/main/java/com/treasure/core/domain/CategoryInfo.kt) 领域类把内建 + 自定义统一；(c) 新 `CategoryRepository` 提供 observe / setHidden / 改插画 / 改名（仅自定义）/ 删（仅自定义）/ 添加；(d) Grid 右上 28dp 圆点（中间小 12dp 红点）→ 打开 ModalBottomSheet 管理器；(e) 管理器分两段："显示中 · N" + 分割线 + "已隐藏 · N"，每行 [隐藏/显示] 一键 toggle + [编辑 →] 进编辑页 + "+ 新增分类" 在标题右侧；(f) 编辑页：中文名 / 英文名（内建锁名）/ 横滚 HeroVector 标签 picker / 显示/隐藏 toggle / 删除按钮（仅自定义，配 AlertDialog 二次确认）；(g) Portal doorways + Grid chip 现在都按 `visibleCategories` 渲染，被 hide 的分类不出 |

## 关于自定义分类能不能"装东西"

这版只把分类管理 UI + visibility 接通了。Item 的 `category` 字段仍是 [`Category`](android/core/src/main/java/com/treasure/core/domain/Category.kt) enum，AI 提取 / 手动录入流程也只能选内建 6 个分类之一。**用户自定义的分类暂时是空容器** — 显示在 Portal / Grid 里能看到，但里面物品数永远是 0。让自定义分类真正能收物品要做的事：

1. `Item.category` 从 enum 改 String id（一刀全域 refactor 风险高）
2. AI prompt 的 enum 列表换成 dynamic「这些是用户当前可选的分类」
3. AddPreview / Edit 的品类 InlineDropdown 换 `CategoryInfo` 列表
4. 至少补一条 Migration 保护历史 item 的 category 引用

这是个独立大刀，放到 cycle 0027。本 cycle 先让 manager UI 和 visibility 两件事先到位，用户可以把不关心的分类（"汽车"什么的）藏掉清理首页。

## 关于"小红点"风格

用户说"右上角一个小红点"。直译就是一个红色圆。最终：右上 28dp 透明圆触控区 + 中心 12dp 实心红 (#C5392E)，离 Header / 控制岛都不会撞。点击就直接 ModalBottomSheet — 无 hover、无 transition，简洁。

## 不在这一刀

- Item.category 改 String，让自定义分类能收物品（cycle 0027 候选）
- 分类拖动重排（manager 抽屉内）— 用户没明说，先按种子顺序 + 新建追加
- 自定义分类的 palette 调色（先固定走 GENERIC 的灰/金调，跟内建 cycle 0013 的 generic 一致）
- cycle 0024 已记的死代码清理 / 撤销采用 / WebView headless

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
