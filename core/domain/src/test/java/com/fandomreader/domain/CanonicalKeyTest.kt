package com.fandomreader.domain

import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.RelationshipType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CanonicalKeyTest {
    @Test
    fun fandomsWithSameCanonicalKey_areEqualForFiltering() {
        val a = Fandom(canonicalKey = "harry-potter", displayName = "Harry Potter - J. K. Rowling")
        val b = Fandom(
            canonicalKey = "harry-potter",
            displayName = "Harry Potter - J. K. Rowling",
            displayRu = "Гарри Поттер",
        )
        assertThat(a.canonicalKey).isEqualTo(b.canonicalKey)
        assertThat(setOf(a.canonicalKey, b.canonicalKey)).hasSize(1)
    }

    @Test
    fun pairingCanonicalKey_ignoresDisplayRuDuplicates() {
        val en = Pairing(
            canonicalKey = "harry-potter/tom-riddle",
            displayName = "Harry Potter/Tom Riddle | Voldemort",
            type = RelationshipType.ROMANTIC,
        )
        val ru = en.copy(displayRu = "Гарри Поттер/Том Риддл")
        assertThat(en.canonicalKey).isEqualTo(ru.canonicalKey)
    }
}
