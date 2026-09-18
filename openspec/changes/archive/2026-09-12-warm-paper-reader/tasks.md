## 1. Warm paper theme

- [x] 1.1 Подключить Paper/Ink/Accent (+ Night companions) к `lightColorScheme`/`darkColorScheme` в `FandomReaderTheme` и проверить визуально/скрином: library background ≈ Paper, не default M3 white
- [x] 1.2 Добавить reader surface tokens Paper/Sepia/Night (bg+fg) в `:core:ui` и проверить, что CompositionLocal/helper отдаёт три различимых пары цветов

## 2. Абзацы в парсинге и UI

- [x] 2.1 Заменить схлопывание `Jsoup…text()` на извлечение абзацев для EPUB и FB2; проверить unit-тестом на фикстуре с ≥2 `<p>`, что текст содержит разделители абзацев
- [x] 2.2 Рендерить главу как набор абзацев (колонка Text / spacing) с учётом `textRu` fallback; проверить Compose/unit: два абзаца не сливаются в одну визуальную простыню

## 3. Reader preferences (шрифт, размер, тема)

- [x] 3.1 DataStore prefs: font family (Serif/Sans/Mono), font size, line height, reader surface theme; проверить unit/instrumented: запись → чтение после «перезапуска» scope
- [x] 3.2 Sheet/панель Aa в chrome: смена family/size/theme применяется к тексту сразу; проверить Compose-тестом или ручным чеклистом по сценариям `immersive-reader` / `warm-paper-theme`
- [x] 3.3 Убедиться, что без сохранённых prefs дефолт = Paper + разумный size/line height; проверить тестом дефолтных значений

## 4. Оглавление и прогресс

- [x] 4.1 TOC sheet/list из `chapters` с подсветкой текущей главы и jump по tap; проверить Compose/unit: выбор главы N меняет `chapterIndex` на N
- [x] 4.2 Chrome: chapter fraction + progress affordance + entry points TOC/Aa; проверить, что default view по-прежнему без persistent FAB/toolbar
- [x] 4.3 Сохранять и восстанавливать `readingProgressOffset` (не всегда 0); проверить: scroll → recreate/reopen → offset приблизительно восстановлен

## 5. Перевод глав с абзацами

- [x] 5.1 `translateChapter` сегментирует по абзацам и пишет кэш с сохранением границ; FakeTranslationProvider сохраняет separators — проверить unit-тестом multi-paragraph round-trip
- [x] 5.2 Стратегия старых кэшей без абзацев (v2 path или invalidate) задокументирована в коде/комментарии и проверена: повторный перевод или миграция не оставляет «простыню» как единственный путь

## 6. Сборка и регрессия

- [x] 6.1 `./gradlew :app:assembleDebug` и релевантные unit-тесты reader/translation/theme проходят
- [x] 6.2 Ручной smoke: импорт → открытие главы с абзацами → Night/Aa → TOC jump → resume offset
