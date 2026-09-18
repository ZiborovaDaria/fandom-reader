package com.fandomreader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fandomreader.domain.model.DisplayTag
import com.fandomreader.domain.model.DisplayTagGroups
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class WorkMetaViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    var work by mutableStateOf<Work?>(null)
        private set

    fun load(id: Long) {
        viewModelScope.launch {
            work = libraryRepository.getWork(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkMetaRoute(
    workId: Long,
    onRead: (Long) -> Unit,
    onBack: () -> Unit,
    vm: WorkMetaViewModel = hiltViewModel(),
) {
    LaunchedEffect(workId) { vm.load(workId) }
    val work = vm.work
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(work?.let { it.titleRu ?: it.title } ?: "Произведение") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
            )
        },
    ) { padding ->
        if (work == null) {
            Text("Загрузка…", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        WorkMetaScreen(
            work = work,
            onRead = { onRead(work.id) },
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkMetaScreen(
    work: Work,
    onRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(work.titleRu ?: work.title, style = MaterialTheme.typography.headlineSmall)
        work.author?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("Описание", style = MaterialTheme.typography.titleMedium)
        val summary = (work.summaryRu ?: work.summary).orEmpty()
        Text(
            text = summary.ifBlank { "Нет описания" },
            style = MaterialTheme.typography.bodyLarge,
        )

        Text("Теги", style = MaterialTheme.typography.titleMedium)
        val tagChips = buildMetaTagChips(work)
        if (tagChips.isEmpty()) {
            Text("Нет тегов", style = MaterialTheme.typography.bodyMedium)
        } else {
            tagChips.forEach { (label, values) ->
                Text(label, style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    values.forEach { value ->
                        AssistChip(onClick = {}, label = { Text(value) })
                    }
                }
            }
        }

        Button(
            onClick = onRead,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Читать")
        }
    }
}

internal fun buildMetaTagChips(work: Work): List<Pair<String, List<String>>> {
    val sections = mutableListOf<Pair<String, List<String>>>()
    if (work.fandoms.isNotEmpty()) {
        sections += "Фэндом" to work.fandoms.map { it.displayRu ?: it.displayName }
    }
    if (work.pairings.isNotEmpty()) {
        sections += "Пейринг" to work.pairings.map { it.displayRu ?: it.displayName }
    }
    fun group(name: String, key: String) {
        val values = work.displayTags.filter { it.group == key }.map { it.displayLabel() }
        if (values.isNotEmpty()) sections += name to values
    }
    group("Рейтинг", DisplayTagGroups.RATING)
    group("Предупреждения", DisplayTagGroups.WARNINGS)
    group("Категория", DisplayTagGroups.CATEGORY)
    group("Персонажи", DisplayTagGroups.CHARACTERS)
    group("Доп. теги", DisplayTagGroups.ADDITIONAL)
    group("Жанр", DisplayTagGroups.GENRE)
    group("Размер", DisplayTagGroups.SIZE)
    val known = setOf(
        DisplayTagGroups.RATING,
        DisplayTagGroups.WARNINGS,
        DisplayTagGroups.CATEGORY,
        DisplayTagGroups.CHARACTERS,
        DisplayTagGroups.ADDITIONAL,
        DisplayTagGroups.GENRE,
        DisplayTagGroups.SIZE,
    )
    work.displayTags
        .filter { it.group !in known }
        .groupBy { it.group }
        .forEach { (g, tags) -> sections += g to tags.map { it.displayLabel() } }
    return sections
}
