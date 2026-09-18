## Purpose

Lets the user configure neural translation provider API keys in-app so live EN→RU translation can run without ad-hoc device debugging of encrypted preferences.

## ADDED Requirements

### Requirement: Settings entry for translation API keys
The system SHALL provide a Settings screen reachable from the library (or equivalent primary navigation) where the user can manage API keys used for neural translation providers.

#### Scenario: Open translation settings
- **WHEN** the user opens Settings from the library
- **THEN** the system MUST show a translation section that includes at least the Gemini API key field

### Requirement: Save and clear Gemini API key securely
The system SHALL persist the Gemini API key in encrypted on-device storage and SHALL allow the user to clear it. The UI MUST mask the key while editing or viewing, and the system MUST NOT write the raw key to application logs.

#### Scenario: Save non-empty key
- **WHEN** the user enters a non-empty Gemini API key and saves
- **THEN** the key MUST be available to the translation pipeline on subsequent translation requests without requiring a rebuild of the app

#### Scenario: Clear key
- **WHEN** the user clears the saved Gemini API key and confirms
- **THEN** subsequent translation requests MUST NOT call the live Gemini API with the previous key

#### Scenario: Key remains masked in UI
- **WHEN** a saved Gemini API key is shown on the Settings screen
- **THEN** the UI MUST not display the full key in plain text by default (masked or reveal-on-demand only)

### Requirement: Guide user when key is missing
When the user attempts a live neural translation and no Gemini API key is configured, the system SHALL inform the user that a key is required and SHALL offer a path to Settings (or equivalent) instead of silently producing fake `[RU]` placeholder translations as if they were neural output.

#### Scenario: Translate without key prompts for settings
- **WHEN** the user requests neural translation for an AO3 work and no Gemini API key is configured
- **THEN** the system MUST show a user-visible message that a key is required and MUST NOT present Fake-provider placeholder text as a successful neural translation
