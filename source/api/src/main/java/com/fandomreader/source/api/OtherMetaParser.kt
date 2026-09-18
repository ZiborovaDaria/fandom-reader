package com.fandomreader.source.api

import com.fandomreader.domain.model.WorkSource
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File

/**
 * Fallback parser for non-portal books: basic EPUB OPF / FB2 title-info only.
 */
class OtherMetaParser : BookMetaParser {
    override fun supports(kind: SourceKind): Boolean = kind == SourceKind.OTHER

    override fun parse(file: File): ParsedWorkMeta {
        return when (BookFormatDetect.detect(file)) {
            "epub" -> parseEpub(file)
            "fb2" -> parseFb2(file)
            else -> ParsedWorkMeta(
                source = WorkSource.OTHER,
                remoteId = null,
                sourceUrl = null,
                title = file.nameWithoutExtension,
                author = null,
                summary = null,
                language = null,
                fandoms = emptyList(),
                pairings = emptyList(),
                format = EpubIo.formatOf(file),
            )
        }
    }

    private fun parseEpub(file: File): ParsedWorkMeta {
        val opf = runCatching { EpubIo.readOpf(file) }.getOrNull()
        val title = opf?.let { EpubIo.dcText(it, "title") } ?: file.nameWithoutExtension
        val author = opf?.let { EpubIo.dcText(it, "creator") }
        val summary = MetaNormalize.stripHtml(opf?.let { EpubIo.dcText(it, "description") })
        val language = opf?.let { EpubIo.dcText(it, "language") }
        return ParsedWorkMeta(
            source = WorkSource.OTHER,
            remoteId = null,
            sourceUrl = null,
            title = title,
            author = author,
            summary = summary,
            language = language,
            fandoms = emptyList(),
            pairings = emptyList(),
            format = "epub",
        )
    }

    private fun parseFb2(file: File): ParsedWorkMeta {
        return try {
            val xml = file.readText(Charsets.UTF_8)
            val doc = Jsoup.parse(xml, "", Parser.xmlParser())
            val titleInfo = doc.selectFirst("description > title-info")
            val title = titleInfo?.selectFirst("book-title")?.text()?.trim()
                ?.takeUnless { it.isBlank() || it.equals("Unknown", ignoreCase = true) }
                ?: file.nameWithoutExtension
            val author = titleInfo?.selectFirst("author")?.let { authorEl ->
                listOfNotNull(
                    authorEl.selectFirst("first-name")?.text()?.trim(),
                    authorEl.selectFirst("middle-name")?.text()?.trim(),
                    authorEl.selectFirst("last-name")?.text()?.trim(),
                ).filter { it.isNotEmpty() }.joinToString(" ").ifBlank { null }
            }
            val summary = titleInfo?.selectFirst("annotation")?.text()?.trim()?.ifBlank { null }
            val language = titleInfo?.selectFirst("lang")?.text()?.trim()?.ifBlank { null }
            ParsedWorkMeta(
                source = WorkSource.OTHER,
                remoteId = null,
                sourceUrl = null,
                title = title,
                author = author,
                summary = summary,
                language = language,
                fandoms = emptyList(),
                pairings = emptyList(),
                format = "fb2",
            )
        } catch (_: Exception) {
            ParsedWorkMeta(
                source = WorkSource.OTHER,
                remoteId = null,
                sourceUrl = null,
                title = file.nameWithoutExtension,
                author = null,
                summary = null,
                language = null,
                fandoms = emptyList(),
                pairings = emptyList(),
                format = "fb2",
            )
        }
    }
}
