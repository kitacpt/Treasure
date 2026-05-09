# Cycle 0016 · 验收

## Portal

- 6 个 doorway 都画得出代表插画：
  - 羽毛球 → 球拍 / 摄影 → 相机 / 汽车 → 轿车 / 电子产品 → 笔电
  - 咖啡（无物品时）→ espresso machine
  - 酒水（无物品时）→ wine bottle
- 点 "汽车" doorway → 落到汽车 grid（不再是羽毛球）；同款验证 Coffee / Wine / 其他每一个

## 主屏 Pager + Grid

- `MainScreen` 不再用 `key(gridCategoryId)` 包 GRID 页
- `GridRoute` 内部 `LaunchedEffect(initialCategoryId)` 在变化时调 `vm.selectCategory(Category.fromId(...))`
- 单一 GridViewModel 实例随 initialCategoryId 切换 displayed category

## 头像（Edit + 各处展示）

- Detail 抽屉 → 影集 → 任意张照片，记 path
- Edit 屏顶部头像选择器点开：候选行最左是影集照片（圆形 56dp），中间一道竖线分隔，右边是按品类相关的线描候选
- 点某张照片：picker 大圆变成那张照片裁切；commit 时 `Item.avatarPhotoPath = path` 落库
- 再点任一线描：清掉 `avatarPhotoPath`，picker 大圆回到线描
- Portal doorway / Latest entry / Grid ItemCard / Detail HeroFront 都用 `HeroAvatar` 渲染：avatarPhotoPath 非空时显示照片 AsyncImage，否则线描
- 删除一张正在被当头像的照片 → DetailViewModel.removePhoto 自动 clear `avatarPhotoPath`

## Schema

- `@Database(version = 8)`；`core/schemas/.../{5,6,7,8}.json` 全部入库
- `Migrations.ALL = [5_6, 6_7, 7_8]`
- MigrationTest 多一条 `migrate_7_to_8_adds_avatar_column_nullable`

## Kimi tool_choice

- `OpenAiClient.isImplicitThinkingModel` 现在命中：
  - `*thinking*`（旧的）
  - `kimi-k*` 前缀 — 含 `kimi-k2-0711-preview` / `kimi-k2-0905-preview` / `kimi-k2-turbo-preview` / `kimi-k2.5` 等
  - `o1*` / `o3*` / `o4*` 前缀
- 命中后 payload 用 `tool_choice: "auto"`，不再发 specified 形式
- 用户即便不开 thinking 开关也能直接用 kimi-k 系列模型测连接成功

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
- `:core:compileDebugAndroidTestKotlin` 通过；`migrate_7_to_8_adds_avatar_column_nullable` 在 emulator / 真机能跑过
