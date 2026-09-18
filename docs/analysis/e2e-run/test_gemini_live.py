"""E2E: restore library via scan, verify Gemini key + live translate logs."""
from __future__ import annotations

import re
import subprocess
import sys
import time
from pathlib import Path

ADB = r"C:\Users\Daria\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "127.0.0.1:5555"
OUT = Path("docs/analysis/e2e-run")


def log(*a):
    print(*a, flush=True)


def adb(*args, timeout=120):
    return subprocess.run(
        [ADB, "-s", SERIAL, *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=timeout,
    )


def dump(name: str | None = None) -> str:
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml").stdout or ""
    if name:
        (OUT / name).write_text(xml, encoding="utf-8")
    return xml


def texts(xml: str) -> list[str]:
    return [t for t in re.findall(r'text="([^"]*)"', xml) if t]


def bounds_for(xml: str, needle: str):
    for m in re.finditer(r"<node\b[^>]*>", xml):
        n = m.group(0)
        if f'text="{needle}"' not in n:
            continue
        bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if bm:
            return tuple(map(int, bm.groups()))
    return None


def tap(xml: str, needle: str):
    b = bounds_for(xml, needle)
    if not b:
        log("NO BOUNDS", needle)
        log("available", texts(xml)[:30])
        sys.exit(1)
    x, y = (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
    log(f"TAP {needle} @ {x},{y} {b}")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(2)


def main():
    adb("shell", "am", "force-stop", "com.bluestacks.gamecenter")
    adb("shell", "am", "start", "-n", "com.fandomreader/.MainActivity")
    time.sleep(2)

    xml = dump("g3-lib.xml")
    log("LIB", texts(xml)[:15])
    tap(xml, "Настройки")
    xml = dump("g3-settings.xml")
    log("SETTINGS", texts(xml))
    key_ok = "Ключ задан" in texts(xml)
    log("KEY_OK", key_ok)
    if "Назад" in texts(xml):
        tap(xml, "Назад")
    else:
        adb("shell", "input", "keyevent", "4")
        time.sleep(1)

    xml = dump("g3-lib2.xml")
    if "Сканировать устройство" in texts(xml):
        tap(xml, "Сканировать устройство")
    else:
        tap(xml, "Сканировать")
    time.sleep(2)
    xml = dump("g3-perm.xml")
    log("AFTER_SCAN_TAP", texts(xml)[:20])
    for t in texts(xml):
        low = t.lower()
        if any(x in low for x in ("allow", "разреш", "while using", "при использовании")):
            tap(xml, t)
            time.sleep(2)
            xml = dump("g3-after-perm.xml")
            log("after perm", texts(xml)[:20])
            break

    imported = False
    for i in range(45):
        xml = dump("g3-wait-import.xml")
        ts = texts(xml)
        if any("Next Best" in t for t in ts) or "Перевести" in ts:
            log("IMPORTED", [t for t in ts if len(t) < 70][:20])
            imported = True
            break
        if i % 5 == 0:
            log("waiting import", i, ts[:8])
        time.sleep(2)
    if not imported:
        log("IMPORT FAIL", texts(xml)[:25])
        sys.exit(4)

    adb("logcat", "-c")
    tap(xml, "Перевести")
    seen: set[str] = set()
    t0 = time.time()
    while time.time() - t0 < 180:
        out = adb("logcat", "-d", "-t", "120").stdout or ""
        for ln in out.splitlines():
            if "FandomGemini" in ln or "FandomTranslate" in ln:
                safe = re.sub(r"key=[A-Za-z0-9_\-]+", "key=REDACTED", ln)
                if safe not in seen:
                    seen.add(safe)
                    log(safe)
        ok_n = sum(1 for x in seen if "OK segment" in x)
        fail = any(("failed" in x) or ("HTTP " in x and "FandomGemini" in x) for x in seen)
        if ok_n >= 2 or (fail and any("start workId" in x for x in seen)):
            break
        time.sleep(3)

    log("SUMMARY key_ok=", key_ok, "log_lines=", len(seen))
    log("START", any("start workId" in x for x in seen))
    log("OK_COUNT", sum(1 for x in seen if "OK segment" in x))
    log("FAIL_LINES", [x for x in seen if "failed" in x or "HTTP" in x][:8])

    if key_ok and any("OK segment" in x for x in seen):
        log("PASS_LIVE_GEMINI")
        return 0
    if any("start workId" in x for x in seen) and any("failed" in x for x in seen):
        log("FAIL_GEMINI_API")
        return 5
    if not key_ok:
        log("FAIL_NO_KEY — re-enter key in Settings")
        return 6
    log("FAIL_NO_EVIDENCE")
    return 7


if __name__ == "__main__":
    sys.exit(main())
