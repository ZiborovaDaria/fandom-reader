## ADDED Requirements

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

## MODIFIED Requirements

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
