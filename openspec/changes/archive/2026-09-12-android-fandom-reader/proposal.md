## Why

Нужна личная Android-читалка фанфиков, где книги сортируются по фэндому и пейрингам, а в списке видны название и аннотация. Книги в основном с AO3 и Ficbook (чёткая структура метаданных); AO3 нужно переводить на русский нейросетью без потери тегов; всё остальное остаётся видимым в отдельной полке «Прочее». Готовые читалки либо не знают фанфик-таксономию, либо ломают метаданные при переводе (как Yandex+Calibre на FB2-образцах).

## What Changes

- Новый Android-проект `fandom-reader` (Kotlin, Jetpack Compose): локальная библиотека, импорт, перевод AO3, immersive reader.
- Импорт и классификация файлов/URL: AO3 EPUB, Ficbook EPUB, прочие EPUB/FB2 → полка «Прочее».
- Нормализованная модель Work с фэндомами и пейрингами; список: название + аннотация; навигация фэндом → пейринг.
- Перевод AO3 EN→RU через Gemini (primary) с сохранением canonical-тегов для фильтров; интерфейс провайдеров (ML Kit / Fake для тестов).
- Читалка с минимумом отвлекающих элементов (chrome по тапу).
- Фикстуры из реальных образцов, golden-тесты парсеров, bake-off/скрипт перевода на `Next_Best_Thing.epub`.
- Правила агента (`AGENTS.md`, `.cursor/rules`) и документация анализа в папке проекта.

## Capabilities

### New Capabilities
- `fanfic-library`: локальная библиотека, полки Фанфики/Прочее, фильтры фэндом/пейринг, список title+summary
- `portal-import`: детект и парсинг AO3/Ficbook/Other, импорт EPUB/FB2 и сохранение метаданных
- `ao3-translation`: structured EN→RU перевод AO3 (Gemini primary), кэш глав, сохранение canonical tags
- `immersive-reader`: офлайн-чтение EPUB/FB2 с минимальным UI и прогрессом

### Modified Capabilities
- (нет — проект greenfield, существующих specs нет)

## Impact

- Код: новый Gradle multi-module проект в `c:\MyProjects\fandom-reader\` (сейчас только образцы книг).
- Зависимости: Compose, Hilt, Room, OkHttp/Jsoup, Coil; Gemini API (ключ пользователя); опционально ML Kit Translate.
- Данные: Room + файлы книг в app storage; API-ключ Gemini только в защищённом хранилище настроек.
- Внешние системы: HTML/EPUB структура AO3 и Ficbook (без официальных API); rate limit и личный offline-архив.
- Тесты: unit на парсеры/классификатор/фильтры; FakeTranslator в CI; ручной bake-off Gemini на срезах `Next_Best_Thing.epub`.
