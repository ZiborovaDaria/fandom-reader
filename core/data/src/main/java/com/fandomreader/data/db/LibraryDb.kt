package com.fandomreader.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "works",
    indices = [
        Index(value = ["source", "remoteId"], unique = true),
        Index(value = ["sourceFingerprint"], unique = true),
    ],
)
data class WorkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val remoteId: String?,
    val sourceUrl: String?,
    val title: String,
    val titleRu: String?,
    val author: String?,
    val summary: String?,
    val summaryRu: String?,
    val language: String?,
    val localPath: String,
    val format: String,
    val shelf: String,
    val translationStatus: String,
    val readingProgressChapterIndex: Int,
    val readingProgressOffset: Int,
    val lastOpenedAt: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val sourceFingerprint: String? = null,
    /** Encoded via [DisplayTagsCodec]; display-only, not filter keys. */
    val displayTagsJson: String? = null,
)

@Entity(tableName = "fandoms", indices = [Index(value = ["canonicalKey"], unique = true)])
data class FandomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val canonicalKey: String,
    val displayName: String,
    val displayRu: String?,
)

@Entity(tableName = "pairings", indices = [Index(value = ["canonicalKey"], unique = true)])
data class PairingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val canonicalKey: String,
    val displayName: String,
    val displayRu: String?,
    val type: String,
)

@Entity(
    tableName = "work_fandoms",
    primaryKeys = ["workId", "fandomId"],
    foreignKeys = [
        ForeignKey(entity = WorkEntity::class, parentColumns = ["id"], childColumns = ["workId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = FandomEntity::class, parentColumns = ["id"], childColumns = ["fandomId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class WorkFandomCrossRef(val workId: Long, val fandomId: Long)

@Entity(
    tableName = "work_pairings",
    primaryKeys = ["workId", "pairingId"],
    foreignKeys = [
        ForeignKey(entity = WorkEntity::class, parentColumns = ["id"], childColumns = ["workId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = PairingEntity::class, parentColumns = ["id"], childColumns = ["pairingId"], onDelete = ForeignKey.CASCADE),
    ],
)
data class WorkPairingCrossRef(val workId: Long, val pairingId: Long)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM works WHERE shelf = :shelf ORDER BY title COLLATE NOCASE")
    fun observeWorksByShelf(shelf: String): Flow<List<WorkEntity>>

    @Query("SELECT * FROM works WHERE id = :id")
    suspend fun getWork(id: Long): WorkEntity?

    @Query("SELECT * FROM fandoms ORDER BY displayName COLLATE NOCASE")
    fun observeFandoms(): Flow<List<FandomEntity>>

    @Query(
        """
        SELECT DISTINCT p.* FROM pairings p
        INNER JOIN work_pairings wp ON wp.pairingId = p.id
        INNER JOIN work_fandoms wf ON wf.workId = wp.workId
        INNER JOIN fandoms f ON f.id = wf.fandomId
        WHERE f.canonicalKey = :fandomKey AND p.type = :type
        ORDER BY p.displayName COLLATE NOCASE
        """,
    )
    fun observePairingsForFandomByType(fandomKey: String, type: String): Flow<List<PairingEntity>>

    @Query("SELECT * FROM pairings WHERE type = :type ORDER BY displayName COLLATE NOCASE")
    fun observePairingsByType(type: String): Flow<List<PairingEntity>>

    @Query(
        """
        SELECT DISTINCT w.* FROM works w
        INNER JOIN work_pairings wp ON wp.workId = w.id
        INNER JOIN pairings p ON p.id = wp.pairingId
        WHERE p.canonicalKey = :pairingKey
        ORDER BY w.title COLLATE NOCASE
        """,
    )
    fun observeWorksByPairing(pairingKey: String): Flow<List<WorkEntity>>

    @Query("SELECT * FROM works WHERE shelf = :shelf ORDER BY title COLLATE NOCASE")
    fun observeWorksByShelfRaw(shelf: String): Flow<List<WorkEntity>>

    @Query("SELECT * FROM pairings WHERE type = :type")
    suspend fun getPairingsByType(type: String): List<PairingEntity>

    @Query("UPDATE pairings SET canonicalKey = :newKey WHERE id = :id")
    suspend fun updatePairingCanonicalKey(id: Long, newKey: String)

    @Query("SELECT * FROM fandoms")
    suspend fun getAllFandoms(): List<FandomEntity>

    @Query("UPDATE fandoms SET displayName = :displayName, displayRu = :displayRu WHERE id = :id")
    suspend fun updateFandomLabels(id: Long, displayName: String, displayRu: String?)

    @Query("UPDATE fandoms SET displayRu = :displayRu WHERE canonicalKey = :canonicalKey")
    suspend fun updateFandomDisplayRu(canonicalKey: String, displayRu: String)

    @Query("UPDATE pairings SET displayRu = :displayRu WHERE canonicalKey = :canonicalKey")
    suspend fun updatePairingDisplayRu(canonicalKey: String, displayRu: String)

    @Query("UPDATE work_fandoms SET fandomId = :keepId WHERE fandomId = :dropId")
    suspend fun reassignWorkFandoms(dropId: Long, keepId: Long)

    @Query("DELETE FROM fandoms WHERE id = :id")
    suspend fun deleteFandom(id: Long)

    @Query("SELECT workId FROM work_fandoms WHERE fandomId = :fandomId")
    suspend fun workIdsForFandom(fandomId: Long): List<Long>

    @Query(
        """
        SELECT DISTINCT p.* FROM pairings p
        INNER JOIN work_pairings wp ON wp.pairingId = p.id
        INNER JOIN work_fandoms wf ON wf.workId = wp.workId
        INNER JOIN fandoms f ON f.id = wf.fandomId
        WHERE f.canonicalKey = :fandomKey
        ORDER BY p.displayName COLLATE NOCASE
        """,
    )
    fun observePairingsForFandom(fandomKey: String): Flow<List<PairingEntity>>

    @Query(
        """
        SELECT DISTINCT w.* FROM works w
        INNER JOIN work_fandoms wf ON wf.workId = w.id
        INNER JOIN fandoms f ON f.id = wf.fandomId
        INNER JOIN work_pairings wp ON wp.workId = w.id
        INNER JOIN pairings p ON p.id = wp.pairingId
        WHERE f.canonicalKey = :fandomKey AND p.canonicalKey = :pairingKey
        ORDER BY w.title COLLATE NOCASE
        """,
    )
    fun observeWorksByFandomAndPairing(fandomKey: String, pairingKey: String): Flow<List<WorkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWork(work: WorkEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFandom(fandom: FandomEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPairing(pairing: PairingEntity): Long

    @Query("SELECT id FROM fandoms WHERE canonicalKey = :key LIMIT 1")
    suspend fun findFandomId(key: String): Long?

    @Query("SELECT id FROM pairings WHERE canonicalKey = :key LIMIT 1")
    suspend fun findPairingId(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkFandom(ref: WorkFandomCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkPairing(ref: WorkPairingCrossRef)

    @Query("SELECT * FROM fandoms INNER JOIN work_fandoms ON fandoms.id = work_fandoms.fandomId WHERE work_fandoms.workId = :workId")
    suspend fun fandomsForWork(workId: Long): List<FandomEntity>

    @Query("SELECT * FROM pairings INNER JOIN work_pairings ON pairings.id = work_pairings.pairingId WHERE work_pairings.workId = :workId")
    suspend fun pairingsForWork(workId: Long): List<PairingEntity>

    @Query("UPDATE works SET readingProgressChapterIndex = :chapterIndex, readingProgressOffset = :offset WHERE id = :workId")
    suspend fun updateProgress(workId: Long, chapterIndex: Int, offset: Int)

    @Query("UPDATE works SET lastOpenedAt = :openedAt WHERE id = :workId")
    suspend fun updateLastOpened(workId: Long, openedAt: Long)

    @Query("UPDATE works SET fileSizeBytes = :fileSizeBytes WHERE id = :workId")
    suspend fun updateFileSizeBytes(workId: Long, fileSizeBytes: Long)

    @Query("UPDATE works SET titleRu = :titleRu, summaryRu = :summaryRu, translationStatus = :status WHERE id = :workId")
    suspend fun updateTranslation(workId: Long, titleRu: String?, summaryRu: String?, status: String)

    @Query("SELECT id FROM works WHERE source = :source AND remoteId = :remoteId LIMIT 1")
    suspend fun findWorkId(source: String, remoteId: String?): Long?

    @Query("SELECT id FROM works WHERE sourceFingerprint = :fingerprint LIMIT 1")
    suspend fun findWorkIdByFingerprint(fingerprint: String): Long?

    @Transaction
    suspend fun upsertFull(
        work: WorkEntity,
        fandoms: List<FandomEntity>,
        pairings: List<PairingEntity>,
    ): Long {
        val existingId = when {
            work.remoteId != null -> findWorkId(work.source, work.remoteId)
            !work.sourceFingerprint.isNullOrBlank() -> findWorkIdByFingerprint(work.sourceFingerprint)
            else -> null
        }
        val workId = if (existingId != null) {
            val previous = getWork(existingId)
            val merged = if (previous == null) {
                work.copy(id = existingId)
            } else {
                work.copy(
                    id = existingId,
                    titleRu = work.titleRu?.takeIf { it.isNotBlank() } ?: previous.titleRu,
                    summaryRu = work.summaryRu?.takeIf { it.isNotBlank() } ?: previous.summaryRu,
                    translationStatus = if (
                        work.translationStatus == "NONE" &&
                        previous.translationStatus != "NONE"
                    ) {
                        previous.translationStatus
                    } else {
                        work.translationStatus
                    },
                    readingProgressChapterIndex = previous.readingProgressChapterIndex,
                    readingProgressOffset = previous.readingProgressOffset,
                    lastOpenedAt = previous.lastOpenedAt,
                    fileSizeBytes = if (work.fileSizeBytes > 0L) work.fileSizeBytes else previous.fileSizeBytes,
                    displayTagsJson = mergeDisplayTagsJson(previous.displayTagsJson, work.displayTagsJson),
                )
            }
            insertWork(merged)
            existingId
        } else {
            insertWork(work.copy(id = 0))
        }
        fandoms.forEach { f ->
            insertFandom(f.copy(id = 0))
            val fid = findFandomId(f.canonicalKey) ?: return@forEach
            if (!f.displayRu.isNullOrBlank()) {
                updateFandomDisplayRu(f.canonicalKey, f.displayRu)
            }
            insertWorkFandom(WorkFandomCrossRef(workId, fid))
        }
        pairings.forEach { p ->
            insertPairing(p.copy(id = 0))
            val pid = findPairingId(p.canonicalKey) ?: return@forEach
            if (!p.displayRu.isNullOrBlank()) {
                updatePairingDisplayRu(p.canonicalKey, p.displayRu)
            }
            insertWorkPairing(WorkPairingCrossRef(workId, pid))
        }
        return workId
    }
}

private fun mergeDisplayTagsJson(existing: String?, incoming: String?): String? {
    val oldTags = DisplayTagsCodec.decode(existing)
    val newTags = DisplayTagsCodec.decode(incoming)
    if (oldTags.isEmpty()) return incoming
    val merged = newTags.map { tag ->
        if (!tag.valueRu.isNullOrBlank()) tag
        else {
            val prev = oldTags.find {
                it.group.equals(tag.group, ignoreCase = true) &&
                    it.value.equals(tag.value, ignoreCase = true)
            }
            if (prev?.valueRu.isNullOrBlank()) tag else tag.copy(valueRu = prev?.valueRu)
        }
    }
    return DisplayTagsCodec.encode(merged) ?: incoming
}

@Database(
    entities = [
        WorkEntity::class,
        FandomEntity::class,
        PairingEntity::class,
        WorkFandomCrossRef::class,
        WorkPairingCrossRef::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class FandomReaderDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
