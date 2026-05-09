# Cycle 0017 · 验收

## AI 客户端

- thinking 模式（toggle 显式开启 OR `kimi-k*` / `kimi-thinking-*` / `o1*` / `o3*` / `o4*` 隐式命中）的请求 callTimeout 升到 360 秒
- 普通模型保持 120 秒
- 用户用 kimi-k2.5 测试不再被 120s 切断（前提：模型本身能在 360s 内响应）

## Portal

- Tally 行只有 `items` + `rooms` 两列；`OWNED` 完全不出现

## Grid

- LazyRow 顶部新加 "全部" chip（terra dot 选中态）
- 从门厅 doorway 进 Grid，对应 chip 自动滚到可见区（首屏可能被遮的右侧 chips 直接展开到位）
- 点 "全部" 显示所有品类聚合 grid；切回某品类正常过滤
- `EmptyHint(null)` 文案 "图鉴还空着 — 去录入页加点东西"

## Edit

- 顶部 EditPageHeader subtitle 直接 = `${item.category.nameZh}`，无 "EDIT · " 前缀
- 头像选择器展开区结构（自上而下）：📷 拍照 / + 选照片 两 chip → 影集照片小圆（tap=换头像 / long-press=删） → 0.5dp 竖分隔 → 品类相关线描小圆
- 没有独立 "实拍" Section / PhotoSection（已整合）
- 没有 "时间" Section（购入 / 出手 都由 history 事件管理）
- 拖动 spec 跨越 hero/tail 分割线时分割线临时换成 16.5dp Spacer，shift 不再错位

## Manual entry

- ManualCategoryPicker 弹层只有 6 行品类卡，无标题 + 副标
- CategoryForm subtitle = 中文品类名（跟 Edit 风格一致）
- Sections：基础 / 标签（状态 chips 行） / 参数 · ${category}
- 没有 "时间" / 单独 "状态" / 中央 italic tagline

## 录入页

- 头部右侧只有 🕐（开抽屉）+ 手动；⊕ 已去除
- 历史抽屉是左侧全高 panel，280-320dp 宽
- HistoryRow title 若是默认 "New entry" 显示成 "New entry · HH:MM"；副标变成 "今天 · HH:MM"
- 点对话行 / 点新对话 不再自动收抽屉，连续操作不打断
- Composer 没有麦克风按钮

## Settings

- DANGER ZONE 文案 "重置设置"
- 点了立刻弹 AlertDialog（标题 "重置设置？" + 解释 + 取消 / 重置 双按钮），点重置才真清

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
