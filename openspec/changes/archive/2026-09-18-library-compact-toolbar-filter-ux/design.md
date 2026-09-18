## Context

См. `proposal.md`. Текущее состояние (`LibraryScreens.kt`):
- Top bar: `TextButton("Сканировать")` + overflow (Каталог, Сканировать папку, Импорт, Настройки).
- Под вкладками полок: `TextButton` «Поиск» / «Скрыть поиск» + условные `OutlinedTextField`.
- `WorkListSortBar` — постоянная строка `TextButton` с dropdown.
- `CatalogRoute` — хаб с пунктами «Фильтры», «Фэндомы», «Пейринги», «Платоника», «Теги».
- `WorkListRow` — `ListItem` + отдельный `TextButton` «Перевести» с `padding(horizontal = 12.dp)` под карточкой.

Сортировка и prefs уже в `LibrarySortPreferences` / `WorkSort.LastOpened` (default). Логику компаратора и DAO ORDER BY менять нельзя.

## Goals / Non-Goals

**Goals:**
- Компактный toolbar: иконки поиска, сортировки, фильтра в `TopAppBar.actions` или единой `Row` под вкладками.
- Фильтр: `library` → `filters` напрямую; убрать `catalog` и пользовательские entry points в browse-маршруты.
- Inline translate в `supportingContent` `ListItem` (например `Text`/`TextButton` labelSmall рядом с «Ещё» или под summary в том же `FlowRow`).
- Сохранить `FilterBuilderRoute` и `LibraryFilterCriteria` без изменения AND/OR/`canonicalKey`.
- Сортировка «по просмотру» — только UI-обёртка, тот же `vm.setSort` / `setSortDirection`.

**Non-Goals:**
- Менять алгоритм фильтрации, Room-запросы, `lastOpenedAt` update path.
- Менять default sort или comparator для `WorkSort.LastOpened`.
- Удалять deep-link маршруты `fandoms`/`pairing-works` из кода, если они нужны для тестов/обратной совместимости — достаточно убрать из UI и `NavHost` start paths (можно удалить composable-блоки целиком, если тесты не завязаны).
- Трогать reader, translation pipeline, scan engine.

## Decisions

### 1. Toolbar layout: иконки в TopAppBar + expandable panel
**Решение:** В `TopAppBar.actions` добавить `IconButton` для фильтра (`Icons.Default.FilterList` или `Tune`), поиска (`Search`), сортировки (`Sort`). Состояние `searchExpanded` / `sortExpanded` в `LibraryRoute`. При expand — `AnimatedVisibility` блок под `PrimaryTabRow` (не в app bar), чтобы не ломать narrow width.

**Альтернатива:** Bottom sheet для сортировки — отвергнута: лишний жест для частой операции.

### 2. Фильтр: прямой navigate, удалить CatalogRoute
**Решение:** `LibraryRoute(onOpenFilter = { nav.navigate("filters") })`. Удалить `onOpenCatalog`, composable `catalog`, пункт меню «Каталог». Browse-маршруты (`fandoms`, `browse-romantic`, `browse-platonic`, `browse-tags`) удалить из `NavHost` и соответствующие `@Composable` route functions, если нет внешних ссылок.

**Альтернатива:** Оставить catalog как dead code — отвергнута: путаница в поддержке.

### 3. Сканирование: только overflow + empty state
**Решение:** Убрать `TextButton("Сканировать")` и `vm.startDeviceScan()` из top bar. Оставить «Сканировать папку…» в overflow и «Сканировать устройство» в empty state (или переименовать empty-state CTA в «Сканировать папку…» для единообразия — опционально в apply).

### 4. Inline translate
**Решение:** Перенести translate trigger внутрь `supportingContent` `ListItem`: после summary/`Ещё` добавить компактный `TextButton` с `contentPadding = PaddingValues(0.dp)` и `style = labelSmall`, цвет primary/onSurfaceVariant. Убрать внешний `Column` wrapper с отдельным `TextButton` снизу.

**Альтернатива:** `IconButton` с `Translate` — менее понятно без подписи; оставить короткий текст.

### 5. Sort bar refactor без смены API
**Решение:** Разбить `WorkListSortBar` на:
- `SortIconButton` + dropdown (как сейчас) — показывается только при `sortExpanded`.
- Вызовы `onSort`/`onDirection` без изменений.

На экранах результатов фильтра (`FilterResultsRoute`) можно оставить sort bar видимым или тоже свернуть — для консистентности применить тот же compact pattern только на `LibraryRoute` (main shelf), чтобы не раздувать scope.

### 6. FilterBuilder: подтверждение без обязательных полей
**Решение:** Кнопка «Показать результаты» при пустом selection — `Snackbar` «Выберите хотя бы один критерий» или disabled state с подсказкой. Логика `LibraryFilterCriteria` не меняется.

## Risks / Trade-offs

- **[Risk] Потеря быстрого device-wide scan** → Mitigation: empty state + будущий пункт в overflow при необходимости; папка остаётся в меню.
- **[Risk] Удаление browse-маршрутов ломает тесты навигации** → Mitigation: обновить `FandomReaderNavRoutesTest`, удалить тесты catalog-only paths.
- **[Risk] Случайно затронуть LastOpened comparator при рефакторе sort UI** → Mitigation: не трогать `LibraryViewModel` sort application и DAO; smoke-тест default order до/после.
- **[Risk] Inline translate слишком мелкий для тапа** → Mitigation: min touch target 48dp через `Modifier.minimumInteractiveComponentSize()` на compact button.

## Migration Plan

1. UI refactor в `LibraryScreens.kt` + навигация в `MainActivity.kt`.
2. Обновить Compose-тесты (`WorkListComposeTest` — селектор «Перевести» может сместиться в row).
3. Прогнать `./gradlew test` и ручной smoke: фильтр с иконки, поиск/сортировка expand/collapse, translate inline, сортировка по просмотру как раньше.
4. Rollback: revert PR; данных миграции нет.

## Open Questions

- Нужен ли device-wide scan (`startDeviceScan`) где-то ещё кроме empty state, или достаточно «Сканировать папку…»? (По умолчанию: empty state оставляет device scan, top bar — нет.)
