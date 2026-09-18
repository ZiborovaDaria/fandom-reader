## Context

См. proposal.md — Why. Сейчас `MainActivity` ведёт `onOpenWork` сразу в `reader/{workId}`. `loadEpub` берёт почти все xhtml/html (кроме nav/toc), поэтому AO3 preface/`split_000` и Ficbook `title.xhtml` становятся «главами». Meta уже парсится в `Ao3EpubParser` / `FicbookEpubParser` в Room (`summary`, `fandoms`, `pairings`), но Characters / Additional Tags / Rating в модели `Work` нет. E2E фиксировал: «текст preface → Глава 1/7».

## Goals / Non-Goals

**Goals:**
- Экран метаданных до чтения (описание + теги)
- Spine ридера без front-matter; глава 1 = первая сюжетная
- TOC-подписи: название главы или «Глава N»
- Достаточный набор display-тегов на meta-странице
- Стабильные тесты на AO3/Ficbook fixtures

**Non-Goals:**
- OPF spine ordering как отдельный большой рефакторинг (можно улучшить фильтр поверх текущего sort-by-name)
- Настройки API-ключей / pipeline перевода / device-wide scan EPUB-FB2
- Bookmarks, поиск по книге, редактирование тегов пользователем

## Decisions

### 1. Meta как отдельный Compose-маршрут, не «глава 0» в ридере
- **Выбор:** `work/{workId}` (WorkMetaScreen) → кнопка «Читать» → `reader/{workId}`. Из chrome ридера — «О книге» обратно на meta (или sheet с теми же данными).
- **Почему:** требование «страничка отдельно от глав»; не ломает immersive default и индексы прогресса.
- **Альтернатива:** front-matter как `chapterIndex = -1` внутри ReaderViewModel — смешивает модели и усложняет TOC/progress.

### 2. Фильтрация front-matter при загрузке глав
- **Выбор:** в `loadEpub` (и при необходимости FB2) исключать документы по эвристикам, согласованным с парсерами:
  - имя: `preface`, `title.xhtml`, `split_000` (AO3 meta), summary-only page если это не глава
  - содержимое: типичные label-блоки Fandom/Relationships / «Фэндом» без сюжетного body
- Переиндексировать `ReaderChapter.index` с 0 после фильтра (UI показывает 1-based).
- **Альтернатива:** опираться только на OPF spine + landmarks — правильнее долгосрочно, но шире скоупа; зафиксировать как follow-up, если heuristics промахнутся на edge EPUB.

### 3. Дополнительные теги — display-only JSON на Work
- **Выбор:** поле вроде `extraTagsJson` / таблица `work_display_tags` с группами (`rating`, `warnings`, `characters`, `additional`, …). Фильтры библиотеки не используют эти ключи.
- MVP минимум: показать уже имеющиеся fandoms+pairings; парсить AO3 Characters/Additional Tags/Rating/Warnings в том же change, если объём умеренный.
- **Альтернатива:** только fandoms+pairings на meta — слабее формулировки «все теги».

### 4. Прогресс чтения после сдвига индексов
- **Выбор:** при первом открытии после обновления, если сохранённый индекс указывает на бывший preface или `index >= newCount`, сбросить к 0 (первая сюжетная) с offset 0; опционально одноразовая миграция «если source AO3/Ficbook и index==0 и текст похож на meta — reset».
- Translation cache `translations_v2/{workId}/chapter_{index}.txt` привязан к старому индексу — **переименовать/пересчитать** кэш по новому spine или bump path на `translations_v3` и инвалидировать старый (предпочтительно v3 + clear stale), иначе перевод «главы 1» останется на preface.

### 5. Навигация
- Все `onOpenWork` → `work/{id}`; reader только с meta или deep-link restore.
- Translate остаётся на списке или дублируется на meta (не блокер).

### 6. Display-label для TOC
- **Выбор:** чистая функция `tocLabel(chapter, workTitle, index1Based)`:
  1. взять `chapter.title`, trim;
  2. отбросить, если blank / equalsIgnoreCase(workTitle) / известные junk (`Preface`, `Summary`, `Title Page`, «Оглавление», …);
  3. иначе показать title; иначе «Глава $index1Based».
- UI TOC и любые списки глав используют только эту функцию (chrome fraction может остаться «Глава i/N»).
- **Альтернатива:** всегда только «Глава N» — проще, но хуже UX, когда у EPUB/FB2 есть нормальные заголовки секций.

## Risks / Trade-offs

- [Ложная фильтрация реальной главы] → Mitigation: fixtures Next_Best_Thing + Ficbook sample; unit-тесты на число глав и отсутствие «Summary»/«Fandom:» в chapter[0].text
- [Сдвиг индексов ломает кэш перевода] → Mitigation: `translations_v3` + invalidate v2; pipeline пишет по новому index
- [Неполные «все теги» без миграции Room] → Mitigation: schema bump + parse на import; для уже импортированных — best-effort re-parse с диска при открытии meta
- [FB2 damaged: meta в body первой section] → Mitigation: не выкидывать единственную section; для multi-section отфильтровать только явную title/meta section
- [Жёсткий junk-filter отбрасывает редкий реальный заголовок «Summary»] → Mitigation: узкий список + equals work title; при сомнении оставлять title

## Migration Plan

1. Добавить маршрут + UI meta (можно на существующих полях Work).
2. Включить фильтр spine + тесты; bump translation cache version.
3. Расширить parse/storage display-тегов; re-parse on open для старых записей.
4. Сброс/clamp progress; ручной E2E: open AO3 → meta с тегами → Читать → глава 1 без preface.

## Open Questions

- Нужна ли кнопка «О книге» в chrome сразу в MVP, или достаточно list → meta → reader (вернуться через system back)? По умолчанию: back на meta + пункт в chrome, если chrome уже есть.
