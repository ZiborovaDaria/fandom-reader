package com.fandomreader.source.ao3

import com.fandomreader.domain.model.WorkSource
import com.fandomreader.source.api.BookMetaParser
import com.fandomreader.source.api.MetaNormalize
import com.fandomreader.source.api.ParsedWorkMeta
import com.fandomreader.source.api.SourceKind
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File

/**
 * Best-effort recovery for AO3-origin FB2 files whose title-info was destroyed
 * by external translation tools (Yandex/Calibre).
 */
class Ao3Fb2Parser : BookMetaParser {
    private val workUrlRegex = Regex(
        """https?://(?:www\.)?archiveofourown\.org/works/(\d+)""",
        RegexOption.IGNORE_CASE,
    )

    override fun supports(kind: SourceKind): Boolean = kind == SourceKind.AO3

    override fun parse(file: File): ParsedWorkMeta {
        return try {
            parseInternal(file)
        } catch (_: Exception) {
            fallback(file)
        }
    }

    private fun parseInternal(file: File): ParsedWorkMeta {
        val raw = file.readText(Charsets.UTF_8)
        val hasAo3Markers = hasAo3Markers(raw)
        val doc = runCatching { Jsoup.parse(raw, "", Parser.xmlParser()) }.getOrNull()
        val bodyText = doc?.selectFirst("body")?.text().orEmpty().ifBlank {
            // Damaged FB2 may still be readable as plain text
            raw
        }

        val fandoms = extractAfterLabel(bodyText, "Фэндом", listOf("Отношения", "Персонажи", "Дополнительные"))
            ?.split(Regex("[,;]"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map(MetaNormalize::fandom)
            .orEmpty()

        val relationships = extractAfterLabel(
            bodyText,
            "Отношения",
            listOf("Персонажи", "Дополнительные", "Краткоесодержание", "Примечания"),
        )
        val pairings = relationships
            ?.split(Regex("[,;]"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.map { MetaNormalize.pairing(it) }
            .orEmpty()
            .ifEmpty {
                // Some damaged bodies lose the "Отношения:" label but keep "A/B Персонажи:"
                Regex("""([^.\n]{3,80}?/[^\s][^.\n]{2,80}?)\s+Персонажи:""")
                    .find(bodyText)
                    ?.groupValues
                    ?.get(1)
                    ?.let { listOf(MetaNormalize.pairing(it.trim())) }
                    .orEmpty()
            }

        val summary = extractAfterLabel(
            bodyText,
            "Краткоесодержание",
            listOf("Примечания", "Глава", "Текст главы"),
        )?.trim()?.ifBlank { null }

        val titleInfoTitle = doc?.selectFirst("title-info book-title, description title-info book-title")
            ?.text()
            ?.trim()
        val title = titleInfoTitle
            ?.takeUnless { it.isBlank() || it.equals("Unknown", ignoreCase = true) }
            ?: guessTitleFromBody(bodyText)
            ?: file.nameWithoutExtension

        val authorFromTitleInfo = doc?.selectFirst("title-info author last-name")?.text()?.trim()
        val author = authorFromTitleInfo
            ?.takeUnless { it.contains("Yandex", ignoreCase = true) || it.contains("Translate", ignoreCase = true) }

        val sourceUrl = workUrlRegex.find(raw)?.let {
            "https://archiveofourown.org/works/${it.groupValues[1]}"
        }
        val remoteId = sourceUrl?.let { workUrlRegex.find(it)?.groupValues?.get(1) }

        val source = if (hasAo3Markers) WorkSource.AO3 else WorkSource.OTHER

        return ParsedWorkMeta(
            source = source,
            remoteId = remoteId,
            sourceUrl = sourceUrl,
            title = title,
            author = author,
            summary = summary,
            language = doc?.selectFirst("title-info lang")?.text()?.trim(),
            fandoms = fandoms,
            pairings = pairings,
            format = "fb2",
        )
    }

    private fun hasAo3Markers(raw: String): Boolean {
        val lower = raw.lowercase()
        return "archiveofourown.org" in lower ||
            "фэндом:" in lower ||
            "отношения:" in lower ||
            "краткоесодержание" in lower
    }

    private fun extractAfterLabel(text: String, label: String, stopLabels: List<String>): String? {
        val start = Regex("""$label\s*:""", RegexOption.IGNORE_CASE).find(text) ?: return null
        val from = text.substring(start.range.last + 1)
        val stopRegex = stopLabels.joinToString("|") { Regex.escape(it) + "\\s*:" }
        val stop = Regex(stopRegex, RegexOption.IGNORE_CASE).find(from)
        val value = if (stop != null) from.substring(0, stop.range.first) else from.take(2_000)
        return value.trim().ifBlank { null }
    }

    private fun guessTitleFromBody(bodyText: String): String? {
        // Often author nick sits just before "Краткоесодержание"
        val beforeSummary = Regex(
            """([A-Za-zА-Яа-яЁё0-9 _\-']{2,80})\s+Краткоесодержание\s*:""",
            RegexOption.IGNORE_CASE,
        ).find(bodyText)?.groupValues?.get(1)?.trim()
        // Prefer a title-like token before author nick if present (last words before summary)
        return beforeSummary
            ?.split(Regex("\\s+"))
            ?.takeLast(6)
            ?.joinToString(" ")
            ?.takeIf { it.isNotBlank() && !it.equals("Unknown", true) }
    }

    private fun fallback(file: File): ParsedWorkMeta =
        ParsedWorkMeta(
            source = WorkSource.AO3,
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
