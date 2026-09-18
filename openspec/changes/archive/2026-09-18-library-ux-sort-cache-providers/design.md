## Context

См. `proposal.md` — Why. Сейчас: списки `ORDER BY title COLLATE NOCASE` без user sort; `LazyColumn` без restore; `FilterBuilderRoute` пересоздаёт `selectedTabIndex` через `remember(availableTabs)` при каждом обновлении Flow → мерцание; `ReaderViewModel.load` всегда `loadChapters` из файла; перевод только Gemini через `ResolvingTranslationProvider` + `GeminiSettings`; кнопка перевода — `trailingContent` у `WorkListRow`.

## Goals / Non-Goals

**Goals:**
- Sort + direction persistence; default last-opened desc.
- List scroll restore к открытой работе.
- Стабильный FilterBuilder (табы/строки не «прыгают»).
- Chapter parse cache на повторный open.
- Harden Ficbook chapter extract (пусто/`***`).
- Settings: слоты ключей + временный Cursor LLM-провайдер; Gemini primary.
- Кнопка перевода под контентом строки.

**Non-Goals:**
- Живые адаптеры OpenAI/Anthropic/и т.д. (только UI-слоты).
- Долгосрочный Cursor как продуктовый провайдер (временный, удалим позже).
- Изменение правил `canonicalKey` / whole-file translate / immersive chrome defaults.
- Облачный sync библиотеки.

## Decisions

### 1. Sort model + persistence
- Добавить domain enum `WorkSort` (`LastOpened`, `Title`, `Size`) + `SortDirection`.
- Хранить preference в DataStore (не Room), default `LastOpened` + `Descending`.
- Room: поля `lastOpenedAt: Long` (обновлять при успешном open reader) и `fileSizeBytes: Long` (при import/scan; 0 если неизвестен).
- Алфавит: `Collator` с `Locale("ru","RU")` по display title (`titleRu ?: title`).
- SQL или in-memory sort после query — выбрать один путь в apply; для Flow-списков предпочтительно DAO с параметризованным ORDER BY / сортировка в repository, чтобы не дублировать в UI.
- **Alternative:** только in-memory sort без DB fields — отвергнуто: last opened/size нужны устойчивые поля.

### 2. List scroll restore
- `LazyListState` + `rememberLazyListState` / saveable; при уходе в reader запомнить `workId` (+ index/offset).
- На return: `scrollToItem` / `animateScrollToItem` к ключу работы.
- Scope: work lists (shelf, filtered, filter results), не обязательно списки фэндомов.
- **Alternative:** Navigation `savedStateHandle` only — недостаточно для LazyColumn offset; комбинировать с list state.

### 3. Filter flicker fix
- Корневая причина: `remember(availableTabs) { mutableIntStateOf(0) }` сбрасывает таб; `availableTabs` пересчитывается на каждый emit `works`.
- Fix: стабилизировать tab index без reset при том же наборе табов; не зависеть selection state от identity list recomputation; увеличить tap target / убрать конфликтующие clickable overlays если есть.
- Selection (`selectedFandoms` и т.д.) оставить в `remember` без ключа на Flow, либо поднять в ViewModel.
- **Alternative:** полный rewrite FilterBuilder — избыточно для бага.

### 4. Chapter parse cache
- Process-scoped (и опционально disk) cache: `workId` + file identity (`path` + `lastModified`/`length`) → list of chapter texts/segments.
- `ReaderViewModel.load`: hit cache → overlay RU + restore progress; miss → parse → put.
- Invalidate on file change; clear entry on demand не требуется в UI.
- **Alternative:** всегда parse — status quo, отвергнуто предложением.

### 5. Ficbook empty / `***`
- Диагностировать на реальных Ficbook EPUB (не только fixtures): spine order, front-matter drop, `paragraphsFromBr` / `collectBlockTexts`, фильтр `length ≤ 1`.
- Починить extract так, чтобы narrative не терялся; `***` как scene break оставлять, но не единственным содержимым при наличии текста.
- Добавить regression fixture/тест на проблемный паттерн, если удастся воспроизвести из user/sample файла.
- Meta path (`FicbookEpubParser`) не менять без нужды.

### 6. Multi-provider keys + temporary Cursor
- Обобщить encrypted storage: map `providerId → key` (Gemini, Cursor, placeholders e.g. `openai`, `anthropic`).
- Settings: masked slots + active provider selector (Gemini | Cursor); placeholder slots save-only.
- `CursorProvider: TranslationProvider` — HTTP chat completions (OpenAI-совместимый shape) с system/user prompt «переведи на русский», batch как у Gemini; base URL + model вынести в константы/настройки с пометкой temporary.
- `ResolvingTranslationProvider`: resolve по selected provider + key at request time; Fake только в тестах / отсутствие live binding.
- Cursor помечен temporary в UI copy; удаление — отдельный follow-up.
- **Alternative:** только UI без Cursor live — отвергнуто ответом пользователя (нужен рабочий Cursor сейчас).
- **Risk note:** у Cursor нет стабильного публичного mobile Translation API как у Gemini; endpoint/model уточнить при apply (конфиг, не spec). Если публичный endpoint недоступен — использовать явно настраиваемый OpenAI-compatible base URL под слотом «Cursor» как временный мост.

### 7. Translate button layout
- Убрать `trailingContent` TextButton; добавить secondary row / full-width TextButton под summary в `WorkListRow` для AO3 translate actions.
- Не добавлять persistent FAB в reader (вне scope).

## Risks / Trade-offs

- [Cursor endpoint нестабилен/закрыт] → Mitigate: configurable base URL+model; feature-flag / easy remove; Gemini остаётся primary.
- [Parse cache memory на больших библиотеках] → Mitigate: LRU по workId, хранить только недавно открытые.
- [fileSizeBytes=0 для старых импортов] → Mitigate: backfill при scan/open; size sort кладёт unknown в конец.
- [Filter fix не покроет все жесты] → Mitigate: ручной QA на FilterBuilder tabs + checkboxes.
- [Ficbook bug только на части экспортов] → Mitigate: лог/ошибка при empty chapter + targeted fixture когда найдён паттерн.

## Migration Plan

1. Room migration: `lastOpenedAt`, `fileSizeBytes` (default 0).
2. DataStore sort prefs с defaults.
3. Backfill size на следующем scan/open.
4. Settings UI: новые слоты; существующий Gemini key мигрирует без потери.
5. Rollback: feature flags не обязательны; при откате Cursor — удалить provider + слот, Gemini path без изменений.

## Open Questions

- Точный публичный Cursor HTTP base URL / model id для Android (зафиксировать константами при apply; не блокирует tasks).
- Нужен ли disk chapter cache между process deaths или достаточно process-scoped LRU (default: process-scoped + optional disk если reopen после kill всё ещё «тяжёлый»).
