"""E2E: large chunks + model ladder + force retranslate on BlueStacks."""
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


def adb(*args):
    return subprocess.run(
        [ADB, "-s", SERIAL, *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=120,
    )


def dump(name=None):
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml").stdout or ""
    if name:
        (OUT / name).write_text(xml, encoding="utf-8")
    return xml


def texts(xml):
    return [t for t in re.findall(r'text="([^"]*)"', xml) if t]


def bounds_for(xml, needle):
    for m in re.finditer(r"<node\b[^>]*>", xml):
        n = m.group(0)
        if f'text="{needle}"' not in n:
            continue
        bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if bm:
            return tuple(map(int, bm.groups()))
    return None


def tap(xml, needle):
    b = bounds_for(xml, needle)
    if not b:
        log("NO", needle, texts(xml)[:25])
        sys.exit(1)
    x, y = (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
    log(f"TAP {needle} @ {x},{y}")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(2)


adb("shell", "am", "force-stop", "com.bluestacks.gamecenter")
adb("shell", "am", "start", "-n", "com.fandomreader/.MainActivity")
time.sleep(2)

xml = dump("g5-lib.xml")
ts = texts(xml)
log("LIB", [t for t in ts if len(t) < 55][:18])

# Settings key
tap(xml, "Настройки")
xml = dump("g5-settings.xml")
key_ok = "Ключ задан" in texts(xml)
log("KEY_OK", key_ok)
if not key_ok:
    sys.exit(6)
tap(xml, "Назад")
xml = dump("g5-lib2.xml")

# Ensure we have a book
if "Перевести" not in texts(xml) and "Перевести заново" not in texts(xml):
    if "Сканировать" in texts(xml):
        tap(xml, "Сканировать")
        time.sleep(3)
        xml = dump("g5-scanned.xml")
        log("AFTER_SCAN", [t for t in texts(xml) if len(t) < 50][:15])

label = "Перевести заново" if "Перевести заново" in texts(xml) else "Перевести"
log("BUTTON", label)
adb("logcat", "-c")
tap(xml, label)

seen = set()
ok_chars = []
models = set()
t0 = time.time()
while time.time() - t0 < 300:
    out = adb("logcat", "-d", "-t", "100").stdout or ""
    for ln in out.splitlines():
        if "FandomGemini" not in ln and "FandomTranslate" not in ln:
            continue
        safe = re.sub(r"key=[A-Za-z0-9_\-]+", "key=REDACTED", ln)
        if safe in seen:
            continue
        seen.add(safe)
        if "OK model=" in safe or "OK segment" in safe or "translateBatch" in safe or "fallback" in safe or "start workId" in safe or "done workId" in safe or "failed" in safe:
            log(safe)
        m = re.search(r"OK model=([^\s]+) .* chars=(\d+)", safe)
        if m:
            models.add(m.group(1))
            ok_chars.append(int(m.group(2)))
        m2 = re.search(r"model=([^\s]+) segment=", safe)
        if m2 and "translateBatch" in safe:
            models.add(m2.group(1).split()[0] if False else None)
        m3 = re.search(r"translateBatch .* model=([^\s]+)", safe)
        if m3:
            models.add(m3.group(1))
        if "fallback model" in safe:
            log("FALLBACK_SEEN")
        if "done workId" in safe:
            break
    if any("done workId" in x for x in seen):
        break
    if any("failed workId" in x for x in seen) and time.time() - t0 > 60:
        # allow some progress first
        if ok_chars:
            break
    time.sleep(4)

xml = dump("g5-after.xml")
ts = texts(xml)
log("UI", [t for t in ts if len(t) < 60][:15])
log("MODELS", sorted(x for x in models if x))
log("OK_CHARS_MAX", max(ok_chars) if ok_chars else 0, "OK_COUNT", len(ok_chars))
log("HAS_RETRANSLATE_BTN", "Перевести заново" in ts or label == "Перевести заново")

# Open reader if Russian title present
title = next((t for t in ts if "Следующий" in t or "лучш" in t), None)
reader_cyr = 0
if title:
    tap(xml, title)
    time.sleep(2.5)
    r = texts(dump("g5-reader.xml"))
    reader_cyr = sum(1 for t in r if re.search(r"[А-Яа-яЁё]", t) and len(t) > 40)
    log("READER_CYR", reader_cyr)

large_chunk = max(ok_chars) if ok_chars else 0
# Success: key ok + live OK with meaningfully larger chunks than old 3500
pass_live = key_ok and large_chunk >= 5000
pass_btn = label == "Перевести заново" or "Перевести заново" in ts
log("PASS_LIVE", pass_live, "PASS_RETRANSLATE_UI", pass_btn)
if pass_live:
    log("PASS_CHUNK_AND_GEMINI")
    sys.exit(0)
log("FAIL")
sys.exit(1)
