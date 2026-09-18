## Why

Chapter notes и послесловие сейчас выглядят как обычный текст главы: парсер сплющивает AO3/FB2-разметку в абзацы, и «Chapter Notes» / «Chapter End Notes» / «Afterword» / «Примечания» / «Послесловие» не отличаются от сюжета. Нужно оставить их **в потоке главы**, но визуально выделить как цитату.

## What Changes

- Распознавать блоки примечаний и послесловия при извлечении текста главы (EPUB/FB2), не вынося их в отдельный пункт TOC и не удаляя из главы
- Отображать такие блоки в ридере в стиле **blockquote**: отступ + лёгкий фон (Paper / Sepia / Night), чуть меньший/приглушённый текст
- Сохранять границы note/body при кэше перевода, чтобы стиль не терялся после EN→RU
- Unit-тесты на AO3-like HTML и RU-заголовки (`Примечания`, `Послесловие`)

## Capabilities

### New Capabilities

- (нет)

### Modified Capabilities

- `immersive-reader`: in-chapter notes / afterword остаются в главе и MUST визуально отличаться от сюжетного текста (blockquote-стиль)
- `ao3-translation`: кэш/склейка перевода MUST сохранять границы note-сегментов относительно body

## Impact

- `:feature:reader` — `ChapterText` / `ReaderScreen` (сегменты + Compose blockquote)
- `:feature:translation` — маркеры/сегменты в `translations_v2` (или эквивалент без ломки body-абзацев)
- Fixtures: `fixtures/ao3/Next_Best_Thing.epub`, RU FB2 с `Примечания:`
- Non-goals: отдельная meta-страница / исключение preface из spine (это `separate-meta-from-chapters`); скрытие notes; merge Afterword-spine в предыдущую главу
