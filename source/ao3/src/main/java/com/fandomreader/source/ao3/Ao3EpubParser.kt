package com.fandomreader.source.ao3

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

class Ao3EpubParser : BookMetaParser {
    private val workUrlRegex = Regex(
        """https?://(?:www\.)?archiveofourown\.org/works/(\d+)""",
        RegexOption.IGNORE_CASE,
    )

    override fun supports(kind: SourceKind): Boolean = kind == SourceKind.AO3

    override fun parse(file: File): ParsedWorkMeta {
        require(file.name.lowercase().endsWith(".epub")) {
            "Ao3EpubParser expects .epub, got ${file.name}"
        }
        val opf = EpubIo.readOpf(file)
        val title = EpubIo.dcText(opf, "title") ?: file.nameWithoutExtension
        val author = EpubIo.dcText(opf, "creator")
        val language = EpubIo.dcText(opf, "language")
        val publisher = EpubIo.dcText(opf, "publisher")
        val dcDescription = MetaNormalize.stripHtml(
            EpubIo.dcText(opf, "description")?.let(MetaNormalize::decodeXmlEntities),
        )

        val preface = findPrefetchXhtml(file)
        val summaryPage = findSummaryXhtml(file)
        val prefaceDoc = preface?.let { Jsoup.parse(it.second) }
        val summaryDoc = summaryPage?.let { Jsoup.parse(it.second) }

        val fandoms = prefaceDoc?.let { extractLabeledLinks(it, "Fandom") }
            ?.map(MetaNormalize::fandom)
            .orEmpty()
        val pairings = prefaceDoc?.let { extractLabeledLinks(it, "Relationships") }
            ?.map { MetaNormalize.pairing(it) }
            .orEmpty()

        val displayTags = buildList {
            prefaceDoc?.let { doc ->
                addAll(toDisplayTags(DisplayTagGroups.RATING, extractLabeledLinks(doc, "Rating")))
                addAll(toDisplayTags(DisplayTagGroups.WARNINGS, extractLabeledLinks(doc, "Archive Warning")))
                addAll(toDisplayTags(DisplayTagGroups.CATEGORY, extractLabeledLinks(doc, "Category")))
                addAll(toDisplayTags(DisplayTagGroups.CHARACTERS, extractLabeledLinks(doc, "Character")))
                addAll(toDisplayTags(DisplayTagGroups.ADDITIONAL, extractLabeledLinks(doc, "Additional Tag")))
            }
        }

        val sourceUrl = findWorkUrl(preface?.second, summaryPage?.second, publisher)
        val remoteId = sourceUrl?.let { workUrlRegex.find(it)?.groupValues?.get(1) }

        val summary = extractSummary(summaryDoc) ?: dcDescription

        return ParsedWorkMeta(
            source = WorkSource.AO3,
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

    private fun toDisplayTags(group: String, values: List<String>): List<DisplayTag> =
        values.map { DisplayTag(group, it) }

    private fun findPrefetchXhtml(file: File): Pair<String, String>? {
        EpubIo.findPrefixedEntry(file, "split_000")?.let { return it }
        EpubIo.findPrefixedEntry(file, "preface")?.let { return it }
        return EpubIo.findFirstEntry(file) { name ->
            val lower = name.lowercase()
            lower.endsWith(".xhtml") || lower.endsWith(".html")
        }?.takeIf { (_, text) ->
            text.contains("Relationships", ignoreCase = true) &&
                text.contains("Fandom", ignoreCase = true)
        }
    }

    private fun findSummaryXhtml(file: File): Pair<String, String>? {
        EpubIo.findPrefixedEntry(file, "split_001")?.let { return it }
        return EpubIo.findFirstEntry(file) { name ->
            val lower = name.lowercase()
            (lower.endsWith(".xhtml") || lower.endsWith(".html")) &&
                !lower.contains("split_000")
        }?.takeIf { (_, text) ->
            text.contains("Summary", ignoreCase = true)
        }
    }

    private fun extractLabeledLinks(doc: org.jsoup.nodes.Document, label: String): List<String> {
        val dts = doc.select("dt")
        for (dt in dts) {
            val dtText = dt.text().trim().trimEnd(':')
            if (!labelMatches(dtText, label)) {
                continue
            }
            val dd = dt.nextElementSibling() ?: continue
            val links = dd.select("a").map { it.text().trim() }.filter { it.isNotEmpty() }
            if (links.isNotEmpty()) return links
            val plain = dd.text().split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (plain.isNotEmpty()) return plain
        }
        return emptyList()
    }

    private fun labelMatches(dtText: String, label: String): Boolean {
        if (dtText.equals(label, ignoreCase = true)) return true
        if (dtText.equals("${label}s", ignoreCase = true)) return true
        // Category → Categories
        if (label.endsWith("y", ignoreCase = true)) {
            val ies = label.dropLast(1) + "ies"
            if (dtText.equals(ies, ignoreCase = true)) return true
        }
        return false
    }

    private fun extractSummary(doc: org.jsoup.nodes.Document?): String? {
        if (doc == null) return null
        val blockquote = doc.selectFirst("blockquote.userstuff")
        if (blockquote != null) {
            return blockquote.text().trim().ifBlank { null }
        }
        val paragraphs = doc.select("p")
        val summaryHeader = paragraphs.firstOrNull { it.text().trim().equals("Summary", ignoreCase = true) }
        if (summaryHeader != null) {
            val collected = mutableListOf<String>()
            var sibling = summaryHeader.nextElementSibling()
            while (sibling != null) {
                val text = sibling.text().trim()
                if (text.equals("Notes", ignoreCase = true)) break
                if (text.isNotEmpty()) collected += text
                sibling = sibling.nextElementSibling()
            }
            return collected.joinToString("\n").ifBlank { null }
        }
        return null
    }

    private fun findWorkUrl(vararg blobs: String?): String? {
        for (blob in blobs) {
            if (blob.isNullOrBlank()) continue
            val match = workUrlRegex.find(blob)
            if (match != null) {
                return "https://archiveofourown.org/works/${match.groupValues[1]}"
            }
        }
        return null
    }
}
