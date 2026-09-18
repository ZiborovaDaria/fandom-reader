package com.fandomreader.source.api

import com.fandomreader.domain.model.WorkSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImportCoordinatorBatchTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun batchContinuesAfterOneFailure() = runTest {
        val booksDir = tempFolder.newFolder("books")
        val good = tempFolder.newFile("good.epub").apply { writeText("ok") }
        val bad = tempFolder.newFile("bad.epub").apply { writeText("bad") }
        val repo = FakeLibraryRepository()
        val coordinator = ImportCoordinator(
            classifier = object : BookClassifier {
                override fun classify(file: File) = SourceKind.OTHER
            },
            parsers = listOf(
                object : BookMetaParser {
                    override fun supports(kind: SourceKind) = kind == SourceKind.OTHER
                    override fun parse(file: File): ParsedWorkMeta {
                        if (file.name.startsWith("bad")) error("parse failed")
                        return ParsedWorkMeta(
                            source = WorkSource.OTHER,
                            remoteId = null,
                            sourceUrl = null,
                            title = "Good",
                            author = null,
                            summary = null,
                            language = "en",
                            fandoms = emptyList(),
                            pairings = emptyList(),
                            format = "epub",
                        )
                    }
                },
            ),
            libraryRepository = repo,
            booksDir = booksDir,
        )

        val result = coordinator.importFiles(listOf(bad, good))
        assertThat(result.importedCount).isEqualTo(1)
        assertThat(result.failed).hasSize(1)
        assertThat(repo.allWorks()).hasSize(1)
    }

    @Test
    fun secondScanSkipsSameFingerprint() = runTest {
        val booksDir = tempFolder.newFolder("books")
        val file = tempFolder.newFile("same.epub").apply { writeText("content") }
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
                        title = "Same",
                        author = null,
                        summary = null,
                        language = "en",
                        fandoms = emptyList(),
                        pairings = emptyList(),
                        format = "epub",
                    )
                },
            ),
            libraryRepository = repo,
            booksDir = booksDir,
        )

        val first = coordinator.importFiles(listOf(file))
        val second = coordinator.importFiles(listOf(file))

        assertThat(first.importedCount).isEqualTo(1)
        assertThat(second.importedCount).isEqualTo(0)
        assertThat(second.skippedCount).isEqualTo(1)
        assertThat(repo.allWorks()).hasSize(1)
    }

    @Test
    fun secondScanSkipsSameRemoteIdentity() = runTest {
        val booksDir = tempFolder.newFolder("books")
        val file1 = tempFolder.newFile("a.epub").apply { writeText("a") }
        val file2 = tempFolder.newFile("b.epub").apply { writeText("b") }
        val repo = FakeLibraryRepository()
        val coordinator = ImportCoordinator(
            classifier = object : BookClassifier {
                override fun classify(file: File) = SourceKind.AO3
            },
            parsers = listOf(
                object : BookMetaParser {
                    override fun supports(kind: SourceKind) = kind == SourceKind.AO3
                    override fun parse(file: File) = ParsedWorkMeta(
                        source = WorkSource.AO3,
                        remoteId = "42",
                        sourceUrl = "https://archiveofourown.org/works/42",
                        title = file.name,
                        author = null,
                        summary = null,
                        language = "en",
                        fandoms = emptyList(),
                        pairings = emptyList(),
                        format = "epub",
                    )
                },
            ),
            libraryRepository = repo,
            booksDir = booksDir,
        )

        assertThat(coordinator.importFiles(listOf(file1)).importedCount).isEqualTo(1)
        val second = coordinator.importFiles(listOf(file2))
        assertThat(second.skippedCount).isEqualTo(1)
        assertThat(repo.allWorks()).hasSize(1)
    }

    @Test
    fun remoteIdRematchPreservesTranslatedMetadata() = runTest {
        val booksDir = tempFolder.newFolder("books-ru")
        val file2 = tempFolder.newFile("second.epub").apply { writeText("b") }
        val repo = FakeLibraryRepository()
        val id = repo.upsertWork(
            com.fandomreader.domain.model.Work(
                source = WorkSource.AO3,
                remoteId = "42",
                sourceUrl = "https://archiveofourown.org/works/42",
                title = "Original EN",
                titleRu = "Русский заголовок",
                summary = "EN summary",
                summaryRu = "Русское описание",
                translationStatus = com.fandomreader.domain.model.TranslationStatus.COMPLETE,
                language = "en",
                localPath = "/tmp/old.epub",
                format = "epub",
                shelf = com.fandomreader.domain.model.Shelf.FANFICTION,
                fandoms = listOf(
                    com.fandomreader.domain.model.Fandom(
                        canonicalKey = "hp",
                        displayName = "Harry Potter",
                        displayRu = "Гарри Поттер",
                    ),
                ),
                displayTags = listOf(
                    com.fandomreader.domain.model.DisplayTag("rating", "Mature", "Для взрослых"),
                ),
                sourceFingerprint = "other-fingerprint",
            ),
        )
        assertThat(id).isGreaterThan(0)

        val coordinator = ImportCoordinator(
            classifier = object : BookClassifier {
                override fun classify(file: File) = SourceKind.AO3
            },
            parsers = listOf(
                object : BookMetaParser {
                    override fun supports(kind: SourceKind) = kind == SourceKind.AO3
                    override fun parse(file: File) = ParsedWorkMeta(
                        source = WorkSource.AO3,
                        remoteId = "42",
                        sourceUrl = "https://archiveofourown.org/works/42",
                        title = file.name,
                        author = null,
                        summary = "Fresh English summary",
                        language = "en",
                        fandoms = listOf(
                            com.fandomreader.domain.model.Fandom(
                                canonicalKey = "hp",
                                displayName = "Harry Potter",
                            ),
                        ),
                        pairings = emptyList(),
                        displayTags = listOf(
                            com.fandomreader.domain.model.DisplayTag("rating", "Mature"),
                        ),
                        format = "epub",
                    )
                },
            ),
            libraryRepository = repo,
            booksDir = booksDir,
        )

        val result = coordinator.importFileDetailed(file2)
        assertThat(result).isInstanceOf(ImportFileResult.Skipped::class.java)
        val work = repo.getWork(id)!!
        assertThat(work.titleRu).isEqualTo("Русский заголовок")
        assertThat(work.summaryRu).isEqualTo("Русское описание")
        assertThat(work.translationStatus)
            .isEqualTo(com.fandomreader.domain.model.TranslationStatus.COMPLETE)
        assertThat(work.fandoms.single().displayRu).isEqualTo("Гарри Поттер")
        assertThat(work.displayTags.single().valueRu).isEqualTo("Для взрослых")
    }
}
