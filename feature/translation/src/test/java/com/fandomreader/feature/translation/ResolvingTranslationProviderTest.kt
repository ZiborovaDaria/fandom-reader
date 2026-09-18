package com.fandomreader.feature.translation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class ResolvingTranslationProviderTest {
    @Test
    fun emptyKeyDoesNotCallLiveProvider() {
        var key = ""
        val live = RecordingProvider()
        val resolving = ResolvingTranslationProvider(
            hasKey = { key.isNotBlank() },
            activeProvider = { live },
        )
        assertThrows(MissingApiKeyException::class.java) {
            runBlocking { resolving.translateBatch(listOf("Hello")) }
        }
        assertThat(live.calls).isEmpty()
    }

    @Test
    fun keyAddedAfterEmptyStartDelegatesToLive() = runBlocking {
        var key = ""
        val live = RecordingProvider()
        val resolving = ResolvingTranslationProvider(
            hasKey = { key.isNotBlank() },
            activeProvider = { live },
        )
        key = "test-key"
        val out = resolving.translateBatch(listOf("Hello"))
        assertThat(out).containsExactly("LIVE:Hello")
        assertThat(live.calls).hasSize(1)
    }

    @Test
    fun clearedKeyStopsLiveCalls() {
        var key = "test-key"
        val live = RecordingProvider()
        val resolving = ResolvingTranslationProvider(
            hasKey = { key.isNotBlank() },
            activeProvider = { live },
        )
        runBlocking { resolving.translateBatch(listOf("A")) }
        key = ""
        assertThrows(MissingApiKeyException::class.java) {
            runBlocking { resolving.translateBatch(listOf("B")) }
        }
        assertThat(live.calls).hasSize(1)
    }

    @Test
    fun switchesActiveProviderAtRequestTime() = runBlocking {
        var useCursor = false
        val gemini = RecordingProvider("G")
        val cursor = RecordingProvider("C")
        val resolving = ResolvingTranslationProvider(
            hasKey = { true },
            activeProvider = { if (useCursor) cursor else gemini },
        )
        assertThat(resolving.translateBatch(listOf("a"))).containsExactly("G:a")
        useCursor = true
        assertThat(resolving.translateBatch(listOf("b"))).containsExactly("C:b")
        assertThat(gemini.calls).hasSize(1)
        assertThat(cursor.calls).hasSize(1)
    }

    private class RecordingProvider(
        private val prefix: String = "LIVE",
    ) : TranslationProvider {
        val calls = mutableListOf<List<String>>()
        override suspend fun translateBatch(
            segments: List<String>,
            sourceLang: String,
            targetLang: String,
        ): List<String> {
            calls += segments
            return segments.map { "$prefix:$it" }
        }
    }
}
