package com.fandomreader.feature.reader

import java.io.File
import java.util.LinkedHashMap

data class ChapterParseCacheKey(
    val workId: Long,
    val path: String,
    val length: Long,
    val lastModified: Long,
)

data class CachedChapters(
    val key: ChapterParseCacheKey,
    val chapters: List<ReaderChapter>,
)

/**
 * Process-scoped LRU of parsed chapter bodies keyed by file identity.
 * Translation overlays are applied by the caller after a cache hit.
 */
object ChapterParseCache {
    private const val MAX_ENTRIES = 8
    private val lock = Any()
    private val map = object : LinkedHashMap<Long, CachedChapters>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, CachedChapters>?): Boolean =
            size > MAX_ENTRIES
    }

    fun get(workId: Long, file: File): List<ReaderChapter>? {
        val key = keyFor(workId, file)
        synchronized(lock) {
            val hit = map[workId] ?: return null
            if (hit.key != key) {
                map.remove(workId)
                return null
            }
            return hit.chapters
        }
    }

    fun put(workId: Long, file: File, chapters: List<ReaderChapter>) {
        val key = keyFor(workId, file)
        synchronized(lock) {
            map[workId] = CachedChapters(key, chapters)
        }
    }

    fun invalidate(workId: Long) {
        synchronized(lock) {
            map.remove(workId)
        }
    }

    fun clear() {
        synchronized(lock) {
            map.clear()
        }
    }

    fun keyFor(workId: Long, file: File): ChapterParseCacheKey =
        ChapterParseCacheKey(
            workId = workId,
            path = file.absolutePath,
            length = file.length(),
            lastModified = file.lastModified(),
        )
}
