package com.treasure.core.ai

import com.treasure.core.domain.HeroSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Per [ADR-0004], AI services are user-supplied (BYO key, device-direct,
 * no proxy). This interface is what the UI talks to; concrete impls live
 * in this package — [AnthropicClient] for Claude, [OpenAiClient] for
 * OpenAI and OpenAI-compatible endpoints (e.g. self-hosted vLLM,
 * DeepSeek, etc).
 *
 * Cycle 0032：协议从单 draft 升级到多 action — 一段会话可以攒多件物品，
 * AI 一次回复也能 [create] / [modify] 多条。AI 看见当前 working set
 * （每行有 id + 当前 draft），明确区分"新建"与"修改"，不会再覆盖错。
 */
interface AiClient {
    /**
     * @param workingSet 这段会话当前的物品列表 — AI 用它判断该 create
     *  还是 modify 现有条目（target_id 指向 working-set 行的 id）。空列
     *  表 = 全新会话。
     * @param categoryHints Cycle 0027：当前用户可用的分类列表（内建 +
     *  未隐藏的自定义）。空时退回 SYSTEM_PROMPT 里的内建 6 个。非空时
     *  会拼到 system prompt 让 AI 选自定义 id（比如"custom-xxx"）。
     */
    suspend fun extractItemDrafts(
        text: String,
        imagesJpegBytes: List<ByteArray> = emptyList(),
        priorTurns: List<AiTurn> = emptyList(),
        workingSet: List<WorkingItemSummary> = emptyList(),
        categoryHints: List<CategoryHint> = emptyList(),
        /** Cycle 0034 v2：用户录的原始音频字节。null = 没附音频。
         *  [audioFormat] = "m4a" / "wav" / "mp3"。Provider 接不接受随它，错就错。 */
        audioBytes: ByteArray? = null,
        audioFormat: String = "m4a",
    ): Result<List<DraftAction>>

    /** Cycle 0031：用户按 stop 时立刻掐掉正在飞的 HTTP 调用。impl 调
     *  OkHttp dispatcher.cancelAll() — 同 instance 上所有 in-flight 全断。 */
    fun cancel() {}
}

/**
 * Cycle 0032：AI 通过 submit_drafts 工具返回一组 actions。每个 action
 * 描述对工作集的一次变更。
 *
 * - kind = [ActionKind.CREATE]: 新物品。[targetId] 应为空；VM 会生成一个
 *   新 ciId 并 upsert PENDING 行。
 * - kind = [ActionKind.MODIFY]: 修改已有工作集行；[targetId] 必须是 system
 *   prompt 里 [CONVERSATION WORKING SET] 中某行的 id。VM 根据原行 status
 *   决定新 status — 原 PENDING / MODIFIED 保持，原 SAVED → MODIFIED。
 */
@Serializable
data class DraftAction(
    val kind: ActionKind,
    @SerialName("target_id") val targetId: String? = null,
    val draft: ItemDraft,
    /**
     * Cycle 0034：用户在本轮 user-turn 里发了几张图，AI 据此把每张图（或一
     * 张图里的一块）分配给具体物品的影集 — 一张图也能切到不同物品（比如
     * 用户发了 1 张 "桌面 4 件器材合照"，AI 给每件分一个 crop rect）。
     *
     * 空列表 = AI 没作图片分配，commit 时 draft.photos 沿用现有逻辑。
     */
    @SerialName("photo_assignments") val photoAssignments: List<PhotoAssignment> = emptyList(),
)

/**
 * Cycle 0034：AI 把用户发的某张图（按 source_index）分配给当前 draft，
 * 可选 [crop] 指定该图内子区域。[setAsAvatar] = true 表示这张（裁剪后的）
 * 子图当物品头像。一个 action 内最多一个 set_as_avatar=true。
 *
 * 索引 source_index 对应本轮 user-turn 里 photos 数组的位置（0-based）。
 */
@Serializable
data class PhotoAssignment(
    @SerialName("source_index") val sourceIndex: Int,
    val crop: NormalizedRect? = null,
    @SerialName("set_as_avatar") val setAsAvatar: Boolean = false,
)

/** [x, y, w, h] 都是 0..1 的归一化坐标，相对原图。整图 = (0,0,1,1)。 */
@Serializable
data class NormalizedRect(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
)

@Serializable
enum class ActionKind {
    @SerialName("create") CREATE,
    @SerialName("modify") MODIFY,
}

/**
 * Cycle 0032：喂给 AI 的工作集摘要 — 每条物品的 id + 当前状态 + 关键
 * 字段。AI 据此判断是 create 新条目还是 modify 旧的。控制 token 上限
 * （别把整张物品 photos / palette / history 全塞进去）。
 */
@Serializable
data class WorkingItemSummary(
    val id: String,
    val status: String, // "PENDING" | "SAVED" | "MODIFIED"
    val title: String,
    val category: String? = null,
    val oneLiner: String = "",
    val specs: List<HeroSpec> = emptyList(),
)

enum class AiRole { USER, ASSISTANT }

data class AiTurn(val role: AiRole, val text: String)

/**
 * 当模型选择 *不* 调 fill_item_draft，而是回了一段普通对话文字时，
 * 客户端把文字封进这个异常。AddViewModel 接到它就直接把 `text` 作为
 * 助手聊天消息插进对话流，不显示为 "出错了：…"。
 */
class ChatOnlyResponseException(val text: String) : Exception("model returned chat text instead of tool call")

/**
 * 在自由文本里捞出第一段平衡的 `{ ... }`。给 thinking 模式 + tool_choice=auto
 * 的回退路径用 — 模型有时不调 tool 而是把 JSON 直接写在文字里，可能还包了
 * markdown 代码围栏。简单括号配对，足够应付 "前面一段思考 + 后面一坨 JSON" 的形态。
 */
internal fun extractFirstJsonObject(text: String): String? {
    val start = text.indexOf('{')
    if (start < 0) return null
    var depth = 0
    var inString = false
    var escape = false
    for (i in start until text.length) {
        val c = text[i]
        if (inString) {
            if (escape) { escape = false; continue }
            when (c) {
                '\\' -> escape = true
                '"' -> inString = false
            }
            continue
        }
        when (c) {
            '"' -> inString = true
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return text.substring(start, i + 1)
            }
        }
    }
    return null
}

/**
 * Provider taxonomy. [OpenAiCompatible] uses the OpenAI client with a
 * custom baseUrl — covers anything that speaks the OpenAI v1 schema.
 */
enum class Provider(val display: String) {
    Anthropic("Anthropic"),
    OpenAi("OpenAI"),
    OpenAiCompatible("Custom (OpenAI-compatible)"),
}

/**
 * Fields the AI tries to fill. [specs] is a single ordered list — the
 * first [com.treasure.core.domain.Item.HERO_SPEC_COUNT] entries are
 * treated as "hero" by the UI; anything after is the long-tail.
 */
@Serializable
data class ItemDraft(
    val category: String? = null, // "badminton" / "photo" / "cars" / "tech"
    val brand: String = "",
    val model: String = "",
    val nickname: String = "",
    val oneLiner: String = "",
    val specs: List<HeroSpec> = emptyList(),
    /** Cycle 0031：草稿也能编辑 history 时间轴 — 跟物品 Edit 页同款逻辑。
     *  AI 不直接填这个字段；用户在 Refine 页手动加。commitDraft 把它带进
     *  最终 Item。 */
    val history: List<com.treasure.core.domain.HistoryEvent> = emptyList(),
    /** Cycle 0033：草稿阶段就能管影集。绝对路径（filesDir/draft-photos/<convo>/<uuid>.jpg）
     *  逐步累加；commitDraft 把它们带进 Item.photos。AI 不填。 */
    val photos: List<String> = emptyList(),
    /** Cycle 0033：用户在 Refine 里挑的头像（必须是 photos 里的一张）；不挑
     *  则 commit 时 Item.avatarPhotoPath = null，列表回到线描插画。 */
    val avatarPhotoPath: String? = null,
    /** Cycle 0034 v5：用户可在 Refine 里把"预制插画"选为头像 — 覆盖品类
     *  模板的默认线描。AI 不直接填这个；用户在 HeroAvatarPicker 里选。null
     *  表示沿用品类模板的默认 heroVector。 */
    val heroVector: com.treasure.core.domain.HeroVector? = null,
    /**
     * Cycle 0034 v4：每张草稿影集照片的归一化裁剪矩形。跟 Item.photoCrops
     * 同语义 — 存全图，显示裁剪。AI 给的 photo_assignments.crop 落到这里。
     * commit 时透传到 Item.photoCrops。
     */
    val photoCrops: Map<String, com.treasure.core.domain.PhotoCrop> = emptyMap(),
)
