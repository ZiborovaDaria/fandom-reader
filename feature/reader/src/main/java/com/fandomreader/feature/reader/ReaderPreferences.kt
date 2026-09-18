package com.fandomreader.feature.reader

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fandomreader.ui.theme.ReaderSurfaceTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ReaderFontFamilyOption {
    Serif,
    Sans,
    Mono,
}

data class ReaderPreferences(
    val fontFamily: ReaderFontFamilyOption = Defaults.fontFamily,
    val fontSizeSp: Int = Defaults.fontSizeSp,
    val lineHeightMult: Float = Defaults.lineHeightMult,
    val surfaceTheme: ReaderSurfaceTheme = Defaults.surfaceTheme,
) {
    object Defaults {
        val fontFamily = ReaderFontFamilyOption.Serif
        const val fontSizeSp = 18
        const val lineHeightMult = 1.55f
        val surfaceTheme = ReaderSurfaceTheme.Paper
        const val minFontSizeSp = 14
        const val maxFontSizeSp = 28
    }
}

private val Context.readerPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "reader_preferences",
)

@Singleton
class ReaderPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.readerPreferencesDataStore

    val preferences: Flow<ReaderPreferences> = dataStore.data.map { prefs ->
        ReaderPreferences(
            fontFamily = prefs[Keys.fontFamily]?.let {
                runCatching { ReaderFontFamilyOption.valueOf(it) }.getOrNull()
            } ?: ReaderPreferences.Defaults.fontFamily,
            fontSizeSp = (prefs[Keys.fontSizeSp] ?: ReaderPreferences.Defaults.fontSizeSp)
                .coerceIn(ReaderPreferences.Defaults.minFontSizeSp, ReaderPreferences.Defaults.maxFontSizeSp),
            lineHeightMult = prefs[Keys.lineHeightMult] ?: ReaderPreferences.Defaults.lineHeightMult,
            surfaceTheme = prefs[Keys.surfaceTheme]?.let {
                runCatching { ReaderSurfaceTheme.valueOf(it) }.getOrNull()
            } ?: ReaderPreferences.Defaults.surfaceTheme,
        )
    }

    suspend fun setFontFamily(value: ReaderFontFamilyOption) {
        dataStore.edit { it[Keys.fontFamily] = value.name }
    }

    suspend fun setFontSizeSp(value: Int) {
        dataStore.edit {
            it[Keys.fontSizeSp] = value.coerceIn(
                ReaderPreferences.Defaults.minFontSizeSp,
                ReaderPreferences.Defaults.maxFontSizeSp,
            )
        }
    }

    suspend fun setLineHeightMult(value: Float) {
        dataStore.edit { it[Keys.lineHeightMult] = value }
    }

    suspend fun setSurfaceTheme(value: ReaderSurfaceTheme) {
        dataStore.edit { it[Keys.surfaceTheme] = value.name }
    }

    private object Keys {
        val fontFamily = stringPreferencesKey("font_family")
        val fontSizeSp = intPreferencesKey("font_size_sp")
        val lineHeightMult = floatPreferencesKey("line_height_mult")
        val surfaceTheme = stringPreferencesKey("surface_theme")
    }
}
