package com.fandomreader.feature.translation

class FakeTranslationProvider : TranslationProvider {
    override suspend fun translateBatch(
        segments: List<String>,
        sourceLang: String,
        targetLang: String,
    ): List<String> = segments.map { "[$targetLang] $it" }
}
