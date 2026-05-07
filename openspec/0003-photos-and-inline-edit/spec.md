# Cycle 0003 · spec · "完成"长什么样

- **状态：** done
- **完成：** 2026-05-07

## 数据 / Schema

- [x] `Item` 加 `photos: List<String>`（绝对文件路径）
- [x] `ItemEntity` 加 `photos_json` 列；走 `JsonCodec.encodeStringList/decodeStringList`
- [x] `TreasureDatabase` 升 v4，`fallbackToDestructiveMigration` 处理升级
- [x] `SeedItems` 8 条全部 `photos = emptyList()`

## ViewModel

- [x] `DetailViewModel` 改为 `AndroidViewModel(application)`，工厂从 `TreasureApp` 传入
- [x] `addPhoto(uri: Uri)`：`Dispatchers.IO` 下从 `ContentResolver.openInputStream(uri)` 复制到 `filesDir/photos/<itemId>/<uuid>.jpg` → 写回 Item
- [x] `removePhoto(path: String)`：删文件 → 从 Item.photos 列表移除 → 写回
- [x] `saveEdits(nickname, oneLiner, status)`：Room upsert 三字段 + 更新 `updatedAt`

## UI · 影集 tab

- [x] 3 列网格
- [x] 第一格永远是 `+` tile（terra 描边），点击启动 `ActivityResultContracts.PickVisualMedia()` 单选图片
- [x] 已上传的照片走 `coil-compose` 的 `AsyncImage` 渲染（自动 cache）
- [x] **长按一张照片** → AlertDialog 二次确认 → 删（同时删文件）
- [x] 底部小字提示：0 张时提示"点 + 添加实拍照片"；有照片时提示"长按一张可删除 · N 张"

## UI · Detail 翻面

- [x] 正面 hero 角标 `0 PHOTOS · TAP TO FLIP` → `${item.photos.size} PHOTOS · TAP TO FLIP`
- [x] 背面：≥1 张 → 前 3 张缩略横排（每张 120dp 方形，2dp 圆角）+ "N 张实拍" / "+M 张 · 共 N 张" + "上滑抽屉看影集" 提示
- [x] 背面：0 张 → 保留原空状态（3 张旋转空相框 + × + "尚未收录实拍" + "上滑抽屉 · 影集 tab 添加" 文案）

## UI · 设置 tab 内嵌编辑

- [x] EDIT 区域：3 个字段
  - 昵称 (BasicTextField)
  - 一句话简介 (BasicTextField)
  - 状态 (3 chips: Owned / Parted / Rented)
- [x] "保存修改" 按钮：`dirty` 时启用（实色 ink 背景），未变动时禁用（card 灰背景，"未变动"文案）
- [x] 切换不同物品时（`item.id` 变化），编辑状态自动重置（`remember(item.id)`）
- [x] MANAGE 区域保持原样（删除按钮 + AlertDialog 二次确认）

## 依赖

- [x] 加 `coil-compose 2.7.0` 到 libs.versions.toml
- [x] :app build.gradle.kts 加 implementation
- [x] `androidx.activity` 已自带 `PickVisualMedia` contract（compose-bom 拉的版本够新）

## 验证

- [x] `./gradlew :app:assembleDebug` 通过（v0.6.0，12 MB）
- [ ] 设备上验证：装 → 进 Detail → 上滑抽屉 → 影集 tab → 点 + → 选图 → 缩略图出现 → 翻面看到照片 + N PHOTOS 角标 → 设置 tab 改昵称 → 保存按钮变 ink → 点保存 → 主标题更新
- [ ] 长按删除走通：长按 → 弹框 → 确认 → 文件删 + UI 更新
- [ ] 杀进程重启数据 + 文件还在
- [ ] `adb shell pm clear com.treasure` → 重启 → 种子重新写入（schema 重新走一次 destructive）

## 不在这一轮（cycle 0004 候选）

- 全屏看图浏览器
- 拍照（直接调相机）
- 多选 / 批量
- 设置页 AI / 对话录入
- callout 文字标注
- 真 schema migration
