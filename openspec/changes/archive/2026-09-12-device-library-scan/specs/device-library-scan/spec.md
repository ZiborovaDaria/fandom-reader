## Purpose

Поиск EPUB и FB2 в типичных папках устройства и пакетное добавление найденных файлов в локальную библиотеку с понятным прогрессом и обработкой разрешений.

## ADDED Requirements

### Requirement: Scan typical folders for EPUB and FB2
The system SHALL discover `.epub` and `.fb2` files under the device's typical public book locations: Downloads, Documents, and Books (when present and readable).

#### Scenario: Discover books in Downloads
- **WHEN** the user runs a library scan and Downloads contains one or more `.epub` or `.fb2` files
- **THEN** the system MUST include those files in the scan results for import

#### Scenario: Ignore non-book files
- **WHEN** a typical folder contains files that are neither `.epub` nor `.fb2`
- **THEN** the system MUST NOT treat those files as scan import candidates

#### Scenario: Missing typical folder is not an error
- **WHEN** one of the typical folders (for example Books) does not exist on the device
- **THEN** the scan MUST continue with the remaining folders and MUST NOT fail solely because that folder is absent

### Requirement: Batch-import discovered files into the library
The system SHALL import each discovered EPUB/FB2 into the library using the same classification and metadata extraction path as a single-file import, then present imported works on the appropriate shelf.

#### Scenario: Successful batch import
- **WHEN** a scan finds readable EPUB/FB2 files and import permissions/access are granted
- **THEN** the system MUST add each successfully parsed file to the library and MUST show a completion summary (imported count and skipped/failed count when any)

#### Scenario: Partial failure does not abort the whole scan
- **WHEN** one discovered file fails to parse or copy during a batch import
- **THEN** the system MUST continue importing the remaining files and MUST report that file as failed without discarding already imported works

### Requirement: Deduplicate repeated scans
The system SHALL avoid creating unrelated duplicate library entries when the same work identity or the same already-imported local file is discovered again on a later scan.

#### Scenario: Rescan same files
- **WHEN** the user runs a scan twice over the same set of files already present in the library
- **THEN** the system MUST NOT create additional unrelated duplicate entries for those same identities/files

### Requirement: Request storage access when needed
The system SHALL request the storage/media read permission or folder access required to read typical folders before scanning, and SHALL explain when a scan cannot run without access.

#### Scenario: Permission missing
- **WHEN** the user starts a scan without the required read access to typical folders
- **THEN** the system MUST prompt for access (or guide the user to grant it) and MUST NOT silently report an empty library as a successful scan of zero books when access was denied

#### Scenario: Permission granted then scan
- **WHEN** the user grants the required access after a prompt
- **THEN** the system MUST proceed with scanning typical folders
