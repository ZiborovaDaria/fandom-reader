## ADDED Requirements

### Requirement: System bars contrast with shell theme
The system SHALL set system status bar (and navigation bar when applicable) icon/content appearance so that time, battery, and related indicators remain legible against the active shell background: dark icons on light/paper shell surfaces and light icons on night/dark shell surfaces.

#### Scenario: Light shell shows dark status icons
- **WHEN** the user uses the light (paper) shell theme
- **THEN** status bar icons MUST be dark enough to remain readable on the cream/paper background

#### Scenario: Night shell shows light status icons
- **WHEN** the user uses the warm night shell theme
- **THEN** status bar icons MUST be light enough to remain readable on the dark shell background

### Requirement: Reading surface updates status bar contrast
When the reader surface theme is Paper or Sepia, the system SHALL use dark status bar icons; when the reader surface is Night, the system SHALL use light status bar icons, so indicators stay legible over the reading background under edge-to-edge layout.

#### Scenario: Paper reading keeps dark status icons
- **WHEN** the user reads with the Paper reading surface under edge-to-edge display
- **THEN** status bar icons MUST remain readable against the paper reading background
