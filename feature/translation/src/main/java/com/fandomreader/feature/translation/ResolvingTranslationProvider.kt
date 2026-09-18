package com.fandomreader.feature.translation

/**
 * Chooses the live neural provider at call time.
 * Throws [MissingApiKeyException] when the selected provider key is blank.
 */
class ResolvingTranslationProvider(
    private val hasKey: () -> Boolean,
    private val activeProvider: () -> TranslationProvider,
) : TranslationProvider {
    constructor(
        settings: TranslationProviderSettings,
        geminiProvider: TranslationProvider,
        cursorProvider: TranslationProvider,
    ) : this(
        hasKey = { settings.hasKeyForSelectedProvider },
        activeProvider = {
            when (settings.selectedProvider) {
                LiveTranslationProviderId.Gemini -> geminiProvider
                LiveTranslationProviderId.Cursor -> cursorProvider
            }
        },
    )

    override fun preferredChunkChars(): Int = activeProvider().preferredChunkChars()

    override suspend fun translateBatch(
        segments: List<String>,
        sourceLang: String,
        targetLang: String,
    ): List<String> {
        if (!hasKey()) {
            throw MissingApiKeyException()
        }
        return activeProvider().translateBatch(segments, sourceLang, targetLang)
    }
}
