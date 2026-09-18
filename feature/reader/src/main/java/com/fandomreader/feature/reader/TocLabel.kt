package com.fandomreader.feature.reader

private val junkTitles = setOf(
    "preface",
    "summary",
    "title page",
    "contents",
    "table of contents",
    "оглавление",
    "содержание",
    "annotation",
    "аннотация",
    "notes",
)

/**
 * TOC / list label: meaningful chapter title, else «Глава N» (1-based).
 */
fun tocLabel(
    chapterTitle: String?,
    workTitle: String?,
    index1Based: Int,
): String {
    val title = chapterTitle?.trim().orEmpty()
    if (title.isEmpty()) return "Глава $index1Based"
    if (workTitle != null && title.equals(workTitle.trim(), ignoreCase = true)) {
        return "Глава $index1Based"
    }
    // AO3 often puts "Work Title - Author - Fandom" in <title>
    if (workTitle != null && title.startsWith(workTitle.trim(), ignoreCase = true) && title.contains(" - ")) {
        return "Глава $index1Based"
    }
    if (junkTitles.contains(title.lowercase())) {
        return "Глава $index1Based"
    }
    return title
}
