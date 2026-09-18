package com.fandomreader.source.ao3

import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.source.api.SourceKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Ao3Fb2ParserTest {
    @Test
    fun supportsAo3() {
        assertThat(Ao3Fb2Parser().supports(SourceKind.AO3)).isTrue()
    }

    @Test
    fun recoversDamagedFb2WithoutThrowing() {
        val file = FixturePaths.resolve("ao3", "damaged-fb2", "zovi-menya-aid.fb2")
        val meta = Ao3Fb2Parser().parse(file)

        assertThat(meta.source).isEqualTo(WorkSource.AO3)
        assertThat(meta.format).isEqualTo("fb2")
        assertThat(meta.title).isNotEqualTo("Unknown")
        assertThat(meta.title).isNotEmpty()
        assertThat(meta.fandoms).isNotEmpty()
        assertThat(meta.pairings).isNotEmpty()
        assertThat(meta.pairings.any { it.type == RelationshipType.ROMANTIC }).isTrue()
        assertThat(meta.summary).isNotNull()
    }

    @Test
    fun recoversSecondDamagedFb2BestEffort() {
        val file = FixturePaths.resolve("ao3", "damaged-fb2", "snova-no-luchshe.fb2")
        val meta = Ao3Fb2Parser().parse(file)

        assertThat(meta.source).isEqualTo(WorkSource.AO3)
        assertThat(meta.title).isNotEqualTo("Unknown")
        assertThat(meta.pairings.any { it.displayName.contains("/") }).isTrue()
    }

    @Test
    fun metaParserRoutesByExtension() {
        val epub = FixturePaths.resolve("ao3", "Next_Best_Thing.epub")
        val fb2 = FixturePaths.resolve("ao3", "damaged-fb2", "zovi-menya-aid.fb2")
        val parser = Ao3MetaParser()
        assertThat(parser.parse(epub).format).isEqualTo("epub")
        assertThat(parser.parse(fb2).format).isEqualTo("fb2")
    }
}
