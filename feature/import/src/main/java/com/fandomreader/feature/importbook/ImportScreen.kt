package com.fandomreader.feature.importbook

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.fandomreader.source.api.ImportCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ImportViewModel @Inject constructor(
    val importCoordinator: ImportCoordinator,
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportRoute(
    onDone: () -> Unit,
    vm: ImportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Выберите EPUB или FB2") }
    var busy by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            status = "Импорт…"
            runCatching {
                withContext(Dispatchers.IO) {
                    val tmp = SafImportCopy.copyToCache(context.contentResolver, uri, context.cacheDir)
                    vm.importCoordinator.importFile(tmp)
                }
            }.onSuccess {
                status = "Готово: ${it.title}"
                onDone()
            }.onFailure {
                status = "Ошибка: ${it.message}"
            }
            busy = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт") },
                navigationIcon = {
                    TextButton(onClick = onDone) { Text("Назад") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Файл с устройства",
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = status,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    launcher.launch(
                        arrayOf(
                            "application/epub+zip",
                            "application/x-fictionbook+xml",
                            "text/xml",
                            "*/*",
                        ),
                    )
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "Импорт…" else "Выбрать файл")
            }
            Text(
                text = "Повторный выбор того же файла не создаёт дубликат — " +
                    "работа уже в библиотеке будет отмечена как существующая.",
            )
        }
    }
}
