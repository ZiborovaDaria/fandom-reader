## 1. Модель display-тегов

- [x] 1.1 Расширить domain/`ParsedWorkMeta` и Room для display-тегов (группы: rating, warnings, characters, additional и т.п.) без влияния на canonical filter keys; проверить миграцию схемы и unit на сохранение/чтение
- [x] 1.2 В `Ao3EpubParser` (и Ficbook при наличии лейблов) парсить дополнительные поля preface/title page в display-теги; проверить golden/unit на fixture AO3, что Characters/Additional Tags/Rating попадают в meta

## 2. Фильтрация spine глав

- [x] 2.1 В `loadEpub` исключать front-matter (preface, `title.xhtml`, AO3 `split_000`/summary-only и аналоги) и переиндексировать сюжетные главы с 0; проверить unit: AO3 fixture — chapter[0] без блока Fandom/Summary, TOC size = сюжетные главы
- [x] 2.2 Обработать FB2 multi-section: не выкидывать единственную section; при явной meta-section исключать её; проверить unit на damaged AO3 FB2 fixture
- [x] 2.3 Поднять кэш перевода до `translations_v3` (или эквивалент) и писать/читать по новому index; проверить, что старый v2 не подхватывается как текст главы 1
- [x] 2.4 Добавить `tocLabel` (название главы или «Глава N», отброс junk/work title) и использовать в TOC UI; проверить unit: полезный title → как есть; null/workTitle/Preface → «Глава N»

## 3. Страница метаданных и навигация

- [x] 3.1 Добавить `WorkMetaScreen`/`WorkMetaRoute`: title, полное summary (Ru предпочтительно), все теги, CTA «Читать»; проверить Compose/UI: summary и теги видны, нет тела главы
- [x] 3.2 В `MainActivity` все `onOpenWork` вести на `work/{id}`; «Читать» → `reader/{id}`; проверить навигацию list → meta → reader
- [x] 3.3 Из chrome ридера добавить «О книге» (возврат на meta); проверить, что meta не появляется как пункт TOC/глава

## 4. Прогресс и регресс

- [x] 4.1 Clamp/reset `readingProgressChapterIndex` при несовместимости со новым spine (в т.ч. бывший preface на index 0); проверить unit/restore сценарий
- [x] 4.2 Прогнать `:feature:reader` и связанные source/library тесты (`./gradlew test` или точечные модули) и обновить e2e-checklist: meta → глава 1 без preface
