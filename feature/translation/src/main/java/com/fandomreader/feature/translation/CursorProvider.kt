package com.fandomreader.feature.translation

import android.util.Log
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Temporary Cursor-backed translator via OpenAI-compatible chat completions.
 * Endpoint/model are configurable constants — remove when a durable provider replaces Cursor.
 */
@Singleton
class CursorProvider @Inject constructor(
    private val settings: TranslationProviderSettings,
) : TranslationProvider {
    private val client = OkHttpClient.Builder()
        .callTimeout(180, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    override fun preferredChunkChars(): Int = CHUNK_CHARS

    override suspend fun translateBatch(
        segments: List<String>,
        sourceLang: String,
        targetLang: String,
    ): List<String> {
        val key = settings.cursorApiKey
        if (key.isBlank()) throw MissingApiKeyException()
        Log.i(TAG, "translateBatch segments=${segments.size} via Cursor model=${settings.cursorModel}")
        return segments.map { segment ->
            translateOne(segment, sourceLang, targetLang, key)
        }
    }

    private suspend fun translateOne(
        text: String,
        sourceLang: String,
        targetLang: String,
        key: String,
    ): String = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("model", settings.cursorModel)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put(
                                "content",
                                "You are a translation engine. Translate the user text from " +
                                    "$sourceLang to $targetLang. Preserve paragraph breaks and " +
                                    "segment markers exactly. Output only the translation.",
                            ),
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", "Переведи на русский:\n\n$text"),
                    ),
            )
            .put("temperature", 0.2)
        val base = settings.cursorBaseUrl.trimEnd('/')
        val request = Request.Builder()
            .url("$base/chat/completions")
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("Cursor HTTP ${response.code}: ${raw.take(200)}")
            }
            val json = JSONObject(raw)
            val content = json
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                .orEmpty()
            content.ifBlank { throw IllegalStateException("Cursor empty translation") }
        }
    }

    companion object {
        private const val TAG = "CursorTranslate"
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val CHUNK_CHARS = 24_000
        private val JSON_MEDIA = "application/json".toMediaType()
    }
}
