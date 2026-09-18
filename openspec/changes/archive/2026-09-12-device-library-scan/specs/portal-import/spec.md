## ADDED Requirements

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
