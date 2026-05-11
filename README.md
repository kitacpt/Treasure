# Treasure

> *a private cabinet of things owned, used, & remembered*

一个 Android 应用，用来记录个人爱好装备：羽毛球拍、相机镜头、租过的车、电子设备 …… 每件东西有它的样貌（一张博物馆线描风的矢量图）、参数表、和一条用过的故事线。

视觉灵感是 19 世纪自然博物馆的图鉴版画 —— 细线勾勒、淡彩平涂、罗马数字标注线。

## 从这里开始

新人 / 新一轮 agent 进来按这个顺序读：

1. [`agent.md`](agent.md) —— 当前状态、做了什么、接下来做啥（**先读这个**）
2. 浏览器开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html) —— v1 视觉规格（主体设计）
3. 浏览器开 [`prototype/add-page-v2/project/Treasure.html`](prototype/add-page-v2/project/Treasure.html) —— v2 录入页设计稿
4. [`docs/dev-loop.md`](docs/dev-loop.md) —— 构建 / 装机 / vivo 调试 / 内循环 / 权限调试
5. [`docs/product.md`](docs/product.md) → [`docs/visual-language.md`](docs/visual-language.md) → [`docs/architecture.md`](docs/architecture.md)
6. [`docs/adr/`](docs/adr/) —— 6 份决策记录
7. [`openspec/`](openspec/) —— cycle 0001-0028 的 proposal / spec / notes

## 当前状态（2026-05-09）

**cycle 0001 → 0028 全部落地**。14 MB debug APK，装到 vivo X200 Pro mini 上端到端跑通：

- 6 个内建品类（羽毛球 / 摄影 / 汽车 / 电子产品 / 咖啡 / 酒水），16 张博物馆线描风插画；**cycle 0026 起在图鉴页右上小红点入口可以管理分类显示/隐藏 + 自定义新分类**（Schema v9 加 `category_prefs` 表，含 Migration 种子）；**cycle 0027 起自定义分类真正能装物品** —`Item.category` 由 enum 改 String id，AI prompt 喂动态 categoryHints，删自定义分类时把物品 rehome 到电子产品兜底；**cycle 0028 起 Manager 改长按拖动**（同段拖动改排序 / 跨分割线 toggle 隐藏），编辑页顶部插画必填，Portal doorway 永远用分类的"基础图"
- 主屏 4 tab 横滑切换：门厅 / 图鉴 / 录入 / 设置（HorizontalPager）；Detail / Edit 是 push 上来的覆盖屏
- Detail 抽屉（历史 / 参数 / 影集）+ 明信片翻面；影集点缩略图 → 全屏 viewer（横滑翻页 / 双指缩放 / 长按图加注 / 长按已有标注改或删，cycle 0012）
- Detail 右上点 → Edit 单页表单，与手动录入共用 EditPageHeader + SectionDivider；状态 / 品类 / 历史类型用统一的 `InlineDropdown`，不再换行
- Edit 实拍：📷 拍照（FileProvider + 直调系统相机）+ + 多选照片（最多 9 张）
- Add (RECORD) chat-first：对话已落 Room（add_conversations / add_messages）；切回历史抽屉里点旧对话能 reload；**cycle 0022 起进入 Record tab 默认续上次对话**（不再每次新建空壳）；发 URL 时聊天里实时显示 "正在抓取 jd.com…" → "✓ 已抓取 jd.com · 1.2K 字" / "⚠ 防爬挡住"；**cycle 0023 起聊天里发的图片单击可全屏预览**（复用影集那边的 FullscreenPhotoViewer，多图可横滑）；**cycle 0024 起"会话 = 草稿"**：AI 提案先以 DraftCta 落到聊天里给 [采用]/[不要]，采用后才升格成 confirmedDraft，下一次 AI 在它上面叠加而不是重写；"手动" 按钮也走 Refine 改 confirmedDraft；"确认收入" 才真把草稿固化成 Item
- AI：多轮 — `extractItemDraft` 现在带 `priorTurns`，把当前对话最后 20 条文字作为上下文喂回模型；Anthropic / OpenAI / Kimi · Moonshot / DeepSeek / 通义千问 / 智谱 GLM / Xiaomi MiLM / 自定义 共 8 个 preset，BYO key 存 EncryptedSharedPreferences；Settings 抽屉 "高阶" 段可调 temperature 和 thinking（cycle 0014）；cycle 0022 起 Settings 摘要卡 / 编辑抽屉根据 model 名启发式显示「🖼 多模态 / 纯文本」 pill；cycle 0023 起 prompt 放开 hero spec 模板（AI 按物品挑最重要的 4 条），草稿页全面镜像 Edit 页 — AI 填什么字段就显什么，不再固定 9 行
- 手动录入：4 品类模板，顶部居中 124dp 大插画 + 56dp 横滚选项；italic tagline + 每个字段单位 / 示例 hint
- Settings：单张摘要卡 + 连通 pill + 底部抽屉编辑
- 真实照片存 `filesDir/photos/<itemId>/<uuid>.jpg`；相机直拍中转 `filesDir/captures/`；callout 数据 `Map<path, List<{x, y, text}>>` 跟 item 一起入库
- Schema **v9**（Room；cycle 0026 加 `category_prefs` 表 + Migration 种子 6 内建分类）；从 cycle 0010 起 `exportSchema = true`，Migration 写在 `core/room/Migrations.kt`，schema JSON 在 `core/schemas/`，不再 destructive — 见 [ADR-0006](docs/adr/0006-schema-migrations.md)
- 16 个博物馆线描插画（含 cycle 0011 加的 espresso machine / coffee grinder / coffee bean / wine bottle / cocktail glass）+ 平面圆环 app 图标（cycle 0026 回到 cycle 0013 那版：23px 圆 + gold gradient + rune/tick 装饰，3D 几版尝试作废）
- 8 条种子数据（首启写入），edge-to-edge，控制岛在 Detail / Edit 屏自然隐藏

GitHub：<https://github.com/kitacpt/Treasure>

## 仓库布局

```
treasure/
├── prototype/      Claude Design 导出的可点击 HTML 原型，作为活的视觉规格
├── android/        Android app（Kotlin + Jetpack Compose；:app + :core）
├── backend/        FastAPI 同步服务（占位，cycle 0003+ 才会接通）
├── docs/           长期指引 —— product / architecture / visual-language / dev-loop / ADRs
├── openspec/       变更周期提案（一个 cycle 一个文件夹）
├── scripts/        bootstrap.sh / prototype-serve.sh / serve-apk.sh
└── agent.md        滚动更新的现状交接
```

## 关键决策（一句话版）

- **平台**：Android 原生，Kotlin + Jetpack Compose（[ADR-0001](docs/adr/0001-android-native.md)、[ADR-0002](docs/adr/0002-jetpack-compose.md)）
- **数据**：Local-first，Room 为权威源；FastAPI 同步层可选，先搭脚手架（[ADR-0003](docs/adr/0003-local-first-with-optional-sync.md)）
- **AI**：用户自带 API key（Anthropic / 模型 / Key），设备直连 provider，不走代理（[ADR-0004](docs/adr/0004-byo-ai-key.md)）
- **插画**：种子物品的 SVG 打包进 app；用户新增物品的插画由配置好的 AI 生成（[ADR-0005](docs/adr/0005-museum-illustration.md)）
- **Schema**：从 cycle 0010 起停止 destructive，每改 schema 必须 bump version + 写 Migration + 提交 schema JSON（[ADR-0006](docs/adr/0006-schema-migrations.md)）
