# Fixtures

Эталонные файлы для парсеров и bake-off перевода. **EPUB/FB2 и скриншоты в git не коммитятся** — держите их локально по этой раскладке или укажите путь через `-Dfandom.fixtures=...` в тестах.

## Ожидаемая раскладка

```
fixtures/
  ao3/
    Next_Best_Thing.epub          # сырой AO3 EPUB
    damaged-fb2/                  # копии FB2 с убитой meta (regression)
  ficbook/
    *.epub                        # экспорты с title.xhtml
  other/
    # обычная книга без portal markers
  mt-samples/
    next-best-thing-summary.txt
    next-best-thing-ch1.txt
  golden/
    next-best-thing.meta.json
    ficbook-*.meta.json
```

Образцы могут также лежать в корне `fandom-reader/` — для разработки это нормально, в репозиторий они не попадают.
