package com.fandomreader.feature.translation

interface TranslationProvider {
    suspend fun translateBatch(
        segments: List<String>,
        sourceLang: String = "en",
        targetLang: String = "ru",
    ): List<String>

    /**
     * Soft budget for paragraph-aligned chapter chunks (characters, not tokens).
     * Providers may lower this after falling back to a weaker / smaller-context model.
     */
    fun preferredChunkChars(): Int = DEFAULT_CHUNK_CHARS

    companion object {
        /** ~25k tokens at ~4 chars/token — well under 65k output limit with EN→RU headroom. */
        const val DEFAULT_CHUNK_CHARS = 100_000
    }
}
