package com.fandomreader.data.repository

import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.LibraryFilterCriteria
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.TranslationStatus
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.domain.model.filterWorksByCriteria
import com.fandomreader.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory repository for early UI / unit tests without Room. */
class FakeInMemoryLibraryRepository(
    seed: List<Work> = defaultSeed(),
) : LibraryRepository {
    private val works = MutableStateFlow(seed)
    private var nextId = (seed.maxOfOrNull { it.id } ?: 0L) + 1L

    override fun observeWorks(shelf: Shelf): Flow<List<Work>> =
        works.map { list -> list.filter { it.shelf == shelf }.sortedBy { it.title.lowercase() } }

    override fun observeFandoms(): Flow<List<Fandom>> =
        works.map { list ->
            list.flatMap { it.fandoms }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName.lowercase() }
        }

    override fun observePairings(fandomCanonicalKey: String): Flow<List<Pairing>> =
        observeRomanticPairings(fandomCanonicalKey)

    override fun observeRomanticPairings(fandomCanonicalKey: String): Flow<List<Pairing>> =
        works.map { list ->
            list.filter { work -> work.fandoms.any { it.canonicalKey == fandomCanonicalKey } }
                .flatMap { it.pairings }
                .filter { it.type == RelationshipType.ROMANTIC }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName.lowercase() }
        }

    override fun observePlatonicRelationships(fandomCanonicalKey: String): Flow<List<Pairing>> =
        works.map { list ->
            list.filter { work -> work.fandoms.any { it.canonicalKey == fandomCanonicalKey } }
                .flatMap { it.pairings }
                .filter { it.type == RelationshipType.PLATONIC }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName.lowercase() }
        }

    override fun observeAllRomanticPairings(): Flow<List<Pairing>> =
        works.map { list ->
            list.flatMap { it.pairings }
                .filter { it.type == RelationshipType.ROMANTIC }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName.lowercase() }
        }

    override fun observeAllPlatonicRelationships(): Flow<List<Pairing>> =
        works.map { list ->
            list.flatMap { it.pairings }
                .filter { it.type == RelationshipType.PLATONIC }
                .distinctBy { it.canonicalKey }
                .sortedBy { it.displayName.lowercase() }
        }

    override fun observeWorksByFandomAndPairing(
        fandomCanonicalKey: String,
        pairingCanonicalKey: String,
    ): Flow<List<Work>> =
        works.map { list ->
            list.filter { work ->
                work.fandoms.any { it.canonicalKey == fandomCanonicalKey } &&
                    work.pairings.any { it.canonicalKey == pairingCanonicalKey }
            }.sortedBy { it.title.lowercase() }
        }

    override fun observeWorksByPairing(pairingCanonicalKey: String): Flow<List<Work>> =
        works.map { list ->
            list.filter { work -> work.pairings.any { it.canonicalKey == pairingCanonicalKey } }
                .sortedBy { it.title.lowercase() }
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
            if (q.isEmpty()) scoped.sortedBy { it.title.lowercase() }
            else scoped.filter { w ->
                listOfNotNull(w.title, w.titleRu, w.summary, w.summaryRu)
                    .any { it.lowercase().contains(q) }
            }.sortedBy { it.title.lowercase() }
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
        val existingId = when {
            work.remoteId != null ->
                works.value.firstOrNull { it.source == work.source && it.remoteId == work.remoteId }?.id
            !work.sourceFingerprint.isNullOrBlank() ->
                works.value.firstOrNull { it.sourceFingerprint == work.sourceFingerprint }?.id
            work.id != 0L -> work.id
            else -> null
        }
        val id = existingId ?: if (work.id != 0L) work.id else nextId++
        val stored = work.copy(id = id)
        works.update { current ->
            val without = current.filterNot { it.id == id }
            without + stored
        }
        return id
    }

    override suspend fun updateReadingProgress(workId: Long, chapterIndex: Int, offset: Int) {
        works.update { list ->
            list.map {
                if (it.id == workId) {
                    it.copy(
                        readingProgressChapterIndex = chapterIndex,
                        readingProgressOffset = offset,
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun touchLastOpened(workId: Long, openedAtMillis: Long) {
        works.update { list ->
            list.map {
                if (it.id == workId) it.copy(lastOpenedAt = openedAtMillis) else it
            }
        }
    }

    override suspend fun updateFileSizeBytes(workId: Long, fileSizeBytes: Long) {
        works.update { list ->
            list.map {
                if (it.id == workId) it.copy(fileSizeBytes = fileSizeBytes) else it
            }
        }
    }

    override suspend fun updateTranslationFields(
        workId: Long,
        titleRu: String?,
        summaryRu: String?,
        status: TranslationStatus,
    ) {
        works.update { list ->
            list.map {
                if (it.id == workId) {
                    it.copy(titleRu = titleRu, summaryRu = summaryRu, translationStatus = status)
                } else {
                    it
                }
            }
        }
    }

    override suspend fun rekeyCollapsedPlatonicPairings(): Int = 0

    override suspend fun coalesceTranslatedFandoms(): Int = 0

    override suspend fun updateFandomDisplayRu(canonicalKey: String, displayRu: String) {
        works.update { list ->
            list.map { work ->
                work.copy(
                    fandoms = work.fandoms.map { f ->
                        if (f.canonicalKey == canonicalKey) f.copy(displayRu = displayRu) else f
                    },
                )
            }
        }
    }

    override suspend fun updatePairingDisplayRu(canonicalKey: String, displayRu: String) {
        works.update { list ->
            list.map { work ->
                work.copy(
                    pairings = work.pairings.map { p ->
                        if (p.canonicalKey == canonicalKey) p.copy(displayRu = displayRu) else p
                    },
                )
            }
        }
    }

    companion object {
        fun defaultSeed(): List<Work> {
            val hp = Fandom(1, "harry-potter", "Harry Potter")
            val pairing = Pairing(
                id = 1,
                canonicalKey = "harry-potter/draco-malfoy",
                displayName = "Harry Potter/Draco Malfoy",
                type = RelationshipType.ROMANTIC,
            )
            return listOf(
                Work(
                    id = 1,
                    source = WorkSource.AO3,
                    remoteId = "demo-1",
                    sourceUrl = null,
                    title = "Demo Fanfic",
                    summary = "Sample work for early UI navigation.",
                    summaryRu = "Пример работы для навигации UI.",
                    language = "en",
                    localPath = "",
                    format = "epub",
                    shelf = Shelf.FANFICTION,
                    fandoms = listOf(hp),
                    pairings = listOf(pairing),
                ),
                Work(
                    id = 2,
                    source = WorkSource.OTHER,
                    remoteId = null,
                    sourceUrl = null,
                    title = "Demo Other Book",
                    summary = "Non-fanfiction sample.",
                    language = "ru",
                    localPath = "",
                    format = "fb2",
                    shelf = Shelf.OTHER,
                ),
            )
        }
    }
}
