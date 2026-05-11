# Cycle 0028 · notes

## 文件改动

- `core/.../domain/Category.kt` — 加 `defaultHeroVector: HeroVector` 字段，6 个内建写正确的代表插画
- `core/.../repo/CategoryRepository.kt` — `toDomain` 内建行 override hero_vector 用 enum 默认
- `app/.../ui/grid/GridViewModel.kt` — `visibleItems` 过滤；"全部" / totalCount 都按它算
- `app/.../ui/portal/PortalViewModel.kt` — 同上；latest/count/owned 都基于 visibleItems
- `app/.../ui/portal/PortalScreen.kt`
  - `PortalRoute` / `PortalScreen` 加 `onOpenCategoryManager`
  - SectionLabel "Latest entry" 改 centered + 两边 ✦
  - 新 `EmptyRoomsHint` / `EmptyLatestHint` composables
  - `DoorwayCard` 去 `latest` 参数；hero 永远 `stubItemFor(info)`
- `app/.../ui/grid/GridScreen.kt`
  - `GridRoute` / `GridScreen` 加 `onOpenCategoryManager`
  - 删除内部 `managerOpen` state；红点点击直接走 callback
- `app/.../ui/main/MainScreen.kt` — `categoryManagerOpen` state 提到顶层；两个 route 都接 callback；顶层 mount `CategoryManager`
- `app/.../ui/category/CategoryManagerViewModel.kt` — 新 `applyReorder(orderedIds, hiddenIds)`
- `app/.../ui/category/CategoryManager.kt` — 全面重写：
  - 删 subtitle、删 [完成]、删 [隐藏/显示] pill、删 "编辑 →" 文字
  - 加 三横纹握把 + `detectDragGesturesAfterLongPress`
  - 渲染：workVisible + DIVIDER + workHidden，`graphicsLayer.translationY` 定位每行
  - 松手 commit `applyReorder`
  - Editor 头部加 112dp AvatarHero
  - HeroVectorRow 加 `enabled` 参数（内建 disabled）
  - `canSave` 强制 `heroVector != null`

## 设计取舍

### 拖动 reorder vs 拖动 hide/show 复用同一手势

用户原话："不要显示隐藏和显示按钮而是直接拖动，拖动后的排序就是首页展示还有图鉴页滚动条的排序"。我理解为单一手势：长按 + 拖。同段拖动是 reorder，跨越分割线是 toggle hidden。

实现上：分割线是一段视觉占位（DIVIDER_HEIGHT 36dp），不是独立 index。`targetIndex` 反算时把 divider 偏移扣掉，落在 divider 区域内取靠近的那侧。最后 commit 时根据 `wasVisible / nowVisible` 决定 `visibleCount` 增减。

代价：单段内能拖；跨段也能拖；DIVIDER 是固定的视觉锚（不随拖动移动），跟用户预期"分割线在哪我看一眼就知道"一致。

### 不重排 `combined` 列表，只 graphicsLayer

每次拖动都 `mutableStateOf list = newList` 会导致大量重组。借用 cycle 0017 `ReorderableSpecs` 同款套路：list 本身不动，只用 graphicsLayer translationY 视觉移动行；松手时一次性 `workVisible / workHidden = newPartition`，然后 `applyReorder` 提交仓库。流畅得多。

### 内建 heroVector 用 enum override 不写 migration

cycle 0026 那次种子 SQL `hero_vector = 'GENERIC'` 是个 bug，6 个内建都得到了通用插画而不是各自的代表插画。修法两条：
1. 写 MIGRATION_9_10 UPDATE category_prefs SET hero_vector = ... WHERE id = ... — schema 版本要 bump
2. 在 RoomCategoryRepository.toDomain 里 override — 不动 schema

走 2。因为：
- 内建 6 个的代表插画其实是产品决策，应该绑在 Category enum 上不绑在 DB 里
- 用户改 / 卸载重装 / 切设备时也都希望内建分类一致
- enum 加字段比写 migration 简单且对后续修改友好

代价：DB 里的 hero_vector 列对内建行实际是死字段。轻微浪费，但比 schema bump 划算。

### Manager 实时生效 + 没有"取消改动"

每次松手就 commit 到仓库。如果用户拖错了不能 undo — 只能再拖回去。考虑过加 [完成 / 取消] 双按钮但拒绝：(a) 用户原话要求 "去掉完成按钮"；(b) 拖动是物理动作，意外操作几率比按钮 tap 低；(c) 实时生效更轻，符合"直接拖动"的简洁感。

如果将来有用户报"我拖错了想撤回"，可以加底部 snackbar "撤销"短暂出现。

### Portal doorway 永远用 info.heroVector

之前是 `latest?.heroVector ?: stubItem`，意思是"有 latest item 就用它的 hero（比如最近收的 Yonex 拍子）；没有就用模板默认"。用户反馈：插画应该是分类的基础图，不是物品的图。改成永远用 info.heroVector — 一致性强，doorway 看一眼就知道是哪个分类，不会跟着新进物品摇摆。

代价：自定义分类必须在 manager 编辑页选个插画，否则 doorway 是占位。这正是本 cycle 加 "插画必填" 的设计。

### Editor 顶部 Avatar 用 112dp 圆 而不是复用 HeroAvatarPicker

物品编辑页用的 `HeroAvatarPicker` 有照片选择 / 拍照 / 长按删除等一堆功能。分类编辑用不上那些。所以写一个简化版 `AvatarHero` — 112dp 圆 + 单张 HeroIllustration 居中。视觉对齐 HeroAvatarPicker，但代码精简。HeroVector 的实际选择仍走下面的横滚 row。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（14 MB）

### 手测

1. 装新 APK
2. 进 Grid → 右上小红点 → manager
3. 长按某行的三横纹握把 → 拖动到分割线下方 → 松手：那行应迁到"已隐藏"段，Portal doorway / Grid chip 同步消失
4. 拖回分割线上方 → 恢复显示
5. 显示中段内拖动：sort_order 重排，Portal doorway 顺序 / Grid chip 顺序跟着改
6. 全部 6 个分类拖到下面隐藏：Portal 应显示 italic "所有分类都被隐藏了 — 没有房间可以走进去。" + "去分类管理 →" 链接；LATEST ENTRY 段同样空文案
7. 点 "去分类管理 →" → 弹同一个 Manager 抽屉
8. 点某个内建分类（如羽毛球）的小红点 → 进编辑页：
   - 顶部 112dp 圆 — 应显示拍子插画（不是 generic 通用图）
   - 中文 / 英文输入框 disabled
   - 插画行半透明 + 不可点
   - 没有 [删除分类] 按钮
9. 返回 → 点 + 新增分类 → 编辑页：
   - 顶部 112dp 圆显示 italic "+ 选张插画"
   - 中文 / 英文输入框可填
   - 插画行可点；选了之后 [新建] 按钮从灰变 terra
10. 不选插画时点 [新建] 应无效（按钮 enabled=false）
11. 隐藏分类后回 Grid "全部" chip：N ITEMS 数字应只算可见分类的物品；之前隐藏分类下的物品不出现在"全部"列表里
