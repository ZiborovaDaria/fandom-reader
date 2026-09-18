## ADDED Requirements

### Requirement: Preserve paragraph boundaries in chapter text
The system SHALL extract and display chapter body text with paragraph boundaries preserved for EPUB and FB2 sources (blank-line or equivalent separation between paragraphs), and MUST NOT collapse the entire chapter into a single unbroken whitespace-normalized string.

#### Scenario: EPUB paragraphs remain separated
- **WHEN** the user opens an EPUB chapter whose source markup contains multiple paragraph elements
- **THEN** the reader MUST show those paragraphs as visually separated blocks (not one continuous run-on paragraph)

#### Scenario: FB2 paragraphs remain separated
- **WHEN** the user opens an FB2 section whose source contains multiple `<p>` elements
- **THEN** the reader MUST show those paragraphs as visually separated blocks

### Requirement: Table of contents jump to chapter
The system SHALL provide a table of contents listing available chapters for the open work and SHALL navigate to the chapter the user selects.

#### Scenario: Open TOC from reading chrome
- **WHEN** the user reveals reading chrome and opens the table of contents
- **THEN** the system MUST list chapters for the current work (using chapter titles when available, otherwise a stable chapter label)

#### Scenario: Jump to selected chapter
- **WHEN** the user selects a chapter entry in the table of contents
- **THEN** the reader MUST display that chapter and MUST close or dismiss the TOC so reading can continue

#### Scenario: Current chapter is identifiable
- **WHEN** the TOC is open
- **THEN** the currently displayed chapter MUST be visually distinguishable in the list

### Requirement: Adjustable reading typography
The system SHALL let the user change reading font family and font size, and SHALL apply line height appropriate for comfortable long-form reading. Chosen typography settings SHALL persist across app restarts.

#### Scenario: Change font family
- **WHEN** the user selects a different reading font family from appearance controls
- **THEN** chapter body text MUST render with that family immediately

#### Scenario: Change font size
- **WHEN** the user increases or decreases reading font size
- **THEN** chapter body text MUST resize within the supported range and MUST remain readable

#### Scenario: Typography persists
- **WHEN** the user changes font family or size and later reopens any work
- **THEN** the reader MUST restore the last saved typography settings

### Requirement: Reading progress includes chapter offset
The system SHALL persist both chapter index and an intra-chapter offset for each work, and SHALL restore both when the work is reopened.

#### Scenario: Resume mid-chapter
- **WHEN** the user leaves a work after scrolling within a chapter
- **THEN** reopening that work MUST restore the same chapter and approximately the same scroll offset

### Requirement: Progress affordance in chrome
When reading chrome is visible, the system SHALL show chapter position progress (at least current chapter index over total, and a progress indication for position in the work or chapter).

#### Scenario: Chrome shows chapter fraction
- **WHEN** reading chrome is visible for a multi-chapter work
- **THEN** the UI MUST show which chapter is active relative to the total chapter count

## MODIFIED Requirements

### Requirement: Immersive reading chrome
While reading, the system SHALL keep the default view focused on text and SHALL reveal reading controls only on explicit user interaction. Essential chrome MUST include progress, chapter navigation, access to the table of contents, and access to appearance controls (theme / font / size), and MUST still allow returning to the immersive text-only view. The default view MUST remain free of persistent toolbars, FABs, badges, or promotional UI.

#### Scenario: Default reading view hides controls
- **WHEN** the user is actively reading a chapter
- **THEN** the default view MUST show the text without persistent toolbars, FABs, badges, or promotional UI

#### Scenario: Reveal controls on tap
- **WHEN** the user taps the reading area to toggle chrome
- **THEN** the system MUST temporarily show essential controls (progress, chapter navigation, TOC entry, appearance entry) and MUST allow returning to the immersive text-only view
