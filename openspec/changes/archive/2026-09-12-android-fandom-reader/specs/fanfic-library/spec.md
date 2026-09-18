## Purpose

Локальная библиотека фанфиков с полками, фильтрами по фэндому и пейрингу и списком произведений (название + аннотация).

## ADDED Requirements

### Requirement: Dual shelves for tagged and untagged works
The system SHALL keep works with recognized fandom/pairing metadata on the Fanfiction shelf and SHALL place works without such metadata on a separate Other shelf that remains visible to the user.

#### Scenario: Tagged work appears on Fanfiction shelf
- **WHEN** a work has at least one recognized fandom or pairing from AO3 or Ficbook import
- **THEN** the work MUST appear on the Fanfiction shelf and MUST NOT be listed as the only home of that work on the Other shelf

#### Scenario: Untagged work appears on Other shelf
- **WHEN** a work is imported without recognizable fandom/pairing structure
- **THEN** the work MUST appear on the Other shelf and MUST remain readable from that shelf

### Requirement: Browse by fandom then pairing
The system SHALL allow the user to navigate the Fanfiction shelf by selecting a fandom and then a pairing belonging to works in that fandom.

#### Scenario: Filter list by fandom and pairing
- **WHEN** the user selects a fandom and then a pairing
- **THEN** the system MUST show only works that include both the selected fandom and the selected pairing

#### Scenario: Empty filter result
- **WHEN** no works match the selected fandom and pairing
- **THEN** the system MUST show an empty state without erroring

### Requirement: Library list shows title and summary
The system SHALL display each work in library lists with its title and summary (annotation) text suitable for scanning.

#### Scenario: List row content
- **WHEN** the user views a library list of works
- **THEN** each row MUST show the work title and its summary text (truncated if long)

#### Scenario: Missing summary
- **WHEN** a work has no summary
- **THEN** the row MUST still show the title and MUST show a clear empty/placeholder summary state

### Requirement: Canonical tags drive filtering
The system SHALL use stable canonical tag identifiers for fandom and pairing filters so that translated display labels do not create duplicate filter entries.

#### Scenario: Translated display does not duplicate pairing filter
- **WHEN** a pairing has a Russian display label and an English canonical key
- **THEN** filtering by that pairing MUST match a single canonical entry and MUST NOT create two separate filter values for the same pairing
