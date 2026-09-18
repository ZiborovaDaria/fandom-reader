## Why

На телефоне в светлой теме system status bar (время/заряд) нечитаем; контент ридера и browse уезжает под status bar; chrome главы обрезает «next», нет стрелок и «Назад» из книги/фэндомов/пейрингов; скан типичных папок всё ещё ненадёжен для `Download`, нет выбора своей папки; поиск по названию занимает экран; позиция чтения должна надёжно сохраняться при уходе.

## What Changes

- Светлая (и Paper/Sepia) тема: тёмные иконки status bar; тёмная/Night — светлые; контент с `statusBars` insets, чтобы первая строка не наползала на панель.
- Скан: приоритет/гарантия обхода Downloads (`Download`), плюс действие «выбрать папку» (SAF tree + persist) для пользовательского каталога.
- Ридер chrome: prev/next как стрелки; явная «Назад» в библиотеку/меню; боковой scrollbar только вместе с chrome по тапу; opaque/непрозрачная нижняя панель (без перекрытия текста).
- Browse (фэндомы, пейринги, теги, списки работ): явная «Назад» на каждом экране стека.
- Прогресс чтения: надёжный flush главы+offset при уходе с экрана/сворачивании, restore при открытии.
- Библиотека: поиск по названию можно свернуть/скрыть (toggle), чтобы не мешал списку.

## Capabilities

### New Capabilities

- (нет)

### Modified Capabilities

- `warm-paper-theme`: контраст system status/navigation bars относительно paper/night shell и reading surfaces.
- `immersive-reader`: стрелки глав, Back из книги, scrollbar+chrome, insets, flush progress.
- `device-library-scan`: Downloads как обязательный типичный корень + user-selected folder scan.
- `fanfic-library`: hideable title search; Back на экранах browse (фэндом/пейринг/…).

## Impact

- `MainActivity` / `FandomReaderTheme` / WindowInsetsController; `ReaderScreen` chrome и insets; `LibraryScreens` browse TopAppBar + search collapse; `TypicalBookFolders` / `MediaStoreBookDiscovery` / `LibraryViewModel` + OpenDocumentTree.
- DataStore/prefs для persist URI папки скана и флага скрытия поиска.
- Тесты: insets/progress flush (unit где возможно), UI smoke для Back/arrows; ручной чеклист Downloads + light status bar.
- Не меняет правила полок Fanfiction/Other и portal classify.
