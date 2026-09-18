package com.fandomreader.feature.library

import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LibrarySearchTest {
    private val works = listOf(
        Work(
            source = WorkSource.AO3,
            remoteId = "1",
            sourceUrl = null,
            title = "Next Best Thing",
            titleRu = "Следующая лучшая вещь",
            summary = "Thomas Potter summary",
            localPath = "x",
            format = "epub",
            shelf = Shelf.FANFICTION,
        ),
        Work(
            source = WorkSource.FICBOOK,
            remoteId = "2",
            sourceUrl = null,
            title = "Поттер, который совсем не Поттер",
            summary = "Другое описание",
            localPath = "y",
            format = "epub",
            shelf = Shelf.FANFICTION,
        ),
    )

    @Test
    fun titleSearchDoesNotMatchSummaryOnly() {
        assertThat(filterWorksByTitle(works, "thomas").map { it.remoteId }).isEmpty()
        assertThat(filterWorksByTitle(works, "best").map { it.remoteId }).containsExactly("1")
        assertThat(filterWorksByTitle(works, "поттер").map { it.remoteId }).containsExactly("2")
    }

    @Test
    fun summarySearchDoesNotMatchTitleOnly() {
        assertThat(filterWorksBySummary(works, "best").map { it.remoteId }).isEmpty()
        assertThat(filterWorksBySummary(works, "thomas").map { it.remoteId }).containsExactly("1")
        assertThat(filterWorksBySummary(works, "описание").map { it.remoteId }).containsExactly("2")
    }

    @Test
    fun combinedAndWhenBothFilled() {
        assertThat(
            filterWorksByTitleAndSummary(works, "best", "thomas").map { it.remoteId },
        ).containsExactly("1")
        assertThat(
            filterWorksByTitleAndSummary(works, "поттер", "thomas").map { it.remoteId },
        ).isEmpty()
    }

    @Test
    fun emptyQueriesReturnAll() {
        assertThat(filterWorksByTitleAndSummary(works, "  ", "")).hasSize(2)
    }
}
