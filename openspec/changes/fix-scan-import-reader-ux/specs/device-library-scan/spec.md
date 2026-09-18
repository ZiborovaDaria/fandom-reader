## ADDED Requirements

### Requirement: Discover books under scoped storage
On Android versions where direct filesystem listing of public folders may return no files despite readable directory metadata, the system SHALL still discover `.epub` and `.fb2` files that exist in the typical public book locations (Downloads/Download, Documents, Books) using a media-index or equivalent content-provider query that can see those files, and SHALL import them through the same batch path as filesystem discoveries.

#### Scenario: Media-visible books in Download are found
- **WHEN** the user runs a library scan on a device where public Download contains `.epub` or `.fb2` files that a direct folder file listing does not return
- **THEN** the system MUST still include those files in the scan results for import

#### Scenario: File listing and media discovery are merged
- **WHEN** some book files are visible via folder listing and others only via the media index
- **THEN** the system MUST union both sources for the scan candidate set and MUST NOT drop either set solely because the other is empty

### Requirement: Distinguish empty folders from failed listing
The system SHALL NOT present a successful zero-book scan completion when typical folders appear readable but no discovery path (filesystem or media index) can enumerate their contents, and SHALL instead explain limited access and offer the existing SAF fallback or guidance to grant usable access.

#### Scenario: Readable metadata but no enumerable books path
- **WHEN** the user starts a scan and typical folder roots report as present/readable yet both filesystem listing and media discovery return no candidates while the device may still hold books outside the app's enumerable view
- **THEN** the system MUST NOT treat that outcome solely as “no EPUB/FB2 found in Downloads/Documents/Books” without also offering SAF import or access guidance

#### Scenario: Truly empty typical folders
- **WHEN** the user runs a scan and discovery paths successfully enumerate the typical folders and find zero `.epub`/`.fb2` files
- **THEN** the system MUST report that no compatible files were found and MUST still keep manual import available
