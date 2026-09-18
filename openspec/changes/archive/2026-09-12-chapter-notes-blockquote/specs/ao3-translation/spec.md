## ADDED Requirements

### Requirement: Translation preserves note versus body segments
When translating chapter text that contains recognized author-note or afterword segments, the system SHALL preserve segment boundaries between note regions and primary story body so that after translation the reader can still apply distinct note styling. Legacy caches that lack segment markers MAY fall back to treating the whole chapter as body text.

#### Scenario: Translated notes stay distinguishable
- **WHEN** a chapter with note segments is translated EN→RU and later opened in the reader
- **THEN** the translated note content MUST still be identifiable as notes (not merged indistinguishably into body) so blockquote styling can apply

#### Scenario: Legacy unsegmented cache still readable
- **WHEN** a previously cached chapter translation has no note-segment markers
- **THEN** the system MUST still show the translated text as readable chapter content (body styling is acceptable)
