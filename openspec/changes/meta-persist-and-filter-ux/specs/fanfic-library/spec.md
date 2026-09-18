## ADDED Requirements

### Requirement: Dedicated filter builder before results
The system SHALL provide a dedicated library filter screen where the user selects all desired filter criteria first, then opens a results list of matching works. The filter builder MUST NOT require a desktop-style persistent sidebar; on mobile it MUST be a separate step before browsing results.

#### Scenario: Select filters then view matching works
- **WHEN** the user opens the library filter builder, selects one or more criteria, and confirms
- **THEN** the system MUST show a results list containing only works that match the combined selection

#### Scenario: Empty combined result
- **WHEN** the selected combination matches no works
- **THEN** the system MUST show an empty state without erroring

#### Scenario: Clear or reset filters
- **WHEN** the user resets all selections on the filter builder
- **THEN** the builder MUST clear selected criteria so a subsequent confirm does not apply stale filters

### Requirement: Search within fandom and pairing pickers
Within the filter builder’s fandom and pairing selection lists, the system SHALL provide a text search that matches display names with case-insensitive full-text and partial-word (substring) matching against both original and Russian display labels when present.

#### Scenario: Partial fandom name match
- **WHEN** the user types a substring of a fandom display name (for example part of “Harry Potter”)
- **THEN** the fandom picker MUST list fandoms whose display name or Russian label contains that substring

#### Scenario: Partial pairing name match
- **WHEN** the user types a substring of a pairing display name
- **THEN** the pairing picker MUST list pairings whose display name or Russian label contains that substring

#### Scenario: No match shows empty picker
- **WHEN** the search query matches no entries in the current picker list
- **THEN** the picker MUST show an empty state without leaving the filter builder

### Requirement: Tabbed display-tag categories in filters
The system SHALL present display-tag filter categories on separate tabs (at least characters, rating, and additional tags when such tags exist in the library), and MUST NOT dump characters, ratings, and other display-tag groups into a single undifferentiated pile on the filter builder.

#### Scenario: Separate tab for characters
- **WHEN** the library contains character display tags and the user opens the characters tab on the filter builder
- **THEN** the system MUST show character tags for selection without mixing rating tags into that same tab list

#### Scenario: Separate tab for rating
- **WHEN** the library contains rating display tags and the user opens the rating tab
- **THEN** the system MUST show rating tags for selection on that tab

#### Scenario: Search within a tag tab
- **WHEN** the user is on a display-tag tab and types a search query
- **THEN** the system MUST filter that tab’s entries with the same case-insensitive substring matching rules as fandom/pairing pickers

### Requirement: Combinable multi-criteria library filters
The system SHALL allow combining selections across filter categories (fandoms, pairings, and display-tag groups). Matching works MUST satisfy all selected categories (AND across categories). Within a single category that allows multi-select, a work MUST match if it includes at least one of the selected values in that category (OR within category), unless the UI explicitly documents a stricter mode. Fandom and pairing identity for filtering MUST continue to use `canonicalKey`, not translated display strings.

#### Scenario: Fandom and rating combined
- **WHEN** the user selects a fandom and a rating tag, then views results
- **THEN** each listed work MUST include that fandom (`canonicalKey`) and that rating display tag

#### Scenario: Multiple pairings within category
- **WHEN** the user selects two pairings in the pairing category and no other category constraints
- **THEN** the results MUST include works that have either pairing (OR within pairings)

#### Scenario: Translated labels do not break combination
- **WHEN** selected fandoms/pairings have Russian display labels
- **THEN** combined filtering MUST still match via `canonicalKey` and MUST NOT require the user to pick English labels separately

### Requirement: Keep slash-safe canonical keys in filter builder results
When navigating from the filter builder to results with a pairing `canonicalKey` that contains `/` or other URI-reserved characters, the system SHALL pass that key unchanged into the filter evaluation (same contract as existing pairing navigation).

#### Scenario: Combined filter with slash pairing key
- **WHEN** the user includes a pairing whose `canonicalKey` contains `/` in a combined filter and opens results
- **THEN** the app MUST show the filtered list without crashing and MUST filter by that exact `canonicalKey`
