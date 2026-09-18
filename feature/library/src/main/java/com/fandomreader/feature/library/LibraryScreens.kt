package com.fandomreader.feature.library

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fandomreader.domain.model.DisplayTag
import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.LibraryFilterCriteria
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.SortDirection
import com.fandomreader.domain.model.TranslationStatus
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSort
import com.fandomreader.domain.model.WorkSource
import com.fandomreader.domain.model.sortWorks
import com.fandomreader.domain.repository.LibraryRepository
import com.fandomreader.feature.reader.loadChapters
import com.fandomreader.feature.translation.MissingApiKeyException
import com.fandomreader.feature.translation.TranslationPipeline
import com.fandomreader.feature.translation.TranslationProviderSettings
import com.fandomreader.source.api.DeviceLibraryScanner
import com.fandomreader.source.api.ImportCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.OutlinedTextField
sealed interface LibraryUiEvent {
    data object NeedApiKey : LibraryUiEvent
    data object NeedStoragePermission : LibraryUiEvent
    data object SuggestSafFallback : LibraryUiEvent
    data class Message(val text: String) : LibraryUiEvent
}

data class ScanProgressUi(
    val inProgress: Boolean = false,
    val done: Int = 0,
    val total: Int = 0,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val translationPipeline: TranslationPipeline,
    private val translationSettings: TranslationProviderSettings,
    private val importCoordinator: ImportCoordinator,
    private val deviceLibraryScanner: DeviceLibraryScanner,
    private val sortPreferencesRepository: LibrarySortPreferencesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _events = MutableSharedFlow<LibraryUiEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val _scanProgress = MutableStateFlow(ScanProgressUi())
    val scanProgress: StateFlow<ScanProgressUi> = _scanProgress.asStateFlow()

    private val _restoreWorkId = MutableStateFlow<Long?>(null)
    val restoreWorkId: StateFlow<Long?> = _restoreWorkId.asStateFlow()

    val sortPreferences: StateFlow<LibrarySortPreferences> =
        sortPreferencesRepository.preferences
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                LibrarySortPreferences(),
            )

    init {
        viewModelScope.launch {
            runCatching { repository.rekeyCollapsedPlatonicPairings() }
        }
    }

    fun setSort(sort: WorkSort) {
        viewModelScope.launch { sortPreferencesRepository.setSort(sort) }
    }

    fun setSortDirection(direction: SortDirection) {
        viewModelScope.launch { sortPreferencesRepository.setDirection(direction) }
    }

    fun rememberOpenedWork(workId: Long) {
        _restoreWorkId.value = workId
    }

    fun clearRestoreWorkId() {
        _restoreWorkId.value = null
    }

    private fun Flow<List<Work>>.sortedByPrefs(): Flow<List<Work>> =
        combine(this, sortPreferencesRepository.preferences) { works, prefs ->
            sortWorks(works, prefs.sort, prefs.direction)
        }

    fun works(shelf: Shelf): StateFlow<List<Work>> =
        repository.observeWorks(shelf).sortedByPrefs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val fandoms: StateFlow<List<Fandom>> =
        repository.observeFandoms()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pairings(fandomKey: String): StateFlow<List<Pairing>> =
        repository.observeRomanticPairings(fandomKey)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun platonic(fandomKey: String): StateFlow<List<Pairing>> =
        repository.observePlatonicRelationships(fandomKey)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allRomantic: StateFlow<List<Pairing>> =
        repository.observeAllRomanticPairings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allPlatonic: StateFlow<List<Pairing>> =
        repository.observeAllPlatonicRelationships()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filterDisplayTags: StateFlow<List<DisplayTag>> =
        repository.observeWorks(Shelf.FANFICTION)
            .map { works ->
                works.flatMap { it.displayTags }
                    .distinctBy { "${it.group.lowercase()}\u0000${it.value.lowercase()}" }
                    .sortedWith(
                        compareBy(
                            { it.group.lowercase() },
                            { it.displayLabel().lowercase() },
                        ),
                    )
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _titleQuery = MutableStateFlow("")
    val titleQuery: StateFlow<String> = _titleQuery.asStateFlow()

    private val _summaryQuery = MutableStateFlow("")
    val summaryQuery: StateFlow<String> = _summaryQuery.asStateFlow()

    private val _searchVisible = MutableStateFlow(false)
    val searchVisible: StateFlow<Boolean> = _searchVisible.asStateFlow()

    fun setTitleQuery(value: String) {
        _titleQuery.value = value
    }

    fun setSummaryQuery(value: String) {
        _summaryQuery.value = value
    }

    fun setSearchVisible(visible: Boolean) {
        _searchVisible.value = visible
    }

    fun toggleSearchVisible() {
        _searchVisible.value = !_searchVisible.value
    }

    fun searchResults(shelf: Shelf): StateFlow<List<Work>> =
        combine(
            repository.observeWorks(shelf),
            _titleQuery,
            _summaryQuery,
            sortPreferencesRepository.preferences,
        ) { works, titleQ, summaryQ, prefs ->
            sortWorks(filterWorksByTitleAndSummary(works, titleQ, summaryQ), prefs.sort, prefs.direction)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun worksByPairing(pairingKey: String): StateFlow<List<Work>> =
        repository.observeWorksByPairing(pairingKey).sortedByPrefs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun worksByDisplayTag(group: String, value: String): StateFlow<List<Work>> =
        repository.observeWorksByDisplayTag(group, value).sortedByPrefs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun worksMatching(criteria: LibraryFilterCriteria): StateFlow<List<Work>> =
        repository.observeWorksMatching(criteria).sortedByPrefs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun filtered(fandomKey: String, pairingKey: String): StateFlow<List<Work>> =
        repository.observeWorksByFandomAndPairing(fandomKey, pairingKey).sortedByPrefs()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun startDeviceScan() {
        viewModelScope.launch {
            when (TypicalBookFolders.accessState(context)) {
                ScanAccessState.NEED_PERMISSION -> {
                    _events.emit(LibraryUiEvent.NeedStoragePermission)
                    return@launch
                }
                ScanAccessState.FOLDERS_UNREADABLE -> {
                    _events.emit(LibraryUiEvent.SuggestSafFallback)
                    return@launch
                }
                ScanAccessState.READY -> Unit
            }
            runScan()
        }
    }

    fun onStoragePermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (!granted) {
                _events.emit(
                    LibraryUiEvent.Message(
                        "Нет доступа к хранилищу. Скан не выполнен — используйте Импорт.",
                    ),
                )
                return@launch
            }
            when (TypicalBookFolders.accessState(context)) {
                ScanAccessState.READY -> runScan()
                else -> _events.emit(LibraryUiEvent.SuggestSafFallback)
            }
        }
    }

    fun startFolderScan(treeUri: android.net.Uri) {
        viewModelScope.launch {
            runFolderScan(treeUri)
        }
    }

    private suspend fun runFolderScan(treeUri: android.net.Uri) {
        _scanProgress.value = ScanProgressUi(inProgress = true, done = 0, total = 0)
        try {
            val files = withContext(Dispatchers.IO) {
                DocumentTreeBookScanner(context).findBookFiles(treeUri, context.cacheDir)
            }
            if (files.isEmpty()) {
                _events.emit(
                    LibraryUiEvent.Message(
                        "В выбранной папке EPUB/FB2 не найдены.",
                    ),
                )
                return
            }
            _scanProgress.value = ScanProgressUi(inProgress = true, done = 0, total = files.size)
            val result = withContext(Dispatchers.IO) {
                importCoordinator.importFiles(files) { done, total ->
                    _scanProgress.value = ScanProgressUi(inProgress = true, done = done, total = total)
                }
            }
            _events.emit(LibraryUiEvent.Message(result.summary))
        } catch (e: Exception) {
            _events.emit(LibraryUiEvent.Message(e.message ?: "Ошибка скана папки"))
        } finally {
            _scanProgress.value = ScanProgressUi()
        }
    }

    private suspend fun runScan() {
        val roots = TypicalBookFolders.readableRoots()
        val fileListingEmptyButRootsPresent =
            roots.isNotEmpty() &&
                withContext(Dispatchers.IO) {
                    roots.all { it.listFiles() == null || it.listFiles()?.isEmpty() == true }
                }
        _scanProgress.value = ScanProgressUi(inProgress = true, done = 0, total = 0)
        try {
            val (files, mediaOk) = withContext(Dispatchers.IO) {
                val fromFiles = if (roots.isEmpty()) {
                    emptyList()
                } else {
                    deviceLibraryScanner.findBookFiles(roots)
                }
                val media = MediaStoreBookDiscovery(context).findBookFiles(context.cacheDir)
                DeviceLibraryScanner.mergeBookFiles(fromFiles, media.files) to media.querySucceeded
            }
            if (files.isEmpty()) {
                val message = when {
                    roots.isEmpty() -> null
                    !mediaOk && (fileListingEmptyButRootsPresent || roots.isNotEmpty()) ->
                        "Ограниченный доступ к файлам. Выберите EPUB/FB2 через Импорт."
                    else ->
                        "В Downloads/Documents/Books EPUB/FB2 не найдены. Можно добавить через Импорт."
                }
                if (message == null || roots.isEmpty()) {
                    _events.emit(LibraryUiEvent.SuggestSafFallback)
                } else {
                    _events.emit(LibraryUiEvent.Message(message))
                }
                return
            }
            _scanProgress.value = ScanProgressUi(inProgress = true, done = 0, total = files.size)
            val result = withContext(Dispatchers.IO) {
                importCoordinator.importFiles(files) { done, total ->
                    _scanProgress.value = ScanProgressUi(inProgress = true, done = done, total = total)
                }
            }
            _events.emit(LibraryUiEvent.Message(result.summary))
        } catch (e: Exception) {
            _events.emit(LibraryUiEvent.Message(e.message ?: "Ошибка скана"))
        } finally {
            _scanProgress.value = ScanProgressUi()
        }
    }

    fun startTranslation(workId: Long, force: Boolean = false) {
        viewModelScope.launch {
            if (!translationSettings.hasKeyForSelectedProvider) {
                _events.emit(LibraryUiEvent.NeedApiKey)
                return@launch
            }
            try {
                val work = repository.getWork(workId) ?: return@launch
                val worksDir = File(context.filesDir, "books")
                val chapterTexts = withContext(Dispatchers.IO) {
                    val file = File(work.localPath)
                    if (!file.exists()) {
                        emptyList()
                    } else {
                        loadChapters(file, work.format).map { it.text }
                    }
                }
                android.util.Log.i(
                    "FandomTranslate",
                    "start workId=$workId force=$force chapters=${chapterTexts.size} " +
                        "chars=${chapterTexts.sumOf { it.length }}",
                )
                translationPipeline.translateWork(workId, worksDir, chapterTexts, force = force)
                android.util.Log.i("FandomTranslate", "done workId=$workId force=$force")
                _events.emit(
                    LibraryUiEvent.Message(
                        if (force) "Перевод обновлён" else "Перевод завершён",
                    ),
                )
            } catch (_: MissingApiKeyException) {
                _events.emit(LibraryUiEvent.NeedApiKey)
            } catch (e: Exception) {
                android.util.Log.e("FandomTranslate", "failed workId=$workId: ${e.message}", e)
                _events.emit(LibraryUiEvent.Message(e.message ?: "Ошибка перевода"))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryRoute(
    onOpenWork: (Long) -> Unit,
    onImport: () -> Unit,
    onOpenFilter: () -> Unit,
    onOpenSettings: () -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    var tab by remember { mutableIntStateOf(0) }
    var sortExpanded by remember { mutableStateOf(false) }
    val shelf = if (tab == 0) Shelf.FANFICTION else Shelf.OTHER
    val titleQuery by vm.titleQuery.collectAsStateWithLifecycle()
    val summaryQuery by vm.summaryQuery.collectAsStateWithLifecycle()
    val searchVisible by vm.searchVisible.collectAsStateWithLifecycle()
    val works by vm.searchResults(shelf).collectAsStateWithLifecycle()
    val scanProgress by vm.scanProgress.collectAsStateWithLifecycle()
    val sortPrefs by vm.sortPreferences.collectAsStateWithLifecycle()
    val restoreWorkId by vm.restoreWorkId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hasSearch = titleQuery.isNotBlank() || summaryQuery.isNotBlank()
    val context = androidx.compose.ui.platform.LocalContext.current
    val openWork: (Long) -> Unit = { id ->
        vm.rememberOpenedWork(id)
        onOpenWork(id)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        vm.onStoragePermissionResult(granted)
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        vm.startFolderScan(uri)
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                LibraryUiEvent.NeedApiKey -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Нужен API key выбранного провайдера для перевода",
                        actionLabel = "Настройки",
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onOpenSettings()
                    }
                }
                LibraryUiEvent.NeedStoragePermission -> {
                    val permission = TypicalBookFolders.requiredPermissionOrNull()
                        ?: Manifest.permission.READ_EXTERNAL_STORAGE
                    permissionLauncher.launch(permission)
                }
                LibraryUiEvent.SuggestSafFallback -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Нет доступа к типичным папкам. Добавьте файл вручную.",
                        actionLabel = "Импорт",
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onImport()
                    }
                }
                is LibraryUiEvent.Message -> {
                    snackbarHostState.showSnackbar(event.text)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (scanProgress.inProgress) {
                        Text("Скан ${scanProgress.done}/${scanProgress.total}")
                    } else {
                        Text("Библиотека")
                    }
                },
                actions = {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onOpenFilter,
                        modifier = Modifier.semantics { contentDescription = "Фильтр" },
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null)
                    }
                    IconButton(
                        onClick = { vm.toggleSearchVisible() },
                        modifier = Modifier.semantics { contentDescription = "Поиск" },
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    IconButton(
                        onClick = { sortExpanded = !sortExpanded },
                        modifier = Modifier.semantics { contentDescription = "Сортировка" },
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null)
                    }
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.semantics { contentDescription = "Ещё" },
                    ) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Сканировать папку…") },
                            onClick = {
                                menuOpen = false
                                folderLauncher.launch(null)
                            },
                            enabled = !scanProgress.inProgress,
                        )
                        DropdownMenuItem(
                            text = { Text("Импорт") },
                            onClick = {
                                menuOpen = false
                                onImport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Настройки") },
                            onClick = {
                                menuOpen = false
                                onOpenSettings()
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Фанфики") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Прочее") })
            }
            AnimatedVisibility(visible = searchVisible) {
                Column {
                    OutlinedTextField(
                        value = titleQuery,
                        onValueChange = vm::setTitleQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        label = { Text("Поиск по названию") },
                    )
                    OutlinedTextField(
                        value = summaryQuery,
                        onValueChange = vm::setSummaryQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        label = { Text("Поиск по описанию") },
                    )
                }
            }
            if (works.isEmpty() && !scanProgress.inProgress) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (hasSearch) "Ничего не найдено."
                        else "Пусто. Найдите EPUB/FB2 в Downloads, Documents и Books.",
                    )
                    if (!hasSearch) {
                        TextButton(onClick = { vm.startDeviceScan() }) {
                            Text("Сканировать устройство")
                        }
                        TextButton(onClick = onImport) {
                            Text("Импорт файла")
                        }
                    }
                }
            } else {
                AnimatedVisibility(visible = sortExpanded) {
                    WorkListSortBar(
                        prefs = sortPrefs,
                        onSort = vm::setSort,
                        onDirection = vm::setSortDirection,
                    )
                }
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

@Composable
fun WorkListSortBar(
    prefs: LibrarySortPreferences,
    onSort: (WorkSort) -> Unit,
    onDirection: (SortDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val sortLabel = when (prefs.sort) {
        WorkSort.LastOpened -> "По просмотру"
        WorkSort.Title -> "По алфавиту"
        WorkSort.Size -> "По размеру"
    }
    val dirLabel = when (prefs.direction) {
        SortDirection.Descending -> when (prefs.sort) {
            WorkSort.LastOpened -> "новые→старые"
            WorkSort.Title -> "Я→А"
            WorkSort.Size -> "большие→малые"
        }
        SortDirection.Ascending -> when (prefs.sort) {
            WorkSort.LastOpened -> "старые→новые"
            WorkSort.Title -> "А→Я"
            WorkSort.Size -> "малые→большие"
        }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { menuOpen = true }) {
            Text("$sortLabel · $dirLabel")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("По последнему просмотру") },
                onClick = {
                    menuOpen = false
                    onSort(WorkSort.LastOpened)
                },
            )
            DropdownMenuItem(
                text = { Text("По алфавиту") },
                onClick = {
                    menuOpen = false
                    onSort(WorkSort.Title)
                },
            )
            DropdownMenuItem(
                text = { Text("По размеру") },
                onClick = {
                    menuOpen = false
                    onSort(WorkSort.Size)
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (prefs.direction == SortDirection.Descending) {
                            "Направление: по возрастанию"
                        } else {
                            "Направление: по убыванию"
                        },
                    )
                },
                onClick = {
                    menuOpen = false
                    onDirection(
                        if (prefs.direction == SortDirection.Descending) {
                            SortDirection.Ascending
                        } else {
                            SortDirection.Descending
                        },
                    )
                },
            )
        }
    }
}

@Composable
fun WorkList(
    works: List<Work>,
    onOpenWork: (Long) -> Unit,
    onTranslate: ((Long, Boolean) -> Unit)? = null,
    restoreWorkId: Long? = null,
    onRestored: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (works.isEmpty()) {
        Text("Пусто", modifier = modifier.padding(24.dp))
        return
    }
    val listState = rememberLazyListState()
    LaunchedEffect(restoreWorkId, works) {
        val target = restoreWorkId ?: return@LaunchedEffect
        val index = works.indexOfFirst { it.id == target }
        if (index >= 0) {
            listState.scrollToItem(index)
            onRestored()
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(works, key = { it.id }) { work ->
            WorkListRow(
                work = work,
                onOpenWork = onOpenWork,
                onTranslate = onTranslate,
            )
        }
    }
}

@Composable
private fun WorkListRow(
    work: Work,
    onOpenWork: (Long) -> Unit,
    onTranslate: ((Long, Boolean) -> Unit)?,
) {
    var expanded by remember(work.id) { mutableStateOf(false) }
    var confirmTranslate by remember(work.id) { mutableStateOf(false) }
    val fullSummary = (work.summaryRu ?: work.summary).orEmpty()
    val canExpand = fullSummary.length > COLLAPSED_SUMMARY_MAX
    val collapsedPreview = remember(fullSummary) { collapsedSummaryPreview(fullSummary) }
    val hasTranslation = work.translationStatus != TranslationStatus.NONE
    val showTranslate = onTranslate != null && work.source == WorkSource.AO3
    val translateLabel = if (hasTranslation) "Перевести заново" else "Перевести"
    val translateColor = if (hasTranslation) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val summaryStyle = MaterialTheme.typography.bodyMedium
    val summaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val expandColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    ListItem(
        headlineContent = {
            Text(
                text = work.titleRu ?: work.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenWork(work.id) },
            )
        },
        supportingContent = {
            if (fullSummary.isNotEmpty() || canExpand || showTranslate) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    when {
                        expanded -> {
                            var summaryLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                            val expandedSummaryBody = remember(fullSummary, canExpand) {
                                if (canExpand) glueLastWordForInlineSuffix(fullSummary) else fullSummary
                            }
                            val expandedSummaryText = remember(expandedSummaryBody, expandColor, canExpand) {
                                buildAnnotatedString {
                                    append(expandedSummaryBody)
                                    if (canExpand) {
                                        append("\u2060")
                                        pushLink(
                                            LinkAnnotation.Clickable(
                                                tag = SUMMARY_COLLAPSE_LINK,
                                                linkInteractionListener = LinkInteractionListener {
                                                    expanded = false
                                                },
                                            ),
                                        )
                                        withStyle(
                                            SpanStyle(
                                                color = expandColor,
                                                fontSize = 11.sp,
                                            ),
                                        ) {
                                            append(SUMMARY_COLLAPSE_SUFFIX)
                                        }
                                        pop()
                                    }
                                }
                            }
                            Text(
                                text = expandedSummaryText,
                                style = summaryStyle.copy(color = summaryColor),
                                onTextLayout = { summaryLayout = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("summary_expanded")
                                    .semantics {
                                        if (canExpand) {
                                            customActions = listOf(
                                                CustomAccessibilityAction("Свернуть") {
                                                    expanded = false
                                                    true
                                                },
                                            )
                                        }
                                    }
                                    .pointerInput(expandedSummaryText, summaryLayout, canExpand) {
                                        detectTapGestures { position ->
                                            val layout = summaryLayout ?: return@detectTapGestures
                                            val charOffset = layout.getOffsetForPosition(position)
                                            if (canExpand) {
                                                val collapseClicked = expandedSummaryText
                                                    .getLinkAnnotations(charOffset, charOffset)
                                                    .any { annotation ->
                                                        (annotation.item as? LinkAnnotation.Clickable)?.tag ==
                                                            SUMMARY_COLLAPSE_LINK
                                                    }
                                                if (collapseClicked) {
                                                    expanded = false
                                                    return@detectTapGestures
                                                }
                                            }
                                            onOpenWork(work.id)
                                        }
                                    },
                            )
                        }
                        canExpand && !expanded -> {
                            var summaryLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                            val collapsedSummaryText = remember(collapsedPreview, expandColor) {
                                buildAnnotatedString {
                                    append(collapsedPreview)
                                    append("\u2060")
                                    pushLink(
                                        LinkAnnotation.Clickable(
                                            tag = SUMMARY_EXPAND_LINK,
                                            linkInteractionListener = LinkInteractionListener {
                                                expanded = true
                                            },
                                        ),
                                    )
                                    withStyle(
                                        SpanStyle(
                                            color = expandColor,
                                            fontSize = 11.sp,
                                        ),
                                    ) {
                                        append(SUMMARY_EXPAND_SUFFIX)
                                    }
                                    pop()
                                }
                            }
                            Text(
                                text = collapsedSummaryText,
                                style = summaryStyle.copy(color = summaryColor),
                                onTextLayout = { summaryLayout = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("summary_collapsed")
                                    .semantics {
                                        customActions = listOf(
                                            CustomAccessibilityAction("Показать полностью") {
                                                expanded = true
                                                true
                                            },
                                        )
                                    }
                                    .pointerInput(collapsedSummaryText, summaryLayout) {
                                        detectTapGestures { position ->
                                            val layout = summaryLayout ?: return@detectTapGestures
                                            val charOffset = layout.getOffsetForPosition(position)
                                            val expandClicked = collapsedSummaryText
                                                .getLinkAnnotations(charOffset, charOffset)
                                                .any { annotation ->
                                                    (annotation.item as? LinkAnnotation.Clickable)?.tag ==
                                                        SUMMARY_EXPAND_LINK
                                                }
                                            if (expandClicked) {
                                                expanded = true
                                            } else {
                                                onOpenWork(work.id)
                                            }
                                        }
                                    },
                            )
                        }
                        fullSummary.isNotEmpty() -> {
                            Text(
                                text = fullSummary,
                                style = summaryStyle,
                                color = summaryColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenWork(work.id) },
                            )
                        }
                    }
                    if (showTranslate) {
                        Text(
                            text = translateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = translateColor,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .semantics { contentDescription = translateLabel }
                                .clickable { confirmTranslate = true },
                        )
                    }
                }
            }
        },
        overlineContent = {
            if (work.translationStatus != TranslationStatus.NONE) {
                Text("Перевод: ${work.translationStatus.name}")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (confirmTranslate && onTranslate != null) {
        AlertDialog(
            onDismissRequest = { confirmTranslate = false },
            title = {
                Text(if (hasTranslation) "Перевести заново?" else "Перевести?")
            },
            text = {
                Text(
                    if (hasTranslation) {
                        "Отправить текст в нейросеть и заменить текущий перевод?"
                    } else {
                        "Отправить текст в нейросеть для перевода?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmTranslate = false
                        onTranslate(work.id, hasTranslation)
                    },
                ) {
                    Text("Отправить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmTranslate = false }) {
                    Text("Отмена")
                }
            },
        )
    }
}
