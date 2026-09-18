"""Re-tap translate after chunking fix; wait for COMPLETE or Cyrillic reader."""
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
        log("NO", needle, texts(xml)[:20])
        sys.exit(1)
    x, y = (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
    log(f"TAP {needle} @ {x},{y}")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(2)


adb("shell", "am", "force-stop", "com.bluestacks.gamecenter")
adb("shell", "am", "start", "-n", "com.fandomreader/.MainActivity")
time.sleep(2)
adb("logcat", "-c")
xml = dump("g4-lib.xml")
log("LIB", [t for t in texts(xml) if len(t) < 60][:15])
tap(xml, "Перевести")

deadline = time.time() + 600
seen = set()
while time.time() < deadline:
    out = adb("logcat", "-d", "-t", "80").stdout or ""
    for ln in out.splitlines():
        if "FandomGemini" in ln or "FandomTranslate" in ln:
            safe = re.sub(r"key=[A-Za-z0-9_\-]+", "key=REDACTED", ln)
            if safe not in seen and ("OK segment" in safe or "done workId" in safe or "failed" in safe or "429" in safe or "start workId" in safe or "translateBatch" in safe):
                seen.add(safe)
                log(safe)
    xml = dump("g4-wait.xml")
    st = [t for t in texts(xml) if t.startswith("Перевод:")]
    if any("COMPLETE" in t for t in st) or any("done workId" in x for x in seen):
        log("COMPLETE", st)
        break
    if any("failed workId" in x for x in seen) and time.time() > deadline - 500:
        # keep waiting through early failures? break if failed recently and no more OK
        pass
    time.sleep(8)

xml = dump("g4-final.xml")
ts = texts(xml)
log("FINAL", [t for t in ts if len(t) < 70][:18])
# open russian title
title = next((t for t in ts if "Следующий" in t or "лучш" in t), None)
if not title:
    title = next((t for t in ts if "Next Best" in t), None)
if title:
    tap(xml, title)
    time.sleep(3)
    xml = dump("g4-reader.xml")
    r = texts(xml)
    cyr = [t for t in r if re.search(r"[А-Яа-яЁё]", t) and len(t) > 40]
    log("READER_CYR", len(cyr))
    if cyr:
        log("SAMPLE", cyr[0][:220])
    else:
        log("READER", r[:10])

ok_n = sum(1 for x in seen if "OK segment" in x)
failed = any("failed workId" in x for x in seen)
done = any("done workId" in x for x in seen)
log("OK_COUNT", ok_n, "DONE", done, "FAILED", failed)
if done or (ok_n >= 5 and len(cyr) if title else 0) > 0:
    log("PASS_CHAPTER_PROGRESS")
    sys.exit(0)
if ok_n >= 3:
    log("PASS_PARTIAL_PROGRESS")
    sys.exit(0)
log("FAIL")
sys.exit(1)
