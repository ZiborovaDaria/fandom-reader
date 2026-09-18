package com.fandomreader.source.api

import com.fandomreader.domain.model.WorkSource
import com.fandomreader.source.FixturePaths
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OtherMetaParserTest {
    @Test
    fun supportsOtherOnly() {
        assertThat(OtherMetaParser().supports(SourceKind.OTHER)).isTrue()
        assertThat(OtherMetaParser().supports(SourceKind.AO3)).isFalse()
    }

    @Test
    fun parsesBasicEpubTitleFromOpf() {
        val file = FixturePaths.resolve("ao3", "Next_Best_Thing.epub")
        val meta = OtherMetaParser().parse(file)
        assertThat(meta.source).isEqualTo(WorkSource.OTHER)
        assertThat(meta.title).isEqualTo("Next Best Thing")
        assertThat(meta.fandoms).isEmpty()
        assertThat(meta.pairings).isEmpty()
    }
}
