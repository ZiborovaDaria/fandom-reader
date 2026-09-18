## ADDED Requirements

### Requirement: Temporary Cursor neural provider
The system SHALL support a temporary Cursor-backed neural translation provider that translates batches via a chat-style prompt instructing EN→RU translation (same pipeline contract as other translation providers). Cursor SHALL be selectable when its API key is configured. The Fake provider MUST remain the only provider used in automated unit tests. Cursor support is temporary and MAY be removed in a later change when a durable alternative exists.

#### Scenario: Cursor key enables live Cursor translation
- **WHEN** a non-blank Cursor API key is configured, Cursor is the selected live provider, and the user requests translation of an English AO3 work
- **THEN** the system MUST send chapter/title/summary segments through the Cursor-backed provider and MUST store results through the existing translation cache path

#### Scenario: Tests still use Fake only
- **WHEN** automated unit tests exercise the translation pipeline
- **THEN** they MUST use the Fake provider and MUST NOT require a live Cursor or Gemini network call

## MODIFIED Requirements

### Requirement: Provider abstraction with Gemini as primary
The system SHALL route translation through a provider interface and SHALL support Gemini as the primary configured provider, a temporary Cursor provider when selected and keyed, and a deterministic fake provider for automated tests. Placeholder UI slots for additional providers MUST NOT silently produce Fake `[RU]` output as if it were neural translation.

#### Scenario: Gemini used when configured
- **WHEN** a valid Gemini API key is configured and translation is requested and no other live provider is explicitly selected
- **THEN** the system MUST use the Gemini provider for that request

#### Scenario: Automated tests use fake provider
- **WHEN** translation pipeline unit tests run in CI
- **THEN** they MUST use a fake provider and MUST NOT require a live network API key

#### Scenario: Cursor used when selected
- **WHEN** the user has selected Cursor as the live provider and a Cursor API key is configured
- **THEN** translation requests MUST use the Cursor provider instead of Gemini

### Requirement: Select translation provider from configured key at request time
The system SHALL choose the live neural provider based on the selected provider and its non-blank API key at the time a translation request starts, and SHALL NOT permanently bind the Fake provider for the whole process lifetime solely because a key was empty at app startup.

#### Scenario: Key added after cold start
- **WHEN** the app started with an empty API key for the selected provider and the user later saves a valid key in Settings, then requests translation
- **THEN** the system MUST use the live provider for that request without requiring an app reinstall

#### Scenario: Key removed after it was configured
- **WHEN** an API key for the selected provider was previously configured and the user clears it in Settings, then requests neural translation
- **THEN** the system MUST NOT call that live provider with the cleared key

#### Scenario: Missing key for selected provider
- **WHEN** the user requests translation and the selected provider has no configured API key
- **THEN** the system MUST fail with a user-visible need-key path and MUST NOT present Fake placeholders as successful neural output
