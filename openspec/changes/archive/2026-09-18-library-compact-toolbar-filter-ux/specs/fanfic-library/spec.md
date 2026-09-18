## ADDED Requirements

### Requirement: Compact icon toolbar on main library list
The system SHALL present search, sort, and filter controls on the main library work list as compact icon actions that expand on demand. When collapsed, these controls MUST NOT occupy persistent text-button rows or full-width fields above the work list.

#### Scenario: Search collapsed by default
- **WHEN** the user opens the main library shelf list
- **THEN** title/summary search fields MUST be hidden and MUST be reachable via a search icon action

#### Scenario: Search expands on icon tap
- **WHEN** the user taps the search icon on the main library list
- **THEN** the system MUST show the existing title and summary search fields and MUST allow collapsing them again without leaving the list

#### Scenario: Sort collapsed by default
- **WHEN** the user opens the main library shelf list
- **THEN** the sort criterion and direction MUST NOT show as a persistent text row above the list; they MUST be reachable via a sort icon action

#### Scenario: Sort expands on icon tap
- **WHEN** the user taps the sort icon on the main library list
- **THEN** the system MUST show the sort criterion and direction controls (same options as today) and MUST allow collapsing them again

#### Scenario: Filter icon on main screen
- **WHEN** the user views the main library screen top actions
- **THEN** a filter icon action labeled for filtering (not «Каталог») MUST be visible without opening the overflow menu

#### Scenario: Filter icon opens extended builder directly
- **WHEN** the user taps the filter icon on the main library screen
- **THEN** the system MUST open the extended filter builder immediately and MUST NOT show an intermediate catalog hub screen

### Requirement: Inline translation control on work row
For AO3 works that offer neural translation from a library work list, the translate / re-translate control SHALL be visually integrated into the work row content area as a compact inline control, not as a separate full-width button block detached below the row card.

#### Scenario: Translate control reads as part of the row
- **WHEN** an AO3 work with translation offered is shown in a library list
- **THEN** the «Перевести» or «Перевести заново» control MUST appear inside the same row content block (title/summary area) as a compact text or icon action and MUST NOT render as a standalone large button spanning the row below the card

#### Scenario: Translate confirmation unchanged
- **WHEN** the user activates the inline translate control
- **THEN** the system MUST still show the existing confirmation dialog before starting translation

### Requirement: Optional combinable extended filter criteria
On the extended filter builder, all filter categories SHALL remain optional. The user MUST be able to confirm with zero, one, or multiple categories filled. When multiple categories are selected, matching MUST continue to use AND across categories and OR within a multi-select category, filtering by `canonicalKey` for fandoms and pairings.

#### Scenario: Confirm with only fandom selected
- **WHEN** the user selects one or more fandoms and leaves pairings and display-tag categories empty, then confirms
- **THEN** results MUST include works matching the selected fandom(s) without requiring pairing or tag selections

#### Scenario: Confirm with only one tag category
- **WHEN** the user selects tags in a single display-tag tab and leaves fandoms and pairings empty, then confirms
- **THEN** results MUST include works matching those tag selections

#### Scenario: Combined categories still AND
- **WHEN** the user selects a fandom and a rating tag, then confirms
- **THEN** each listed work MUST satisfy both selections

#### Scenario: Empty confirm shows guidance
- **WHEN** the user confirms the filter builder with no criteria selected
- **THEN** the system MUST NOT apply a stale filter and MUST show clear guidance that at least one criterion is needed (or equivalent empty-selection handling without error)

### Requirement: Last-opened sort semantics are preserved
Refactoring library toolbar and sort UI MUST NOT change last-opened sort behavior. Default sort SHALL remain last opened, newest first. Ordering rules for `LastOpened` ascending and descending MUST remain identical to the current implementation.

#### Scenario: Default last-opened order unchanged
- **WHEN** the user has not changed sort preference and opens a shelf list
- **THEN** works MUST appear ordered by last opened time descending (most recently opened first; never-opened works after opened ones in the same stable secondary order as today)

#### Scenario: Last-opened direction toggle unchanged
- **WHEN** the user selects last-opened sort and toggles direction
- **THEN** the list MUST reorder by last opened time in the chosen direction using the same comparator semantics as before this change

#### Scenario: Compact sort UI uses same backend preference
- **WHEN** the user changes sort via the collapsed/expanded sort control
- **THEN** the stored sort preference and list ordering MUST match what the previous full sort bar produced for the same selection

## MODIFIED Requirements

### Requirement: Device scan is the primary library fill action
The system SHALL keep device library scan available for adding books from local files, with folder scan in the overflow menu and empty-state actions as the primary entry points. The library top bar MUST NOT show a separate persistent «Сканировать» text button when «Сканировать папку…» is already available in the overflow menu. Manual single-file import SHALL remain available as a secondary action.

#### Scenario: Primary scan action visible on library
- **WHEN** the user opens the library screen
- **THEN** the UI MUST offer a clear action to scan for EPUB/FB2 via overflow «Сканировать папку…» and/or empty-state scan, without requiring a duplicate top-bar «Сканировать» button

#### Scenario: Folder scan reachable from overflow
- **WHEN** the user opens the library screen overflow menu
- **THEN** the UI MUST offer «Сканировать папку…» to scan a user-selected folder for EPUB/FB2

#### Scenario: No duplicate scan button on top bar
- **WHEN** the user views the library top bar in the default state
- **THEN** a standalone «Сканировать» action MUST NOT appear alongside the overflow menu

#### Scenario: Empty state still offers scan
- **WHEN** the library shelf is empty and no search is active
- **THEN** the empty state MUST still offer a scan action and manual import

#### Scenario: Manual import remains available
- **WHEN** the user needs to pick a single file outside the typical folders or prefers manual selection
- **THEN** the system MUST still provide a secondary path to the existing single-file import flow from the overflow menu or empty state

## REMOVED Requirements

### Requirement: Browse by fandom then pairing
**Reason**: Drill-down каталог (фэндом → пейринг) заменён прямым расширенным фильтром с иконки на главном экране; отдельные browse-экраны убираются из UI.
**Migration**: Пользователь открывает «Фильтр» на главном экране библиотеки, выбирает фэндом и/или пейринг (и другие критерии) в `FilterBuilder`, затем смотрит результаты.
