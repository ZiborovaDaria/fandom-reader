"""E2E Gemini on BlueStacks — unbuffered, coordinate taps for TopAppBar."""
from __future__ import annotations

import re
import subprocess
import sys
import time
from pathlib import Path

ADB = r"C:\Users\Daria\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "127.0.0.1:5555"
OUT = Path(r"docs/analysis/e2e-run")
OUT.mkdir(parents=True, exist_ok=True)


def log(*args):
    print(*args, flush=True)


def adb(*args, timeout=90):
    return subprocess.run(
        [ADB, "-s", SERIAL, *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=timeout,
    )


def dump(name: str) -> str:
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml").stdout or ""
    (OUT / name).write_text(xml, encoding="utf-8")
    return xml


def texts(xml: str) -> list[str]:
    return [t for t in re.findall(r'text="([^"]*)"', xml) if t]


def node_bounds(xml: str, needle: str):
    # text then bounds OR bounds then text on same node
    pats = [
        rf'text="{re.escape(needle)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
        rf'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*text="{re.escape(needle)}"',
    ]
    for pat in pats:
        m = re.search(pat, xml)
        if m:
            return tuple(map(int, m.groups()))
    return None


def tap_xy(x: int, y: int, label: str):
    log(f"TAP {label} @ {x},{y}")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(1.8)


def tap_text(xml: str, needle: str, label: str | None = None):
    b = node_bounds(xml, needle)
    if not b:
        log(f"FAIL: no bounds for {needle!r}")
        log("texts:", texts(xml)[:40])
        sys.exit(1)
    tap_xy((b[0] + b[2]) // 2, (b[1] + b[3]) // 2, label or needle)


def redacted_logcat(n: int = 80) -> list[str]:
    raw = adb("logcat", "-d", "-t", str(n)).stdout or ""
    out = []
    for ln in raw.splitlines():
        low = ln.lower()
        if not any(k in low for k in ("fandomreader", "gemini", "okhttp", "missingapikey", "translation")):
            continue
        if "key=" in low or "api_key" in low:
            out.append("[redacted]")
        else:
            out.append(ln)
    return out[-30:]


results: dict = {}

log("=== force-stop + launch ===")
adb("shell", "am", "force-stop", "com.fandomreader")
time.sleep(0.7)
adb("shell", "monkey", "-p", "com.fandomreader", "-c", "android.intent.category.LAUNCHER", "1")
time.sleep(3)
xml = dump("gemini-e2e-01-library.xml")
log("LIBRARY:", texts(xml)[:20])
assert "com.fandomreader" in xml

log("=== Settings (coordinate TopAppBar) ===")
# From hierarchy: actions L→R Настройки, Импорт, Фэндомы
# Настройки ~ [1381,39][1581,129]
tap_xy(1481, 84, "settings-coord")
xml = dump("gemini-e2e-02-settings.xml")
ts = texts(xml)
log("SETTINGS:", ts)
results["key_configured"] = "Ключ задан" in ts
if not results["key_configured"]:
    log("FAIL: key not configured in Settings")
    sys.exit(1)
log("PASS: Ключ задан")

tap_text(xml, "Назад", "back")
xml = dump("gemini-e2e-03-library.xml")
log("BACK library:", texts(xml)[:15])

log("=== clear logcat + tap first Перевести ===")
adb("logcat", "-c")
# Prefer exact Перевести node — first occurrence
all_tr = list(re.finditer(
    r'text="Перевести"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"|'
    r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*text="Перевести"',
    xml,
))
if not all_tr:
    log("FAIL: no Перевести")
    sys.exit(1)
g = [x for x in all_tr[0].groups() if x is not None]
x1, y1, x2, y2 = map(int, g)
tap_xy((x1 + x2) // 2, (y1 + y2) // 2, "translate-first")
time.sleep(2)
xml = dump("gemini-e2e-04-after-tap.xml")
joined = " ".join(texts(xml))
log("after tap:", texts(xml)[:25])
if "Нужен Gemini API key" in joined:
    log("FAIL: snackbar says key missing")
    sys.exit(1)

log("=== wait up to 10 min for COMPLETE / snackbar ===")
deadline = time.time() + 600
last = None
saw_complete = False
saw_done_msg = False
while time.time() < deadline:
    xml = dump("gemini-e2e-05-wait.xml")
    ts = texts(xml)
    st = [t for t in ts if t.startswith("Перевод:")]
    if "Перевод завершён" in ts:
        saw_done_msg = True
        log("saw snackbar Перевод завершён")
    if st != last:
        log("status", st, "left", int(deadline - time.time()))
        last = st
    if any("COMPLETE" in t for t in st):
        saw_complete = True
        break
    # error snackbar?
    for t in ts:
        if "Ошибка" in t or "HTTP" in t or "failed" in t.lower():
            log("UI error text:", t)
    time.sleep(10)

xml = dump("gemini-e2e-06-done.xml")
ts = texts(xml)
results["statuses"] = [t for t in ts if t.startswith("Перевод:")]
results["complete"] = saw_complete or any("COMPLETE" in t for t in results["statuses"])
results["done_snackbar"] = saw_done_msg
log("final statuses", results["statuses"], "complete", results["complete"])

log("=== open Next Best Thing / first title ===")
# Title may still be Fake-prefixed
title = None
for t in ts:
    if "Next Best Thing" in t or t.startswith("[ru] Next"):
        title = t
        break
if not title:
    for t in ts:
        if t not in (
            "Библиотека", "Настройки", "Импорт", "Фэндомы", "Фанфики", "Прочее", "Перевести", "Пусто",
        ) and not t.startswith("Перевод:") and len(t) < 60:
            title = t
            break
if not title:
    log("FAIL: no title")
    sys.exit(1)
tap_text(xml, title, "open-work")
time.sleep(2.5)
xml = dump("gemini-e2e-07-reader.xml")
rtexts = texts(xml)
log("READER sample:", rtexts[:12])
# body paragraphs — long strings
body = [t for t in rtexts if len(t) > 40]
cyr_body = [t for t in body if re.search(r"[А-Яа-яЁё]", t)]
eng_only_long = [t for t in body if re.search(r"[A-Za-z]{4,}", t) and not re.search(r"[А-Яа-яЁё]", t)]
fake_body = [t for t in body if t.strip().startswith("[ru]")]
results["reader_cyrillic_paragraphs"] = len(cyr_body)
results["reader_english_only_long"] = len(eng_only_long)
results["reader_fake_prefix"] = len(fake_body)
log("cyr paras", len(cyr_body), "eng-only long", len(eng_only_long), "fake prefix", len(fake_body))
if cyr_body:
    log("cyr sample:", cyr_body[0][:180])

# chrome
tap_xy(960, 540, "toggle-chrome")
xml = dump("gemini-e2e-08-chrome.xml")
log("chrome:", texts(xml)[:15])

errs = redacted_logcat(300)
if errs:
    log("logcat hints:")
    for e in errs:
        log(" ", e)

log("RESULTS", results)
ok = results["key_configured"] and (
    results["complete"]
    or results["reader_cyrillic_paragraphs"] > 0
)
# Stronger: live chapter should be Cyrillic without [ru] fake prefix
strong = (
    results["key_configured"]
    and results["reader_cyrillic_paragraphs"] > 0
    and results["reader_fake_prefix"] == 0
)
if strong:
    log("PASS: Settings key OK + reader shows Cyrillic chapter (not Fake [ru] prefix)")
    sys.exit(0)
if ok and results["complete"]:
    log("PASS: key OK + COMPLETE (check reader manually if body empty in dump)")
    sys.exit(0)
if results["key_configured"] and not results["complete"]:
    log("PARTIAL: key OK but translation did not finish / no Cyrillic body in UI dump")
    sys.exit(2)
log("FAIL")
sys.exit(1)
