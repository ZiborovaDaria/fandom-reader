package com.fandomreader.source.ficbook

import com.fandomreader.domain.model.DisplayTag
import com.fandomreader.domain.model.DisplayTagGroups
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.source.api.BookMetaParser
import com.fandomreader.source.api.EpubIo
import com.fandomreader.source.api.MetaNormalize
import com.fandomreader.source.api.ParsedWorkMeta
import com.fandomreader.source.api.SourceKind
import org.jsoup.Jsoup
import java.io.File

class FicbookEpubParser : BookMetaParser {
    private val workUrlRegex = Regex(
        """https?://(?:www\.)?ficbook\.net/readfic/(\d+)""",
        RegexOption.IGNORE_CASE,
    )

    override fun supports(kind: SourceKind): Boolean = kind == SourceKind.FICBOOK

    override fun parse(file: File): ParsedWorkMeta {
        require(file.name.lowercase().endsWith(".epub")) {
            "FicbookEpubParser expects .epub, got ${file.name}"
        }
        val opf = EpubIo.readOpf(file)
        val title = EpubIo.dcText(opf, "title") ?: file.nameWithoutExtension
        val author = EpubIo.dcText(opf, "creator")
        val language = EpubIo.dcText(opf, "language")
        val summary = MetaNormalize.stripHtml(EpubIo.dcText(opf, "description"))

        val titlePage = EpubIo.findFirstEntry(file) { name ->
            name.substringAfterLast('/').equals("title.xhtml", ignoreCase = true)
        } ?: EpubIo.findFirstEntry(file) { name ->
            val lower = name.lowercase()
            lower.endsWith(".xhtml") || lower.endsWith(".html")
        }?.takeIf { (_, text) ->
            text.contains("Фэндом") && text.contains("ficbook.net")
        }

        val titleDoc = titlePage?.let { Jsoup.parse(it.second) }
        val fandoms = titleDoc?.let { extractLabeledValues(it, "Фэндом") }
            ?.flatMap { splitFandoms(it) }
            ?.map(MetaNormalize::fandom)
            .orEmpty()

        val pairings = titleDoc?.let { extractLabeledValues(it, "Пэйринг и персонажи") }
            ?.flatMap { splitPairingField(it) }
            .orEmpty()

        val blob = listOfNotNull(titlePage?.second, EpubIo.dcText(opf, "rights")).joinToString("\n")
        val sourceUrl = workUrlRegex.find(blob)?.let {
            "https://ficbook.net/readfic/${it.groupValues[1]}"
        }
        val remoteId = sourceUrl?.let { workUrlRegex.find(it)?.groupValues?.get(1) }

        val displayTags = buildList {
            titleDoc?.let { doc ->
                extractLabeledValues(doc, "Рейтинг").forEach {
                    add(DisplayTag(DisplayTagGroups.RATING, it))
                }
                extractLabeledValues(doc, "Жанры").flatMap { it.split(',') }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { add(DisplayTag(DisplayTagGroups.GENRE, it)) }
                extractLabeledValues(doc, "Размер").forEach {
                    add(DisplayTag(DisplayTagGroups.SIZE, it))
                }
            }
        }

        return ParsedWorkMeta(
            source = WorkSource.FICBOOK,
            remoteId = remoteId,
            sourceUrl = sourceUrl,
            title = title,
            author = author,
            summary = summary,
            language = language,
            fandoms = fandoms,
            pairings = pairings,
            format = "epub",
            displayTags = displayTags,
        )
    }

    private fun extractLabeledValues(doc: org.jsoup.nodes.Document, label: String): List<String> {
        val bold = doc.select("b").firstOrNull { el ->
            el.text().trim().trimEnd(':').equals(label, ignoreCase = true)
        } ?: return emptyList()

        // Value is typically in the same parent after the <b>label</b>
        val parent = bold.parent() ?: return emptyList()
        val html = parent.html()
        val marker = Regex(
            """<b[^>]*>\s*${Regex.escape(label)}\s*:?\s*</b>""",
            RegexOption.IGNORE_CASE,
        ).find(html) ?: return emptyList()
        val after = html.substring(marker.range.last + 1)
        val nextBold = Regex("""<b\b""", RegexOption.IGNORE_CASE).find(after)
        val chunk = if (nextBold != null) after.substring(0, nextBold.range.first) else after
        val text = Jsoup.parseBodyFragment(chunk).text().trim()
        return if (text.isNotEmpty()) listOf(text) else emptyList()
    }

    private fun splitFandoms(raw: String): List<String> {
        return raw
            .replace(Regex("""\(кроссовер\)""", RegexOption.IGNORE_CASE), "")
            .split(',')
            .map { it.trim().trimEnd(',').trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun splitPairingField(raw: String): List<com.fandomreader.domain.model.Pairing> {
        return raw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it.contains('/') || it.contains('&') }
            .map { MetaNormalize.pairing(it) }
    }
}
