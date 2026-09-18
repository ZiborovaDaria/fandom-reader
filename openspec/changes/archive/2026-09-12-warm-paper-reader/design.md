## Context

См. `proposal.md` — Why. MVP уже есть: `:feature:reader` грузит EPUB/FB2 через Jsoup, показывает один `Text`, chrome = prev/next. В `:core:ui` заданы `Paper`/`Ink`/`Accent`, но `FandomReaderTheme` их не подключает. Room уже хранит `readingProgressChapterIndex` + `readingProgressOffset`, но reader всегда пишет `offset = 0`. Перевод главы кладёт целый оригинал в один batch → риск потери абзацев и в `textRu`.

Правила: Material 3 only; immersive chrome; не копировать GPL-код читалок; FakeTranslationProvider в CI.

## Goals / Non-Goals

**Goals:**

- Подключить warm paper (+ Night) к shell; Paper/Sepia/Night на поверхности чтения
- Абзацы в парсинге и UI; TOC → глава; шрифт/размер (+ line height); persist prefs; offset
- Перевод/кэш глав сохраняет границы абзацев

**Non-Goals:**

- Bookmarks, search in book, highlights, dictionary, page-turn vs scroll mode
- Dynamic Color / Material You как основа бренда
- Readium navigator, кастомные TTF из GPL-проектов
- Полный редизайн карточек библиотеки beyond theme tokens

## Decisions

### 1. Два слоя цвета: App scheme vs Reader surface

- **Выбор:** `FandomReaderTheme` строит `lightColorScheme`/`darkColorScheme` из Paper/Ink/Accent для shell. Отдельно `ReaderTheme` (Paper/Sepia/Night) через CompositionLocal / собственные bg+fg для reading surface.
- **Почему:** shell следует system light/dark; чтение часто хочет Sepia/Night независимо.
- **Альтернатива:** один Dynamic Color scheme → отклонено (ломает warm paper).

### 2. Абзацы: extract → `List<String>` / `\n\n`, не `.text()` целиком

- **Выбор:** для EPUB — `body.select("p, …")` (с разумным fallback); для FB2 — `<p>` внутри section. Модель главы хранит текст с `\n\n` или список параграфов. UI — колонка `Text` по абзацам или один Text с paragraph spacing.
- **Почему:** минимальный фикс наблюдаемого бага без Readium.
- **Альтернатива:** WebView HTML → тяжелее, хуже контроль темы/шрифта в MVP.

### 3. Настройки чтения в DataStore

- **Выбор:** DataStore preferences: `readerFontFamily`, `readerFontSizeSp`, `readerLineHeightMult`, `readerSurfaceTheme`. Не в Room per-work.
- **Почему:** глобальный комфорт; Room уже занят прогрессом книги.
- **Шрифты MVP:** Serif / SansSerif / Monospace (системные `FontFamily`). Размер ~14–28sp ступенями.

### 4. TOC = список загруженных глав

- **Выбор:** modal/sheet из `chapters` (title или «Глава N»). Tap → `chapterIndex = n`, закрыть sheet. Не парсить отдельный nav.xhtml в первой итерации, если главы уже нарезаны.
- **Почему:** уже есть `List<ReaderChapter>`; быстро и достаточно для AO3/Ficbook.
- **Риск порядка EPUB:** сейчас сортировка по имени файла — зафиксировать как known limitation; при необходимости позже spine/OPF (отдельный follow-up, не блокирует TOC UX).

### 5. Chrome: TOC + Aa + progress, всё по тапу

- **Выбор:** расширить нижний/верхний chrome: fraction глав, progress bar (по главе или по книге), кнопки TOC и Aa (theme/font/size). Без FAB.
- **Offset:** сохранять scroll offset (символы или pixel→нормализованный int) через существующий `updateReadingProgress`; восстанавливать в `LaunchedEffect`/`ScrollState`.

### 6. Перевод: сегменты по абзацам

- **Выбор:** `translateChapter` режет оригинал по абзацам, `translateBatch` по сегментам, собирает кэш с `\n\n`. Fake provider сохраняет separators.
- **Почему:** соответствует spec; не требует нового API провайдера.
- **Альтернатива:** один prompt «сохрани абзацы» → хрупко на live Gemini; структура на входе надёжнее.

## Risks / Trade-offs

- [Неверный порядок глав EPUB без spine] → Mitigation: TOC всё равно полезен; задача/комментарий на OPF spine later
- [Старые кэши перевода без абзацев] → Mitigation: при изменении формата кэша — versioned path или invalidate; документировать в tasks
- [Offset в пикселях хрупок при смене шрифта] → Mitigation: предпочтительно character/paragraph index; пиксели — fallback с best-effort restore
- [Слишком много контролов в chrome] → Mitigation: Aa и TOC за вторичными кнопками/sheet, не постоянная панель

## Migration Plan

- Тема: drop-in в `FandomReaderTheme`; визуальный регресс ожидаем и желаем
- Кэш глав: при несовместимости формата — переименовать директорию кэша (`translations/` → `translations_v2/`) или удалять при чтении, если нет маркеров абзацев
- Rollback: откат change; prefs DataStore безвредны

## Open Questions

- Точная метрика offset (char index vs paragraph index) — выбрать при реализации самый простой стабильный вариант; на поведение spec («approximately the same scroll offset») не влияет
