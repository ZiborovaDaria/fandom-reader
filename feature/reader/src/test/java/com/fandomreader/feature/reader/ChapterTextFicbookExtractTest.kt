package com.fandomreader.feature.reader

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChapterTextFicbookExtractTest {
    @Test
    fun divBrNarrativeWithSceneBreaksKeepsStoryText() {
        val html = """
            <html><body>
            <div>Однажды в Хогвартсе случилось странное.</div>
            <br/>
            <div>***</div>
            <br/>
            <div>Гарри посмотрел на Тома и кивнул.</div>
            </body></html>
        """.trimIndent()
        val text = extractChapterBody(html)
        assertThat(text).contains("Однажды в Хогвартсе")
        assertThat(text).contains("Гарри посмотрел")
        val bodyParas = splitParagraphs(text).filter { !isSegmentMarker(it) }
        assertThat(bodyParas.any { it.length > 3 && !it.trim().matches(Regex("""\*+""")) }).isTrue()
    }

    @Test
    fun doesNotCollapseToOnlyAsterisksWhenNarrativePresent() {
        val html = """
            <body>
            <div>Первый абзац истории про волшебников.</div><br/>
            <div>***</div><br/>
            <div>Второй абзац продолжает сюжет дальше.</div>
            </body>
        """.trimIndent()
        val paras = splitParagraphs(extractChapterBody(html))
            .filter { !isSegmentMarker(it) }
        assertThat(paras.any { it.contains("Первый абзац") }).isTrue()
        assertThat(paras.any { it.contains("Второй абзац") }).isTrue()
        assertThat(paras.all { it.trim() == "***" }).isFalse()
    }

    @Test
    fun chapterParseCacheInvalidatesOnFileIdentityChange() {
        val tmp = org.junit.rules.TemporaryFolder().also { it.create() }
        try {
            val file = tmp.newFile("a.epub")
            file.writeText("v1")
            ChapterParseCache.put(
                1L,
                file,
                listOf(ReaderChapter(0, "c", "hello", null)),
            )
            assertThat(ChapterParseCache.get(1L, file)).isNotNull()
            file.writeText("v2-longer-content")
            // length/mtime changed → miss
            assertThat(ChapterParseCache.get(1L, file)).isNull()
        } finally {
            ChapterParseCache.clear()
            tmp.delete()
        }
    }
}
