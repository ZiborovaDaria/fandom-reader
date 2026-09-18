package com.fandomreader.feature.translation

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class GeminiModelLadderTest {
    @Test
    fun primaryChunkBudgetIsLarge() {
        assertThat(GeminiProvider.TIERS.first().id).isEqualTo("gemini-3.5-flash-lite")
        assertThat(GeminiProvider.TIERS.first().maxChunkChars)
            .isEqualTo(TranslationProvider.DEFAULT_CHUNK_CHARS)
        assertThat(GeminiProvider.TIERS.first().maxChunkChars).isAtLeast(50_000)
    }

    @Test
    fun fallbackTiersTightenChunkBudget() {
        val budgets = GeminiProvider.TIERS.map { it.maxChunkChars }
        assertThat(budgets.zipWithNext().all { (a, b) -> a > b }).isTrue()
    }

    @Test
    fun resolvingDelegatesChunkBudget() {
        val live = object : TranslationProvider {
            override fun preferredChunkChars(): Int = 12_345
            override suspend fun translateBatch(
                segments: List<String>,
                sourceLang: String,
                targetLang: String,
            ): List<String> = segments
        }
        val resolving = ResolvingTranslationProvider(
            hasKey = { true },
            activeProvider = { live },
        )
        assertThat(resolving.preferredChunkChars()).isEqualTo(12_345)
    }

    @Test
    fun forceRetranslateClearsChapterCache() = runBlocking {
        val repo = FakeRepo()
        val id = repo.upsertWork(
            com.fandomreader.domain.model.Work(
                source = com.fandomreader.domain.model.WorkSource.AO3,
                remoteId = "force-1",
                sourceUrl = null,
                title = "Title",
                summary = "Summary",
                localPath = "x",
                format = "epub",
                shelf = com.fandomreader.domain.model.Shelf.FANFICTION,
            ),
        )
        val tmp = org.junit.rules.TemporaryFolder().also { it.create() }
        try {
            val calls = AtomicInteger(0)
            val counting = object : TranslationProvider {
                override suspend fun translateBatch(
                    segments: List<String>,
                    sourceLang: String,
                    targetLang: String,
                ): List<String> {
                    calls.incrementAndGet()
                    return segments.map { "[ru] $it" }
                }
            }
            val pipeline = TranslationPipeline(counting, repo)
            pipeline.translateWork(id, tmp.root, listOf("Hello\n\nWorld"), force = false)
            val afterFirst = calls.get()
            pipeline.translateWork(id, tmp.root, listOf("Hello\n\nWorld"), force = false)
            assertThat(calls.get()).isEqualTo(afterFirst) // cache hit
            pipeline.translateWork(id, tmp.root, listOf("Hello\n\nWorld"), force = true)
            assertThat(calls.get()).isGreaterThan(afterFirst)
            assertThat(repo.getWork(id)!!.titleRu).startsWith("[ru]")
        } finally {
            tmp.delete()
        }
    }

    @Test
    fun emptyKeyStillThrows() {
        val live = FakeTranslationProvider()
        val resolving = ResolvingTranslationProvider(
            hasKey = { false },
            activeProvider = { live },
        )
        assertThrows(MissingApiKeyException::class.java) {
            runBlocking { resolving.translateBatch(listOf("x")) }
        }
    }
}