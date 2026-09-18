package com.fandomreader.feature.reader

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.zip.ZipFile
import org.junit.Test

/**
 * One-shot probe for AO3/Ficbook sample EPUBs: chapter count, paragraph density, front-matter.
 * Not a golden; fails only if a sample is unreadable or collapses to a single wall of text.
 */
class SampleBooksFormatProbeTest {
    @Test
    fun probeRootSampleBooks() {
        val names = listOf(
            "His_Darkest_Devotion.epub",
            "The_Farland_Files.epub",
            "Potter-kotoryj-sovsem-ne-Potter-1.epub",
            "Prizrak-prezrenia.epub",
            "Vlozit-dusu.epub",
        )
        val reports = mutableListOf<String>()
        for (name in names) {
            val file = resolve(name) ?: error("Missing $name")
            val kind = detectKind(file)
            val chapters = loadChapters(file, "epub")
            assertThat(chapters).isNotEmpty()
            val first = chapters.first()
            val paras = splitParagraphs(first.text)
            val report = buildString {
                appendLine("=== $name ($kind) size=${file.length()}")
                appendLine("chapters=${chapters.size}")
                appendLine("firstTitle=${first.title}")
                appendLine("firstParas=${paras.size} firstChars=${first.text.length}")
                appendLine("frontMatter=${chapterLooksLikeFrontMatter(first.text)}")
                appendLine("sample=${paras.take(2).joinToString(" | ") { it.take(80) }}")
                appendLine("toc=${chapters.take(8).mapIndexed { i, c -> "${i + 1}:${c.title}" }}")
            }
            reports += report
            println(report)
            assertThat(paras.size).isAtLeast(2)
            assertThat(chapterLooksLikeFrontMatter(first.text)).isFalse()
        }
        assertThat(reports).hasSize(5)
    }

    @Test
    fun ao3CalibreSummaryPageExcludedFromSpine() {
        for (name in listOf("His_Darkest_Devotion.epub", "The_Farland_Files.epub")) {
            val epub = resolve(name) ?: continue
            val chapters = loadChapters(epub, "epub")
            assertThat(chapters).isNotEmpty()
            val first = chapters.first()
            assertThat(first.title.orEmpty()).ignoringCase().contains("Chapter")
            assertThat(splitParagraphs(first.text).filterNot { isSegmentMarker(it) }.firstOrNull().orEmpty())
                .doesNotContain("Summary")
        }
    }

    private fun detectKind(file: File): String {
        ZipFile(file).use { zip ->
            val text = zip.entries().asSequence()
                .filter {
                    !it.isDirectory && (
                        it.name.endsWith(".xhtml", true) ||
                            it.name.endsWith(".html", true) ||
                            it.name.endsWith(".opf", true)
                        )
                }
                .take(12)
                .joinToString("\n") { e ->
                    zip.getInputStream(e).bufferedReader().use { it.readText() }
                }
            return when {
                text.contains("archiveofourown.org", ignoreCase = true) ||
                    text.contains("Archive of Our Own", ignoreCase = true) -> "AO3"
                text.contains("ficbook.net", ignoreCase = true) -> "Ficbook"
                else -> "Other?"
            }
        }
    }

    private fun resolve(name: String): File? {
        val candidates = listOf(
            File(name),
            File("../$name"),
            File("../../$name"),
            File("fixtures/ficbook/$name"),
            File("../fixtures/ficbook/$name"),
            File("../../fixtures/ficbook/$name"),
            File("fixtures/ao3/$name"),
        )
        return candidates.firstOrNull { it.exists() }
    }
}
