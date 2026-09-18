## 1. Format detection shared helper

- [x] 1.1 Добавить утилиту детекта формата книги по имени / MIME / magic bytes (`epub` | `fb2` | `unknown`) в подходящем shared-модуле (`:core:data` или `:source:api`) и покрыть unit-тестами: ZIP `PK` + epub mimetype → epub; FictionBook/XML → fb2; мусор → unknown
- [x] 1.2 Подключить детект в `EpubIo.formatOf` / путь классификации так, чтобы отсутствие расширения не блокировало ZIP/FB2 sniff; проверить существующими или новыми unit-тестами classifier

## 2. SAF import filename + format

- [x] 2.1 В `ImportScreen` (или helper рядом) при копировании SAF URI читать `OpenableColumns.DISPLAY_NAME`, MIME и нормализовать имя temp-файла с `.epub`/`.fb2`; проверить unit/instrumentation fake resolver или ручным сценарием «URI без расширения → корректный temp»
- [x] 2.2 Прогнать импорт AO3/Ficbook fixture через путь без расширения в имени и убедиться (тест или assert в coordinator), что source ≠ OTHER при наличии маркеров портала

## 3. Device scan under scoped storage

- [x] 3.1 Реализовать MediaStore (или эквивалентный) discovery `.epub`/`.fb2` для типичных локаций и объединить кандидатов с `DeviceLibraryScanner` / File listing с дедупом; unit-тест на merge/dedup списка
- [x] 3.2 Обновить `LibraryViewModel.runScan` / `TypicalBookFolders.accessState`: не выдавать «успешный пустой скан», когда листинг заведомо недостоверен; оставить SAF fallback; проверить тексты toast/snackbar на API 33+ логике (тест состояния или ручной чеклист)
- [x] 3.3 На устройстве/эмуляторе с файлами в `Download` подтвердить, что «Сканировать» находит `.fb2`/`.epub` (ручной шаг; зафиксировать в отчёте apply)

## 4. Reader binary dump fix

- [x] 4.1 Изменить `loadChapters`: при неизвестном format детектировать epub/fb2 и парсить; иначе показывать ошибку/unsupported, **не** `readText` всего бинарника; unit-тест на EPUB без format=`epub` (ожидать главы, не `PK` в тексте)
- [x] 4.2 Добавить/обновить тест, что unsupported файл не попадает в UI как сырой бинарный текст

## 5. Library top bar UX

- [x] 5.1 Переделать `LibraryRoute` TopAppBar actions: «Сканировать» остаётся на виду; Каталог / Импорт / Настройки — overflow (⋮) или IconButtons так, чтобы на узкой ширине Настройки были достижимы; визуально проверить на phone-width preview/устройстве
- [x] 5.2 Убедиться, что empty-state ссылки «Сканировать устройство» / «Импорт файла» по-прежнему работают после правки топбара

## 6. Verification

- [x] 6.1 `./gradlew test` (или целевые module tests) — зелёные для затронутых модулей
- [x] 6.2 `./gradlew :app:assembleDebug` успешно собирает APK для выкладки на телефон
