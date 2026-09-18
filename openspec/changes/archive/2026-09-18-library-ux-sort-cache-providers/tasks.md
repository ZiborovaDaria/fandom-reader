## 1. Data model: sort fields and preferences

- [x] 1.1 Добавить в Room `lastOpenedAt` и `fileSizeBytes` (+ migration), обновить domain `Work` и маппинг; проверить миграцию на чистой и существующей БД
- [x] 1.2 При import/scan заполнять `fileSizeBytes`; при успешном open reader обновлять `lastOpenedAt`; проверить unit/fake repository
- [x] 1.3 Добавить DataStore prefs `WorkSort` + `SortDirection` (default LastOpened + Descending) и API чтения/записи; проверить persist после restart

## 2. Library list sort and scroll restore

- [x] 2.1 Провести сортировку work lists через repository/DAO по активным prefs (title через `titleRu ?: title` + Collator ru; size; lastOpened); проверить порядок unit-тестами на смешанных RU/EN titles
- [x] 2.2 Добавить UI выбора критерия и направления на library work lists; проверить, что смена sort сразу перестраивает список
- [x] 2.3 Восстанавливать scroll к `workId` после возврата из reader/meta; проверить, что после back открытая книга снова на экране без скролла с начала

## 3. Filter stability and translate button

- [x] 3.1 Исправить `FilterBuilderScreen`: не сбрасывать tab/selection на каждый Flow emit; стабильные keys у строк; проверить, что тап по фэндому/пейрингу/метке надёжно переключает checkbox без мерцания
- [x] 3.2 Перенести «Перевести» / «Перевести заново» под title/summary в `WorkListRow` (убрать trailing); проверить визуально на AO3 row с PENDING

## 4. Reader chapter cache and Ficbook extract

- [x] 4.1 Ввести process-scoped LRU cache распарсенных глав с ключом file identity; `ReaderViewModel.load` использует cache hit без полного re-parse; проверить повторный open той же книги
- [x] 4.2 Инвалидировать cache при смене файла (`path`/`length`/`lastModified`); проверить, что после замены файла текст обновляется
- [x] 4.3 Воспроизвести/зафиксировать Ficbook empty/`***` на реальном или synthetic EPUB; починить `ChapterText` extract; добавить regression unit test, что narrative не схлопывается в пусто/`***`

## 5. Multi-provider settings and temporary Cursor

- [x] 5.1 Обобщить encrypted key storage (Gemini + Cursor + placeholder slots) с миграцией существующего Gemini key; проверить save/clear/mask без логов ключа
- [x] 5.2 Обновить Settings UI: слоты, selector активного провайдера (Gemini|Cursor), пометка Cursor temporary; проверить need-key snackbar для выбранного провайдера без ключа
- [x] 5.3 Реализовать временный `CursorProvider` (chat prompt EN→RU, configurable base URL/model) и resolve в `ResolvingTranslationProvider` at request time; unit tests только с Fake; smoke: Cursor+key → pipeline не падает на MissingApiKey

## 6. Verification

- [x] 6.1 Прогнать `./gradlew test` (как минимум translation, reader ChapterText, library sort/filter) и убедиться, что тесты зелёные без live Gemini/Cursor
- [x] 6.2 Ручной smoke: sort default last-opened; reopen без тяжёлого re-parse; back → та же книга в списке; фильтры без моргания; Ficbook глава с текстом; кнопка перевода снизу
