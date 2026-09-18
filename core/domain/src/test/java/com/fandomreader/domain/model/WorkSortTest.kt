package com.fandomreader.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkSortTest {
    private fun work(
        id: Long,
        title: String,
        titleRu: String? = null,
        lastOpenedAt: Long = 0L,
        fileSizeBytes: Long = 0L,
    ) = Work(
        id = id,
        source = WorkSource.OTHER,
        remoteId = null,
        sourceUrl = null,
        title = title,
        titleRu = titleRu,
        localPath = "/w/$id",
        format = "EPUB",
        shelf = Shelf.FANFICTION,
        lastOpenedAt = lastOpenedAt,
        fileSizeBytes = fileSizeBytes,
    )

    @Test
    fun titleSortUsesDisplayTitleAndRussianCollator() {
        val works = listOf(
            work(1, "Zebra"),
            work(2, "Apple", titleRu = "Яблоко"),
            work(3, "Banana", titleRu = "Ананас"),
            work(4, "Moratorium"),
        )
        val sorted = sortWorks(works, WorkSort.Title, SortDirection.Ascending)
        val collator = java.text.Collator.getInstance(java.util.Locale("ru", "RU")).apply {
            strength = java.text.Collator.PRIMARY
        }
        assertEquals(
            works.map { it.displayTitle() }.sortedWith(collator),
            sorted.map { it.displayTitle() },
        )
        val desc = sortWorks(works, WorkSort.Title, SortDirection.Descending)
        assertEquals(sorted.map { it.id }.asReversed(), desc.map { it.id })
    }

    @Test
    fun lastOpenedDescendingPutsRecentFirstAndNeverOpenedLast() {
        val works = listOf(
            work(1, "A", lastOpenedAt = 100),
            work(2, "B", lastOpenedAt = 0),
            work(3, "C", lastOpenedAt = 300),
            work(4, "D", lastOpenedAt = 200),
        )
        val sorted = sortWorks(works, WorkSort.LastOpened, SortDirection.Descending)
        assertEquals(listOf(3L, 4L, 1L, 2L), sorted.map { it.id })
    }

    @Test
    fun sizeAscendingPutsUnknownLast() {
        val works = listOf(
            work(1, "A", fileSizeBytes = 500),
            work(2, "B", fileSizeBytes = 0),
            work(3, "C", fileSizeBytes = 100),
        )
        val sorted = sortWorks(works, WorkSort.Size, SortDirection.Ascending)
        assertEquals(listOf(3L, 1L, 2L), sorted.map { it.id })
    }
}
