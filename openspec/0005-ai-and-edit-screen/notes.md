# Cycle 0005 · 工作笔记

## AI 接通

- **不用 Anthropic Java SDK**：手撸 OkHttp + kotlinx-serialization 比拉 SDK 整套依赖更轻。Messages API 不复杂，~120 行搞定。
- **强制 tool-use**：`tool_choice = {type: "tool", name: "fill_item_draft"}` 比"约定输出 JSON 然后字符串解析"稳得多。模型必须调工具，response 一定能 parse。
- **prompt 把 hero spec 顺序硬约定**：badminton=[重量,平衡点,中杆,握把] 等，跟 `CategoryTemplate.heroSpecLabels` 对齐。位置映射比 fuzzy label 匹配可靠。
- **vision = base64 image 块**：`java.util.Base64.getEncoder().encodeToString(bytes)`（minSdk 26+ 支持）。不用 android.util.Base64 是为了 :core 模块少绑 Android Util。
- **timeout = 120s call timeout**：vision 慢得很，10s 不够。
- **EncryptedSharedPreferences 1.1.0-alpha06**：`MasterKey.Builder` API 比 1.0.0 的 `MasterKeys` 干净。alpha 标签别看着害怕，已经稳定多年。

## 编辑屏重做

cycle 0004 把编辑塞 4 tabs 抽屉里被用户否了。复盘：

- **抽屉里既展示又编辑** UX 撕裂 —— 用户上滑抽屉本想看一眼内容，结果碰一下就在编辑模式。
- **每 tab 各自有保存按钮** —— 不知道改 A tab 后切到 B tab 是什么状态、为啥还能改。
- **HeroVector 14 chips** —— 视觉跟正文混在一块儿。

cycle 0005 改方案：

- **入口分离**：详情默认全只读；想改 → 点 · → 进 Edit。两件事不混。
- **单页表单 + 一个保存按钮**：所有 form 字段一次提交，符合"我改了一通再保存"的预期。
- **photos / history 即时 commit**：因为它们是"动作"（add/remove），不是"打字"。这个划分自然。
- **section + hairline label**：取代 chunky 分块，节奏统一。
- **label 左下划线 value 右**：取代每个字段一个独立卡片框。密度上去后整页清爽。
- **HeroVector 横滚缩略**：每个 72dp 卡片显示真实绘制；选中 terra 描边。比文字 chip 直观 100 倍。

## 几个坑

- **`mutableStateListOf<HeroSpec>` 替换元素**：HeroSpec 是 immutable data class，要 `list[i] = HeroSpec(...)` 整体替换，不能字段改。
- **LazyVerticalGrid 嵌套在 LazyColumn 里报错**：编辑屏的"实拍"section 我换成了 chunked List + Row 手摆，避免嵌套滚动。
- **ViewModel 复用**：EditRoute 跟 DetailRoute 都用 `DetailViewModel.factory(itemId)`。两个独立实例，但都看同一个 Room flow，互不冲突。
- **`Modifier.size(Dp)` 经常忘了 import** —— 补上。
- **DotButton 实心 vs 空心**：实心 12dp terra 圆点最像"小红点"；空心环不够实体。

## 给下一个 agent

cycle 0006 候选（按优先级）：

1. **真 schema migration**：v1→v4 的 Migration object 全部补上，加 MigrationTest，删 `fallbackToDestructiveMigration()`。再不做就来不及，cycle 0005 已经有真用户数据了。
2. **拍照** + **多选照片**：cycle 0003 留下的；现在只能从相册单选。
3. **OpenAI / 自定义 provider**：cycle 0005 锁死 Anthropic；扩展也容易，AiClient 接口已经在。
4. **AI 生成博物馆插画**：`AiClient.generateIllustration()` 方法 + cache 在 `filesDir/illustrations/<itemId>.svg`；然后把 HeroIllustration dispatcher 在那里读 cache。

模型注：cycle 0005 默认用 `claude-haiku-4-5-20251001`（便宜+快+视觉够），用户可在 Settings 改。如果以后默认想换 `claude-sonnet-4-6` 提质量，改 `AnthropicClient.DEFAULT_MODEL` 就行。
