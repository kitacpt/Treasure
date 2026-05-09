# Cycle 0013 · 性能 · 修插画变白 · 图标精简

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 3 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | 上滑下滑加载有掉帧、整体没之前丝滑 | 三件事：(a) 把 `palette: List<Color>` 包成 `@Immutable @JvmInline value class IllustPalette(val colors: List<Color>)` — 16 张 illustration 全部从不可 skip 变成可 skip，LazyColumn / LazyVerticalGrid 滚到同一张 ItemCard 不再触发 Canvas 重绘；(b) `Item / HeroSpec / HistoryEvent / PhotoCallout` 全部加 `@Immutable`，:core 引入 `compose-runtime` BOM dep；(c) HorizontalPager 的 `beyondViewportPageCount = 1` 删了，回归默认 0 — 只渲染当前页，Portal 滚动时 Grid 不在背景跑布局；(d) AddViewModel 的 `recentConversations` 之前用嵌套 `let { MutableStateFlow().also { collect } }` 等于让数据走两遍 collector，改成 `combine(observeRecent, _state.map { conversationId })` 一根流到底 |
| 2 | 插画全是白的 | `HeroAvatarPicker` 里 Canvas 给的 modifier 是 `fillMaxWidth()` —— Canvas 没有 intrinsic 高度，结果实际渲染高度 = 0。改成 `fillMaxSize()` 后大头像 + 候选小头像两处都正常显示。顺手把 EditScreen 里 cycle 0011 留下的死代码 `HeroVectorPicker` 整段删了 |
| 3 | 图标 ring 再调小一点点 + 中心三个 terra 点去掉 | `ic_launcher_foreground.xml` 把外径 26→23 / 内径 20→17.5；rune / tick / hairline 全部按比例往里缩；中心 terra 宝石（halo + radial gradient gem）+ 两侧 terra dot 全部删除，环身就是干净的金环；环底 cast shadow 同步缩小到 19dp 椭圆 |

## 顺手的连带改动

- `:core/build.gradle.kts` 加 `kotlin-compose` 插件 + `compose-runtime` 依赖。BOM 让 :app / :core 版本对齐。代价是 :core 多了一份 compose-runtime 但 release APK 不增大（已经被 :app 引了）
- `gradle/libs.versions.toml` 加 `androidx-compose-runtime` alias
- `IllustPalette.Empty` 公共单例，`HeroIllustration(item = null)` 的兜底分支不再每次新建空 list
- `palette4()` 签名跟着改成 `IllustPalette` 入参

## 不在这一刀

- 进一步改 `Modifier.drawWithCache` 给 illustration 录制 Picture（更激进的缓存，目前 @Immutable + skip 已经够用，不动）
- 云端 STT、AI 生成插画、image vision context 多轮、preset 校准 — 仍在 cycle 0014 候选清单

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
