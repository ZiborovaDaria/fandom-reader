package com.fandomreader.feature.reader

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ReaderDisplayTextTest {
    @Test
    fun prefersTextRuWhenPresent() {
        val chapter = ReaderChapter(0, "Ch1", "Hello", textRu = "Привет")
        val display = chapter.textRu?.takeIf { it.isNotBlank() } ?: chapter.text
        assertThat(display).isEqualTo("Привет")
    }

    @Test
    fun fallsBackToOriginalWhenTextRuMissing() {
        val chapter = ReaderChapter(0, "Ch1", "Hello", textRu = null)
        val display = chapter.textRu?.takeIf { it.isNotBlank() } ?: chapter.text
        assertThat(display).isEqualTo("Hello")
    }

    @Test
    fun loadsFb2SectionsWithoutCrash() {
        val fb2 = File.createTempFile("sample", ".fb2").apply {
            writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <FictionBook>
                  <body>
                    <section><title><p>One</p></title><p>Alpha</p></section>
                    <section><title><p>Two</p></title><p>Beta</p></section>
                  </body>
                </FictionBook>
                """.trimIndent(),
                Charsets.UTF_8,
            )
            deleteOnExit()
        }
        val chapters = loadChapters(fb2, "fb2")
        assertThat(chapters).hasSize(2)
        assertThat(chapters[0].text).contains("Alpha")
        assertThat(chapters[1].text).contains("Beta")
    }

    @Test
    fun extractChapterBodyKeepsParagraphSeparators() {
        val html = """
            <html><body>
              <p>First paragraph.</p>
              <p>Second paragraph.</p>
            </body></html>
        """.trimIndent()
        val body = extractChapterBody(html)
        assertThat(body).contains(PARAGRAPH_SEPARATOR)
        val content = splitParagraphs(body).filterNot { isSegmentMarker(it) }
        assertThat(content).containsExactly("First paragraph.", "Second paragraph.")
    }

    @Test
    fun fb2KeepsMultipleParagraphs() {
        val fb2 = File.createTempFile("paras", ".fb2").apply {
            writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <FictionBook>
                  <body>
                    <section>
                      <title><p>One</p></title>
                      <p>Alpha one.</p>
                      <p>Alpha two.</p>
                    </section>
                  </body>
                </FictionBook>
                """.trimIndent(),
                Charsets.UTF_8,
            )
            deleteOnExit()
        }
        val chapters = loadChapters(fb2, "fb2")
        assertThat(chapters).hasSize(1)
        val content = splitParagraphs(chapters[0].text).filterNot { isSegmentMarker(it) }
        assertThat(content).containsExactly("Alpha one.", "Alpha two.")
    }

    @Test
    fun ruChapterNotesParsedAsNoteSegment() {
        val text = listOf(
            "Примечания к главе",
            "Примечания автора: Честно говоря, эта глава — эксперимент.",
            "Томас Поттер — только отец называл его «Томом» в лицо — проводит день не лучшим образом.",
            "Миссия сама по себе прошла хорошо, разумеется.",
        ).joinToString(PARAGRAPH_SEPARATOR)
        val segments = parseChapterSegments(text)
        assertThat(segments).hasSize(2)
        assertThat(segments[0].role).isEqualTo(ChapterSegmentRole.Note)
        assertThat(segments[0].paragraphs[0]).isEqualTo("Примечания к главе")
        assertThat(segments[0].paragraphs[1]).startsWith("Примечания автора:")
        assertThat(segments[1].role).isEqualTo(ChapterSegmentRole.Body)
        assertThat(segments[1].paragraphs[0]).startsWith("Томас Поттер")
    }

    @Test
    fun ao3HtmlNotesBecomeNoteSegments() {
        val html = """
            <html><body>
              <h2>Chapter 1</h2>
              <p>Chapter Notes</p>
              <p>See the end of the chapter for notes</p>
              <p>Thomas Potter was not having a good day.</p>
              <p>The mission itself went well, of course.</p>
              <div id="endnotes1">
                <p>Chapter End Notes</p>
                <p>Author aside about Mundungus.</p>
              </div>
            </body></html>
        """.trimIndent()
        val segments = extractChapterSegments(html)
        assertThat(segments.count { it.role == ChapterSegmentRole.Note }).isAtLeast(1)
        assertThat(segments.any { it.role == ChapterSegmentRole.Body }).isTrue()
        val flat = serializeChapterSegments(segments)
        assertThat(flat).contains(NOTE_SEGMENT_MARKER)
        assertThat(flat).contains(BODY_SEGMENT_MARKER)
        val again = parseChapterSegments(flat)
        assertThat(again.any { it.role == ChapterSegmentRole.Note }).isTrue()
    }

    @Test
    fun readerPreferenceDefaultsArePaperSerif() {
        val defaults = ReaderPreferences()
        assertThat(defaults.surfaceTheme.name).isEqualTo("Paper")
        assertThat(defaults.fontFamily).isEqualTo(ReaderFontFamilyOption.Serif)
        assertThat(defaults.fontSizeSp).isEqualTo(18)
        assertThat(defaults.lineHeightMult).isWithin(0.01f).of(1.55f)
    }

    @Test
    fun jumpToChapterChangesIndex() {
        // Pure logic mirror of ViewModel.jumpToChapter bounds check
        val chapters = listOf(
            ReaderChapter(0, "A", "a"),
            ReaderChapter(1, "B", "b"),
            ReaderChapter(2, "C", "c"),
        )
        var chapterIndex = 0
        fun jumpToChapter(index: Int) {
            if (index in chapters.indices) chapterIndex = index
        }
        jumpToChapter(2)
        assertThat(chapterIndex).isEqualTo(2)
        jumpToChapter(99)
        assertThat(chapterIndex).isEqualTo(2)
    }

    @Test
    fun translationCacheFileUsesV3Path() {
        val parent = File("/tmp/books")
        val cache = translationCacheFile(parent, 42L, 3)
        assertThat(cache.path.replace('\\', '/')).endsWith("translations_v3/42/chapter_3.txt")
    }

    @Test
    fun tocLabelPrefersChapterTitle() {
        assertThat(tocLabel("Chapter 1", "Next Best Thing", 1)).isEqualTo("Chapter 1")
    }

    @Test
    fun tocLabelFallsBackForJunkAndWorkTitle() {
        assertThat(tocLabel(null, "Next Best Thing", 2)).isEqualTo("Глава 2")
        assertThat(tocLabel("Preface", "Next Best Thing", 1)).isEqualTo("Глава 1")
        assertThat(tocLabel("Next Best Thing", "Next Best Thing", 1)).isEqualTo("Глава 1")
        assertThat(
            tocLabel(
                "Next Best Thing - KojisApple - Harry Potter - J. K. Rowling",
                "Next Best Thing",
                3,
            ),
        ).isEqualTo("Глава 3")
    }

    @Test
    fun resolveReadingChapterIndexClampsAndResetsFrontMatter() {
        assertThat(resolveReadingChapterIndex(99, 5)).isEqualTo(0)
        assertThat(resolveReadingChapterIndex(2, 5)).isEqualTo(2)
        assertThat(resolveReadingChapterIndex(0, 5, savedChapterLooksLikeFrontMatter = true)).isEqualTo(0)
    }

    @Test
    fun loadEpubExcludesAo3FrontMatter() {
        val candidates = listOf(
            File("Next_Best_Thing.epub"),
            File("../Next_Best_Thing.epub"),
            File("../../Next_Best_Thing.epub"),
            File("fixtures/ao3/Next_Best_Thing.epub"),
            File("../fixtures/ao3/Next_Best_Thing.epub"),
            File("../../fixtures/ao3/Next_Best_Thing.epub"),
        )
        val epub = candidates.firstOrNull { it.exists() } ?: return
        val chapters = loadChapters(epub, "epub")
        assertThat(chapters).isNotEmpty()
        assertThat(chapters[0].text).doesNotContain("Posted originally on the Archive of Our Own")
        assertThat(chapterLooksLikeFrontMatter(chapters[0].text)).isFalse()
        assertThat(chapters.any { it.title?.contains("Chapter", ignoreCase = true) == true }).isTrue()
    }

    @Test
    fun naturalCompareOrdersChapterNumbersAscending() {
        val names = listOf(
            "OEBPS/chapter_1.xhtml",
            "OEBPS/chapter_11.xhtml",
            "OEBPS/chapter_2.xhtml",
            "OEBPS/chapter_10.xhtml",
        )
        val sorted = names.sortedWith { a, b -> naturalCompare(a, b) }
        assertThat(sorted).containsExactly(
            "OEBPS/chapter_1.xhtml",
            "OEBPS/chapter_2.xhtml",
            "OEBPS/chapter_10.xhtml",
            "OEBPS/chapter_11.xhtml",
        ).inOrder()
    }

    @Test
    fun orderEpubHtmlEntriesUsesSpineThenNatural() {
        val entries = listOf(
            "OEBPS/chapter_10.xhtml",
            "OEBPS/chapter_2.xhtml",
            "OEBPS/chapter_1.xhtml",
        )
        val spine = listOf("chapter_1.xhtml", "chapter_2.xhtml", "chapter_10.xhtml")
        assertThat(orderEpubHtmlEntries(entries, spine)).containsExactly(
            "OEBPS/chapter_1.xhtml",
            "OEBPS/chapter_2.xhtml",
            "OEBPS/chapter_10.xhtml",
        ).inOrder()
    }

    @Test
    fun ficbookChapterKeepsParagraphsFromBr() {
        val candidates = listOf(
            File("fixtures/ficbook/Potter-kotoryj-sovsem-ne-Potter-1.epub"),
            File("../fixtures/ficbook/Potter-kotoryj-sovsem-ne-Potter-1.epub"),
            File("../../fixtures/ficbook/Potter-kotoryj-sovsem-ne-Potter-1.epub"),
            File("Potter-kotoryj-sovsem-ne-Potter-1.epub"),
        )
        val epub = candidates.firstOrNull { it.exists() } ?: return
        val chapters = loadChapters(epub, "epub")
        assertThat(chapters).isNotEmpty()
        // After title.xhtml exclusion, first story chapter should have multiple paragraphs
        val first = chapters.first()
        assertThat(splitParagraphs(first.text).size).isAtLeast(2)
    }
}
