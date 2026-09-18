package com.fandomreader.source.api

import com.fandomreader.domain.model.Work
import java.io.File

data class ScanResult(
    val importedCount: Int = 0,
    val skippedCount: Int = 0,
    val failed: List<FailedImport> = emptyList(),
    val importedWorks: List<Work> = emptyList(),
) {
    data class FailedImport(
        val path: String,
        val message: String,
    )

    val summary: String
        get() = buildString {
            append("Импортировано: $importedCount")
            if (skippedCount > 0) {
                append(", уже в библиотеке: $skippedCount")
            }
            if (failed.isNotEmpty()) append(", ошибок: ${failed.size}")
        }
}

sealed class ImportFileResult {
    data class Imported(val work: Work) : ImportFileResult()
    data class Skipped(val work: Work) : ImportFileResult()
    data class Failed(val file: File, val message: String) : ImportFileResult()
}
