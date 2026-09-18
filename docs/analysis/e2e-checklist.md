# Manual E2E checklist

- [x] Import `fixtures/ao3/Next_Best_Thing.epub` → appears on Фанфики with title+summary (BlueStacks 2026-09-12)
- [x] Browse Фэндомы → pairing → filtered list matches Harry/Tom
- [x] Open reader → immersive text; tap shows chrome; progress UI visible
- [x] Import Ficbook EPUB → fandom/pairing parsed
- [x] Import damaged AO3 FB2 → recovered to Фанфики (not Other)
- [ ] Other shelf with truly non-portal file (not covered this run)

## Device library scan

- [x] Tap **Сканировать** (primary) on library → imports from Downloads (BlueStacks 2026-09-12)
- [ ] Re-run **Сканировать** on the same files → skipped, no duplicate rows

## Translation + Settings (BlueStacks 2026-09-12 evening)

- [x] Open **Настройки** → «Перевод (Gemini)» + indicator **Ключ задан** (user-entered key kept across install `-r`)
- [x] Save/mask/clear UI present (not cleared during this run)
- [x] Live Gemini path works after model fix `gemini-2.0-flash-lite` → `gemini-3.5-flash-lite` (old model returned HTTP 404)
- [x] **Перевести** with key → title/summary become real Russian (e.g. «Следующий лучший вариант»), status PARTIAL — not Fake `[ru]` prefix
- [x] Chapter translation issues live Gemini calls (chunked paragraphs); 18+ OK segments observed before emulator network `ECONNREFUSED` / intermittent HTTP 429
- [ ] Full-work **COMPLETE** + reader showing Russian chapter body end-to-end (blocked this run by BlueStacks connectivity mid-job; resume should continue from cache)
- [x] Unit/CI path uses `FakeTranslationProvider` only in tests — production uses ResolvingTranslationProvider → Gemini

## Chapter order + library browse (unit-covered 2026-09-12)

- [x] TOC / chapter load: numeric order 1→2→…→11 (OPF spine + natural-sort fallback) — `feature:reader` unit
- [x] Ficbook body: block/`br` paragraphs (not one wall of text) — fixture «Поттер…» — `feature:reader` unit
- [x] Romantic (`/`) vs platonic (`&`) facets stay separate; filter chips on library home
- [x] Search by title/summary (EN+RU partial) + romantic/platonic/display-tag filters — `feature:library` / `core:data` unit
- [x] Persist RU metadata (title/summary/fandom/pairing/display tags) + re-import merge — unit (`TranslationPipeline` / `LibraryFilter` / `ImportCoordinator`)
- [x] Filter builder: tabs + substring search + AND/OR combine — domain unit + Compose routes
- [x] Manual BlueStacks 2026-09-14: dual title/summary search, Фильтры tabs, fandom/pairing/platonic, meta page, dark theme → filters, relaunch — `docs/analysis/e2e-run/changes-smoke-20260914/REPORT.md` (23/23)
- [x] Manual BlueStacks 2026-09-14: live Gemini translate → force-stop → RU title/summary/tags survive — `docs/analysis/e2e-run/gemini-persist-20260914/REPORT.md`
- [ ] Manual: open multi-chapter AO3 TOC on device → order matches spine
- [ ] Manual: open Ficbook «Поттер…» chapter → readable paragraphs
- [ ] Manual: search + romantic/platonic/tag chips return expected works

Notes:

- Do not use obsolete Fake-success checklist item.
- Long AO3 works: prefer chunked chapter calls + 429 backoff; full book may still take many minutes / hit quota.
