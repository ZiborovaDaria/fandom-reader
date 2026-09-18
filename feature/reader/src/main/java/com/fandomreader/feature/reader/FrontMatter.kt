package com.fandomreader.feature.reader

/**
 * Heuristics to exclude portal front-matter from the story chapter spine.
 * Prefer keeping documents with `id="chapters"` (AO3 chapter body).
 */
internal fun isEpubFrontMatter(entryName: String, html: String): Boolean {
    val lower = entryName.lowercase().substringAfterLast('/')
    if (lower.contains("nav") || lower.contains("toc")) return true
    if (lower.contains("title.xhtml") || lower == "title.html") return true
    if (lower.contains("preface") || lower.contains("split_000")) return true

    // AO3 Calibre often keeps id="preface" on the Summary card even when id="chapters"
    // is also present after a split — treat preface as front-matter first.
    if (html.contains("id=\"preface\"", ignoreCase = true) ||
        html.contains("id='preface'", ignoreCase = true)
    ) {
        return true
    }

    val hasChaptersId = html.contains("id=\"chapters\"", ignoreCase = true) ||
        html.contains("id='chapters'", ignoreCase = true)
    if (hasChaptersId) return false

    if (html.contains("dl class=\"tags\"", ignoreCase = true) ||
        html.contains("class=\"tags\"", ignoreCase = true) &&
        html.contains("Fandom", ignoreCase = true)
    ) {
        return true
    }
    if (html.contains("Фэндом") && html.contains("ficbook.net", ignoreCase = true)) {
        return true
    }
    return false
}

internal fun isFb2MetaSection(title: String?, bodyText: String): Boolean {
    val t = title?.trim().orEmpty()
    if (t.equals("Annotation", ignoreCase = true) ||
        t.equals("Аннотация", ignoreCase = true) ||
        t.equals("Title Page", ignoreCase = true) ||
        t.equals("Preface", ignoreCase = true)
    ) {
        return true
    }
    val hasPortalMeta =
        (bodyText.contains("Fandom", ignoreCase = true) &&
            bodyText.contains("Relationships", ignoreCase = true)) ||
            (bodyText.contains("Фэндом") &&
                (bodyText.contains("Отношения") || bodyText.contains("Пэйринг")))
    val looksShortMeta = bodyText.length < 2500 && hasPortalMeta
    return looksShortMeta
}

/**
 * Clamp saved progress to the story spine. Out-of-range index → 0.
 * If the saved chapter body still looks like front-matter, reset to first story chapter.
 */
fun resolveReadingChapterIndex(
    savedIndex: Int,
    chapterCount: Int,
    savedChapterLooksLikeFrontMatter: Boolean = false,
): Int {
    if (chapterCount <= 0) return 0
    if (savedIndex !in 0 until chapterCount) return 0
    if (savedChapterLooksLikeFrontMatter) return 0
    return savedIndex
}

fun chapterLooksLikeFrontMatter(text: String): Boolean {
    val sample = splitParagraphs(text)
        .filterNot { isSegmentMarker(it) }
        .joinToString(" ")
        .take(1200)
    return (sample.contains("Fandom", ignoreCase = true) &&
        sample.contains("Relationships", ignoreCase = true)) ||
        (sample.contains("Фэндом") && sample.contains("Пэйринг")) ||
        sample.contains("Posted originally on the Archive of Our Own", ignoreCase = true)
}
