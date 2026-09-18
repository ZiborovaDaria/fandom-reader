package com.fandomreader.feature.library

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File

enum class ScanAccessState {
    /** Runtime permission needed (API ≤ 32). */
    NEED_PERMISSION,
    /** Typical folders are not readable; use SAF fallback. */
    FOLDERS_UNREADABLE,
    /** At least one typical folder can be scanned. */
    READY,
}

object TypicalBookFolders {
    fun roots(): List<File> {
        val names = listOf(
            Environment.DIRECTORY_DOWNLOADS,
            Environment.DIRECTORY_DOCUMENTS,
            "Books", // Environment.DIRECTORY_BOOKS (API 19+); string avoids stub gaps
        )
        return names.mapNotNull { name ->
            runCatching { Environment.getExternalStoragePublicDirectory(name) }.getOrNull()
        }.distinctBy { it.absolutePath }
    }

    fun readableRoots(): List<File> =
        roots().filter { it.exists() && it.isDirectory && it.canRead() }

    fun accessState(context: Context): ScanAccessState {
        if (Build.VERSION.SDK_INT <= 32) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return ScanAccessState.NEED_PERMISSION
        }
        return if (readableRoots().isEmpty()) {
            ScanAccessState.FOLDERS_UNREADABLE
        } else {
            ScanAccessState.READY
        }
    }

    fun requiredPermissionOrNull(): String? =
        if (Build.VERSION.SDK_INT <= 32) Manifest.permission.READ_EXTERNAL_STORAGE else null
}
