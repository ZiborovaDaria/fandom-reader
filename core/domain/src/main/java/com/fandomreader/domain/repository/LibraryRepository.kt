package com.fandomreader.domain.repository

import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.LibraryFilterCriteria
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun observeWorks(shelf: Shelf): Flow<List<Work>>
    fun observeFandoms(): Flow<List<Fandom>>
    /** Romantic pairings for a fandom (type ROMANTIC only). */
    fun observePairings(fandomCanonicalKey: String): Flow<List<Pairing>>
    fun observeRomanticPairings(fandomCanonicalKey: String): Flow<List<Pairing>>
    fun observePlatonicRelationships(fandomCanonicalKey: String): Flow<List<Pairing>>
    fun observeAllRomanticPairings(): Flow<List<Pairing>>
    fun observeAllPlatonicRelationships(): Flow<List<Pairing>>
    fun observeWorksByFandomAndPairing(
        fandomCanonicalKey: String,
        pairingCanonicalKey: String,
    ): Flow<List<Work>>
    fun observeWorksByPairing(pairingCanonicalKey: String): Flow<List<Work>>
    fun observeWorksByDisplayTag(group: String, value: String): Flow<List<Work>>
    fun searchWorks(query: String, shelf: Shelf? = Shelf.FANFICTION): Flow<List<Work>>
    /** Works with fandoms, pairings, and display tags loaded. */
    fun observeWorksHydrated(shelf: Shelf): Flow<List<Work>>
    fun observeWorksMatching(criteria: LibraryFilterCriteria): Flow<List<Work>>

    suspend fun getWork(id: Long): Work?
    suspend fun findWorkIdBySourceFingerprint(fingerprint: String): Long?
    suspend fun findWorkIdBySourceRemote(source: WorkSource, remoteId: String): Long?
    suspend fun upsertWork(work: Work): Long
    suspend fun updateReadingProgress(workId: Long, chapterIndex: Int, offset: Int)
    suspend fun touchLastOpened(workId: Long, openedAtMillis: Long = System.currentTimeMillis())
    suspend fun updateFileSizeBytes(workId: Long, fileSizeBytes: Long)
    suspend fun updateTranslationFields(
        workId: Long,
        titleRu: String?,
        summaryRu: String?,
        status: com.fandomreader.domain.model.TranslationStatus,
    )
    /** Rekey platonic pairings whose canonicalKey collapsed & into /. */
    suspend fun rekeyCollapsedPlatonicPairings(): Int
    /** Merge fandom rows that represent the same fandom after RU labels are set. */
    suspend fun coalesceTranslatedFandoms(): Int
    suspend fun updateFandomDisplayRu(canonicalKey: String, displayRu: String)
    suspend fun updatePairingDisplayRu(canonicalKey: String, displayRu: String)
}
