package com.fandomreader.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LibraryFilterTest {
    private val hp = Fandom(canonicalKey = "hp", displayName = "Harry Potter", displayRu = "Гарри Поттер")
    private val naruto = Fandom(canonicalKey = "naruto", displayName = "Naruto")
    private val hd = Pairing(
        canonicalKey = "harry-potter/draco-malfoy",
        displayName = "Harry/Draco",
        type = RelationshipType.ROMANTIC,
    )
    private val ht = Pairing(
        canonicalKey = "harry-potter/tom-riddle",
        displayName = "Harry/Tom",
        type = RelationshipType.ROMANTIC,
    )

    private val workHdMature = Work(
        id = 1,
        source = WorkSource.AO3,
        remoteId = "1",
        sourceUrl = null,
        title = "HD Mature",
        localPath = "a",
        format = "epub",
        shelf = Shelf.FANFICTION,
        fandoms = listOf(hp),
        pairings = listOf(hd),
        displayTags = listOf(DisplayTag(DisplayTagGroups.RATING, "Mature")),
    )
    private val workHtTeen = Work(
        id = 2,
        source = WorkSource.AO3,
        remoteId = "2",
        sourceUrl = null,
        title = "HT Teen",
        localPath = "b",
        format = "epub",
        shelf = Shelf.FANFICTION,
        fandoms = listOf(hp),
        pairings = listOf(ht),
        displayTags = listOf(DisplayTag(DisplayTagGroups.RATING, "Teen And Up Audiences")),
    )
    private val workNaruto = Work(
        id = 3,
        source = WorkSource.AO3,
        remoteId = "3",
        sourceUrl = null,
        title = "Naruto Slash",
        localPath = "c",
        format = "epub",
        shelf = Shelf.FANFICTION,
        fandoms = listOf(naruto),
        pairings = listOf(hd.copy(canonicalKey = "kakashi/iruka", displayName = "Kakashi/Iruka")),
        displayTags = listOf(DisplayTag(DisplayTagGroups.RATING, "Mature")),
    )

    private val all = listOf(workHdMature, workHtTeen, workNaruto)

    @Test
    fun andAcrossFandomAndRating() {
        val criteria = LibraryFilterCriteria(
            fandomKeys = setOf("hp"),
            displayTags = mapOf(DisplayTagGroups.RATING to setOf("Mature")),
        )
        assertThat(filterWorksByCriteria(all, criteria).map { it.id }).containsExactly(1L)
    }

    @Test
    fun orWithinPairings() {
        val criteria = LibraryFilterCriteria(
            pairingKeys = setOf("harry-potter/draco-malfoy", "harry-potter/tom-riddle"),
        )
        assertThat(filterWorksByCriteria(all, criteria).map { it.id })
            .containsExactly(1L, 2L)
    }

    @Test
    fun slashPairingKeyFiltersWithoutCrash() {
        val criteria = LibraryFilterCriteria(pairingKeys = setOf("harry-potter/tom-riddle"))
        assertThat(filterWorksByCriteria(all, criteria).map { it.id }).containsExactly(2L)
    }

    @Test
    fun encodeDecodeRoundTrip() {
        val criteria = LibraryFilterCriteria(
            fandomKeys = setOf("hp"),
            pairingKeys = setOf("harry-potter/draco-malfoy"),
            displayTags = mapOf(DisplayTagGroups.RATING to setOf("Mature")),
        )
        assertThat(LibraryFilterCriteria.decode(criteria.encode())).isEqualTo(criteria)
    }

    @Test
    fun matchesPartialQueryOnRuAndEn() {
        assertThat(matchesPartialQuery("поттер", hp.displayName, hp.displayRu)).isTrue()
        assertThat(matchesPartialQuery("harry", hp.displayName, hp.displayRu)).isTrue()
        assertThat(matchesPartialQuery("zzz", hp.displayName, hp.displayRu)).isFalse()
        assertThat(matchesPartialQuery("  ", hp.displayName)).isTrue()
    }

    @Test
    fun preservingTranslationKeepsRuOnReimport() {
        val existing = workHdMature.copy(
            titleRu = "Заголовок",
            summaryRu = "Описание",
            translationStatus = TranslationStatus.COMPLETE,
            fandoms = listOf(hp),
            pairings = listOf(hd.copy(displayRu = "Гарри/Драко")),
            displayTags = listOf(DisplayTag(DisplayTagGroups.RATING, "Mature", "Для взрослых")),
        )
        val incoming = workHdMature.copy(
            title = "HD Mature Updated",
            titleRu = null,
            summaryRu = null,
            translationStatus = TranslationStatus.NONE,
            fandoms = listOf(hp.copy(displayRu = null)),
            pairings = listOf(hd.copy(displayRu = null)),
            displayTags = listOf(DisplayTag(DisplayTagGroups.RATING, "Mature", null)),
        )
        val merged = incoming.preservingTranslationFrom(existing)
        assertThat(merged.titleRu).isEqualTo("Заголовок")
        assertThat(merged.summaryRu).isEqualTo("Описание")
        assertThat(merged.translationStatus).isEqualTo(TranslationStatus.COMPLETE)
        assertThat(merged.fandoms.first().displayRu).isEqualTo("Гарри Поттер")
        assertThat(merged.pairings.first().displayRu).isEqualTo("Гарри/Драко")
        assertThat(merged.displayTags.first().valueRu).isEqualTo("Для взрослых")
        assertThat(merged.displayTags.first().value).isEqualTo("Mature")
    }
}
