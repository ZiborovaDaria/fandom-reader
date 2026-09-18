package com.fandomreader.feature.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.fandomreader.source.api.BookFormatDetect
import com.fandomreader.source.api.DeviceLibraryScanner
import java.io.File

/**
 * Discovers EPUB/FB2 via MediaStore when direct File listing of public folders is empty
 * under scoped storage (typical on API 33+).
 */
class MediaStoreBookDiscovery(
    private val context: Context,
) {
    data class Result(
        val files: List<File>,
        val querySucceeded: Boolean,
    )

    fun findBookFiles(cacheDir: File = context.cacheDir): Result {
        return try {
            val collected = linkedMapOf<String, File>()
            queryCollection(
                MediaStore.Files.getContentUri("external"),
                cacheDir,
                collected,
                requireTypicalLocation = true,
            )
            if (Build.VERSION.SDK_INT >= 29) {
                queryCollection(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    cacheDir,
                    collected,
                    requireTypicalLocation = false,
                )
            }
            Result(
                files = collected.values.sortedBy { it.name.lowercase() },
                querySucceeded = true,
            )
        } catch (_: SecurityException) {
            Result(emptyList(), querySucceeded = false)
        } catch (_: Exception) {
            Result(emptyList(), querySucceeded = false)
        }
    }

    private fun queryCollection(
        collection: Uri,
        cacheDir: File,
        out: MutableMap<String, File>,
        requireTypicalLocation: Boolean,
    ) {
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
        )
        if (Build.VERSION.SDK_INT >= 29) {
            projection += MediaStore.MediaColumns.RELATIVE_PATH
        }
        @Suppress("DEPRECATION")
        projection += MediaStore.MediaColumns.DATA

        val selection = (
            "(LOWER(${MediaStore.MediaColumns.DISPLAY_NAME}) LIKE ? OR " +
                "LOWER(${MediaStore.MediaColumns.DISPLAY_NAME}) LIKE ? OR " +
                "${MediaStore.MediaColumns.MIME_TYPE} = ? OR " +
                "LOWER(${MediaStore.MediaColumns.MIME_TYPE}) LIKE ?)"
            )
        val args = arrayOf(
            "%.epub",
            "%.fb2",
            "application/epub+zip",
            "%fictionbook%",
        )

        context.contentResolver.query(
            collection,
            projection.toTypedArray(),
            selection,
            args,
            null,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            @Suppress("DEPRECATION")
            val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val relCol = if (Build.VERSION.SDK_INT >= 29) {
                cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            } else {
                -1
            }

            while (cursor.moveToNext()) {
                val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else -1L
                val relative = if (relCol >= 0) cursor.getString(relCol) else null
                if (requireTypicalLocation &&
                    !isTypicalLocation(relative, dataCol >= 0, cursor, dataCol)
                ) {
                    continue
                }

                val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                val fromPath = dataPath?.let { File(it) }?.takeIf {
                    it.isFile && it.canRead() && DeviceLibraryScanner.isBookFile(it)
                }
                if (fromPath != null) {
                    out.putIfAbsent(DeviceLibraryScanner.sourceFingerprint(fromPath), fromPath)
                    continue
                }

                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val copied = copyToCache(contentUri, displayName, mime, size, cacheDir) ?: continue
                out.putIfAbsent(DeviceLibraryScanner.sourceFingerprint(copied), copied)
            }
        }
    }

    private fun isTypicalLocation(
        relativePath: String?,
        hasData: Boolean,
        cursor: android.database.Cursor,
        dataCol: Int,
    ): Boolean {
        val rel = relativePath?.replace('\\', '/')?.lowercase().orEmpty()
        if (rel.isNotEmpty()) {
            return rel.contains("download") ||
                rel.contains("document") ||
                rel.contains("books")
        }
        if (hasData) {
            val data = cursor.getString(dataCol)?.replace('\\', '/')?.lowercase().orEmpty()
            if (data.isNotEmpty()) {
                return data.contains("/download") ||
                    data.contains("/document") ||
                    data.contains("/books")
            }
        }
        // No path metadata: still accept (Downloads collection already scoped).
        return true
    }

    private fun copyToCache(
        uri: Uri,
        displayName: String?,
        mime: String?,
        size: Long,
        cacheDir: File,
    ): File? {
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
                fallbackBase = "scan",
            )
            val dest = File(cacheDir, "mediascan-${System.currentTimeMillis()}-$name")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            format = BookFormatDetect.detect(dest, displayName = displayName, mimeType = mime)
            if (format == "unknown" && !DeviceLibraryScanner.isBookFile(dest)) {
                dest.delete()
                return null
            }
            if (size > 0 && dest.length() == 0L) {
                dest.delete()
                return null
            }
            // Ensure extension for downstream classifiers.
            if (format != "unknown" && !dest.name.lowercase().endsWith(".$format")) {
                val renamed = File(
                    cacheDir,
                    "mediascan-${System.currentTimeMillis()}-" +
                        BookFormatDetect.normalizedFileName(displayName, format, "scan"),
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

    companion object {
        /** Public Download/Documents/Books roots used for messaging. */
        fun typicalFolderLabels(): String = "Downloads/Documents/Books"

        fun downloadDirName(): String =
            Environment.DIRECTORY_DOWNLOADS // "Download" on Android
    }
}
