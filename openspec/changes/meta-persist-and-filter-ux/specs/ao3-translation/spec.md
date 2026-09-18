## ADDED Requirements

### Requirement: Persist translated work metadata across restarts
The system SHALL persist Russian title and summary for a translated work in durable library storage so that after an application process restart the library and work meta surfaces continue to show the Russian title and summary when available (preferring Russian over English), not only English source fields.

#### Scenario: Title and summary survive cold start
- **WHEN** an AO3 work has been translated such that Russian title and summary were produced, and the user fully restarts the app
- **THEN** library lists and the work meta page MUST still show the Russian title and summary when those fields were previously available

#### Scenario: Chapters remain independently cached
- **WHEN** chapter translations were already cached and metadata RU fields are present
- **THEN** after restart both chapter Russian text and metadata Russian fields MUST remain available offline

### Requirement: Persist translated fandom and pairing display labels
The system SHALL store Russian display labels for fandoms and pairings associated with a translated work under the same canonical keys, and SHALL reload those labels after restart so UI prefers Russian display text without creating duplicate filter entries.

#### Scenario: Fandom displayRu survives restart
- **WHEN** fandom display labels were translated to Russian for a work and the app restarts
- **THEN** browse and meta UI MUST show the Russian fandom labels for those canonical keys when stored

#### Scenario: Pairing displayRu survives restart
- **WHEN** pairing display labels were translated to Russian for a work and the app restarts
- **THEN** browse and meta UI MUST show the Russian pairing labels for those canonical keys when stored

#### Scenario: Canonical keys unchanged
- **WHEN** Russian display labels are persisted for fandoms or pairings
- **THEN** filter matching MUST continue to use the original `canonicalKey` values and MUST NOT create a second filter entry for the same tag

### Requirement: Persist translated display tags
The system SHALL translate and durably store Russian display values for portal display tags (including at least rating, warnings, characters, and additional tags when present on the work), and SHALL show those Russian values after restart when available while keeping original English values for identity/matching within a tag group.

#### Scenario: Display tags survive restart
- **WHEN** an AO3 work’s display tags were translated to Russian and the app restarts
- **THEN** the work meta page and library tag surfaces MUST show Russian tag values when stored

#### Scenario: Tag identity stable for filtering
- **WHEN** a display tag has both an original value and a Russian display value
- **THEN** filtering by that tag MUST match the same works as filtering by the original value (no duplicate filter entries for the same tag)

### Requirement: Re-import must not wipe translated metadata
When re-importing or upserting a work that already has Russian metadata fields, the system SHALL preserve existing non-blank Russian title, summary, fandom/pairing display labels, and translated display-tag values unless the user explicitly requests a fresh retranslation.

#### Scenario: remoteId rematch keeps titleRu and summaryRu
- **WHEN** a work is re-imported matching an existing `remoteId` and Russian title/summary were already stored
- **THEN** the upsert MUST NOT replace those Russian fields with null or English-only blanks solely because the freshly parsed package lacked RU fields

#### Scenario: Explicit retranslate may refresh RU fields
- **WHEN** the user starts a new translation job for the work
- **THEN** the system MAY update Russian metadata fields as part of that job while still keeping canonical keys stable
