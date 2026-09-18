package com.fandomreader.feature.reader

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LoadChaptersFormatFallbackTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun epubWithoutFormatString_parsesChaptersNotRawZip() {
        val epub = tempFolder.newFile("opaque.bin")
        ZipOutputStream(epub.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(
                """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(
                """
                <?xml version="1.0"?>
                <package>
                  <metadata>
                    <dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">T</dc:title>
                  </metadata>
                  <manifest>
                    <item id="c1" href="chap1.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                  </spine>
                </package>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("OEBPS/chap1.xhtml"))
            zip.write(
                """
                <html><body><p>Hello chapter body.</p><p>Second paragraph.</p></body></html>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()
        }

        val chapters = loadChapters(epub, "unknown")
        assertThat(chapters).isNotEmpty()
        val text = chapters.joinToString("\n") { it.text }
        assertThat(text).doesNotContain("PK")
        assertThat(text).doesNotContain("mimetypeapplication/epub+zip")
        assertThat(text).contains("Hello chapter body")
    }

    @Test
    fun unsupportedBinary_isNotShownAsRawBytes() {
        val junk = tempFolder.newFile("noise.dat")
        junk.writeBytes(byteArrayOf(0x00, 0x01, 0x50, 0x4B, 0x03, 0x04, 0xFF.toByte()))
        val chapters = loadChapters(junk, "unknown")
        assertThat(chapters).hasSize(1)
        assertThat(chapters[0].text).contains("Неподдерживаемый формат")
        assertThat(chapters[0].text).doesNotContain(String(byteArrayOf(0x00, 0x01)))
    }
}
