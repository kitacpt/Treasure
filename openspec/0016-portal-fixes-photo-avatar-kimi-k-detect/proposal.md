# Cycle 0016 · Portal 修复 · 影集照片当头像 · Kimi k 系列嗅探

- **状态：** done
- **完成：** 2026-05-09

## 用户反馈 4 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | Portal 的 Coffee / Wine 没用插画图 | 这俩品类还没物品 → `state.latestByCategory[c]` 是 null → `HeroIllustration(item=null)` 走兜底 Generic。改成：latest=null 时用 `CategoryTemplates.forCategory(category)` 合成一个 stub Item（带模板默认 heroVector + palette），再喂给 HeroIllustration —— Coffee 显示 espresso machine、Wine 显示 wine bottle |
| 2 | 主页点品类全部跳到羽毛球 | cycle 0010 把主屏改 HorizontalPager 后，GridViewModel 通过 `viewModel(factory = factory(initialCategoryId))` 拿，但 `viewModel()` 的 store 缓存只看 modelClass + key，*忽略 factory 的参数差异*。结果 GRID 页第一次 lazy-compose 时（用户第一次点的那个品类）创建的 VM 一直被复用。`key(gridCategoryId)` 也没用，因为 ViewModelStoreOwner 没变。 修法：MainScreen 去掉 `key(...)` 包装；GridRoute 用 `LaunchedEffect(initialCategoryId)` 监听，把改动派给 `vm.selectCategory(Category.fromId(...))`。同一个 VM，单向数据流补上 |
| 3 | 头像可以用影集里的照片当 | (a) `Item.avatarPhotoPath: String? = null`；ItemEntity 加 `avatar_photo_path` 列；schema bump v7→v8 + `MIGRATION_7_8`（ALTER TABLE ADD COLUMN）+ MigrationTest 多一条 case；(b) 新组件 [`ui/components/HeroAvatar`](../../android/app/src/main/java/com/treasure/ui/components/HeroAvatar.kt)：`item.avatarPhotoPath` 非空时 AsyncImage(crop)，否则回退 HeroIllustration；(c) Portal / Grid / Detail 的 hero 渲染都换成 HeroAvatar；DraftCta 和 picker 内部预览仍用 HeroIllustration（那边只关心 vector）；(d) `HeroAvatarPicker` 加 `photoOptions: List<String>` / `selectedPhoto: String?` / `onSelectPhoto: ((String) -> Unit)?` 三个新参数：候选行最左侧多一排照片小圆，再一道短分隔线，然后才是品类相关的线描；点照片把它设为头像（同时大圆显示照片），点线描清掉照片回到插画；(e) DetailViewModel.removePhoto 顺手清掉 `avatarPhotoPath`（删的若是当前头像）；(f) EditScreen 接 picker 的两个回调，commit 时把 `avatarPhotoPath` 一并写回 |
| 4 | Kimi k2.5 还是报 specified incompatible | 之前 cycle 0015 的 isImplicitThinkingModel 只查 `model.contains("thinking")`，但 Moonshot 整个 `kimi-k*` 系列（k2-0711 / k2-0905 / k2-turbo / k2.5）都是 reasoning 模型 — 名字里没 "thinking" 也内置 CoT，发 specified tool_choice 直接被拒。OpenAiClient 的检测扩展为：`model.contains("thinking") ∨ model.startsWith("kimi-k") ∨ startsWith("o1"/"o3"/"o4")`。这跟 Moonshot 文档 `moonshot-v1-*` = 经典 / `kimi-k*` = thinking 的分类一致 |

## 数据 / Schema

- 加列 `avatar_photo_path TEXT` (nullable, default null)
- `MIGRATION_7_8` 是单 ALTER TABLE，旧数据无影响
- 新 schema 文件：`core/schemas/.../8.json`

## 不在这一刀

- 云端 STT 兜底 / image vision 多轮 / AI 生成插画 / Xiaomi preset 校准 / MigrationTest CI 接入

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
