# ao3-translation Specification

## Purpose
Structured EN→RU translation for AO3 works that preserves filterable canonical tags while producing readable Russian title, summary, and chapter text.

## Requirements

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

### Requirement: Temporary Cursor neural provider
The system SHALL support a temporary Cursor-backed neural translation provider that translates batches via a chat-style prompt instructing EN→RU translation (same pipeline contract as other translation providers). Cursor SHALL be selectable when its API key is configured. The Fake provider MUST remain the only provider used in automated unit tests. Cursor support is temporary and MAY be removed in a later change when a durable alternative exists.

#### Scenario: Cursor key enables live Cursor translation
- **WHEN** a non-blank Cursor API key is configured, Cursor is the selected live provider, and the user requests translation of an English AO3 work
- **THEN** the system MUST send chapter/title/summary segments through the Cursor-backed provider and MUST store results through the existing translation cache path

#### Scenario: Tests still use Fake only
- **WHEN** automated unit tests exercise the translation pipeline
- **THEN** they MUST use the Fake provider and MUST NOT require a live Cursor or Gemini network call

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

### Requirement: Preserve paragraph breaks in cached chapter translations
When translating chapter body text, the system SHALL preserve paragraph boundaries from the source chapter text in the cached Russian chapter output (for example blank-line separators between paragraphs) so the reader can display translated text with the same paragraph structure.

#### Scenario: Translated chapter keeps paragraph separation
- **WHEN** a chapter whose source text contains multiple paragraphs is translated and cached
- **THEN** the cached Russian chapter text MUST retain separable paragraph boundaries corresponding to the source paragraphs

#### Scenario: Fake provider preserves structure in tests
- **WHEN** automated translation tests run with the fake provider on multi-paragraph input
- **THEN** the fake provider path MUST preserve paragraph boundaries so CI can assert structure without a live Gemini call

### Requirement: Translation preserves note versus body segments
When translating chapter text that contains recognized author-note or afterword segments, the system SHALL preserve segment boundaries between note regions and primary story body so that after translation the reader can still apply distinct note styling. Legacy caches that lack segment markers MAY fall back to treating the whole chapter as body text.

#### Scenario: Translated notes stay distinguishable
- **WHEN** a chapter with note segments is translated EN→RU and later opened in the reader
- **THEN** the translated note content MUST still be identifiable as notes (not merged indistinguishably into body) so blockquote styling can apply

#### Scenario: Legacy unsegmented cache still readable
- **WHEN** a previously cached chapter translation has no note-segment markers
- **THEN** the system MUST still show the translated text as readable chapter content (body styling is acceptable)
