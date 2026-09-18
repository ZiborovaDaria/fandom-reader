## MODIFIED Requirements

### Requirement: Immersive reading chrome
While reading, the system SHALL keep the default view focused on text and SHALL reveal reading controls only on explicit user interaction. Essential chrome MUST include progress, chapter navigation as clear previous/next arrow controls, access to the table of contents, access to appearance controls (theme / font / size), a back control that returns to the library/menu, and a vertical chapter scrollbar affordance. Chrome surfaces MUST remain opaque enough that chapter body text does not show through control labels. The system MUST still allow returning to the immersive text-only view. The default view MUST remain free of persistent toolbars, FABs, badges, or promotional UI.

#### Scenario: Default reading view hides controls
- **WHEN** the user is actively reading a chapter
- **THEN** the default view MUST show the text without persistent toolbars, FABs, badges, or promotional UI

#### Scenario: Reveal controls on tap
- **WHEN** the user taps the reading area to toggle chrome
- **THEN** the system MUST temporarily show essential controls (progress, previous/next chapter arrows, TOC entry, appearance entry, back-to-library, and the vertical scrollbar) and MUST allow returning to the immersive text-only view

#### Scenario: Chapter arrows remain reachable
- **WHEN** reading chrome is visible on a narrow phone width
- **THEN** both previous and next chapter arrow controls MUST remain visible and tappable (not clipped off-screen)

#### Scenario: Back leaves the reader to library
- **WHEN** the user activates the back control from reading chrome
- **THEN** the app MUST leave the reader and return to the library/menu navigation context

### Requirement: Reading progress includes chapter offset
The system SHALL persist both chapter index and an intra-chapter offset for each work, SHALL flush that progress when the user leaves the reader or the app stops the reading screen, and SHALL restore both when the work is reopened.

#### Scenario: Resume mid-chapter
- **WHEN** the user leaves a work after scrolling within a chapter
- **THEN** reopening that work MUST restore the same chapter and approximately the same scroll offset

#### Scenario: Progress flushed on leave
- **WHEN** the user navigates away from the reader (back to library) after changing chapter or scroll position
- **THEN** the system MUST persist the latest chapter index and offset before or as the screen is left so a later reopen restores that position

## ADDED Requirements

### Requirement: Chapter text clears the status bar
The system SHALL inset the reading text area below the system status bar (and other relevant top system insets) so the first line of chapter text does not draw under or overlap the status bar region.

#### Scenario: First line below status bar
- **WHEN** the user opens a chapter in the reader under edge-to-edge display
- **THEN** the first visible line of chapter text MUST sit below the status bar and MUST remain fully readable

### Requirement: Scrollbar only with reading chrome
The system SHALL show a vertical scrollbar (or equivalent scroll-position affordance along the chapter edge) only while reading chrome is visible after the user toggles chrome, and MUST hide it again when chrome is dismissed.

#### Scenario: Scrollbar hidden in immersive mode
- **WHEN** reading chrome is hidden
- **THEN** the vertical scrollbar MUST NOT remain persistently visible

#### Scenario: Scrollbar appears with chrome
- **WHEN** the user taps to show reading chrome
- **THEN** the vertical scrollbar MUST appear together with the chapter navigation chrome
