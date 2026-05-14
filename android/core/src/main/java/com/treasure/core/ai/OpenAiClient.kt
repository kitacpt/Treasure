package com.treasure.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * OpenAI v1 /chat/completions client. Doubles as the implementation for
 * [Provider.OpenAiCompatible] — the only difference is the [baseUrl].
 *
 * Uses function-calling (the OpenAI flavour of tool-use) to force the
 * model to call fill_item_draft, mirroring the Anthropic flow.
 */
class OpenAiClient(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://api.openai.com",
    /** null = 用 provider 的默认采样温度 */
    private val temperature: Double? = null,
    /**
     * Cycle 0014：thinking on 时改用 `tool_choice = auto` 而不是强制点名。
     * Cycle 0015：除了 toggle，按模型名嗅出 thinking。
     * Cycle 0016：把 Kimi 整个 k 系列都纳入隐式 thinking — Moonshot 的
     * `kimi-k2-0711-preview` / `kimi-k2-0905-preview` / `kimi-k2-turbo-preview`
     * / `kimi-k2.5` 系列模型即便名字里没 "thinking" 也内置 CoT，发
     * `tool_choice: { type: "function" }` 会拿到 "tool_choice 'specified' is
     * incompatible with thinking enabled"；只有 `moonshot-v1-*` 系列接受
     * specified 形式。检测命中时自动回退到 `tool_choice: "auto"`。
     */
    private val thinkingEnabled: Boolean = false,
    httpClient: OkHttpClient? = null,
) : AiClient {

    /** 启发式：模型名暗示了 thinking 的，强制按 thinking 模式走。 */
    private val isImplicitThinkingModel: Boolean =
        model.contains("thinking", ignoreCase = true) ||
            // Moonshot k 系列：kimi-k2-*, kimi-k1-*, kimi-k2.5, kimi-k2-turbo
            // 等都内置 CoT。覆盖 "kimi-k" 前缀（不区分大小写）。
            model.startsWith("kimi-k", ignoreCase = true) ||
            // OpenAI o-series（o1 / o3 / o3-mini / o4-mini 等）也是 reasoning
            // 模型，只允许 tool_choice = auto / none。
            model.startsWith("o1", ignoreCase = true) ||
            model.startsWith("o3", ignoreCase = true) ||
            model.startsWith("o4", ignoreCase = true)

    /** 真正决定本次请求是否按 thinking 模式构造 payload 的有效值。 */
    private val effectiveThinking: Boolean = thinkingEnabled || isImplicitThinkingModel

    /**
     * 只有 Qwen DashScope 兼容模式 / 智谱 GLM 这种把 thinking 暴露成
     * 顶层布尔字段的厂商才需要发 `enable_thinking: true`。OpenAI / Kimi /
     * DeepSeek 都用模型名隐式控制 thinking，发了反而可能引起 "未知字段"。
     */
    private val supportsEnableThinkingFlag: Boolean =
        baseUrl.contains("dashscope.aliyuncs.com", ignoreCase = true) ||
            baseUrl.contains("open.bigmodel.cn", ignoreCase = true)

    private val client: OkHttpClient = httpClient ?: defaultHttpClient(
        callTimeoutSec = if (thinkingEnabled || isImplicitThinkingModelStatic(model)) 360L else 120L,
    )
    private val json = Json { ignoreUnknownKeys = true }

    /** Cycle 0031：用户按 stop 时调；掐掉这个 client 上所有 in-flight 请求。 */
    override fun cancel() {
        client.dispatcher.cancelAll()
    }

    override suspend fun extractItemDrafts(
        text: String,
        imagesJpegBytes: List<ByteArray>,
        priorTurns: List<AiTurn>,
        workingSet: List<WorkingItemSummary>,
        categoryHints: List<CategoryHint>,
    ): Result<List<DraftAction>> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPayload(text, imagesJpegBytes, priorTurns, workingSet, categoryHints)
            val response = client.newCall(
                Request.Builder()
                    .url(buildUrl())
                    .header("Authorization", "Bearer $apiKey")
                    .header("content-type", "application/json")
                    .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                    .build(),
            ).execute()

            response.use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("HTTP ${resp.code}: ${body.take(500)}")
                }
                parseDrafts(body)
            }
        }
    }

    private fun buildUrl(): String {
        val trimmed = baseUrl.trimEnd('/')
        return when {
            // User pasted the full chat-completions endpoint already.
            trimmed.endsWith("/chat/completions") -> trimmed
            // Base already ends with an /vN segment (e.g. /v1, /v4,
            // /compatible-mode/v1) — append only the action path.
            VERSIONED_TAIL.containsMatchIn(trimmed) -> "$trimmed/chat/completions"
            // Plain host root — assume the OpenAI v1 layout.
            else -> "$trimmed/v1/chat/completions"
        }
    }

    private fun buildPayload(
        text: String,
        images: List<ByteArray>,
        priorTurns: List<AiTurn>,
        workingSet: List<WorkingItemSummary>,
        categoryHints: List<CategoryHint>,
    ): String {
        val toolSchema = json.parseToJsonElement(EXTRACT_TOOL_PARAMETERS).jsonObject
        val payload = buildJsonObject {
            put("model", model)
            temperature?.let { put("temperature", it) }
            // Cycle 0032：多 action 时 tool call 体积变大；max_tokens 给充足
            // 余量，否则 OpenAI 默认 1024 容易在第 3-4 件物品中段截断 JSON。
            put("max_tokens", if (effectiveThinking) 8192 else 4096)
            // 仅 Qwen / 智谱这种把 enable_thinking 暴露成顶层字段的厂商需要发；
            // OpenAI / Kimi / DeepSeek 走模型名隐式 thinking，发了反而可能炸
            if (effectiveThinking && supportsEnableThinkingFlag) {
                put("enable_thinking", true)
            }
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", buildSystemWithWorkingSet(workingSet, json, categoryHints, images.size))
                })
                priorTurns.forEach { turn ->
                    add(buildJsonObject {
                        put("role", if (turn.role == AiRole.USER) "user" else "assistant")
                        put("content", turn.text)
                    })
                }
                add(buildJsonObject {
                    put("role", "user")
                    if (images.isNotEmpty()) {
                        // OpenAI vision: array content with text + image_url blocks
                        putJsonArray("content") {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", "User description:\n\n$text")
                            })
                            // Cycle 0034：多张图按 source_index 顺序送入
                            images.forEach { img ->
                                val b64 = Base64.getEncoder().encodeToString(img)
                                add(buildJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") {
                                        put("url", "data:image/jpeg;base64,$b64")
                                    }
                                })
                            }
                        }
                    } else {
                        put("content", "User description:\n\n$text")
                    }
                })
            }
            putJsonArray("tools") {
                add(buildJsonObject {
                    put("type", "function")
                    putJsonObject("function") {
                        put("name", EXTRACT_TOOL_NAME)
                        put("description", "Fill out the structured Treasure item draft form")
                        put("parameters", toolSchema)
                    }
                })
            }
            // Kimi / OpenAI thinking 模式（包括按模型名隐式判定的）只接受
            // auto；非 thinking 时强制点名工具。
            if (effectiveThinking) {
                put("tool_choice", "auto")
            } else {
                putJsonObject("tool_choice") {
                    put("type", "function")
                    putJsonObject("function") { put("name", EXTRACT_TOOL_NAME) }
                }
            }
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun parseDrafts(body: String): List<DraftAction> {
        val response = json.parseToJsonElement(body).jsonObject
        val choices = response["choices"]?.jsonArray
            ?: throw IllegalStateException("response missing 'choices'")
        val first = choices.firstOrNull()?.jsonObject
            ?: throw IllegalStateException("empty choices")
        val message = first["message"]?.jsonObject
            ?: throw IllegalStateException("choice missing 'message'")

        // 优先：tool_calls 里第一个的 function.arguments（强制 tool_choice 时一定有）
        val toolArgs = message["tool_calls"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("function")?.jsonObject
            ?.get("arguments")?.jsonPrimitive?.content
        if (toolArgs != null) {
            val parsed = json.parseToJsonElement(toolArgs).jsonObject
            return parseActionsObject(parsed)
        }

        // 回退：thinking + tool_choice=auto 时模型可能直接把 JSON 写进 content。
        // 抓出第一个 `{...}` 试着解析；失败说明模型选择了纯聊天回复（用户没
        // 提物品信息），把那段文字 surface 成普通对话消息，而不是错误。
        val contentText = message["content"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("no tool_calls and no content — empty response")
        val jsonChunk = extractFirstJsonObject(contentText)
        if (jsonChunk != null) {
            val parsed = json.parseToJsonElement(jsonChunk).jsonObject
            return parseActionsObject(parsed)
        }
        throw ChatOnlyResponseException(contentText.trim())
    }

    private fun parseActionsObject(obj: JsonObject): List<DraftAction> {
        val actionsArr = obj["actions"]?.jsonArray
            ?: throw IllegalStateException("tool response missing 'actions'")
        return actionsArr.map { el ->
            json.decodeFromJsonElement(DraftAction.serializer(), el)
        }
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val VERSIONED_TAIL = Regex("/v\\d+(?:beta)?$")

        /** 在静态上下文里判断是不是 thinking model（init 顺序原因，不能用实例属性）。 */
        private fun isImplicitThinkingModelStatic(model: String): Boolean =
            model.contains("thinking", ignoreCase = true) ||
                model.startsWith("kimi-k", ignoreCase = true) ||
                model.startsWith("o1", ignoreCase = true) ||
                model.startsWith("o3", ignoreCase = true) ||
                model.startsWith("o4", ignoreCase = true)

        private fun defaultHttpClient(callTimeoutSec: Long = 120) = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            // OkHttp 默认 readTimeout = 10s。thinking 模型在第一个 byte 之前
            // 可能要 30-180s 思考；不抬高 readTimeout 的话，callTimeout 调多
            // 大都没用 — read 会先在 10s 超时把连接干掉。和 callTimeout 同档。
            .readTimeout(callTimeoutSec, TimeUnit.SECONDS)
            // Cycle 0031 复修：writeTimeout 之前固定 60s — 多模态请求里
            // base64 图片把请求体撑到 1-3 MB，慢网下传 60+ 秒，触发
            // "Software caused connection abort"。抬到 callTimeout 同档（最多
            // 360s）。压缩在 AddViewModel 那一侧已经做了 —— 这是兜底。
            .writeTimeout(callTimeoutSec, TimeUnit.SECONDS)
            .callTimeout(callTimeoutSec, TimeUnit.SECONDS)
            .build()
    }
}
