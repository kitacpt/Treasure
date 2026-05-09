# Cycle 0010 · 验收

## 适配 / 视觉

- 编辑页 “品类” 行：6 个品类全选都显示在折叠后单行内（`InlineDropdown`），不会再换行
- 新增历史 dialog 的 “类型” 行：5 个 HistoryKind 折叠成单行 `+ 购入 ▾`
- “尚未配置 AI” 一行字 banner，紧贴 ChatHeader 下 0.5dp divider；点 → 跳到 Settings tab（pager animateScroll）

## 录入页

- 手动录入屏顶部居中：124dp paper 框大插画 + 下方 56dp 横滚选项；按品类只显示相关 HeroVector
- 选项点击 / 大图点击都能切换；选中插画进 saveManual → 持久化到 Item.heroVector

## 主屏架构

- NavHost 路由收缩成 Main / Detail / Edit 三条
- 应用启动直接到 Main（HorizontalPager 4 页：Portal / Grid / Add / Settings）
- 控制岛点击 = pagerState.animateScrollToPage(...)；左右滑 = pagerState.currentPage 反向高亮
- 门厅点 “The Rooms” doorway → set gridCategoryId + pager 滑到 Grid；Add 内 “尚未配置 AI · 前往设置” → pager 滑到 Settings
- Detail / Edit 仍是 NavHost 推上来的覆盖屏，`popBackStack()` 回 pager（保留 pagerState 位置）

## Schema migration

- `@Database(version = 7, exportSchema = true)`；`core/schemas/com.treasure.core.room.TreasureDatabase/` 下三份 JSON：5.json / 6.json / 7.json
- `Migrations.ALL = [MIGRATION_5_6, MIGRATION_6_7]`
- 不再有任何 `.fallbackToDestructiveMigration()`；只留 `.fallbackToDestructiveMigrationOnDowngrade()`

## 历史对话持久化 + 多轮

- Room 表 `add_conversations(id, title, created_at, updated_at)` + `add_messages(id, conversation_id, role, text, photo_uri, voice_duration, draft_json, field_count, created_at)`
- AddViewModel 启动 → `newConversation()` 创一段新对话，写到 Room
- 每条 AddMessage（assistant / user / user_photo / user_voice / draft_cta）都即时 `appendMessage(convoId, ...)` 落盘
- 历史抽屉里点旧对话 → `openConversation(id)`，从 Room 读 messages + 解析回 AddMessage / 更新 conversationTitle / 还原 draft
- AI 调用时 `runExtract()` 把当前对话最后 20 条文字消息（assistant + user + voice 转写 + 已有 draft 的简描）作为 `priorTurns` 传 `AiClient`；UserPhoto 不放 prior（图片仍走当条 image block）
- `AnthropicClient.buildPayload` 在 messages 数组前面塞 prior turns；`OpenAiClient.buildPayload` 在 system + user 之间插 prior

## 拍照 + 多选

- AndroidManifest 加 `<uses-permission CAMERA>` + `<uses-feature camera.any>` + `FileProvider` (`${applicationId}.fileprovider`，路径 `xml/file_paths.xml` 暴露 `files-path captures/`)
- EditScreen.PhotoSection 顶部一行两个 terra 描边按钮：📷 拍照 / + 选照片
- 拍照路径：`launchCamera(context, takePicture, onPending)` 先生成 `filesDir/captures/<uuid>.jpg`，FileProvider URI 喂给系统相机；返回 success → `onAddPhoto(uri)` 把那张图复制进 `filesDir/photos/<itemId>/`
- 多选：`PickMultipleVisualMedia(maxItems = 9)` → `onAddPhotos(List<Uri>)`；DetailViewModel 一次性 IO 复制 + 单次 upsert
- CAMERA 权限缺失时弹原生权限对话；Photo Picker 不需要 READ_MEDIA_IMAGES 但 vivo OEM 兜底，仍保留

## 全屏看图 + callout

- DetailScreen 影集 tab 缩略图点击 → 弹 `FullscreenPhotoViewer(photos, initialIndex, callouts, onSaveCallout, onClose)`
- viewer 行为：HorizontalPager 翻页 / `detectTransformGestures` 双指缩放 1×–5× + 平移 / 双击 1×↔2.5× / 长按某点弹文字输入
- callout 数据：`PhotoCallout(x, y, text)`，x/y 归一化到 0..1；存在 `Item.callouts: Map<String, List<PhotoCallout>>`
- 显示：terra dot + 半透明 paper 气泡 + ink 文字
- 删除照片时同步清掉 `item.callouts - path`

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
- APK 还是 ~13 MB，debug 签名
