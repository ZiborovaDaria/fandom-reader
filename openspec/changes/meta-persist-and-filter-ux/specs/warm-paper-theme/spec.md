## ADDED Requirements

### Requirement: Filter and browse surfaces follow shell theme
Library filter builder, filter pickers, and related browse/results surfaces SHALL use the application shell Material color scheme (warm paper in light mode, warm night in dark mode) for backgrounds and surfaces. They MUST NOT use a fixed light/cream background when the shell is in dark (Night) mode.

#### Scenario: Dark theme filter background
- **WHEN** the app shell is in dark (Night) mode and the user opens the library filter builder or a filter picker
- **THEN** the screen background MUST use the warm night surface/background from the shell color scheme and MUST NOT remain a light cream paper fill

#### Scenario: Light theme filter background
- **WHEN** the app shell is in light mode and the user opens the library filter builder
- **THEN** the screen background MUST use the warm paper shell background/surface tones
