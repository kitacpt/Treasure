package com.treasure.core.ai

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

Specs: provide 4 to 8 entries total in this order:
  - The first 4 are "hero" specs displayed on the card, and MUST follow the category-specific labels:
      badminton: [重量 (g), 平衡点 (mm), 中杆硬度, 穿线磅数]
      photo:     [画幅, 像素, ISO 范围, 连拍]
      cars:      [动力, 马力 (PS), 0-100 (s), 驱动]
      tech:      [芯片, 内存, 存储, 屏幕]
      coffee:    [品类, 产地 / 烘焙度, 研磨度, 用法]
      wine:      [酒种, 酒精度 / 容量, 产地 / 年份, 酒款]
  - After those four, append any further specs the description supports (型号 / 配色 / 购入价 / …).
Use empty string for hero values you can't tell. Never invent specs you don't see evidence for.
"""

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
      "enum": ["badminton", "photo", "cars", "tech", "coffee", "wine"]
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
