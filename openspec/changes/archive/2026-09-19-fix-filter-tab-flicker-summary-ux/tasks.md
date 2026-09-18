## 1. FilterBuilder — стабильные вкладки и selection

- [x] 1.1 Заменить `selectedTabIndex: Int` на `selectedTab: FilterTab` с вычислением index для `ScrollableTabRow`; при смене `availableTabs` сохранять вкладку по identity — проверить вручную: на «Категории» ввести поиск, вкладка не переключается на «Пейринги»
- [x] 1.2 Стабилизировать `availableTabs`: фиксированный порядок enum (без SIZE), distinct tag groups из ViewModel/repository с `distinctUntilChanged`; показывать tag-вкладку если группа в каталоге или есть selection в `selectedTags` — проверить: «Персонажи»/«Доп. метки» не мигают при фоновом emit works
- [x] 1.3 Убедиться, что `selectedFandoms` / `selectedPairings` / `selectedTags` / `pickerQuery` не сбрасываются при recomposition от Flow — проверить: отметить чекбокс, дождаться обновления списка works, selection остаётся
- [x] 1.4 Удалить `FilterTab.SIZE` и mapping на `DisplayTagGroups.SIZE` из `FilterBuilderScreen.kt` — проверить: вкладки «Размер» нет, фильтр по size недоступен; `WorkMetaScreen` по-прежнему показывает размер

## 2. WorkListRow — inline «Ещё» и отделение «Перевести»

- [x] 2.1 Добавить helper обрезки summary по word boundary в пределах `COLLAPSED_SUMMARY_MAX` — проверить unit-тест на строках с пробелами и без пробела в окне
- [x] 2.2 Перестроить `WorkListRow`: один inline `Text`/`ClickableText` с «Ещё»/«Свернуть» сразу после preview; «Перевести» на отдельной строке (`Column`) — проверить Compose: «Ещё» видно, клик не открывает работу
- [x] 2.3 Обновить `WorkListComposeTest`: expand/collapse и translate confirmation; добавить проверку, что «Перевести» не в одном горизонтальном ряду с «Ещё» (отдельный semantics node / отсутствие общего FlowRow) — `./gradlew :feature:library:testDebugUnitTest`

## 3. Регрессия FilterBuilder

- [x] 3.1 Добавить тест (Compose или ViewModel): при повторном emit works активная вкладка и selected tags не сбрасываются — тест зелёный в `:feature:library:testDebugUnitTest`
- [x] 3.2 Прогнать `./gradlew :feature:library:testDebugUnitTest` и `./gradlew :app:assembleDebug` — сборка и тесты без ошибок
