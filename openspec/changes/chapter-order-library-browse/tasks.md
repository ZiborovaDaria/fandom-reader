## 1. Порядок глав и абзацы

- [x] 1.1 Реализовать порядок EPUB по OPF spine с fallback natural-sort имён файлов; проверить unit: TOC/loadChapters даёт 1→2→…→11, а не 1→11→2
- [x] 1.2 Расширить `extractChapterBody` для Ficbook (block/`br`, не только `<p>`); проверить unit на fixture «Поттер, который совсем не Поттер» — ≥2 абзаца, не одна простыня

## 2. Romantic / platonic split

- [x] 2.1 Убрать схлопывание `&`→`/` в canonicalKey для platonic; импорт AO3/Ficbook кладёт `/` в ROMANTIC и `&` в PLATONIC отдельно; проверить golden/unit и что romantic list не содержит `&`
- [x] 2.2 Миграция/rekey уже сохранённых PLATONIC с искажённым ключом; проверить repository unit
- [x] 2.3 API/UI списков: `observeRomanticPairings` vs platonic facet; проверить, что фасеты не смешиваются

## 3. Merge фэндомов после перевода

- [x] 3.1 После перевода label фэндомов выполнить coalesce EN/RU в один canonical; проверить unit: два alias → один filter entry, filter матчит оба варианта работ

## 4. Поиск и фильтры библиотеки

- [x] 4.1 Поиск по title/titleRu/summary/summaryRu (partial, case-insensitive) на главном/библиотечном экране; проверить unit/Compose: частичное название и описание находят работу
- [x] 4.2 Фильтры по romantic, platonic, display-тегам (characters/additional/category); проверить сценарии AND с поиском
- [x] 4.3 Прогнать связанные module tests и обновить e2e-checklist (TOC order, Ficbook paragraphs, filters/search)
