## 1. Persist RU-метаданных

- [x] 1.1 Добавить `valueRu` в `DisplayTag` и backward-compatible JSON codec `displayTagsJson`; проверить unit decode старых записей без `valueRu` и round-trip с `valueRu`
- [x] 1.2 В `TranslationPipeline` после перевода фэндомов вызывать UPDATE `displayRu` по `canonicalKey` (не только `insert IGNORE`); аналогично переводить и UPDATE pairing `displayRu`; проверить FakeTranslationProvider unit: после translate + reload из репозитория RU labels на месте
- [x] 1.3 Переводить blank display tags → `valueRu`, сохранять через `updateTranslation` / upsert work; UI meta/list показывает `valueRu ?: value`; проверить unit + что filter identity остаётся `(group, value)`
- [x] 1.4 В `ImportCoordinator` при rematch по `remoteId` мержить non-blank `titleRu`/`summaryRu`/`translationStatus` и не затирать существующие `displayRu`/`valueRu`; проверить unit: re-import не обнуляет RU

## 2. Комбинируемая фильтрация (domain/data)

- [x] 2.1 Ввести модель выбранных критериев (fandom keys, pairing keys, display tags по группам) и функцию AND-across / OR-within; проверить unit на фикстурных работах
- [x] 2.2 Подключить evaluate к `LibraryRepository` / ViewModel (клиентский filter поверх observe или query); проверить, что slash-bearing pairing key фильтрует без краша

## 3. Filter builder UI

- [x] 3.1 Добавить route экрана сборки фильтров и экрана результатов; вход с библиотеки; проверить навигацию filters → results без краша
- [x] 3.2 Вкладки/секции: фэндомы, пейринги, персонажи, рейтинг, предупреждения, доп. метки (и др. группы при наличии данных); multi-select + Apply; проверить, что categories не свалены в одну кучу
- [x] 3.3 Поиск substring (case-insensitive) в пикерах фэндомов, пейрингов и tab-списках тегов по EN и RU labels; проверить unit/UI: частичное слово находит запись, пустой запрос — empty state
- [x] 3.4 Результаты показывают только работы, удовлетворяющие комбинации; empty state при нуле; Reset очищает выбор; проверить сценарии fandom∧rating и OR двух пейрингов

## 4. Тема поверхностей фильтров

- [x] 4.1 Filter builder / pickers / results используют `colorScheme.background`/`surface` без hardcoded cream; проверить visually/Compose: Night → тёмный фон, Light → paper

## 5. Регрессия

- [x] 5.1 Прогнать `./gradlew test` (или затронутые module tests) с FakeTranslationProvider; убедиться, что live Gemini не требуется
- [x] 5.2 Ручной smoke BlueStacks: dual search + filters + theme + relaunch (23/23); live Gemini → force-stop → RU title/summary/tags persist (`gemini-persist-20260914`)
