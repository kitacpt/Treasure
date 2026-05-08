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
                parseDraft(body)
            }
        }
    }

    private fun buildUrl(): String =
        if (baseUrl.endsWith("/v1")) "$baseUrl/chat/completions"
        else "${baseUrl.trimEnd('/')}/v1/chat/completions"

    private fun buildPayload(text: String, image: ByteArray?): String {
        val toolSchema = json.parseToJsonElement(EXTRACT_TOOL_PARAMETERS).jsonObject
        val payload = buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })
                add(buildJsonObject {
                    put("role", "user")
                    if (image != null) {
                        // OpenAI vision: array content with text + image_url blocks
                        putJsonArray("content") {
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", "User description:\n\n$text")
                            })
                            val b64 = Base64.getEncoder().encodeToString(image)
                            add(buildJsonObject {
                                put("type", "image_url")
                                putJsonObject("image_url") {
                                    put("url", "data:image/jpeg;base64,$b64")
                                }
                            })
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
            putJsonObject("tool_choice") {
                put("type", "function")
                putJsonObject("function") { put("name", EXTRACT_TOOL_NAME) }
            }
        }
        return json.encodeToString(JsonObject.serializer(), payload)
    }

    private fun parseDraft(body: String): ItemDraft {
        val response = json.parseToJsonElement(body).jsonObject
        val choices = response["choices"]?.jsonArray
            ?: throw IllegalStateException("response missing 'choices'")
        val first = choices.firstOrNull()?.jsonObject
            ?: throw IllegalStateException("empty choices")
        val message = first["message"]?.jsonObject
            ?: throw IllegalStateException("choice missing 'message'")
        val toolCalls = message["tool_calls"]?.jsonArray
            ?: throw IllegalStateException("no tool_calls — model didn't call the tool")
        val toolCall = toolCalls.firstOrNull()?.jsonObject
            ?: throw IllegalStateException("empty tool_calls")
        val function = toolCall["function"]?.jsonObject
            ?: throw IllegalStateException("tool_call missing 'function'")
        val argumentsString = function["arguments"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("function missing 'arguments'")
        // arguments is a JSON string we still need to parse into ItemDraft
        return json.decodeFromString(ItemDraft.serializer(), argumentsString)
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private fun defaultHttpClient() = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
