## 1. Парсинг сегментов

- [x] 1.1 Добавить модель `ChapterSegment` (Body / Note) и `extractChapterSegments` в `ChapterText.kt` с детекцией AO3 (`#endnotes*`, `#afterword`, note headings + blockquote) и RU (`Примечания`, `Послесловие`); проверить unit: AO3-like HTML даёт note+body+endnote сегменты
- [x] 1.2 Сохранить совместимый flatten (`extractChapterBody` / join сегментов) для мест, где нужна одна строка; проверить, что существующие paragraph-тесты не ломаются или обновлены осознанно
- [x] 1.3 Подключить сегменты в `loadEpub` / `loadFb2` / отображение главы; проверить unit на fixture-фрагменте FB2 с «Примечания:»

## 2. UI blockquote

- [x] 2.1 В `ChapterBody` рисовать Note-сегменты как blockquote (отступ + tint фона + muted/меньший текст) для Paper/Sepia/Night; проверить визуально или Compose-логикой, что Body остаётся прежним
- [x] 2.2 Убедиться, что in-chapter notes не добавляют отдельный пункт TOC; проверить unit/ручной сценарий на главе с notes

## 3. Перевод

- [x] 3.1 Сериализация/десериализация маркеров границ note/body и сохранение маркеров в `TranslationPipeline` (Fake не трогает маркеры); проверить unit: после translate note-регион всё ещё отличим от body
- [x] 3.2 Legacy кэш без маркеров читается как целиком Body; проверить unit на строке без маркеров

## 4. Проверка

- [x] 4.1 Прогнать `:feature:reader:testDebugUnitTest` и `:feature:translation:testDebugUnitTest` (или эквивалент) — BUILD SUCCESSFUL
- [x] 4.2 Ручной smoke на эмуляторе: Next Best Thing — start/end notes и Afterword в blockquote-стиле, сюжетные абзацы без изменения
