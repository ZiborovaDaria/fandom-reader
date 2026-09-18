## Context

См. motivation в `proposal.md`. Сейчас единственный локальный вход — `ImportRoute` + `ActivityResultContracts.OpenDocument` → `ImportCoordinator.importFile`. Библиотека уже сортирует полки и фильтры (`ORDER BY title COLLATE NOCASE` / displayName). Разрешение `READ_EXTERNAL_STORAGE` (maxSdk 32) есть в манифесте; скана типичных папок нет.

Ограничения:
- Scope: только скан библиотеки (настройки перевода вне change)
- Типичные папки: Downloads, Documents, Books — не весь диск
- Скан — primary; SAF — fallback
- Не ломать classify/parse AO3/Ficbook; фильтры по `canonicalKey`

## Goals / Non-Goals

**Goals:**
- `DeviceLibraryScanner` (или эквивалент), который находит `.epub`/`.fb2` в типичных папках
- Batch-импорт через `ImportCoordinator` с прогрессом и partial failure
- UI библиотеки: primary «Сканировать», secondary «Импорт»
- Корректный запрос storage/media permission под целевые API
- Дедуп повторных сканов (path fingerprint и/или remote identity upsert)

**Non-Goals:**
- Рекурсивный обход всего внешнего хранилища / SD card root
- MediaStore-индексация произвольных MIME по всему устройству как primary
- Удаление SAF-импорта
- Автоперевод после скана
- Фоновый периодический скан без действия пользователя (можно later)

## Decisions

### 1. Типичные корни: Environment public dirs
Сканировать:
- `Environment.DIRECTORY_DOWNLOADS`
- `Environment.DIRECTORY_DOCUMENTS`
- `Environment.DIRECTORY_BOOKS` (если API/устройство отдаёт)

Рекурсия: ограниченная глубина (например 3) внутри каждой типичной папки, чтобы поймать подпапки вроде `Books/AO3`, без обхода всего дерева.

**Alternatives:** только MediaStore query (проще permissions на новых API, но хуже контроль «типичных папок» и FB2 MIME); SAF tree picker один раз (не «сразу все файлы» без шага пользователя).

### 2. Переиспользовать ImportCoordinator
Добавить `importFiles(files: List<File>): ScanResult` (или цикл снаружи) поверх существующего `importFile`. Classify/parse/shelf без дублирования логики.

Дедуп:
- если есть `source` + `remoteId` — существующий `upsertWork`
- дополнительно fingerprint по нормализованному абсолютному path / size+name, чтобы Other-файлы без remoteId не плодили копии при повторном скане

**Alternatives:** отдельный batch pipeline (лишняя сложность).

### 3. Permissions
- API ≤ 32: `READ_EXTERNAL_STORAGE` (уже в манифесте) + runtime request
- API 33+: `READ_MEDIA_IMAGES` не подходит; для документов — предпочтительно чтение известных public dirs где доступно, иначе запрос `READ_MEDIA_*` не покрывает EPUB; на 33+ использовать доступ к public Downloads/Documents через существующие пути приложения + при отказе предложить SAF fallback / `MANAGE_EXTERNAL_STORAGE` **не** требовать (слишком тяжёлый UX)

Практически: runtime permission где нужно; если типичная папка нечитаема — показать понятную ошибку и оставить SAF.

**Alternatives:** только SAF multi-select (не соответствует «сразу все»); `MANAGE_EXTERNAL_STORAGE` (Play policy / UX риск).

### 4. UI в library
- Primary action в TopAppBar / empty state: «Сканировать»
- Secondary: «Импорт» (текущий маршрут)
- Индикатор прогресса (N/M) и snackbar/summary по завершении
- Опционально: автоскан при первом открытии библиотеки после grant — как усиление primary; минимум — явная кнопка

### 5. Сортировка
Не менять DAO-порядок: title NOCASE уже есть. После batch upsert Flow обновит списки отсортированными. Зафиксировать в spec как контракт.

### 6. Тесты
- Unit: scanner на temp dirs с фикстурными именами файлов; batch result counts; dedup
- Не трогать live Gemini; FakeTranslationProvider не задействован

## Risks / Trade-offs

- [На API 33+ чтение произвольных public dirs ограничено] → Mitigation: явный permission UX + SAF fallback; не обещать полный доступ ко всему диску
- [Много больших EPUB в Downloads → долгий скан] → Mitigation: IO dispatcher, прогресс, отмена; не блокировать UI
- [Дубли Other без remoteId] → Mitigation: path/size fingerprint в upsert
- [Глубокие деревья] → Mitigation: лимит глубины + только типичные корни

## Migration Plan

Нет миграции схемы Room, если fingerprint укладывается в существующий upsert (path уже в `localPath`). При необходимости — лёгкое поле `sourceFingerprint` в следующей итерации; для MVP достаточно сравнения path basename+size или remote identity.

Rollback: скрыть CTA скана / feature-flag; SAF остаётся рабочим.

## Open Questions

- Автоскан при каждом открытии библиотеки vs только по кнопке — по умолчанию кнопка + empty-state prompt; автоскан можно включить позже без смены specs (если specs требуют «primary action», не «каждый resume»).
