package com.fandomreader.source.api

import com.fandomreader.domain.model.WorkSource
import com.fandomreader.source.FixturePaths
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ImportWithoutExtensionTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun ao3EpubWithoutExtension_isNotOther() = runTest {
        val src = FixturePaths.resolve("ao3", "Next_Best_Thing.epub")
        val opaque = File(tempFolder.root, "content-provider-id")
        src.copyTo(opaque, overwrite = true)
        val booksDir = tempFolder.newFolder("books")
        val coordinator = ImportCoordinator(
            classifier = DefaultBookClassifier(),
            parsers = listOf(
                object : BookMetaParser {
                    override fun supports(kind: SourceKind) = kind == SourceKind.AO3
                    override fun parse(file: File) = ParsedWorkMeta(
                        source = WorkSource.AO3,
                        remoteId = "87947496",
                        sourceUrl = "https://archiveofourown.org/works/87947496",
                        title = "Next Best Thing",
                        author = "KojisApple",
                        summary = "s",
                        language = "en",
                        fandoms = emptyList(),
                        pairings = emptyList(),
                        format = "epub",
                    )
                },
                OtherMetaParser(),
            ),
            libraryRepository = FakeLibraryRepository(),
            booksDir = booksDir,
        )
        assertThat(DefaultBookClassifier().classify(opaque)).isEqualTo(SourceKind.AO3)
        val work = coordinator.importFile(opaque)
        assertThat(work.source).isNotEqualTo(WorkSource.OTHER)
        assertThat(work.source).isEqualTo(WorkSource.AO3)
        assertThat(work.format).isEqualTo("epub")
    }
}
