# E2E Gemini translation — BlueStacks (127.0.0.1:5555) — 2026-09-12 evening

## Environment
- Device: BlueStacks (`SM_G998B` over adb)
- APK: `app-debug` installed with `-r` (encrypted key preserved)
- Fixtures re-imported via **Сканировать** from `/sdcard/Download/`

## Results
| Check | Result |
|---|---|
| Settings → Ключ задан | OK |
| Model `gemini-2.0-flash-lite` | FAIL HTTP 404 — model retired |
| Model `gemini-3.5-flash-lite` | OK |
| Metadata EN→RU | OK — «Следующий лучший вариант» + Russian summary |
| Fake `[ru]` path | Not used when key present |
| Chapter chunks via Gemini | OK — many `OK segment` ~3k chars |
| Full COMPLETE | Not reached — mid-job `ECONNREFUSED` to Google APIs on emulator + earlier 429 |
| Reader RU chapter body | Not fully verified after network drop (metadata RU confirmed in library) |

## Fixes applied during test
1. Switch Gemini model to `gemini-3.5-flash-lite`
2. Stronger HTTP 429 exponential backoff (up to 30s, 8 attempts)
3. Chunk chapter paragraphs (~3500 chars/request) instead of 1 HTTP call per paragraph

## Scripts
- `docs/analysis/e2e-run/test_gemini_live.py`
- `docs/analysis/e2e-run/test_gemini_chapters.py`
