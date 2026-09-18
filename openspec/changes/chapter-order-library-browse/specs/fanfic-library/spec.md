## ADDED Requirements

### Requirement: Library navigation exposes romantic and platonic facets
The Fanfiction library browse flow SHALL expose separate navigation or filter facets for romantic pairings and platonic relationships, in addition to fandom browse.

#### Scenario: Romantic facet available from library
- **WHEN** the user is on the main library / Fanfiction browse surface and romantic pairings exist
- **THEN** the UI MUST provide a way to browse or filter by romantic pairings

#### Scenario: Platonic facet available from library
- **WHEN** platonic relationships exist in the library
- **THEN** the UI MUST provide a way to browse or filter by those platonic relationships separately from romantic pairings
