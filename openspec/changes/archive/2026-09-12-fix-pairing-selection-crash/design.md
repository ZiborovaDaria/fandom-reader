## Context

См. `proposal.md` — Why. Сейчас в `FandomReaderNav` маршруты собираются строками:

- `pairings/{fandomKey}`
- `filtered/{fandomKey}/{pairingKey}`

`MetaNormalize.canonicalKey` оставляет `/` (романтические теги) и заменяет `&` на `/`, поэтому типичный пейринг даёт ключ вроде `harry potter/tom riddle`. Подстановка в path без кодирования создаёт лишние сегменты; Navigation Compose не матчит `filtered/{fandomKey}/{pairingKey}` → краш/ошибка навигации.

Фильтрация в репозитории по `canonicalKey` уже корректна (unit-тесты есть). Ломается только UI-навигация. Правило архитектуры: prefer type-safe Navigation Compose; минимум для фикса — безопасная передача строковых аргументов.

## Goals / Non-Goals

**Goals:**

- Стабильный переход fandom → pairing → filtered list при любых реальных `canonicalKey`
- Round-trip ключа без потери `/`, пробелов и прочих символов
- Регрессионный тест на ключ с `/`

**Non-Goals:**

- Менять алгоритм `MetaNormalize.canonicalKey` / убирать `/` из ключей
- Полная миграция всего графа навигации на type-safe routes (допустимо точечно для этих экранов)
- Менять SQL/Room фильтры или модель `Pairing`

## Decisions

### 1. Кодировать path-аргументы (`Uri.encode` / `Uri.decode`), не менять canonicalKey

- **Выбор:** при `navigate` кодировать `fandomKey` и `pairingKey` (`android.net.Uri.encode(..., null)` или эквивалент, который кодирует `/`); при чтении из `NavBackStackEntry` — `Uri.decode`.
- **Почему:** минимальный дифф в `:app`, ключи в БД/домене остаются как есть, фильтр продолжает работать.
- **Альтернативы:**
  - Заменить `/` в canonicalKey на другой разделитель → **отклонено** (ломает существующие данные, тесты, правило «фильтр по canonicalKey»).
  - Query-параметры (`filtered?fandom=…&pairing=…`) → рабочий вариант, но больше правок route-графа.
  - Сразу полная type-safe Navigation → желательно позже; для бага избыточно, если encode закрывает сценарий.

### 2. Кодировать и fandomKey

- Fandom canonical keys тоже могут содержать `/` или пробелы после нормализации; кодировать оба аргумента на всех переходах (`pairings/…`, `filtered/…`).

### 3. Где жить хелперу

- Предпочтительно маленький helper в `:app` (или `:core:ui`, если уже есть место для nav-утилит). Не тащить Android `Uri` в `:core:domain`.
- Если появится unit-тест без Android: вынести pure percent-encode/decode или тестировать через `androidx` test / Robolectric — выбрать самый дешёвый вариант в модуле, где уже есть тестовый стек.

### 4. Объём type-safe nav

- В рамках этого change type-safe routes **не обязательны**. Если при правке удобно ввести `@Serializable` route только для filtered/pairings — допустимо; иначе encode достаточно.

## Risks / Trade-offs

- [Двойное кодирование при повторном navigate] → Mitigation: encode только на границе `navigate(...)`, decode только при чтении args один раз
- [Пробелы/`+` в decode] → Mitigation: использовать `Uri.encode`/`Uri.decode` согласованно; покрыть тестом ключ с пробелом и `/`
- [E2E раньше мог «проходить» на пейрингах без явного романтического `/` в UI-строке] → Mitigation: регрессионный тест/сценарий именно с `/` в canonicalKey

## Migration Plan

- Чистый bugfix, миграции данных нет
- Rollback: откат коммита навигации

## Open Questions

- (нет блокирующих; полная type-safe миграция — отдельный change при желании)
