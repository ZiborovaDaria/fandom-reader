## ADDED Requirements

### Requirement: Resolve SAF import filename and format
When the user imports a file via the system document picker (SAF), the system SHALL determine a usable display filename and book format using the document display name and/or MIME type and/or file content signatures, and MUST NOT rely solely on the opaque URI path segment (for example `msf:…` without an extension).

#### Scenario: Display name carries extension
- **WHEN** the SAF document provides a display name ending in `.epub` or `.fb2`
- **THEN** the system MUST use that name (or an equivalent normalized temp name with the same extension) for classification and format detection

#### Scenario: Opaque URI without extension
- **WHEN** the SAF URI path segment has no `.epub`/`.fb2` extension but the document is an EPUB or FB2 by MIME type or content signature
- **THEN** the system MUST still treat the import as that book format for classification, metadata extraction, shelf placement rules, and later reading

### Requirement: Classify by content when extension is missing
The system SHALL run AO3 / Ficbook / Other classification and metadata extraction on the imported bytes using content-aware opening (for example ZIP/EPUB structure or FB2 XML), not only filename extension checks, whenever the file is a supported book container.

#### Scenario: AO3 EPUB imported without filename extension
- **WHEN** an AO3 EPUB is imported through SAF without a recoverable `.epub` filename but ZIP/EPUB content is readable
- **THEN** the system MUST classify it as AO3 when AO3 markers are present and MUST extract fandom/pairing/summary using the same rules as an extension-named AO3 EPUB

#### Scenario: Non-portal book still imports as Other
- **WHEN** an EPUB or FB2 without AO3 or Ficbook structure is imported with correct format detection
- **THEN** the system MUST classify it as Other and MUST place it on the Other shelf when no recognized fandom/pairing metadata is available
