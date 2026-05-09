# Cycle 0013 · notes

## 文件改动一览

新建：

- `app/.../illust/IllustHelpers.kt` 加 `IllustPalette` value class
- `core/build.gradle.kts` + `gradle/libs.versions.toml` 加 compose-runtime dep

主要修改：

- `app/.../illust/HeroIllustration.kt` — palette wrap 成 IllustPalette 后再 dispatch
- `app/.../illust/{Racket,Shoes,Camera,Lens,Tripod,Car,Laptop,Tablet,Earbuds,Watch,Generic,EspressoMachine,CoffeeGrinder,CoffeeBean,WineBottle,CocktailGlass}.kt` — 16 个文件 sed 把 `palette: List<Color>` 全部换成 `palette: IllustPalette`
- `app/.../illust/IllustHelpers.kt` — `palette4()` 签名换成 IllustPalette
- `core/.../domain/{Item,HeroSpec,HistoryEvent,PhotoCallout}.kt` — 加 `@Immutable`
- `app/.../ui/components/HeroAvatarPicker.kt` — Canvas modifier 从 `fillMaxWidth` 改 `fillMaxSize`，加 import
- `app/.../ui/edit/EditScreen.kt` — 删除已不用的 HeroVectorPicker + previewItem 死代码
- `app/.../ui/main/MainScreen.kt` — 去掉 `beyondViewportPageCount = 1`，回归默认 0
- `app/.../ui/add/AddViewModel.kt` — `recentConversations` 用 `combine(observeRecent, _state.map.distinctUntilChanged)` 重写
- `app/src/main/res/drawable/ic_launcher_foreground.xml` — 整图缩 + 删 3 颗 terra dot

## 设计取舍

### Compose 稳定性是那个被忽视的大事

之前一份 ItemCard 渲染 Canvas 一次 ~0.1ms，看起来无所谓。但 Compose 重新调度时会把整个 LazyColumn 子树通知 "可能需要重组" — 没有 stable 标记的话，每个 ItemCard 都会跑 composer 里的 *skip 计算* 和 *applier 决策*，哪怕最后没真重画。这种隐式开销在滚动时累计可见。

`@Immutable` + `IllustPalette` 把 Item 链路上的所有参数都标稳，Compose 直接走 fast skip。验证方法：layoutInspector 的 recomp counter 应该停在最初的次数，不再随滚动增加。

### List<Color> 不能直接 @Immutable

`@Immutable` 只能贴在类上。`List<Color>` 是 stdlib 类型，没法直接标。所以包一层 value class（同时 @JvmInline 让 runtime 不多分配对象）。

`@Immutable` + `value class` 的组合是 Compose 团队推荐的稳定值包装姿势。

### compose-runtime 进 :core

之前 :core 不依赖 Compose，是一个 platform-light 模块。Cycle 0013 引入 compose-runtime（不是 compose-ui）就是为了能在 domain 数据类上贴 `@Immutable`。

代价：
- :core 多了一个 lightweight dep（~600KB；BOM 对齐，跟 :app 一致）
- :core 现在带 kotlin-compose 编译器插件（让 @Immutable 有效）

收益：
- 所有 LazyColumn / Pager 的 stable param 链通起来

如果以后想严格分层（domain 不依赖 UI 框架），可以把 @Immutable 拆到一个 :core-ui 中间层。当前规模没必要。

### HorizontalPager 的 beyondViewportPageCount 取舍

之前 = 1：邻接页提前 compose，swipe 起手就有内容，但代价是邻页常驻（LazyColumn / LazyVerticalGrid 持续 measure）。

现在 = 默认 0：只渲染当前页。Swipe 时新页 "现去现 compose"，可能有一两帧空白。但一旦切完就丝滑，平时 idle 也不再 burning compose。

权衡：用户已经反映 "上滑下滑掉帧"，说明常驻邻页的代价 > swipe 起手的代价。回归默认是对的。

### 图标精简

用户说 "圆圈再调小一点点 + 中间的三个点去掉"。我把外径从 26 调到 23（约 11.5% 缩小）+ 完全删除中心宝石族。环身保留 gradient + bevel + rune + tick + hairline，所以立体感还在，只是更克制。

副作用：原本中心宝石的 radial gradient 和 halo 占了一些 GPU 时间（vector drawable 的 gradient 有栅格化成本），删掉之后 launcher 上图标渲染也更快一点点。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测要点

- 装机后看 launcher：金环更精致 / 中心是空的 paper 底
- Portal 顶下滑、Grid 上下滑动：不应再出现明显 frame drop
- 录入页打开手动：顶部插画选择器大圆里有清晰的 hero line drawing；点头像展开后小圆里也都画得出
- Edit 顶部头像同样应能看见 line drawing
- 切 4 个 tab：第一次切到陌生 tab 可能略有一帧空白（这是 lazy compose 代价），之后切回流畅
