package com.fandomreader.source.api

import java.io.File

/**
 * Finds EPUB/FB2 under provided roots with a bounded recursion depth.
 * Missing or unreadable roots are skipped (not errors).
 */
class DeviceLibraryScanner(
    private val maxDepth: Int = 3,
) {
    fun findBookFiles(roots: List<File>): List<File> {
        val found = linkedMapOf<String, File>()
        for (root in roots) {
            if (!root.exists() || !root.isDirectory) continue
            walk(root, depth = 0, out = found)
        }
        return found.values.sortedBy { it.name.lowercase() }
    }

    private fun walk(dir: File, depth: Int, out: MutableMap<String, File>) {
        if (depth > maxDepth) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            when {
                child.isFile && isBookFile(child) -> out.putIfAbsent(child.absolutePath, child)
                child.isDirectory -> walk(child, depth + 1, out)
            }
        }
    }

    companion object {
        fun isBookFile(file: File): Boolean {
            val name = file.name.lowercase()
            return name.endsWith(".epub") || name.endsWith(".fb2")
        }

        fun sourceFingerprint(file: File): String {
            val path = runCatching { file.canonicalFile.absolutePath }
                .getOrElse { file.absolutePath }
            return "$path|${file.length()}"
        }

        /** Union of discovery sources keyed by fingerprint (path|size). */
        fun mergeBookFiles(vararg lists: List<File>): List<File> {
            val found = linkedMapOf<String, File>()
            for (list in lists) {
                for (file in list) {
                    found.putIfAbsent(sourceFingerprint(file), file)
                }
            }
            return found.values.sortedBy { it.name.lowercase() }
        }
    }
}
