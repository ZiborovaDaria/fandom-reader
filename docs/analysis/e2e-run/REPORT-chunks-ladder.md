# Translation chunks + model ladder — 2026-09-12

## Context analysis (official Gemini docs)

| Model | Input | Output | Chunk budget (chars, paragraph-aligned) |
|---|---|---|---|
| `gemini-3.5-flash-lite` | 1 048 576 tok | 65 536 tok | **100 000** (~25k tok @4 ch/tok; EN→RU headroom under 65k out) |
| `gemini-3.1-flash-lite` | (3.x Flash-Lite class) | | **40 000** |
| `gemini-2.5-flash-lite` | 1 048 576 tok | 65 536 tok | **16 000** (tighter under free-tier pressure) |

Previous budget was **3 500** chars (~30× smaller than primary).

Chunks are built only by whole paragraphs (`\n\n`); a single oversize paragraph is never mid-cut.

## Behaviour

- On HTTP **429** (after a few retries) or **404** (retired model): advance ladder and retry.
- UI: **Перевести** / **Перевести заново** (force clears `translations_v3` + re-translates metadata).

## Emulator (BlueStacks)

- Key: «Ключ задан» OK
- Live Gemini OK with large segments (see `test_gemini_chunks_ladder.py` log)
- Retranslate button when status ≠ NONE
