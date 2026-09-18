## Purpose

Structured EN→RU translation for AO3 works that preserves filterable canonical tags while producing readable Russian title, summary, and chapter text.

## ADDED Requirements

### Requirement: Translate AO3 narrative fields to Russian
The system SHALL provide EN→RU translation for AO3 work title, summary, and chapter text using a configured neural translation provider.

#### Scenario: Translate summary for library list
- **WHEN** the user requests translation for an imported English AO3 work
- **THEN** the system MUST produce a Russian summary available for library display

#### Scenario: Translate chapter content for reading
- **WHEN** a chapter of an AO3 work is translated
- **THEN** the reader MUST be able to display the Russian chapter text offline after translation completes

### Requirement: Preserve canonical tags during translation
The system SHALL keep canonical fandom and pairing identifiers unchanged for filtering while optionally storing separate Russian display labels.

#### Scenario: Filters remain stable after translation
- **WHEN** an AO3 work is translated to Russian
- **THEN** fandom/pairing filter keys MUST remain the canonical originals and MUST still match the same works as before translation

#### Scenario: Do not treat machine-translated tag synonyms as new pairings
- **WHEN** display labels for the same pairing exist in English and Russian
- **THEN** the library MUST expose one filter entry for that pairing

### Requirement: Provider abstraction with Gemini as primary
The system SHALL route translation through a provider interface and SHALL support Gemini as the primary configured provider, with a deterministic fake provider for automated tests.

#### Scenario: Gemini used when configured
- **WHEN** a valid Gemini API key is configured and translation is requested
- **THEN** the system MUST use the Gemini provider for that request

#### Scenario: Automated tests use fake provider
- **WHEN** translation pipeline unit tests run in CI
- **THEN** they MUST use a fake provider and MUST NOT require a live network API key

### Requirement: Cache and resume chapter translation
The system SHALL cache translated chapters and SHALL be able to resume incomplete book translation without retranslating already cached chapters.

#### Scenario: Skip already translated chapter
- **WHEN** chapter N was previously translated and cached
- **THEN** a subsequent full-work translation job MUST skip retranslating chapter N

#### Scenario: Partial failure leaves successful chapters usable
- **WHEN** translation fails on chapter N after earlier chapters succeeded
- **THEN** successfully translated chapters MUST remain available and the job MUST report failure for the remaining work

### Requirement: Never destroy package metadata as the translation path
The system MUST NOT use an external whole-file translate-and-repack path that replaces title/author/summary metadata with translator tool placeholders as the primary AO3 translation workflow.

#### Scenario: Reject Calibre/Yandex-style metadata wipe as primary path
- **WHEN** translating an AO3 work inside the app
- **THEN** the stored work title and author MUST NOT be replaced with placeholder values such as "Unknown" / "Yandex.Translate"
