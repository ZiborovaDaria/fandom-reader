## ADDED Requirements

### Requirement: Preserve paragraph breaks in cached chapter translations
When translating chapter body text, the system SHALL preserve paragraph boundaries from the source chapter text in the cached Russian chapter output (for example blank-line separators between paragraphs) so the reader can display translated text with the same paragraph structure.

#### Scenario: Translated chapter keeps paragraph separation
- **WHEN** a chapter whose source text contains multiple paragraphs is translated and cached
- **THEN** the cached Russian chapter text MUST retain separable paragraph boundaries corresponding to the source paragraphs

#### Scenario: Fake provider preserves structure in tests
- **WHEN** automated translation tests run with the fake provider on multi-paragraph input
- **THEN** the fake provider path MUST preserve paragraph boundaries so CI can assert structure without a live Gemini call
