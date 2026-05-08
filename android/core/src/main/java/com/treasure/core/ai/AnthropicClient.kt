package com.treasure.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
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
 *
 * Built with stock OkHttp + kotlinx.serialization (no Anthropic SDK dep).
 */
class AnthropicClient(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://api.anthropic.com",
    httpClient: OkHttpClient? = null,
) : AiClient {

    private val client: OkHttpClient = httpClient ?: OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)  // vision can be slow
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extractItemDraft(
        text: String,
        imageJpegBytes: ByteArray?,
    ): Result<ItemDraft> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildExtractPayload(text, imageJpegBytes)
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
                parseDraftFromResponse(body)
            }
        }
    }

    private fun buildExtractPayload(text: String, image: ByteArray?): String {
        val payload = buildJsonObject {
            put("model", model)
            put("max_tokens", 1024)
            put("system", SYSTEM_PROMPT)
            putJsonArray("tools") { add(EXTRACT_TOOL_SCHEMA) }
            putJsonObject("tool_choice") {
                put("type", "tool")
                put("name", "fill_item_draft")
            }
            putJsonArray("messages") {
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

    private fun parseDraftFromResponse(body: String): ItemDraft {
        val response = json.parseToJsonElement(body).jsonObject
        val content = response["content"]?.jsonArray
            ?: throw IllegalStateException("response missing 'content'")
        val toolUse = content.firstOrNull {
            it.jsonObject["type"]?.jsonPrimitive?.content == "tool_use"
        }?.jsonObject ?: throw IllegalStateException("response had no tool_use block")
        val input = toolUse["input"]?.jsonObject
            ?: throw IllegalStateException("tool_use missing 'input'")
        return json.decodeFromJsonElement(ItemDraft.serializer(), input)
    }

    companion object {
        const val DEFAULT_MODEL = "claude-haiku-4-5-20251001"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

private const val SYSTEM_PROMPT = """You are a museum cataloguer for Treasure, a personal-collection app.
Given a user's description (and optionally a photo) of an item they own, fill out the structured form by
calling the fill_item_draft tool. Never reply with prose; always call the tool.

Categories — pick exactly one of:
  badminton  (羽毛球: rackets, shuttles, badminton shoes, …)
  photo      (摄影: cameras, lenses, tripods, …)
  cars       (汽车: rented or owned cars)
  tech       (电子产品: laptops, phones, watches, earbuds, e-readers, tablets, …)

Brand + model: actual product names. Don't make them up — if you can't tell, leave them empty.
Nickname: optional, short Chinese pet name (e.g. "黑刃" for a black racket); leave empty unless the user gave one.
oneLiner: one short Chinese line, like "进攻型 4U · 拉26磅" or "APS-C 旗舰 · 4020 万像素".

Hero specs: provide exactly 4, in this category-specific order. Use empty string for values you can't tell.
  badminton: [重量, 平衡点, 中杆, 握把]
  photo:     [传感器, 像素, 机身防抖, 快门]
  cars:      [动力, 马力, 0-100, 驱动]
  tech:      [CPU, 内存, 存储, 屏幕]
"""

private val EXTRACT_TOOL_SCHEMA: JsonObject = buildJsonObject {
    put("name", "fill_item_draft")
    put("description", "Fill out the structured Treasure item draft form")
    putJsonObject("input_schema") {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("category") {
                put("type", "string")
                putJsonArray("enum") {
                    add("badminton"); add("photo"); add("cars"); add("tech")
                }
            }
            putJsonObject("brand") { put("type", "string") }
            putJsonObject("model") { put("type", "string") }
            putJsonObject("nickname") { put("type", "string") }
            putJsonObject("oneLiner") { put("type", "string") }
            putJsonObject("heroSpecs") {
                put("type", "array")
                put(
                    "description",
                    "Exactly 4 specs, in category-specific order. Empty values for unknowns.",
                )
                putJsonObject("items") {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("label") { put("type", "string") }
                        putJsonObject("value") { put("type", "string") }
                    }
                    putJsonArray("required") { add("label"); add("value") }
                }
            }
        }
        putJsonArray("required") {
            add("category"); add("brand"); add("model"); add("oneLiner"); add("heroSpecs")
        }
    }
}
