# Cycle 0028 · 隐藏真生效 · Manager 拖动改造 · Editor 插画必填

- **状态：** done
- **完成：** 2026-05-11

## 用户反馈 3 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 隐藏分类的时候，全部的统计值还会显示全部的值，并且我全部隐藏后，羽毛球居然还是出现在了全部里，首页的 LATEST ENTRY 也还有东西。（LATEST ENTRY 的展示和 THE ROOMS 的展示改成一样，居中左右都有一颗星星），全空时要有对应的交互引导去分类管理 | (a) Grid / Portal 全部统计 `total`、`latestOverall`、"全部" chip 的过滤集合都改成只算 visibleItems（item.category 在 visibleCategories 里）— 这是 cycle 0026 当时没补上的过滤盲区。(b) Portal `LATEST ENTRY` 标签改成 `✦ Latest entry ✦` 居中两边星星，跟 `THE ROOMS` 一致。(c) 全空状态：`THE ROOMS` 段全隐藏时画 italic 文案 + "去分类管理 →" terra link；`LATEST ENTRY` 没物品（所有可见分类都空）同样给文案 + 链接。两个链接都通过 MainScreen 顶层的 `categoryManagerOpen` state 弹同一个 Manager 抽屉 |
| 2 | Manager 抽屉细节：去副标题（"隐藏只是 ..."）；不要"显示/隐藏"按钮改成直接拖动；拖动后排序就是首页+图鉴的顺序；不要"编辑"二字用小红点；去掉"完成"按钮（实时生效） | 重写 [`CategoryManager.kt`](android/app/src/main/java/com/treasure/ui/category/CategoryManager.kt)：(a) 删 italic 副标题。(b) 每行去 [隐藏/显示] outline pill，改成左边一个三横纹握把（长按 + 拖）。(c) 中间渲染一条 italic "↑ 显示中 · ↓ 已隐藏" 分割线 — 拖一行跨过它就 toggle hidden，同段拖动改 sort_order；松手一次性提交 `applyReorder(orderedIds, hiddenIds)`。(d) 右边 28dp 触控区 + 中央 10dp 实心红圆点取代 "编辑 →" 文字，点击进编辑页。(e) 底部 [完成] 按钮删掉 — Manager 关闭走系统手势 / scrim |
| 3 | 编辑页：像物品编辑页一样顶部是"头像"；必须有插画才能创建成功；内置品类用内置插画；自定义品类必须上传插画。插画是首页展示的基础图，而不是选第一个物品的插图 | (a) `CategoryEditor` 顶部加 112dp 圆 AvatarHero — 跟 `HeroAvatarPicker` 视觉同款；null 时画 italic "+ 选张插画" 占位提示。(b) `canSave = (isBuiltIn || nameZh.isNotBlank()) && heroVector != null` — 自定义且未选插画时 [新建] / [保存] 按钮置灰。(c) 内建：`HeroVectorRow` 行 `enabled = !isBuiltIn`，整行半透明 + 点击无响应；用 enum 自带的 `defaultHeroVector` 显示（cycle 0028 给 `Category` enum 加了这字段）。(d) Portal doorway 现在永远用 `info.heroVector` 渲染（去掉了 `latest ?: stubItem` 的回退）— 即"插画是基础图"，物品多少不影响首页那张图 |

## 关于内建 heroVector 的修正

cycle 0026 的 Migration_8_9 种子插入 6 个内建行时把 hero_vector 一律填了 `"GENERIC"`，是个 bug — 应该按品类的代表插画来填（羽毛球→拍子、摄影→单反相机 等）。

不写新 Migration 改数据，而是在 `Category` enum 上加 `defaultHeroVector` 字段，`RoomCategoryRepository.toDomain` 在内建行返回时 **override** stored hero_vector 用 enum 默认值。stored 'GENERIC' 还在 DB 里，但 domain 层永远看到正确的。

这样以后想改某个内建的代表插画，只要改 enum 一行就行，不动 schema 不动迁移。

## 不在这一刀

- AI prompt 的 hero spec 模板提示按自定义分类适配（cycle 0027 留的）
- 死代码清理（CategoryForm.kt / saveManual）
- 撤销采用、WebView headless、流式输出
- 拖动手势的"自动滚动"（拖到 sheet 顶/底缘不会触发抽屉滚动）— 当前 Manager 内容用 `verticalScroll(rememberScrollState())`，长 list 拖到屏外要松手再滚

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
