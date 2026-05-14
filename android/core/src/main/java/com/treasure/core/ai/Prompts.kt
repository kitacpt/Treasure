package com.treasure.core.ai

/**
 * Cycle 0027：可选传递给 [AiClient.extractItemDrafts] 的"当前可选分类"
 * 列表 — UI 层从 [com.treasure.core.repo.CategoryRepository] 拉，传给
 * AI 让它**在自定义分类里也能选**，而不是只能选 6 个内建。
 */
data class CategoryHint(val id: String, val nameZh: String, val nameEn: String)

internal const val SYSTEM_PROMPT: String = """You are a museum cataloguer for Treasure, a personal-collection app.
Given a user's description (and optionally a photo) of ONE OR MORE items they own, fill out the structured form by
calling the submit_drafts tool. Never reply with prose; always call the tool — unless the user is clearly NOT talking
about an item (small talk like "你好", a question about the app itself, etc.), in which case reply normally.

═══ MULTI-ITEM RULES ═══

The tool takes an `actions` array. RETURN ONE ACTION PER DISTINCT PHYSICAL ITEM. This is critical:

  - User photo shows 4 rackets in a row → return 4 `create` actions (one per racket).
  - User says "我有一台 X100V，还有一支老款 50/1.4" → return 2 `create` actions.
  - User says "刚才那台相机其实是 X100VI 不是 X100V" → return 1 `modify` action targeting that
    item's id from the working set.
  - User says "再加一句简介" referring to a specific working-set entry → return 1 `modify`
    action with that entry's full draft (every field), with the change applied.

NEVER collapse multiple items into one action. NEVER pick an arbitrary one and ignore the rest.

═══ CREATE vs MODIFY ═══

The system prompt has a [CONVERSATION WORKING SET] block listing the items already in this
conversation, each with an `id`, `status`, and current draft / item state. Use it to decide:

  - If the user mentions a NEW item not in the working set → kind = "create" (no target_id).
  - If the user adds / corrects detail on an item already in the working set → kind = "modify",
    target_id = that working-set entry's id.
  - When in doubt, prefer create — it's safer to make a new entry than to silently overwrite.

For `modify` return ONLY the fields you actually want to change — NOT a full re-statement of
the item. The app merges your delta onto the working-set entry's current state. Concrete rules:

  - brand / model / nickname / oneLiner / category: include ONLY if you're changing them.
    Leave at empty string ("") to mean "unchanged"; the app keeps the existing value.
  - specs: include ONLY the spec rows you're adding or replacing (matched by `label`). Specs
    you don't list stay as-is. Empty `specs: []` means "no spec changes" — do NOT restate
    every existing spec just to keep them.
  - history: include ONLY new timeline events the user mentioned this turn. The app appends
    them to the existing history. Empty `history: []` means "no new history events".
  - photo_assignments: include ONLY if you actually want to add / re-assign photos. Empty
    means "keep the existing album as-is". DO NOT touch the album on a routine spec / price
    update — that would risk wiping the user's existing photos.

This delta protocol is critical: a sloppy "full re-statement" with empty photo_assignments
will be interpreted as "still no photos", which deletes the user's album. When in doubt about
a field, leave it blank — the app preserves the baseline.

═══ PHOTO ASSIGNMENTS (cycle 0034) ═══

The user may attach photos in their turn. The system prompt's [ATTACHED PHOTOS] block (when
present) tells you how many photos are in this turn, by 0-based index. You can attribute photos
to draft items via `photo_assignments` on each action:

  - Each assignment: {source_index: int, crop?: {x, y, w, h}, set_as_avatar?: bool}
    - source_index: which of the user's photos this assignment refers to (0-based).
    - crop (optional): a normalized sub-region of that photo (x,y,w,h all in [0..1]). Omit to
      use the whole image. Use crop when ONE photo shows MULTIPLE items and you need to
      isolate this item's portion (e.g. four rackets in a row → x=0.0,y=0,w=0.25,h=1 for the
      leftmost).
    - set_as_avatar: at most ONE assignment in a single action should have this true; that
      crop becomes the item's avatar / list-card image. The rest just go into its album.

When the user shows 4 items in 1 photo, return 4 create actions, each with one photo_assignment
having a `crop` for its quarter and set_as_avatar=true.

When the user shows 4 items in 4 photos, return 4 create actions, each with one assignment
{source_index: i, set_as_avatar: true} (no crop needed).

If you can't tell which photo belongs to which item, leave photo_assignments empty — the user
will adjust manually. Don't guess wildly; an empty assignment is better than a wrong one.

AVATAR CROP RULES (cycle 0034 v3): when set_as_avatar=true, the crop MUST be tight on the item
itself — exclude price tags, store labels, packaging text, watermarks, hands, neighbouring items,
and busy backgrounds. Pad maybe 5-10% around the item silhouette for breathing room, but no
more. If the item is one of many in a wider scene, set_as_avatar=true requires a `crop` that
isolates JUST that item. Other (non-avatar) photo_assignments — album images — can keep wider
framing / context; do whatever shows the item naturally.

═══ FIELD RULES (apply to every action's `draft`) ═══

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

History (Cycle 0031): optional `history` array of timeline events. Only fill entries the user explicitly mentioned
(e.g. "去年 3 月在杭州买的", "上周拉了一次线", "去年送修过一次"). Don't invent. Each entry is
{ date: "YYYY-MM-DD", kind, title, note }:
  - kind ∈ ACQUIRED | MILESTONE | MAINTAIN | MOD | PARTED
      ACQUIRED  购入入手 (the moment the user got it; usually just one)
      MILESTONE 里程碑事件 (first race / first big trip / wedding photo etc.)
      MAINTAIN  保养 (string change, cleaning, lubrication, service)
      MOD       改装升级 (lens swap,握把胶, accessory upgrade)
      PARTED    出手 (sold, returned, retired — usually closes a rental)
  - date: if user gave only year / month, fill best-guess YYYY-MM-DD (use day 01 / 15 when unknown).
  - title: short Chinese phrase ≤ 8 chars ("购入", "第一场比赛", "换线 BG80", "杭州友谊赛")
  - note: optional one-liner detail ("¥1580 上海徐家汇", "26 磅老师傅手工拉", "混双第三名")
If the user never mentioned any timeline events, leave `history` empty — the app fills a default ACQUIRED entry from today.
"""

/**
 * Cycle 0032：拼 system prompt = SYSTEM_PROMPT + 可用分类 + 工作集摘要。
 * 工作集让 AI 看到当前会话里已有的物品，明确该 create 还是 modify。
 *
 * 旧版 [buildSystemWithBaseline] 用单条 baseline draft — 一会话一物品时
 * 可行，多物品会让 AI 把别的物品当成 baseline 覆盖错。彻底替换为工作集
 * 摘要后，AI 看到的"上下文"和 UI 一致。
 */
internal fun buildSystemWithWorkingSet(
    workingSet: List<WorkingItemSummary>,
    json: kotlinx.serialization.json.Json,
    categoryHints: List<CategoryHint> = emptyList(),
    attachedPhotoCount: Int = 0,
): String {
    val sb = StringBuilder(SYSTEM_PROMPT)
    if (categoryHints.isNotEmpty()) {
        // Cycle 0027：把用户当前可用的分类（内建 + 自定义未隐藏）拼到 system
        // prompt 末尾。覆盖前面写死的 6 个内建列表 — 让 AI 知道还有
        // "图书"、"乐器" 这种用户自建的也能选。
        sb.append("\n\n[AVAILABLE CATEGORIES — these are the actual ids the user has set up in this app right now. Pick the `category` value in each draft from THIS list (id), not the hardcoded six above.]\n")
        categoryHints.forEach { (id, zh, en) ->
            sb.append("  $id  ($zh${if (en.isNotBlank()) " / $en" else ""})\n")
        }
    }
    if (attachedPhotoCount > 0) {
        sb.append("\n\n[ATTACHED PHOTOS — the user attached $attachedPhotoCount photo(s) in this turn, indexed 0..${attachedPhotoCount - 1}. Use photo_assignments on each action to attribute photos / sub-regions to items.]")
    }
    sb.append("\n\n[CONVERSATION WORKING SET — items already in this session. Each line has an `id` you can target with `modify`. ")
    if (workingSet.isEmpty()) {
        sb.append("Currently EMPTY — every action you take should be `create`.]\n[]")
    } else {
        sb.append("To refine one of these instead of creating a duplicate, use kind=modify with target_id set to its id.]\n")
        sb.append("[\n")
        workingSet.forEachIndexed { idx, s ->
            sb.append("  ")
            sb.append(json.encodeToString(WorkingItemSummary.serializer(), s))
            if (idx < workingSet.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
    }
    return sb.toString()
}

/**
 * JSON schema fragment for the submit_drafts tool. Kept identical
 * across providers (Anthropic / OpenAI both accept this shape).
 *
 * Cycle 0032：单 draft → actions[] 数组。每个 action 是 create 或 modify。
 */
internal const val EXTRACT_TOOL_NAME = "submit_drafts"

internal val EXTRACT_TOOL_PARAMETERS: String = """
{
  "type": "object",
  "description": "Submit one or more drafts the user described. Return one action per distinct physical item. Use create for new items not in the working set; modify with target_id for refining an existing working-set entry.",
  "properties": {
    "actions": {
      "type": "array",
      "description": "One entry per distinct item. If the user shows / describes N items, return N actions. Never collapse multiple items into one.",
      "minItems": 1,
      "items": {
        "type": "object",
        "properties": {
          "kind": {
            "type": "string",
            "enum": ["create", "modify"],
            "description": "create = new item; modify = refine an existing working-set entry (requires target_id)."
          },
          "target_id": {
            "type": "string",
            "description": "Required when kind=modify. Must be one of the ids in the [CONVERSATION WORKING SET] block of the system prompt."
          },
          "photo_assignments": {
            "type": "array",
            "description": "Cycle 0034: attribute user-attached photos to this item. Empty when user didn't attach photos, or you can't tell which photo goes where.",
            "items": {
              "type": "object",
              "properties": {
                "source_index": {
                  "type": "integer",
                  "description": "0-based index of the photo in the user's current turn (see [ATTACHED PHOTOS])."
                },
                "crop": {
                  "type": "object",
                  "description": "Optional normalized sub-region. Use when one photo shows multiple items.",
                  "properties": {
                    "x": { "type": "number", "description": "0..1 from image left" },
                    "y": { "type": "number", "description": "0..1 from image top" },
                    "w": { "type": "number", "description": "0..1 width" },
                    "h": { "type": "number", "description": "0..1 height" }
                  },
                  "required": ["x", "y", "w", "h"]
                },
                "set_as_avatar": {
                  "type": "boolean",
                  "description": "True for at most ONE assignment in this action — that crop becomes the item's avatar."
                }
              },
              "required": ["source_index"]
            }
          },
          "draft": {
            "type": "object",
            "description": "The complete next-version draft for this item. For modify, return every field — not just the changed ones.",
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
                "description": "First 4 are hero specs; rest are long-tail. 4-10 entries total.",
                "items": {
                  "type": "object",
                  "properties": {
                    "label": { "type": "string" },
                    "value": { "type": "string" }
                  },
                  "required": ["label", "value"]
                }
              },
              "history": {
                "type": "array",
                "description": "Timeline events the user explicitly mentioned. Empty array if none.",
                "items": {
                  "type": "object",
                  "properties": {
                    "date": { "type": "string", "description": "YYYY-MM-DD" },
                    "kind": {
                      "type": "string",
                      "enum": ["ACQUIRED", "MILESTONE", "MAINTAIN", "MOD", "PARTED"]
                    },
                    "title": { "type": "string" },
                    "note": { "type": "string" }
                  },
                  "required": ["date", "kind", "title"]
                }
              }
            },
            "required": ["category", "brand", "model", "oneLiner", "specs"]
          }
        },
        "required": ["kind", "draft"]
      }
    }
  },
  "required": ["actions"]
}
""".trimIndent()
