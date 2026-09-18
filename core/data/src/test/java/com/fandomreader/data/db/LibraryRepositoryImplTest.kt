package com.fandomreader.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fandomreader.data.LibraryRepositoryImpl
import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryRepositoryImplTest {
    private lateinit var db: FandomReaderDatabase
    private lateinit var repository: LibraryRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FandomReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LibraryRepositoryImpl(db.libraryDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertAndFilterByFandomPairing() = runTest {
        val work = Work(
            source = WorkSource.AO3,
            remoteId = "123",
            sourceUrl = null,
            title = "Test",
            summary = "Summary",
            language = "en",
            localPath = "/tmp/test.epub",
            format = "epub",
            shelf = Shelf.FANFICTION,
            fandoms = listOf(Fandom(canonicalKey = "hp", displayName = "Harry Potter")),
            pairings = listOf(
                Pairing(
                    canonicalKey = "harry/draco",
                    displayName = "Harry/Draco",
                    type = RelationshipType.ROMANTIC,
                ),
            ),
        )
        val id = repository.upsertWork(work)
        assertThat(id).isGreaterThan(0)

        val fandoms = repository.observeFandoms().first()
        assertThat(fandoms.map { it.canonicalKey }).contains("hp")

        val pairings = repository.observePairings("hp").first()
        assertThat(pairings.map { it.canonicalKey }).contains("harry/draco")

        val works = repository.observeWorksByFandomAndPairing("hp", "harry/draco").first()
        assertThat(works).hasSize(1)
        assertThat(works.first().title).isEqualTo("Test")
    }

    @Test
    fun upsertPersistsDisplayTags() = runTest {
        val work = Work(
            source = WorkSource.AO3,
            remoteId = "tags-1",
            sourceUrl = null,
            title = "Tagged",
            summary = "Summary",
            language = "en",
            localPath = "/tmp/tagged.epub",
            format = "epub",
            shelf = Shelf.FANFICTION,
            displayTags = listOf(
                com.fandomreader.domain.model.DisplayTag("rating", "Mature"),
                com.fandomreader.domain.model.DisplayTag("characters", "Harry Potter"),
            ),
        )
        val id = repository.upsertWork(work)
        val loaded = repository.getWork(id)!!
        assertThat(loaded.displayTags).hasSize(2)
        assertThat(loaded.displayTags.map { it.group to it.value })
            .containsExactly("rating" to "Mature", "characters" to "Harry Potter")
    }

    @Test
    fun rekeyCollapsedPlatonicPairings() = runTest {
        val badKey = "harry potter / tom riddle"
        val work = Work(
            source = WorkSource.AO3,
            remoteId = "plat-1",
            sourceUrl = null,
            title = "P",
            localPath = "/tmp/p.epub",
            format = "epub",
            shelf = Shelf.FANFICTION,
            pairings = listOf(
                Pairing(
                    canonicalKey = badKey,
                    displayName = "Harry Potter & Tom Riddle",
                    type = RelationshipType.PLATONIC,
                ),
            ),
        )
        val id = repository.upsertWork(work)
        val n = repository.rekeyCollapsedPlatonicPairings()
        assertThat(n).isAtLeast(1)
        val loaded = repository.getWork(id)!!
        assertThat(loaded.pairings.single().canonicalKey).contains("&")
        assertThat(loaded.pairings.single().canonicalKey).doesNotContain("/")
    }

    @Test
    fun coalesceTranslatedFandomsMergesEnRu() = runTest {
        repository.upsertWork(
            Work(
                source = WorkSource.AO3,
                remoteId = "f1",
                sourceUrl = null,
                title = "A",
                localPath = "/a",
                format = "epub",
                shelf = Shelf.FANFICTION,
                fandoms = listOf(
                    Fandom(canonicalKey = "harry potter - j. k. rowling", displayName = "Harry Potter - J. K. Rowling", displayRu = "Гарри Поттер"),
                ),
            ),
        )
        repository.upsertWork(
            Work(
                source = WorkSource.FICBOOK,
                remoteId = "f2",
                sourceUrl = null,
                title = "B",
                localPath = "/b",
                format = "epub",
                shelf = Shelf.FANFICTION,
                fandoms = listOf(
                    Fandom(canonicalKey = "гарри поттер", displayName = "Гарри Поттер"),
                ),
            ),
        )
        val before = repository.observeFandoms().first().size
        assertThat(before).isAtLeast(2)
        repository.coalesceTranslatedFandoms()
        val after = repository.observeFandoms().first()
        assertThat(after.size).isLessThan(before)
    }
}
