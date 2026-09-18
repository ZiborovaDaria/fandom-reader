package com.fandomreader.source.ao3

import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.source.api.ParsedWorkMeta
import com.fandomreader.source.api.SourceKind
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class Ao3EpubParserTest {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    @Test
    fun supportsAo3() {
        assertThat(Ao3EpubParser().supports(SourceKind.AO3)).isTrue()
        assertThat(Ao3EpubParser().supports(SourceKind.FICBOOK)).isFalse()
    }

    @Test
    fun parsesNextBestThingEpub() {
        val file = FixturePaths.resolve("ao3", "Next_Best_Thing.epub")
        val meta = Ao3EpubParser().parse(file)

        assertThat(meta.source).isEqualTo(WorkSource.AO3)
        assertThat(meta.title).isEqualTo("Next Best Thing")
        assertThat(meta.author).isEqualTo("KojisApple")
        assertThat(meta.remoteId).isEqualTo("87947496")
        assertThat(meta.sourceUrl).isEqualTo("https://archiveofourown.org/works/87947496")
        assertThat(meta.language).isEqualTo("en")
        assertThat(meta.fandoms).isNotEmpty()
        assertThat(meta.fandoms.any { it.displayName.contains("Harry Potter") }).isTrue()
        assertThat(meta.pairings).isNotEmpty()
        assertThat(meta.pairings.any { it.type == RelationshipType.ROMANTIC && it.displayName.contains("/") }).isTrue()
        assertThat(meta.pairings.any { it.type == RelationshipType.PLATONIC && it.displayName.contains("&") }).isTrue()
        assertThat(
            meta.pairings.none { it.type == RelationshipType.ROMANTIC && it.displayName.contains("&") },
        ).isTrue()
        assertThat(
            meta.pairings.filter { it.type == RelationshipType.PLATONIC }
                .none { it.canonicalKey.contains('/') && !it.displayName.contains('/') },
        ).isTrue()
        assertThat(meta.summary).isNotNull()
        assertThat(meta.summary!!).contains("Thomas Potter")
        assertThat(meta.displayTags.any { it.group == "rating" && it.value == "Mature" }).isTrue()
        assertThat(meta.displayTags.any { it.group == "characters" }).isTrue()
        assertThat(meta.displayTags.any { it.group == "additional" }).isTrue()

        assertGolden(meta, "next-best-thing.meta.json")
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
            fandoms = meta.fandoms.map {
                GoldenFandom(it.canonicalKey, it.displayName)
            },
            pairings = meta.pairings.map {
                GoldenPairing(it.canonicalKey, it.displayName, it.type.name)
            },
            displayTags = meta.displayTags.map {
                GoldenDisplayTag(it.group, it.value)
            },
        )
    }
}

@Serializable
data class GoldenFandom(val canonicalKey: String, val displayName: String)

@Serializable
data class GoldenPairing(val canonicalKey: String, val displayName: String, val type: String)

@Serializable
data class GoldenDisplayTag(val group: String, val value: String)
