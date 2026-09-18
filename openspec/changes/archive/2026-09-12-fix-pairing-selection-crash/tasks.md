## 1. Nav helpers

- [x] 1.1 Добавить encode/decode helper для path-аргументов навигации (кодирует `/` и пробелы) в `:app` или `:core:ui` и проверить unit-тестом round-trip для ключей вроде `harry-potter/tom-riddle` и `harry potter/tom riddle`
- [x] 1.2 Убедиться, что helper не живёт в `:core:domain` (нет Android/`Uri` в domain) — проверка по зависимостям модуля

## 2. Navigation fix

- [x] 2.1 В `FandomReaderNav` кодировать `fandomKey` при переходе на `pairings/…` и декодировать при чтении args; проверить, что список пейрингов открывается для fandom key со спецсимволами
- [x] 2.2 Кодировать `fandomKey` и `pairingKey` при `navigate("filtered/…")` и декодировать оба при чтении; проверить, что выбор пейринга с `/` в `canonicalKey` открывает `FilteredWorksRoute` без краша
- [x] 2.3 Передать декодированный `pairingKey` в `FilteredWorksRoute` / `observeWorksByFandomAndPairing` без подмены на display-строку; проверить, что фильтр совпадает с репозиторным сценарием по `canonicalKey`

## 3. Verification

- [x] 3.1 Прогнать `./gradlew test` (или целевые модули с новыми тестами) и убедиться, что новые round-trip тесты зелёные
- [x] 3.2 Ручная или инструментальная проверка: Фэндомы → пейринг с `/` (например Harry Potter/Tom Riddle) → список работ без вылета; empty state допустим, краш — нет
