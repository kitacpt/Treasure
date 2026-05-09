# Cycle 0022 · notes

## 文件改动

- `app/.../ui/add/AddViewModel.kt`
  - 加 `AddMessage.SystemNote` + `NoteTone` enum
  - `init { newConversation() }` → `init { viewModelScope.launch { ...resume or new... } }` + 新 `resumeConversation(c)`
  - `sendText` 在 fetch 路径里 append "正在抓取" SystemNote，fetch 完后用 `replaceLastWorkingNote` 替成结果
  - 新 `appendTransientNote` / `replaceLastWorkingNote`：SystemNote 只塞 UI state，不入 Room
  - `toDomainMessage` 返回 nullable，SystemNote → null（persist 跳过）
  - `buildPriorTurns` 跳过 SystemNote
  - `persist` 处理 null
- `app/.../ui/add/AddChat.kt`
  - `MessageRow` 加 `is AddMessage.SystemNote -> SystemNoteRow(...)` 分支
  - 新 `SystemNoteRow` composable：小字斜体居中，按 tone 着色
- `core/.../web/PageFetcher.kt`
  - 抽出 `decodeWithBestCharset(buf, length, headerCharset?)`
  - 新 `META_CHARSET_REGEX` / `META_HTTP_EQUIV_REGEX`
  - 不带 Content-Type charset 时 → ISO-8859-1 probe 头部 4KB → grep meta → 重解
- `app/.../data/AiProviderPreset.kt`
  - 新顶层函数 `modelSupportsVision(model: String): Boolean`
- `app/.../ui/settings/SettingsScreen.kt`
  - import `modelSupportsVision`
  - 摘要卡：原 `InfoRow("Model", ...)` → `ModelRow(saved.model)` — Column + 右下挂 `VisionChip`
  - 编辑抽屉 Model 输入下面挂 `ModelCapabilityHint(model)` 一行小字
  - 新 `ModelRow` / `VisionChip` / `ModelCapabilityHint` composables

## 设计取舍

### init suspend vs blocking

`observeRecent(1).first()` 是 `suspend` 调用。我用 `viewModelScope.launch {}` 把 init 包成异步。这意味着 VM 被 Compose 收到时 `_state` 短暂是默认的 `AddUiState()`（空 messages、空 conversationId），毫秒级之后才填上数据。Compose 收到 state change 重 compose，看起来跟以前同步 newConversation 设置 state 一样快 — 实测无感。

替代方案是把 first conversation 通过 SavedStateHandle 同步缓存，但当前没有这层抽象，工程不值得。

### SystemNote 替换 vs 直接 append

考虑过两条路：
1. 简单：append "正在抓取" + append 结果（两行）
2. 现在：append "正在抓取"，fetch 完原地替

选 2，因为 (a) 一行更干净，(b) 如果用户连发多 URL（罕见但合理）每次都两行就刷屏。

替换实现是 `indexOfLast { tone == Working }` — 遇到不需要替换的特殊情况（fetch 结束时 Working 已被某种 race 清掉了）就 append 一行，不阻塞。

### charset probe 用 ISO-8859-1 不用 ASCII

ISO-8859-1 (Latin-1) 是 1byte 1char 的双射，能保所有 byte 不丢；ASCII 在 byte > 127 时会出 ?。HTML 头部 meta 标签 attribute name + value 都是 ASCII 子集，用 ISO-8859-1 解出来再 regex 是稳的。

### 为什么 modelSupportsVision 不挂在 enum 上

启发式吃的是 model 名（用户字段），不是 preset enum。同 preset 不同 model 完全可能一个吃图一个不吃 — 比如 OpenAI preset 可以填 "gpt-4o"（吃图）也可以填 "o1-preview"（不吃）。所以函数签名接 model 字符串、跟 preset 解耦。

### 不做流式

已经在 proposal.md 里讲清楚。再加一句：当前 AiClient.extractItemDraft 是 `suspend fun .. : Result<ItemDraft>`，要改流式得拆成 Flow + 中间增量状态机 + tool-call delta 重组。三家 provider 行为还都不一样（Anthropic content blocks vs OpenAI delta vs OpenAI 兼容的简化版）。投入产出比低。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（13 MB）

### 手测

1. 装新 APK，杀掉，重启 → 进 Record tab → 应直接续上次跑的对话（不再是 "New entry · HH:MM 你好。把新东西的照片..."）
2. 录入页发一条带 jd.com 链接的消息 → 应立刻冒一行小字 "正在抓取 jd.com…" → 短暂 → 替成 "✓ 已抓取 jd.com · X 字"（或防爬警告）
3. 设置页打开 AI 编辑抽屉，把 Model 字段从 "claude-opus-4-7" 换成 "moonshot-v1-8k" → 下面提示行从 "🖼 多模态..." 变成 "纯文本模型..."；改回 → 切回多模态
4. 摘要卡 Model 行：当前 model 是多模态时，应在 model 名下挂个 "🖼 多模态" 椭圆 pill
5. (charset) 找一个 GBK 编码的老站点（如部分小网店），分享链接到 Treasure → SystemNote 应是 "✓ 已抓取..."，AI 回复应该是中文识别而不是乱码
