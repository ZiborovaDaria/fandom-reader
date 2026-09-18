package com.fandomreader.feature.translation

import com.fandomreader.domain.model.TranslationStatus
import com.fandomreader.domain.repository.LibraryRepository
import java.io.File

/**
 * Chapter translation cache v3 stores paragraph-separated text (`\n\n`)
 * keyed to the story spine (front-matter excluded). Legacy v2 caches are ignored.
 */
class TranslationPipeline(
    private val provider: TranslationProvider,
    private val libraryRepository: LibraryRepository,
) {
    suspend fun translateWorkMetadata(
        workId: Long,
        worksDir: File,
        force: Boolean = false,
    ): Unit {
        val work = libraryRepository.getWork(workId) ?: return
        libraryRepository.updateTranslationFields(
            workId = workId,
            titleRu = if (force) null else work.titleRu,
            summaryRu = if (force) null else work.summaryRu,
            status = TranslationStatus.PENDING,
        )
        val fresh = libraryRepository.getWork(workId) ?: return
        val titleRu = fresh.titleRu ?: provider.translateBatch(listOf(fresh.title)).first()
        val summaryRu = when {
            fresh.summaryRu != null -> fresh.summaryRu
            fresh.summary.isNullOrBlank() -> null
            else -> provider.translateBatch(listOf(fresh.summary!!)).first()
        }
        worksDir.mkdirs()
        libraryRepository.updateTranslationFields(
            workId = workId,
            titleRu = titleRu,
            summaryRu = summaryRu,
            status = TranslationStatus.PARTIAL,
        )
        // Translate fandom/pairing display labels and persist via UPDATE (insert IGNORE would skip).
        val afterMeta = libraryRepository.getWork(workId) ?: return
        val fandomsNeedingRu = afterMeta.fandoms.filter { it.displayRu.isNullOrBlank() }
        if (fandomsNeedingRu.isNotEmpty()) {
            val translated = provider.translateBatch(fandomsNeedingRu.map { it.displayName })
            fandomsNeedingRu.forEachIndexed { idx, fandom ->
                libraryRepository.updateFandomDisplayRu(fandom.canonicalKey, translated[idx])
            }
        }
        val afterFandoms = libraryRepository.getWork(workId) ?: return
        val pairingsNeedingRu = afterFandoms.pairings.filter { it.displayRu.isNullOrBlank() }
        if (pairingsNeedingRu.isNotEmpty()) {
            val translated = provider.translateBatch(pairingsNeedingRu.map { it.displayName })
            pairingsNeedingRu.forEachIndexed { idx, pairing ->
                libraryRepository.updatePairingDisplayRu(pairing.canonicalKey, translated[idx])
            }
        }
        val afterPairings = libraryRepository.getWork(workId) ?: return
        val tagsNeedingRu = afterPairings.displayTags.filter { it.valueRu.isNullOrBlank() }
        if (tagsNeedingRu.isNotEmpty()) {
            val translated = provider.translateBatch(tagsNeedingRu.map { it.value })
            val updatedTags = afterPairings.displayTags.map { tag ->
                val idx = tagsNeedingRu.indexOfFirst {
                    it.group == tag.group && it.value == tag.value
                }
                if (idx >= 0) tag.copy(valueRu = translated[idx]) else tag
            }
            libraryRepository.upsertWork(afterPairings.copy(displayTags = updatedTags))
        }
        libraryRepository.coalesceTranslatedFandoms()
    }

    suspend fun translateChapter(
        workId: Long,
        chapterIndex: Int,
        original: String,
        worksDir: File,
        force: Boolean = false,
    ): String {
        val cacheDir = File(worksDir, "translations_v3/$workId").also { it.mkdirs() }
        val cacheFile = File(cacheDir, "chapter_$chapterIndex.txt")
        if (!force && cacheFile.exists() && cacheFile.length() > 0L) {
            return cacheFile.readText(Charsets.UTF_8)
        }
        val paragraphs = splitParagraphs(original)
        val maxChars = provider.preferredChunkChars()
        val translated = if (paragraphs.isEmpty()) {
            provider.translateBatch(listOf(original)).first()
        } else {
            translatePreservingMarkers(paragraphs, maxChars)
        }
        cacheFile.writeText(translated, Charsets.UTF_8)
        libraryRepository.updateTranslationFields(
            workId = workId,
            titleRu = libraryRepository.getWork(workId)?.titleRu,
            summaryRu = libraryRepository.getWork(workId)?.summaryRu,
            status = TranslationStatus.PARTIAL,
        )
        return translated
    }

    /**
     * Metadata then chapters in order; marks COMPLETE when all chapters succeed.
     * [force] clears metadata + chapter cache so the user can re-run a bad translation.
     */
    suspend fun translateWork(
        workId: Long,
        worksDir: File,
        chapterTexts: List<String>,
        force: Boolean = false,
    ) {
        if (force) {
            clearChapterCache(workId, worksDir)
        }
        translateWorkMetadata(workId, worksDir, force = force)
        chapterTexts.forEachIndexed { index, text ->
            translateChapter(workId, index, text, worksDir, force = force)
        }
        markComplete(workId)
    }

    fun clearChapterCache(workId: Long, worksDir: File) {
        val dir = File(worksDir, "translations_v3/$workId")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    suspend fun markComplete(workId: Long) {
        val work = libraryRepository.getWork(workId) ?: return
        libraryRepository.updateTranslationFields(
            workId = workId,
            titleRu = work.titleRu,
            summaryRu = work.summaryRu,
            status = TranslationStatus.COMPLETE,
        )
    }

    private suspend fun translatePreservingMarkers(
        paragraphs: List<String>,
        maxChars: Int,
    ): String {
        val out = mutableListOf<String>()
        val pending = mutableListOf<String>()
        suspend fun flushPending() {
            if (pending.isEmpty()) return
            chunkParagraphs(pending, maxChars = maxChars)
                .forEach { chunk ->
                    val joined = chunk.joinToString(PARAGRAPH_SEPARATOR)
                    val ru = provider.translateBatch(listOf(joined)).first()
                    val parts = splitParagraphs(ru)
                    out += parts.ifEmpty { listOf(ru.trim()) }
                }
            pending.clear()
        }
        for (p in paragraphs) {
            if (isSegmentMarker(p)) {
                flushPending()
                out += p
            } else {
                pending += p
            }
        }
        flushPending()
        return out.joinToString(PARAGRAPH_SEPARATOR)
    }

    companion object {
        const val PARAGRAPH_SEPARATOR = "\n\n"
        /** Must match `:feature:reader` ChapterText markers. */
        const val NOTE_SEGMENT_MARKER = "§§NOTE§§"
        const val BODY_SEGMENT_MARKER = "§§BODY§§"

        fun isSegmentMarker(paragraph: String): Boolean =
            paragraph == NOTE_SEGMENT_MARKER || paragraph == BODY_SEGMENT_MARKER

        fun splitParagraphs(text: String): List<String> =
            text.split(PARAGRAPH_SEPARATOR)
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        fun chunkParagraphs(paragraphs: List<String>, maxChars: Int): List<List<String>> {
            if (paragraphs.isEmpty()) return emptyList()
            val chunks = mutableListOf<List<String>>()
            var current = mutableListOf<String>()
            var size = 0
            for (p in paragraphs) {
                val extra = p.length + if (current.isEmpty()) 0 else PARAGRAPH_SEPARATOR.length
                if (current.isNotEmpty() && size + extra > maxChars) {
                    chunks += current
                    current = mutableListOf()
                    size = 0
                }
                // A single paragraph longer than maxChars stays whole (never mid-cut).
                current += p
                size += if (current.size == 1) p.length else extra
            }
            if (current.isNotEmpty()) chunks += current
            return chunks
        }
    }
}
