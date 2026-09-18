## ADDED Requirements

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
