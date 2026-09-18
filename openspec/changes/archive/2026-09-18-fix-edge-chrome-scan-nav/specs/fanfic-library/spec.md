## ADDED Requirements

### Requirement: Title search can be hidden
On the library screen, the system SHALL allow the user to collapse or hide the title-search field so it does not permanently occupy vertical space, and SHALL let the user reveal it again. Any entered title query MUST be retained while the field is collapsed unless the user clears it.

#### Scenario: Collapse title search
- **WHEN** the user chooses to hide the title search
- **THEN** the title-search field MUST no longer occupy the main library list area as an always-visible input

#### Scenario: Reveal title search again
- **WHEN** the user chooses to show title search after it was hidden
- **THEN** the title-search field MUST reappear and MUST still contain the previous query if one was entered

### Requirement: Browse screens provide Back
On fandom, pairing, tag, and filtered-works browse screens in the library navigation stack, the system SHALL provide an explicit Back control that returns to the previous screen in that stack.

#### Scenario: Back from fandom list
- **WHEN** the user is on the fandom browse screen
- **THEN** the UI MUST offer Back that returns to the prior library/catalog context

#### Scenario: Back from pairing or works list
- **WHEN** the user is on a pairing list or a pairing/filtered works list
- **THEN** the UI MUST offer Back that returns to the previous browse screen without leaving the app
