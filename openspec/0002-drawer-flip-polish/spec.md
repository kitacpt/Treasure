# Cycle 0002 · spec · "完成"长什么样

- **状态：** done
- **完成：** 2026-05-07

## 用户反馈修复（cycle 0001 → 0002 过渡）

- [x] Add 屏改 stub（删 `AddScreen` / `AddViewModel`，删 edit 路由）；控制岛"录入"展示 "对话式录入 · 拍照 → AI 自动识别 — coming"
- [x] 顶部 status bar 留白：`enableEdgeToEdge()` + 每屏 `Modifier.statusBarsPadding()`
- [x] 控制岛底部 `navigationBarsPadding()`，避免和 vivo 手势条重叠
- [x] 页面转场左右滑：`slideIntoContainer(Start/End)` + 300ms tween；前进新页从右进、旧页向左推；回退反向

## Detail polish（cycle 0001 → 0002 过渡）

- [x] back 按钮换成手绘 Canvas 加粗箭头（2.6dp 圆头线条），无文字
- [x] 顶 bar 删掉 delete 按钮（误触风险）；下沉到抽屉"设置" tab
- [x] Grid 顶部副标题简化：从 `A CATALOGUE OF THINGS OWNED · N ITEMS` → `N ITEMS`
- [x] Portal 删除顶部 `EST. 2020 · MAY VI · MMXXVI` 日期条
- [x] Portal 三连计数（items / owned / rooms）的边框框去掉

## 抽屉（drawer）

- [x] `BottomSheetScaffold` 接入 Detail 屏
- [x] peek = 40dp，**只露拖拽柄**；tabs 必须上滑展开后才出现
- [x] 4 个 tabs：历史 / 参数 / 影集 / 设置
- [x] tab 切换不改变抽屉高度（固定 78% 屏高）
- [x] 各 tab 内部 `verticalScroll` 处理超长内容
- [x] **历史 tab**：垂直时间轴；每条 = 日期 + kind 字形（`+ ★ ↻ Δ −`）+ 标题 + 备注；不同 kind 用不同色（acquired = terra；其它按 ink/sub）
- [x] **参数 tab**：specs map 的 key-value 行，分隔线
- [x] **影集 tab**：3×3 占位灰格 + "添加照片 — coming" italic
- [x] **设置 tab**：删除按钮（terra 色，描边）+ 二次确认 AlertDialog；不可恢复警告；预留"更多操作 — coming"

## 明信片翻面（postcard flip）

- [x] Detail hero 卡片点击 → 600ms `rotationY` 翻转，`cameraDistance = 12 * density`
- [x] 正面：博物馆线描 + 右下角 "0 PHOTOS · TAP TO FLIP" 角标
- [x] 背面：3 张轻微旋转、堆叠的空相框（最上面一张带 × 标记），下面 italic "尚未收录实拍" + 物品名 + "+ 添加照片" 胶囊（terra 描边）
- [x] 翻转期间 Box 通过 `graphicsLayer { rotationY = 180f }` 反向矫正，让背面朝上读

## Schema v3

- [x] `Item` 加 `history: List<HistoryEvent>` 字段
- [x] `HistoryEvent(date, kind, title, note)`，`HistoryKind` enum：ACQUIRED / MILESTONE / MAINTAIN / MOD / PARTED
- [x] 列名 `history_json`，序列化用 kotlinx-serialization
- [x] `fallbackToDestructiveMigration()` 处理 v2 → v3
- [x] 8 条种子物品全都补真实历史事件（移植 `prototype/project/data.jsx`）
- [x] `JsonCodec` 内部对象统一封装编/解码

## 验证

- [x] `./gradlew :app:assembleDebug` 通过（v0.5.2，11 MB APK）
- [x] 在 vivo X200 Pro mini 上肉眼对照原型 `a-detail`、`a-drawer-history`、`a-drawer-specs`、`a-drawer-album`、`a-flipped` 5 个画板
- [x] 抽屉 4 个 tab 切换不变高
- [x] 翻面 600ms 平滑无闪烁
- [x] 删除 → 二次确认 → 物品消失 → 自动 popBack 到 Grid

## 不在这一轮（cycle 0003+）

- 真实照片上传（相册 / 相机 / 文件）
- 抽屉"添加历史"功能
- 影集真实图片
- 设置 tab 里"更多操作"（导出 / copy id / 标记等）
- callout 引线 + 罗马数字标注（`i · pentaprism` 那种）
- DatePicker / 真 migration / UI test 套件
