## Why

На реальном телефоне (API 33+, `targetSdk` 35) скан типичных папок сообщает «файлы не найдены», хотя в `Download` лежат `.fb2`/`.epub`: `File.listFiles()` даёт пустой список при ложном `READY`. Ручной SAF-импорт часто сохраняет temp без расширения → классификация/полка Other и ридер делает `readText` по ZIP («кракозябры»). Верхняя панель библиотеки с четырьмя текстовыми actions обрезается — «Настройки» недоступны.

## What Changes

- Скан на API 33+: обнаруживать EPUB/FB2 в public Download/Documents/Books через MediaStore (и/или иной доступный listing), а не полагаться только на `File.listFiles()`; различать «доступ есть, но листинг пуст из‑за scoped storage» vs «реально пусто»; сохранять SAF fallback.
- SAF-импорт: брать `OpenableColumns.DISPLAY_NAME` / MIME / magic bytes; нормализовать temp к `.epub`/`.fb2`; классифицировать и парсить по содержимому, не только по расширению имени.
- Ридер: не рендерить бинарный ZIP как UTF-8 текст; при неизвестном `format` детектировать EPUB/FB2 по magic и открывать через существующие парсеры.
- Топбар библиотеки: все действия (Каталог, Сканировать, Импорт, Настройки) доступны на узком экране (overflow menu / иконки / scrollable actions).
- Полка «Прочее» остаётся для файлов без portal-метаданных; исправление касается ложного Other из‑за потери формата/расширения.

## Capabilities

### New Capabilities

- (нет)

### Modified Capabilities

- `device-library-scan`: надёжное обнаружение на scoped storage (API 33+), корректная диагностика пустого результата vs недоступного листинга.
- `portal-import`: определение формата и отображаемого имени при SAF-импорте; sniff по содержимому для классификации.
- `immersive-reader`: запрет показа сырого бинарного контейнера; fallback детекта EPUB/FB2.
- `fanfic-library`: доступность действий топбара (включая Настройки) на узкой ширине.

## Impact

- Код: `TypicalBookFolders`, `LibraryViewModel`/`LibraryScreens` (скан + TopAppBar), `ImportScreen` (SAF copy), `DefaultBookClassifier` / `OtherMetaParser` / `EpubIo`, `loadChapters` в `ReaderScreen`.
- Манифест: возможны `READ_MEDIA_*` или MediaStore без broad storage; **не** добавлять `MANAGE_EXTERNAL_STORAGE` как primary.
- Тесты: unit на magic/DISPLAY_NAME/format fallback; сценарии «listing empty на File API»; UI-тест/ручная проверка overflow топбара.
- Не меняет модель полок Fanfiction vs Other по смыслу метаданных; чинит ложный путь Other + кракозябры.
