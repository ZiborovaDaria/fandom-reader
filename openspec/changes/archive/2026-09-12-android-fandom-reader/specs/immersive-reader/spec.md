## Purpose

Офлайн-читалка EPUB/FB2 с удобным чтением и минимумом отвлекающих элементов интерфейса.

## ADDED Requirements

### Requirement: Open imported works offline
The system SHALL open imported EPUB and FB2 works for reading without requiring a network connection after the file is stored locally.

#### Scenario: Open EPUB offline
- **WHEN** the user opens an imported EPUB while offline
- **THEN** the system MUST display the work content from local storage

#### Scenario: Open FB2 offline
- **WHEN** the user opens an imported FB2 while offline
- **THEN** the system MUST display the work content from local storage

### Requirement: Immersive reading chrome
While reading, the system SHALL keep the default view focused on text and SHALL reveal reading controls only on explicit user interaction.

#### Scenario: Default reading view hides controls
- **WHEN** the user is actively reading a chapter
- **THEN** the default view MUST show the text without persistent toolbars, FABs, badges, or promotional UI

#### Scenario: Reveal controls on tap
- **WHEN** the user taps the reading area to toggle chrome
- **THEN** the system MUST temporarily show essential controls (at least progress and chapter navigation) and MUST allow returning to the immersive text-only view

### Requirement: Persist reading progress
The system SHALL persist reading progress per work and restore it when the work is reopened.

#### Scenario: Resume at last position
- **WHEN** the user reopens a work after reading part of it
- **THEN** the reader MUST restore the last saved reading position

### Requirement: Prefer translated text when available
For AO3 works with completed chapter translation, the reader SHALL display the Russian translated chapter text by default.

#### Scenario: Show Russian chapter when translation exists
- **WHEN** the user opens a chapter that has a completed Russian translation
- **THEN** the reader MUST show the translated text by default

#### Scenario: Fall back to original when translation missing
- **WHEN** the user opens a chapter without a completed translation
- **THEN** the reader MUST show the original chapter text
