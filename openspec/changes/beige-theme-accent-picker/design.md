## Context

См. `proposal.md`. Сейчас:

- `AppThemeMode` (Light / Dark / System) в DataStore управляет только яркостью shell.
- `FandomReaderTheme` собирает фиксированные `WarmPaperLight` / `WarmPaperNight` из `Color.kt`; акцент — `Accent` (#2F5D50) и `NightAccent` (#8FBFB0).
- Настройки (`SettingsScreen`) показывают только переключатель Light/Dark/System.
- Reader Paper/Sepia/Night — отдельный `ReaderSurfaceTheme`, не трогаем.

## Goals / Non-Goals

**Goals:**

- Вторая светлая shell-палитра «Беж» + 10 приглушённых accent-пресетов.
- Персистентность в DataStore, мгновенное применение через `FandomReaderTheme`.
- Контраст кнопок и system bars сохраняется.
- Unit-тесты prefs и сборки схемы.

**Non-Goals:**

- Произвольный color picker / HSV.
- Dynamic Color / Material You как основа бренда.
- Смена reader surface themes (Paper/Sepia/Night).
- Отдельная «бежевая ночь» — night base остаётся warm night, меняется только accent.

## Decisions

### 1. Два независимых pref: `AppShellPalette` и `AccentPreset`

- `AppShellPalette`: `Paper` (default) | `Beige` — влияет только на light shell background/surface/ink.
- `AccentPreset`: enum из 10 значений — влияет на `primary` / `primaryContainer` / `tertiary` (light + night пары).
- `AppThemeMode` не расширяем четвёртым значением «Beige» — бежевое это палитра светлого shell, не режим яркости.

**Альтернатива:** объединить в один enum «тема» — отвергнута: ломает модель Light/Dark/System.

### 2. Палитра цветов (light base + accent pairs)

**Light shell bases** (фон / текст — как сейчас для Paper, новое для Beige):

| Palette | Background | Ink | Surface variant hint |
|---------|------------|-----|----------------------|
| Paper (default) | `#F7F3EC` | `#1C1B19` | `#D8E6E0` (accent-soft, пересчитывается от пресета) |
| Beige | `#EDE4D3` | `#1C1B19` | тёплый muted tint от accent (~12–18% alpha на beige) |

**10 accent presets** (light primary → night primary). Все приглушённые, без неона:

| ID | RU label | Light `primary` | Night `primary` |
|----|----------|-----------------|-----------------|
| `Forest` (default) | Лесной | `#2F5D50` | `#8FBFB0` |
| `Slate` | Сланцевый | `#4A5D6B` | `#8FA3B0` |
| `Plum` | Сливовый | `#6B4A5D` | `#B09BA8` |
| `Olive` | Оливковый | `#5D5A3F` | `#A8A58F` |
| `Rust` | Терракотовый | `#7A4E3E` | `#C4A090` |
| `Teal` | Морской | `#3F5D58` | `#8FB5AD` |
| `Mauve` | Лиловый | `#5D4F6B` | `#A89FB8` |
| `Brown` | Кофейный | `#5C4A3A` | `#B0A090` |
| `Denim` | Джинсовый | `#3F4F6B` | `#90A0B8` |
| `Sage` | Шалфей | `#4F5D4A` | `#A0B09A` |

`primaryContainer` (light): accent @ ~15% alpha на фоне палитры; night: приглушённый tint на `NightSurface`.

`onPrimary`: светлый фон палитры на light primary; `NightBoard` на night primary — как сейчас.

### 3. Сборка темы в `FandomReaderTheme`

```kotlin
fun FandomReaderTheme(
    darkTheme: Boolean,
    shellPalette: AppShellPalette = AppShellPalette.Paper,
    accentPreset: AccentPreset = AccentPreset.Forest,
    readerSurfaceTheme: ReaderSurfaceTheme = ReaderSurfaceTheme.Paper,
    content: @Composable () -> Unit,
)
```

Фабрика `shellColorScheme(dark, palette, accent)` заменяет хардкод `WarmPaperLight`/`WarmPaperNight`. Существующие call sites без новых параметров получают defaults.

### 4. DataStore keys

В `AppThemePreferencesRepository`:

- `app_shell_palette` → `AppShellPalette.name`
- `app_accent_preset` → `AccentPreset.name`

Миграция не нужна: отсутствие ключей = Paper + Forest.

### 5. UI в Settings

Под блоком «Тема приложения» (Light/Dark/System):

1. **Палитра** — два `FilterChip` / `TextButton`: «Бумага» | «Беж» (disabled hint или скрытие не нужно в dark — выбор сохраняется, применяется при возврате в light).
2. **Цвет акцента** — горизонтальный ряд из 10 круглых swatch (36dp), выбранный — обводка `primary`; tap → `setAccentPreset`. Подпись текущего пресета текстом.

Без FAB в ридере; настройки — единственная точка входа в v1.

### 6. Контраст и system bars

- Перед фиксацией пресетов прогнать пары `primary`/`onPrimary` и night-аналоги; при fail WCAG AA для normal text на кнопках — слегка затемнить light primary или скорректировать `onPrimary` (только в enum, не в runtime).
- `SystemBarAppearance` без изменений логики: dark icons в light shell, light icons в night.

### 7. Тесты

- `AppThemePreferencesTest`: round-trip palette + accent; unknown stored value → default.
- `ShellColorSchemeTest` (или расширить `ReaderSurfaceColorsTest`): Paper+Forest = текущие hex; Beige меняет background; смена accent меняет primary.

## Risks / Trade-offs

- [10 пресетов × 2 режима = много комбинаций] → Mitigation: табличные unit-тесты на factory, не скриншоты всех 20.
- [Beige + некоторые accent могут сливаться] → Mitigation: `primaryContainer` от accent, не фиксированный зелёный soft; ручная проверка в apply.
- [Prefs в feature/library, enums в core/ui] → Mitigation: уже так с `AppThemeMode`; repository импортирует enum из `:core:ui`.

## Migration Plan

1. Добавить enums и color factory в `:core:ui`.
2. Расширить repository + MainActivity wiring.
3. UI settings + тесты.
4. Rollback: удалить UI; defaults Paper+Forest — визуально как сейчас.

## Open Questions

- (нет — scope зафиксирован в proposal/spec)
