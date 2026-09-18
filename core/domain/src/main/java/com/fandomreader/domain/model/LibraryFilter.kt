package com.fandomreader.domain.model

/**
 * Combined library filter: AND across categories, OR within a category.
 * Fandom/pairing identity is [canonicalKey]; display tags use original (group, value).
 */
data class LibraryFilterCriteria(
    val fandomKeys: Set<String> = emptySet(),
    val pairingKeys: Set<String> = emptySet(),
    val displayTags: Map<String, Set<String>> = emptyMap(),
) {
    fun isEmpty(): Boolean =
        fandomKeys.isEmpty() && pairingKeys.isEmpty() && displayTags.values.all { it.isEmpty() }

    fun encode(): String {
        val parts = mutableListOf<String>()
        if (fandomKeys.isNotEmpty()) {
            parts += "F:" + fandomKeys.joinToString(UNIT)
        }
        if (pairingKeys.isNotEmpty()) {
            parts += "P:" + pairingKeys.joinToString(UNIT)
        }
        displayTags.forEach { (group, values) ->
            if (values.isNotEmpty()) {
                parts += "T:$group:" + values.joinToString(UNIT)
            }
        }
        return parts.joinToString(RECORD)
    }

    companion object {
        const val RECORD = "\u001e"
        const val UNIT = "\u001f"

        fun decode(raw: String): LibraryFilterCriteria {
            if (raw.isBlank()) return LibraryFilterCriteria()
            val fandoms = mutableSetOf<String>()
            val pairings = mutableSetOf<String>()
            val tags = mutableMapOf<String, Set<String>>()
            raw.split(RECORD).forEach { part ->
                when {
                    part.startsWith("F:") ->
                        fandoms += part.removePrefix("F:").split(UNIT).filter { it.isNotEmpty() }
                    part.startsWith("P:") ->
                        pairings += part.removePrefix("P:").split(UNIT).filter { it.isNotEmpty() }
                    part.startsWith("T:") -> {
                        val rest = part.removePrefix("T:")
                        val colon = rest.indexOf(':')
                        if (colon <= 0) return@forEach
                        val group = rest.substring(0, colon)
                        val values = rest.substring(colon + 1).split(UNIT).filter { it.isNotEmpty() }
                        if (values.isNotEmpty()) tags[group] = values.toSet()
                    }
                }
            }
            return LibraryFilterCriteria(fandoms, pairings, tags)
        }
    }
}

fun workMatchesFilter(work: Work, criteria: LibraryFilterCriteria): Boolean {
    if (criteria.isEmpty()) return true
    if (criteria.fandomKeys.isNotEmpty()) {
        val keys = work.fandoms.map { it.canonicalKey }.toSet()
        if (criteria.fandomKeys.none { it in keys }) return false
    }
    if (criteria.pairingKeys.isNotEmpty()) {
        val keys = work.pairings.map { it.canonicalKey }.toSet()
        if (criteria.pairingKeys.none { it in keys }) return false
    }
    criteria.displayTags.forEach { (group, selected) ->
        if (selected.isEmpty()) return@forEach
        val values = work.displayTags
            .filter { it.group.equals(group, ignoreCase = true) }
            .map { it.value }
        if (selected.none { want -> values.any { it.equals(want, ignoreCase = true) } }) {
            return false
        }
    }
    return true
}

fun filterWorksByCriteria(works: List<Work>, criteria: LibraryFilterCriteria): List<Work> =
    works.filter { workMatchesFilter(it, criteria) }.sortedBy { it.title.lowercase() }

/** Case-insensitive substring match against any of the labels. Empty query matches all. */
fun matchesPartialQuery(query: String, vararg labels: String?): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return labels.any { it != null && it.lowercase().contains(q) }
}

fun Work.preservingTranslationFrom(existing: Work): Work {
    return copy(
        titleRu = titleRu?.takeIf { it.isNotBlank() } ?: existing.titleRu,
        summaryRu = summaryRu?.takeIf { it.isNotBlank() } ?: existing.summaryRu,
        translationStatus = if (
            translationStatus == TranslationStatus.NONE &&
            existing.translationStatus != TranslationStatus.NONE
        ) {
            existing.translationStatus
        } else {
            translationStatus
        },
        fandoms = fandoms.map { f ->
            val old = existing.fandoms.find { it.canonicalKey == f.canonicalKey }
            if (f.displayRu.isNullOrBlank()) f.copy(displayRu = old?.displayRu) else f
        },
        pairings = pairings.map { p ->
            val old = existing.pairings.find { it.canonicalKey == p.canonicalKey }
            if (p.displayRu.isNullOrBlank()) p.copy(displayRu = old?.displayRu) else p
        },
        displayTags = displayTags.map { t ->
            val old = existing.displayTags.find {
                it.group.equals(t.group, ignoreCase = true) &&
                    it.value.equals(t.value, ignoreCase = true)
            }
            if (t.valueRu.isNullOrBlank()) t.copy(valueRu = old?.valueRu) else t
        },
        readingProgressChapterIndex = existing.readingProgressChapterIndex,
        readingProgressOffset = existing.readingProgressOffset,
        lastOpenedAt = existing.lastOpenedAt,
        fileSizeBytes = if (fileSizeBytes > 0L) fileSizeBytes else existing.fileSizeBytes,
    )
}
