## Purpose

Отдельная страница произведения для полного описания и всех тегов вне потока глав ридера, чтобы метаданные не смешивались с чтением.

## ADDED Requirements

### Requirement: Dedicated work meta page
The system SHALL provide a dedicated work page that displays the work description (summary) and all available tags for that work, separate from chapter reading content.

#### Scenario: Open meta page from library
- **WHEN** the user opens a work from a library list
- **THEN** the system MUST show the work meta page with title, full summary (preferring Russian when available), and tags before chapter reading begins

#### Scenario: Summary not mixed into chapter body
- **WHEN** the user views the work meta page
- **THEN** the summary and tags MUST appear on that page and MUST NOT be presented as chapter body text of chapter 1

### Requirement: Show all available tags on meta page
The system SHALL show all tags available for the work on the meta page, including at least fandoms and pairings, and any additional portal tags stored for display when present.

#### Scenario: Fandoms and pairings visible
- **WHEN** a work has stored fandom and pairing tags
- **THEN** the meta page MUST list those tags with their display labels

#### Scenario: Additional display tags when stored
- **WHEN** a work has additional display tags beyond fandom/pairing (for example characters or additional AO3 tags)
- **THEN** the meta page MUST show those tags as well

#### Scenario: Missing tags
- **WHEN** a work has no tags
- **THEN** the meta page MUST still show the description area and MUST present a clear empty state for tags without failing

### Requirement: Start reading from meta page
The system SHALL allow the user to start reading chapters from the meta page without treating the meta page as a chapter.

#### Scenario: Navigate to reader
- **WHEN** the user chooses to start reading from the meta page
- **THEN** the system MUST open the immersive reader at the first story chapter (or restored progress) and MUST NOT treat the meta page as chapter index 0 of the chapter list
