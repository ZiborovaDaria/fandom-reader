package com.fandomreader.source.ficbook

import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.source.api.ParsedWorkMeta
import com.fandomreader.source.api.SourceKind
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class FicbookEpubParserTest {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    @Test
    fun supportsFicbook() {
        assertThat(FicbookEpubParser().supports(SourceKind.FICBOOK)).isTrue()
        assertThat(FicbookEpubParser().supports(SourceKind.AO3)).isFalse()
    }

    @Test
    fun parsesPotterFicbookEpub() {
        val file = FixturePaths.resolve("ficbook", "Potter-kotoryj-sovsem-ne-Potter-1.epub")
        val meta = FicbookEpubParser().parse(file)

        assertThat(meta.source).isEqualTo(WorkSource.FICBOOK)
        assertThat(meta.title).contains("Поттер")
        assertThat(meta.author).isEqualTo("katya_briss")
        assertThat(meta.remoteId).isEqualTo("8307205")
        assertThat(meta.sourceUrl).isEqualTo("https://ficbook.net/readfic/8307205")
        assertThat(meta.fandoms).isNotEmpty()
        assertThat(meta.pairings).isNotEmpty()
        assertThat(meta.pairings.any { it.type == RelationshipType.ROMANTIC }).isTrue()
        assertThat(meta.summary).isNotNull()
        assertThat(meta.summary!!).contains("Поттер")

        assertGolden(meta, "ficbook-potter.meta.json")
    }

    private fun assertGolden(meta: ParsedWorkMeta, goldenName: String) {
        val goldenFile = FixturePaths.resolve("golden", goldenName)
        val actual = json.encodeToString(GoldenMeta.from(meta))
        val update = System.getProperty("fandom.updateGolden") == "true" ||
            System.getenv("FANDOM_UPDATE_GOLDEN") == "true"
        if (!goldenFile.exists() || update) {
            goldenFile.parentFile.mkdirs()
            goldenFile.writeText(actual + "\n")
        }
        val expected = goldenFile.readText().trim()
        assertThat(actual.trim()).isEqualTo(expected)
    }
}

@Serializable
data class GoldenMeta(
    val source: String,
    val remoteId: String?,
    val sourceUrl: String?,
    val title: String,
    val author: String?,
    val summary: String?,
    val language: String?,
    val format: String,
    val fandoms: List<GoldenFandom>,
    val pairings: List<GoldenPairing>,
    val displayTags: List<GoldenDisplayTag> = emptyList(),
) {
    companion object {
        fun from(meta: ParsedWorkMeta) = GoldenMeta(
            source = meta.source.name,
            remoteId = meta.remoteId,
            sourceUrl = meta.sourceUrl,
            title = meta.title,
            author = meta.author,
            summary = meta.summary,
            language = meta.language,
            format = meta.format,
            fandoms = meta.fandoms.map { GoldenFandom(it.canonicalKey, it.displayName) },
            pairings = meta.pairings.map { GoldenPairing(it.canonicalKey, it.displayName, it.type.name) },
            displayTags = meta.displayTags.map { GoldenDisplayTag(it.group, it.value) },
        )
    }
}

@Serializable
data class GoldenFandom(val canonicalKey: String, val displayName: String)

@Serializable
data class GoldenPairing(val canonicalKey: String, val displayName: String, val type: String)

@Serializable
data class GoldenDisplayTag(val group: String, val value: String)
