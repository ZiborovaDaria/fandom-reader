## ADDED Requirements

### Requirement: Multi-provider API key slots in Settings
The Settings translation section SHALL show distinct masked key fields (slots) for Gemini, temporary Cursor, and at least one additional future-provider placeholder slot. Only Gemini and Cursor SHALL be usable for live translation in this change; other slots MAY persist a key for later without a live adapter. Clearing a slot MUST remove that provider’s stored key. Raw keys MUST NOT be written to application logs.

#### Scenario: Cursor key slot save and clear
- **WHEN** the user saves a non-empty Cursor API key and later clears it
- **THEN** the Cursor key MUST be available to the translation pipeline while saved, and MUST NOT be used after clear

#### Scenario: Placeholder provider slot without live adapter
- **WHEN** the user saves a key into a future-provider placeholder slot that has no live adapter yet
- **THEN** the system MUST persist the masked key for later use and MUST NOT route live translation through Fake placeholders as if that provider were active

#### Scenario: Provider selection for live translation
- **WHEN** both Gemini and Cursor keys are saved
- **THEN** the Settings UI MUST let the user choose which live provider is active for subsequent translation requests

## MODIFIED Requirements

### Requirement: Settings entry for translation API keys
The system SHALL provide a Settings screen reachable from the library (or equivalent primary navigation) where the user can manage API keys used for neural translation providers.

#### Scenario: Open translation settings
- **WHEN** the user opens Settings from the library
- **THEN** the system MUST show a translation section that includes the Gemini API key field, the temporary Cursor API key field, and at least one additional provider key slot

### Requirement: Save and clear Gemini API key securely
The system SHALL persist the Gemini API key in encrypted on-device storage and SHALL allow the user to clear it. The UI MUST mask the key while editing or viewing, and the system MUST NOT write the raw key to application logs. Other provider keys on the same screen SHALL follow the same masking and encrypted-storage rules.

#### Scenario: Save non-empty key
- **WHEN** the user enters a non-empty Gemini API key and saves
- **THEN** the key MUST be available to the translation pipeline on subsequent translation requests without requiring a rebuild of the app

#### Scenario: Clear key
- **WHEN** the user clears the saved Gemini API key and confirms
- **THEN** subsequent translation requests that would use Gemini MUST NOT call the live Gemini API with the previous key

#### Scenario: Key remains masked in UI
- **WHEN** a saved Gemini API key is shown on the Settings screen
- **THEN** the UI MUST not display the full key in plain text by default (masked or reveal-on-demand only)

### Requirement: Guide user when key is missing
When the user attempts a live neural translation and the selected provider has no API key configured, the system SHALL inform the user that a key is required and SHALL offer a path to Settings (or equivalent) instead of silently producing fake `[RU]` placeholder translations as if they were neural output.

#### Scenario: Translate without key prompts for settings
- **WHEN** the user requests neural translation for an AO3 work and the selected provider has no API key configured
- **THEN** the system MUST show a user-visible message that a key is required and MUST NOT present Fake-provider placeholder text as a successful neural translation
