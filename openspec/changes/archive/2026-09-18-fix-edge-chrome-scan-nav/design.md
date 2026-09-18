## Context

См. `proposal.md` — Why. Наблюдения после `fix-scan-import-reader-ux`:

- `enableEdgeToEdge()` без `SystemBarStyle` / light-status appearance → белые иконки на paper.
- Ридер: chrome TextButton `‹ prev` / `next ›` в одном ряду → next обрезается; нет Back; нет statusBars padding; scrollbar нет; progress debounce без flush on leave.
- Browse-экраны без TopAppBar/Back.
- Скан: MediaStore+File есть, custom folder нет; Downloads на устройстве не подтверждён.
- Поиск по названию всегда виден.

Ограничения AGENTS: immersive default text-first; no persistent FAB; canonicalKey filters; no MANAGE_EXTERNAL_STORAGE primary.

## Goals / Non-Goals

**Goals:**

- Читаемые system bars на light/night/paper/sepia/night-reader.
- Insets: текст главы и browse headers не под status bar.
- Chrome: стрелки глав, Back→библиотека, scrollbar только с chrome, непрозрачная панель.
- Скан Downloads + OpenDocumentTree custom folder.
- Hideable title search; Back на browse stack.
- Flush progress при уходе.

**Non-Goals:**

- Полный редизайн темы / Dynamic Color.
- Постоянный scrollbar или постоянный chrome в ридере.
- All-files access / рекурсивный скан всего устройства.
- Изменение правил полок / classify.

## Decisions

### 1. Status bar appearance

- **Выбор:** `enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto/light/dark)` + Compose `SideEffect` / `WindowInsetsControllerCompat.isAppearanceLightStatusBars` от `isSystemInDarkTheme()` и reader surface (Paper/Sepia → light bars appearance true; Night → false).
- **Альтернативы:** только `themes.xml` `windowLightStatusBar` — слабее при edge-to-edge и смене reader theme.

### 2. Insets

- **Выбор:** reader content `Modifier.windowInsetsPadding(WindowInsets.statusBars)` (и bottom chrome учитывает navigationBars). Browse screens — Scaffold/TopAppBar с insets, не «голый» LazyColumn под status bar.
- **Альтернативы:** фиксированный `padding(48.dp)` — хрупко на вырезах.

### 3. Reader chrome layout

- **Выбор:** нижняя opaque панель: ряд1 — Back | ← | глава N/M | → ; ряд2 — Оглавление | Aa | О книге; справа `VerticalScrollbar` / `Scrollbar` tied to scroll state, visible iff chrome. Стрелки IconButton/`←`/`→`, не длинный Text.
- **Back:** `popBackStack` в библиотеку (меню), не WorkMeta. «О книге» остаётся отдельно.
- **Альтернативы:** Back только system gesture — на части устройств неочевидно.

### 4. Progress flush

- **Выбор:** при `DisposableEffect` onDispose / Lifecycle ON_STOP вызывать немедленный `saveProgress`; оставить debounce для scroll.
- **Альтернативы:** только debounce — теряется позиция при быстром Back.

### 5. Custom scan folder

- **Выбор:** `OpenDocumentTree` + `takePersistableUriPermission`; обход DocumentFile на `.epub`/`.fb2`; URI в DataStore; пункт в ⋮ «Сканировать папку…». Default «Сканировать» по-прежнему typical+MediaStore с упором на Downloads.
- **Альтернативы:** только MediaStore — недостаточно для произвольных папок пользователя.

### 6. Hide title search

- **Выбор:** иконка/текст «Поиск» toggle; по умолчанию свёрнут на узком экране **или** развёрнут с кнопкой скрыть — зафиксируем: **по умолчанию поля видны, кнопка «Скрыть поиск»** сворачивает title field (description search тоже сворачивается вместе, чтобы освободить место). Query в VM сохраняется.
- **Альтернативы:** скрывать только title, оставлять description — меньше выгоды по высоте.

## Risks / Trade-offs

- [OEM MediaStore всё ещё пуст для FB2] → Mitigation: custom folder + Import; улучшить Downloads query.
- [Persistable URI отозван] → Mitigation: сообщение + повторный picker.
- [Scrollbar API на Compose] → Mitigation: простой трек/thumb по `scrollState` value/max; не системный View scrollbar.
- [Двухрядный chrome съест высоту] → Mitigation: opaque panel ok; immersive hide по тапу.

## Migration Plan

- Клиентский апдейт; DataStore keys для tree URI и searchCollapsed.
- Rollback APK-совместим.

## Open Questions

- Нет (Back = библиотека зафиксировано в proposal).
