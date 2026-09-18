# Анализ fandom-reader

Краткий итог explore перед реализацией OpenSpec change `android-fandom-reader`.

## Образцы

| Файл | Тип | Вывод |
|---|---|---|
| `Next_Best_Thing.epub` | сырой AO3 | Эталон: publisher AO3, preface typed tags, EN, ~23k слов — bake-off Gemini |
| `Potter-*.epub`, `Prizrak-*.epub`, `Vlozit-dusu.epub` | Ficbook EPUB | Meta в `title.xhtml` (Фэндом / Пэйринг), rights ficbook.net |
| `зови меня аид.fb2`, `снова, но лучше.fb2` | AO3 после Yandex+Calibre | title Unknown — антипаттерн; теги ещё в body |

## Стек

Kotlin + Compose + Hilt + Room; Source adapters (Mihon-идея без APK plugins); Gemini primary translation; immersive reader.

## Референсы (паттерны, не копипаст GPL)

- Mihon / AppO3 — Source API
- NoveLA / LNReader — TranslationProvider
- Book's Story / FrogReader — reader UX ideas
- trans-epub — chapter cache/resume для bake-off

## Перевод

Тестировать на срезах `Next_Best_Thing` (summary + ch1): Gemini Flash-Lite primary (ключ есть), Fake в CI. Фильтры только по canonicalKey.
