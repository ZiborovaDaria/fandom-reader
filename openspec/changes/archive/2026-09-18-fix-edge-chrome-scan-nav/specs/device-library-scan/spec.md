## ADDED Requirements

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
