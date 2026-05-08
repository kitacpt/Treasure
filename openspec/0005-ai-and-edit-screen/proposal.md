# Cycle 0005 · AI 服务接通 + 编辑屏重做

- **状态：** done
- **完成：** 2026-05-07

## 这一刀切什么

cycle 0004 留下两件事 + 一件用户在中途反馈的事：

1. **AI 服务接通**（cycle 0004 留的最高优先级）：录入页 AI 模式接通真实 Anthropic API
2. **设置页接通**（cycle 0004 留的"AI 服务 — coming"占位）：BYO key 表单
3. **重做编辑入口**（用户中途反馈："抽屉里只展示不编辑，编辑功能移到详细页左上角以一个点进入"）

## AI 服务

按 [ADR-0004](../../docs/adr/0004-byo-ai-key.md)：BYO key、设备直连 provider、不走代理。

- **`core/ai/AiClient.kt`** —— interface，方法 `extractItemDraft(text, imageJpegBytes)` 返回 `ItemDraft`
- **`core/ai/AnthropicClient.kt`** —— 用 OkHttp + kotlinx-serialization 手撸的 Messages API 客户端
  - 走 tool-use（强制 `fill_item_draft`），保证返回结构化输出，不需要解析自由文本
  - 系统提示约定 4 品类 + 每品类的 hero spec 标签顺序，跟 `CategoryTemplate` 对齐
  - 支持 vision：image 块作为 base64 + text 块
- **`app/data/SettingsStore.kt`** —— `EncryptedSharedPreferences` 包装，存 `apiKey` + `model`
- **`SettingsRoute / Screen / VM`** 替换 `SettingsStubScreen`：
  - Provider 锁死 Anthropic（cycle 0006+ 加 OpenAI / 自定义）
  - Model 字段（默认 `claude-haiku-4-5-20251001`）
  - API Key 字段（密码可视化，可显示/隐藏）
  - "保存" + "测试" 两个按钮
  - "测试" 调一次 `extractItemDraft("测试连接：随便编一个 AirPods Pro 2")`，成功显示"✓ 连接成功"
  - DANGER ZONE：清除所有设置
- **AddRoute 的 AI 模式接通**：
  - `AiChatPanel` 取代 stub：助手气泡 + 输入框 + "+ 选张图" + "发送"
  - 没配 key 时显示"AI 录入需要先配置 API key · 去设置"卡片
  - 发送 → AddViewModel.extractDraft → ItemDraft → 弹同样的 ModalBottomSheet，但这次 CategoryForm 接受 `initial: ItemDraft?` 预填字段

新依赖：`com.squareup.okhttp3:okhttp:4.12.0`、`androidx.security:security-crypto-ktx:1.1.0-alpha06`。AndroidManifest 加 `INTERNET` 权限。

## 编辑屏重做

cycle 0004 把编辑塞抽屉 4 tab 里的方案被用户否了（"做得非常差"）。重做：

- **抽屉变只读**：3 tabs 历史 / 参数 / 影集（"基础"消失，删除按钮也没了），所有编辑 affordance 全删
- **详情屏左上角加点**（`DotButton` 组件）：在 BackArrow 右边，terra 色 12dp 实心圆，不带文字
- **新路由 `Routes.Edit("edit/{itemId}")`**，从详情点点进入；左右滑动转场（已经全局生效）
- **EditScreen** 单页长滚 + 清晰分区：
  - Section: 基础 / 时间 / 标签 / 插画 / 关键参数 / 完整参数 / 历史 / 实拍 / DANGER ZONE
  - section 标题左侧 + 右侧细线，视觉统一
  - 字段全部"label 左 + 输入框右下划线"行排版（取代 cycle 0004 那一堆框框）
  - 插画选择器：横向滚动 72dp 缩略，选中态 terra 1.5dp 描边
  - 完整参数：每行 `[key] [value] [−]` + 底部 `+ 加一行`
  - 历史：行排版（日期 / kind 字形 badge / 标题 / 备注），tap 编辑、长按删除、底部 `+ 加一条`
  - 实拍：3 列 + tile + 长按删（即时存盘）
  - DANGER ZONE：删除按钮 + 二次确认
- **EditRoute 复用 `DetailViewModel`**（同一个 factory，相同的 `update(Item)` 入口）；表单字段一并 commit、照片 / 历史即时 commit
- 顶部右上角"保存" 在 dirty 时 terra 启用，未变动时显示"已保存"
- 控制岛在 Edit 屏自动隐藏（focused form）

## 不做（cycle 0006+）

- 真 schema migration（cycle 0006 必做）
- AI 生成博物馆插画（用 AiClient 但 prompt 不同，cycle 0007）
- OpenAI / 其它 provider 支持（cycle 0006+）
- 对话多轮（现在是单轮 extract）
- 全屏看图浏览器
- 拍照（直接调相机，跳过相册）

## 验收

详见 [`spec.md`](spec.md)。
