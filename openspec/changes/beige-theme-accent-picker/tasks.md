## 1. Модели и цвета (`:core:ui`)

- [ ] 1.1 Добавить `AppShellPalette` (Paper, Beige) и `AccentPreset` (10 значений с RU-label) в `core/ui` и проверить, что default = Paper + Forest
- [ ] 1.2 Добавить beige base colors в `Color.kt` (`Beige`, `BeigeInk` и т.п.) и проверить hex из design.md
- [ ] 1.3 Реализовать `shellColorScheme(darkTheme, palette, accent)` с таблицей 10 accent-пар и проверить unit-тестом: Paper+Forest совпадает с текущими `WarmPaperLight`/`WarmPaperNight` primary/background
- [ ] 1.4 Обновить `FandomReaderTheme` — параметры `shellPalette` и `accentPreset`, делегировать в factory; проверить компиляцию `:core:ui`

## 2. Персистентность (`feature/library`)

- [ ] 2.1 Расширить `AppThemePreferencesRepository` ключами `app_shell_palette` и `app_accent_preset` с Flow + setter; проверить `AppThemePreferencesTest` (round-trip, unknown → default)
- [ ] 2.2 Прокинуть palette/accent из repository в `MainActivity` → `FandomReaderTheme`; проверить, что при смене в DataStore тема пересобирается без restart (manual или instrumented smoke)

## 3. UI настроек

- [ ] 3.1 Расширить `SettingsViewModel` state + методы `setShellPalette` / `setAccentPreset`; проверить collect в Route
- [ ] 3.2 Добавить в `SettingsScreen` блок «Палитра»: Бумага | Беж (chip/кнопки); проверить визуально в light mode смену фона shell
- [ ] 3.3 Добавить ряд из 10 accent swatch с подписью выбранного пресета; проверить, что primary-кнопки и выделенные вкладки меняют цвет сразу после tap
- [ ] 3.4 Убедиться, что в dark mode палитра Beige не ломает night shell (остаётся warm night, accent из пресета); проверить переключение Light→Dark→Light с сохранённым Beige

## 4. Контраст и регрессии

- [ ] 4.1 Пройти все 10 пресетов на light и night: кнопки читаемы, system bar icons корректны; зафиксировать правки hex при необходимости
- [ ] 4.2 Убедиться, что reader Paper/Sepia/Night не зависят от shell palette/accent; проверить открытие ридера после смены accent
- [ ] 4.3 Запустить `./gradlew test` и `./gradlew :app:assembleDebug`; оба должны завершиться успешно
