## Context

См. proposal.md — Why. Сейчас `extractChapterBody` в `:feature:reader` собирает все `<p>` в плоский текст через `\n\n`, поэтому AO3 `blockquote.userstuff`, `#endnotes*`, `#afterword` и заголовки «Chapter Notes» / «Примечания» выглядят как сюжет. Ридер (`ChapterBody`) рисует каждый абзац одинаково. Перевод (`translations_v2`) тоже работает с плоской склейкой абзацев.

Параллельный change `separate-meta-from-chapters` убирает **front-matter / preface** из spine; этот change про **in-chapter** notes/afterword и не зависит от meta-страницы.

## Goals / Non-Goals

**Goals:**
- Сегментация Body vs Note при извлечении HTML/FB2
- Blockquote UI: отступ + лёгкий tint под Paper/Sepia/Night
- Сохранение границ сегментов через translation cache
- Тесты на fixture-подобные фрагменты AO3 и RU-заголовки

**Non-Goals:**
- Отдельная страница метаданных / фильтр preface из spine
- Слияние отдельного Afterword-spine в предыдущую главу (Afterword-файл остаётся своей «главой» в TOC, но тело — note-style)
- Скрытие notes, bookmarks, поиск

## Decisions

### 1. Сегменты вместо только плоской строки
- **Выбор:** модель `ChapterSegment` (`Body` | `Note`) при парсинге; для хранения/перевода — стабильные текстовые маркеры границ (например `§§NOTE§§` / `§§BODY§§`) вокруг регионов, совместимые с `\n\n`-абзацами.
- **Почему:** UI нужен role; перевод уже режет по абзацам — маркеры дешевле новой схемы БД.
- **Альтернатива:** только эвристики по заголовкам на уже плоском тексте — хрупко для «See the end…» и смешанных блоков.

### 2. Детекция (EPUB AO3 + FB2 RU)
- **Выбор:** HTML-first: `#endnotes*`, `#afterword`, `blockquote.userstuff` рядом с note-заголовками, `.endnote-link` в note-регионе; плюс match заголовков (case-insensitive): `Chapter Notes`, `Chapter End Notes`, `Afterword`, `Примечания`, `Послесловие` и близкие варианты с двоеточием.
- **Почему:** совпадает с реальными fixtures (`Next_Best_Thing.epub`, damaged AO3 FB2).
- **Альтернатива:** только CSS-классы Calibre — ломается на non-Calibre EPUB.

### 3. Визуал — blockquote (выбор пользователя)
- **Выбор:** отступ + subtle background tint (foreground/ink low-alpha на Paper; светлее veil на Night); ~90% размера шрифта; muted foreground; заголовок notes чуть сильнее внутри блока.
- **Альтернатива:** только курсив / только left border — отклонены пользователем в пользу цитаты.

### 4. Translation
- **Выбор:** сериализовать сегменты в одну строку с маркерами; `TranslationPipeline` переводит абзацы, **не переводя** строки-маркеры; Fake сохраняет маркеры; ридер парсит маркеры обратно в сегменты. Legacy v2 без маркеров → весь текст как Body.
- **Альтернатива:** `translations_v3` JSON — избыточно для MVP, если маркеры стабильны.

### 5. Граница с `separate-meta-from-chapters`
- **Выбор:** не трогать исключение preface/`split_000` из spine в этом change.
- **Почему:** другой scope; конфликты merge решать при apply, если оба в работе.

## Risks / Trade-offs

- [Ложное срабатывание на абзац «Summary» в сюжете] → Mitigation: узкий whitelist заголовков + HTML-контейнеры; не матчить произвольный текст
- [Gemini съест маркеры] → Mitigation: явный instruction «preserve marker lines»; Fake-тесты; fallback body-only
- [Afterword как отдельный spine выглядит «главой» в TOC] → Mitigation: допустимо; стиль note на всём теле; merge в предыдущую главу — non-goal
- [Пересечение с фильтром front-matter] → Mitigation: notes внутри сюжетных xhtml остаются; preface meta — зона другого change

## Migration Plan

1. Парсинг сегментов + UI blockquote на оригинале.
2. Маркеры в translation path; старый кэш без маркеров читается как body.
3. Unit-тесты; ручной smoke на Next Best Thing (start notes + end notes + Afterword).

## Open Questions

- Нет блокирующих; тон tint/отступа подобрать на Paper/Night при реализации.
