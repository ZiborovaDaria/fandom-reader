package com.fandomreader.feature.importbook

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.fandomreader.source.api.BookFormatDetect
import java.io.File

/**
 * Copies a SAF document into [cacheDir] with a normalized `.epub`/`.fb2` name
 * derived from DISPLAY_NAME, MIME type, and content magic — not the opaque URI path.
 */
object SafImportCopy {
    fun copyToCache(resolver: ContentResolver, uri: Uri, cacheDir: File): File {
        val displayName = queryDisplayName(resolver, uri)
        val mimeType = runCatching { resolver.getType(uri) }.getOrNull()
        val header = resolver.openInputStream(uri)?.use { BookFormatDetect.readHeader(it) }
            ?: error("Cannot open $uri")
        val format = BookFormatDetect.detect(
            displayName = displayName,
            mimeType = mimeType,
            header = header,
            openZipProbe = null,
        ).let { detected ->
            if (detected != "unknown") {
                detected
            } else {
                // Copy first, then refine with ZipFile probe when header alone is inconclusive.
                "unknown"
            }
        }
        val provisionalName = BookFormatDetect.normalizedFileName(
            preferredName = displayName ?: uri.lastPathSegment?.substringAfterLast('/'),
            format = if (format == "unknown") "unknown" else format,
            fallbackBase = "import",
        )
        val tmp = File(cacheDir, "import-${System.currentTimeMillis()}-$provisionalName")
        resolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open $uri")
        val resolved = BookFormatDetect.detect(tmp, displayName = displayName, mimeType = mimeType)
        if (resolved == "unknown" || tmp.name.lowercase().endsWith(".$resolved")) {
            return tmp
        }
        val renamed = File(
            cacheDir,
            "import-${System.currentTimeMillis()}-" +
                BookFormatDetect.normalizedFileName(
                    preferredName = displayName ?: tmp.nameWithoutExtension,
                    format = resolved,
                ),
        )
        if (!tmp.renameTo(renamed)) {
            tmp.copyTo(renamed, overwrite = true)
            tmp.delete()
        }
        return renamed
    }

    fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        return runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx < 0) null else cursor.getString(idx)?.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }
}
