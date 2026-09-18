package com.fandomreader.source.api

import com.fandomreader.domain.model.DisplayTag
import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.WorkSource
import java.io.File

enum class SourceKind {
    AO3,
    FICBOOK,
    OTHER,
}

data class ParsedWorkMeta(
    val source: WorkSource,
    val remoteId: String?,
    val sourceUrl: String?,
    val title: String,
    val author: String?,
    val summary: String?,
    val language: String?,
    val fandoms: List<Fandom>,
    val pairings: List<Pairing>,
    val format: String,
    val displayTags: List<DisplayTag> = emptyList(),
)

interface BookClassifier {
    fun classify(file: File): SourceKind
}

interface BookMetaParser {
    fun supports(kind: SourceKind): Boolean
    fun parse(file: File): ParsedWorkMeta
}
