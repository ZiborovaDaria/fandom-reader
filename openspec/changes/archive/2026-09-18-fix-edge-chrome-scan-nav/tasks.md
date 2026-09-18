## 1. System bars and insets

- [x] 1.1 Настроить appearance status/navigation bars для light/night shell и Paper/Sepia/Night reader (`MainActivity` / Theme / SideEffect) и проверить, что на paper-фоне иконки тёмные (скрин/ручной чеклист)
- [x] 1.2 Добавить `WindowInsets.statusBars` (и связанные) padding для текста главы в ридере и TopAppBar/Scaffold на browse-экранах; убедиться, что первая строка главы не под status bar

## 2. Reader chrome, Back, scrollbar, progress

- [x] 2.1 Перестроить chrome: opaque панель, prev/next как стрелки, обе кнопки видимы на узкой ширине; unit/Compose или ручная проверка, что next не обрезается
- [x] 2.2 Добавить Back в chrome → `popBackStack` в библиотеку; прокинуть `onBack` из `MainActivity`/`ReaderRoute`; проверить системный Back тем же путём
- [x] 2.3 Показать вертикальный scrollbar только при видимом chrome (вместе с панелью глав); скрыть в immersive
- [x] 2.4 Flush chapter+offset при уходе/ON_STOP (`DisposableEffect`/Lifecycle); тест или ручной reopen mid-chapter

## 3. Library browse Back and search

- [x] 3.1 Добавить TopAppBar/явный Back на экраны фэндомов, пейрингов, тегов и списков работ; проверить `popBackStack` на каждом
- [x] 3.2 Toggle скрытия поиска по названию (вместе с полем описания для экономии места); сохранить query в VM; проверить collapse/expand

## 4. Scan Downloads and custom folder

- [x] 4.1 Усилить discovery Downloads/`Download` (MediaStore + File) и зафиксировать в apply-notes ручной/эмулятор чеклист с `.fb2` в Download
- [x] 4.2 Добавить «Сканировать папку…» (OpenDocumentTree + persist URI + обход DocumentFile); пункт в меню библиотеки; проверить импорт из выбранной папки

## 5. Verification

- [x] 5.1 `./gradlew test` (затронутые модули) — зелёные
- [x] 5.2 `./gradlew :app:assembleDebug` успешно; краткий ручной чеклист: light status bar, Back, стрелки, hide search, custom folder
