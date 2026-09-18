# fandom-reader — agent instructions

## Project
Android fanfic reader: library sorted by fandom/pairing, AO3+Ficbook import, AO3 EN→RU via Gemini, immersive reader, Other shelf for non-matching books.

## Stack
- Kotlin 2.x, Jetpack Compose, Material 3, Hilt+KSP, Room, Coroutines/Flow, DataStore
- OkHttp + Jsoup for portal HTML/EPUB parsing
- TranslationProvider: Gemini primary, Fake in tests (ML Kit optional later)
- minSdk 26 (unless AGENTS updated)

## Layout
- App code: Gradle modules per `openspec/changes/android-fandom-reader/design.md`
- Samples/fixtures: `fixtures/` and root sample books
- Plan: OpenSpec change `android-fandom-reader` under `openspec/changes/`

## Hard rules
- Do not wipe AO3 metadata via Yandex/Calibre-style whole-file translate as the primary path
- Filter fandom/pairing by `canonicalKey`, not translated display strings
- AO3 meta: prefer preface typed fields over flat `dc:subject`
- Ficbook meta: prefer `title.xhtml` labels over OPF-only
- Reader default: text-first, chrome on tap; no persistent FAB/badges in reader
- Tests: fakes over mocks; FakeTranslationProvider in CI; never require live Gemini in unit tests
- Do not copy GPL reader code; reuse patterns only

## Commands (after scaffold)
```bash
./gradlew :app:assembleDebug
./gradlew test
```

## Specs source of truth
OpenSpec capabilities: `fanfic-library`, `portal-import`, `ao3-translation`, `immersive-reader`.
