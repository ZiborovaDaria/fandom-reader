package com.fandomreader.domain.model

import java.text.Collator
import java.util.Locale

enum class WorkSort {
    LastOpened,
    Title,
    Size,
}

enum class SortDirection {
    Ascending,
    Descending,
}

fun Work.displayTitle(): String = titleRu?.takeIf { it.isNotBlank() } ?: title

private val titleCollator: Collator =
    Collator.getInstance(Locale("ru", "RU")).apply {
        strength = Collator.PRIMARY
    }

fun sortWorks(
    works: List<Work>,
    sort: WorkSort,
    direction: SortDirection,
): List<Work> {
    return when (sort) {
        WorkSort.Title -> {
            val ascending = works.sortedWith { a, b ->
                titleCollator.compare(a.displayTitle(), b.displayTitle())
            }
            if (direction == SortDirection.Ascending) ascending else ascending.asReversed()
        }
        WorkSort.Size -> {
            val known = works.filter { it.fileSizeBytes > 0L }.sortedBy { it.fileSizeBytes }
            val unknown = works.filter { it.fileSizeBytes <= 0L }.sortedBy { it.id }
            val ascending = known + unknown
            if (direction == SortDirection.Ascending) ascending else known.asReversed() + unknown
        }
        WorkSort.LastOpened -> {
            val opened = works.filter { it.lastOpenedAt > 0L }.sortedBy { it.lastOpenedAt }
            val never = works.filter { it.lastOpenedAt <= 0L }.sortedBy { it.id }
            if (direction == SortDirection.Ascending) {
                opened + never
            } else {
                opened.asReversed() + never
            }
        }
    }
}
