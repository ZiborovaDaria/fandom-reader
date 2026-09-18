# portal-import Specification

## Purpose
Импорт книг с AO3 и Ficbook (и прочих файлов) с классификацией источника и извлечением фэндома, пейрингов и аннотации.

## Requirements

### Requirement: Classify AO3, Ficbook, and Other imports
The system SHALL classify each imported file or URL as AO3, Ficbook, or Other based on detectable source markers.

#### Scenario: Detect AO3 EPUB
- **WHEN** an EPUB has publisher or content markers for Archive of Our Own (for example publisher "Archive of Our Own" or an archiveofourown.org work URL)
- **THEN** the system MUST classify the import as AO3

#### Scenario: Detect Ficbook EPUB
- **WHEN** an EPUB contains ficbook.net markers (for example rights/title page references to ficbook.net)
- **THEN** the system MUST classify the import as Ficbook

#### Scenario: Fallback to Other
- **WHEN** an imported EPUB or FB2 matches neither AO3 nor Ficbook templates
- **THEN** the system MUST classify it as Other and still import basic title/author/file data when available

### Requirement: Extract AO3 metadata from preface
For AO3 EPUB imports, the system SHALL extract fandoms and relationships primarily from the structured preface fields, not only from flat OPF subjects.

#### Scenario: Parse fandom and relationships from preface
- **WHEN** an AO3 EPUB contains a preface with Fandom and Relationships fields
- **THEN** the system MUST store those fandoms and relationships on the work

#### Scenario: Distinguish romantic and platonic relationships
- **WHEN** Relationships include tags using "/" and tags using "&"
- **THEN** the system MUST retain both and MUST record relationship type romantic for "/" and platonic for "&" when the AO3 convention applies

### Requirement: Extract Ficbook metadata from title page
For Ficbook EPUB imports, the system SHALL extract fandom, pairing/characters, and related fields from the title-page label structure used by Ficbook exporters.

#### Scenario: Parse Ficbook title page labels
- **WHEN** a Ficbook EPUB title page includes labels such as «Фэндом» and «Пэйринг и персонажи»
- **THEN** the system MUST store fandom and pairing data from those labels and MUST store the summary from available description metadata

### Requirement: Preserve work identity and summary on import
The system SHALL store source identity (source type and remote work id or URL when present), title, and summary at import time.

#### Scenario: AO3 work URL preserved
- **WHEN** an AO3 EPUB preface contains an archiveofourown.org/works/{id} URL
- **THEN** the system MUST store that work identity with the imported work

#### Scenario: Re-import same identity
- **WHEN** the user imports a work with the same source type and remote identity as an existing library entry
- **THEN** the system MUST update or deduplicate that entry instead of creating an unrelated duplicate identity

### Requirement: Import damaged translated AO3 FB2 without crashing
The system SHALL attempt best-effort metadata recovery for AO3-origin FB2 files whose package metadata was damaged by external translation tools.

#### Scenario: Recover tags from FB2 body header
- **WHEN** an FB2 has destroyed title-info but body text still contains fandom/relationship markers and/or archiveofourown.org tag links
- **THEN** the system MUST classify it as AO3 when markers are found and MUST recover available fandom/pairing/summary fields without failing the whole import

### Requirement: Batch import reuses single-file import pipeline
The system SHALL classify and parse each file discovered by a device scan through the same AO3 / Ficbook / Other import path used for a single-file import, including shelf placement and metadata extraction rules.

#### Scenario: Scanned AO3 EPUB classified like manual import
- **WHEN** a scan discovers an AO3 EPUB with recognizable AO3 markers
- **THEN** the system MUST classify it as AO3 and MUST extract fandom/pairing/summary using the same rules as a manual import of that file

#### Scenario: Scanned Other file goes to Other shelf
- **WHEN** a scan discovers an EPUB or FB2 without AO3 or Ficbook structure
- **THEN** the system MUST classify it as Other and MUST place it on the Other shelf when no recognized fandom/pairing metadata is available

### Requirement: Manual SAF import remains as fallback
The system SHALL keep the single-file document picker (SAF) import available so the user can import EPUB/FB2 from locations outside the typical scanned folders.

#### Scenario: Import via document picker still works
- **WHEN** the user chooses the secondary manual import action and picks an EPUB or FB2 via the system document picker
- **THEN** the system MUST import that file through the existing single-file import path

### Requirement: Capture display tags for meta page
For portal imports, the system SHALL capture display-oriented tags needed for the work meta page in addition to fandom and pairing filter keys, without using translated display strings as filter keys.

#### Scenario: AO3 additional tag labels stored for display
- **WHEN** an AO3 EPUB preface contains structured fields beyond Fandom and Relationships (for example Characters, Additional Tags, Rating, or Archive Warning)
- **THEN** the system MUST store those values for display on the work meta page when present

#### Scenario: Filter keys remain canonical
- **WHEN** additional display tags are stored
- **THEN** fandom/pairing library filters MUST continue to use `canonicalKey` values and MUST NOT key filters off translated display strings of those additional tags
