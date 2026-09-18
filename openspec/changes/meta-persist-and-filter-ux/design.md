## Context

См. `proposal.md` (Why). Сейчас:

- Главы: файловый кэш `translations_v3/{workId}/chapter_N.txt` — переживает restart.
- Title/summary: Room `titleRu`/`summaryRu` пишутся из `TranslationPipeline`, но upsert по `remoteId` через `ImportCoordinator.buildWork` + REPLACE может обнулить RU.
- Фэндомы: pipeline считает `displayRu`, но `insertFandom` с `OnConflictStrategy.IGNORE` не обновляет существующие строки; `updateFandomDisplayRu` не вызывается.
- Пейринги / display tags: `displayRu` у pairing не заполняется; у `DisplayTag` нет RU-поля — UI показывает EN.
- Библиотека: chip → drill-down (один критерий → сразу список). Поиск только по книгам на home. Теги — секции в одном списке. Нет AND по категориям.

## Goals / Non-Goals

**Goals:**

- Надёжный persist RU-метаданных (title, summary, fandom/pairing labels, display tags) + защита от wipe при re-import.
- Filter builder: вкладки категорий, поиск substring, multi-select, AND между категориями → экран результатов.
- Surfaces фильтров строго через `MaterialTheme.colorScheme` (Night = тёмный фон).

**Non-Goals:**

- Удалённый поиск/фильтры на AO3/Ficbook (скриншоты порталов — только UX-референс).
- Смена провайдера перевода или схемы кэша глав.
- Сохранённые «ленты»/пресеты фильтров как на Ficbook (можно позже).
- Переписывание всего drill-down в ноль в первой итерации — chip-навигация может остаться как быстрый путь, пока builder — основной для комбинирования.

## Decisions

### 1. Persist fandom/pairing `displayRu` через UPDATE, не через insert IGNORE

После batch-перевода вызывать DAO UPDATE по `canonicalKey` (`updateFandomDisplayRu` / аналог для pairing). Альтернатива — `OnConflictStrategy.REPLACE` — отвергнута: риск сброса id/связей.

### 2. Display tags: расширить модель `DisplayTag(valueRu)`

Добавить опциональный `valueRu` в domain + JSON codec `displayTagsJson`. Фильтрация и identity — по `(group, value)` оригинала; UI — `valueRu ?: value`. Альтернатива «отдельная таблица tags» — избыточна для текущего объёма.

### 3. Merge RU при re-import

В `ImportCoordinator` / `upsertWork`: при совпадении `remoteId` (или существующего id) копировать non-blank `titleRu`, `summaryRu`, `translationStatus`, и не затирать уже сохранённые `displayRu` / `valueRu`. Альтернатива «не REPLACE целиком» (column-wise UPDATE) — допустима, если проще в реализации; суть — не терять RU.

### 4. Filter builder + in-memory/Room AND

Состояние выбора в ViewModel: множества `canonicalKey` фэндомов/пейрингов + множества `(group, value)` display tags. Результаты: пересечение (AND) по категориям; внутри категории — OR. Пока библиотека небольшая — клиентская фильтрация по `observeAll` / существующим observe допустима; при росте — составной Room query. Навигация: `filters` → `filter-results` с сериализованным state (или shared VM scope).

### 5. Поиск в пикерах

Переиспользовать подход `filterWorksByQuery`: case-insensitive `contains` по `displayName` и `displayRu` / `valueRu`. Не полноценный FTS SQLite в v1.

### 6. Вкладки = `DisplayTagGroups` + фэндомы/пейринги

Top-level tabs или секции builder: Фэндомы | Пейринги | Персонажи | Рейтинг | Предупреждения | Доп. метки | … (показывать вкладку, если в библиотеке есть теги группы). Не смешивать characters/rating/additional в один список.

### 7. Тема

Никаких hardcoded cream на новых экранах; `Scaffold`/`Surface` с `colorScheme.background` / `surface`. Reader Paper/Sepia не трогать.

## Risks / Trade-offs

- [JSON schema bump для displayTags] → Mitigation: backward-compatible decode (`valueRu` optional); старые записи без поля валидны.
- [Клиентский AND на больших библиотеках] → Mitigation: измерить; при тормозах — Room query; не блокирует UX v1.
- [Два пути browse: chips и builder] → Mitigation: chips остаются shortcut; docs/tasks явно не ломают slash-safe keys.
- [Повторный перевод тегов дороже по API] → Mitigation: переводить только blank RU; Fake в CI.

## Migration Plan

1. Расширить codec/модель display tags; миграция Room только если нужны новые колонки (fandom/pairing `displayRu` уже есть).
2. Починить pipeline UPDATE + import merge; покрыть unit-тестами (Fake provider).
3. Добавить filter UI поверх существующих репозиторных API.
4. Rollback: при проблемах UI — скрыть builder route; данные RU обратно совместимы.

## Open Questions

- Оставлять ли chip drill-down навсегда или deprecate после стабилизации builder — решить при apply по UX-ощущению, на контракт спеки не влияет (оба пути допустимы, если builder есть).
