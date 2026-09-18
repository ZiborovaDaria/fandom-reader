package com.fandomreader.data.db

import com.fandomreader.domain.model.DisplayTag
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DisplayTagsCodecTest {
    @Test
    fun decodeLegacyWithoutValueRu() {
        val tags = DisplayTagsCodec.decode("rating\tMature\ncharacters\tHarry Potter")
        assertThat(tags).hasSize(2)
        assertThat(tags[0]).isEqualTo(DisplayTag("rating", "Mature", null))
        assertThat(tags[1]).isEqualTo(DisplayTag("characters", "Harry Potter", null))
    }

    @Test
    fun roundTripWithValueRu() {
        val original = listOf(
            DisplayTag("rating", "Mature", "Для взрослых"),
            DisplayTag("characters", "Harry", null),
        )
        val encoded = DisplayTagsCodec.encode(original)
        val decoded = DisplayTagsCodec.decode(encoded)
        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun encodeNullForEmpty() {
        assertThat(DisplayTagsCodec.encode(emptyList())).isNull()
        assertThat(DisplayTagsCodec.decode(null)).isEmpty()
    }
}
