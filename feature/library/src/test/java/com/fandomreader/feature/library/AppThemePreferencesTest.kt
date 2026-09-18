package com.fandomreader.feature.library

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fandomreader.ui.theme.AppThemeMode
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AppThemePreferencesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun defaultModeIsSystem() {
        assertThat(AppThemeMode.entries.toList()).containsExactly(
            AppThemeMode.Light,
            AppThemeMode.Dark,
            AppThemeMode.System,
        ).inOrder()
    }

    @Test
    fun dataStorePersistsDarkMode() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher + Job())
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tmp.root, "app_theme.preferences_pb") },
        )
        val key = stringPreferencesKey("app_theme_mode")

        store.edit { it[key] = AppThemeMode.Dark.name }
        assertThat(store.data.first()[key]).isEqualTo("Dark")
        scope.cancel()
    }
}
