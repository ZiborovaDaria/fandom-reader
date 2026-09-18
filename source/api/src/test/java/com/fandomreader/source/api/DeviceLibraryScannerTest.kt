package com.fandomreader.source.api

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DeviceLibraryScannerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun findsEpubAndFb2_ignoresOther_skipsMissingRoot() {
        val downloads = tempFolder.newFolder("Download")
        val documents = tempFolder.newFolder("Documents")
        File(downloads, "a.epub").writeText("epub")
        File(downloads, "notes.txt").writeText("nope")
        File(documents, "b.fb2").writeText("fb2")
        val nested = File(documents, "Books/AO3").also { it.mkdirs() }
        File(nested, "c.epub").writeText("nested")

        val missing = File(tempFolder.root, "Books-missing")
        val found = DeviceLibraryScanner(maxDepth = 3).findBookFiles(
            listOf(downloads, documents, missing),
        )

        assertThat(found.map { it.name }).containsExactly("a.epub", "b.fb2", "c.epub")
        assertThat(found.none { it.name.endsWith(".txt") }).isTrue()
    }

    @Test
    fun respectsMaxDepth() {
        val root = tempFolder.newFolder("Download")
        val deep = File(root, "d1/d2/d3/d4").also { it.mkdirs() }
        File(File(root, "d1"), "shallow.epub").writeText("ok")
        File(deep, "too-deep.epub").writeText("skip")

        val found = DeviceLibraryScanner(maxDepth = 3).findBookFiles(listOf(root))
        assertThat(found.map { it.name }).containsExactly("shallow.epub")
    }

    @Test
    fun mergeBookFiles_dedupesByFingerprint() {
        val a = tempFolder.newFile("a.epub").apply { writeText("same") }
        val b = File(tempFolder.root, "copy.epub").apply {
            // Different path, different size → both kept
            writeText("other-content")
        }
        val samePathAgain = File(a.absolutePath)
        val merged = DeviceLibraryScanner.mergeBookFiles(
            listOf(a, b),
            listOf(samePathAgain),
        )
        assertThat(merged).hasSize(2)
        assertThat(merged.map { it.name }).containsExactly("a.epub", "copy.epub")
    }
}
