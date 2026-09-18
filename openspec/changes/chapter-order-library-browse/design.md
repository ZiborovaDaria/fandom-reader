## Context

См. proposal.md — Why. Сейчас `loadEpub` сортирует xhtml по `entry.name` → TOC «1, 11, 2…». `extractChapterBody` берёт только `<p>`; у части Ficbook глав текст в других блоках/`div` → одна «простыня». `MetaNormalize.canonicalKey` заменяет `&` на `/`, схлопывая platonic и romantic. Фэндомы после перевода могут дублироваться как разные `canonicalKey`. Browse: фэндом → пейринг, без search/display-tag filters. Решение пользователя: вариант **B** — `/` romantic, `&` отдельная platonic сущность.

## Goals / Non-Goals

**Goals:**
- Spine/TOC ascending; Ficbook paragraphs; romantic/platonic split; fandom merge post-translate; library search + tag filters

**Non-Goals:**
- Полнотекстовый индекс (Room LIKE / in-memory достаточно)
- Ручное редактирование тегов
- Идеальный OPF для всех malformed EPUB (fallback natural sort)

## Decisions

### 1. Порядок глав: OPF spine, fallback natural sort
- Читать `spine/itemref` из OPF; сопоставить id→href; исключить front-matter как сейчас.
- Fallback: natural/alphanumeric compare имени файла (`1` < `2` < `11`), не `String.compareTo`.
- **Альтернатива:** только natural sort — проще, но хуже, когда имена не отражают spine.

### 2. Абзацы Ficbook
- Расширить extraction: `<p>`, затем block-level (`div` с текстом, `br`-разделы), не `body.text()` целиком.
- Fixture: «Поттер, который совсем не Поттер»; unit на ≥2 paragraphs.

### 3. Platonic как отдельная модель (вариант B)
- Domain: `PlatonicRelationship` (или `Pairing` + UI/repo API `observeRomantic` / `observePlatonic`) с собственным `canonicalKey` **без** `&`→`/` rewrite.
- Room: либо `type` уже есть на pairings + раздельные queries; либо таблица `work_platonics`. Предпочтение: оставить `Pairing.type`, **убрать** `&`→`/` в `canonicalKey`, UI/API не смешивают списки.
- Import: `/` → ROMANTIC; `&` → PLATONIC; Ficbook splitPairingField аналогично.
- Миграция: существующие PLATONIC с ключом, где `&` уже стал `/`, пересчитать ключ из `displayName`.

### 4. Merge фэндомов после перевода
- После `displayRu` / translate fandom labels: alias map или merge rows с одинаковым смыслом (нормализация: strip «- J. K. Rowling», compare displayRu↔displayName через translation dictionary / same work co-occurrence + fuzzy).
- MVP: merge когда `displayRu` одной записи equals `displayName` другой, или оба ключа связаны через известный alias после batch translate fandom names to RU and re-key to shared canonical (prefer Russian key or stable EN slug + displayRu).
- Фильтры только по merged `canonicalKey`.

### 5. Library browse UI
- Поиск: TopAppBar/field → filter Flow по title/titleRu/summary/summaryRu contains (case-insensitive).
- Фасеты: Фэндомы | Пейринги (romantic) | Платоника | Теги (characters/additional/category…) — chips или вложенные списки.
- Комбинирование: AND между выбранными фасетами + search query.

## Risks / Trade-offs

- [Spine parse fails] → Mitigation: natural-sort fallback + test on Ficbook + AO3 fixtures
- [Ficbook markup без `<p>`] → Mitigation: block/`br` heuristics; snapshot test
- [Merge фэндомов ложно склеит разные] → Mitigation: conservative rules; only merge with explicit displayRu link or identical normalized names
- [Старые `&` ключи уже как `/`] → Mitigation: one-shot rekey from displayName containing `&`

## Migration Plan

1. Reader order + paragraphs (user-visible immediately).
2. Stop `&`→`/` collapse; split import; migrate DB types/keys.
3. Fandom merge on translate path + optional one-shot coalesce.
4. Library search/filter UI.

## Open Questions

- Нет блокирующих: UI layout фасетов (tabs vs bottom sheet) можно уточнить на apply.
