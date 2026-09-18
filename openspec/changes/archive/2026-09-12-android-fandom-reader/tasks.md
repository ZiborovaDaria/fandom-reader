## 1. Проектный каркас и правила агента

- [x] 1.1 Создать Gradle multi-module scaffold в `fandom-reader` (модули из design.md, minSdk 26) и проверить, что `:app` собирается (`./gradlew :app:assembleDebug`)
- [x] 1.2 Добавить `AGENTS.md` и `.cursor/rules/*.mdc` (Compose, architecture, sources, translation, reader UX, testing) и проверить, что файлы на месте и читаются из корня проекта
- [x] 1.3 Разложить `fixtures/` (ao3/ficbook/other + mt-samples) и `docs/analysis/` по эталонным образцам; проверить наличие `Next_Best_Thing.epub` и хотя бы одного Ficbook EPUB в fixtures
- [x] 1.4 Добавить Version Catalog + Hilt/KSP/Compose зависимости в каталог и проверить sync Gradle без ошибок

## 2. Domain и хранилище библиотеки

- [x] 2.1 Реализовать domain-модели Work/Fandom/Pairing (canonicalKey, displayRu, relationship type) в `:core:domain` и проверить unit-тестом равенства canonicalKey
- [x] 2.2 Создать Room-схему и DAO (M:N фэндом/пейринг, уникальность source+remoteId) в `:core:data` и проверить in-memory insert/query тест
- [x] 2.3 Реализовать репозиторий полок Fanfiction/Other и фильтр fandom→pairing; проверить unit-тестом сценарии из `fanfic-library` spec

## 3. Импорт и парсеры порталов

- [x] 3.1 Реализовать `source:api` (classify/parse/store контракт) и проверить FakeSource в unit-тесте
- [x] 3.2 Реализовать AO3 EPUB parser (publisher/URL + preface Fandom/Relationships `/` vs `&`) и проверить golden JSON на `Next_Best_Thing.epub`
- [x] 3.3 Реализовать Ficbook EPUB parser (`ficbook.net` + title.xhtml labels) и проверить golden JSON на одном из трёх Ficbook EPUB
- [x] 3.4 Реализовать Other + best-effort AO3 FB2 recovery и проверить: ordinary→Other; damaged FB2→AO3 без crash (фикстуры из samples)
- [x] 3.5 Собрать import use-case (файл → classify → persist → shelf) и проверить интеграционным тестом на AO3 и Ficbook фикстурах

## 4. UI библиотеки

- [x] 4.1 Экраны полок Fanfiction/Other и список title+summary (Compose) и проверить Compose UI test: две полки видны, строка показывает title+summary
- [x] 4.2 Навигация фэндом → пейринг → список и проверить UI/unit тест фильтрации по выбранному пейрингу из фикстуры
- [x] 4.3 Экран/флоу выбора файла для импорта (SAF) и проверить ручным сценарием импорт `Next_Best_Thing.epub` в библиотеку

## 5. Перевод AO3

- [x] 5.1 Ввести `TranslationProvider` + `FakeTranslationProvider` и pipeline title/summary/chapters с сохранением canonical tags; проверить unit-тест, что фильтр не дублируется после «перевода»
- [x] 5.2 Реализовать `GeminiProvider` (Flash-Lite, ключ в encrypted settings, chunking/backoff) и проверить ручным вызовом перевод summary из `Next_Best_Thing`
- [x] 5.3 Кэш перевода по главам + resume job и проверить: повторный запуск пропускает уже переведённую главу (unit/integration с Fake)
- [x] 5.4 UI статуса перевода на карточке работы и запуск перевода; проверить, что после перевода список показывает summaryRu
- [x] 5.5 Скрипт/док `tools/mt-bakeoff` для среза Next Best Thing (Gemini vs заметки) и сохранить scorecard в `docs/analysis/`

## 6. Immersive reader

- [x] 6.1 Открытие локального EPUB офлайн с прогрессом и проверить resume позиции при повторном открытии
- [x] 6.2 Поддержка FB2 в reader MVP и проверить открытие фикстуры FB2 офлайн
- [x] 6.3 Immersive chrome (текст по умолчанию, controls по тапу) и проверить Compose/UI тест или чеклист: нет постоянного toolbar/FAB
- [x] 6.4 Показ `textRu` по умолчанию при наличии перевода и fallback на оригинал; проверить unit/UI сценарии из `immersive-reader` spec

## 7. Стабилизация

- [x] 7.1 Прогнать все unit-тесты парсеров/библиотеки/перевода (`./gradlew test`) и убедиться, что CI-локально зелёные без сети Gemini
- [x] 7.2 Ручной E2E чеклист: импорт AO3 → перевод summary/ch1 → фильтр пейринга; импорт Ficbook → фэндом/пейринг; Other-файл → полка Прочее; чтение immersive
- [x] 7.3 Удалить временный `_probe/` из рабочей папки (если остался) и проверить, что fixtures/docs не содержат мусора разбора
