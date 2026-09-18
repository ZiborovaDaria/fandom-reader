#!/usr/bin/env python3
"""Minimal Gemini bake-off for Next Best Thing slices."""
from __future__ import annotations

import json
import os
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SAMPLES = ROOT / "fixtures" / "mt-samples"
OUT = ROOT / "docs" / "analysis" / "mt-scorecard.md"
MODEL = "gemini-2.0-flash-lite"


def translate(text: str, key: str) -> str:
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent?key={key}"
    payload = {
        "contents": [
            {
                "parts": [
                    {
                        "text": (
                            "Translate literary fanfiction text from English to Russian. "
                            "Keep character names consistent. Return only translation.\n\n"
                            + text
                        )
                    }
                ]
            }
        ]
    }
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=120) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    return data["candidates"][0]["content"]["parts"][0]["text"].strip()


def main() -> None:
    key = os.environ.get("GEMINI_API_KEY", "").strip()
    lines = ["# MT scorecard — Next Best Thing", ""]
    if not key:
        lines += [
            "GEMINI_API_KEY not set — skipped live call.",
            "",
            "Manual checklist:",
            "- names stable",
            "- tags not duplicated",
            "- readable RU summary",
        ]
        OUT.write_text("\n".join(lines), encoding="utf-8")
        print("wrote placeholder", OUT)
        return

    for name in ("next-best-thing-summary.txt", "next-best-thing-ch1.txt"):
        src = (SAMPLES / name).read_text(encoding="utf-8")
        ru = translate(src[:4000], key)
        lines += [f"## {name}", "", "### EN (trim)", "", src[:800], "", "### RU", "", ru, ""]

    lines += [
        "## Checklist",
        "- [ ] Names stable",
        "- [ ] Filter keys unchanged",
        "- [ ] Readable Russian",
    ]
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
