## Purpose

Поиск и расширенные фильтры библиотеки: название/описание, романтические и платонические связи, display-теги (персонажи, категории и др.).

## ADDED Requirements

### Requirement: Search works by title and summary
The system SHALL allow the user to search the library by work title (including partial match) and by summary/description text.

#### Scenario: Partial title match
- **WHEN** the user enters a non-empty query that is a substring of a work title (original or Russian title when present)
- **THEN** the matching work MUST appear in search results

#### Scenario: Summary match
- **WHEN** the user searches with text that appears in a work summary (original or Russian when present)
- **THEN** that work MUST appear in search results

#### Scenario: Empty query clears search constraint
- **WHEN** the search query is empty
- **THEN** the system MUST not restrict results by search text alone

### Requirement: Filter by romantic and platonic relationships separately
The system SHALL provide browse/filter entry points for romantic pairings and for platonic relationships as separate facets.

#### Scenario: Filter by romantic pairing
- **WHEN** the user selects a romantic pairing facet
- **THEN** the system MUST show works that include that romantic pairing and MUST NOT require a platonic match

#### Scenario: Filter by platonic relationship
- **WHEN** the user selects a platonic relationship facet
- **THEN** the system MUST show works that include that platonic relationship

### Requirement: Filter by display tags
The system SHALL allow filtering works by stored display-tag groups such as characters, additional tags, and categories when those tags exist on works.

#### Scenario: Filter by character tag
- **WHEN** the user selects a character display tag that is stored on one or more works
- **THEN** the result list MUST include only works that have that character tag

#### Scenario: Filter by category or additional tag
- **WHEN** the user selects a category or additional display tag
- **THEN** the result list MUST include only works that have that tag
