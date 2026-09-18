## ADDED Requirements

### Requirement: Merge equivalent fandoms after translation
After fandom display labels are translated (or Russian display labels are set), the system SHALL merge fandom entries that represent the same fandom so that English and Russian labels resolve to a single canonical filter entry.

#### Scenario: EN and RU labels become one filter entry
- **WHEN** translation produces a Russian display label for a fandom that already exists under an English display/canonical form for the same conceptual fandom
- **THEN** library fandom browse/filter MUST show a single merged entry rather than two duplicate fandom rows for that fandom

#### Scenario: Filtering uses merged canonical identity
- **WHEN** the user filters by the merged fandom entry
- **THEN** works tagged with either former EN or RU variant MUST match that single filter
