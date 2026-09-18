package com.fandomreader.feature.translation

import android.util.Log
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Model ladder for free-tier / rate-limit fallback.
 *
 * gemini-3.5-flash-lite: 1_048_576 input / 65_536 output tokens (official).
 * Chunk budgets are in characters, paragraph-aligned by [TranslationPipeline],
 * sized so EN→RU output stays under the model output limit with headroom.
 */
internal data class GeminiModelTier(
    val id: String,
    val maxChunkChars: Int,
)

@Singleton
class GeminiProvider @Inject constructor(
    private val settings: GeminiSettings,
) : TranslationProvider {
    private val client = OkHttpClient.Builder()
        .callTimeout(180, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val tierIndex = AtomicInteger(0)

    override fun preferredChunkChars(): Int = currentTier().maxChunkChars

    override suspend fun translateBatch(
        segments: List<String>,
        sourceLang: String,
        targetLang: String,
    ): List<String> {
        val key = settings.apiKey
        if (key.isBlank()) throw MissingApiKeyException()
        Log.i(TAG, "translateBatch segments=${segments.size} $sourceLang->$targetLang model=${currentTier().id}")
        return segments.mapIndexed { index, segment ->
            translateOne(segment, sourceLang, targetLang, key, index, segments.size)
        }
    }

    private fun currentTier(): GeminiModelTier = TIERS[tierIndex.get().coerceIn(0, TIERS.lastIndex)]

    private fun advanceTier(): Boolean {
        while (true) {
            val cur = tierIndex.get()
            if (cur >= TIERS.lastIndex) return false
            if (tierIndex.compareAndSet(cur, cur + 1)) {
                Log.w(TAG, "fallback model ${TIERS[cur].id} → ${TIERS[cur + 1].id} (chunk≤${TIERS[cur + 1].maxChunkChars})")
                return true
            }
        }
    }

    private suspend fun translateOne(
        text: String,
        sourceLang: String,
        targetLang: String,
        key: String,
        index: Int,
        total: Int,
    ): String = withContext(Dispatchers.IO) {
        var attempt = 0
        var lastError: Exception? = null
        while (attempt < 10) {
            attempt++
            val tier = currentTier()
            try {
                val body = JSONObject()
                    .put(
                        "contents",
                        JSONArray().put(
                            JSONObject().put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put(
                                        "text",
                                        "Translate the following literary text from $sourceLang to $targetLang. " +
                                            "Keep character names consistent. Preserve blank-line paragraph breaks. " +
                                            "Return only the translation.\n\n$text",
                                    ),
                                ),
                            ),
                        ),
                    )
                // Key only in request URL — never log the URL.
                val request = Request.Builder()
                    .url(
                        "https://generativelanguage.googleapis.com/v1beta/models/" +
                            "${tier.id}:generateContent?key=$key",
                    )
                    .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                val response = client.newCall(request).execute()
                try {
                    val code = response.code
                    if (code == 429) {
                        val backoffMs = (1500L * (1 shl (attempt - 1).coerceAtMost(5))).coerceAtMost(45_000L)
                        Log.w(
                            TAG,
                            "HTTP 429 model=${tier.id} segment=${index + 1}/$total " +
                                "attempt=$attempt backoffMs=$backoffMs",
                        )
                        lastError = IOException("Gemini HTTP 429")
                        // After a few 429s on this model, fall back to the next tier.
                        if (attempt >= 3 && advanceTier()) {
                            attempt = 0
                            continue
                        }
                        delay(backoffMs)
                        continue
                    }
                    if (code == 404) {
                        // Retired / unavailable model — fall back immediately.
                        Log.e(TAG, "HTTP 404 model=${tier.id} — switching tier")
                        lastError = IOException("Gemini HTTP 404 model=${tier.id}")
                        if (advanceTier()) {
                            attempt = 0
                            continue
                        }
                        throw lastError
                    }
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string().orEmpty().take(200)
                        Log.e(
                            TAG,
                            "HTTP $code model=${tier.id} segment=${index + 1}/$total " +
                                "body=${errBody.replace(key, "REDACTED")}",
                        )
                        throw IOException("Gemini HTTP $code")
                    }
                    val json = JSONObject(response.body!!.string())
                    val out = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()
                    Log.i(
                        TAG,
                        "OK model=${tier.id} segment=${index + 1}/$total chars=${out.length}",
                    )
                    return@withContext out
                } finally {
                    response.close()
                }
            } catch (e: Exception) {
                lastError = e
                Log.w(
                    TAG,
                    "retry model=${currentTier().id} segment=${index + 1}/$total " +
                        "attempt=$attempt: ${e.javaClass.simpleName}: ${e.message}",
                )
                delay(500L * attempt)
            }
        }
        throw lastError ?: IOException("Gemini translation failed")
    }

    companion object {
        private const val TAG = "FandomGemini"

        /**
         * Primary: 3.5 Flash-Lite (1M / 65k out) → large literary chunks.
         * Fallbacks: separate quota pools / older Flash-Lite with tighter chunk budgets.
         */
        internal val TIERS = listOf(
            GeminiModelTier("gemini-3.5-flash-lite", maxChunkChars = 100_000),
            GeminiModelTier("gemini-3.1-flash-lite", maxChunkChars = 40_000),
            GeminiModelTier("gemini-2.5-flash-lite", maxChunkChars = 16_000),
        )
    }
}
