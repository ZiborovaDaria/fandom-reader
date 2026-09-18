package com.fandomreader.feature.translation

import com.fandomreader.domain.model.DisplayTag
import com.fandomreader.domain.model.DisplayTagGroups
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
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FakeRepo : LibraryRepository {
    private val works = mutableListOf<Work>()
    override fun observeWorks(shelf: Shelf) = MutableStateFlow(works.filter { it.shelf == shelf })
    override fun observeFandoms() = MutableStateFlow(emptyList<Fandom>())
    override fun observePairings(fandomCanonicalKey: String) = MutableStateFlow(emptyList<Pairing>())
    override fun observeRomanticPairings(fandomCanonicalKey: String) = MutableStateFlow(emptyList<Pairing>())
    override fun observePlatonicRelationships(fandomCanonicalKey: String) = MutableStateFlow(emptyList<Pairing>())
    override fun observeAllRomanticPairings() = MutableStateFlow(emptyList<Pairing>())
    override fun observeAllPlatonicRelationships() = MutableStateFlow(emptyList<Pairing>())
    override fun observeWorksByFandomAndPairing(fandomCanonicalKey: String, pairingCanonicalKey: String) =
        MutableStateFlow(emptyList<Work>())
    override fun observeWorksByPairing(pairingCanonicalKey: String) = MutableStateFlow(emptyList<Work>())
    override fun observeWorksByDisplayTag(group: String, value: String) = MutableStateFlow(emptyList<Work>())
    override fun searchWorks(query: String, shelf: Shelf?) = MutableStateFlow(emptyList<Work>())
    override fun observeWorksHydrated(shelf: Shelf) = MutableStateFlow(works.filter { it.shelf == shelf })
    override fun observeWorksMatching(criteria: LibraryFilterCriteria) =
        MutableStateFlow(filterWorksByCriteria(works.filter { it.shelf == Shelf.FANFICTION }, criteria))
    override suspend fun getWork(id: Long) = works.firstOrNull { it.id == id }
    override suspend fun findWorkIdBySourceFingerprint(fingerprint: String): Long? =
        works.firstOrNull { it.sourceFingerprint == fingerprint }?.id
    override suspend fun findWorkIdBySourceRemote(source: WorkSource, remoteId: String): Long? =
        works.firstOrNull { it.source == source && it.remoteId == remoteId }?.id
    override suspend fun upsertWork(work: Work): Long {
        if (work.id != 0L) {
            val idx = works.indexOfFirst { it.id == work.id }
            if (idx >= 0) {
                works[idx] = work
                return work.id
            }
        }
        val id = (works.maxOfOrNull { it.id } ?: 0) + 1
        works += work.copy(id = id)
        return id
    }
    override suspend fun updateReadingProgress(workId: Long, chapterIndex: Int, offset: Int) {}
    override suspend fun touchLastOpened(workId: Long, openedAtMillis: Long) {}
    override suspend fun updateFileSizeBytes(workId: Long, fileSizeBytes: Long) {}
    override suspend fun updateTranslationFields(
        workId: Long,
        titleRu: String?,
        summaryRu: String?,
        status: TranslationStatus,
    ) {
        val idx = works.indexOfFirst { it.id == workId }
        if (idx >= 0) {
            works[idx] = works[idx].copy(titleRu = titleRu, summaryRu = summaryRu, translationStatus = status)
        }
    }
    override suspend fun rekeyCollapsedPlatonicPairings(): Int = 0
    override suspend fun coalesceTranslatedFandoms(): Int = 0
    override suspend fun updateFandomDisplayRu(canonicalKey: String, displayRu: String) {
        works.replaceAll { work ->
            work.copy(
                fandoms = work.fandoms.map { f ->
                    if (f.canonicalKey == canonicalKey) f.copy(displayRu = displayRu) else f
                },
            )
        }
    }
    override suspend fun updatePairingDisplayRu(canonicalKey: String, displayRu: String) {
        works.replaceAll { work ->
            work.copy(
                pairings = work.pairings.map { p ->
                    if (p.canonicalKey == canonicalKey) p.copy(displayRu = displayRu) else p
                },
            )
        }
    }
}

class TranslationPipelineTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun translationDoesNotDuplicateCanonicalPairings() = runBlocking {
        val repo = FakeRepo()
        val pairing = Pairing(
            canonicalKey = "harry-potter/tom-riddle",
            displayName = "Harry Potter/Tom Riddle",
            type = RelationshipType.ROMANTIC,
        )
        val id = repo.upsertWork(
            Work(
                source = WorkSource.AO3,
                remoteId = "1",
                sourceUrl = null,
                title = "Title",
                summary = "Summary",
                localPath = "x",
                format = "epub",
                shelf = Shelf.FANFICTION,
                fandoms = listOf(Fandom(canonicalKey = "hp", displayName = "HP")),
                pairings = listOf(pairing),
            ),
        )
        val pipeline = TranslationPipeline(FakeTranslationProvider(), repo)
        pipeline.translateWorkMetadata(id, tmp.root)
        val work = repo.getWork(id)!!
        assertThat(work.pairings.map { it.canonicalKey }.toSet()).hasSize(1)
        assertThat(work.titleRu).startsWith("[ru]")
        assertThat(work.summaryRu).startsWith("[ru]")
    }

    @Test
    fun chapterCacheSkipsRetranslation() = runBlocking {
        val repo = FakeRepo()
        val id = repo.upsertWork(
            Work(
                source = WorkSource.AO3,
                remoteId = "2",
                sourceUrl = null,
                title = "T",
                localPath = "x",
                format = "epub",
                shelf = Shelf.FANFICTION,
            ),
        )
        val pipeline = TranslationPipeline(FakeTranslationProvider(), repo)
        val first = pipeline.translateChapter(id, 0, "Hello", tmp.root)
        val second = pipeline.translateChapter(id, 0, "CHANGED", tmp.root)
        assertThat(first).isEqualTo(second)
        assertThat(File(tmp.root, "translations_v3/$id/chapter_0.txt").exists()).isTrue()
    }

    @Test
    fun chapterTranslationPreservesParagraphBoundaries() = runBlocking {
        val repo = FakeRepo()
        val id = repo.upsertWork(
            Work(
                source = WorkSource.AO3,
                remoteId = "3",
                sourceUrl = null,
                title = "T",
                localPath = "x",
                format = "epub",
                shelf = Shelf.FANFICTION,
            ),
        )
        val pipeline = TranslationPipeline(FakeTranslationProvider(), repo)
        val original = "First paragraph.\n\nSecond paragraph."
        val translated = pipeline.translateChapter(id, 0, original, tmp.root)
        val parts = TranslationPipeline.splitParagraphs(translated)
        assertThat(parts).hasSize(2)
        assertThat(parts[0]).startsWith("[ru]")
        assertThat(parts[0]).contains("First paragraph.")
        assertThat(parts[1]).contains("Second paragraph.")
        assertThat(translated).contains(TranslationPipeline.PARAGRAPH_SEPARATOR)
    }

    @Test
    fun chapterTranslationPreservesNoteBodyMarkers() = runBlocking {
        val repo = FakeRepo()
        val id = repo.upsertWork(
            Work(
                source = WorkSource.AO3,
                remoteId = "5",
                sourceUrl = null,
                title = "T",
                localPath = "x",
                format = "epub",
                shelf = Shelf.FANFICTION,
            ),
        )
        val pipeline = TranslationPipeline(FakeTranslationProvider(), repo)
        val original = listOf(
            TranslationPipeline.NOTE_SEGMENT_MARKER,
            "Chapter Notes",
            "Author aside.",
            TranslationPipeline.BODY_SEGMENT_MARKER,
            "Story paragraph one.",
        ).joinToString(TranslationPipeline.PARAGRAPH_SEPARATOR)
        val translated = pipeline.translateChapter(id, 0, original, tmp.root)
        val parts = TranslationPipeline.splitParagraphs(translated)
        assertThat(parts).contains(TranslationPipeline.NOTE_SEGMENT_MARKER)
        assertThat(parts).contains(TranslationPipeline.BODY_SEGMENT_MARKER)
        assertThat(parts.none { it.startsWith("[ru]") && it.contains("§§") }).isTrue()
        val noteIdx = parts.indexOf(TranslationPipeline.NOTE_SEGMENT_MARKER)
        val bodyIdx = parts.indexOf(TranslationPipeline.BODY_SEGMENT_MARKER)
        assertThat(noteIdx).isLessThan(bodyIdx)
        assertThat(parts[noteIdx + 1]).contains("Chapter Notes")
        assertThat(parts[bodyIdx + 1]).contains("Story paragraph one.")
    }

    @Test
    fun legacyUnsegmentedCacheStillReadableAsBody() {
        val plain = "Just a translated chapter without markers."
        val parts = TranslationPipeline.splitParagraphs(plain)
        assertThat(parts).hasSize(1)
        assertThat(TranslationPipeline.isSegmentMarker(parts[0])).isFalse()
    }

    @Test
    fun legacyTranslationsDirIsNotUsedAsCache() = runBlocking {
        val repo = FakeRepo()
        val id = repo.upsertWork(
            Work(
                source = WorkSource.AO3,
                remoteId = "4",
                sourceUrl = null,
                title = "T",
                localPath = "x",
                format = "epub",
                shelf = Shelf.FANFICTION,
            ),
        )
        val legacy = File(tmp.root, "translations/$id").also { it.mkdirs() }
        File(legacy, "chapter_0.txt").writeText("LEGACY FLAT", Charsets.UTF_8)
        val pipeline = TranslationPipeline(FakeTranslationProvider(), repo)
        val translated = pipeline.translateChapter(id, 0, "Hello\n\nWorld", tmp.root)
        assertThat(translated).isNotEqualTo("LEGACY FLAT")
        assertThat(translated).contains(TranslationPipeline.PARAGRAPH_SEPARATOR)
    }

    @Test
    fun translateWorkUpdatesMetadataAndMarksComplete() = runBlocking {
        val repo = FakeRepo()
        val id = repo.upsertWork(
            Work(
                source = WorkSource.AO3,
                remoteId = "5",
                sourceUrl = null,
                title = "Title",
                summary = "Summary",
                localPath = "x",
                format = "epub",
                shelf = Shelf.FANFICTION,
            ),
        )
        val pipeline = TranslationPipeline(FakeTranslationProvider(), repo)
        pipeline.translateWork(id, tmp.root, listOf("Chapter one.", "Chapter two."))
        val work = repo.getWork(id)!!
        assertThat(work.translationStatus).isEqualTo(TranslationStatus.COMPLETE)
        assertThat(work.titleRu).startsWith("[ru]")
        assertThat(work.summaryRu).startsWith("[ru]")
        assertThat(File(tmp.root, "translations_v3/$id/chapter_0.txt").exists()).isTrue()
        assertThat(File(tmp.root, "translations_v3/$id/chapter_1.txt").exists()).isTrue()
    }

    @Test
    fun chunkParagraphsRespectsMaxChars() {
        val paras = listOf("aaaa", "bbbb", "cccc", "dddd")
        val tight = TranslationPipeline.chunkParagraphs(paras, maxChars = 9)
        assertThat(tight).hasSize(4)
        val mid = TranslationPipeline.chunkParagraphs(paras, maxChars = 20)
        assertThat(mid).hasSize(2)
        assertThat(mid[0]).containsExactly("aaaa", "bbbb", "cccc").inOrder()
        assertThat(mid[1]).containsExactly("dddd")
        val wide = TranslationPipeline.chunkParagraphs(paras, maxChars = 50)
        assertThat(wide).hasSize(1)
        assertThat(wide[0]).containsExactly("aaaa", "bbbb", "cccc", "dddd").inOrder()
    }

    @Test
    fun chunkNeverSplitsMidParagraph() {
        val long = "x".repeat(100)
        val chunks = TranslationPipeline.chunkParagraphs(listOf(long, "short"), maxChars = 50)
        assertThat(chunks).hasSize(2)
        assertThat(chunks[0]).containsExactly(long)
        assertThat(chunks[1]).containsExactly("short")
    }

    @Test
    fun metadataTranslationPersistsFandomPairingAndDisplayTagRu() {
        runBlocking {
            val repo = FakeRepo()
            val id = repo.upsertWork(
                Work(
                    source = WorkSource.AO3,
                    remoteId = "meta-ru",
                    sourceUrl = null,
                    title = "Title",
                    summary = "Summary",
                    localPath = "x",
                    format = "epub",
                    shelf = Shelf.FANFICTION,
                    fandoms = listOf(Fandom(canonicalKey = "hp", displayName = "Harry Potter")),
                    pairings = listOf(
                        Pairing(
                            canonicalKey = "harry-potter/tom-riddle",
                            displayName = "Harry Potter/Tom Riddle",
                            type = RelationshipType.ROMANTIC,
                        ),
                    ),
                    displayTags = listOf(
                        DisplayTag(DisplayTagGroups.RATING, "Mature"),
                        DisplayTag(DisplayTagGroups.CHARACTERS, "Harry Potter"),
                    ),
                ),
            )
            TranslationPipeline(FakeTranslationProvider(), repo).translateWorkMetadata(id, tmp.root)
            val work = repo.getWork(id)!!
            assertThat(work.fandoms.single().displayRu).startsWith("[ru]")
            assertThat(work.pairings.single().displayRu).startsWith("[ru]")
            assertThat(work.displayTags).hasSize(2)
            assertThat(work.displayTags.all { !it.valueRu.isNullOrBlank() && it.valueRu!!.startsWith("[ru]") }).isTrue()
            assertThat(work.displayTags.map { it.group to it.value })
                .containsExactly(
                    DisplayTagGroups.RATING to "Mature",
                    DisplayTagGroups.CHARACTERS to "Harry Potter",
                )
        }
    }
}
