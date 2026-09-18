# immersive-reader Specification

## Purpose
Офлайн-читалка EPUB/FB2 с удобным чтением и минимумом отвлекающих элементов интерфейса.

## Requirements

### Requirement: Open imported works offline
The system SHALL open imported EPUB and FB2 works for reading without requiring a network connection after the file is stored locally.

#### Scenario: Open EPUB offline
- **WHEN** the user opens an imported EPUB while offline
- **THEN** the system MUST display the work content from local storage

#### Scenario: Open FB2 offline
- **WHEN** the user opens an imported FB2 while offline
- **THEN** the system MUST display the work content from local storage

### Requirement: Immersive reading chrome
While reading, the system SHALL keep the default view focused on text and SHALL reveal reading controls only on explicit user interaction. Essential chrome MUST include progress, chapter navigation as clear previous/next arrow controls, access to the table of contents, access to appearance controls (theme / font / size), a back control that returns to the library/menu, and a vertical chapter scrollbar affordance. Chrome surfaces MUST remain opaque enough that chapter body text does not show through control labels. The system MUST still allow returning to the immersive text-only view. The default view MUST remain free of persistent toolbars, FABs, badges, or promotional UI.

#### Scenario: Default reading view hides controls
- **WHEN** the user is actively reading a chapter
- **THEN** the default view MUST show the text without persistent toolbars, FABs, badges, or promotional UI

#### Scenario: Reveal controls on tap
- **WHEN** the user taps the reading area to toggle chrome
- **THEN** the system MUST temporarily show essential controls (progress, previous/next chapter arrows, TOC entry, appearance entry, back-to-library, and the vertical scrollbar) and MUST allow returning to the immersive text-only view

#### Scenario: Chapter arrows remain reachable
- **WHEN** reading chrome is visible on a narrow phone width
- **THEN** both previous and next chapter arrow controls MUST remain visible and tappable (not clipped off-screen)

#### Scenario: Back leaves the reader to library
- **WHEN** the user activates the back control from reading chrome
- **THEN** the app MUST leave the reader and return to the library/menu navigation context

### Requirement: Persist reading progress
The system SHALL persist reading progress per work and restore it when the work is reopened.

#### Scenario: Resume at last position
- **WHEN** the user reopens a work after reading part of it
- **THEN** the reader MUST restore the last saved reading position

### Requirement: Prefer translated text when available
For AO3 works with completed chapter translation, the reader SHALL display the Russian translated chapter text by default.

#### Scenario: Show Russian chapter when translation exists
- **WHEN** the user opens a chapter that has a completed Russian translation
- **THEN** the reader MUST show the translated text by default

#### Scenario: Fall back to original when translation missing
- **WHEN** the user opens a chapter without a completed translation
- **THEN** the reader MUST show the original chapter text

### Requirement: Preserve paragraph boundaries in chapter text
The system SHALL extract and display chapter body text with paragraph boundaries preserved for EPUB and FB2 sources (blank-line or equivalent separation between paragraphs), and MUST NOT collapse the entire chapter into a single unbroken whitespace-normalized string.

#### Scenario: EPUB paragraphs remain separated
- **WHEN** the user opens an EPUB chapter whose source markup contains multiple paragraph elements
- **THEN** the reader MUST show those paragraphs as visually separated blocks (not one continuous run-on paragraph)

#### Scenario: FB2 paragraphs remain separated
- **WHEN** the user opens an FB2 section whose source contains multiple `<p>` elements
- **THEN** the reader MUST show those paragraphs as visually separated blocks

### Requirement: Table of contents jump to chapter
The system SHALL provide a table of contents listing available chapters for the open work and SHALL navigate to the chapter the user selects.

#### Scenario: Open TOC from reading chrome
- **WHEN** the user reveals reading chrome and opens the table of contents
- **THEN** the system MUST list chapters for the current work (using chapter titles when available, otherwise a stable chapter label)

#### Scenario: Jump to selected chapter
- **WHEN** the user selects a chapter entry in the table of contents
- **THEN** the reader MUST display that chapter and MUST close or dismiss the TOC so reading can continue

#### Scenario: Current chapter is identifiable
- **WHEN** the TOC is open
- **THEN** the currently displayed chapter MUST be visually distinguishable in the list

### Requirement: Adjustable reading typography
The system SHALL let the user change reading font family and font size, and SHALL apply line height appropriate for comfortable long-form reading. Chosen typography settings SHALL persist across app restarts.

#### Scenario: Change font family
- **WHEN** the user selects a different reading font family from appearance controls
- **THEN** chapter body text MUST render with that family immediately

#### Scenario: Change font size
- **WHEN** the user increases or decreases reading font size
- **THEN** chapter body text MUST resize within the supported range and MUST remain readable

#### Scenario: Typography persists
- **WHEN** the user changes font family or size and later reopens any work
- **THEN** the reader MUST restore the last saved typography settings

### Requirement: Reading progress includes chapter offset
The system SHALL persist both chapter index and an intra-chapter offset for each work, SHALL flush that progress when the user leaves the reader or the app stops the reading screen, and SHALL restore both when the work is reopened.

#### Scenario: Resume mid-chapter
- **WHEN** the user leaves a work after scrolling within a chapter
- **THEN** reopening that work MUST restore the same chapter and approximately the same scroll offset

#### Scenario: Progress flushed on leave
- **WHEN** the user navigates away from the reader (back to library) after changing chapter or scroll position
- **THEN** the system MUST persist the latest chapter index and offset before or as the screen is left so a later reopen restores that position

### Requirement: Chapter text clears the status bar
The system SHALL inset the reading text area below the system status bar (and other relevant top system insets) so the first line of chapter text does not draw under or overlap the status bar region.

#### Scenario: First line below status bar
- **WHEN** the user opens a chapter in the reader under edge-to-edge display
- **THEN** the first visible line of chapter text MUST sit below the status bar and MUST remain fully readable

### Requirement: Scrollbar only with reading chrome
The system SHALL show a vertical scrollbar (or equivalent scroll-position affordance along the chapter edge) only while reading chrome is visible after the user toggles chrome, and MUST hide it again when chrome is dismissed.

#### Scenario: Scrollbar hidden in immersive mode
- **WHEN** reading chrome is hidden
- **THEN** the vertical scrollbar MUST NOT remain persistently visible

#### Scenario: Scrollbar appears with chrome
- **WHEN** the user taps to show reading chrome
- **THEN** the vertical scrollbar MUST appear together with the chapter navigation chrome

### Requirement: Progress affordance in chrome
When reading chrome is visible, the system SHALL show chapter position progress (at least current chapter index over total, and a progress indication for position in the work or chapter).

#### Scenario: Chrome shows chapter fraction
- **WHEN** reading chrome is visible for a multi-chapter work
- **THEN** the UI MUST show which chapter is active relative to the total chapter count

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
