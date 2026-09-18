## 1. Provider resolver и DI

- [x] 1.1 Заменить one-shot bind в `AppModule.translationProvider` на runtime-resolver (чтение ключа на каждый запрос → Gemini или отказ) и проверить unit-тестом: пустой ключ после «сохранения» не вызывает Gemini; непустой ключ после старта с пустым — делегирует в Gemini mock/fake double
- [x] 1.2 Убрать тихий Fake из production UI-пути (Fake только в unit-тестах / явной test DI) и проверить, что `./gradlew :feature:translation:test` зелёный на FakeTranslationProvider

## 2. Экран настроек ключей

- [x] 2.1 Добавить Settings screen (Material 3): поле Gemini API key (masked), Save / Clear, индикатор «ключ задан»; проверить ручным открытием экрана без краша
- [x] 2.2 Подключить save/clear к `GeminiSettings` (encrypted prefs) и проверить: после Save ключ читается в resolver; после Clear — пустой; ключ не пишется в Logcat
- [x] 2.3 Добавить nav route `settings` и пункт «Настройки» в TopAppBar библиотеки; проверить навигацию library → settings → back

## 3. UX перевода без ключа и с ключом

- [x] 3.1 При «Перевести» без ключа показать сообщение + переход в Settings (не сохранять `[RU]…` как успех) и проверить сценарий на устройстве/эмуляторе
- [x] 3.2 При наличии ключа `startTranslation` выполняет metadata через neural path и обновляет `titleRu`/`summaryRu`/status; проверить unit + что статус уходит с NONE

## 4. Перевод глав и ридер

- [x] 4.1 После metadata в том же job последовательно вызывать `translateChapter` для доступных глав (PARTIAL → COMPLETE), с resume по кэшу; проверить unit на skip уже закэшированной главы
- [x] 4.2 В ридере показывать кэшированный RU текст главы при наличии файла кэша; проверить на импортированном AO3 work после перевода хотя бы одной главы
- [x] 4.3 Перевести blocking Gemini `execute`/`Thread.sleep` на IO-диспетчер + coroutine delay (без смены API) и убедиться, что unit-тесты pipeline не требуют сети

## 5. Документация проверки

- [x] 5.1 Обновить `docs/analysis/e2e-checklist.md`: шаги Settings → ключ → Translate → summaryRu + chapter cache; отметить отличие от Fake
- [x] 5.2 Прогнать `./gradlew test` и зафиксировать, что CI-локально зелёный без live Gemini
