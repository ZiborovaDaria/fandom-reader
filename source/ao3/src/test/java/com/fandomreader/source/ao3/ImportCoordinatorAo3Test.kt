package com.fandomreader.source.ao3

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

class ImportCoordinatorAo3Test {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun importsNextBestThingToFanfictionShelf() = runTest {
        val file = FixturePaths.resolve("ao3", "Next_Best_Thing.epub")
        val repo = FakeLibraryRepository()
        val coordinator = ImportCoordinator(
            classifier = DefaultBookClassifier(),
            parsers = listOf(Ao3MetaParser(), OtherMetaParser()),
            libraryRepository = repo,
            booksDir = tmp.newFolder("books"),
        )
        val work = coordinator.importFile(file)
        assertThat(work.source).isEqualTo(WorkSource.AO3)
        assertThat(work.shelf).isEqualTo(Shelf.FANFICTION)
        assertThat(work.title).isEqualTo("Next Best Thing")
        assertThat(work.fandoms).isNotEmpty()
        assertThat(work.pairings).isNotEmpty()
        assertThat(repo.getWork(work.id)).isNotNull()
    }
}
