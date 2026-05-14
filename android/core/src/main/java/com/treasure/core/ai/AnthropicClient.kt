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
 * Hand-rolled Anthropic Messages API client. Uses tool-use to force
 * structured output — the model must call `fill_item_draft`, so the
 * response is always parseable as an [ItemDraft] (no markdown / no prose).
 */
class AnthropicClient(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://api.anthropic.com",
    /** null = 用 provider 的默认采样温度 */
    private val temperature: Double? = null,
    /** true 时给 messages 请求加 `thinking` block */
    private val thinkingEnabled: Boolean = false,
    httpClient: OkHttpClient? = null,
) : AiClient {

    private val client: OkHttpClient = httpClient ?: defaultHttpClient(
        callTimeoutSec = if (thinkingEnabled) 360L else 120L,
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
                    .url("$baseUrl/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
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
            // Cycle 0032：多 action 时 tool call 体积变大（4 件物品 × 8 specs
            // ≈ 700 token）；max_tokens 上调一档，留 4096 给 thinking。
            put("max_tokens", if (thinkingEnabled) 8192 else 4096)
            put("system", buildSystemWithWorkingSet(workingSet, json, categoryHints, images.size))
            temperature?.let { put("temperature", it) }
            if (thinkingEnabled) {
                putJsonObject("thinking") {
                    put("type", "enabled")
                    put("budget_tokens", 2048)
                }
            }
            putJsonArray("tools") {
                add(buildJsonObject {
                    put("name", EXTRACT_TOOL_NAME)
                    put("description", "Fill out the structured Treasure item draft form")
                    put("input_schema", toolSchema)
                })
            }
            // thinking 开启时不能强制 tool_choice = tool —— 让模型 auto 选
            if (thinkingEnabled) {
                putJsonObject("tool_choice") { put("type", "auto") }
            } else {
                putJsonObject("tool_choice") {
                    put("type", "tool")
                    put("name", EXTRACT_TOOL_NAME)
                }
            }
            putJsonArray("messages") {
                priorTurns.forEach { turn ->
                    add(buildJsonObject {
                        put("role", if (turn.role == AiRole.USER) "user" else "assistant")
                        putJsonArray("content") {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", turn.text)
                            })
                        }
                    })
                }
                add(buildJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        // Cycle 0034：多张图作为独立 image block 按顺序送入，
                        // 顺序 = photo_assignments 里 source_index 的顺序。
                        images.forEach { img ->
                            val b64 = Base64.getEncoder().encodeToString(img)
                            add(buildJsonObject {
                                put("type", "image")
                                putJsonObject("source") {
                                    put("type", "base64")
                                    put("media_type", "image/jpeg")
                                    put("data", b64)
                                }
                            })
                        }
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", "User description:\n\n$text")
                        })
                    }
                })
            }
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun parseDrafts(body: String): List<DraftAction> {
        val response = json.parseToJsonElement(body).jsonObject
        val content = response["content"]?.jsonArray
            ?: throw IllegalStateException("response missing 'content'")
        // 优先：tool_use block（强制 tool_choice 时一定有）
        val toolUse = content.firstOrNull {
            it.jsonObject["type"]?.jsonPrimitive?.content == "tool_use"
        }?.jsonObject
        if (toolUse != null) {
            val input = toolUse["input"]?.jsonObject
                ?: throw IllegalStateException("tool_use missing 'input'")
            return parseActionsObject(input)
        }
        // 回退：thinking 模式 + tool_choice=auto 时，模型可能把 JSON 直接写
        // 在 text block 里。抓不到 JSON 就当用户没提物品 — 模型聊天回复 surface
        // 成普通助手消息，不当错误。
        val textBlock = content.firstOrNull {
            it.jsonObject["type"]?.jsonPrimitive?.content == "text"
        }?.jsonObject?.get("text")?.jsonPrimitive?.content
            ?: throw IllegalStateException("response had no tool_use or text block")
        val jsonChunk = extractFirstJsonObject(textBlock)
        if (jsonChunk != null) {
            val parsed = json.parseToJsonElement(jsonChunk).jsonObject
            return parseActionsObject(parsed)
        }
        throw ChatOnlyResponseException(textBlock.trim())
    }

    private fun parseActionsObject(obj: JsonObject): List<DraftAction> {
        val actionsArr = obj["actions"]?.jsonArray
            ?: throw IllegalStateException("tool response missing 'actions'")
        return actionsArr.map { el ->
            json.decodeFromJsonElement(DraftAction.serializer(), el)
        }
    }

    companion object {
        const val DEFAULT_MODEL = "claude-haiku-4-5-20251001"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private fun defaultHttpClient(callTimeoutSec: Long = 120) = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            // OkHttp 默认 readTimeout = 10s。thinking 模型第一个 byte 可能
            // 30-180s 之后才到，不抬高这条 callTimeout 调多大都救不了。
            .readTimeout(callTimeoutSec, TimeUnit.SECONDS)
            // Cycle 0031 复修：writeTimeout 之前固定 60s — 大图 base64 后请
            // 求体 1-3 MB，慢网传不完 60s 就 abort。跟 callTimeout 同档。
            .writeTimeout(callTimeoutSec, TimeUnit.SECONDS)
            .callTimeout(callTimeoutSec, TimeUnit.SECONDS)
            .build()
    }
}
