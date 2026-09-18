## Why

MVP читалки уже открывает EPUB/FB2, но текст выглядит «простынёй» без абзацев (Jsoup `.text()` схлопывает разметку), нет тёплой бумажной темы, тёмного режима, смены шрифта/размера и оглавления с переходом в главу. Без этого приложение не ощущается нормальной читалкой, хотя библиотека и импорт уже есть.

## What Changes

- Подключить визуальную идентичность **warm paper** (Paper/Ink/Accent) к Material 3 теме приложения; добавить тёплый Night (и Sepia для чтения)
- Исправить извлечение и отображение текста глав: сохранять границы абзацев в оригинале и в переведённом кэше
- Добавить настройки чтения: семейство шрифта, размер текста, межстрочный интервал; сохранять в DataStore
- Добавить оглавление (TOC) с переходом к выбранной главе; улучшить chrome (прогресс %, bar) без постоянных FAB/toolbar
- Сохранять и восстанавливать позицию внутри главы (offset), а не только индекс главы
- Уточнить перевод глав AO3: сохранять разрывы абзацев при кэшировании `textRu`

## Capabilities

### New Capabilities
- `warm-paper-theme`: app-wide тёплая бумажная палитра Material 3 + companion Night для shell; чтение использует Paper / Sepia / Night независимо от system theme при необходимости

### Modified Capabilities
- `immersive-reader`: абзацы, TOC→глава, шрифт/размер/line height, reading themes, прогресс %/bar, persist настроек и offset внутри главы
- `ao3-translation`: кэш перевода главы SHALL сохранять структуру абзацев исходного текста

## Impact

- `:core:ui` — `Color.kt` / `Theme.kt` / `Type.kt`, токены warm paper + dark
- `:feature:reader` — парсинг абзацев, UI параграфов, chrome (Aa, TOC), preferences
- `:feature:library` (и остальные экраны) — автоматически через `FandomReaderTheme`; точечная полировка списков по желанию в рамках темы
- `:feature:translation` — сегментация/сохранение абзацев при `translateChapter`
- DataStore для reader preferences; Room progress уже есть — расширить использование offset
- Тесты: unit на paragraph extraction (EPUB/FB2 fixtures); FakeTranslationProvider сохраняет `\n\n`; Compose-сценарии TOC и theme chrome
- Non-goals этого change: bookmarks, search in book, highlights/notes, dictionary, page-turn mode, Dynamic Color, копирование GPL-кода читалок
