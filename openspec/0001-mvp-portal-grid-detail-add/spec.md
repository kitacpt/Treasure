# Cycle 0001 · Spec · "完成"长什么样

可以一条一条对着勾的验收清单。

## 工程脚手架

- [x] `android/` 是一个 Gradle 工程，命令行 `./gradlew :app:assembleDebug` 能出 APK
- [x] 模块：`:app`、`:core`
- [x] minSdk = 26，targetSdk = 35，Kotlin 2.0.21，AGP 8.7.2，Compose BOM 2024.10.01
- [x] `gradle/libs.versions.toml` 集中管理依赖版本
- [~] `:core` 是 Android library 模块（Room runtime 依赖 android）；纯 JVM 单元测试以后再扩

## 主题

- [x] `app/theme/Color.kt` 定义 token：`paper / ink / terra / card / sub / line`（light + dark 两套）
- [~] `app/theme/Type.kt` 装好 Cormorant Garamond（含 italic）/ Space Grotesk / JetBrains Mono；Noto Sans/Serif SC 暂未打包，靠系统中文回退
- [x] 系统深浅模式自动跟随
- [x] 没有调用 Material3 任何默认色 —— 全是自定义 token

## 数据层

- [~] Room schema：`items` 一张表已通；`history_events` 留给下一刀（Detail 屏需要时再加）
- [x] palette 字段编码（小型分隔符替代 JSON，避免现在引序列化库；hero_specs / specs 留待表单上线时切到 kotlinx.serialization）
- [x] `core/seed/SeedItems.kt` 包含 8 条种子物品（覆盖 4 个品类）
- [x] 首次启动检测数据库为空 → 写入种子；非空不重写（用 `dao.count() == 0` 判断）
- [x] `ItemRepository.items: Flow<List<Item>>`；`PortalViewModel` 直接订阅
- [ ] `RemoteItemSource` 接口（cycle 0003+ 才需要，先不写）

## Portal 屏 ✅

对照 [`prototype/project/Treasure.html`](../../prototype/project/Treasure.html) 的 `a-portal` 画板：

- [x] 顶部日期 strip（`EST. 2020` / `MAY VI · MMXXVI`），等宽 9.5px
- [x] ornament 装饰（罗盘风：两段细线 + 同心圆 + 钻石）
- [x] "Treasure" 大标题 64sp serif，下方斜体副标题
- [x] 三连计数（items / owned / rooms），数字 22sp serif tabular-nums，**从 Room 来**
- [x] "✦ The Rooms ✦" 节标题
- [x] 4 扇门（2×2）：罗马数字角标 + hero 占位 + 中文名 + `count pcs · english_name`
- [x] "✦ Latest entry" 卡片，从 Room 实时算出
- [x] 底部 ornament 收尾
- [x] 控制岛浮在底部，"门厅" tab 选中态
- [x] 点 4 扇门任意一扇 → 进 Grid 屏（filtered 到该品类）
- [x] 点 Latest entry → 进 Detail
- [x] hero 占位换成真正的博物馆线描矢量（10 个 Composable + Generic 兜底；callout 文字标注待 polish）

## Grid 屏 ✅

对照 `a-home` 画板：

- [x] 顶部 "Treasure" + "A catalogue of things owned · N items"
- [x] 横向滚动品类胶囊（4 个），选中态填充
- [x] 2 列卡片网格，每张卡片：方形 hero + 标题 + 一句话简介
- [x] 点卡片 → 进 Detail
- [x] 控制岛"图鉴"选中态

## Detail 屏 ✅

对照 `a-detail` 画板，**不做翻面也不做抽屉**：

- [x] 顶部 hero 插画（按 `item.heroVector` 分发到 hero 占位；真插画在 chunk B）
- [x] 品名（serif 大字）+ 副标题（昵称 italic + 状态徽章 + 一句话）
- [x] 4 条 hero specs（标签 + 值），从 Room 来
- [x] 底部 "上滑查看 历史 · 参数 · 影集" 提示条占位
- [x] 顶部 back 按钮回 Grid；右上角 edit 跳到编辑表单；delete 删除并返回

## Add / Edit 屏 ✅

cycle 0001 朴素版：

- [x] 选品类 chip（4 颗）
- [x] 文字字段：brand、model、nickname、acquired (YYYY-MM-DD 文本框)、one_liner
- [x] hero specs 4 行，每行 label + value
- [x] 从 14 个预置 `HeroVector` 里挑一张当 hero（横向滚动小预览）
- [x] palette：按品类默认 palette（`paletteFor()`），不让用户改色
- [x] "保存" → 写 Room → 关闭返回上一页（new 模式跳到新 Detail；edit 模式回原 Detail）
- [x] Detail 屏 edit 按钮 → 同表单 prefill；保存写回（保留 createdAt）；delete 删除并返回
- [~] 控制岛"录入"选中态（在 Add 屏；Edit 屏隐藏）
- [ ] 不做：拍照、AI 自动填、history events 录入（cycle 0002）
- [ ] 没做 DatePicker（仅文本框）；后续 polish

## Settings 屏（占位） ✅

- [ ] 屏顶 "Settings"（serif）
- [ ] 一行说明："AI integration is coming. For now, browse and add manually."
- [ ] 一个版本号小字
- [ ] 控制岛"设置"选中态

## 控制岛

- [ ] 4 颗胶囊：门厅 / 图鉴 / 录入 / 设置
- [ ] 浮于底部，浅色模式背景 `rgba(26,24,21,0.85)`，blur 20px
- [ ] 选中态切换有 180ms ease 过渡
- [ ] 在 Detail 屏**隐藏**（不挡 hero）

## 测试

- [ ] `:core` 测试覆盖：`Converters` 双向、`ItemRepository` 的 query / insert / update / delete、`SeedItems.contents()` 不报错
- [ ] `:app` 测试覆盖（Compose UI test）：Portal 渲染、点 4 扇门跳转、Grid 渲染、Detail 渲染、Add 表单提交后 Room 里能查到

## 验证

- [ ] `./gradlew :app:assembleDebug` 通过
- [ ] `./gradlew :core:test :app:test` 全绿
- [ ] 模拟器（Pixel 5 / API 33）跑：冷启动 → Portal 出现 → 4 个品类的"门"显示有种子物品 → 点击进 Grid → 看到 ≥ 1 件物品 → 点进 Detail → 看到 hero 矢量图
- [ ] 杀掉进程重启 → 新加的物品仍在
- [ ] 清除应用数据 → 重启 → 种子物品重新出现

## 完成后

- 改 [`agent.md`](../../agent.md) 的"今天"段
- 把这份 spec 顶部 status 改成 `done`
- 写本 cycle 的复盘到 `notes.md` 末尾
- 在 [`openspec/README.md`](../README.md) 索引表里把 0001 标 `done`，开 0002
