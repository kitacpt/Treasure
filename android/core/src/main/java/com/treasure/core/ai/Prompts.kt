package com.treasure.core.ai

/**
 * Cycle 0027：可选传递给 [AiClient.extractItemDraft] 的"当前可选分类"
 * 列表 — UI 层从 [com.treasure.core.repo.CategoryRepository] 拉，传给
 * AI 让它**在自定义分类里也能选**，而不是只能选 6 个内建。
 */
data class CategoryHint(val id: String, val nameZh: String, val nameEn: String)

internal const val SYSTEM_PROMPT: String = """You are a museum cataloguer for Treasure, a personal-collection app.
Given a user's description (and optionally a photo) of an item they own, fill out the structured form by
calling the fill_item_draft tool. Never reply with prose; always call the tool.

Categories — pick exactly one of:
  badminton  (羽毛球: rackets, shuttles, badminton shoes, …)
  photo      (摄影: cameras, lenses, tripods, …)
  cars       (汽车: rented or owned cars)
  tech       (电子产品: laptops, phones, watches, earbuds, e-readers, tablets, …)
  coffee     (咖啡: espresso machines, grinders, coffee beans, …)
  wine       (酒水: wine, whisky, gin, cocktail tools, …)

Brand + model: actual product names. Don't make them up — if you can't tell, leave them empty.
Nickname: optional, short Chinese pet name (e.g. "黑刃" for a black racket); leave empty unless the user gave one.
oneLiner: one short Chinese line, like "进攻型 4U · 拉26磅" or "APS-C 旗舰 · 4020 万像素".

Specs: 4 to 10 entries total. Fill what is genuinely informative for THIS specific item — pick whatever
attributes matter, in whatever order makes sense, using natural Chinese labels and units.
  - The first 4 are "hero" specs (displayed on the card). Choose the most important attributes for THIS
    item — they don't need to follow a fixed per-category template. Examples of good hero picks (NOT
    a required list — adapt to the item):
      badminton racket: 重量 / 平衡点 / 中杆硬度 / 穿线磅数
      camera body:      画幅 / 像素 / 连拍 / ISO 范围
      coffee bean:      产地 / 烘焙度 / 处理法 / 风味
      wine:             酒种 / 容量 · 酒精度 / 产地 · 年份 / 酒款
      headphones:       驱动单元 / 阻抗 / 蓝牙版本 / 主动降噪
  - After hero, append tail specs the description supports — purchase info (入手日期 / 入手价格 / 入手渠道 / 颜色)
    or any further attributes the user mentioned. The user expects whatever you fill to show up as-is in
    the draft preview, so choose labels you'd want them to see.
Leave the value as an empty string for any spec you can't determine. Never invent specs you have no
evidence for.
"""

/**
 * Cycle 0024：会话 = 草稿。如果上一次用户已"采用"过一份草稿（[baseline]
 * 非空），把它的 JSON 拼到 system prompt 末尾，告诉模型"这是当前已确认
 * 状态，请基于它给下一版"。这样多轮对话不会每次都生成完全不同的字段，
 * 而是 incremental refine — 用户加一句"颜色是红色"，AI 应只在 specs 里
 * 加一行颜色，其它字段保持原样。
 *
 * [baseline] = null（首轮）→ 与之前完全相同的 system prompt。
 */
internal fun buildSystemWithBaseline(
    baseline: ItemDraft?,
    json: kotlinx.serialization.json.Json,
    categoryHints: List<CategoryHint> = emptyList(),
): String {
    val sb = StringBuilder(SYSTEM_PROMPT)
    if (categoryHints.isNotEmpty()) {
        // Cycle 0027：把用户当前可用的分类（内建 + 自定义未隐藏）拼到 system
        // prompt 末尾。覆盖前面写死的 6 个内建列表 — 让 AI 知道还有
        // "图书"、"乐器" 这种用户自建的也能选。
        sb.append("\n\n[AVAILABLE CATEGORIES — these are the actual ids the user has set up in this app right now. Pick the `category` value from THIS list (id), not the hardcoded six above.]\n")
        categoryHints.forEach { (id, zh, en) ->
            sb.append("  $id  ($zh${if (en.isNotBlank()) " / $en" else ""})\n")
        }
    }
    if (baseline != null) {
        val baselineJson = json.encodeToString(ItemDraft.serializer(), baseline)
        sb.append("\n\n")
        sb.append(
            """
            [CURRENT CONFIRMED DRAFT — the user has accepted this as the
            baseline for this conversation. Your job is to give the *next
            version* of this draft, not start from scratch. Keep fields you
            don't have evidence to change. Only add / refine / overwrite the
            parts the user's new message actually addresses.]

            $baselineJson
            """.trimIndent(),
        )
    }
    return sb.toString()
}

/**
 * JSON schema fragment for the fill_item_draft tool. Kept identical
 * across providers (Anthropic / OpenAI both accept this shape).
 */
internal const val EXTRACT_TOOL_NAME = "fill_item_draft"

internal val EXTRACT_TOOL_PARAMETERS: String = """
{
  "type": "object",
  "properties": {
    "category": {
      "type": "string",
      "description": "Category id; pick from the list in the system prompt (built-in or user-added)."
    },
    "brand": { "type": "string" },
    "model": { "type": "string" },
    "nickname": { "type": "string" },
    "oneLiner": { "type": "string" },
    "specs": {
      "type": "array",
      "description": "First 4 follow the category template (hero specs); rest are long-tail.",
      "items": {
        "type": "object",
        "properties": {
          "label": { "type": "string" },
          "value": { "type": "string" }
        },
        "required": ["label", "value"]
      }
    }
  },
  "required": ["category", "brand", "model", "oneLiner", "specs"]
}
""".trimIndent()
