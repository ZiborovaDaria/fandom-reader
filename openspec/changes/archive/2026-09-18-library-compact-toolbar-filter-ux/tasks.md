## 1. Навигация и удаление каталога

- [x] 1.1 Заменить `onOpenCatalog` на `onOpenFilter` в `LibraryRoute` и `MainActivity`: `library` → `filters` напрямую; проверить, что иконка фильтра открывает `FilterBuilderRoute` без экрана «Каталог»
- [x] 1.2 Удалить composable `catalog` и пункт overflow «Каталог»; убрать browse entry points (`fandoms`, `browse-romantic`, `browse-platonic`, `browse-tags`) из `NavHost`; проверить, что в UI нет путей к drill-down каталогу
- [x] 1.3 Удалить или пометить unused `CatalogRoute` и связанные browse routes, если больше не вызываются; обновить `FandomReaderNavRoutesTest` и убедиться, что `./gradlew :app:testDebugUnitTest --tests "*NavRoutes*"` проходит

## 2. Компактный toolbar (поиск, сортировка, фильтр)

- [x] 2.1 Убрать `TextButton("Сканировать")` с top bar; оставить «Сканировать папку…» в overflow; проверить визуально, что дублирующей кнопки нет
- [x] 2.2 Добавить иконку «Фильтр» в `TopAppBar.actions` с `contentDescription` «Фильтр»; проверить доступность и переход на `filters`
- [x] 2.3 Свернуть поиск: иконка `Search` + `AnimatedVisibility` для полей title/summary (убрать постоянный `TextButton` «Поиск»); проверить expand/collapse и работу `vm.setTitleQuery` / `setSummaryQuery`
- [x] 2.4 Свернуть сортировку: иконка `Sort` + expand панели с текущим dropdown `WorkListSortBar`; проверить, что критерии и направления те же, что до рефактора
- [x] 2.5 Убедиться, что смена sort через новый UI вызывает те же `vm.setSort` / `setSortDirection` и prefs persist; проверить default «По просмотру · новые→старые» после cold start

## 3. Inline «Перевести» в строке книги

- [x] 3.1 Перенести translate trigger в `supportingContent` `WorkListRow` как компактный inline-контрол; убрать отдельный `TextButton` под `ListItem`; проверить `WorkListComposeTest.translateAsksConfirmationBeforeSending`
- [x] 3.2 Сохранить диалог подтверждения и стили «Перевести» / «Перевести заново»; проверить визуально на AO3 row, что кнопка не выглядит отдельным блоком

## 4. Расширенный фильтр (без смены логики)

- [x] 4.1 Подтвердить, что `FilterBuilderRoute` оставляет вкладки по категориям меток и не требует заполнения всех полей; при пустом confirm — Snackbar/disabled с подсказкой; проверить сценарии: только фэндом, только метка, комбинация AND
- [x] 4.2 Не менять `LibraryFilterCriteria` encode/decode и `canonicalKey` фильтрацию; проверить filter-results с pairing key содержащим `/`

## 5. Сортировка «по просмотру» — регрессия

- [x] 5.1 Не трогать comparator/DAO для `WorkSort.LastOpened`; добавить или обновить unit/compose smoke: список с разными `lastOpenedAt` в default sort совпадает с порядком до change; проверить `./gradlew test` зелёный
- [x] 5.2 Ручной smoke: открыть книгу → назад → порядок «по просмотру» и позиция списка без изменений; смена направления last-opened работает как раньше

## 6. Финальная проверка

- [x] 6.1 Прогнать `./gradlew test` и `./gradlew :app:assembleDebug`; убедиться, что CI-релевантные тесты зелёные без live Gemini
- [x] 6.2 Ручной smoke на устройстве/эмуляторе: toolbar иконки, фильтр, поиск, сортировка, inline translate, scan только через overflow/empty state
