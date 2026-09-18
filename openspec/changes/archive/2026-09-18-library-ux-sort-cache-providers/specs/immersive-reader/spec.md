## ADDED Requirements

### Requirement: Skip full re-parse for recently opened works
When the user reopens a work whose chapter text was already successfully loaded in the current app process (or a short-lived on-device chapter cache still valid for that file), the system SHALL present chapter text without performing a full EPUB/FB2 re-parse of the file. A first open, a changed source file, or an invalidated cache MUST still parse from the local file. Offline reading after import MUST continue to work without a network.

#### Scenario: Reopen same work uses cached chapters
- **WHEN** the user opens a work, leaves the reader, and soon reopens the same work without the source file changing
- **THEN** the reader MUST show the same chapter body content without requiring a full file re-parse on that reopen

#### Scenario: Changed file invalidates cache
- **WHEN** the local source file for a work changes after chapters were cached
- **THEN** the next open MUST re-parse from the file and MUST NOT show stale chapter text from the previous file version

#### Scenario: Progress still restores on cached reopen
- **WHEN** a work is reopened from valid chapter cache
- **THEN** the system MUST still restore persisted chapter index and intra-chapter offset as today

### Requirement: Ficbook chapter bodies load as story text
For Ficbook-classified EPUB works, the reader SHALL extract story chapter bodies so that opened chapters show readable narrative text (with paragraph structure), and MUST NOT present an empty body or a body consisting only of scene-break placeholders such as `***` when the EPUB contains real chapter HTML.

#### Scenario: Ficbook chapter with div/br markup shows paragraphs
- **WHEN** the user opens a Ficbook EPUB whose chapter HTML uses `div`/`br` (or equivalent) markup with narrative text
- **THEN** the reader MUST display that narrative as one or more paragraphs and MUST NOT show an empty chapter

#### Scenario: Scene breaks alone are not the whole chapter
- **WHEN** a Ficbook chapter HTML contains narrative paragraphs plus `***` scene-break lines
- **THEN** the reader MUST keep the narrative paragraphs visible and MUST NOT reduce the chapter to only `***` lines

#### Scenario: Front-matter still excluded
- **WHEN** a Ficbook EPUB includes a title/meta page
- **THEN** that page MUST remain excluded from the story chapter spine as required by existing immersive-reader front-matter rules
