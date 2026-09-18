## ADDED Requirements

### Requirement: Device scan is the primary library fill action
The system SHALL present device library scan as the primary way to add books from local files on the library screen, and SHALL keep manual single-file import available as a secondary action.

#### Scenario: Primary scan action visible on library
- **WHEN** the user opens the library screen
- **THEN** the UI MUST offer a clear primary action to scan typical folders for EPUB/FB2

#### Scenario: Manual import remains available
- **WHEN** the user needs to pick a single file outside the typical folders or prefers manual selection
- **THEN** the system MUST still provide a secondary path to the existing single-file import flow

### Requirement: Library lists stay sorted after scan
After a device scan imports works, the system SHALL show library lists sorted by title (case-insensitive), consistent with the existing shelf and filtered list ordering.

#### Scenario: Works sorted by title after batch import
- **WHEN** a scan imports multiple works onto a shelf
- **THEN** the shelf list MUST display those works ordered by title case-insensitively

#### Scenario: Fandom and pairing filters remain canonical
- **WHEN** scanned works include fandom/pairing metadata
- **THEN** fandom and pairing browsing MUST continue to filter by `canonicalKey` and MUST keep fandom/pairing lists sorted by display name as today
