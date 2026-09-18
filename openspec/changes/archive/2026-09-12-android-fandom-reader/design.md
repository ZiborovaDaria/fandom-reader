## Context

См. motivation в `proposal.md`. В `fandom-reader/` сейчас только образцы книг (Ficbook EPUB, сырой AO3 EPUB `Next_Best_Thing.epub`, AO3 FB2 после Yandex+Calibre с убитой meta). Кода приложения нет — greenfield.

Ограничения:
- Android-only
- артефакты OpenSpec на русском (заголовки/SHALL на английском)
- официальных API AO3/Ficbook нет → парсинг известных шаблонов экспорта
- у пользователя есть ключ Gemini
- перевод не должен повторять путь «слепой MT → Calibre → Unknown title»

## Goals / Non-Goals

**Goals:**
- Модульный каркас приложения с Source adapters и Room-библиотекой
- Надёжный импорт AO3/Ficbook по реальным фикстурам
- Structured translation pipeline (Gemini primary, Fake для CI)
- Immersive reader MVP для EPUB/FB2
- Правила агента и fixtures в папке проекта

**Non-Goals:**
- iOS / Flutter / KMP UI
- Динамические APK-расширения как у Mihon
- Аккаунты/логин AO3 или Ficbook, синхронизация закладок с сайтом
- Полноценный Readium polish в первой итерации (допустим более простой EPUB/FB2 renderer)
- Массовое зеркалирование каталогов порталов

## Decisions

### 1. Стек: Kotlin 2.x + Jetpack Compose + Hilt + Room
- **Why:** Android-only; лучший опенсорс в нише (Book's Story, NoveLA, LightNovelReader, AppO3) на Compose; нативные Room/WorkManager.
- **Alternatives:** Flutter (лишний runtime без iOS-цели); чистый View system (устарело для нового UI).

### 2. Модули и Source API (идея Mihon, без APK plugins)
```
:app
:core:domain / :core:data / :core:ui
:feature:library / :feature:import / :feature:reader / :feature:translation
:source:api / :source:ao3 / :source:ficbook
```
- `CatalogueSource`-подобный контракт: classify → parseMeta → store local file.
- Два портала compile-time; Other — локальный импорт без portal-specific parser.
- **Alternatives:** один монолитный app-модуль (быстрее старт, хуже границы); dynamic extensions (overkill для 2 источников).

### 3. Парсинг метаданных по эталонам из фикстур
| Источник | Детект | Primary meta |
|---|---|---|
| AO3 EPUB | publisher AO3 / ao3 URL | Preface: Fandom, Relationships (`/` romantic, `&` platonic) |
| Ficbook EPUB | ficbook.net | `title.xhtml` labels «Фэндом», «Пэйринг и персонажи» |
| AO3 FB2 damaged | ao3 URLs / body «Фэндом:» | best-effort recovery |
| Other | else | OPF/FB2 title-info only → полка Прочее |

OPF `dc:subject` у AO3 — плоский dump; **не** primary для типов тегов.

### 4. Модель данных
- `Work` (sourceId, remoteId, title, summary, paths, lang, translationStatus)
- M:N `Fandom`, `Pairing` с `canonicalKey` + optional `displayRu`
- Фильтры только по `canonicalKey`
- FTS по title/summary для поиска позже (можно во 2-й итерации; индекс фэндом/пейринг — в MVP)

### 5. Перевод: TranslationProvider + Gemini primary
- Interface: `translateBatch(segments)`; HTML strip/restore как у LNReader.
- Providers: `GeminiProvider` (Flash-Lite по умолчанию), `FakeTranslationProvider`, опционально ML Kit later.
- Translate: title, summary, chapters. Keep canonical tags; optional displayRu.
- Cache per chapter; WorkManager/queue для фоновых книг.
- Bake-off корпус: `Next_Best_Thing.epub` (summary + chapter 1).
- **Alternatives:** только ML Kit (хуже лит. качество); только GTX (хуже + серый ToS); Yandex+Calibre (запрещён как primary).

### 6. Reader MVP
- Immersive text; chrome по тапу (progress, chapter nav, font size/theme).
- EPUB+FB2; простой renderer (Compose pager / WebView) в MVP; Readium — возможный апгрейд.
- Если есть `chapter.textRu` — показывать его по умолчанию.

### 7. Тестирование
- Unit: parsers vs golden JSON из fixtures; classifier; library filters; translation pipeline + Fake.
- Не звать Gemini в CI.
- Desktop/скрипт bake-off отдельно (`tools/mt-bakeoff`) для сравнения качества.
- Правила тестов: fakes > mocks, Turbine, Compose test v2 (в AGENTS/rules).

### 8. Правила и доки в `fandom-reader/`
- `AGENTS.md` + `.cursor/rules/*.mdc` (Compose, architecture, sources, translation, reader UX)
- `docs/analysis/` — разбор образцов и стека
- `fixtures/` — копирование/ссылки на эталонные файлы + golden

## Risks / Trade-offs

- [AO3/Ficbook меняют HTML/EPUB шаблон] → Mitigation: golden fixtures + узкие парсеры + быстрые unit-тесты на регрессию
- [Gemini quota/429 на длинных работах] → Mitigation: chapter chunks, cache/resume, backoff; Flash-Lite для объёма
- [Дубли пейрингов после перевода] → Mitigation: filter by canonicalKey only
- [GPL-опенсорс (Book's Story и др.)] → Mitigation: заимствовать паттерны, не копировать код GPL в проект
- [Простой reader хуже Readium на сложных EPUB] → Mitigation: MVP на типовых AO3/Ficbook экспортах; Readium later
- [Хранение API-ключа] → Mitigation: EncryptedSharedPreferences / DataStore + не логировать ключ

## Migration Plan

Greenfield: нет миграции пользовательских данных.  
Порядок внедрения: scaffold + rules/fixtures → domain/Room → parsers + golden tests → library UI → translation pipeline → reader → polish.

Rollback: не применимо (новый проект); при плохом провайдере перевода — переключение на Fake/отключение автоперевода без потери оригиналов.

## Open Questions

- Точная минимальная версия Android (предположение: API 26+, как у многих Compose-читалок) — подтвердить при scaffold.
- Нужен ли ML Kit в первом релизе или только Gemini + Fake (предположение: Gemini + Fake в MVP, ML Kit следующим шагом).
