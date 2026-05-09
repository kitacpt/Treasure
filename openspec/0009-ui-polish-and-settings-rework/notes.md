# Cycle 0009 · notes

## 代码改动一览

| 文件 | 角色 |
|---|---|
| `app/.../res/drawable/ic_launcher_foreground.xml` | 图标缩 ring，留更多 paper 留白 |
| `app/.../ui/components/SectionDivider.kt` *(new)* | 共享分组分割线 |
| `app/.../ui/components/EditPageHeader.kt` *(new)* | 共享编辑屏 header |
| `app/.../ui/add/AddChat.kt` | 头改 Cormorant + NotConfigured 横幅顶部 + LazyColumn bottom inset 修复 + Cormorant italic |
| `app/.../ui/add/CategoryForm.kt` | 用 EditPageHeader + SectionDivider + tagline + 字段 hint |
| `app/.../ui/add/CategoryTemplate.kt` | 加 `tagline` `heroSpecHints`，4 个模板字段升级 |
| `app/.../ui/edit/EditScreen.kt` | 用 EditPageHeader + SectionDivider，删自家 TopBar / Header / Section |
| `app/.../ui/settings/SettingsScreen.kt` | 主页摘要卡 + DANGER ZONE + 抽屉编辑器 |
| `app/.../ui/settings/SettingsViewModel.kt` | saved / draft 双状态 + openEditor / closeEditor |
| `app/.../data/SettingsStore.kt` | 新增 `presetId` |
| `app/.../data/AiProviderPreset.kt` *(new)* | Anthropic / OpenAI / Moonshot / DeepSeek / Qwen / Zhipu / Xiaomi / 自定义 |
| `core/.../ai/OpenAiClient.kt` | `buildUrl()` 兼容多种 base URL 形态 |

## 设计取舍

### 共享组件

之前每屏自家写一份 Section / TopBar / Header，慢慢漂；这一轮 EditScreen 有 internal Section / FieldLabel / LabeledField / InlineField，CategoryForm 自己一套，AddChat header 又是另一套。把分组 + 顶部抽到 `ui/components` 后，新加屏直接复用。

LabeledField / InlineField 还留在 EditScreen 内部——CategoryForm 自己有一份精简版，未来如果要加第三屏（比如添加新 preset 也想用 LabeledField 风格），再抽。

### Settings drawer 而不是子页

抽屉好处：保留主页摘要给用户当 “眼前一份配置都看见”，编辑是临时进入的状态；点摘要卡 / “调整 →” 进，取消 / 保存 / 蒙层 / 物理返回都退出。

劣势：drawer 内部的 Provider 下拉是嵌入式（点同一张卡），不是真正的 Material `ExposedDropdownMenuBox`——仅折叠 / 展开当前卡片内的列表。这是为了避免 drawer 内嵌弹层导致的层级混乱。

### Provider preset URL 准确性

公开 endpoint 我们能查到的：

- Moonshot (Kimi): `https://api.moonshot.cn/v1` ✓
- DeepSeek: `https://api.deepseek.com` ✓ (会自动补 `/v1/chat/completions`)
- Qwen / 通义千问 OpenAI 兼容: `https://dashscope.aliyuncs.com/compatible-mode/v1` ✓
- 智谱 GLM: `https://open.bigmodel.cn/api/paas/v4` （`buildUrl()` 加了 `/v\d+` 兼容才能正确补 `/chat/completions`）
- Xiaomi MiLM: 没有正式公开的 OpenAI 兼容公网 endpoint。preset 给了 `https://api.xiaomi.com/v1` 占位，并标 `baseUrlMandatory` 让用户自己改

如果厂商升级 endpoint，`AiProviderPreset` 一行改即可。

### 字段 hint 显示策略

CategoryForm 的 `LabeledField` 已经有 hint 槽，原本只 “购入” 一个用了。这一轮全部基础字段 + 全部 hero 字段都填上 hint，模板 tagline 在分组前先放一行 italic 介绍。

### Section 字号选择

试过：

1. titleMedium (Cormorant 18sp) 居中加粗黑分割线 → 太喧宾夺主
2. labelSmall sub 9.5sp 左侧 + 单边线 → 太弱、跟段落 caption 没区分
3. **当前**：ink 11sp 2.4 letter-spacing 居中嵌在两侧细线之间 → 像博物馆展柜的小铜牌

## 验证

### 构建

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk` (13 MB)

### 手测要点

- 装机后看 launcher 图标，ring 应有明显 paper 留白
- 录入页头部 “Record” 字号、字体应跟 Grid 的 “Treasure” 同款
- 录入页未配置 AI 时，banner 在 RECORD 下面
- 录入页快速发多条消息，最后一条应不被输入框遮挡
- “手动” → 任一品类，应看到顶部 italic tagline + 字段下面一行灰色 hint
- 详情页右上 dot → Edit，标题区应跟手动录入页同款
- Settings 应只显示一张卡 + DANGER ZONE，点卡片弹抽屉，下拉看到 8 个 preset
