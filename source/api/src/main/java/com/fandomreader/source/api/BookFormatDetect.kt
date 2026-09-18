package com.fandomreader.source.api

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Detects EPUB / FB2 from filename, MIME type, and/or file content signatures.
 * Returns lowercase `"epub"`, `"fb2"`, or `"unknown"`.
 */
object BookFormatDetect {
    fun detect(
        file: File,
        displayName: String? = null,
        mimeType: String? = null,
    ): String {
        fromName(displayName)?.let { return it }
        fromName(file.name)?.let { return it }
        fromMime(mimeType)?.let { return it }
        return fromContent(file)
    }

    fun detect(
        displayName: String?,
        mimeType: String?,
        header: ByteArray,
        openZipProbe: (() -> Boolean)? = null,
    ): String {
        fromName(displayName)?.let { return it }
        fromMime(mimeType)?.let { return it }
        return fromHeader(header, openZipProbe)
    }

    fun extensionFor(format: String): String = when (format.lowercase()) {
        "epub" -> ".epub"
        "fb2" -> ".fb2"
        else -> ""
    }

    fun normalizedFileName(
        preferredName: String?,
        format: String,
        fallbackBase: String = "import",
    ): String {
        val base = preferredName
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.takeIf { it.isNotBlank() }
            ?: fallbackBase
        val withoutExt = base.substringBeforeLast('.', missingDelimiterValue = base)
            .ifBlank { fallbackBase }
        val ext = extensionFor(format)
        return if (ext.isEmpty()) {
            if (base.contains('.')) base else "$withoutExt.bin"
        } else {
            "$withoutExt$ext"
        }
    }

    private fun fromName(name: String?): String? {
        val lower = name?.lowercase() ?: return null
        return when {
            lower.endsWith(".epub") -> "epub"
            lower.endsWith(".fb2") -> "fb2"
            else -> null
        }
    }

    private fun fromMime(mimeType: String?): String? {
        val mime = mimeType?.lowercase()?.trim() ?: return null
        return when {
            mime == "application/epub+zip" || mime.contains("epub") -> "epub"
            mime.contains("fictionbook") || mime == "application/x-fictionbook+xml" -> "fb2"
            else -> null
        }
    }

    private fun fromContent(file: File): String {
        if (!file.exists() || !file.isFile || file.length() == 0L) return "unknown"
        val header = file.inputStream().use { readHeader(it, 512) }
        return fromHeader(header) {
            isEpubZip(file)
        }
    }

    private fun fromHeader(header: ByteArray, openZipProbe: (() -> Boolean)? = null): String {
        if (header.isEmpty()) return "unknown"
        val text = runCatching {
            header.toString(Charsets.UTF_8).lowercase()
        }.getOrDefault("")
        if ("fictionbook" in text || "<fictionbook" in text) return "fb2"
        if (looksLikeZip(header)) {
            if (openZipProbe?.invoke() == true) return "epub"
            // ZIP without probe: treat as possible epub only when mimetype string is in header
            if ("epub" in text || "meta-inf" in text) return "epub"
        }
        return "unknown"
    }

    private fun looksLikeZip(header: ByteArray): Boolean =
        header.size >= 2 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()

    private fun isEpubZip(file: File): Boolean {
        return try {
            ZipFile(file).use { zip ->
                val mimeEntry = zip.getEntry("mimetype")
                if (mimeEntry != null) {
                    val mime = zip.getInputStream(mimeEntry).bufferedReader().use { it.readText() }
                    if (mime.contains("epub", ignoreCase = true)) return@use true
                }
                zip.getEntry("META-INF/container.xml") != null
            }
        } catch (_: Exception) {
            false
        }
    }

    fun readHeader(stream: InputStream, max: Int = 512): ByteArray {
        val buf = ByteArray(max)
        var off = 0
        while (off < max) {
            val n = stream.read(buf, off, max - off)
            if (n <= 0) break
            off += n
        }
        return if (off == max) buf else buf.copyOf(off)
    }
}
