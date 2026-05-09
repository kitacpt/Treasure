# Cycle 0009 · UI polish + Settings 改造 + 品类模板升级

- **状态：** done
- **完成：** 2026-05-08

## 背景

接手 cycle 0008 之后用户给了两批反馈，全部围绕 “界面要更克制、风格要更统一”。这一轮把它们一次性收掉。

## 用户反馈 9 条 + 落地

| # | 反馈 | 实现 |
|---|---|---|
| 1 | App 图标的 “魔戒” 圈太大、没留白 | `drawable/ic_launcher_foreground.xml` 把外环半径 30→22、stroke 6→5，刻字 / tick 同步内移；adaptive crop 18dp 之后大约还剩 ~10dp paper 留白 |
| 2 | 录入页字体跟其他页面不一致 | ChatHeader 头改成 Cormorant titleLarge `Record` + 下面 mono `RECORD` caption（同 Portal/Grid 的 header rhythm）；assistant / voice / draft 的 italic 文本改用 Cormorant 而不是合成斜体的 SpaceGrotesk |
| 3 | 手动录入分组标题居中、字号大、分割线粗一点 | 新增 `ui/components/SectionDivider`：标题 ink 色 11sp letterSpacing 2.4 居中嵌在两侧 0.6dp 细线之间，“`──── 基础 ────`” |
| 4 | 录入对话滚动时消息会跑到输入框下面 | LazyColumn `contentPadding.bottom += navigationBars + 160dp`，确保最后一条消息能完整地停在输入框上方 |
| 5 | Settings 页太杂、提示太多、Provider 改下拉 + 加国产厂商 + 编辑放进抽屉 + 显示连通状态 | Settings 重写：主页只剩一张 AI 摘要卡（provider + 已配置/未配置 pill + Model / Base URL / 掩码 Key + “调整 →”），编辑全部进底部抽屉；新增 `data/AiProviderPreset` 把 Anthropic / OpenAI / Kimi · Moonshot / DeepSeek / 通义千问 / 智谱 GLM / Xiaomi · MiLM / 自定义 列成下拉；删了 “仅本机可读 / 设备直连不走代理 / API key 会从本机抹除” 三段备注 |
| 6 | 手动录入分组标题嵌进分割线里，不用那么大那么粗 | SectionDivider 字号已经从尝试过的 titleMedium 退到 11sp 嵌入式，跟其它 caption 鼓点一致 |
| 7 | 录入页 “未配置 AI” 横幅从底部挪到顶部 | `AddChat`：横幅放在 ChatHeader 0.5dp divider 之后、消息列表之前 |
| 8 | 详情页编辑屏的标题处理也跟手动录入页统一 | 新增 `ui/components/EditPageHeader` (utility row + Cormorant titleLarge + mono caption)；`EditScreen` 与 `CategoryForm` 都换上同款 header；两屏都换上 `SectionDivider` |
| 9 | 四种品类模板可以更有意思 | `CategoryTemplate` 加 `tagline` (一句博物馆 italic 标语) + `heroSpecHints` (每个 hero 字段的单位 / 示例)；字段从 “动力 / 马力 / 0-100 / 驱动” 升级到 “动力 / 马力 (PS) / 0-100 (s) / 驱动”，羽毛球加 “重量 (g)” “平衡点 (mm)” 等单位，电子产品改成 “芯片 / 内存 / 存储 / 屏幕” + 例子；表单顶部新出 italic tagline |

## 顺手的连带改动

- `OpenAiClient.buildUrl()` 现在能识别 `/v4` `/compatible-mode/v1` `/chat/completions` 等不同 base URL 形态，不会再死板地往 `/v1/chat/completions` 后面再补一截
- `SettingsStore` 新增 `presetId` 字段（preset 选哪个）；旧用户首次打开会按 (provider, baseUrl) best-effort 推一个 preset
- `SettingsViewModel` 拆 saved / draft 双状态：抽屉里改 draft，取消放弃，保存才落到 store

## 共享组件

- `ui/components/SectionDivider.kt` — 表单分组分割线
- `ui/components/EditPageHeader.kt` — 编辑屏顶部（leading + trailing slot + Cormorant 大标题 + mono caption）

## 不在这一轮

- 真 schema migration（cycle 0001-0008 已 destructive 8 次，最高优先级仍未做）
- 历史对话持久化、多轮 refine
- 拍照、AI 生成插画
- Xiaomi / 智谱 base URL 准确性 — 当前 preset 给的是开放平台公开 endpoint 的最佳猜测，落地实际 key 时用户可能要在 “自定义” 里自填

## 验收

详见 [`spec.md`](spec.md) / [`notes.md`](notes.md)。
