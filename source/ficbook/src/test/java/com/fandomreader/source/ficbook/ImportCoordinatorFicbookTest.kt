package com.fandomreader.source.ficbook

import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.source.api.DefaultBookClassifier
import com.fandomreader.source.api.FakeLibraryRepository
import com.fandomreader.source.api.ImportCoordinator
import com.fandomreader.source.api.OtherMetaParser
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ImportCoordinatorFicbookTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun importsFicbookEpubToFanfictionShelf() = runTest {
        val file = FixturePaths.resolve("ficbook", "Potter-kotoryj-sovsem-ne-Potter-1.epub")
        val repo = FakeLibraryRepository()
        val coordinator = ImportCoordinator(
            classifier = DefaultBookClassifier(),
            parsers = listOf(FicbookEpubParser(), OtherMetaParser()),
            libraryRepository = repo,
            booksDir = tmp.newFolder("books"),
        )
        val work = coordinator.importFile(file)
        assertThat(work.source).isEqualTo(WorkSource.FICBOOK)
        assertThat(work.shelf).isEqualTo(Shelf.FANFICTION)
        assertThat(work.title).isNotEmpty()
        assertThat(work.fandoms).isNotEmpty()
        assertThat(repo.getWork(work.id)).isNotNull()
    }
}
