## ADDED Requirements

### Requirement: Library top actions remain reachable on narrow screens
On the library screen, the system SHALL keep Catalog, Scan, Import, and Settings actions reachable on narrow device widths so that no required action is permanently clipped off-screen without an alternative entry (for example an overflow menu, icon actions, or a horizontally scrollable action row).

#### Scenario: Settings reachable when width is tight
- **WHEN** the user opens the library on a phone-width viewport where four full text labels would not fit in the top app bar
- **THEN** the Settings action MUST still be reachable in one or two taps without leaving the library navigation context

#### Scenario: Scan and Import remain available
- **WHEN** the library top bar uses a compact or overflow presentation
- **THEN** the primary Scan action and secondary Import action MUST remain available alongside Catalog and Settings
