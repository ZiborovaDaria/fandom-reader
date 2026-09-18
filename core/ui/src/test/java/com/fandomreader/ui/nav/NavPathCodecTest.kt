package com.fandomreader.ui.nav

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NavPathCodecTest {
    @Test
    fun roundTrip_preservesSlashInPairingKey() {
        val key = "harry-potter/tom-riddle"
        val encoded = NavPathCodec.encode(key)
        assertThat(encoded).doesNotContain("/")
        assertThat(encoded).contains("%2F")
        assertThat(NavPathCodec.decode(encoded)).isEqualTo(key)
    }

    @Test
    fun roundTrip_preservesSpacesAndSlash() {
        val key = "harry potter/tom riddle"
        val encoded = NavPathCodec.encode(key)
        assertThat(encoded).doesNotContain(" ")
        assertThat(encoded).doesNotContain("/")
        assertThat(NavPathCodec.decode(encoded)).isEqualTo(key)
    }

    @Test
    fun encode_doesNotLeaveSlashUnescaped() {
        // Uri.encode(s) without null allow leaves '/' intact — we must not use that.
        assertThat(NavPathCodec.encode("a/b")).isEqualTo("a%2Fb")
    }
}
