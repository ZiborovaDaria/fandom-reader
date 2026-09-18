# E2E changes smoke — blocker 2026-09-14

## Changes in scope

| Change | Status | Emulator checks |
|--------|--------|-----------------|
| `chapter-order-library-browse` | complete (9/9 tasks) | blocked by adb shell |
| `meta-persist-and-filter-ux` | in-progress (12/13; 5.2 manual) | blocked by adb shell |
| Archived (2026-09-12-*) | archived | not re-run this session |

## What worked

- `:app:assembleDebug` OK
- BlueStacks running (`HD-Player`), ADB port `127.0.0.1:5555`
- `adb connect` → `device`
- `adb install -r app-debug.apk` → Streamed Install succeeded at least once
- Fixtures pushed to `/sdcard/Download/fandom-fixtures/` (Next_Best_Thing, Potter, smoke fb2, snova)

## Blocker

Every `adb shell` / `exec-out` / `uiautomator dump` returns **`error: closed`**.

Same with:

- Android SDK adb 1.0.41 (37.0.1)
- BlueStacks `HD-Adb.exe` (client 36)

Without shell we cannot dump UI, tap, or verify library/filters/reader on device.

## Ready script (after ADB shell works)

```powershell
$env:PYTHONIOENCODING='utf-8'
python docs\analysis\e2e-run\smoke_active_changes.py
```

Covers: dual title/summary search, Фильтры builder tabs, fandom/pairing/platonic chips, meta→reader, relaunch, theme/settings.

## Ask user

1. In BlueStacks: **Settings → Advanced → Android Debug Bridge** — toggle Off/On (or restart Pie64 instance).
2. Confirm: `adb -s 127.0.0.1:5555 shell echo ok` prints `ok`.
3. Re-run smoke script (or ask agent to continue).

## Unit coverage already green (proxy until device)

- domain `LibraryFilterTest` (AND/OR, slash keys, RU merge)
- `DisplayTagsCodecTest`
- `TranslationPipelineTest` metadata RU persist
- `ImportCoordinatorBatchTest` rematch preserves RU
- `LibrarySearchTest` separate title vs summary
