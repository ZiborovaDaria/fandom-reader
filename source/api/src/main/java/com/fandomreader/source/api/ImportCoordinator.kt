package com.fandomreader.source.api

import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.domain.model.preservingTranslationFrom
import com.fandomreader.domain.repository.LibraryRepository
import java.io.File

class ImportCoordinator(
    private val classifier: BookClassifier,
    private val parsers: List<BookMetaParser>,
    private val libraryRepository: LibraryRepository,
    private val booksDir: File,
) {
    suspend fun importFile(file: File): Work {
        return when (val result = importFileDetailed(file)) {
            is ImportFileResult.Imported -> result.work
            is ImportFileResult.Skipped -> result.work
            is ImportFileResult.Failed -> error(result.message)
        }
    }

    suspend fun importFileDetailed(file: File): ImportFileResult {
        return try {
            val fingerprint = DeviceLibraryScanner.sourceFingerprint(file)
            libraryRepository.findWorkIdBySourceFingerprint(fingerprint)?.let { existingId ->
                val existing = libraryRepository.getWork(existingId)
                    ?: return@let null
                return ImportFileResult.Skipped(existing)
            }

            val kind = classifier.classify(file)
            val parser = parsers.firstOrNull { it.supports(kind) }
                ?: error("No parser for $kind")
            val meta = parser.parse(file)

            if (meta.remoteId != null) {
                libraryRepository.findWorkIdBySourceRemote(meta.source, meta.remoteId)?.let { existingId ->
                    booksDir.mkdirs()
                    val dest = File(booksDir, "${System.currentTimeMillis()}_${file.name}")
                    file.copyTo(dest, overwrite = true)
                    val incoming = buildWork(meta, dest, fingerprint).copy(id = existingId)
                    val existing = libraryRepository.getWork(existingId)
                    val updated = if (existing != null) {
                        incoming.preservingTranslationFrom(existing)
                    } else {
                        incoming
                    }
                    libraryRepository.upsertWork(updated)
                    val work = libraryRepository.getWork(existingId) ?: updated
                    return ImportFileResult.Skipped(work)
                }
            }

            booksDir.mkdirs()
            val dest = File(booksDir, "${System.currentTimeMillis()}_${file.name}")
            file.copyTo(dest, overwrite = true)
            val work = buildWork(meta, dest, fingerprint)
            val id = libraryRepository.upsertWork(work)
            ImportFileResult.Imported(work.copy(id = id))
        } catch (e: Exception) {
            ImportFileResult.Failed(file, e.message ?: e.toString())
        }
    }

    suspend fun importFiles(
        files: List<File>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): ScanResult {
        var importedCount = 0
        var skippedCount = 0
        val failed = mutableListOf<ScanResult.FailedImport>()
        val importedWorks = mutableListOf<Work>()
        val total = files.size
        files.forEachIndexed { index, file ->
            when (val result = importFileDetailed(file)) {
                is ImportFileResult.Imported -> {
                    importedCount++
                    importedWorks += result.work
                }
                is ImportFileResult.Skipped -> skippedCount++
                is ImportFileResult.Failed -> failed += ScanResult.FailedImport(
                    path = result.file.absolutePath,
                    message = result.message,
                )
            }
            onProgress(index + 1, total)
        }
        return ScanResult(
            importedCount = importedCount,
            skippedCount = skippedCount,
            failed = failed,
            importedWorks = importedWorks,
        )
    }

    private fun buildWork(
        meta: ParsedWorkMeta,
        dest: File,
        fingerprint: String,
    ): Work {
        val shelf = when (meta.source) {
            WorkSource.OTHER -> Shelf.OTHER
            else -> if (meta.fandoms.isNotEmpty() || meta.pairings.isNotEmpty()) {
                Shelf.FANFICTION
            } else {
                Shelf.OTHER
            }
        }
        return Work(
            source = meta.source,
            remoteId = meta.remoteId,
            sourceUrl = meta.sourceUrl,
            title = meta.title,
            author = meta.author,
            summary = meta.summary,
            language = meta.language,
            localPath = dest.absolutePath,
            format = meta.format,
            shelf = shelf,
            fandoms = meta.fandoms,
            pairings = meta.pairings,
            displayTags = meta.displayTags,
            sourceFingerprint = fingerprint,
            fileSizeBytes = dest.length(),
        )
    }
}
