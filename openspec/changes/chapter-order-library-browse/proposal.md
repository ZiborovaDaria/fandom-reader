## Why

Пользователь видит хаотичный TOC (лексикографический порядок файлов: 1, 11, 2…), «простыню» без абзацев в Ficbook EPUB, смешанные `&`/`/` в одном списке пейрингов, дубли EN/RU фэндомов после перевода и слишком узкий browse библиотеки. Нужно исправить чтение и расширить поиск/фильтры на главном экране.

## What Changes

- **BREAKING (import semantics):** `/` → романтические пейринги; `&` → отдельная сущность платонических связей (не в общем списке «пейрингов» как сейчас). Фильтры меню — по обоим типам раздельно.
- Порядок глав в ридере и TOC — **по возрастанию narrative order** (OPF spine / natural chapter order), не по string sort имени файла
- Восстановить абзацы для Ficbook (и аналогичных EPUB), где сейчас сплошной текст
- После перевода **склеивать** фэндомы: EN и RU отображения одной сущности → один canonical filter entry
- Главное меню / библиотека: поиск по названию (в т.ч. частичному), описанию; фильтры по пейрингам (romantic), платонике, доп. тегам, персонажам, категориям и т.п.

## Capabilities

### New Capabilities
- `library-browse`: поиск и расширенные фильтры на главном/библиотечном экране (title/summary, pairings, platonic, display tags)

### Modified Capabilities
- `immersive-reader`: порядок глав ascending; абзацы в Ficbook/problem EPUB
- `portal-import`: split romantic `/` vs platonic `&` в разные сущности; не класть `&` в romantic pairings list
- `ao3-translation`: после перевода merge фэндомов EN/RU в один canonical
- `fanfic-library`: browse/filter UI и модель для platonic vs romantic; обновлённая навигация фильтров

## Impact

- `:feature:reader` — OPF spine / natural order; extractChapterBody для Ficbook markup
- `:source:ao3` / `:source:ficbook` / `:source:api` MetaNormalize — pairing split; canonicalKey больше не схлопывает `&`→`/` для platonic identity
- `:core:domain` / Room — platonic relationships (или typed Pairing + UI split); fandom alias/merge
- `:feature:library` / `:feature:translation` — search UI, filters, post-translate fandom coalesce
- Миграция уже импортированных `&` пейрингов в platonic bucket
- Non-goals: OPF-perfect multi-rendition EPUB edge cases; full-text search engine; editing tags by hand
