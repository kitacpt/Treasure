# Cycle 0022 · spec

## 1. Record 默认续上次会话

- 启动 / 切到 Record tab → 不再生一段新对话，自动 reload 上次的最近一条
- 历史抽屉里的 "New entry · 15:32" 不再堆叠空壳（之前每打开一次就 +1 行）
- 真要新对话：历史抽屉里有「+ 新对话」按钮（cycle 0017 已加）
- 上次对话已含 DraftCta → 状态里 `draft` 也回填，能直接跳草稿页继续编

## 2. URL fetch 全程对用户可见

- 用户发出含 `https?://...` 的消息：
  - 立刻在 chat 里出现一行 "正在抓取 jd.com …"（小字斜体居中，灰色）
  - fetch 完成后那一行被原地替成结果：
    - 成功：「✓ 已抓取 jd.com · 1.2K 字」绿色
    - 防爬：「⚠ jd.com 防爬挡住 · login wall」橙色
    - 失败：「⚠ jd.com 抓取失败 · UnknownHostException」红色
- 这些 SystemNote 不持久化、不喂 AI priorTurns
- AI 拿到的 prompt 三态分明（cycle 0021 已实现）

## 3. PageFetcher charset 稳固

- Content-Type 带 charset → 用它（兼容 cycle 0021 行为）
- Content-Type 不带 → 用 ISO-8859-1 把 body 头 4KB probe 一遍
  - grep `<meta charset="X">`
  - grep `<meta http-equiv="Content-Type" content="...; charset=X">`
  - 命中 → 用那个 charset 重解
  - 不命中 → fallback UTF-8
- 京东 / 当当老站点的 GBK / GB2312 不再出乱码

## 4. 多模态能力提示

- 摘要卡 Model 行：右下挂 "🖼 多模态" pill（仅 vision-capable model）
- 编辑抽屉 Model 输入框下面：
  - 多模态 → "🖼 多模态 · 录入页可发图给它认"
  - 否则 → "纯文本模型 · 不支持发图"
- 启发式覆盖 Anthropic 全系、OpenAI gpt-4o/gpt-4-turbo/o4、Qwen-VL、GLM-4V、显式带 vision/vl 的所有自定义 endpoint

## 5. Out of scope

- 流式输出（用户授权跳过）
- WebView headless render
- provider catalog API
