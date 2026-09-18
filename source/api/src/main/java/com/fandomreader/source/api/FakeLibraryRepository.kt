package com.fandomreader.source.api

import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.LibraryFilterCriteria
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.TagKeys
import com.fandomreader.domain.model.TranslationStatus
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.domain.model.filterWorksByCriteria
import com.fandomreader.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeLibraryRepository : LibraryRepository {
    private val works = MutableStateFlow<List<Work>>(emptyList())
    private var nextId = 1L

    fun allWorks(): List<Work> = works.value

    override fun observeWorks(shelf: Shelf): Flow<List<Work>> =
        works.map { list -> list.filter { it.shelf == shelf } }

    override fun observeFandoms(): Flow<List<Fandom>> =
        works.map { list ->
            list.flatMap { it.fandoms }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName }
        }

    override fun observePairings(fandomCanonicalKey: String): Flow<List<Pairing>> =
        observeRomanticPairings(fandomCanonicalKey)

    override fun observeRomanticPairings(fandomCanonicalKey: String): Flow<List<Pairing>> =
        pairingsForFandom(fandomCanonicalKey, RelationshipType.ROMANTIC)

    override fun observePlatonicRelationships(fandomCanonicalKey: String): Flow<List<Pairing>> =
        pairingsForFandom(fandomCanonicalKey, RelationshipType.PLATONIC)

    override fun observeAllRomanticPairings(): Flow<List<Pairing>> =
        works.map { list ->
            list.flatMap { it.pairings }
                .filter { it.type == RelationshipType.ROMANTIC }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName }
        }

    override fun observeAllPlatonicRelationships(): Flow<List<Pairing>> =
        works.map { list ->
            list.flatMap { it.pairings }
                .filter { it.type == RelationshipType.PLATONIC }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName }
        }

    private fun pairingsForFandom(fandomKey: String, type: RelationshipType): Flow<List<Pairing>> =
        works.map { list ->
            list.filter { work -> work.fandoms.any { it.canonicalKey == fandomKey } }
                .flatMap { it.pairings }
                .filter { it.type == type }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName }
        }

    override fun observeWorksByFandomAndPairing(
        fandomCanonicalKey: String,
        pairingCanonicalKey: String,
    ): Flow<List<Work>> =
        works.map { list ->
            list.filter { work ->
                work.fandoms.any { it.canonicalKey == fandomCanonicalKey } &&
                    work.pairings.any { it.canonicalKey == pairingCanonicalKey }
            }
        }

    override fun observeWorksByPairing(pairingCanonicalKey: String): Flow<List<Work>> =
        works.map { list ->
            list.filter { work -> work.pairings.any { it.canonicalKey == pairingCanonicalKey } }
        }

    override fun observeWorksByDisplayTag(group: String, value: String): Flow<List<Work>> =
        works.map { list ->
            list.filter { work ->
                work.displayTags.any {
                    it.group.equals(group, ignoreCase = true) &&
                        it.value.equals(value, ignoreCase = true)
                }
            }
        }

    override fun searchWorks(query: String, shelf: Shelf?): Flow<List<Work>> =
        works.map { list ->
            val scoped = if (shelf != null) list.filter { it.shelf == shelf } else list
            val q = query.trim().lowercase()
            if (q.isEmpty()) scoped
            else scoped.filter { w ->
                listOfNotNull(w.title, w.titleRu, w.summary, w.summaryRu)
                    .any { it.lowercase().contains(q) }
            }
        }

    override fun observeWorksHydrated(shelf: Shelf): Flow<List<Work>> =
        observeWorks(shelf)

    override fun observeWorksMatching(criteria: LibraryFilterCriteria): Flow<List<Work>> =
        observeWorksHydrated(Shelf.FANFICTION).map { filterWorksByCriteria(it, criteria) }

    override suspend fun getWork(id: Long): Work? = works.value.firstOrNull { it.id == id }

    override suspend fun findWorkIdBySourceFingerprint(fingerprint: String): Long? =
        works.value.firstOrNull { it.sourceFingerprint == fingerprint }?.id

    override suspend fun findWorkIdBySourceRemote(source: WorkSource, remoteId: String): Long? =
        works.value.firstOrNull { it.source == source && it.remoteId == remoteId }?.id

    override suspend fun upsertWork(work: Work): Long {
        val existing = works.value.firstOrNull { current ->
            (work.remoteId != null &&
                current.source == work.source &&
                current.remoteId == work.remoteId) ||
                (!work.sourceFingerprint.isNullOrBlank() &&
                    current.sourceFingerprint == work.sourceFingerprint)
        }
        return if (existing != null) {
            val updated = work.copy(id = existing.id)
            works.value = works.value.map { if (it.id == existing.id) updated else it }
            existing.id
        } else {
            val id = if (work.id != 0L) work.id else nextId++
            val stored = work.copy(id = id)
            works.value = works.value + stored
            id
        }
    }

    override suspend fun updateReadingProgress(workId: Long, chapterIndex: Int, offset: Int) {
        works.value = works.value.map { work ->
            if (work.id == workId) {
                work.copy(
                    readingProgressChapterIndex = chapterIndex,
                    readingProgressOffset = offset,
                )
            } else {
                work
            }
        }
    }

    override suspend fun touchLastOpened(workId: Long, openedAtMillis: Long) {
        works.value = works.value.map { work ->
            if (work.id == workId) work.copy(lastOpenedAt = openedAtMillis) else work
        }
    }

    override suspend fun updateFileSizeBytes(workId: Long, fileSizeBytes: Long) {
        works.value = works.value.map { work ->
            if (work.id == workId) work.copy(fileSizeBytes = fileSizeBytes) else work
        }
    }

    override suspend fun updateTranslationFields(
        workId: Long,
        titleRu: String?,
        summaryRu: String?,
        status: TranslationStatus,
    ) {
        works.value = works.value.map { work ->
            if (work.id == workId) {
                work.copy(titleRu = titleRu, summaryRu = summaryRu, translationStatus = status)
            } else {
                work
            }
        }
    }

    override suspend fun rekeyCollapsedPlatonicPairings(): Int {
        var n = 0
        works.value = works.value.map { work ->
            val updated = work.pairings.map { p ->
                if (p.type == RelationshipType.PLATONIC &&
                    p.displayName.contains('&') &&
                    p.canonicalKey.contains('/')
                ) {
                    n++
                    p.copy(canonicalKey = TagKeys.canonicalKey(p.displayName))
                } else {
                    p
                }
            }
            work.copy(pairings = updated)
        }
        return n
    }

    override suspend fun coalesceTranslatedFandoms(): Int = 0

    override suspend fun updateFandomDisplayRu(canonicalKey: String, displayRu: String) {
        works.value = works.value.map { work ->
            work.copy(
                fandoms = work.fandoms.map { f ->
                    if (f.canonicalKey == canonicalKey) f.copy(displayRu = displayRu) else f
                },
            )
        }
    }

    override suspend fun updatePairingDisplayRu(canonicalKey: String, displayRu: String) {
        works.value = works.value.map { work ->
            work.copy(
                pairings = work.pairings.map { p ->
                    if (p.canonicalKey == canonicalKey) p.copy(displayRu = displayRu) else p
                },
            )
        }
    }
}
