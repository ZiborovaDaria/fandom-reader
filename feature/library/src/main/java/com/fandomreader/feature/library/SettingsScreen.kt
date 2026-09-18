package com.fandomreader.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.fandomreader.feature.translation.LiveTranslationProviderId
import com.fandomreader.feature.translation.TranslationKeySlot
import com.fandomreader.feature.translation.TranslationProviderSettings
import com.fandomreader.ui.theme.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val translationSettings: TranslationProviderSettings,
    private val appThemePreferences: AppThemePreferencesRepository,
) : ViewModel() {
    private val _selectedProvider = MutableStateFlow(translationSettings.selectedProvider)
    val selectedProvider: StateFlow<LiveTranslationProviderId> = _selectedProvider.asStateFlow()

    private val _configuredSlots = MutableStateFlow(
        TranslationKeySlot.entries.associateWith { translationSettings.hasKey(it) },
    )
    val configuredSlots: StateFlow<Map<TranslationKeySlot, Boolean>> = _configuredSlots.asStateFlow()

    val themeMode: StateFlow<AppThemeMode> = appThemePreferences.themeMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppThemeMode.System,
    )

    fun setSelectedProvider(id: LiveTranslationProviderId) {
        translationSettings.selectedProvider = id
        _selectedProvider.value = id
    }

    fun saveKey(slot: TranslationKeySlot, raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        translationSettings.setKey(slot, trimmed)
        refreshConfigured()
    }

    fun clearKey(slot: TranslationKeySlot) {
        translationSettings.clearKey(slot)
        refreshConfigured()
    }

    fun cursorBaseUrl(): String = translationSettings.cursorBaseUrl
    fun cursorModel(): String = translationSettings.cursorModel

    fun saveCursorEndpoint(baseUrl: String, model: String) {
        if (baseUrl.isNotBlank()) translationSettings.cursorBaseUrl = baseUrl
        if (model.isNotBlank()) translationSettings.cursorModel = model
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { appThemePreferences.setThemeMode(mode) }
    }

    private fun refreshConfigured() {
        _configuredSlots.value =
            TranslationKeySlot.entries.associateWith { translationSettings.hasKey(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val configured by vm.configuredSlots.collectAsStateWithLifecycle()
    val selected by vm.selectedProvider.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }
    var cursorBaseDraft by remember { mutableStateOf(vm.cursorBaseUrl()) }
    var cursorModelDraft by remember { mutableStateOf(vm.cursorModel()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Тема приложения", fontWeight = FontWeight.SemiBold)
            Text("Библиотека и остальные экраны (кроме поверхности чтения в ридере)")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        AppThemeMode.Light -> "Светлая"
                        AppThemeMode.Dark -> "Тёмная"
                        AppThemeMode.System -> "Система"
                    }
                    TextButton(onClick = { vm.setThemeMode(mode) }) {
                        Text(
                            label,
                            fontWeight = if (themeMode == mode) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Text("Провайдер перевода", fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LiveTranslationProviderId.entries.forEach { id ->
                    val label = when (id) {
                        LiveTranslationProviderId.Gemini -> "Gemini"
                        LiveTranslationProviderId.Cursor -> "Cursor"
                    }
                    TextButton(onClick = { vm.setSelectedProvider(id) }) {
                        Text(
                            label,
                            fontWeight = if (selected == id) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            TranslationKeySlot.entries.forEach { slot ->
                ProviderKeySlot(
                    slot = slot,
                    configured = configured[slot] == true,
                    onSave = { key ->
                        vm.saveKey(slot, key)
                        message = "${slot.label}: ключ сохранён"
                    },
                    onClear = {
                        vm.clearKey(slot)
                        message = "${slot.label}: ключ удалён"
                    },
                )
            }

            Text("Cursor endpoint (временно)", fontWeight = FontWeight.SemiBold)
            Text("OpenAI-совместимый base URL и model id")
            OutlinedTextField(
                value = cursorBaseDraft,
                onValueChange = { cursorBaseDraft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Base URL") },
                singleLine = true,
            )
            OutlinedTextField(
                value = cursorModelDraft,
                onValueChange = { cursorModelDraft = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Model") },
                singleLine = true,
            )
            Button(
                onClick = {
                    vm.saveCursorEndpoint(cursorBaseDraft, cursorModelDraft)
                    message = "Cursor endpoint сохранён"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить endpoint")
            }

            message?.let { Text(it) }
        }
    }
}

@Composable
private fun ProviderKeySlot(
    slot: TranslationKeySlot,
    configured: Boolean,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var draft by remember(slot) { mutableStateOf("") }
    var reveal by remember(slot) { mutableStateOf(false) }
    Text(slot.label, fontWeight = FontWeight.SemiBold)
    Text(
        buildString {
            append(if (configured) "Ключ задан" else "Ключ не задан")
            if (slot.temporary) append(" · временный")
            if (!slot.live) append(" · только слот, адаптер позже")
        },
    )
    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("${slot.label} API key") },
        placeholder = {
            Text(if (configured) "•••••••• (введите новый, чтобы заменить)" else "Вставьте ключ")
        },
        singleLine = true,
        visualTransformation = if (reveal) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            TextButton(onClick = { reveal = !reveal }) {
                Text(if (reveal) "Скрыть" else "Показать")
            }
        },
    )
    Button(
        onClick = {
            if (draft.isNotBlank()) {
                onSave(draft)
                draft = ""
                reveal = false
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Сохранить")
    }
    TextButton(
        onClick = {
            onClear()
            draft = ""
        },
        enabled = configured,
    ) {
        Text("Очистить ключ")
    }
}
