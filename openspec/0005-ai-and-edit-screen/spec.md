# Cycle 0005 · spec

- **状态：** done
- **完成：** 2026-05-07

## AI 服务

- [x] `core/ai/AiClient` interface
- [x] `core/ai/AnthropicClient` 实现（OkHttp + kotlinx-serialization；tool-use 强制结构化）
- [x] 系统提示按 4 品类约定 hero spec 标签顺序
- [x] `app/data/SettingsStore`：EncryptedSharedPreferences 存 apiKey + model；`hasKey()`、`clear()`
- [x] `TreasureApp.aiClient()` 工厂：从 SettingsStore 拿 key → 构造 AnthropicClient；没 key 返回 null
- [x] `core` 加 `okhttp` 依赖，`app` 加 `security-crypto-ktx`，AndroidManifest 加 `INTERNET`
- [x] APK 大小升到 ~13 MB（OkHttp + security 拉的家族）

## Settings 屏

- [x] 替换 `SettingsStubScreen` → `SettingsRoute`
- [x] 表单：Provider readonly（Anthropic）/ Model 文本 / API Key 密码框（可显示/隐藏）
- [x] 保存按钮：disabled 直到 key + model 都非空
- [x] 测试按钮：调 `extractItemDraft("测试连接...")`；显示 idle / running / ok / failed 文案
- [x] DANGER ZONE：清除所有设置（无二次确认，因为不会丢图鉴数据）

## AddRoute AI 模式

- [x] AI tab 显示 `AiChatPanel`
- [x] 没配 key 时顶部展示"AI 录入需要先配置 API key · 去设置"卡片
- [x] 配了 key 后：助手气泡（固定）+ 输入框 + "+ 选张图" + "发送"
- [x] 发送 → `AddViewModel.extractDraft(text, imageUri)`：复制图为 bytes → AnthropicClient.extractItemDraft → ItemDraft
- [x] 成功 → 弹 ModalBottomSheet，CategoryForm 用 ItemDraft 预填 brand / model / nickname / oneLiner / 4 hero spec values
- [x] 失败 → 红字错误提示在 panel 底部
- [x] CategoryForm 顶部 label 显示 "AI 预填 · 摄影"（vs "新增 · 摄影"）

## 编辑入口

- [x] 详情屏左上角：BackArrow 右边一个 12dp terra 实心圆（`DotButton`）
- [x] 点点 → `nav.navigate(Routes.edit(itemId))`，左右滑动转场
- [x] Edit 屏控制岛隐藏

## 抽屉变只读

- [x] tabs 缩到 3 个：历史 / 参数 / 影集（"基础"和"设置"全去掉）
- [x] 历史 tab：纯 timeline 展示，无 + 加 / 无 tap edit / 无长按删
- [x] 参数 tab：纯展示 hero specs + specs map
- [x] 影集 tab：3 列网格 + 纯展示，**无 + tile / 无长按删**
- [x] 各 tab 空状态文案统一为"... · 点左上 · 编辑添加"

## EditScreen

- [x] 单页 LazyColumn 长滚
- [x] 顶部：BackArrow + 右侧"保存"按钮（dirty=terra / clean="已保存" 灰）
- [x] Sections（label + hairline 分割）：基础 / 时间 / 标签 / 插画 / 关键参数 / 完整参数 / 历史 / 实拍 / DANGER ZONE
- [x] 基础：品牌 / 型号 / 昵称 / 简介（label 左 56dp、value 右下划线）
- [x] 时间：购入 / 出手（同上）
- [x] 标签：状态 chips × 3，品类 chips × 4
- [x] 插画：横滚 14 个 72dp HeroIllustration 缩略，选中态 terra 1.5dp
- [x] 关键参数：4 行（label + value 双 InlineField）
- [x] 完整参数：变长行（key + value + −）+ `+ 加一行 参数`
- [x] 历史：行（日期 + kind badge + 标题 + 备注），tap edit、长按删除（dialog）+ `+ 加一条 历史`；保存时按日期排序
- [x] 实拍：3 列网格（用 chunked Row 而非 LazyVerticalGrid，避免嵌套滚动），第一格 + tile，其余照片，长按删除（dialog）
- [x] DANGER ZONE：删除按钮 + 二次确认 → popBack
- [x] form 改 → 顶部"保存"启用 → 点击 commit；history / photos 即时 commit

## 验证

- [x] `./gradlew :app:assembleDebug` 通过（v0.8.0，13 MB）
- [ ] 装机：进设置 → 输 API key → 保存 → 测试 → ✓ 连接成功
- [ ] AI 录入：进 Add → AI tab → 写"Yonex Astrox 99 Pro 桃田款" → 发送 → 回来弹预填表单 → 保存 → 跳新 Detail
- [ ] 详情：左上角小点 → 进 Edit → 改昵称 → 保存 → 回 → 标题更新
- [ ] 抽屉：上滑 → 3 tabs 都没编辑按钮、没 + 没 −
- [ ] 编辑屏：完整参数加一行 → 保存 → 抽屉参数 tab 看见
- [ ] 编辑屏：长按一张照片 → 确认 → 立即消失 + 文件删

## 不在这一轮

- 真 schema migration（cycle 0006）
- 拍照 / 多选照片
- AI 多轮对话 / 流式响应
- AI 生成博物馆插画
