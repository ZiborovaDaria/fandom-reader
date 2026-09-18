## Purpose

Визуальная идентичность приложения: тёплая «бумажная» палитра Material 3 для shell и спокойные режимы поверхности чтения (Paper / Sepia / Night) без Dynamic Color как основы бренда.

## ADDED Requirements

### Requirement: Warm paper app color scheme
The system SHALL apply a warm paper Material color scheme for the application shell (library and non-reader screens), using a cream paper background, soft ink foreground, and restrained forest accent rather than the default Material palette.

#### Scenario: Library uses paper surfaces
- **WHEN** the user opens the library in light mode
- **THEN** primary backgrounds MUST use the warm paper tone and body text MUST use soft ink (not pure white / pure black defaults)

#### Scenario: Accent is forest green
- **WHEN** interactive accents (selected tabs, primary actions) are shown in the shell
- **THEN** they MUST use the configured forest accent family

### Requirement: Warm night companion for shell
The system SHALL provide a warm dark (Night) color scheme for the application shell that avoids pure black backgrounds and pure white text.

#### Scenario: System dark uses warm night
- **WHEN** the device or app is in dark mode
- **THEN** the shell MUST use a warm dark surface with off-white ink and MUST NOT rely on pure `#000000` background with pure `#FFFFFF` text as the default night look

### Requirement: Reading surface themes independent of chrome clutter
The reader SHALL offer Paper, Sepia, and Night reading surface themes selectable by the user while keeping the default reading view free of persistent chrome.

#### Scenario: Switch reading theme from chrome
- **WHEN** the user opens reading appearance controls and selects Sepia or Night
- **THEN** the reading surface background and text colors MUST update to that theme without introducing a persistent FAB or promotional badge

#### Scenario: Default reading theme is Paper
- **WHEN** the user has not chosen a reading theme yet
- **THEN** the reader MUST default to the Paper reading theme
