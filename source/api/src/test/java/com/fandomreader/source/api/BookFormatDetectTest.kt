package com.fandomreader.source.api

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BookFormatDetectTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun detectsEpubByZipMimetype() {
        val epub = tempFolder.newFile("no-ext.bin")
        ZipOutputStream(epub.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(
                """
                <?xml version="1.0"?>
                <container><rootfiles><rootfile full-path="content.opf"/></rootfiles></container>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()
        }
        assertThat(BookFormatDetect.detect(epub)).isEqualTo("epub")
        assertThat(BookFormatDetect.detect(epub, displayName = "msf:123")).isEqualTo("epub")
    }

    @Test
    fun detectsFb2ByFictionBookXml() {
        val fb2 = tempFolder.newFile("raw.bin")
        fb2.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0">
              <description><title-info><book-title>T</book-title></title-info></description>
            </FictionBook>
            """.trimIndent(),
        )
        assertThat(BookFormatDetect.detect(fb2)).isEqualTo("fb2")
    }

    @Test
    fun garbageIsUnknown() {
        val junk = tempFolder.newFile("junk.dat")
        junk.writeBytes(byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte()))
        assertThat(BookFormatDetect.detect(junk)).isEqualTo("unknown")
    }

    @Test
    fun prefersDisplayNameExtension() {
        val junk = tempFolder.newFile("x.bin")
        junk.writeBytes(byteArrayOf(1, 2, 3))
        assertThat(BookFormatDetect.detect(junk, displayName = "book.epub")).isEqualTo("epub")
        assertThat(BookFormatDetect.detect(junk, displayName = "book.fb2")).isEqualTo("fb2")
    }

    @Test
    fun prefersMimeType() {
        val junk = tempFolder.newFile("y.bin")
        junk.writeBytes(byteArrayOf(1, 2, 3))
        assertThat(
            BookFormatDetect.detect(junk, mimeType = "application/epub+zip"),
        ).isEqualTo("epub")
        assertThat(
            BookFormatDetect.detect(junk, mimeType = "application/x-fictionbook+xml"),
        ).isEqualTo("fb2")
    }

    @Test
    fun normalizedFileNameAddsExtension() {
        assertThat(
            BookFormatDetect.normalizedFileName("msf:99", "epub"),
        ).isEqualTo("msf:99.epub")
        assertThat(
            BookFormatDetect.normalizedFileName("Story.EPUB", "epub"),
        ).isEqualTo("Story.epub")
        assertThat(
            BookFormatDetect.normalizedFileName(null, "fb2", fallbackBase = "import"),
        ).isEqualTo("import.fb2")
    }
}
