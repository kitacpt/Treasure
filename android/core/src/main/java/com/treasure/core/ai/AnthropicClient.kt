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
    httpClient: OkHttpClient? = null,
) : AiClient {

    private val client: OkHttpClient = httpClient ?: defaultHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun extractItemDraft(
        text: String,
        imageJpegBytes: ByteArray?,
    ): Result<ItemDraft> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = buildPayload(text, imageJpegBytes)
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

    private fun buildPayload(text: String, image: ByteArray?): String {
        val toolSchema = json.parseToJsonElement(EXTRACT_TOOL_PARAMETERS).jsonObject
        val payload = buildJsonObject {
            put("model", model)
            put("max_tokens", 1024)
            put("system", SYSTEM_PROMPT)
            putJsonArray("tools") {
                add(buildJsonObject {
                    put("name", EXTRACT_TOOL_NAME)
                    put("description", "Fill out the structured Treasure item draft form")
                    put("input_schema", toolSchema)
                })
            }
            putJsonObject("tool_choice") {
                put("type", "tool")
                put("name", EXTRACT_TOOL_NAME)
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

    private fun parseDraft(body: String): ItemDraft {
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
        private fun defaultHttpClient() = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
