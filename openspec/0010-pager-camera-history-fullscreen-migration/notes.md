# Cycle 0010 · notes

## 文件改动一览

新建：

- `app/.../ui/components/InlineDropdown.kt` — 共享下拉
- `app/.../ui/main/MainScreen.kt` — 4-tab pager
- `app/.../ui/photo/FullscreenPhotoViewer.kt` — 全屏图 + callout
- `core/.../room/ConversationEntity.kt` — 对话表
- `core/.../room/ConversationDao.kt`
- `core/.../room/Migrations.kt` — 5→6 / 6→7
- `core/.../repo/AddConversationRepository.kt`
- `core/.../domain/PhotoCallout.kt`
- `core/schemas/com.treasure.core.room.TreasureDatabase/{5,6,7}.json` — schema baseline
- `app/src/main/res/xml/file_paths.xml` — FileProvider
- `docs/adr/0006-schema-migrations.md`

主要修改：

- `core/build.gradle.kts` — KSP arg `room.schemaLocation`
- `core/.../room/TreasureDatabase.kt` — exportSchema / addMigrations / 新 entities
- `core/.../room/ItemEntity.kt` — `calloutsJson` 列 + JsonCodec.encode/decodeCallouts
- `core/.../domain/Item.kt` — `callouts: Map<String, List<PhotoCallout>>`
- `core/.../ai/AiClient.kt` — `priorTurns: List<AiTurn>`、新 `AiRole` / `AiTurn`
- `core/.../ai/AnthropicClient.kt`、`OpenAiClient.kt` — buildPayload 接 prior
- `app/.../TreasureApp.kt` — 多挂一个 `conversationRepository`
- `app/.../ui/add/AddViewModel.kt` — 重写：持久化 + multi-turn + openConversation
- `app/.../ui/add/AddRoute.kt` / `AddChat.kt` — 接 `onPickConversation` + StateFlow recents
- `app/.../ui/add/CategoryForm.kt` / `CategoryTemplate.kt` — 顶部插画选择器 + heroVectorOptionsFor
- `app/.../ui/edit/EditScreen.kt` — InlineDropdown / PhotoSection 改双按钮 / kindLabelZh
- `app/.../ui/detail/DetailScreen.kt` — DrawerContent 加 `onOpenPhoto`，AlbumList itemsIndexed → 弹全屏 viewer
- `app/.../ui/detail/DetailViewModel.kt` — `addPhotos(List<Uri>)`、`addCallout(...)`
- `app/.../ui/nav/Routes.kt`、`TreasureNavHost.kt` — 三条路由
- `app/src/main/AndroidManifest.xml` — CAMERA / camera.any / FileProvider

## 设计取舍

### 4-tab pager 而不是 NavHost 子路由

之前每个 tab 都是 NavHost 顶级路由，用 popUpTo + launchSingleTop 切。问题：

1. 不能左右滑切换 — 用户明确要这个
2. tab 间过渡是 NavHost 的 slide 转场，不是页面 swipe — 视觉错位
3. 切回前一个 tab 会重建 vm（NavHost 默认行为）

新结构 NavHost 只剩 Main / Detail / Edit 三条；Main 是 pager；pagerState 同时被控制岛和手势驱动。Detail / Edit 还是 push（动画用原 slide 转场），retainBackStack 自然恢复 pager 状态。

代价：从 Detail 返回时如果想 “回到我刚看的 Grid 位置 + 同一个品类”，需要 pager 状态保留。`rememberSaveable`/`rememberPagerState` 已经做到，gridCategoryId 也 saveable。

### Schema migration 仅从 v5 起

cycle 0001-0009 一直 destructive，所以 v1-v4 实际没有过持久数据，写它们的 migration 没有用户数据可迁。这一刀只承诺：v5 是 baseline，v5 → 之后的每一步都写 Migration + schema JSON。Anyone running an old build with destructive 5/6/7 already has a v7-clean schema.

Settings / 录入页对话历史 之前不存在，在 v6 加表是无损的（创建空表）。Callout 在 v7 加列时给 default `'{}'`，旧数据 toDomain 解析空 map。

### 历史对话表设计

权衡：放在 items 表里？太杂。独立两张：

- `add_conversations` 主表：id / title / 时间戳
- `add_messages` 子表（FK 用 conversation_id 索引，没有真实 FK 约束 — 简化）；payload 字段按角色取舍而不是 union JSON，这样 `text` `photo_uri` `voice_duration` `draft_json` `field_count` 各有空间，反序列化也不用 sealed-class 编码

`appendMessage` + 每条都 `upsert(conversation)` 来更新 `updated_at`：插入开销 vs 历史抽屉的 “按时间最近” 排序逻辑，前者赢。

### 多轮 prior_turns 范围

`buildPriorTurns` 取最后 20 条文字消息。AddMessage.UserPhoto 跳过（图片单独走当条 image block）；DraftCta 用一句概括 “已经替用户写出一份草稿” 替代 — 否则 prior 会塞下整个 draft JSON 浪费上下文。

20 条是经验值。Cycle 0011 看下用户反馈再 tune。

### Callout 用归一化坐标 + box bounds

理论上应当根据图片实际 fit 区域算 normalized 坐标（图比例 ≠ 屏比例时空白区不该受 callout）。这一版偷懒：以 viewport box 为坐标系。后续如果 callout 错位明显，再算图比例。

scale / pan 也只在当条 page 里有效；翻到下一页 reset。这是符合直觉的。

### 拍照 vs 多选并列

放在 PhotoSection 顶部并排两个等宽按钮。以前的 “大 +” 占一格略小气；现在把 “给一张拍 / 给一组挑” 两种意图前置，给 photo grid 节省一格。

按钮的 “📷” 是 emoji 而不是 Canvas 路径，跟 existing CameraGlyph 不太一致。考虑过统一成 vector，这一版先省事。

## 验证

### 构建

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）。

### 手测路径

- 装机后从启动到首屏：直接到 Portal（pager page 0）
- 横滑：Portal ↔ Grid ↔ Add ↔ Settings；控制岛同步高亮
- 点门厅 doorway：Grid 的 category 应是被点的那个
- Edit 屏点 “品类”：折叠下拉 → 点 “电子产品” 不再换行
- Edit 屏 “历史” → “新增” → “类型”：dropdown 显示中文 label
- Add 屏未配置 AI：顶部一行字 banner，点 → 滑到 Settings
- 手动录入：选品类 → 顶部大插画 + 下面横滚小图，点小图 / 点大图都切换
- Edit 屏 “实拍” → 点 📷 拍照（首次会要 CAMERA 权限）；拍完返回看见缩略图
- Edit 屏 “实拍” → 点 + 选照片 → 多选 3 张 → 全部进网格
- Detail 屏抽屉 “影集” → 点缩略图 → 全屏 viewer 弹起；横滑翻页 / 双指放大 / 双击 / 长按弹文字 “左侧擦痕” → 保存 → 退出再进 viewer，标注还在

### Schema 文件

```
$ ls core/schemas/com.treasure.core.room.TreasureDatabase
5.json  6.json  7.json
```

### 已知 / 留待

- 升级用户体验（v5 → v7）需要在真机验证一遍：装着 cycle 0009 APK 的设备上跑 `installDebug`，物品数据 / 照片应当还在
- MigrationTest 没写（依赖 androidTest source set）；ADR-0006 说 cycle 0011 补
- Pager swipe 在 Detail 抽屉拖动时有可能误触：抽屉的 verticalDrag 应该 consume，HorizontalPager 应当不接收。手测正常，注意监控
