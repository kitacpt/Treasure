# Cycle 0016 · notes

## 文件改动

新建：

- `app/.../ui/components/HeroAvatar.kt` — photo-or-illustration dispatcher

主要修改：

- `core/.../ai/OpenAiClient.kt` — `isImplicitThinkingModel` 加 `kimi-k*` / `o1*` / `o3*` / `o4*` 前缀
- `core/.../domain/Item.kt` — 加 `avatarPhotoPath: String? = null`
- `core/.../room/ItemEntity.kt` — 列 `avatar_photo_path` + toDomain / fromDomain 同步
- `core/.../room/TreasureDatabase.kt` — version 7 → 8
- `core/.../room/Migrations.kt` — `MIGRATION_7_8` (ALTER TABLE)
- `core/src/androidTest/.../MigrationTest.kt` — 多 `migrate_7_to_8_adds_avatar_column_nullable`
- `app/.../ui/portal/PortalScreen.kt` — `stubItemFor(category)` 兜底空品类；HeroAvatar 替换 HeroIllustration
- `app/.../ui/grid/GridScreen.kt` — HeroAvatar 替换；`GridRoute` 加 LaunchedEffect 监听 initialCategoryId
- `app/.../ui/main/MainScreen.kt` — 去掉 `key(gridCategoryId)` 包装
- `app/.../ui/detail/DetailScreen.kt` — HeroAvatar 替换；`onSetCallouts` 仍在
- `app/.../ui/detail/DetailViewModel.kt` — `removePhoto` 清掉 avatarPhotoPath（如果是同一张）
- `app/.../ui/edit/EditScreen.kt` — `avatarPhoto` state；commit 写 `avatarPhotoPath`；HeroAvatarPicker 新参数 photoOptions / selectedPhoto / onSelectPhoto
- `app/.../ui/components/HeroAvatarPicker.kt` — 新 photo 列；showingPhoto 时大圆走 AsyncImage；分隔线把 photo 区和 illustration 区分开

## 设计取舍

### 为什么 viewModel(factory = ...) 不跟着 initialCategoryId 走

`viewModel()` 在 ViewModelStoreOwner 里按 `(modelClass, key)` 缓存。同 modelClass + 同 key（默认是类全名）→ 同一个 VM，*工厂参数完全被忽略*。`key(...)` 只重组 composable 子树，不动 store —— 所以也救不了。

正确做法两条：

1. 给 `viewModel(key = "..." )` 加唯一 key，让每个 categoryId 拥有独立 VM。代价是 ViewModelStore 越攒越多。
2. 共享一个 VM，外部派 setter — `LaunchedEffect(initialCategoryId) { vm.selectCategory(...) }`。

选了 (2)，更轻量；GridViewModel 本来就有 `selectCategory` 方法，单向数据流也更干净。

### Doorway 空品类用 stub Item

简单直接：`stubItemFor(category)` 用品类模板的默认 heroVector + palette，喂给 HeroIllustration。`remember(category)` 给它一个稳定 key —— 滚动时不重新分配。

### avatarPhotoPath vs photos[0]

考虑过把 photos 列表的第一张当 "头像" — 不需要 schema 改动。但语义不直观（用户重排照片就改头像？），且无法 "用线描，不用照片"。还是单独存一个字段更清晰。

### HeroAvatarPicker 候选行排版

photo 优先在前是因为：用户进来要换头像时多半是想用照片（更具体），线描是兜底。短分隔线把两类视觉上区分开 —— 0.5dp × 56dp 高的灰色细线，简洁。

photo 圆 click → 设 `avatarPhoto = path`；illustration 圆 click → 同时设 `heroVector` 并清掉 `avatarPhoto`（用户既然主动点了线描，意思就是不要照片）。

### HeroAvatar 渲染 photo 的时候

photo 用 `AsyncImage(model = path, contentScale = ContentScale.Crop)`，外面 caller 给的 modifier 决定形状（Detail 是矩形，picker 是 CircleShape）。callsite 不动。

### Kimi k 系列

依据：实际报错 + Moonshot API docs：

- `moonshot-v1-{auto, 8k, 32k, 128k}`：classic generative，接受 specified tool_choice
- `kimi-k2-0711-preview` / `kimi-k2-0905-preview` / `kimi-k2-turbo-preview`：内置 CoT，禁 specified
- `kimi-k2-thinking-*`：明显的 thinking
- `kimi-thinking-preview`：旧的 thinking

旧的 cycle 0015 只查 "thinking" 子串，漏掉 k2-0711 / k2-0905 / k2-turbo。本刀 `startsWith("kimi-k")` 一刀切，覆盖整个 k 系列（同时跟 moonshot-v1 严格分开）。

OpenAI o-series 同时扩到 `o4*`（GPT-4o 系列已经不算 reasoning，只 o1/o3/o4 才是）。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL

cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :core:compileDebugAndroidTestKotlin
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测

1. Portal 应该 6 个 doorway 都画得出对应插画（Coffee = espresso, Wine = wine bottle）
2. 点 "汽车" doorway → 落汽车 grid；点 "咖啡" → 落咖啡 grid（不再全跳羽毛球）
3. 进任一物品 → Edit → Edit 顶部头像点开 → 如果该 item 有照片，候选行最左有照片圆 → 点照片 → 大圆变照片 → 保存 → Portal / Grid / Detail 显示照片当头像
4. 同一物品再回 Edit → 头像区点任意线描 → 大圆回到插画 → 保存 → 各处恢复线描
5. 删除一张正在当头像的照片 → 自动清空头像 → 各处回到线描
6. Settings → Provider Kimi · Moonshot → Model 字段填 `kimi-k2-0905-preview`（不开 thinking 开关）→ 测试连接 → 应该不再出 specified incompatible
