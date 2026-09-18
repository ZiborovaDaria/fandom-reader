## ADDED Requirements

### Requirement: Exclude front-matter from chapter spine
The system SHALL exclude front-matter documents (AO3 preface/summary pages, Ficbook title pages, and equivalent metadata-only sections) from the reader chapter list so that chapter bodies contain story content only.

#### Scenario: AO3 EPUB chapter 1 is story
- **WHEN** the user opens an AO3 EPUB that includes a preface with description and tags plus subsequent chapter documents
- **THEN** the first chapter in the reader MUST be the first story chapter and MUST NOT contain the preface description/tag block as chapter body

#### Scenario: Ficbook title page excluded
- **WHEN** the user opens a Ficbook EPUB that includes a title page with labels and description
- **THEN** that title page MUST NOT appear as a numbered story chapter in the reader

#### Scenario: TOC lists only story chapters
- **WHEN** the user opens the table of contents for a multi-chapter portal EPUB
- **THEN** the TOC MUST list only story chapters and MUST NOT list excluded front-matter as chapter entries

### Requirement: Chapter numbering reflects story chapters only
The system SHALL number and label reading progress using the story chapter spine only (first story chapter is chapter 1 of N).

#### Scenario: Chrome shows story chapter fraction
- **WHEN** reading chrome is visible after opening a portal EPUB with excluded front-matter
- **THEN** the UI MUST show chapter position relative to the story chapter count (for example 1/N for the first story chapter)

### Requirement: TOC chapter labels are titles or numbered chapters
The system SHALL label each table-of-contents entry with a meaningful chapter title when one is available, and otherwise MUST use a stable numbered label of the form «Глава N» (1-based within the story spine).

#### Scenario: Prefer real chapter title
- **WHEN** a story chapter has a non-empty title that is useful as a chapter name
- **THEN** the TOC entry for that chapter MUST show that title

#### Scenario: Fallback to numbered chapter label
- **WHEN** a story chapter has no useful title (missing, blank, or discarded as non-chapter metadata such as the work title or Preface)
- **THEN** the TOC entry MUST show «Глава N» where N is the 1-based index in the story chapter list

#### Scenario: No junk titles in TOC
- **WHEN** the extracted document title equals the work title or is a known front-matter label
- **THEN** the TOC MUST NOT use that string as the chapter label and MUST fall back to «Глава N»
