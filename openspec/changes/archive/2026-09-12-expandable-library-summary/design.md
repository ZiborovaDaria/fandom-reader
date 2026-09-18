## Context

См. `proposal.md` — Why. Сейчас `WorkList` в `LibraryScreens.kt` рендерит аннотацию так:

```kotlin
Text((work.summaryRu ?: work.summary).orEmpty().take(220))
```

Обрезка по символам даёт обрыв на середине слова; полного текста в списке нет. `WorkList` общий для полок «Фанфики», «Прочее» и filtered-списков — правим один composable.

## Goals / Non-Goals

**Goals:**

- Свёрнутое превью = 440 символов (ровно ×2 от текущего 220)
- Inline expand/collapse полного `summaryRu ?: summary` в строке
- Кнопка разворота не открывает книгу (не вызывает `onOpenWork`)
- Регрессия Compose UI-тестом

**Non-Goals:**

- Отдельный экран/диалог только для аннотации
- Переписывать `ListItem` на другую карточку/layout-систему
- Менять данные Room / перевод / парсеры
- Переходить на `maxLines`+`TextOverflow` как единственный критерий (символьный бюджет сохраняем для детерминированных тестов; визуально ≈ удвоение)

## Decisions

### 1. Остаться на символьном бюджете, удвоить константу

- **Выбор:** `COLLAPSED_SUMMARY_MAX = 440` (было `take(220)`).
- **Почему:** текущая реализация и тесты завязаны на текст; ×2 буквально совпадает с запросом; детерминированно в Compose UI test без layout measurement.
- **Альтернативы:** `maxLines = 4` + `TextOverflow.Ellipsis` + `onTextLayout` для детекта overflow — лучше визуально, но сложнее в unit/Compose test; можно позже, не в этом change.

### 2. Локальный expand state на строку

- **Выбор:** в item composable `var expanded by remember(work.id) { mutableStateOf(false) }`; при `expanded` показывать полный текст, иначе `take(440)`.
- **Почему:** не нужен ViewModel; state сбрасывается при уходе с экрана — приемлемо для списка.
- **Альтернатива:** хранить `Set<Long>` expanded ids в VM — избыточно для MVP.

### 3. Контрол «Ещё» / «Свернуть»

- Показывать `TextButton` (или `ClickableText`) только если `full.length > COLLAPSED_SUMMARY_MAX`.
- Подписи RU: «Ещё» / «Свернуть».
- Кнопка в `supportingContent` рядом с текстом аннотации, под превью.

### 4. Hit-testing: expand не открывает книгу

- Сейчас `Modifier.clickable { onOpenWork(work.id) }` на всём `ListItem`.
- **Выбор:** на кнопке expand/collapse использовать вложенный `clickable`/`TextButton`, который поглощает жест (стандартное поведение Compose для вложенных clickable), либо вынести `onOpenWork` с корня на `headlineContent` / область заголовка.
- **Проверка:** Compose UI test — клик по «Ещё» показывает полный текст; `onOpenWork` не вызван.

### 5. Пустое summary

- Как сейчас: пустая строка / отсутствие текста; отдельный placeholder не обязателен, если уже не показывается в UI; не ломать сценарий missing summary из спеки (title остаётся).

## Risks / Trade-offs

- [Очень длинные аннотации раздувают LazyColumn item] → Mitigation: expand по запросу; по умолчанию 440 символов
- [Вложенный clickable на ListItem не сработает на части устройств/API] → Mitigation: при провале теста перенести `onOpenWork` только на headline
- [Обрезка по символам всё ещё режет слово] → Mitigation: принято как trade-off vs `maxLines`; полный текст доступен по «Ещё»

## Migration Plan

- Чистый UI-change, миграции данных нет
- Rollback: вернуть `.take(220)` без кнопки

## Open Questions

- (нет — бюджет 440 и подписи «Ещё»/«Свернуть» зафиксированы здесь)
