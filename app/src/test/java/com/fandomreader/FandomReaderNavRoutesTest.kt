package com.fandomreader

import com.fandomreader.ui.nav.NavPathCodec
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Guards the route shapes used by [FandomReaderNav] so slash-bearing
 * canonical keys stay single path segments.
 */
@RunWith(RobolectricTestRunner::class)
class FandomReaderNavRoutesTest {
    @Test
    fun filteredRoute_keepsPairingKeyAsSingleSegment() {
        val fandomKey = "harry potter - j. k. rowling"
        val pairingKey = "harry potter/tom riddle"
        val route =
            "filtered/${NavPathCodec.encode(fandomKey)}/${NavPathCodec.encode(pairingKey)}"
        val segments = route.split('/')
        assertThat(segments).hasSize(3)
        assertThat(segments[0]).isEqualTo("filtered")
        assertThat(NavPathCodec.decode(segments[1])).isEqualTo(fandomKey)
        assertThat(NavPathCodec.decode(segments[2])).isEqualTo(pairingKey)
    }

    @Test
    fun pairingsRoute_keepsFandomKeyAsSingleSegment() {
        val fandomKey = "harry potter/draco malfoy" // unlikely but slash-safe
        val route = "pairings/${NavPathCodec.encode(fandomKey)}"
        val segments = route.split('/')
        assertThat(segments).hasSize(2)
        assertThat(NavPathCodec.decode(segments[1])).isEqualTo(fandomKey)
    }
}
