## Context

См. proposal.md — Why. Сейчас: `GeminiSettings` + EncryptedSharedPreferences уже есть; UI настроек отсутствует; в `AppModule.translationProvider` выбор Gemini vs Fake делается один раз при создании Singleton; из библиотеки вызывается только `translateWorkMetadata`; `translateChapter` в ридере не подключён; e2e отмечал, что без ключа всегда Fake.

Ограничения: Material 3; ключ не логировать; Fake в unit/CI; не wipe metadata Calibre/Yandex-путём; фильтры по `canonicalKey`.

## Goals / Non-Goals

**Goals:**

- Экран Settings с полем Gemini API key (save/clear, маскирование)
- Runtime-выбор провайдера по актуальному ключу
- Рабочий путь metadata + chapter translation при наличии ключа; явный UX без ключа
- Регрессионные unit-тесты на Fake / cache resume

**Non-Goals:**

- Несколько провайдеров в UI (OpenAI и т.п.) в этом change — только Gemini; задел на расширение секции
- ML Kit / on-device MT
- Облачный аккаунт / синхронизация ключей
- Полный bake-off качества перевода (есть `tools/mt-bakeoff`)
- Параграфный сегментаж из `warm-paper-reader` (отдельный change)

## Decisions

### 1. Экран Settings в `:app` / feature UI, хранение — существующий `GeminiSettings`

- Переиспользовать `gemini_secure_prefs` / `api_key`.
- Route `settings` из TopAppBar библиотеки («Настройки»).
- Альтернатива: ввод ключа в диалоге при первом Translate — отвергнуто: хуже discoverability и очистка ключа.

### 2. Provider resolver вместо one-shot Hilt bind

- Заменить `if (settings.apiKey.isNotBlank()) gemini else Fake` на обёртку/`TranslationProvider` который на каждый `translateBatch` читает ключ и делегирует в Gemini или отказывает / не маскируется под успех Fake.
- Поведение без ключа: **не** выдавать `[RU]…` как успешный нейро-перевод из UI-кнопки «Перевести»; показать сообщение + deep-link в Settings. Fake остаётся только для DI в тестах и явных debug-сборок при необходимости.
- Альтернатива: `EntryPoint` + пересоздание графа — хрупко.

### 3. Chapter translation entry point

- Подключить вызов `translateChapter` из потока перевода работы (после metadata — последовательный/по запросу в ридере). Минимально: при «Перевести» гонять metadata + главы с прогрессом/PARTIAL; ридер читает кэш `translations/{workId}/chapter_N.txt` если есть.
- Альтернатива: только metadata в этом change — отвергнуто: спека AO3 уже требует главы; «проверить перевод» без глав неполное.

### 4. Сетевой вызов Gemini

- Оставить OkHttp generateContent Flash-Lite; по возможности перевести blocking `execute`/`Thread.sleep` на `withContext(Dispatchers.IO)` + delay — без смены API контракта.
- Ключ только в query/header как сейчас; не логировать URL с ключом.

### 5. Тесты

- Unit: resolver (ключ пустой → отказ для UI-пути / Fake только в test construct); pipeline metadata + chapter cache; Settings ViewModel save/clear на fake prefs при возможности.
- Live Gemini — ручной чеклист (обновить `docs/analysis/e2e-checklist.md`), не CI.

## Risks / Trade-offs

- [Смена поведения «без ключа»] → Раньше UI «успешно» показывал Fake; теперь ошибка/навигация в Settings. Mitigation: явный текст в UI.
- [Долгий перевод всех глав] → Mitigation: статус PARTIAL, кэш/resume, не блокировать UI-поток; главы можно переводить по одной из ридера, если полный job слишком тяжёл — зафиксировать в tasks один выбранный UX (job после metadata **или** on-demand в ридере).
- [EncryptedSharedPreferences на части устройств] → Mitigation: существующий путь; при сбое хранения показать ошибку сохранения ключа.

**Выбор chapter UX (закрыт):** после успешного metadata translation запускать последовательный перевод глав в том же user-initiated job с обновлением статуса; ридер предпочитает кэш RU при наличии.

## Migration Plan

- Нет миграции БД.
- Пользователи с уже записанным ключом в prefs получают UI, отражающий «ключ задан» (masked), без повторного ввода.
- Rollback: убрать route Settings; вернуть one-shot bind — нежелательно.

## Open Questions

- Нужен ли отдельный debug-toggle «разрешить Fake в UI» для демо без ключа — можно отложить; по умолчанию нет.
