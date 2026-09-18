package com.fandomreader.feature.library

import com.fandomreader.domain.model.DisplayTag
import com.fandomreader.domain.model.DisplayTagGroups
import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WorkMetaTagChipsTest {
    @Test
    fun buildsSectionsFromFandomPairingAndDisplayTags() {
        val work = Work(
            source = WorkSource.AO3,
            remoteId = "1",
            sourceUrl = null,
            title = "T",
            localPath = "/x",
            format = "epub",
            shelf = Shelf.FANFICTION,
            fandoms = listOf(Fandom(canonicalKey = "hp", displayName = "Harry Potter")),
            pairings = listOf(
                Pairing(
                    canonicalKey = "h/d",
                    displayName = "Harry/Draco",
                    type = RelationshipType.ROMANTIC,
                ),
            ),
            displayTags = listOf(
                DisplayTag(DisplayTagGroups.RATING, "Mature"),
                DisplayTag(DisplayTagGroups.CHARACTERS, "Harry"),
            ),
        )
        val chips = buildMetaTagChips(work)
        assertThat(chips.map { it.first }).containsExactly(
            "Фэндом",
            "Пейринг",
            "Рейтинг",
            "Персонажи",
        ).inOrder()
        assertThat(chips.first { it.first == "Рейтинг" }.second).containsExactly("Mature")
    }

    @Test
    fun prefersValueRuForDisplayTags() {
        val work = Work(
            source = WorkSource.AO3,
            remoteId = "2",
            sourceUrl = null,
            title = "T",
            localPath = "/x",
            format = "epub",
            shelf = Shelf.FANFICTION,
            displayTags = listOf(
                DisplayTag(DisplayTagGroups.RATING, "Mature", "Для взрослых"),
            ),
        )
        val chips = buildMetaTagChips(work)
        assertThat(chips.first { it.first == "Рейтинг" }.second).containsExactly("Для взрослых")
    }

    @Test
    fun emptyTagsShowNoSections() {
        val work = Work(
            source = WorkSource.OTHER,
            remoteId = null,
            sourceUrl = null,
            title = "T",
            localPath = "/x",
            format = "fb2",
            shelf = Shelf.OTHER,
        )
        assertThat(buildMetaTagChips(work)).isEmpty()
    }
}
