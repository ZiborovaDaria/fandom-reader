package com.fandomreader.source.api

import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FakeLibraryRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun upsertWork_assignsIdAndDedupsBySourceRemoteId() = runTest {
        val repo = FakeLibraryRepository()
        val work = sampleWork(remoteId = "87947496", title = "Next Best Thing")
        val id1 = repo.upsertWork(work)
        val id2 = repo.upsertWork(work.copy(title = "Next Best Thing (updated)"))

        assertThat(id1).isEqualTo(id2)
        assertThat(repo.allWorks()).hasSize(1)
        assertThat(repo.getWork(id1)?.title).isEqualTo("Next Best Thing (updated)")
    }

    @Test
    fun observeWorks_filtersByShelf() = runTest {
        val repo = FakeLibraryRepository()
        repo.upsertWork(sampleWork(remoteId = "1", title = "Fan", shelf = Shelf.FANFICTION))
        repo.upsertWork(
            sampleWork(
                remoteId = null,
                title = "Other Book",
                source = WorkSource.OTHER,
                shelf = Shelf.OTHER,
            ),
        )

        assertThat(repo.observeWorks(Shelf.FANFICTION).first()).hasSize(1)
        assertThat(repo.observeWorks(Shelf.OTHER).first().single().title).isEqualTo("Other Book")
    }

    @Test
    fun observeWorksByFandomAndPairing_filtersByCanonicalKeys() = runTest {
        val repo = FakeLibraryRepository()
        repo.upsertWork(sampleWork(remoteId = "1", title = "Match"))
        repo.upsertWork(
            sampleWork(remoteId = "2", title = "Other pairing").copy(
                pairings = listOf(
                    Pairing(
                        canonicalKey = "harry-potter/draco-malfoy",
                        displayName = "Harry Potter/Draco Malfoy",
                        type = RelationshipType.ROMANTIC,
                    ),
                ),
            ),
        )

        val filtered = repo.observeWorksByFandomAndPairing(
            "harry-potter",
            "harry-potter/tom-riddle",
        ).first()
        assertThat(filtered.map { it.title }).containsExactly("Match")
    }

    @Test
    fun importCoordinator_persistsParsedMetaViaFakeRepo() = runTest {
        val booksDir = tempFolder.newFolder("books")
        val sourceFile = File(booksDir, "sample.txt").apply {
            writeText("Archive of Our Own https://archiveofourown.org/works/1")
        }
        val repo = FakeLibraryRepository()
        val coordinator = ImportCoordinator(
            classifier = object : BookClassifier {
                override fun classify(file: File) = SourceKind.OTHER
            },
            parsers = listOf(
                object : BookMetaParser {
                    override fun supports(kind: SourceKind) = kind == SourceKind.OTHER
                    override fun parse(file: File) = ParsedWorkMeta(
                        source = WorkSource.OTHER,
                        remoteId = null,
                        sourceUrl = null,
                        title = "Sample",
                        author = "Author",
                        summary = "Summary",
                        language = "en",
                        fandoms = emptyList(),
                        pairings = emptyList(),
                        format = "txt",
                    )
                },
            ),
            libraryRepository = repo,
            booksDir = booksDir,
        )

        val imported = coordinator.importFile(sourceFile)
        assertThat(imported.id).isGreaterThan(0)
        assertThat(imported.shelf).isEqualTo(Shelf.OTHER)
        assertThat(repo.getWork(imported.id)?.title).isEqualTo("Sample")
    }

    private fun sampleWork(
        remoteId: String?,
        title: String,
        source: WorkSource = WorkSource.AO3,
        shelf: Shelf = Shelf.FANFICTION,
    ): Work = Work(
        source = source,
        remoteId = remoteId,
        sourceUrl = remoteId?.let { "https://archiveofourown.org/works/$it" },
        title = title,
        author = "Author",
        summary = "Summary",
        language = "en",
        localPath = "/tmp/$title",
        format = "epub",
        shelf = shelf,
        fandoms = listOf(Fandom(canonicalKey = "harry-potter", displayName = "Harry Potter")),
        pairings = listOf(
            Pairing(
                canonicalKey = "harry-potter/tom-riddle",
                displayName = "Harry Potter/Tom Riddle",
                type = RelationshipType.ROMANTIC,
            ),
        ),
    )
}
