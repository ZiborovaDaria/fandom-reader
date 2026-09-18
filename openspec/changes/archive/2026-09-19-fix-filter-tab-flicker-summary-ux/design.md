## Context

См. `proposal.md`. Сейчас `FilterBuilderRoute` держит `selectedTabIndex: Int` и пересчитывает `availableTabs` из `works.flatMap { displayTags }` на каждый emit Flow. Когда набор групп тегов «прыгает», тот же индекс указывает на другую вкладку (типичный срыв: «Категории» → «Пейринги»). Частичный фикс в `library-ux-sort-cache-providers` убрал `remember(availableTabs)` для state, но не устранил сдвиг индекса.

`WorkListRow` использует `FlowRow` с отдельными `Text` для preview, «Ещё» и «Перевести» — «Ещё» визуально оторвано от обрыва текста и стоит рядом с переводом.

## Goals / Non-Goals

**Goals:**
- Стабильная вкладка и selection в FilterBuilder при поиске и фоновых обновлениях библиотеки.
- Убрать вкладку «Размер» из фильтра.
- Inline «Ещё» у последнего символа preview; «Перевести» на отдельной строке или с явным отступом.

**Non-Goals:**
- Менять семантику `LibraryFilterCriteria` / AND-OR фильтрации.
- Убирать группу `size` с экрана метаданных работы.
- Переписывать навигацию или toolbar библиотеки.

## Decisions

### 1. Идентификация вкладки по `FilterTab`, не по index
- Хранить `var selectedTab: FilterTab` (default `FANDOMS` или первая доступная).
- `ScrollableTabRow.selectedTabIndex` вычислять как `availableTabs.indexOf(selectedTab).coerceAtLeast(0)`.
- При смене `availableTabs`: если `selectedTab` исчез — fallback на ближайшую соседнюю или `FANDOMS`, **без** сброса `pickerQuery` и selection maps.
- **Alternative:** только `LaunchedEffect` на lastIndex — недостаточно, не ловит смену вкладки при том же size.

### 2. Стабильный каталог вкладок display-tags
- Вынести distinct tag groups + tag lists в `LibraryViewModel` как `StateFlow`/`Flow`, обновляемый из repository при изменении works, с `distinctUntilChanged` по набору групп.
- `availableTabs` для tag-вкладок: фиксированный порядок enum (без `SIZE`), показывать вкладку если группа есть в каталоге **или** пользователь уже выбрал теги этой группы (чтобы selection не исчезал).
- Список тегов внутри вкладки фильтровать по `pickerQuery` локально; не пересоздавать `LazyColumn` keys без нужды.
- **Alternative:** `rememberSaveable` только в UI — не решает сдвиг availableTabs.

### 3. Selection state без ключей на Flow
- `selectedFandoms`, `selectedPairings`, `selectedTags`, `pickerQuery` остаются в `remember` без зависимости от `works`.
- Опционально поднять в ViewModel scoped к back stack entry фильтра — только если Compose-тесты покажут recomposition reset; начать с локального `remember`.

### 4. Удаление `FilterTab.SIZE`
- Удалить enum entry и mapping `DisplayTagGroups.SIZE` в `FilterBuilderScreen`.
- Не трогать `WorkMetaScreen` / parsers.

### 5. Inline summary + «Ещё» + «Перевести»
- Заменить `FlowRow` на:
  - один `Text` с `AnnotatedString` + `ClickableText` / `Text` + inline `LinkAnnotation` для «Ещё» сразу после preview-текста;
  - обрезка preview: `take(COLLAPSED_SUMMARY_MAX)` с откатом к последнему пробелу/переносу в пределах ~20 символов от лимита;
  - «Перевести» — отдельный `Text`/`TextButton` на следующей строке (`Column`) или с `padding(top = 4.dp)` вне inline-блока summary.
- Сохранить semantics/contentDescription для a11y («Ещё», «Перевести»).
- **Alternative:** `BasicText` + `InlineTextContent` — сложнее; `AnnotatedString` достаточно.

## Risks / Trade-offs

- [Вкладка остаётся для пустой группы после удаления всех книг с тегами] → Mitigation: скрывать только если нет тегов в каталоге **и** нет selection в группе.
- [Word-boundary truncate короче 440 символов] → Mitigation: fallback на hard cut если пробел не найден в окне.
- [Compose inline click конфликтует с row click] → Mitigation: отдельные clickable spans с `stopPropagation`; тесты на `openWorkCalls == 0` при «Ещё».

## Migration Plan

1. UI-only change; миграций БД нет.
2. Обновить/добавить Compose-тесты; прогнать `./gradlew :feature:library:test`.
3. Rollback: revert PR без data migration.

## Open Questions

- (нет) — поведение согласовано с запросом пользователя.
