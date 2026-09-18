# device-library-scan Specification

## Purpose
Поиск EPUB и FB2 в типичных папках устройства и пакетное добавление найденных файлов в локальную библиотеку с понятным прогрессом и обработкой разрешений.

## Requirements

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

### Requirement: Downloads folder is a primary scan root
The system SHALL include the device public Downloads directory (`Download` / `DIRECTORY_DOWNLOADS`) as a primary discovery root for `.epub` and `.fb2` during a library scan, using filesystem listing and/or media-index discovery so books present there are candidates for import when access allows.

#### Scenario: Books in Download are scan candidates
- **WHEN** the user runs a library scan and the public Downloads/Download folder contains one or more `.epub` or `.fb2` files visible to the app's discovery paths
- **THEN** the system MUST include those files in the scan results for import

### Requirement: User can choose a custom folder to scan
The system SHALL let the user pick a folder (document tree) to scan for `.epub` and `.fb2`, SHALL request durable access when the platform provides it, and SHALL import discovered books from that folder through the same batch import path as typical-folder scans.

#### Scenario: Scan user-selected folder
- **WHEN** the user chooses a custom folder for scanning and that folder contains `.epub` or `.fb2` files the app can read
- **THEN** the system MUST discover those files and MUST import them through the batch import path

#### Scenario: Custom folder action is available from library
- **WHEN** the user is on the library screen
- **THEN** the UI MUST offer a way to start a scan of a user-selected folder in addition to the default typical-folder scan
