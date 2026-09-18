package com.fandomreader.domain.model

enum class WorkSource {
    AO3,
    FICBOOK,
    OTHER,
}

enum class Shelf {
    FANFICTION,
    OTHER,
}

enum class RelationshipType {
    ROMANTIC,
    PLATONIC,
    OTHER,
}

enum class TranslationStatus {
    NONE,
    PENDING,
    PARTIAL,
    COMPLETE,
    FAILED,
}

data class Fandom(
    val id: Long = 0,
    val canonicalKey: String,
    val displayName: String,
    val displayRu: String? = null,
)

data class Pairing(
    val id: Long = 0,
    val canonicalKey: String,
    val displayName: String,
    val displayRu: String? = null,
    val type: RelationshipType = RelationshipType.OTHER,
)

/** Display-only portal tag; not used for library filter keys. Identity is (group, value). */
data class DisplayTag(
    val group: String,
    val value: String,
    val valueRu: String? = null,
) {
    fun displayLabel(): String = valueRu?.takeIf { it.isNotBlank() } ?: value
}

object DisplayTagGroups {
    const val RATING = "rating"
    const val WARNINGS = "warnings"
    const val CATEGORY = "category"
    const val CHARACTERS = "characters"
    const val ADDITIONAL = "additional"
    const val GENRE = "genre"
    const val SIZE = "size"
}

data class Work(
    val id: Long = 0,
    val source: WorkSource,
    val remoteId: String?,
    val sourceUrl: String?,
    val title: String,
    val titleRu: String? = null,
    val author: String? = null,
    val summary: String? = null,
    val summaryRu: String? = null,
    val language: String? = null,
    val localPath: String,
    val format: String,
    val shelf: Shelf,
    val translationStatus: TranslationStatus = TranslationStatus.NONE,
    val fandoms: List<Fandom> = emptyList(),
    val pairings: List<Pairing> = emptyList(),
    val displayTags: List<DisplayTag> = emptyList(),
    val readingProgressChapterIndex: Int = 0,
    val readingProgressOffset: Int = 0,
    /** Epoch millis of last successful reader open; 0 = never opened. */
    val lastOpenedAt: Long = 0L,
    /** On-disk size of [localPath] at import/scan time; 0 = unknown. */
    val fileSizeBytes: Long = 0L,
    /** Stable id of the original on-device file (path|size) for rescan dedup. */
    val sourceFingerprint: String? = null,
)

data class ChapterContent(
    val index: Int,
    val title: String?,
    val text: String,
    val textRu: String? = null,
)
