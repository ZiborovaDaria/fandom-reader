package com.fandomreader.feature.library

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fandomreader.domain.model.SortDirection
import com.fandomreader.domain.model.WorkSort
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class LibrarySortPreferences(
    val sort: WorkSort = Defaults.sort,
    val direction: SortDirection = Defaults.direction,
) {
    object Defaults {
        val sort = WorkSort.LastOpened
        val direction = SortDirection.Descending
    }
}

private val Context.librarySortDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "library_sort_preferences",
)

@Singleton
class LibrarySortPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.librarySortDataStore

    val preferences: Flow<LibrarySortPreferences> = dataStore.data.map { prefs ->
        LibrarySortPreferences(
            sort = prefs[Keys.sort]?.let {
                runCatching { WorkSort.valueOf(it) }.getOrNull()
            } ?: LibrarySortPreferences.Defaults.sort,
            direction = prefs[Keys.direction]?.let {
                runCatching { SortDirection.valueOf(it) }.getOrNull()
            } ?: LibrarySortPreferences.Defaults.direction,
        )
    }

    suspend fun setSort(sort: WorkSort) {
        dataStore.edit { it[Keys.sort] = sort.name }
    }

    suspend fun setDirection(direction: SortDirection) {
        dataStore.edit { it[Keys.direction] = direction.name }
    }

    suspend fun setSortAndDirection(sort: WorkSort, direction: SortDirection) {
        dataStore.edit {
            it[Keys.sort] = sort.name
            it[Keys.direction] = direction.name
        }
    }

    private object Keys {
        val sort = stringPreferencesKey("work_sort")
        val direction = stringPreferencesKey("sort_direction")
    }
}
