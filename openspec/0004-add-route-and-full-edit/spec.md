# Cycle 0004 · spec · "完成"长什么样

- **状态：** done
- **完成：** 2026-05-07

## 录入页

- [x] 替换 `AddStubScreen`，控制岛"录入" → 真页面
- [x] 顶部 Header："Treasure" + "NEW ENTRY"（同 Grid 风格）
- [x] 模式切换 chips：手动录入 / AI 录入
- [x] **手动 tab**：BoxWithConstraints 撑满，4 个气泡 2×2 错落，浮动周期 2.2 / 2.7 / 2.4 / 2.9 秒
- [x] 气泡内：CategoryGlyph（小线条图标）+ 中文名 + 英文 caps
- [x] 气泡点击 → ModalBottomSheet（不是 BottomSheetScaffold）
- [x] 表单顶部 Hero 预览（用模板 heroVector + palette，正方形卡片）
- [x] 表单字段：品牌 / 型号 / 昵称 / 购入日期 / 一句话 / 状态 chips
- [x] 4 行 hero specs：标签从模板来（不可改），值用户填
- [x] 4 个品类的模板：badminton / photo / cars / tech 各自的标签 + heroVector + palette
- [x] 保存按钮：disabled 直到 brand + model 都非空；点击 → 写 Room → onSaved(id) → 跳新 Detail
- [x] **AI tab**：白底 + ink 底两个气泡 + "AI 录入 — coming" 占位 + "去设置"链接
- [x] 控制岛在录入页保持显示，"录入"高亮

## Detail 抽屉 4 tabs

- [x] tabs 顺序：基础 / 参数 / 历史 / 影集（"设置"删除，用户要求）
- [x] tab 切换不改抽屉高度（保持 cycle 0002 的 78% 屏高 + 内部 verticalScroll 模式）

### 基础 tab
- [x] 文本字段：品牌 / 型号 / 昵称 / 一句话 / 购入日期 / 出手日期
- [x] STATUS chips（3 个）
- [x] CATEGORY chips（4 个）
- [x] HERO ILLUSTRATION：14 个 HeroVector enum 平铺成 chips（3-4 行 × 4 列）
- [x] 保存修改按钮：dirty 检测覆盖 9 个字段
- [x] DANGER ZONE：删除按钮 + 二次确认 AlertDialog（保留 cycle 0002 行为）

### 参数 tab
- [x] HERO SPECS：4 行（标签 + 值都可改）
- [x] SPECS：自由 key-value 表
  - 空时显示 "还没有完整参数 · 点 + 加一行"
  - 每行 key + value + `−` 按钮（点击移除该行）
  - 底部 `+ 加一行` 按钮（terra 描边）
- [x] 保存修改按钮：dirty = heroSpecs 或 specs 变了；保存时过滤掉空 key 的行

### 历史 tab
- [x] 顶部 `+ 加一条历史` 按钮（terra 描边）
- [x] 列表保留 cycle 0002 的时间轴样式（kind 字形 + badge + 连线）
- [x] tap 行 → AlertDialog 编辑（日期 / kind chips × 5 / 标题 / 备注）
- [x] 长按行 → AlertDialog 删除二次确认
- [x] 提交时按日期排序
- [x] 底部提示 "tap 编辑 · 长按删除"
- [x] 列表为空时显示 "还没有时间轴 · 点上面 + 添加"

### 影集 tab
- [x] 保持 cycle 0003 实现：3 列网格 + 第一格 +tile + 长按删

## ViewModel API

- [x] `DetailViewModel.update(item: Item)`：统一入口替代多个 setter
- [x] tab 内部维护 local edited state（`remember(item.id)`），保存时调用 `onUpdate(item.copy(...))`
- [x] dirty 检测：local state vs item 的纯 equality 对比

## 验证

- [x] `./gradlew :app:assembleDebug` 通过（v0.7.0，12 MB）
- [ ] 装机：进 Add → 看到 4 气泡浮动 → 点羽毛球气泡 → 弹底部 sheet → 表单可填 → 保存 → 跳新 Detail
- [ ] AI 模式：占位渲染正常 + 去设置可跳
- [ ] Detail 基础 tab：改品牌 / 型号 → 保存 → 标题更新
- [ ] Detail 参数 tab：加一行 specs → 保存 → 抽屉重新打开仍在
- [ ] Detail 历史 tab：+ 一条 → 保存 → 时间轴出现新条；tap 旧条 → 改 → 保存
- [ ] 长按历史 → 删除 → 消失
- [ ] 删除物品（基础 tab 底部 DANGER ZONE）→ 二次确认 → 物品消失，回 Grid

## 不在这一轮

- AI 实接通 / 设置页真页面 / 真 migration / 全屏看图 / DatePicker UI / callout 标注
