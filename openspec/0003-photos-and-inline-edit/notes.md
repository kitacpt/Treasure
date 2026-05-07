# Cycle 0003 · 工作笔记

## 几个决定

- **DetailViewModel → AndroidViewModel**：照片 IO 要 `Application` 拿 `filesDir` + `ContentResolver`，AndroidViewModel 是最干净的口子。换的代价：factory 多一行 `app as TreasureApp` cast，无伤大雅。
- **照片存 app 私有 `filesDir`**：不去 MediaStore / 公开相册。理由：图鉴是私人的，不混入相册；reinstall 会丢但反正还没真用户。备份策略 cycle 0006 跟云同步一起做。
- **Coil 而不是手写 BitmapFactory**：手写要自己管 `inSampleSize`、cache、async 解码。Coil 直接 `AsyncImage(model = absolutePath)` 就行，多出 1 个 dep，省半天。
- **photos 字段就是 `List<String>` 而不是 `List<Photo>`**：现在只有路径需要存。如果以后加 caption / order / takenAt 再升数据结构。`Photo` data class 是 YAGNI。
- **inline edit 限 3 字段**：identity 类（brand / model / category / heroVector）不让在这改。这些改了会让物品"变成另一件"，应该走"重建"流程而不是 edit。
- **dirty 检测**：纯 equality 比对，不用 ViewModel 状态。`val dirty = nickname != item.nickname || ...`，简单且对。
- **`remember(item.id) { mutableStateOf(item.nickname) }`**：保证切到新 item 时表单重置。如果 Room flow 推一个新 item 副本但 id 不变（比如外部 update），编辑中的字段不会被覆盖。

## 坑

- **PickVisualMedia 在某些 OEM 上 fallback 行为不稳**：vivo / 华为 自家相册接管 picker 可能行为略不同。选图后能拿到 content:// URI 就 OK。
- **`itemsIndexed` 在 LazyVerticalGrid 里要 `androidx.compose.foundation.lazy.grid.itemsIndexed`**——一开始我搞混了 LazyColumn 那个，编译报"itemsIndexed 不存在"。
- **长按删除手势**：`Modifier.combinedClickable` 也行；`pointerInput { detectTapGestures(onLongPress = ...) }` 显式一些。后者选了。
- **背面 hero 显示前 3 张时的 aspectRatio**：`Modifier.height(120.dp).aspectRatio(1f)` —— 先固定高度再正方形。如果反过来（先方再高）容易和外层 fillMaxWidth 打架。

## 踩到没

- DetailViewModel 改 AndroidViewModel 后 factory 必须从 `ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY` 拿 Application；之前 cycle 0001 留下的 cast `app as TreasureApp` 仍然适用，没改。
- Schema v3 → v4 期间老 entity 解码会报错（缺 `photos_json` 列），靠 `fallbackToDestructiveMigration` 兜住。**cycle 0004 之后必须停止吹库。**

## 给下一刀的备忘

cycle 0004 候选：

1. **全屏看图**：照片 tap 进入全屏，hero 翻面 + 拖拽 dismiss
2. **AI 服务设置页 + 对话式录入**：实现 ADR-0004 的接口（`AiClient`）；BYO key 表单；`Add` stub 真接通
3. **真 migration**：cycle 0001-0003 全 destructive，再不写真 migration 之后用户数据要丢

按用户优先级走。
