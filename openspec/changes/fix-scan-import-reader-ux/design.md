## Context

См. `proposal.md` — Why. Наблюдаемое состояние кода:

- Скан: `TypicalBookFolders` + `File.listFiles()`; на API 33+ permission не запрашивается; `canRead()==true` при пустом листинге → toast «не найдены».
- Импорт: `ImportScreen` копирует URI с именем из `lastPathSegment` → часто без `.epub`/`.fb2`.
- Классификация/формат завязаны на расширение; `loadChapters` для unknown делает `file.readText`.
- `LibraryRoute` TopAppBar: четыре `Text` в `actions` без overflow.

Ограничения: не использовать `MANAGE_EXTERNAL_STORAGE` как primary (Play policy); фильтры по `canonicalKey`; FakeTranslationProvider в тестах; не копировать GPL reader-код.

## Goals / Non-Goals

**Goals:**

- Находить EPUB/FB2 в public Download/Documents/Books на API 33+ при scoped storage.
- Корректный SAF → format → classify → reader без ZIP-дампа.
- Все действия топбара библиотеки доступны на узком экране.

**Non-Goals:**

- Полный редизайн библиотеки / тема.
- Автоперевод метаданных или смена правил Fanfiction vs Other для валидных non-portal книг.
- Рекурсивный скан всего устройства или cloud providers.
- Включение broad all-files access как основного пути.

## Decisions

### 1. Скан: MediaStore primary для API 33+, File API оставить как дополнение

- **Выбор:** на API 33+ (и как fallback при пустом File listing) запрашивать `MediaStore` (Downloads/Documents и файлы с MIME `application/epub+zip`, `application/x-fictionbook+xml` / расширение `.epub`/`.fb2`), объединять с результатами `DeviceLibraryScanner` по стабильному id (path|uri + size).
- **Почему:** MediaStore видит media-сканируемые файлы в Download без all-files permission; File API уже работает на части эмуляторов/старых API.
- **Альтернативы:** только `OpenDocumentTree` persist — хуже UX (каждый раз выбирать папку); `MANAGE_EXTERNAL_STORAGE` — отклонено политикой.

### 2. Диагностика пустого скана

- **Выбор:** если File listing пуст на всех roots, а MediaStore тоже 0 — показывать текущее «не найдены» + Импорт; если File «readable» но MediaStore недоступен/ошибка — сообщение про ограниченный доступ + SAF. Не вводить ложный NEED_PERMISSION на API 33+ без реального permission.
- **Альтернативы:** всегда требовать tree SAF — лишний friction для happy path.

### 3. SAF: DISPLAY_NAME + MIME + magic → нормализованный temp

- **Выбор:** helper `resolveImportFile(uri)` читает `OpenableColumns.DISPLAY_NAME`, MIME, первые байты (`PK\x03\x04` → epub; XML/`FictionBook` → fb2); пишет temp с корректным суффиксом.
- **Почему:** чинит classify + reader одним местом на входе.
- **Альтернативы:** только MIME — ненадёжно у провайдеров; только magic после copy — ок как второй этап, но имя всё равно нужно.

### 4. Content sniff в classifier / formatOf

- **Выбор:** `DefaultBookClassifier` / `EpubIo.formatOf` при отсутствии расширения открывают поток/ZIP по magic; AO3/Ficbook sniff без требования endsWith `.epub`.
- **Почему:** защищает и скан, и повторное открытие уже сохранённых wrong-named файлов.

### 5. Reader: magic fallback, запрет raw dump

- **Выбор:** `loadChapters`: если format не epub/fb2 — детект по файлу; при ZIP-EPUB/FB2 парсить; иначе одна глава-ошибка / empty с UI-сообщением, **не** `readText` всего файла.
- **Почему:** скриншот с `PK`/`mimetypeapplication/epub+zip` закрывается даже для уже импортированных записей.

### 6. TopAppBar: Overflow menu

- **Выбор:** primary «Сканировать» оставить видимым (TextButton или Icon+label); Каталог / Импорт / Настройки — в `DropdownMenu` (⋮) или IconButtons с contentDescription. На широких экранах можно оставить ряд.
- **Почему:** минимальный Compose-фикс без новой навигации.
- **Альтернативы:** только horizontal scroll — Settings всё ещё можно не заметить; bottom bar — больший UX-сдвиг.

## Risks / Trade-offs

- [MediaStore не индексирует свежий файл] → Mitigation: после пустого MediaStore предложить SAF; опционально подсказка «обновить медиа» не обязательна в v1.
- [Уже импортированные wrong-format записи в Room] → Mitigation: reader magic fallback; опциональный one-shot reclassify out of scope unless cheap.
- [READ_MEDIA_* на части OEM] → Mitigation: MediaStore query часто работает без grant для чужих Downloads; если нужен permission — запросить `READ_MEDIA_IMAGES` не подходит; для документов обычно query без photo permission. Проверить на target device; при необходимости document URI copy через MediaStore `openFileDescriptor`.
- [Overflow прячет Импорт] → Mitigation: пустое состояние библиотеки уже имеет «Импорт файла»; держать Scan на виду.

## Migration Plan

- Чисто клиентский апдейт; миграция БД не требуется для happy path.
- Уже сломанные импорты: открытие через reader magic; пользователь может удалить и переимпортировать при желании.
- Rollback: откат APK; данные библиотеки совместимы.

## Open Questions

- Нужен ли one-shot reclassify существующих `format=unknown` в Room при старте — отложено; reader fallback закрывает UX.
