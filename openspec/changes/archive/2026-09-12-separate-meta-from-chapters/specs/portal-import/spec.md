## ADDED Requirements

### Requirement: Capture display tags for meta page
For portal imports, the system SHALL capture display-oriented tags needed for the work meta page in addition to fandom and pairing filter keys, without using translated display strings as filter keys.

#### Scenario: AO3 additional tag labels stored for display
- **WHEN** an AO3 EPUB preface contains structured fields beyond Fandom and Relationships (for example Characters, Additional Tags, Rating, or Archive Warning)
- **THEN** the system MUST store those values for display on the work meta page when present

#### Scenario: Filter keys remain canonical
- **WHEN** additional display tags are stored
- **THEN** fandom/pairing library filters MUST continue to use `canonicalKey` values and MUST NOT key filters off translated display strings of those additional tags
