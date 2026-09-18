## ADDED Requirements

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

## MODIFIED Requirements

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
