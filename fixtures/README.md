# Fixtures

Эталонные файлы для парсеров и bake-off перевода.

## Ожидаемая раскладка (после задачи 1.3)

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

Пока образцы могут лежать в корне `fandom-reader/`; при scaffold перенести/скопировать сюда без удаления оригиналов, если пользователь держит их как библиотеку.
