## ADDED Requirements

### Requirement: Beige light shell palette
The system SHALL offer a beige light shell palette as an alternative to the default warm paper (cream) palette. The beige palette MUST use a warmer beige background and coordinated surface/foreground tones while remaining a light shell (not a dark/night scheme).

#### Scenario: User selects beige palette in light mode
- **WHEN** the user chooses the beige shell palette and the app shell is in light mode
- **THEN** library and non-reader shell backgrounds MUST use the beige tone (distinct from the default cream paper) and body text MUST remain soft ink (not pure black)

#### Scenario: Beige palette does not apply in dark shell
- **WHEN** the app shell is in dark (warm night) mode regardless of palette preference
- **THEN** the shell MUST use the warm night color scheme; the beige palette MUST NOT replace the dark shell base

### Requirement: User-selectable muted accent presets
The system SHALL let the user choose one accent preset from exactly ten predefined muted (non-neon) colors for shell interactive elements. The default preset MUST match the current forest green accent. The selection MUST affect primary actions, selected states, and other shell tokens mapped to Material `primary` / `primaryContainer` (and their night companions), not reader Paper/Sepia/Night surfaces.

#### Scenario: Default accent is forest green
- **WHEN** the user has not changed accent preference
- **THEN** shell accents MUST use the forest green preset (current default look)

#### Scenario: Accent change updates buttons immediately
- **WHEN** the user selects a different accent preset in settings
- **THEN** filled primary buttons, bold selected tab labels, and other shell primary accents MUST update to the chosen preset without restarting the app

#### Scenario: Night shell uses paired night accent
- **WHEN** the shell is in dark (warm night) mode and the user has selected an accent preset
- **THEN** night primary accents MUST use the preset's paired muted night tone (lighter/desaturated companion), not the light-mode primary hex directly on dark surfaces

#### Scenario: Preset palette is fixed and muted
- **WHEN** the accent picker is shown
- **THEN** the user MUST see exactly ten preset swatches with human-readable labels; the app MUST NOT offer a free-form color picker or bright saturated defaults in this release

### Requirement: Shell palette and accent persistence
The system SHALL persist the user's shell palette (paper vs beige) and accent preset across app restarts.

#### Scenario: Preferences survive restart
- **WHEN** the user sets beige palette and a non-default accent, then force-stops and reopens the app
- **THEN** the shell MUST restore the same palette and accent preset

## MODIFIED Requirements

### Requirement: Warm paper app color scheme
The system SHALL apply a warm light Material color scheme for the application shell (library and non-reader screens). The user MAY choose between two light palettes: default warm paper (cream) or beige. Both palettes MUST use soft ink foreground and a user-selected muted accent preset rather than the default Material palette.

#### Scenario: Library uses paper surfaces
- **WHEN** the user opens the library in light mode with the default paper palette
- **THEN** primary backgrounds MUST use the warm paper tone and body text MUST use soft ink (not pure white / pure black defaults)

#### Scenario: Library uses beige surfaces
- **WHEN** the user opens the library in light mode with the beige palette selected
- **THEN** primary backgrounds MUST use the beige shell tone and body text MUST use soft ink

#### Scenario: Accent follows selected preset
- **WHEN** interactive accents (selected tabs, primary actions) are shown in the shell
- **THEN** they MUST use the currently selected muted accent preset (forest green by default), not a hard-coded color unrelated to user preference

### Requirement: Warm night companion for shell
The system SHALL provide a warm dark (Night) color scheme for the application shell that avoids pure black backgrounds and pure white text. Night shell accents MUST follow the user-selected accent preset's night companion.

#### Scenario: System dark uses warm night
- **WHEN** the device or app is in dark mode
- **THEN** the shell MUST use a warm dark surface with off-white ink and MUST NOT rely on pure `#000000` background with pure `#FFFFFF` text as the default night look

#### Scenario: Night accent respects preset
- **WHEN** the shell is in dark mode and the user has chosen a non-default accent preset
- **THEN** primary accents in the night shell MUST use that preset's night companion color
