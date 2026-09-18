## ADDED Requirements

### Requirement: Never render book containers as raw binary text
The system SHALL open stored EPUB and FB2 works through book parsers and MUST NOT display the raw binary container bytes (for example ZIP local-file headers such as `PK` and `mimetypeapplication/epub+zip`) as the chapter reading text.

#### Scenario: Mis-tagged EPUB opens as parsed chapters
- **WHEN** the user opens a locally stored EPUB whose recorded format string is missing or not `epub` but the file content is an EPUB ZIP container
- **THEN** the reader MUST detect the EPUB container and MUST show extracted chapter text instead of raw binary/UTF-8 dump

#### Scenario: Mis-tagged FB2 opens as parsed chapters
- **WHEN** the user opens a locally stored FB2 whose recorded format string is missing or not `fb2` but the file content is FB2 XML
- **THEN** the reader MUST detect FB2 and MUST show extracted chapter text instead of an unparsed binary-looking dump

#### Scenario: Unsupported binary is not shown as chapter body
- **WHEN** the user opens a local file that is neither a parsable EPUB nor FB2
- **THEN** the reader MUST NOT present the raw file bytes as readable chapter text and MUST show a clear failure or unsupported-format state instead
