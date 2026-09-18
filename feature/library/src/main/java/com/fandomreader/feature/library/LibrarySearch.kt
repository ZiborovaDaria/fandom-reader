package com.fandomreader.feature.library

import com.fandomreader.domain.model.Work

/** Case-insensitive partial match on title (EN/RU). Empty query matches all. */
fun filterWorksByTitle(works: List<Work>, query: String): List<Work> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return works
    return works.filter { w ->
        listOfNotNull(w.title, w.titleRu).any { it.lowercase().contains(q) }
    }
}

/** Case-insensitive partial match on summary/description (EN/RU). Empty query matches all. */
fun filterWorksBySummary(works: List<Work>, query: String): List<Work> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return works
    return works.filter { w ->
        listOfNotNull(w.summary, w.summaryRu).any { it.lowercase().contains(q) }
    }
}

fun filterWorksByTitleAndSummary(
    works: List<Work>,
    titleQuery: String,
    summaryQuery: String,
): List<Work> =
    filterWorksBySummary(filterWorksByTitle(works, titleQuery), summaryQuery)
