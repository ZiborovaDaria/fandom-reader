"""Live Gemini: translate metadata → kill app → RU title/tags must survive.

Focus of meta-persist-and-filter-ux task 5.2 (persist, not full-book COMPLETE).
"""
from __future__ import annotations

import re
import subprocess
import sys
import time
from pathlib import Path

ADB = r"C:\Users\Daria\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "127.0.0.1:5555"
OUT = Path(r"C:\MyProjects\fandom-reader\docs\analysis\e2e-run\gemini-persist-20260914")
OUT.mkdir(parents=True, exist_ok=True)

TARGET_EN = "Next Best Thing"
RESULTS: dict = {}


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


def dump(name: str) -> str:
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml").stdout or ""
    (OUT / name).write_text(xml, encoding="utf-8")
    adb("shell", "screencap", "-p", "/sdcard/shot.png")
    adb("pull", "/sdcard/shot.png", str(OUT / name.replace(".xml", ".png")))
    return xml


def texts(xml: str) -> list[str]:
    return [t for t in re.findall(r'text="([^"]*)"', xml) if t]


def bounds_for(xml: str, needle: str, contains: bool = False):
    for m in re.finditer(r"<node\b[^>]*>", xml):
        n = m.group(0)
        tm = re.search(r'text="([^"]*)"', n)
        if not tm:
            continue
        t = tm.group(1)
        ok = (needle in t) if contains else (t == needle)
        if not ok:
            continue
        bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if bm:
            return tuple(map(int, bm.groups())), t
    return None, None


def tap_text(xml: str, needle: str, contains: bool = False) -> str | None:
    b, matched = bounds_for(xml, needle, contains=contains)
    if not b:
        log("MISS", needle, texts(xml)[:25])
        return None
    x, y = (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
    log(f"TAP {matched!r} @ {x},{y}")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(1.6)
    return matched


def tap_xy(x: int, y: int, label: str = ""):
    log(f"TAP_XY {label} @ {x},{y}")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(1.5)


def has_cyrillic(s: str) -> bool:
    return bool(re.search(r"[А-Яа-яЁё]", s))


def is_fake_prefix(s: str) -> bool:
    return s.strip().startswith("[ru]")


def launch():
    adb("shell", "am", "force-stop", "com.fandomreader")
    time.sleep(0.8)
    adb("shell", "am", "start", "-n", "com.fandomreader/.MainActivity")
    time.sleep(2.5)


def find_nbt_translate(xml: str):
    """Find Перевести / Перевести заново near Next Best Thing row."""
    # Collect all translate buttons with y coords
    buttons = []
    for m in re.finditer(r"<node\b[^>]*>", xml):
        n = m.group(0)
        tm = re.search(r'text="(Перевести(?: заново)?)"', n)
        bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if tm and bm:
            buttons.append((tm.group(1), tuple(map(int, bm.groups()))))
    # Find NBT title bounds
    title_b, title_t = bounds_for(xml, TARGET_EN, contains=True)
    if not title_b:
        # already translated?
        for t in texts(xml):
            if "лучш" in t.lower() or "Следующ" in t:
                title_b, title_t = bounds_for(xml, t)
                break
    if not title_b:
        return None, None, buttons
    # Pick translate button with closest vertical center to title
    ty = (title_b[1] + title_b[3]) // 2
    best = None
    best_d = 10**9
    for label, b in buttons:
        by = (b[1] + b[3]) // 2
        d = abs(by - ty)
        if d < best_d:
            best_d = d
            best = (label, b)
    return title_t, best, buttons


CHROME = {
    "Библиотека", "Фанфики", "Прочее", "Сканировать", "Импорт", "Настройки",
    "Фильтры", "Фэндомы", "Пейринги", "Платоника", "Теги", "Ещё",
    "Поиск по названию", "Поиск по описанию", "Перевести", "Перевести заново",
    "Отправить", "Отмена", "Назад", "Читать", "Светлая", "Тёмная", "Система",
    "Показать", "Сохранить", "Очистить ключ", "Ключ задан",
}


def wait_for_ru_metadata(timeout_sec: int = 300) -> tuple[str, list[str]]:
    """Wait until library shows Cyrillic title for NBT (or PARTIAL/COMPLETE)."""
    deadline = time.time() + timeout_sec
    last_status = None
    while time.time() < deadline:
        xml = dump("wait.xml")
        ts = texts(xml)
        statuses = [t for t in ts if t.startswith("Перевод:")]
        if statuses != last_status:
            log("status", statuses, "left", int(deadline - time.time()))
            last_status = statuses
        for t in ts:
            if "Ошибка" in t or "HTTP" in t or "429" in t or "Нужен Gemini" in t:
                log("UI_ERR", t)
        for t in ts:
            if t in CHROME or t.startswith("Перевод:") or t.endswith("?"):
                continue
            if is_fake_prefix(t):
                log("FAIL_FAKE_PREFIX", t[:80])
                return "FAKE", ts
            if has_cyrillic(t) and (
                "лучш" in t.lower()
                or "следующ" in t.lower()
                or ("вещ" in t.lower() and 5 < len(t) < 90)
            ):
                log("SAW_RU_TITLE", t)
                return t, ts
        # After PARTIAL+, if EN title gone and a short Cyrillic headline appeared
        if any("PARTIAL" in s or "COMPLETE" in s for s in statuses):
            if TARGET_EN not in ts:
                for t in ts:
                    if t in CHROME or t.startswith("Перевод:") or len(t) > 90 or len(t) < 5:
                        continue
                    if has_cyrillic(t) and not t.startswith("Перевод"):
                        log("SAW_LIKELY_RU_TITLE", t)
                        return t, ts
        time.sleep(8)
    return "", texts(dump("wait-timeout.xml"))


def main():
    adb("connect", SERIAL)
    time.sleep(1)
    if adb("shell", "echo", "ok").stdout.strip() != "ok":
        log("FAIL: adb shell dead")
        sys.exit(1)

    launch()
    xml = dump("01-library.xml")
    ts = texts(xml)

    # Settings key check
    if not tap_text(xml, "Настройки"):
        tap_xy(1814, 84, "settings-fallback")
    xml = dump("02-settings.xml")
    ts = texts(xml)
    RESULTS["key_configured"] = "Ключ задан" in ts
    log("KEY", RESULTS["key_configured"], ts[:15])
    if not RESULTS["key_configured"]:
        log("FAIL: Gemini key not configured")
        sys.exit(2)
    tap_text(xml, "Назад")

    xml = dump("03-library.xml")
    title, btn, all_btns = find_nbt_translate(xml)
    log("NBT title", title, "btn", btn, "all_btns", len(all_btns))
    if not btn:
        # scroll? or use first Перевести
        if not tap_text(xml, "Перевести") and not tap_text(xml, "Перевести заново"):
            log("FAIL: no translate button")
            sys.exit(3)
    else:
        label, b = btn
        tap_xy((b[0] + b[2]) // 2, (b[1] + b[3]) // 2, label)

    time.sleep(1.5)
    xml = dump("04-confirm.xml")
    if "Отправить" in texts(xml):
        tap_text(xml, "Отправить")
    elif "Перевести?" in " ".join(texts(xml)) or "Перевести заново?" in " ".join(texts(xml)):
        tap_text(xml, "Отправить")
    else:
        log("no confirm dialog?", texts(xml)[:20])

    adb("logcat", "-c")
    log("=== waiting for live RU metadata (up to 5 min) ===")
    ru_title, ts = wait_for_ru_metadata(300)
    RESULTS["ru_title_before_kill"] = ru_title
    RESULTS["live_not_fake"] = bool(ru_title) and ru_title != "FAKE" and not is_fake_prefix(ru_title)
    if not RESULTS["live_not_fake"]:
        # dump logcat hints
        raw = adb("logcat", "-d", "-t", "200").stdout or ""
        hints = [
            ln for ln in raw.splitlines()
            if any(k in ln.lower() for k in ("fandomtranslate", "gemini", "http", "error", "done work"))
            and "key=" not in ln.lower()
        ][-25:]
        log("LOGCAT", *hints, sep="\n")
        log("FAIL: no live Russian title after translate")
        sys.exit(4)

    # Open meta to capture tags before kill
    xml = dump("05-before-meta.xml")
    opened = tap_text(xml, ru_title, contains=True) or tap_text(xml, "лучш", contains=True)
    if opened:
        time.sleep(1.5)
        xml = dump("06-meta-before-kill.xml")
        mts = texts(xml)
        RESULTS["meta_before"] = [t for t in mts if has_cyrillic(t) and len(t) < 80][:20]
        log("META_BEFORE", RESULTS["meta_before"])
        # scroll down for Читать / tags
        adb("shell", "input", "swipe", "960", "900", "960", "300", "400")
        time.sleep(1)
        xml = dump("06b-meta-scrolled.xml")
        RESULTS["meta_before_tags"] = [
            t for t in texts(xml)
            if t in ("Фэндом", "Пейринг", "Рейтинг", "Персонажи", "Доп. теги")
            or (has_cyrillic(t) and len(t) < 60)
        ][:30]
        log("META_TAGS_SAMPLE", RESULTS["meta_before_tags"][:15])
        adb("shell", "input", "keyevent", "4")
        time.sleep(1)

    log("=== force-stop + relaunch ===")
    adb("shell", "am", "force-stop", "com.fandomreader")
    time.sleep(2)
    launch()
    xml = dump("07-after-relaunch.xml")
    ts = texts(xml)
    RESULTS["title_survived"] = any(
        ru_title in t or (has_cyrillic(t) and ("лучш" in t.lower() or "следующ" in t.lower()))
        for t in ts
    )
    RESULTS["english_only_nbt"] = TARGET_EN in ts and not any(
        has_cyrillic(t) and ("лучш" in t.lower() or "следующ" in t.lower()) for t in ts
    )
    log("SURVIVED", RESULTS["title_survived"], "EN_ONLY", RESULTS["english_only_nbt"])
    log("LIB_AFTER", [t for t in ts if len(t) < 70][:25])

    # Open meta again
    title_after = next(
        (t for t in ts if has_cyrillic(t) and ("лучш" in t.lower() or "следующ" in t.lower())),
        ru_title,
    )
    if tap_text(xml, title_after, contains=True) or tap_text(xml, "лучш", contains=True):
        time.sleep(1.5)
        xml = dump("08-meta-after-relaunch.xml")
        mts = texts(xml)
        RESULTS["meta_title_ru"] = any(has_cyrillic(t) and len(t) < 90 for t in mts[:8])
        RESULTS["meta_summary_ru"] = any(
            has_cyrillic(t) and len(t) > 40 for t in mts
        )
        adb("shell", "input", "swipe", "960", "900", "960", "300", "400")
        time.sleep(1)
        xml = dump("08b-meta-tags-after.xml")
        mts2 = texts(xml)
        # Prefer RU display labels for fandom/pairing if translated
        RESULTS["meta_has_tags"] = any(
            t in mts2 for t in ("Фэндом", "Пейринг", "Рейтинг", "Теги")
        )
        RESULTS["meta_tag_cyrillic"] = any(
            has_cyrillic(t) and t not in ("Описание", "Теги", "Фэндом", "Пейринг", "Рейтинг", "Предупреждения", "Персонажи", "Доп. теги", "Читать", "Назад")
            for t in mts2
        )
        log("META_AFTER title_ru", RESULTS["meta_title_ru"], "summary_ru", RESULTS["meta_summary_ru"])
        log("META_AFTER tags", RESULTS["meta_has_tags"], "cyr_tags", RESULTS["meta_tag_cyrillic"])
        log("META_SAMPLE", [t for t in mts2 if len(t) < 70][:25])

    report = OUT / "REPORT.md"
    lines = [
        "# Gemini persist RU metadata (BlueStacks) 2026-09-14",
        "",
        f"- key_configured: {RESULTS.get('key_configured')}",
        f"- live_not_fake: {RESULTS.get('live_not_fake')}",
        f"- ru_title_before_kill: {RESULTS.get('ru_title_before_kill')!r}",
        f"- title_survived: {RESULTS.get('title_survived')}",
        f"- english_only_nbt: {RESULTS.get('english_only_nbt')}",
        f"- meta_title_ru: {RESULTS.get('meta_title_ru')}",
        f"- meta_summary_ru: {RESULTS.get('meta_summary_ru')}",
        f"- meta_has_tags: {RESULTS.get('meta_has_tags')}",
        f"- meta_tag_cyrillic: {RESULTS.get('meta_tag_cyrillic')}",
        "",
    ]
    report.write_text("\n".join(lines), encoding="utf-8")
    log("REPORT", report)
    log("RESULTS", RESULTS)

    ok = (
        RESULTS.get("key_configured")
        and RESULTS.get("live_not_fake")
        and RESULTS.get("title_survived")
        and not RESULTS.get("english_only_nbt")
        and RESULTS.get("meta_title_ru")
        and RESULTS.get("meta_summary_ru")
    )
    if ok:
        log("PASS: live Gemini RU title/summary survived force-stop")
        sys.exit(0)
    log("FAIL: persist check incomplete")
    sys.exit(5)


if __name__ == "__main__":
    main()
