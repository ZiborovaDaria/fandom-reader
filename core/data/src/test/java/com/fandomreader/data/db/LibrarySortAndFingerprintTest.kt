package com.fandomreader.data.db

import com.fandomreader.data.LibraryRepositoryImpl
import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context

@RunWith(RobolectricTestRunner::class)
class LibrarySortAndFingerprintTest {
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
    fun observeWorks_sortedByTitleCaseInsensitive() = runTest {
        repository.upsertWork(sample(title = "zeta", remoteId = "1"))
        repository.upsertWork(sample(title = "Alpha", remoteId = "2"))
        repository.upsertWork(sample(title = "beta", remoteId = "3"))

        val titles = repository.observeWorks(Shelf.FANFICTION).first().map { it.title }
        assertThat(titles).containsExactly("Alpha", "beta", "zeta").inOrder()
    }

    @Test
    fun fingerprintDedupsOtherWorks() = runTest {
        val first = sample(
            title = "Other",
            remoteId = null,
            source = WorkSource.OTHER,
            shelf = Shelf.OTHER,
            fingerprint = "/dl/book.epub|12",
        )
        val id1 = repository.upsertWork(first)
        val id2 = repository.upsertWork(first.copy(title = "Other renamed"))
        assertThat(id1).isEqualTo(id2)
        assertThat(repository.observeWorks(Shelf.OTHER).first()).hasSize(1)
    }

    @Test
    fun fandomPairingFilterUsesCanonicalKey() = runTest {
        repository.upsertWork(
            sample(title = "Match", remoteId = "9").copy(
                fandoms = listOf(Fandom(canonicalKey = "hp", displayName = "Harry Potter")),
                pairings = listOf(
                    Pairing(
                        canonicalKey = "harry/draco",
                        displayName = "Harry/Draco",
                        type = RelationshipType.ROMANTIC,
                    ),
                ),
            ),
        )
        val works = repository.observeWorksByFandomAndPairing("hp", "harry/draco").first()
        assertThat(works.map { it.title }).containsExactly("Match")
    }

    private fun sample(
        title: String,
        remoteId: String?,
        source: WorkSource = WorkSource.AO3,
        shelf: Shelf = Shelf.FANFICTION,
        fingerprint: String? = null,
    ) = Work(
        source = source,
        remoteId = remoteId,
        sourceUrl = null,
        title = title,
        summary = "s",
        language = "en",
        localPath = "/tmp/$title",
        format = "epub",
        shelf = shelf,
        sourceFingerprint = fingerprint,
    )
}
