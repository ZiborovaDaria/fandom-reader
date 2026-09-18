package com.fandomreader.feature.translation

/**
 * Thrown when a neural translation is requested but no Gemini API key is configured.
 * UI should guide the user to Settings instead of treating Fake placeholders as success.
 */
class MissingApiKeyException : IllegalStateException("Gemini API key is not configured")
