package com.fandomreader.feature.reader

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.fandomreader.ui.theme.ReaderSurfaceTheme
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
class ReaderPreferencesRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun defaultsMatchSpecPaperBaseline() {
        assertThat(ReaderPreferences.Defaults.surfaceTheme).isEqualTo(ReaderSurfaceTheme.Paper)
        assertThat(ReaderPreferences.Defaults.fontFamily).isEqualTo(ReaderFontFamilyOption.Serif)
        assertThat(ReaderPreferences.Defaults.fontSizeSp).isEqualTo(18)
        assertThat(ReaderPreferences.Defaults.lineHeightMult).isWithin(0.01f).of(1.55f)
    }

    @Test
    fun dataStoreWriteThenReadPersistsValues() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher + Job())
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tmp.root, "reader.preferences_pb") },
        )
        val fontSizeKey = intPreferencesKey("font_size_sp")
        val themeKey = stringPreferencesKey("surface_theme")

        assertThat(store.data.first()[fontSizeKey]).isNull()

        store.edit {
            it[fontSizeKey] = 22
            it[themeKey] = ReaderSurfaceTheme.Night.name
        }

        val prefs = store.data.first()
        assertThat(prefs[fontSizeKey]).isEqualTo(22)
        assertThat(prefs[themeKey]).isEqualTo("Night")
        scope.cancel()
    }
}
