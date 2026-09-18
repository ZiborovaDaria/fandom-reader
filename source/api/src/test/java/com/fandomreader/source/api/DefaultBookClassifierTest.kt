package com.fandomreader.source.api

import com.fandomreader.source.FixturePaths
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DefaultBookClassifierTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun classifiesAo3EpubWithoutExtension() {
        val src = FixturePaths.resolve("ao3", "Next_Best_Thing.epub")
        val copy = File(tempFolder.root, "msf-opaque")
        src.copyTo(copy, overwrite = true)
        assertThat(BookFormatDetect.detect(copy)).isEqualTo("epub")
        assertThat(DefaultBookClassifier().classify(copy)).isEqualTo(SourceKind.AO3)
    }

    @Test
    fun classifiesFicbookEpubWithoutExtension() {
        val src = FixturePaths.resolve("ficbook", "Vlozit-dusu.epub")
        val copy = File(tempFolder.root, "document-456")
        src.copyTo(copy, overwrite = true)
        assertThat(DefaultBookClassifier().classify(copy)).isEqualTo(SourceKind.FICBOOK)
    }
}
