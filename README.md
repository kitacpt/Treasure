# Treasure

> *a private cabinet of things owned, used, & remembered*

Treasure 把你拥有过的"心头好"——球拍、相机、镜头、租过的车、咖啡机、酒、电子产品——整理成一本私人的博物馆图鉴。每件物品有它的样貌（一张博物馆线描风的矢量图）、关键参数、和一条用过的故事线。

不是清单。不是评测。不是社交。**是一个人收藏自己时间的方式。**

视觉灵感来自 19 世纪自然博物馆的图鉴版画 —— 细线勾勒、淡彩平涂、罗马数字标注。

GitHub：<https://github.com/kitacpt/Treasure> · 当前 **v1.0**（debug APK / Android 8.0+）

---

## ✦ 它能做什么

- **6 内建分类**（羽毛球 / 摄影 / 汽车 / 电子产品 / 咖啡 / 酒水），16 张博物馆线描插画
- **自定义分类**：从相册挑代表图，自建任意"图鉴"（书、文具、香水、模型……都行）
- **AI 录入**（chat-first）—— 多模态：拍照 / 选图 / 录音 / 粘 URL / 打字一起喂；AI 一次能识别多件物品，按 `photo_assignments` 把图分配给具体物品的影集；后续聊天里描述变更，AI 返回**增量**，代码侧合并到原物品，影集不丢
- **草稿与工作集**：一段对话 = 一个工作集，多件物品并行；每张 DraftCta 卡片单独 `[保存草稿]` / `[直接录入]`
- **非破坏裁剪**：原图原样存盘，裁剪框只影响显示；想换裁剪随时可改
- **多 AI 服务并存**：BYO key（Anthropic / OpenAI / Kimi / DeepSeek / Qwen / GLM / Xiaomi / 自定义 OpenAI-兼容端点）；设置页横滑切换，单个标记为"默认"；录入页 chip 临时切本会话用谁
- **本地优先**：Room 数据库 + 文件系统，无云依赖；BYO-key 直连模型厂商，不走代理

---

## ✦ App 一日游

| 屏 | 干什么 |
|---|---|
| **Portal**（门厅） | 大字 Treasure + 总数三连 + 4 扇分类大门 + Latest entry。仪式感的家 |
| **Grid**（图鉴） | 某分类的 2 列卡片网格；右上 🔍 搜索 / 🔧 分类管理；长按进编辑态，可批量选 / 拖动调序 / 删除 / 一键扔到录入页 |
| **Detail**（明信片） | Hero 线描 + 4 行关键参数；点 hero 翻面看真实照片影集；上滑抽屉切参数 / 历史 / 影集三 tab |
| **Edit**（编辑） | 单页表单：基础 / 标签 / 插画 / 参数（拖动选前 4 hero）/ 历史 / 实拍（拍照或多选）/ DANGER ZONE |
| **Record**（录入） | chat-first。chip 行 `[+ 附件]` `[✦ 模型]`；附件抽屉 = 图片 / 文件 / 物品三选一；语音是点击麦克风进语音态、长按输入框录、松手发；emoji 在输入框内右；模型 chip 切换本会话用谁 |
| **Settings**（设置） | 横滑切多份 AI 服务；幽灵卡 → 添加 provider；每份卡有连通灯 / 调整 / 设为默认；编辑抽屉内含移除 |

主屏底部是 4 颗胶囊的"控制岛"，4 个 tab 横滑切换。Detail / Edit / Search / CategoryEditor / Crop 是 NavHost push 路由，控制岛在那些屏自然隐藏。

---

## ✦ 技术栈

| 维度 | 选择 |
|---|---|
| 平台 | Android 原生（API 26+），Kotlin 2.0 + Jetpack Compose（BOM 2024.10） |
| 模块 | `:app`（UI / 导航 / 主题 / 插画）+ `:core`（纯 Kotlin/JVM：domain / Room / repo / AI / web） |
| 数据 | Room v16（exportSchema，从 v5 起 5 条 Migration，没再 destructive） |
| 序列化 | kotlinx-serialization-json（specs / history / photos / callouts / photo_crops / photo_assignments / AI profiles 都是 JSON 列） |
| 加密 | `androidx.security:security-crypto`（API key 存 EncryptedSharedPreferences） |
| 网络 | 手写 OkHttp 客户端，直连 Anthropic / OpenAI / OpenAI-兼容端点 |
| 图片 | Coil（影集 + 头像 + crop 显示） |
| 音频 | MediaRecorder（AAC m4a） / MediaPlayer，前台保活 service（API 34+ `FOREGROUND_SERVICE_DATA_SYNC`） |
| DI | 不引 Hilt/Koin，手写 ServiceLocator（`TreasureApp`） |

---

## ✦ 给 AI agent / 新人开发者

**进来按这个顺序读，先全局再局部。**

1. **[`agent.md`](agent.md)** —— 当前状态、做完了什么、下一刀候选。**滚动更新，先看这个**
2. **[`docs/README.md`](docs/README.md)** —— 文档分布索引
3. **[`docs/dev-loop.md`](docs/dev-loop.md)** —— 构建 / 装机 / vivo 调试 / 内循环 / adb 速查
4. **[`docs/architecture.md`](docs/architecture.md)** —— 模块划分 / 数据流 / Schema 演化 / AI 流水线 / 拖动与录入数据流
5. **[`docs/product.md`](docs/product.md)** —— 产品定位与不做什么
6. **[`docs/visual-language.md`](docs/visual-language.md)** —— 配色 / 字体 / 插画规则 / 控制岛规格
7. **[`docs/adr/`](docs/adr/)** —— 6 份钉死的决策记录（推翻要写新 ADR supersede）
8. **[`openspec/`](openspec/)** —— cycle 0001-0031 的 proposal / spec / notes（0032+ 写在 [`agent.md`](agent.md) 里）
9. **[`prototype/`](prototype/)** —— Claude Design 导出的可点击 HTML 原型，**视觉规格的唯一权威**

**改视觉之前**先打开 [`prototype/project/Treasure.html`](prototype/project/Treasure.html) 对照（双击在浏览器打开）。

**改 schema 之前**先读 [`docs/adr/0006-schema-migrations.md`](docs/adr/0006-schema-migrations.md) —— 从 v5 起 Migration 是硬规矩，绝不 destructive。

**改 AI prompt 之前**先看 [`android/core/src/main/java/com/treasure/core/ai/Prompts.kt`](android/core/src/main/java/com/treasure/core/ai/Prompts.kt) 和 [`agent.md`](agent.md) 里"会话 = 草稿 / MODIFY 增量"的协议描述。

---

## ✦ 仓库布局

```
treasure/
├── README.md          这一份（人类入口）
├── agent.md           滚动更新的现状交接
├── docs/              长期指引（product / architecture / visual-language / dev-loop + 6 ADR）
├── openspec/          cycle 0001-0031 提案历史（0032+ 在 agent.md）
├── prototype/         Claude Design 导出的 HTML 原型（活的视觉规格）
├── android/           Android app
│   ├── app/           :app — Compose 屏幕 / 导航 / 主题 / 插画 / 数据存储 / 音频 / 后台 service
│   ├── core/          :core — 领域模型 / Room / Repository / AI 客户端 / PageFetcher（纯 Kotlin/JVM）
│   └── gradle/        version catalog
├── backend/           FastAPI 同步占位（未启用，预留给 ADR-0003 的可选同步层）
└── scripts/           bootstrap.sh / serve-apk.sh
```

---

## ✦ Quick start

```bash
# Linux 开发机（首次）— 详细见 docs/dev-loop.md
sudo apt install openjdk-17-jdk-headless adb
export ANDROID_HOME=$HOME/Android/Sdk

# 构建 debug APK
cd android
./gradlew :app:assembleDebug
# → android/app/build/outputs/apk/debug/app-debug.apk（~14 MB，debug-signed）

# 装到当前连着的 Android 设备
./gradlew :app:installDebug
adb shell am start -n com.treasure/.MainActivity
```

启动后：**设置 tab → 调整 → 粘 Anthropic / OpenAI / 国内兼容端点的 API key → 测试连接**。然后就能用 AI 录入了。

---

## ✦ 一句话决策记录

来自 [`docs/adr/`](docs/adr/)（推翻请新写一份 supersede，不要改老的）：

- **平台**：Android 原生，Kotlin + Jetpack Compose（[ADR-0001](docs/adr/0001-android-native.md) · [ADR-0002](docs/adr/0002-jetpack-compose.md)）
- **数据**：Local-first，Room 是权威源；FastAPI 同步层可选先搭脚手架（[ADR-0003](docs/adr/0003-local-first-with-optional-sync.md)）
- **AI**：BYO key，设备直连 provider，不走代理（[ADR-0004](docs/adr/0004-byo-ai-key.md)）
- **插画**：种子物品的 SVG 打包进 app；用户新增物品的插画由 AI 生成（[ADR-0005](docs/adr/0005-museum-illustration.md)）
- **Schema**：从 v5 起停止 destructive，每改 schema 必须 bump + 写 Migration + 提交 schema JSON（[ADR-0006](docs/adr/0006-schema-migrations.md)）

---

## ✦ 项目纪事

| 里程碑 | 日期 |
|---|---|
| Cycle 0001 — MVP（Portal + Grid + Detail + Room） | 2026-05-06 |
| Cycle 0010 — Schema migration 制度落地（v5 起 exportSchema） | 2026-05-08 |
| Cycle 0024 — "会话 = 草稿"协议重构（DraftCta + baseline） | 2026-05-10 |
| Cycle 0029 — 全屏 CategoryEditor + Search 路由 + BackHandler 栈 | 2026-05-11 |
| Cycle 0032 — AI 多 action 协议（一次返多件 + create/modify） | 2026-05-13 |
| Cycle 0034 — 多模态：多图 + 语音 + 非破坏裁剪 + MODIFY 增量合并 | 2026-05-14 |
| **v1.0 release** | **2026-05-14** |
| Cycle 0035 — 多 AI 服务管理 + 录入页 chip/drawer 重做 + Grid 拖动重写 | 2026-05-15 |

完整 cycle 索引见 [`openspec/README.md`](openspec/README.md) 和 [`agent.md`](agent.md) 的 "Cycle 一览" 表。
