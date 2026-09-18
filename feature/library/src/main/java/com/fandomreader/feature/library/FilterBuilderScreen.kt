package com.fandomreader.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fandomreader.domain.model.DisplayTag
import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.LibraryFilterCriteria
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.matchesPartialQuery

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBuilderRoute(
    onApply: (LibraryFilterCriteria) -> Unit,
    onBack: () -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val fandoms by vm.fandoms.collectAsStateWithLifecycle()
    val romantic by vm.allRomantic.collectAsStateWithLifecycle()
    val platonic by vm.allPlatonic.collectAsStateWithLifecycle()
    val displayTags by vm.filterDisplayTags.collectAsStateWithLifecycle()
    val pairings = remember(romantic, platonic) { romantic + platonic }

    var selectedFandoms by remember { mutableStateOf(setOf<String>()) }
    var selectedPairings by remember { mutableStateOf(setOf<String>()) }
    var selectedTags by remember { mutableStateOf(mapOf<String, Set<String>>()) }
    var pickerQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(FilterTab.FANDOMS) }

    val catalogTagGroups = remember(displayTags) {
        displayTags.map { it.group.lowercase() }.toSet()
    }
    val selectedTagGroups = remember(selectedTags) {
        selectedTags.filterValues { it.isNotEmpty() }.keys.map { it.lowercase() }.toSet()
    }
    val availableTabs = remember(fandoms, pairings, catalogTagGroups, selectedTagGroups) {
        buildFilterAvailableTabs(
            fandoms = fandoms,
            pairings = pairings,
            catalogTagGroups = catalogTagGroups,
            selectedTagGroups = selectedTagGroups,
        )
    }

    LaunchedEffect(availableTabs) {
        selectedTab = resolveFilterTab(selectedTab, availableTabs)
    }

    val selectedTabIndex = availableTabs.indexOf(selectedTab).coerceAtLeast(0)
    val tab = selectedTab
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Фильтры") },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("Назад") }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                selectedFandoms = emptySet()
                                selectedPairings = emptySet()
                                selectedTags = emptyMap()
                                pickerQuery = ""
                            },
                        ) { Text("Сбросить") }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Button(
                        onClick = {
                            val hasSelection = selectedFandoms.isNotEmpty() ||
                                selectedPairings.isNotEmpty() ||
                                selectedTags.values.any { it.isNotEmpty() }
                            if (!hasSelection) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Выберите хотя бы один критерий")
                                }
                            } else {
                                onApply(
                                    LibraryFilterCriteria(
                                        fandomKeys = selectedFandoms,
                                        pairingKeys = selectedPairings,
                                        displayTags = selectedTags.filterValues { it.isNotEmpty() },
                                    ),
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text("Показать результаты")
                    }
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
                    availableTabs.forEach { t ->
                        Tab(
                            selected = t == selectedTab,
                            onClick = {
                                selectedTab = t
                                pickerQuery = ""
                            },
                            text = { Text(t.title) },
                        )
                    }
                }
                OutlinedTextField(
                    value = pickerQuery,
                    onValueChange = { pickerQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text("Поиск") },
                )
                when (tab) {
                    FilterTab.FANDOMS -> FandomPickerList(
                        fandoms = fandoms,
                        query = pickerQuery,
                        selected = selectedFandoms,
                        onToggle = { key ->
                            selectedFandoms = selectedFandoms.toggle(key)
                        },
                    )
                    FilterTab.PAIRINGS -> PairingPickerList(
                        pairings = pairings,
                        query = pickerQuery,
                        selected = selectedPairings,
                        onToggle = { key ->
                            selectedPairings = selectedPairings.toggle(key)
                        },
                    )
                    else -> {
                        val group = tab.displayTagGroup()!!
                        DisplayTagPickerList(
                            tags = displayTags.filter { it.group.equals(group, ignoreCase = true) }
                                .distinctBy { it.value.lowercase() }
                                .sortedBy { it.displayLabel().lowercase() },
                            query = pickerQuery,
                            selected = selectedTags[group].orEmpty(),
                            onToggle = { value ->
                                val current = selectedTags[group].orEmpty()
                                selectedTags = selectedTags + (group to current.toggle(value))
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterResultsRoute(
    criteria: LibraryFilterCriteria,
    onOpenWork: (Long) -> Unit,
    onBack: () -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val works by vm.worksMatching(criteria).collectAsStateWithLifecycle()
    val sortPrefs by vm.sortPreferences.collectAsStateWithLifecycle()
    val restoreWorkId by vm.restoreWorkId.collectAsStateWithLifecycle()
    val openWork: (Long) -> Unit = { id ->
        vm.rememberOpenedWork(id)
        onOpenWork(id)
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Результаты (${works.size})") },
                    navigationIcon = {
                        TextButton(onClick = onBack) { Text("Назад") }
                    },
                )
            },
        ) { padding ->
            if (works.isEmpty()) {
                Text(
                    "Нет подходящих книг.",
                    modifier = Modifier
                        .padding(padding)
                        .padding(24.dp),
                )
            } else {
                Column(Modifier.padding(padding).fillMaxSize()) {
                    WorkListSortBar(
                        prefs = sortPrefs,
                        onSort = vm::setSort,
                        onDirection = vm::setSortDirection,
                    )
                    WorkList(
                        works = works,
                        onOpenWork = openWork,
                        onTranslate = vm::startTranslation,
                        restoreWorkId = restoreWorkId,
                        onRestored = vm::clearRestoreWorkId,
                    )
                }
            }
        }
    }
}

@Composable
private fun FandomPickerList(
    fandoms: List<Fandom>,
    query: String,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val filtered = fandoms.filter {
        matchesPartialQuery(query, it.displayName, it.displayRu)
    }
    if (filtered.isEmpty()) {
        Text("Ничего не найдено.", Modifier.padding(24.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(filtered, key = { it.canonicalKey }) { fandom ->
            CheckRow(
                label = fandom.displayRu ?: fandom.displayName,
                checked = fandom.canonicalKey in selected,
                onToggle = { onToggle(fandom.canonicalKey) },
            )
        }
    }
}

@Composable
private fun PairingPickerList(
    pairings: List<Pairing>,
    query: String,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val filtered = pairings
        .distinctBy { it.canonicalKey }
        .filter { matchesPartialQuery(query, it.displayName, it.displayRu) }
    if (filtered.isEmpty()) {
        Text("Ничего не найдено.", Modifier.padding(24.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(filtered, key = { it.canonicalKey }) { pairing ->
            CheckRow(
                label = pairing.displayRu ?: pairing.displayName,
                checked = pairing.canonicalKey in selected,
                onToggle = { onToggle(pairing.canonicalKey) },
            )
        }
    }
}

@Composable
private fun DisplayTagPickerList(
    tags: List<DisplayTag>,
    query: String,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    val filtered = tags.filter { matchesPartialQuery(query, it.value, it.valueRu) }
    if (filtered.isEmpty()) {
        Text("Ничего не найдено.", Modifier.padding(24.dp))
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(filtered, key = { it.value.lowercase() }) { tag ->
            CheckRow(
                label = tag.displayLabel(),
                checked = selected.any { it.equals(tag.value, ignoreCase = true) },
                onToggle = { onToggle(tag.value) },
            )
        }
    }
}

@Composable
private fun CheckRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this) this - item else this + item
