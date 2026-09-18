package com.fandomreader.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fandomreader.domain.repository.LibraryRepository
import com.fandomreader.source.api.BookFormatDetect
import com.fandomreader.ui.theme.ReaderSurfaceTheme
import com.fandomreader.ui.theme.SystemBarAppearance
import com.fandomreader.ui.theme.prefersDarkStatusIcons
import com.fandomreader.ui.theme.readerSurfaceColors
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class ReaderChapter(
    val index: Int,
    val title: String?,
    val text: String,
    val textRu: String? = null,
)

/** Prefer v3 paragraph-preserving cache after spine front-matter exclusion. */
internal fun translationCacheFile(booksParent: File?, workId: Long, chapterIndex: Int): File =
    File(booksParent, "translations_v3/$workId/chapter_$chapterIndex.txt")

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val preferencesRepository: ReaderPreferencesRepository,
) : ViewModel() {
    var chapters by mutableStateOf<List<ReaderChapter>>(emptyList())
        private set
    var chapterIndex by mutableIntStateOf(0)
        private set
    var chromeVisible by mutableStateOf(false)
        private set
    var savedOffset by mutableIntStateOf(0)
        private set
    var workTitle by mutableStateOf<String?>(null)
        private set
    private var workId: Long = 0L

    val preferences = preferencesRepository.preferences

    fun load(id: Long) {
        workId = id
        viewModelScope.launch {
            val work = libraryRepository.getWork(id) ?: return@launch
            workTitle = work.titleRu ?: work.title
            val savedIndex = work.readingProgressChapterIndex
            savedOffset = work.readingProgressOffset
            val loaded = withContext(Dispatchers.IO) {
                val file = File(work.localPath)
                if (!file.exists()) {
                    listOf(
                        ReaderChapter(
                            0,
                            work.titleRu ?: work.title,
                            work.summary ?: "Файл не найден",
                            work.summaryRu,
                        ),
                    )
                } else {
                    val parsed = ChapterParseCache.get(work.id, file)
                        ?: loadChapters(file, work.format).also {
                            ChapterParseCache.put(work.id, file, it)
                        }
                    if (work.fileSizeBytes <= 0L) {
                        libraryRepository.updateFileSizeBytes(work.id, file.length())
                    }
                    libraryRepository.touchLastOpened(work.id)
                    parsed.map { chapter ->
                        val cache = translationCacheFile(file.parentFile, work.id, chapter.index)
                        if (cache.exists()) {
                            chapter.copy(textRu = cache.readText(Charsets.UTF_8))
                        } else {
                            chapter
                        }
                    }
                }
            }
            chapters = loaded
            val looksLikeMeta = loaded.getOrNull(savedIndex)?.let {
                chapterLooksLikeFrontMatter(it.text)
            } == true
            chapterIndex = resolveReadingChapterIndex(
                savedIndex = savedIndex,
                chapterCount = loaded.size,
                savedChapterLooksLikeFrontMatter = looksLikeMeta,
            )
            if (chapterIndex != savedIndex) {
                savedOffset = 0
                saveProgress(0)
            }
        }
    }

    fun toggleChrome() {
        chromeVisible = !chromeVisible
    }

    fun next() {
        if (chapterIndex < chapters.lastIndex) {
            chapterIndex++
            savedOffset = 0
            saveProgress(0)
        }
    }

    fun prev() {
        if (chapterIndex > 0) {
            chapterIndex--
            savedOffset = 0
            saveProgress(0)
        }
    }

    fun jumpToChapter(index: Int) {
        if (index in chapters.indices) {
            chapterIndex = index
            savedOffset = 0
            saveProgress(0)
            chromeVisible = false
        }
    }

    fun saveProgress(offset: Int) {
        viewModelScope.launch {
            libraryRepository.updateReadingProgress(workId, chapterIndex, offset.coerceAtLeast(0))
        }
    }

    fun setFontFamily(option: ReaderFontFamilyOption) {
        viewModelScope.launch { preferencesRepository.setFontFamily(option) }
    }

    fun setFontSizeSp(size: Int) {
        viewModelScope.launch { preferencesRepository.setFontSizeSp(size) }
    }

    fun setSurfaceTheme(theme: ReaderSurfaceTheme) {
        viewModelScope.launch { preferencesRepository.setSurfaceTheme(theme) }
    }

    fun displayText(chapter: ReaderChapter): String =
        chapter.textRu?.takeIf { it.isNotBlank() } ?: chapter.text
}

fun loadChapters(file: File, format: String): List<ReaderChapter> {
    val resolved = when (val f = format.lowercase()) {
        "epub", "fb2" -> f
        else -> BookFormatDetect.detect(file)
    }
    return when (resolved) {
        "epub" -> loadEpub(file)
        "fb2" -> loadFb2(file)
        else -> listOf(
            ReaderChapter(
                0,
                null,
                "Неподдерживаемый формат файла. Нужен EPUB или FB2.",
            ),
        )
    }
}

private fun loadEpub(file: File): List<ReaderChapter> {
    ZipFile(file).use { zip ->
        val htmlEntries = zip.entries().asSequence()
            .filter { !it.isDirectory && (it.name.endsWith(".xhtml", true) || it.name.endsWith(".html", true)) }
            .filter { !it.name.contains("nav", true) && !it.name.contains("toc", true) }
            .map { it.name }
            .toList()
        val spine = readOpfSpineHrefs(file)
        val orderedNames = orderEpubHtmlEntries(htmlEntries, spine)
        val chapters = orderedNames.mapNotNull { name ->
            val entry = zip.getEntry(name) ?: return@mapNotNull null
            val html = zip.getInputStream(entry).bufferedReader().readText()
            if (isEpubFrontMatter(name, html)) return@mapNotNull null
            val doc = Jsoup.parse(html)
            val heading = doc.selectFirst("h2.heading, h1, h2")?.text()?.trim()
                ?.takeIf { it.isNotEmpty() }
            ReaderChapter(
                index = 0,
                title = heading,
                text = extractChapterBody(html),
            )
        }.mapIndexed { index, chapter -> chapter.copy(index = index) }
        return chapters.ifEmpty { listOf(ReaderChapter(0, null, "Empty EPUB")) }
    }
}

private fun loadFb2(file: File): List<ReaderChapter> {
    val xml = file.readText(Charsets.UTF_8)
    val rawSections = Regex(
        """<section\b[^>]*>(.*?)</section>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    ).findAll(xml)
        .map { match ->
            val html = match.groupValues[1]
            val title = Regex(
                """<title>(.*?)</title>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
            ).find(html)?.groupValues?.getOrNull(1)?.let { Jsoup.parse(it).text() }
            val bodyHtml = html.replace(
                Regex("""<title>.*?</title>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
                "",
            )
            val text = extractChapterBody(bodyHtml)
            Triple(title, text, isFb2MetaSection(title, text))
        }
        .toList()

    val filtered = if (rawSections.size <= 1) {
        rawSections
    } else {
        rawSections.filterNot { it.third }
    }.ifEmpty { rawSections }

    return filtered.mapIndexed { index, (title, text, _) ->
        ReaderChapter(index, title, text)
    }.ifEmpty {
        listOf(ReaderChapter(0, null, extractChapterBody(xml)))
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderRoute(
    workId: Long,
    onBack: () -> Unit = {},
    onOpenMeta: (() -> Unit)? = null,
    vm: ReaderViewModel = hiltViewModel(),
) {
    LaunchedEffect(workId) { vm.load(workId) }
    val prefs by vm.preferences.collectAsStateWithLifecycle(
        initialValue = ReaderPreferences(),
    )
    SystemBarAppearance(darkIcons = prefs.surfaceTheme.prefersDarkStatusIcons())
    val chapter = vm.chapters.getOrNull(vm.chapterIndex)
    val surface = readerSurfaceColors(prefs.surfaceTheme)
    var showToc by remember { mutableStateOf(false) }
    var showAa by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val latestOffset = rememberUpdatedState(vm.savedOffset)
    var restoredForChapter by remember { mutableIntStateOf(-1) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(chapter?.index, chapter?.text, chapter?.textRu) {
        if (chapter == null) return@LaunchedEffect
        if (restoredForChapter != chapter.index) {
            scrollState.scrollTo(latestOffset.value.coerceAtLeast(0))
            restoredForChapter = chapter.index
        }
    }

    LaunchedEffect(scrollState, chapter?.index) {
        snapshotFlow { scrollState.value }
            .debounce(300)
            .distinctUntilChanged()
            .collect { offset ->
                if (chapter != null && restoredForChapter == chapter.index) {
                    vm.saveProgress(offset)
                }
            }
    }

    DisposableEffect(workId, chapter?.index, scrollState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.saveProgress(scrollState.value)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            vm.saveProgress(scrollState.value)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(surface.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { vm.toggleChrome() },
    ) {
        if (chapter == null) {
            Text(
                "Загрузка…",
                color = surface.foreground,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(20.dp),
            )
        } else {
            ChapterBody(
                text = vm.displayText(chapter),
                prefs = prefs,
                color = surface.foreground,
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp, bottom = if (vm.chromeVisible) 120.dp else 16.dp)
                    .verticalScroll(scrollState),
            )
        }

        if (vm.chromeVisible) {
            ChapterScrollbar(
                scrollState = scrollState,
                trackColor = surface.foreground.copy(alpha = 0.15f),
                thumbColor = surface.foreground.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(end = 4.dp, top = 8.dp, bottom = 130.dp)
                    .width(4.dp),
            )
            ReaderChrome(
                chapterIndex = vm.chapterIndex,
                chapterCount = vm.chapters.size.coerceAtLeast(1),
                scrollProgress = if (scrollState.maxValue > 0) {
                    scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                } else {
                    0f
                },
                onBack = onBack,
                onPrev = vm::prev,
                onNext = vm::next,
                onToc = { showToc = true },
                onAa = { showAa = true },
                onAbout = onOpenMeta,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(surface.background)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { /* consume */ },
                foreground = surface.foreground,
            )
        }
    }

    if (showToc) {
        ModalBottomSheet(
            onDismissRequest = { showToc = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Text(
                "Оглавление",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            LazyColumn {
                itemsIndexed(vm.chapters, key = { _, c -> c.index }) { _, item ->
                    val selected = item.index == vm.chapterIndex
                    Text(
                        text = tocLabel(item.title, vm.workTitle, item.index + 1),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.jumpToChapter(item.index)
                                showToc = false
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAa) {
        ModalBottomSheet(
            onDismissRequest = { showAa = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            AppearanceSheet(
                prefs = prefs,
                onFontFamily = vm::setFontFamily,
                onFontSize = vm::setFontSizeSp,
                onTheme = vm::setSurfaceTheme,
            )
        }
    }
}

@Composable
private fun ChapterBody(
    text: String,
    prefs: ReaderPreferences,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val family = when (prefs.fontFamily) {
        ReaderFontFamilyOption.Serif -> FontFamily.Serif
        ReaderFontFamilyOption.Sans -> FontFamily.SansSerif
        ReaderFontFamilyOption.Mono -> FontFamily.Monospace
    }
    val size = prefs.fontSizeSp.sp
    val lineHeight = (prefs.fontSizeSp * prefs.lineHeightMult).sp
    val noteSize = (prefs.fontSizeSp * 0.9f).sp
    val noteLineHeight = (prefs.fontSizeSp * 0.9f * prefs.lineHeightMult).sp
    val noteColor = color.copy(alpha = 0.78f)
    val noteTint = color.copy(alpha = 0.08f)
    val segments = remember(text) { parseChapterSegments(text) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        segments.forEach { segment ->
            when (segment.role) {
                ChapterSegmentRole.Body -> {
                    segment.paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            color = color,
                            fontFamily = family,
                            fontSize = size,
                            lineHeight = lineHeight,
                        )
                    }
                }
                ChapterSegmentRole.Note -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(noteTint)
                            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        segment.paragraphs.forEachIndexed { index, paragraph ->
                            Text(
                                text = paragraph,
                                color = noteColor,
                                fontFamily = family,
                                fontSize = noteSize,
                                lineHeight = noteLineHeight,
                                fontWeight = if (index == 0 && isNoteHeading(paragraph)) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterScrollbar(
    scrollState: androidx.compose.foundation.ScrollState,
    trackColor: Color,
    thumbColor: Color,
    modifier: Modifier = Modifier,
) {
    val max = scrollState.maxValue.coerceAtLeast(1)
    val fraction = scrollState.value.toFloat() / max.toFloat()
    val thumbFraction = 0.12f
    val topWeight = (fraction * (1f - thumbFraction)).coerceIn(0f, 1f)
    val bottomWeight = (1f - topWeight - thumbFraction).coerceIn(0f, 1f)
    Column(modifier = modifier.background(trackColor)) {
        if (topWeight > 0f) Spacer(Modifier.weight(topWeight))
        Box(
            Modifier
                .fillMaxWidth()
                .weight(thumbFraction.coerceAtLeast(0.05f))
                .background(thumbColor),
        )
        if (bottomWeight > 0f) Spacer(Modifier.weight(bottomWeight))
    }
}

@Composable
private fun ReaderChrome(
    chapterIndex: Int,
    chapterCount: Int,
    scrollProgress: Float,
    onBack: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToc: () -> Unit,
    onAa: () -> Unit,
    onAbout: (() -> Unit)?,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = {
                ((chapterIndex + scrollProgress) / chapterCount.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("Назад", color = foreground) }
            TextButton(onClick = onPrev) {
                Text("←", color = foreground, style = MaterialTheme.typography.titleLarge)
            }
            Text(
                "Глава ${chapterIndex + 1}/$chapterCount",
                color = foreground,
                style = MaterialTheme.typography.labelLarge,
            )
            TextButton(onClick = onNext) {
                Text("→", color = foreground, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(8.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onToc) { Text("Оглавление", color = foreground) }
            TextButton(onClick = onAa) { Text("Aa", color = foreground) }
            if (onAbout != null) {
                TextButton(onClick = onAbout) { Text("О книге", color = foreground) }
            }
        }
    }
}

@Composable
private fun AppearanceSheet(
    prefs: ReaderPreferences,
    onFontFamily: (ReaderFontFamilyOption) -> Unit,
    onFontSize: (Int) -> Unit,
    onTheme: (ReaderSurfaceTheme) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Оформление", style = MaterialTheme.typography.titleLarge)
        Text("Тема", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderSurfaceTheme.entries.forEach { theme ->
                TextButton(onClick = { onTheme(theme) }) {
                    Text(
                        theme.name,
                        fontWeight = if (prefs.surfaceTheme == theme) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Text("Шрифт", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReaderFontFamilyOption.entries.forEach { family ->
                TextButton(onClick = { onFontFamily(family) }) {
                    Text(
                        family.name,
                        fontWeight = if (prefs.fontFamily == family) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        Text("Размер: ${prefs.fontSizeSp}", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = prefs.fontSizeSp.toFloat(),
            onValueChange = { onFontSize(it.toInt()) },
            valueRange = ReaderPreferences.Defaults.minFontSizeSp.toFloat()..
                ReaderPreferences.Defaults.maxFontSizeSp.toFloat(),
            steps = ReaderPreferences.Defaults.maxFontSizeSp - ReaderPreferences.Defaults.minFontSizeSp - 1,
        )
        Spacer(Modifier.height(24.dp))
    }
}
