## ADDED Requirements

### Requirement: In-chapter notes use blockquote styling
The system SHALL keep author chapter notes and afterword content in the chapter reading flow (MUST NOT remove them from the chapter body and MUST NOT present them as a separate table-of-contents entry solely because they are notes). The system MUST render recognized note and afterword blocks with blockquote-style formatting that is visually distinct from primary story paragraphs: at least horizontal indent and a subtle background tint appropriate to the active reader surface theme (Paper / Sepia / Night).

#### Scenario: Chapter-start notes look distinct
- **WHEN** the user opens a chapter whose source contains a chapter-notes region (for example AO3 “Chapter Notes” with note text, or an equivalent RU heading such as «Примечания»)
- **THEN** those note paragraphs MUST remain visible in the chapter and MUST appear in a blockquote-styled block distinct from surrounding story text

#### Scenario: Chapter-end notes look distinct
- **WHEN** the user reaches end-of-chapter notes in the same chapter (for example AO3 “Chapter End Notes” / `#endnotes*`, or equivalent RU notes at the end of the section)
- **THEN** those notes MUST remain in the chapter flow and MUST use the same blockquote-style distinction from story text

#### Scenario: Afterword content looks distinct
- **WHEN** the user opens afterword content (for example AO3 `#afterword` / “Afterword”, or RU «Послесловие»)
- **THEN** the afterword text MUST render with blockquote-style formatting distinct from primary story paragraphs

#### Scenario: Notes are not TOC-only chapters
- **WHEN** chapter notes appear inside a story chapter document
- **THEN** the system MUST NOT invent an extra TOC entry solely for those in-chapter notes; they remain part of that chapter’s body presentation
