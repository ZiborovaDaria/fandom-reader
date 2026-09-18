package com.fandomreader.feature.library

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.fandomreader.source.api.BookFormatDetect
import com.fandomreader.source.api.DeviceLibraryScanner
import java.io.File

/**
 * Scans a user-selected SAF document tree for EPUB/FB2 and copies readable files into cache.
 */
class DocumentTreeBookScanner(
    private val context: Context,
    private val maxDepth: Int = 4,
) {
    fun findBookFiles(treeUri: Uri, cacheDir: File): List<File> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val found = linkedMapOf<String, File>()
        walk(root, depth = 0, cacheDir = cacheDir, out = found)
        return found.values.sortedBy { it.name.lowercase() }
    }

    private fun walk(
        dir: DocumentFile,
        depth: Int,
        cacheDir: File,
        out: MutableMap<String, File>,
    ) {
        if (depth > maxDepth) return
        for (child in dir.listFiles()) {
            when {
                child.isDirectory -> walk(child, depth + 1, cacheDir, out)
                child.isFile && isBookDocument(child) -> {
                    val copied = copyToCache(child, cacheDir) ?: continue
                    out.putIfAbsent(DeviceLibraryScanner.sourceFingerprint(copied), copied)
                }
            }
        }
    }

    private fun isBookDocument(doc: DocumentFile): Boolean {
        val name = doc.name?.lowercase().orEmpty()
        if (name.endsWith(".epub") || name.endsWith(".fb2")) return true
        val mime = doc.type?.lowercase().orEmpty()
        return mime == "application/epub+zip" || mime.contains("fictionbook")
    }

    private fun copyToCache(doc: DocumentFile, cacheDir: File): File? {
        val uri = doc.uri
        val displayName = doc.name
        val mime = doc.type
        return try {
            val header = context.contentResolver.openInputStream(uri)
                ?.use { BookFormatDetect.readHeader(it) }
                ?: return null
            var format = BookFormatDetect.detect(
                displayName = displayName,
                mimeType = mime,
                header = header,
            )
            val name = BookFormatDetect.normalizedFileName(
                preferredName = displayName,
                format = if (format == "unknown") "unknown" else format,
                fallbackBase = "treescan",
            )
            val dest = File(cacheDir, "treescan-${System.currentTimeMillis()}-$name")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            format = BookFormatDetect.detect(dest, displayName = displayName, mimeType = mime)
            if (format == "unknown" && !DeviceLibraryScanner.isBookFile(dest)) {
                dest.delete()
                return null
            }
            if (format != "unknown" && !dest.name.lowercase().endsWith(".$format")) {
                val renamed = File(
                    cacheDir,
                    "treescan-${System.currentTimeMillis()}-" +
                        BookFormatDetect.normalizedFileName(displayName, format, "treescan"),
                )
                if (dest.renameTo(renamed)) return renamed
                dest.copyTo(renamed, overwrite = true)
                dest.delete()
                return renamed
            }
            dest
        } catch (_: Exception) {
            null
        }
    }
}
