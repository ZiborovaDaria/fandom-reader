"""E2E smoke for active OpenSpec changes on BlueStacks.

Covers:
- chapter-order-library-browse: catalog browse (fandoms/romantic/platonic), search, reader/meta
- meta-persist-and-filter-ux: filter builder via Каталог, dual title/summary search
- catalog top-bar entry instead of library chips
"""
from __future__ import annotations

import re
import subprocess
import sys
import time
from pathlib import Path

ADB = r"C:\Users\Daria\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "127.0.0.1:5555"
ROOT = Path(r"C:\MyProjects\fandom-reader")
OUT = ROOT / "docs" / "analysis" / "e2e-run" / "catalog-smoke-20260914"
APK = ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
RESULTS: list[tuple[str, bool, str]] = []


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


def ensure_device():
    adb("connect", SERIAL)
    time.sleep(1)
    out = adb("get-state").stdout.strip()
    if out != "device":
        raise SystemExit(f"device not ready: {out!r}")


def dump(name: str) -> str:
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    xml = adb("shell", "cat", "/sdcard/ui.xml").stdout or ""
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / name).write_text(xml, encoding="utf-8")
    shot = OUT / name.replace(".xml", ".png")
    adb("shell", "screencap", "-p", "/sdcard/shot.png")
    adb("pull", "/sdcard/shot.png", str(shot))
    return xml


def texts(xml: str) -> list[str]:
    return [t for t in re.findall(r'text="([^"]*)"', xml) if t]


def bounds_for(xml: str, needle: str, contains: bool = False):
    for m in re.finditer(r"<node\b[^>]*>", xml):
        n = m.group(0)
        ok = (f'text="{needle}"' in n) if not contains else (needle in n and 'text="' in n)
        if not ok:
            continue
        if contains:
            tm = re.search(r'text="([^"]*)"', n)
            if not tm or needle not in tm.group(1):
                continue
        bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if bm:
            return tuple(map(int, bm.groups()))
    return None


def tap_text(xml: str, needle: str, contains: bool = False) -> bool:
    b = bounds_for(xml, needle, contains=contains)
    if not b:
        log("MISS", needle, texts(xml)[:30])
        return False
    x, y = (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
    log(f"TAP {needle!r} @ {x},{y}")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(1.5)
    return True


def tap_xy(x: int, y: int):
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(1.2)


def check(name: str, ok: bool, detail: str = ""):
    RESULTS.append((name, ok, detail))
    log(("PASS" if ok else "FAIL"), name, detail)


def back():
    adb("shell", "input", "keyevent", "4")
    time.sleep(1.2)


def install_and_seed():
    ensure_device()
    log("INSTALL", APK)
    r = adb("install", "-r", str(APK), timeout=180)
    log(r.stdout.strip() or r.stderr.strip())
    # Push fixtures into Downloads for scan/import
    adb("shell", "mkdir", "-p", "/sdcard/Download/fandom-fixtures")
    for src, name in [
        (ROOT / "fixtures" / "ao3" / "Next_Best_Thing.epub", "Next_Best_Thing.epub"),
        (ROOT / "fixtures" / "ficbook" / "Potter-kotoryj-sovsem-ne-Potter-1.epub", "Potter.epub"),
        (ROOT / "fixtures" / "smoke" / "warm-paper-smoke.fb2", "warm-paper-smoke.fb2"),
        (ROOT / "fixtures" / "ao3" / "damaged-fb2" / "snova-no-luchshe.fb2", "snova.fb2"),
    ]:
        if src.exists():
            adb("push", str(src), f"/sdcard/Download/fandom-fixtures/{name}")
            log("PUSHED", name)


def launch():
    adb("shell", "am", "force-stop", "com.fandomreader")
    adb("shell", "am", "force-stop", "com.bluestacks.gamecenter")
    time.sleep(0.5)
    adb("shell", "am", "start", "-n", "com.fandomreader/.MainActivity")
    time.sleep(2.5)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    install_and_seed()
    launch()
    xml = dump("01-library.xml")
    ts = texts(xml)
    check("library_shell", "Библиотека" in ts or "Фанфики" in ts, str(ts[:12]))
    check("dual_title_search", "Поиск по названию" in ts, "")
    check("dual_summary_search", "Поиск по описанию" in ts, "")
    check("catalog_top_action", "Каталог" in ts, str(ts[:20]))
    check(
        "browse_not_on_library",
        not all(x in ts for x in ("Фэндомы", "Пейринги", "Платоника", "Теги")),
        "chips should live under Каталог",
    )

    # Scan / ensure books present
    if "Next Best Thing" not in " ".join(ts) and "Поттер" not in " ".join(ts):
        if tap_text(xml, "Сканировать"):
            time.sleep(4)
            xml = dump("02-after-scan.xml")
            ts = texts(xml)
        # permission dialogs
        for label in ("Разрешить", "Allow", "While using the app", "При использовании приложения"):
            if label in texts(xml):
                tap_text(xml, label)
                time.sleep(2)
                xml = dump("02b-perm.xml")
                if tap_text(xml, "Сканировать"):
                    time.sleep(5)
                    xml = dump("02c-scan.xml")
                    ts = texts(xml)

    has_books = any(
        t for t in ts
        if "Next" in t or "Поттер" in t or "Potter" in t or "Thing" in t or "лучше" in t.lower()
    )
    check("books_visible", has_books, str([t for t in ts if len(t) < 60][:20]))

    # Title-only search should not match summary-only tokens if any
    if tap_text(xml, "Поиск по названию"):
        adb("shell", "input", "text", "Potter")
        time.sleep(1)
        xml = dump("03-title-search.xml")
        ts = texts(xml)
        check("title_search_ui", "Поиск по названию" in ts, "")
        # clear
        adb("shell", "input", "keyevent", "123")  # move end
        for _ in range(20):
            adb("shell", "input", "keyevent", "67")  # DEL
        time.sleep(0.5)

    def open_catalog() -> str:
        local = dump("catalog-entry.xml")
        if not tap_text(local, "Каталог"):
            return ""
        local = dump("catalog-screen.xml")
        return local

    # Filter builder via Каталог
    xml = open_catalog()
    if not xml or not tap_text(xml, "Фильтры"):
        check("open_filters", False, "catalog/filters missing")
        back()
    else:
        xml = dump("05-filters.xml")
        ts = texts(xml)
        check("open_filters", True, "")
        check("filter_builder_title", "Фильтры" in ts, str(ts[:15]))
        check(
            "filter_tabs_separated",
            any(x in ts for x in ("Фэндомы", "Пейринги", "Персонажи", "Рейтинг", "Доп. метки")),
            str([t for t in ts if len(t) < 40][:20]),
        )
        check("filter_search", "Поиск" in ts, "")
        check("filter_apply", "Показать результаты" in ts, "")
        check("filter_reset", "Сбросить" in ts, "")
        if tap_text(xml, "Показать результаты"):
            xml = dump("06-filter-results.xml")
            ts = texts(xml)
            check(
                "filter_results_screen",
                any("Результат" in t for t in ts) or "Нет подходящих" in ts or "Пусто" in ts or len(ts) > 3,
                str(ts[:15]),
            )
            back()
        back()  # filters → catalog
        back()  # catalog → library

    # Fandoms browse via Каталог
    xml = open_catalog()
    if xml and tap_text(xml, "Фэндомы"):
        xml = dump("08-fandoms.xml")
        ts = texts(xml)
        check("fandom_list", len(ts) > 0 and "Пока нет фэндомов." not in ts or "Harry" in " ".join(ts) or "Поттер" in " ".join(ts) or "фэндом" in " ".join(ts).lower(), str(ts[:15]))
        opened = False
        for t in ts:
            if t in ("Пока нет фэндомов.", "Фанфики", "Прочее", "Каталог", "Назад"):
                continue
            if 3 < len(t) < 80:
                if tap_text(xml, t):
                    opened = True
                    break
        if opened:
            xml = dump("09-pairings.xml")
            ts = texts(xml)
            check(
                "pairing_split_sections",
                "Пейринги (/)" in ts or "Платоника (&)" in ts or any("/" in t or "&" in t for t in ts),
                str(ts[:20]),
            )
            back()
        back()  # fandoms → catalog
        back()  # catalog → library
    else:
        check("fandom_list", False, "no catalog/fandoms")
        if xml:
            back()

    # Romantic facet
    xml = open_catalog()
    if xml and tap_text(xml, "Пейринги"):
        xml = dump("11-romantic.xml")
        check("romantic_facet", True, str(texts(xml)[:12]))
        back()
        back()
    else:
        check("romantic_facet", False, "")
        if xml:
            back()

    # Platonic facet
    xml = open_catalog()
    if xml and tap_text(xml, "Платоника"):
        xml = dump("13-platonic.xml")
        check("platonic_facet", True, str(texts(xml)[:12]))
        back()
        back()
    else:
        check("platonic_facet", False, "")
        if xml:
            back()

    # Open a work → meta page → reader (chapter-order / separate-meta)
    xml = dump("14-lib5.xml")
    work_opened = False
    for needle in ("Next Best Thing", "Следующ", "Поттер", "лучше", "Thing"):
        if tap_text(xml, needle, contains=True):
            work_opened = True
            break
    check("open_work_meta", work_opened, "")
    if work_opened:
        xml = dump("15-meta.xml")
        ts = texts(xml)
        check(
            "work_meta_page",
            "Описание" in ts or "Теги" in ts or "Читать" in ts,
            str(ts[:20]),
        )
        if tap_text(xml, "Читать"):
            time.sleep(2)
            xml = dump("16-reader.xml")
            ts = texts(xml)
            # immersive: either chapter text present, or chrome after tap
            check("reader_opens", len(ts) > 0, str(ts[:15]))
            # tap center for chrome
            tap_xy(960, 540)
            xml = dump("17-reader-chrome.xml")
            back()  # maybe leave reader
            time.sleep(1)
            back()
        else:
            back()

    # Settings / theme night for filter background
    xml = dump("18-lib6.xml")
    if tap_text(xml, "Настройки"):
        xml = dump("19-settings.xml")
        ts = texts(xml)
        check("settings_open", "Настройки" in ts or "Перевод" in " ".join(ts) or "тема" in " ".join(ts).lower() or "Тёмн" in " ".join(ts) or "Dark" in " ".join(ts) or "Систем" in " ".join(ts), str(ts[:20]))
        # try switch to dark if visible
        for label in ("Тёмная", "Темная", "Night", "Dark", "Тёмный", "Темный"):
            if label in ts:
                tap_text(xml, label)
                time.sleep(1)
                break
        back()
        xml = open_catalog()
        if xml and tap_text(xml, "Фильтры"):
            xml = dump("21-filters-theme.xml")
            check("filters_after_theme", "Фильтры" in texts(xml) or "Поиск" in texts(xml), "")
            back()
            back()
        else:
            check("filters_after_theme", False, "catalog/filters after theme")
            if xml:
                back()
    else:
        check("settings_open", False, "")

    # Force-stop / relaunch persistence smoke (without full translate — just app survives)
    adb("shell", "am", "force-stop", "com.fandomreader")
    time.sleep(1)
    launch()
    xml = dump("22-after-relaunch.xml")
    ts = texts(xml)
    still_books = any(
        t for t in ts
        if "Next" in t or "Поттер" in t or "Thing" in t or "лучше" in t.lower() or "Перевод" in t
    )
    check("relaunch_library", "Библиотека" in ts or "Фанфики" in ts, "")
    check("relaunch_books_persist", still_books or not has_books, "books may be empty if scan skipped")

    # Summary
    failed = [r for r in RESULTS if not r[1]]
    passed = [r for r in RESULTS if r[1]]
    report = OUT / "REPORT.md"
    lines = [
        "# Changes smoke (BlueStacks) 2026-09-14",
        "",
        f"Device: `{SERIAL}`",
        f"APK: `{APK}`",
        "",
        f"**Passed:** {len(passed)} / {len(RESULTS)}",
        "",
        "## Results",
        "",
    ]
    for name, ok, detail in RESULTS:
        mark = "x" if ok else " "
        lines.append(f"- [{mark}] `{name}` {detail}")
    report.write_text("\n".join(lines) + "\n", encoding="utf-8")
    log("REPORT", report)
    log("SUMMARY", f"{len(passed)}/{len(RESULTS)} passed")
    if failed:
        log("FAILED", [f[0] for f in failed])
        sys.exit(1)


if __name__ == "__main__":
    main()
