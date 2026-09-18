# E2E на BlueStacks (127.0.0.1:5555) — 2026-09-12

## Окружение
- APK: app-debug.apk установлен успешно
- Фикстуры: /sdcard/Download/fandom-fixtures/

## Результаты
| Сценарий | Статус |
|---|---|
| Запуск приложения / Библиотека | OK — вкладки Фанфики/Прочее, Импорт, Фэндомы |
| Импорт AO3 EPUB (Next Best Thing) | OK — title+summary на полке Фанфики |
| Импорт Ficbook EPUB | OK — «Поттер, который совсем не Поттер» |
| Импорт damaged AO3 FB2 | OK — recovered «Но лучше арсенал» → Фанфики (не Прочее) |
| Вкладка Прочее | OK — Пусто (все портальные книги на Фанфики) |
| Фэндомы → пейринги | OK — HP AO3 + Ficbook фэндом; romantic/platonic |
| Ридер EPUB | OK — текст preface; тап → chrome «Глава 1/7», prev/next |
| Перевести (Fake) | OK — статус PARTIAL, titleRu/summaryRu с префиксом [ru] |

## Замечания
- BlueStacks Store иногда перехватывает фокус при неточных тапах
- Кнопка «Перевести» работает при точном тапе по ней (внутри ListItem)
- Gemini не тестировался (нет ключа в encrypted prefs → Fake provider)
