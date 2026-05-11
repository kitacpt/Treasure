# Cycle 0030 · notes

## 文件改动

### Schema / 数据层
- `core/.../room/CategoryPrefEntity.kt` — 加 `hero_photo_path: String?`
- `core/.../room/CategoryPrefDao.kt` — 加 `setHeroPhotoPath(id, path)` query
- `core/.../room/Migrations.kt` — 加 `MIGRATION_9_10`（ALTER TABLE ADD COLUMN）
- `core/.../room/TreasureDatabase.kt` — version 9 → 10
- `core/schemas/.../10.json` — 自动生成
- `core/.../domain/CategoryInfo.kt` — 加 `heroPhotoPath: String?`
- `core/.../repo/CategoryRepository.kt` — 接口 + Room 实现：`setHeroPhotoPath`；toDomain 读 hero_photo_path

### UI 修改
- `app/.../ui/portal/PortalScreen.kt`
  - `GrandTitle` 还原 cycle 0023 样式（displayLarge + English + displayMedium + 24dp padding）
  - LazyColumn 末尾补 Spacer 28dp + Ornament
  - `stubItemFor` 把 `info.heroPhotoPath` 写到 stub Item.avatarPhotoPath
- `app/.../ui/search/SearchRoute.kt` — collect categoryRepository 算 visibleIds + filter
- `app/.../ui/grid/GridViewModel.kt` — selectedId 落 visibleIds 外时 fallback null
- `app/.../ui/category/CategoryManager.kt` — 拖动逻辑重写：divider 当 row-height 块；新 `computeShift` / `commitDrag` 顶级私有函数
- `app/.../ui/category/CategoryManagerViewModel.kt` — 新 `pickHeroPhoto / clearHeroPhoto / addCustomWithPhoto / pendingPhotoForNew`
- `app/.../ui/category/CategoryEditorRoute.kt`
  - 删 `HeroVectorRow` + `heroLabel`
  - `AvatarHero` 改 `(photoPath, fallbackHeroVector)` 两参数 — AsyncImage 优先
  - 新 PickVisualMedia launcher + `[+ 从相册选 / 换一张]` + `[清除]` 双按钮
  - `canSave` 调成 (内建恒 true / 自定义新建必须 photo + nameZh)
  - `onSaveBuiltIn(hidden)` / `onSaveCustom(nameZh, nameEn, hidden, photoPath)` 签名变

## 设计取舍

### 拖动 vs 列表重排时机

拖动 visual feedback 用 `graphicsLayer.translationY` shift（不重排 list）；松手 `commitDrag` 才把 `combined` 列表真的重排 + 算 `newVisibleCount`。这样 (a) 拖动时无 list 重排 → 平滑；(b) 一次 commit 出新的 orderedIds + hiddenIds 给 `applyReorder` 提交仓库。

### `commitDrag` 的 adjustedTarget 补偿

用户的拖动目标 visualSlot 是基于"含 dragging 行"的当前布局算的。从 combined 列表里移除 dragging 行后，**比 origin 大的所有 slot 都减 1 个位**。所以：

```kotlin
val adjustedTarget = if (originVisual < targetVisualSlot) targetVisualSlot - 1
                     else targetVisualSlot
```

不补偿的话：用户想把第 1 行拖到第 5 行位置，commit 会变成插入到第 5 行后面，因为移除第 1 行后第 5 行已经变成"原来的第 6 行"。

### divider slot snap 方向

拖动行落在 divider slot（即 visualSlot == visibleCount）：

- 从上方过来（originVisual < visibleCount）→ 已经决定跨段去 hidden → 落 hidden 首位
- 从下方过来（originVisual > visibleCount）→ 已经决定跨段去 visible → 落 visible 末位

代码里 `originVisual < targetVisualSlot` 这个分支判断蕴含这点。

### 自定义新建 photo 暂存在 VM 字段 而不是 Compose state

`pendingPhotoForNew` 是 VM 字段。理由：跨 onLifecycleEvent 保活（Configuration change 不丢），同时编辑页 Compose state 可以通过 `vm.pendingPhotoForNew` 直接读。简化超过 lifting state up + saving / restoring。

代价：`pendingPhotoForNew` 改变不会自动触发 recompose。所以编辑页用 `photoTick` 一个 Int counter — pick / clear 时手动 `photoTick++` 触发重组。

更"Compose-native" 的做法是把 pendingPhotoForNew 包成 StateFlow，但属于过度工程。

### 内建分类编辑 onSaveBuiltIn 不传 heroVector

之前 onSaveBuiltIn(heroVector, hidden) 让用户能改内建分类的 HeroVector 选项。cycle 0030 删了 HeroVectorRow，所以这个能力没了 — 内建分类的插画固定走 `Category.defaultHeroVector`。但内建可以挑相册图覆盖（通过 photoPath，不通过 onSaveBuiltIn 走，而是 picker launcher 直接 `vm.pickHeroPhoto(initial.id, uri)` 实时写 DB）。

签名简化到 onSaveBuiltIn(hidden)。

### Tmp photo 文件可能留下

新建分类时：用户挑了图 → 放到 `category-photos/tmp/<uuid>.jpg` → 没点 [新建] 退出 → 文件留在 tmp。

不主动清理，因为：
- 文件就 100-500KB，量级小
- 下次新建同样落 tmp/，旧的不影响
- 真要清理可以在 `clearHeroPhoto` 或 onBack 里删 — 但 `onBack` 不知道用户是想"丢弃"还是"待会儿再来"

下个 cycle 如果空间问题被报，再加 GC 策略（如：app 启动时清空 `category-photos/tmp/`）。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（14 MB）；`core/schemas/.../10.json` 已生成。

### 手测

1. 装新 APK
2. 主屏：Treasure 标题应该回到大字 displayLarge + 下方英文 "a private cabinet of things owned, used, & remembered"；底部 Ornament 装饰回来
3. 进图鉴 → 右上 [🔍][小红点] → 点小红点 manager
4. 长按某行的左侧三横纹握把 → 上下拖：分割线位置稳，被拖行下移 / 让位 ok；松手后 commit
5. 把可见区最后一行拖到 divider 下方：松手时应进入"已隐藏"段（之前 cycle 0029 拖不到下面去）
6. 把所有 6 个内建都拖到 divider 下方：Portal 应 "所有分类都被隐藏了" + 链接，搜索结果 0 条
7. 点 [+ 新增分类] → 编辑页：顶部 italic "+ 从相册选" 占位 + [+ 从相册选] 按钮 + [新建] 灰
8. 点 [+ 从相册选] → 系统相册 picker → 选张图 → 头像变成那张图 + [新建] 转 terra
9. 输入中文名 → [新建] → 回主屏，新分类在 Portal doorway 显示那张图 + Grid chip 行显示
10. 编辑某个内建分类（如羽毛球）：头像默认显示拍子线描（不再是错的 GENERIC 通用图）；点 [+ 从相册选] 挑图 → 头像变照片；点 [清除] 回到拍子线描
11. 在某个 chip（如羽毛球）页 → 进 manager → 把羽毛球拖到 hidden 段：回 Grid 应自动跳回 "全部" chip（之前会停在已消失的 chip 上还显示羽毛球物品）
12. 进搜索 → 输入羽毛球某个品牌：之前能搜到的现在不应出现（因为它的分类被隐藏了）
