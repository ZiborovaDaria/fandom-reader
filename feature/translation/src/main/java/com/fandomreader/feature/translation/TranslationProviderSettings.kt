package com.fandomreader.feature.translation

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Live neural providers selectable in Settings. Placeholder slots are storage-only. */
enum class LiveTranslationProviderId {
    Gemini,
    Cursor,
}

enum class TranslationKeySlot(
    val prefsKey: String,
    val label: String,
    val temporary: Boolean = false,
    val live: Boolean = false,
) {
    Gemini("gemini_api_key", "Gemini", live = true),
    Cursor("cursor_api_key", "Cursor (временно)", temporary = true, live = true),
    OpenAi("openai_api_key", "OpenAI (скоро)"),
    Anthropic("anthropic_api_key", "Anthropic (скоро)"),
}

@Singleton
class TranslationProviderSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ).also { migrateLegacyGeminiKey(it) }
    }

    var selectedProvider: LiveTranslationProviderId
        get() = prefs.getString(KEY_SELECTED, null)
            ?.let { runCatching { LiveTranslationProviderId.valueOf(it) }.getOrNull() }
            ?: LiveTranslationProviderId.Gemini
        set(value) = prefs.edit().putString(KEY_SELECTED, value.name).apply()

    fun keyFor(slot: TranslationKeySlot): String =
        prefs.getString(slot.prefsKey, "").orEmpty()

    fun setKey(slot: TranslationKeySlot, value: String) {
        prefs.edit().putString(slot.prefsKey, value.trim()).apply()
    }

    fun clearKey(slot: TranslationKeySlot) {
        prefs.edit().putString(slot.prefsKey, "").apply()
    }

    fun hasKey(slot: TranslationKeySlot): Boolean = keyFor(slot).isNotBlank()

    val geminiApiKey: String
        get() = keyFor(TranslationKeySlot.Gemini)

    val cursorApiKey: String
        get() = keyFor(TranslationKeySlot.Cursor)

    var cursorBaseUrl: String
        get() = prefs.getString(KEY_CURSOR_BASE, CursorProvider.DEFAULT_BASE_URL)
            ?.takeIf { it.isNotBlank() }
            ?: CursorProvider.DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_CURSOR_BASE, value.trim()).apply()

    var cursorModel: String
        get() = prefs.getString(KEY_CURSOR_MODEL, CursorProvider.DEFAULT_MODEL)
            ?.takeIf { it.isNotBlank() }
            ?: CursorProvider.DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_CURSOR_MODEL, value.trim()).apply()

    val hasKeyForSelectedProvider: Boolean
        get() = when (selectedProvider) {
            LiveTranslationProviderId.Gemini -> hasKey(TranslationKeySlot.Gemini)
            LiveTranslationProviderId.Cursor -> hasKey(TranslationKeySlot.Cursor)
        }

    companion object {
        private const val PREFS_NAME = "gemini_secure_prefs"
        private const val LEGACY_GEMINI_KEY = "api_key"
        private const val KEY_SELECTED = "selected_provider"
        private const val KEY_CURSOR_BASE = "cursor_base_url"
        private const val KEY_CURSOR_MODEL = "cursor_model"

        private fun migrateLegacyGeminiKey(prefs: SharedPreferences) {
            val legacy = prefs.getString(LEGACY_GEMINI_KEY, "").orEmpty()
            val modern = prefs.getString(TranslationKeySlot.Gemini.prefsKey, "").orEmpty()
            if (legacy.isNotBlank() && modern.isBlank()) {
                prefs.edit()
                    .putString(TranslationKeySlot.Gemini.prefsKey, legacy)
                    .remove(LEGACY_GEMINI_KEY)
                    .apply()
            }
        }
    }
}

/** Backward-compatible Gemini-only facade used by [GeminiProvider]. */
@Singleton
class GeminiSettings @Inject constructor(
    private val settings: TranslationProviderSettings,
) {
    var apiKey: String
        get() = settings.geminiApiKey
        set(value) = settings.setKey(TranslationKeySlot.Gemini, value)

    val hasApiKey: Boolean
        get() = settings.hasKey(TranslationKeySlot.Gemini)

    fun clearApiKey() {
        settings.clearKey(TranslationKeySlot.Gemini)
    }
}
