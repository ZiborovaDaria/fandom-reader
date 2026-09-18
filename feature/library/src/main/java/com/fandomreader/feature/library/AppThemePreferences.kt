package com.fandomreader.feature.library

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fandomreader.ui.theme.AppThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appThemePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_theme_preferences",
)

@Singleton
class AppThemePreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.appThemePreferencesDataStore

    val themeMode: Flow<AppThemeMode> = dataStore.data.map { prefs ->
        prefs[Keys.themeMode]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
            ?: AppThemeMode.System
    }

    suspend fun setThemeMode(value: AppThemeMode) {
        dataStore.edit { it[Keys.themeMode] = value.name }
    }

    private object Keys {
        val themeMode = stringPreferencesKey("app_theme_mode")
    }
}
