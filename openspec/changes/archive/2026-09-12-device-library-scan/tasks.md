## 1. Scanner и batch-import

- [x] 1.1 Реализовать `DeviceLibraryScanner` (типичные корни Downloads/Documents/Books, расширения `.epub`/`.fb2`, глубина ≤3, отсутствующая папка не падает) и проверить unit-тестом на temp-дереве
- [x] 1.2 Добавить batch-импорт поверх `ImportCoordinator` с `ScanResult` (imported / skipped / failed) и partial failure; проверить unit: один битый файл не отменяет остальные
- [x] 1.3 Добавить дедуп повторного скана (remote identity upsert + fingerprint path/size для Other) и проверить unit: второй скан тех же файлов не создаёт лишние записи

## 2. Permissions

- [x] 2.1 Обновить манифест/runtime request для чтения типичных папок (API ≤32 `READ_EXTERNAL_STORAGE`; на 33+ без `MANAGE_EXTERNAL_STORAGE`) и проверить, что отказ в доступе показывает понятное сообщение, а не «0 книг — успех»
- [x] 2.2 После grant запускать скан; при нечитаемых папках предложить SAF fallback; проверить на эмуляторе/устройстве сценарий deny → grant → import

## 3. UI библиотеки

- [x] 3.1 Сделать «Сканировать» primary CTA на экране библиотеки (TopAppBar и/или empty state) с прогрессом N/M и summary по завершении; проверить Compose/ручной прогон
- [x] 3.2 Оставить «Импорт» secondary → существующий SAF `ImportRoute`; проверить, что одиночный выбор файла по-прежнему импортирует
- [x] 3.3 Убедиться, что после batch-импорта списки полок отсортированы по title (NOCASE) и фильтры фэндом/пейринг по `canonicalKey` не сломаны; проверить запросом/unit на Room или UI-список

## 4. Проверка

- [x] 4.1 Добавить/обновить пункты в `docs/analysis/e2e-checklist.md`: скан Downloads → книги на полках; повторный скан без дублей; SAF fallback
- [x] 4.2 Прогнать `./gradlew test` и убедиться, что тесты зелёные без live-сети
