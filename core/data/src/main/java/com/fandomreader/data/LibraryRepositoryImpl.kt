package com.fandomreader.data

import com.fandomreader.data.db.DisplayTagsCodec
import com.fandomreader.data.db.FandomEntity
import com.fandomreader.data.db.LibraryDao
import com.fandomreader.data.db.PairingEntity
import com.fandomreader.data.db.WorkEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val dao: LibraryDao,
) : LibraryRepository {
    override fun observeWorks(shelf: Shelf): Flow<List<Work>> =
        dao.observeWorksByShelf(shelf.name).map { list -> list.map { it.toDomain(emptyList(), emptyList()) } }

    override fun observeFandoms(): Flow<List<Fandom>> =
        dao.observeFandoms().map { list -> list.map { it.toDomain() } }

    override fun observePairings(fandomCanonicalKey: String): Flow<List<Pairing>> =
        observeRomanticPairings(fandomCanonicalKey)

    override fun observeRomanticPairings(fandomCanonicalKey: String): Flow<List<Pairing>> =
        dao.observePairingsForFandomByType(fandomCanonicalKey, RelationshipType.ROMANTIC.name)
            .map { list -> list.map { it.toDomain() } }

    override fun observePlatonicRelationships(fandomCanonicalKey: String): Flow<List<Pairing>> =
        dao.observePairingsForFandomByType(fandomCanonicalKey, RelationshipType.PLATONIC.name)
            .map { list -> list.map { it.toDomain() } }

    override fun observeAllRomanticPairings(): Flow<List<Pairing>> =
        dao.observePairingsByType(RelationshipType.ROMANTIC.name).map { list -> list.map { it.toDomain() } }

    override fun observeAllPlatonicRelationships(): Flow<List<Pairing>> =
        dao.observePairingsByType(RelationshipType.PLATONIC.name).map { list -> list.map { it.toDomain() } }

    override fun observeWorksByFandomAndPairing(
        fandomCanonicalKey: String,
        pairingCanonicalKey: String,
    ): Flow<List<Work>> =
        dao.observeWorksByFandomAndPairing(fandomCanonicalKey, pairingCanonicalKey)
            .map { list -> list.map { it.toDomain(emptyList(), emptyList()) } }

    override fun observeWorksByPairing(pairingCanonicalKey: String): Flow<List<Work>> =
        dao.observeWorksByPairing(pairingCanonicalKey)
            .map { list -> list.map { it.toDomain(emptyList(), emptyList()) } }

    override fun observeWorksByDisplayTag(group: String, value: String): Flow<List<Work>> =
        dao.observeWorksByShelfRaw(Shelf.FANFICTION.name).map { entities ->
            entities.map { it.toDomain(emptyList(), emptyList()) }
                .filter { work ->
                    work.displayTags.any {
                        it.group.equals(group, ignoreCase = true) &&
                            it.value.equals(value, ignoreCase = true)
                    }
                }
        }

    override fun observeWorksHydrated(shelf: Shelf): Flow<List<Work>> = flow {
        dao.observeWorksByShelf(shelf.name).collect { entities ->
            emit(
                entities.map { entity ->
                    entity.toDomain(
                        dao.fandomsForWork(entity.id).map { it.toDomain() },
                        dao.pairingsForWork(entity.id).map { it.toDomain() },
                    )
                },
            )
        }
    }

    override fun observeWorksMatching(criteria: LibraryFilterCriteria): Flow<List<Work>> =
        observeWorksHydrated(Shelf.FANFICTION).map { filterWorksByCriteria(it, criteria) }

    override fun searchWorks(query: String, shelf: Shelf?): Flow<List<Work>> {
        val q = query.trim()
        val shelves = if (shelf != null) listOf(shelf) else listOf(Shelf.FANFICTION, Shelf.OTHER)
        if (shelves.size == 1) {
            return dao.observeWorksByShelf(shelves.first().name).map { entities ->
                filterSearch(entities.map { it.toDomain(emptyList(), emptyList()) }, q)
            }
        }
        return combine(
            dao.observeWorksByShelf(Shelf.FANFICTION.name),
            dao.observeWorksByShelf(Shelf.OTHER.name),
        ) { a, b -> a + b }.map { entities ->
            filterSearch(entities.map { it.toDomain(emptyList(), emptyList()) }, q)
        }
    }

    override suspend fun getWork(id: Long): Work? {
        val entity = dao.getWork(id) ?: return null
        return entity.toDomain(dao.fandomsForWork(id).map { it.toDomain() }, dao.pairingsForWork(id).map { it.toDomain() })
    }

    override suspend fun findWorkIdBySourceFingerprint(fingerprint: String): Long? =
        dao.findWorkIdByFingerprint(fingerprint)

    override suspend fun findWorkIdBySourceRemote(source: WorkSource, remoteId: String): Long? =
        dao.findWorkId(source.name, remoteId)

    override suspend fun upsertWork(work: Work): Long {
        return dao.upsertFull(
            work = work.toEntity(),
            fandoms = work.fandoms.map {
                FandomEntity(canonicalKey = it.canonicalKey, displayName = it.displayName, displayRu = it.displayRu)
            },
            pairings = work.pairings.map {
                PairingEntity(
                    canonicalKey = it.canonicalKey,
                    displayName = it.displayName,
                    displayRu = it.displayRu,
                    type = it.type.name,
                )
            },
        )
    }

    override suspend fun updateReadingProgress(workId: Long, chapterIndex: Int, offset: Int) {
        dao.updateProgress(workId, chapterIndex, offset)
    }

    override suspend fun touchLastOpened(workId: Long, openedAtMillis: Long) {
        dao.updateLastOpened(workId, openedAtMillis)
    }

    override suspend fun updateFileSizeBytes(workId: Long, fileSizeBytes: Long) {
        dao.updateFileSizeBytes(workId, fileSizeBytes)
    }

    override suspend fun updateTranslationFields(
        workId: Long,
        titleRu: String?,
        summaryRu: String?,
        status: TranslationStatus,
    ) {
        dao.updateTranslation(workId, titleRu, summaryRu, status.name)
    }

    override suspend fun rekeyCollapsedPlatonicPairings(): Int {
        val platonic = dao.getPairingsByType(RelationshipType.PLATONIC.name)
        var updated = 0
        for (p in platonic) {
            if (!p.displayName.contains('&')) continue
            val correct = TagKeys.canonicalKey(p.displayName)
            if (correct == p.canonicalKey) continue
            // If key still has / where display has &, it was collapsed
            if (p.canonicalKey.contains('/') && p.displayName.contains('&')) {
                dao.updatePairingCanonicalKey(p.id, correct)
                updated++
            }
        }
        return updated
    }

    override suspend fun updateFandomDisplayRu(canonicalKey: String, displayRu: String) {
        dao.updateFandomDisplayRu(canonicalKey, displayRu)
    }

    override suspend fun updatePairingDisplayRu(canonicalKey: String, displayRu: String) {
        dao.updatePairingDisplayRu(canonicalKey, displayRu)
    }

    override suspend fun coalesceTranslatedFandoms(): Int {
        val all = dao.getAllFandoms()
        var merges = 0
        // Prefer keeping entry that already has displayRu, merge others whose displayName
        // equals someone's displayRu (case-insensitive).
        val byRu = all.mapNotNull { f -> f.displayRu?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()?.let { it to f } }
            .groupBy({ it.first }, { it.second })
        for ((ru, owners) in byRu) {
            val keep = owners.first()
            val duplicates = all.filter { other ->
                other.id != keep.id &&
                    (other.displayName.trim().equals(keep.displayRu!!.trim(), ignoreCase = true) ||
                        other.displayRu?.trim().equals(keep.displayRu!!.trim(), ignoreCase = true) == true ||
                        other.displayName.trim().equals(ru, ignoreCase = true))
            }
            for (drop in duplicates) {
                dao.reassignWorkFandoms(drop.id, keep.id)
                dao.deleteFandom(drop.id)
                merges++
            }
        }
        // Also: displayRu of A equals displayName of B
        val refreshed = dao.getAllFandoms()
        for (a in refreshed) {
            val ru = a.displayRu?.trim()?.takeIf { it.isNotEmpty() } ?: continue
            val drop = refreshed.firstOrNull { b ->
                b.id != a.id && b.displayName.trim().equals(ru, ignoreCase = true)
            } ?: continue
            dao.reassignWorkFandoms(drop.id, a.id)
            val mergedName = a.displayName
            dao.updateFandomLabels(a.id, mergedName, ru)
            dao.deleteFandom(drop.id)
            merges++
        }
        return merges
    }
}

private fun filterSearch(works: List<Work>, query: String): List<Work> {
    if (query.isBlank()) return works.sortedBy { it.title.lowercase() }
    val q = query.lowercase()
    return works.filter { w ->
        listOfNotNull(w.title, w.titleRu, w.summary, w.summaryRu)
            .any { it.lowercase().contains(q) }
    }.sortedBy { it.title.lowercase() }
}

private fun WorkEntity.toDomain(fandoms: List<Fandom>, pairings: List<Pairing>) = Work(
    id = id,
    source = WorkSource.valueOf(source),
    remoteId = remoteId,
    sourceUrl = sourceUrl,
    title = title,
    titleRu = titleRu,
    author = author,
    summary = summary,
    summaryRu = summaryRu,
    language = language,
    localPath = localPath,
    format = format,
    shelf = Shelf.valueOf(shelf),
    translationStatus = TranslationStatus.valueOf(translationStatus),
    fandoms = fandoms,
    pairings = pairings,
    displayTags = DisplayTagsCodec.decode(displayTagsJson),
    readingProgressChapterIndex = readingProgressChapterIndex,
    readingProgressOffset = readingProgressOffset,
    lastOpenedAt = lastOpenedAt,
    fileSizeBytes = fileSizeBytes,
    sourceFingerprint = sourceFingerprint,
)

private fun Work.toEntity() = WorkEntity(
    id = id,
    source = source.name,
    remoteId = remoteId,
    sourceUrl = sourceUrl,
    title = title,
    titleRu = titleRu,
    author = author,
    summary = summary,
    summaryRu = summaryRu,
    language = language,
    localPath = localPath,
    format = format,
    shelf = shelf.name,
    translationStatus = translationStatus.name,
    readingProgressChapterIndex = readingProgressChapterIndex,
    readingProgressOffset = readingProgressOffset,
    lastOpenedAt = lastOpenedAt,
    fileSizeBytes = fileSizeBytes,
    sourceFingerprint = sourceFingerprint,
    displayTagsJson = DisplayTagsCodec.encode(displayTags),
)

private fun FandomEntity.toDomain() = Fandom(id, canonicalKey, displayName, displayRu)
private fun PairingEntity.toDomain() = Pairing(id, canonicalKey, displayName, displayRu, RelationshipType.valueOf(type))
