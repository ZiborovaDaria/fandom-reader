# fanfic-library Specification

## Purpose
Локальная библиотека фанфиков с полками, фильтрами по фэндому и пейрингу и списком произведений (название + аннотация).

## Requirements

### Requirement: Dual shelves for tagged and untagged works
The system SHALL keep works with recognized fandom/pairing metadata on the Fanfiction shelf and SHALL place works without such metadata on a separate Other shelf that remains visible to the user.

#### Scenario: Tagged work appears on Fanfiction shelf
- **WHEN** a work has at least one recognized fandom or pairing from AO3 or Ficbook import
- **THEN** the work MUST appear on the Fanfiction shelf and MUST NOT be listed as the only home of that work on the Other shelf

#### Scenario: Untagged work appears on Other shelf
- **WHEN** a work is imported without recognizable fandom/pairing structure
- **THEN** the work MUST appear on the Other shelf and MUST remain readable from that shelf

### Requirement: Library list shows title and summary
The system SHALL display each work in library lists with its title and summary (annotation) text suitable for scanning. When the summary is longer than the collapsed preview budget, the system SHALL show an inline expand/collapse control immediately adjacent to the last visible character of the collapsed preview (preferably at a word boundary), not as a separate chip far from the truncated text. The expand control MUST remain visually distinct from the translate control on the same row.

#### Scenario: List row content
- **WHEN** the user views a library list of works
- **THEN** each row MUST show the work title and its summary text (collapsed preview if long)

#### Scenario: Collapsed preview is twice the previous budget
- **WHEN** a work has a summary longer than the collapsed preview budget
- **THEN** the row MUST show a collapsed preview of up to 440 characters (twice the previous 220-character budget) and MUST NOT use the old 220-character hard cut as the default preview

#### Scenario: Expand control sits at preview break
- **WHEN** a work summary is truncated for the collapsed preview
- **THEN** the «Ещё» control MUST appear inline immediately after the last visible preview character (with only minimal spacing) and MUST NOT appear on a separate line or block detached from the preview text end

#### Scenario: Expand full summary inline
- **WHEN** the collapsed preview truncates the summary and the user activates the expand control on that row
- **THEN** the system MUST show the full summary text on that same list row and MUST NOT navigate away from the list or open the work

#### Scenario: Collapse expanded summary
- **WHEN** a row summary is expanded and the user activates the collapse control
- **THEN** the system MUST return that row to the collapsed preview state while remaining on the list

#### Scenario: Expand control does not open the work
- **WHEN** the user activates the expand or collapse control on a work row
- **THEN** the system MUST NOT open the work (no navigation to reader or work meta)

#### Scenario: Short summary needs no expand control
- **WHEN** a work summary fits entirely within the collapsed preview budget
- **THEN** the row MUST show the full summary and MUST NOT show an expand control

#### Scenario: Missing summary
- **WHEN** a work has no summary
- **THEN** the row MUST still show the title and MUST show a clear empty/placeholder summary state

#### Scenario: Expand control separate from translate control
- **WHEN** an AO3 work row shows both a truncated summary with «Ещё» and an inline «Перевести» control
- **THEN** «Ещё» MUST remain tied to the summary text break and MUST NOT share the same inline spacing block as «Перевести» (translate MUST stay visually separate, e.g. on the following line or with clear separation)

### Requirement: Canonical tags drive filtering
The system SHALL use stable canonical tag identifiers for fandom and pairing filters so that translated display labels do not create duplicate filter entries.

#### Scenario: Translated display does not duplicate pairing filter
- **WHEN** a pairing has a Russian display label and an English canonical key
- **THEN** filtering by that pairing MUST match a single canonical entry and MUST NOT create two separate filter values for the same pairing

### Requirement: Pairing selection survives slash-bearing canonical keys
The system SHALL allow the user to open the filtered works list from a pairing whose `canonicalKey` contains `/` (or other URI-reserved characters) without crashing, and SHALL pass that same `canonicalKey` unchanged into the fandom+pairing filter.

#### Scenario: Romantic pairing key with slash opens filtered list
- **WHEN** the user selects a pairing whose `canonicalKey` contains `/` (for example `harry-potter/tom-riddle`)
- **THEN** the app MUST navigate to the filtered works list without crashing and MUST filter works by that exact `canonicalKey`

#### Scenario: Platonic pairing normalized with slash opens filtered list
- **WHEN** the user selects a pairing whose stored `canonicalKey` contains `/` because `&` was normalized to `/`
- **THEN** the app MUST navigate without crashing and MUST use the stored `canonicalKey` for filtering

#### Scenario: Filter key is not altered by navigation encoding
- **WHEN** a pairing `canonicalKey` round-trips through navigation to the filtered list screen
- **THEN** the filter MUST receive the original `canonicalKey` byte-for-byte equal to the library filter key (no truncated path segments, no substitution with display labels)

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
For AO3 works that offer neural translation from a library work list, the translate / re-translate control SHALL be visually integrated into the work row content area as a compact inline control, not as a separate full-width button block detached below the row card. When a summary expand control is present, the translate control MUST NOT be laid out as a sibling chip immediately beside «Ещё» in the same horizontal flow row.

#### Scenario: Translate control reads as part of the row
- **WHEN** an AO3 work with translation offered is shown in a library list
- **THEN** the «Перевести» or «Перевести заново» control MUST appear inside the same row content block (title/summary area) as a compact text or icon action and MUST NOT render as a standalone large button spanning the row below the card

#### Scenario: Translate not bundled with expand chip
- **WHEN** a row shows a collapsed long summary with an «Ещё» control and translation is offered
- **THEN** «Перевести» MUST NOT appear in the same horizontal chip row immediately after «Ещё»; it MUST render on a separate line or with clearly distinct spacing from the summary+«Ещё» inline text

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

### Requirement: Library lists stay sorted after scan
After a device scan imports works, the system SHALL show library work lists using the user’s current sort preference (default: last opened, newest first). Fandom and pairing browse lists SHALL remain sorted by display name as today. Filtering MUST continue to use `canonicalKey`.

#### Scenario: Works sorted by title after batch import
- **WHEN** a scan imports multiple works onto a shelf and the active sort is alphabetical
- **THEN** the shelf list MUST display those works ordered by display title case-insensitively in the chosen direction

#### Scenario: Works follow active sort after batch import
- **WHEN** a scan imports multiple works onto a shelf
- **THEN** the shelf list MUST display those works ordered according to the active sort criterion and direction (not forced to title-only if the user selected another sort)

#### Scenario: Default after scan when user never changed sort
- **WHEN** a scan imports works and the user has never overridden sort preference
- **THEN** the shelf list MUST use last-opened descending as the default order

#### Scenario: Fandom and pairing filters remain canonical
- **WHEN** scanned works include fandom/pairing metadata
- **THEN** fandom and pairing browsing MUST continue to filter by `canonicalKey` and MUST keep fandom/pairing lists sorted by display name as today

### Requirement: User-selectable work list sort with two directions
The system SHALL let the user sort library work lists by display title (Russian and English letters in one locale-aware collation), by stored file size, and by last opened time. Each sort criterion SHALL support two directions (ascending and descending). The default sort SHALL be last opened, newest first. Changing sort MUST NOT break fandom/pairing filtering by `canonicalKey`.

#### Scenario: Default sort is last opened descending
- **WHEN** the user opens a shelf or filtered works list without having chosen a different sort preference
- **THEN** works MUST appear ordered by last opened time, most recently opened first (works never opened MUST sort after opened ones in a stable secondary order)

#### Scenario: Alphabetical sort handles Russian and English titles
- **WHEN** the user selects alphabetical sort in either direction
- **THEN** the list MUST order works by the title shown in the list (prefer Russian display title when present, otherwise original title) using case-insensitive locale-aware collation that correctly interleaves Cyrillic and Latin scripts

#### Scenario: Size sort both directions
- **WHEN** the user selects size sort ascending or descending
- **THEN** the list MUST order works by stored file size in the chosen direction

#### Scenario: Last opened sort both directions
- **WHEN** the user selects last-opened sort ascending or descending
- **THEN** the list MUST order works by last opened time in the chosen direction

#### Scenario: Sort preference persists
- **WHEN** the user changes sort criterion or direction and later returns to a work list
- **THEN** the previously chosen sort MUST still apply until the user changes it again

### Requirement: Restore list position after leaving a work
When the user opens a work from a library work list and then returns to that list, the system SHALL restore scroll so the opened work remains visible in approximately the same list place (same work row, not forced to the top of the list).

#### Scenario: Return from reader to same list row
- **WHEN** the user opens a work from a scrolled library list and then navigates back to that list
- **THEN** the list MUST show the previously opened work row on screen without requiring the user to scroll from the top to find it

#### Scenario: Restore survives configuration change
- **WHEN** the user opens a work from a list, rotates the device or the process is briefly recreated while still in that navigation flow, and returns to the list
- **THEN** the system MUST still restore to the opened work row when possible

### Requirement: Stable filter builder tab and selection state
While the user is on the extended filter builder, the system SHALL keep the active filter tab and all in-progress checkbox selections stable across background library Flow updates (import, translation metadata refresh, sort-related re-queries). Changing the picker search query on one tab MUST NOT switch the user to a different filter category tab.

#### Scenario: Typing search does not change active tab
- **WHEN** the user is on a display-tag tab (for example «Категории») and types in the picker search field
- **THEN** the filter builder MUST remain on that same tab and MUST NOT jump to «Пейринги» or another tab

#### Scenario: Library refresh does not clear selections
- **WHEN** the underlying library works Flow emits again while the filter builder is open and the user has selected fandoms, pairings, or display tags
- **THEN** those selections MUST remain checked and MUST NOT flicker off and on

#### Scenario: Tab identity survives tab list shrink
- **WHEN** the set of available filter tabs changes because new tag groups appear or disappear in the library catalog
- **THEN** the builder MUST preserve the user's active tab by category identity (not by numeric index alone) and MUST keep the picker search query scoped to that tab

### Requirement: Tabbed display-tag categories in filters
The system SHALL present display-tag filter categories on separate tabs (characters, rating, warnings, additional tags, category, genre when such tags exist in the library). The extended filter builder MUST NOT expose a «Размер» / file-size display-tag tab; size metadata MAY still appear on the work meta page.

#### Scenario: Separate tab for characters
- **WHEN** the library contains character display tags and the user opens the characters tab on the filter builder
- **THEN** the system MUST show character tags for selection without mixing rating tags into that same tab list

#### Scenario: Separate tab for additional tags
- **WHEN** the library contains additional display tags and the user opens the «Доп. метки» tab
- **THEN** the system MUST show additional tags for selection on that tab only

#### Scenario: Size tab not in filter builder
- **WHEN** the user opens the extended filter builder and works contain size display tags
- **THEN** the filter builder MUST NOT show a «Размер» tab and MUST NOT offer size as a filter category

#### Scenario: Search within a tag tab
- **WHEN** the user is on a display-tag tab and types a search query
- **THEN** the system MUST filter that tab’s entries with case-insensitive substring matching and MUST keep the user on that tab

### Requirement: Stable filter picker interaction
On the filter builder screen, filter tabs and selectable filter rows (fandoms, pairings, display tags) SHALL remain stable under normal Flow updates so that tapping a row toggles selection reliably without visible flicker that prevents interaction.

#### Scenario: Toggle fandom without flicker blocking tap
- **WHEN** the user taps a fandom (or pairing/tag) row to select or deselect it while library data may be refreshing
- **THEN** the row MUST register the tap, update selection state, and MUST NOT remount or jump in a way that makes consecutive taps unreliable

#### Scenario: Tab selection survives data refresh
- **WHEN** the user has selected a non-first filter tab and underlying available filter data refreshes without removing that tab
- **THEN** the selected tab MUST remain selected and MUST NOT reset to the first tab solely because of that refresh

### Requirement: Translation action below work row content
For AO3 works that offer neural translation from a library work list, the translate / re-translate control SHALL appear below the row’s title and summary content, not as a side trailing action beside the title.

#### Scenario: Re-translate control is under the summary
- **WHEN** an AO3 work with a non-NONE translation status is shown in a library list that offers translation
- **THEN** the «Перевести заново» (or equivalent) control MUST be laid out under the title/summary block and MUST NOT sit as a trailing side button next to the title

#### Scenario: First translate control placement
- **WHEN** an AO3 work with no translation yet is shown in a list that offers translation
- **THEN** the «Перевести» control MUST use the same below-content placement

### Requirement: Title search can be hidden
On the library screen, the system SHALL allow the user to collapse or hide the title-search field so it does not permanently occupy vertical space, and SHALL let the user reveal it again. Any entered title query MUST be retained while the field is collapsed unless the user clears it.

#### Scenario: Collapse title search
- **WHEN** the user chooses to hide the title search
- **THEN** the title-search field MUST no longer occupy the main library list area as an always-visible input

#### Scenario: Reveal title search again
- **WHEN** the user chooses to show title search after it was hidden
- **THEN** the title-search field MUST reappear and MUST still contain the previous query if one was entered

### Requirement: Browse screens provide Back
On fandom, pairing, tag, and filtered-works browse screens in the library navigation stack, the system SHALL provide an explicit Back control that returns to the previous screen in that stack.

#### Scenario: Back from fandom list
- **WHEN** the user is on the fandom browse screen
- **THEN** the UI MUST offer Back that returns to the prior library/catalog context

#### Scenario: Back from pairing or works list
- **WHEN** the user is on a pairing list or a pairing/filtered works list
- **THEN** the UI MUST offer Back that returns to the previous browse screen without leaving the app
