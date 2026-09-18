## MODIFIED Requirements

### Requirement: Library list shows title and summary
The system SHALL display each work in library lists with its title and summary (annotation) text suitable for scanning. When the summary is longer than the collapsed preview budget, the system SHALL show a control that expands the full summary inline in the list without opening the work.

#### Scenario: List row content
- **WHEN** the user views a library list of works
- **THEN** each row MUST show the work title and its summary text (collapsed preview if long)

#### Scenario: Collapsed preview is twice the previous budget
- **WHEN** a work has a summary longer than the collapsed preview budget
- **THEN** the row MUST show a collapsed preview of up to 440 characters (twice the previous 220-character budget) and MUST NOT use the old 220-character hard cut as the default preview

#### Scenario: Expand full summary inline
- **WHEN** the collapsed preview truncates the summary and the user activates the expand control on that row
- **THEN** the system MUST show the full summary text on that same list row and MUST NOT navigate away from the list or open the work

#### Scenario: Collapse expanded summary
- **WHEN** a row summary is expanded and the user activates the collapse control
- **THEN** the system MUST return that row to the collapsed preview state while remaining on the list

#### Scenario: Expand control does not open the work
- **WHEN** the user activates the expand or collapse control on a work row
- **THEN** the system MUST NOT open the work (no navigation to reader or work meta)

#### Scenario: Short summary needs no expand control
- **WHEN** a work summary fits entirely within the collapsed preview budget
- **THEN** the row MUST show the full summary and MUST NOT show an expand control

#### Scenario: Missing summary
- **WHEN** a work has no summary
- **THEN** the row MUST still show the title and MUST show a clear empty/placeholder summary state
