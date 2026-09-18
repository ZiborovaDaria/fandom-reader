# MT bake-off (Next Best Thing)

## Purpose
Compare Gemini Flash-Lite vs notes on AO3 EN→RU slices without destroying metadata.

## Samples
- `fixtures/mt-samples/next-best-thing-summary.txt`
- `fixtures/mt-samples/next-best-thing-ch1.txt`

## How to run (manual)
1. Set env `GEMINI_API_KEY`
2. `python tools/mt-bakeoff/bakeoff.py`
3. Review `docs/analysis/mt-scorecard.md`

## Checklist
- [ ] Names stable (Harry/Severus/Tom)
- [ ] Pairing tags not rewritten into filter keys
- [ ] Summary readable Russian
- [ ] No Unknown/Yandex placeholders
