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

    override suspend fun extractItemDraft(
        text: String,
        imageJpegBytes: ByteArray?,
        priorTurns: List<AiTurn>,
        baseline: ItemDraft?,
    ): Result<ItemDraft> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPayload(text, imageJpegBytes, priorTurns, baseline)
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
                parseDraft(body)
            }
        }
    }

    private fun buildPayload(
        text: String,
        image: ByteArray?,
        priorTurns: List<AiTurn>,
        baseline: ItemDraft?,
    ): String {
        val toolSchema = json.parseToJsonElement(EXTRACT_TOOL_PARAMETERS).jsonObject
        val payload = buildJsonObject {
            put("model", model)
            put("max_tokens", if (thinkingEnabled) 4096 else 1024)
            put("system", buildSystemWithBaseline(baseline, json))
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
                        if (image != null) {
                            val b64 = Base64.getEncoder().encodeToString(image)
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

    private fun parseDraft(body: String): ItemDraft {
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
            return json.decodeFromJsonElement(ItemDraft.serializer(), input)
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
            return json.decodeFromString(ItemDraft.serializer(), jsonChunk)
        }
        throw ChatOnlyResponseException(textBlock.trim())
    }

    companion object {
        const val DEFAULT_MODEL = "claude-haiku-4-5-20251001"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private fun defaultHttpClient(callTimeoutSec: Long = 120) = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            // OkHttp 默认 readTimeout = 10s。thinking 模型第一个 byte 可能
            // 30-180s 之后才到，不抬高这条 callTimeout 调多大都救不了。
            .readTimeout(callTimeoutSec, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(callTimeoutSec, TimeUnit.SECONDS)
            .build()
    }
}
