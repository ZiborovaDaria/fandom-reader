## Why

При выборе пейринга в цепочке «Фэндомы → пейринги → список» приложение падает. `canonicalKey` пейринга содержит `/` (романтические теги и нормализация `&` → `/` в `MetaNormalize`), а Navigation Compose передаёт ключ как сегмент пути без кодирования — маршрут `filtered/{fandomKey}/{pairingKey}` ломается.

## What Changes

- Исправить передачу `fandomKey` / `pairingKey` в навигации так, чтобы символы `/`, пробелы и прочие reserved-символы не ломали route
- Сохранить фильтрацию строго по `canonicalKey` (не менять модель ключей и не подменять их display-строками)
- Добавить регрессионную проверку сценария выбора пейринга с `/` в ключе
- Уточнить требование browse fandom→pairing: переход MUST не падать и MUST доставлять исходный `canonicalKey` на экран фильтра

## Capabilities

### New Capabilities

- (нет)

### Modified Capabilities

- `fanfic-library`: уточнить browse fandom→pairing — безопасная навигация при `canonicalKey` с `/` и доставка неискажённого ключа в фильтр

## Impact

- `:app` — `MainActivity` / `FandomReaderNav` (сборка и разбор route-аргументов)
- возможно `:feature:library` — только если вынести хелперы кодирования/декодирования
- тесты навигации или unit-тест helper’а кодирования + сценарий фильтра с ключом вида `harry-potter/tom-riddle`
- домен, Room, парсеры AO3/Ficbook и `MetaNormalize.canonicalKey` — без изменения семантики ключей
