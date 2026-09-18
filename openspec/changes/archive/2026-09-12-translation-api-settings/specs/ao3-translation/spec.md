## ADDED Requirements

### Requirement: Select translation provider from configured key at request time
The system SHALL choose the live Gemini provider when a non-blank Gemini API key is configured at the time a translation request starts, and SHALL NOT permanently bind the Fake provider for the whole process lifetime solely because the key was empty at app startup.

#### Scenario: Key added after cold start
- **WHEN** the app started with an empty Gemini API key and the user later saves a valid key in Settings, then requests translation
- **THEN** the system MUST use the Gemini provider for that request without requiring an app reinstall

#### Scenario: Key removed after it was configured
- **WHEN** a Gemini API key was previously configured and the user clears it in Settings, then requests neural translation
- **THEN** the system MUST NOT call Gemini with the cleared key

### Requirement: Metadata translation uses neural provider when configured
When a Gemini API key is configured and the user requests translation of an English AO3 work, the system SHALL translate title and summary via the neural provider and SHALL store Russian fields for library display.

#### Scenario: Translate title and summary with configured key
- **WHEN** a valid Gemini API key is configured and the user starts translation for an imported English AO3 work that has no Russian summary yet
- **THEN** the system MUST produce a Russian summary (and title when missing) via the neural provider path and MUST update translation status away from NONE

### Requirement: Chapter translation available after key configuration
The system SHALL allow translating chapter narrative text through the translation pipeline when a Gemini API key is configured, caching results so the reader can show Russian chapter text offline after success.

#### Scenario: Translate a chapter with configured key
- **WHEN** a valid Gemini API key is configured and chapter translation is requested for an AO3 work chapter that is not yet cached
- **THEN** the system MUST produce Russian chapter text via the neural provider, cache it, and make it available for reading

#### Scenario: Resume skips cached chapter
- **WHEN** chapter N was previously translated and cached
- **THEN** a subsequent request for chapter N MUST return the cached text without calling the neural provider again
