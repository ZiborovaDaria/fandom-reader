import re, subprocess, sys, time, pathlib

ADB = r"C:\Users\Daria\AppData\Local\Android\Sdk\platform-tools\adb.exe"
SERIAL = "127.0.0.1:5555"
OUT = pathlib.Path(r"docs\analysis\e2e-run")

def adb(*args):
    return subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, text=True, encoding="utf-8", errors="replace")

def dump(name):
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    p = adb("shell", "cat", "/sdcard/ui.xml")
    path = OUT / name
    path.write_text(p.stdout, encoding="utf-8")
    return p.stdout

def texts(xml):
    return re.findall(r'text="([^"]*)"', xml)

def find_text_bounds(xml, needle):
    # Prefer exact, then substring
    for pat in [
        rf'text="{re.escape(needle)}"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
        rf'text="([^"]*{re.escape(needle)}[^"]*)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"',
    ]:
        m = re.search(pat, xml)
        if m:
            gs = m.groups()
            if len(gs) == 4:
                return tuple(map(int, gs)), needle
            return tuple(map(int, gs[1:])), gs[0]
    return None, None

def smallest_clickable_containing(xml, tb):
    tx1, ty1, tx2, ty2 = tb
    best = None
    for m in re.finditer(r'clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"|bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"[^>]*clickable="true"', xml):
        vals = [int(x) for x in m.groups() if x is not None]
        x1,y1,x2,y2 = vals
        if x1 <= tx1 and y1 <= ty1 and x2 >= tx2 and y2 >= ty2:
            area = (x2-x1)*(y2-y1)
            if best is None or area < best[0]:
                best = (area, x1,y1,x2,y2)
    return best[1:] if best else tb

def tap_text(xml, needle, label):
    tb, found = find_text_bounds(xml, needle)
    if not tb:
        print(f"FAIL: text not found: {needle}")
        print("Available:", [t for t in texts(xml) if t][:30])
        sys.exit(1)
    bounds = smallest_clickable_containing(xml, tb)
    cx = (bounds[0]+bounds[2])//2
    cy = (bounds[1]+bounds[3])//2
    print(f"TAP {label}: '{found}' @ {cx},{cy} bounds={bounds}")
    adb("shell", "input", "tap", str(cx), str(cy))
    time.sleep(2)

# ensure app foreground
adb("shell", "monkey", "-p", "com.fandomreader", "-c", "android.intent.category.LAUNCHER", "1")
time.sleep(2)
xml = dump("fix-nav-01-launch.xml")
print("LAUNCH texts:", [t for t in texts(xml) if t][:20])
pkg = "com.fandomreader" if "com.fandomreader" in xml else "?"
print("package hint ok" if pkg == "com.fandomreader" else "WARN wrong package")

tap_text(xml, "Фэндомы", "fandoms")
xml = dump("fix-nav-02-fandoms.xml")
print("FANDOMS:", [t for t in texts(xml) if t])

# open Harry Potter fandom
tap_text(xml, "Harry Potter", "fandom")
xml = dump("fix-nav-03-pairings.xml")
print("PAIRINGS:", [t for t in texts(xml) if t])

# romantic pairing with slash in display (canonical has /)
target = None
for t in texts(xml):
    if "/" in t and "Harry" in t:
        target = t
        break
if not target:
    # any slash pairing
    for t in texts(xml):
        if "/" in t:
            target = t
            break
if not target:
    print("FAIL: no slash pairing found")
    sys.exit(1)

tap_text(xml, target, "pairing-with-slash")
xml = dump("fix-nav-04-filtered.xml")
print("FILTERED texts:", [t for t in texts(xml) if t][:30])

# success criteria: still in our app, not crashed to launcher/store, and not empty error crash
still = "com.fandomreader" in xml
has_content = any(t for t in texts(xml) if t and t not in ("",))
crashed = ("isn't responding" in xml.lower()) or ("has stopped" in xml.lower())
print("RESULT still_app=", still, "has_content=", has_content, "crashed=", crashed)
if still and not crashed:
    print("PASS: pairing selection did not crash")
    sys.exit(0)
else:
    print("FAIL")
    sys.exit(1)