package com.fandomreader.feature.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

/** Joins paragraphs with a blank line so UI and translation can split on `\n\n`. */
const val PARAGRAPH_SEPARATOR = "\n\n"

/** Stable markers for note/body regions (must survive translation as whole paragraphs). */
const val NOTE_SEGMENT_MARKER = "§§NOTE§§"
const val BODY_SEGMENT_MARKER = "§§BODY§§"

enum class ChapterSegmentRole {
    Body,
    Note,
}

data class ChapterSegment(
    val role: ChapterSegmentRole,
    val paragraphs: List<String>,
)

fun splitParagraphs(text: String): List<String> =
    text.split(PARAGRAPH_SEPARATOR)
        .map { it.trim() }
        .filter { it.isNotEmpty() }

fun isSegmentMarker(paragraph: String): Boolean =
    paragraph == NOTE_SEGMENT_MARKER || paragraph == BODY_SEGMENT_MARKER

/**
 * Extract readable chapter body from HTML/XHTML/FB2-ish markup while keeping
 * paragraph boundaries and note/body segment markers for styling + translation.
 */
fun extractChapterBody(html: String): String =
    serializeChapterSegments(extractChapterSegments(html))

fun extractChapterSegments(html: String): List<ChapterSegment> {
    val doc = Jsoup.parse(html)
    val body: Element = doc.body() ?: return emptyList()
    body.select("script, style, nav, header, footer").remove()

    val afterword = body.selectFirst("#afterword, [id=afterword]")
    if (afterword != null) {
        val hasStory = body.select(".userstuff2 p").isNotEmpty()
        if (!hasStory) {
            val paras = paragraphTexts(afterword)
            if (paras.isNotEmpty()) {
                return listOf(ChapterSegment(ChapterSegmentRole.Note, paras))
            }
        }
    }

    val pNodes = body.select("p")
    if (pNodes.size >= 2) {
        val flat = pNodes.map { it.text().trim() }.filter { it.isNotEmpty() }
        // Prefer plain heuristics so RU/EN headings work even when Calibre classes vary
        val fromPlain = parseChapterSegmentsFromPlain(flat.joinToString(PARAGRAPH_SEPARATOR))
        if (fromPlain.any { it.role == ChapterSegmentRole.Note }) return fromPlain

        val ordered = mutableListOf<Pair<ChapterSegmentRole, String>>()
        fun emit(role: ChapterSegmentRole, text: String) {
            val t = text.trim()
            if (t.isNotEmpty()) ordered += role to t
        }
        fun roleFor(el: Element): ChapterSegmentRole {
            if (el.closest("[id^=endnotes], #afterword, .endnote-link") != null) {
                return ChapterSegmentRole.Note
            }
            if (el.tagName().equals("blockquote", ignoreCase = true)) {
                return ChapterSegmentRole.Note
            }
            val t = el.text().trim()
            if (isNoteHeading(t) || isNotePrefix(t) || isEndNoteTeaser(t)) {
                return ChapterSegmentRole.Note
            }
            return ChapterSegmentRole.Body
        }
        pNodes.forEach { p -> emit(roleFor(p), p.text()) }
        body.select("blockquote").forEach { bq ->
            if (bq.select("p").isEmpty()) emit(ChapterSegmentRole.Note, bq.text())
        }
        return coalescePairs(ordered).ifEmpty {
            listOf(ChapterSegment(ChapterSegmentRole.Body, flat))
        }
    }

    val fromBreaks = paragraphsFromBr(body)
    if (fromBreaks.isNotEmpty()) {
        val fromBr = parseChapterSegmentsFromPlain(fromBreaks.joinToString(PARAGRAPH_SEPARATOR))
        if (!isOnlySceneBreaks(fromBr)) return fromBr
    }

    val blocks = collectBlockTexts(body)
    if (blocks.isNotEmpty()) {
        val fromBlocks = parseChapterSegmentsFromPlain(blocks.joinToString(PARAGRAPH_SEPARATOR))
        if (!isOnlySceneBreaks(fromBlocks)) return fromBlocks
    }
    val single = when {
        pNodes.size == 1 -> pNodes.first()!!.text().trim()
        blocks.size == 1 -> blocks.first()
        else -> body.wholeText().replace('\u00a0', ' ').trim()
    }
    if (single.isEmpty()) return emptyList()
    return parseChapterSegmentsFromPlain(single)
}

private fun isOnlySceneBreaks(segments: List<ChapterSegment>): Boolean {
    val paras = segments.flatMap { it.paragraphs }.map { it.trim() }.filter { it.isNotEmpty() }
    if (paras.isEmpty()) return true
    return paras.all { isSceneBreakLine(it) }
}

private fun isSceneBreakLine(text: String): Boolean {
    val t = text.trim()
    if (t.isEmpty()) return false
    return t.matches(Regex("""^(\*{3,}|\*{1}\s+\*{1}\s+\*{1}|[—–-]{3,}|_{3,})$"""))
}

fun serializeChapterSegments(segments: List<ChapterSegment>): String {
    if (segments.isEmpty()) return ""
    return segments.joinToString(PARAGRAPH_SEPARATOR) { seg ->
        val marker = when (seg.role) {
            ChapterSegmentRole.Note -> NOTE_SEGMENT_MARKER
            ChapterSegmentRole.Body -> BODY_SEGMENT_MARKER
        }
        (listOf(marker) + seg.paragraphs).joinToString(PARAGRAPH_SEPARATOR)
    }
}

/** Parse display segments from chapter text (markers and/or heading heuristics). */
fun parseChapterSegments(text: String): List<ChapterSegment> {
    if (text.isBlank()) return emptyList()
    if (splitParagraphs(text).any { isSegmentMarker(it) }) {
        return parseChapterSegmentsFromMarkers(text)
    }
    return parseChapterSegmentsFromPlain(text)
}

internal fun parseChapterSegmentsFromMarkers(text: String): List<ChapterSegment> {
    val paras = splitParagraphs(text)
    val out = mutableListOf<ChapterSegment>()
    var role: ChapterSegmentRole? = null
    val buf = mutableListOf<String>()
    fun flush() {
        if (role != null && buf.isNotEmpty()) {
            out += ChapterSegment(role!!, buf.toList())
        }
        buf.clear()
    }
    for (p in paras) {
        when (p) {
            NOTE_SEGMENT_MARKER -> {
                flush()
                role = ChapterSegmentRole.Note
            }
            BODY_SEGMENT_MARKER -> {
                flush()
                role = ChapterSegmentRole.Body
            }
            else -> {
                if (role == null) role = ChapterSegmentRole.Body
                buf += p
            }
        }
    }
    flush()
    return out.ifEmpty {
        listOf(ChapterSegment(ChapterSegmentRole.Body, paras.filterNot { isSegmentMarker(it) }))
    }
}

internal fun parseChapterSegmentsFromPlain(text: String): List<ChapterSegment> {
    val paras = splitParagraphs(text)
    if (paras.isEmpty()) return emptyList()

    if (paras.firstOrNull()?.let { isAfterwordHeading(it) } == true && paras.size <= 8) {
        return listOf(ChapterSegment(ChapterSegmentRole.Note, paras))
    }

    val roles = Array(paras.size) { ChapterSegmentRole.Body }

    var i = 0
    while (i < paras.size &&
        (isNoteHeading(paras[i]) || isNotePrefix(paras[i]) || isEndNoteTeaser(paras[i]))
    ) {
        roles[i] = ChapterSegmentRole.Note
        i++
    }
    // Author note paragraph immediately after a note heading (e.g. «Примечания автора: …»)
    if (i < paras.size &&
        i > 0 &&
        roles[i - 1] == ChapterSegmentRole.Note &&
        isNoteHeading(paras[i - 1])
    ) {
        if (isNotePrefix(paras[i]) || !looksLikeStoryProse(paras[i])) {
            roles[i] = ChapterSegmentRole.Note
            i++
        }
    }

    val endStart = paras.indices.firstOrNull { idx ->
        idx >= i && (isEndNoteHeading(paras[idx]) || isAfterwordHeading(paras[idx]))
    }
    if (endStart != null) {
        for (j in endStart until paras.size) roles[j] = ChapterSegmentRole.Note
    }

    return coalesceRoles(paras, roles)
}

internal fun isNoteHeading(text: String): Boolean {
    val t = text.trim()
    return NOTE_HEADING_REGEX.matches(t) || isAfterwordHeading(t) || isEndNoteHeading(t)
}

internal fun isEndNoteHeading(text: String): Boolean =
    END_NOTE_HEADING_REGEX.matches(text.trim())

internal fun isAfterwordHeading(text: String): Boolean =
    AFTERWORD_HEADING_REGEX.matches(text.trim())

internal fun isNotePrefix(text: String): Boolean =
    NOTE_PREFIX_REGEX.containsMatchIn(text.trim())

internal fun isEndNoteTeaser(text: String): Boolean =
    END_NOTE_TEASER_REGEX.containsMatchIn(text.trim())

private fun looksLikeStoryProse(text: String): Boolean {
    val t = text.trim()
    if (isNoteHeading(t) || isNotePrefix(t) || isEndNoteTeaser(t)) return false
    return t.length >= 60
}

private fun coalescePairs(ordered: List<Pair<ChapterSegmentRole, String>>): List<ChapterSegment> {
    if (ordered.isEmpty()) return emptyList()
    val refined = parseChapterSegmentsFromPlain(
        ordered.joinToString(PARAGRAPH_SEPARATOR) { it.second },
    )
    if (refined.any { it.role == ChapterSegmentRole.Note }) return refined

    val out = mutableListOf<ChapterSegment>()
    var role = ordered.first().first
    val buf = mutableListOf<String>()
    for ((r, t) in ordered) {
        if (r != role && buf.isNotEmpty()) {
            out += ChapterSegment(role, buf.toList())
            buf.clear()
        }
        role = r
        buf += t
    }
    if (buf.isNotEmpty()) out += ChapterSegment(role, buf.toList())
    return out
}

private fun coalesceRoles(paras: List<String>, roles: Array<ChapterSegmentRole>): List<ChapterSegment> {
    val out = mutableListOf<ChapterSegment>()
    var role = roles[0]
    val buf = mutableListOf<String>()
    for (i in paras.indices) {
        if (roles[i] != role && buf.isNotEmpty()) {
            out += ChapterSegment(role, buf.toList())
            buf.clear()
        }
        role = roles[i]
        buf += paras[i]
    }
    if (buf.isNotEmpty()) out += ChapterSegment(role, buf.toList())
    return out
}

private fun paragraphTexts(root: Element): List<String> {
    val ps = root.select("p").map { it.text().trim() }.filter { it.isNotEmpty() }
    if (ps.isNotEmpty()) return ps
    val t = root.text().trim()
    return if (t.isEmpty()) emptyList() else listOf(t)
}

private fun paragraphsFromBr(root: Element): List<String> {
    val html = root.html()
    if (!html.contains("<br", ignoreCase = true)) return emptyList()
    val normalized = html
        .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</div\s*>""", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("""</p\s*>""", RegexOption.IGNORE_CASE), "\n")
    // wholeText keeps the newlines we injected; Element.text() would collapse them.
    val plain = Jsoup.parseBodyFragment(normalized).body().wholeText()
        .replace('\u00a0', ' ')
        .replace(Regex("""[ \t]+\n"""), "\n")
        .replace(Regex("""\n[ \t]+"""), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
    return plain.split(Regex("""\n\s*\n|\n"""))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        // Keep scene breaks (`***`) and short words; only drop empty/whitespace.
        .filter { it.length > 1 || it == "*" }
}

private fun collectBlockTexts(root: Element): List<String> {
    val out = mutableListOf<String>()
    fun walk(node: Node) {
        if (node is Element) {
            val tag = node.tagName().lowercase()
            if (tag in setOf("p", "div", "section", "article", "blockquote", "li")) {
                val nestedBlocks = node.children().count {
                    it.tagName().lowercase() in setOf("p", "div", "section", "article", "blockquote")
                }
                if (nestedBlocks == 0) {
                    val t = node.text().trim()
                    if (t.isNotEmpty()) out += t
                    return
                }
            }
            node.childNodes().forEach(::walk)
        }
    }
    root.childNodes().forEach(::walk)
    return out.distinct()
}

private val NOTE_HEADING_REGEX = Regex(
    """^(Chapter\s+Notes|Notes|Примечания(\s+к\s+главе)?|Примечания\s*:?)\s*$""",
    RegexOption.IGNORE_CASE,
)

private val END_NOTE_HEADING_REGEX = Regex(
    """^(Chapter\s+End\s+Notes|End\s+Notes|Примечания\s+в\s+конце(\s+главы)?)\s*$""",
    RegexOption.IGNORE_CASE,
)

private val AFTERWORD_HEADING_REGEX = Regex(
    """^(Afterword|Послесловие)\s*$""",
    RegexOption.IGNORE_CASE,
)

private val NOTE_PREFIX_REGEX = Regex(
    """^(Author'?s\s+Notes?|Notes\s*:|Примечания\s+автора|Примечания\s*:)""",
    RegexOption.IGNORE_CASE,
)

private val END_NOTE_TEASER_REGEX = Regex(
    """See the end of the chapter|См\.\s*в\s*конце|примечания\s*см\.\s*в\s*конце|Дополнительные\s+примечания""",
    RegexOption.IGNORE_CASE,
)
