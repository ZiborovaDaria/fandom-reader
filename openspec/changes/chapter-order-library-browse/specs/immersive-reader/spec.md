## ADDED Requirements

### Requirement: Chapter spine order is ascending narrative order
The system SHALL present chapters in ascending narrative reading order (OPF spine order when available, otherwise a stable natural chapter order), and MUST NOT rely on lexicographic filename sort alone when that would reorder chapters (for example 1, 11, 2).

#### Scenario: TOC ascending for numbered chapter files
- **WHEN** the user opens the TOC for a multi-chapter EPUB whose files would lexicographically sort as 1, 11, 2
- **THEN** the TOC MUST list chapters in ascending chapter order (1, 2, …, 11) consistent with reading progression

#### Scenario: Prev/next follows TOC order
- **WHEN** the user navigates with previous/next from chapter N
- **THEN** the reader MUST move to the previous/next chapter in the same ascending spine order as the TOC

### Requirement: Preserve paragraphs in Ficbook chapter HTML
The system SHALL extract and display Ficbook (and similar) chapter bodies with paragraph boundaries preserved, and MUST NOT collapse the chapter into a single unbroken whitespace-normalized string when the source markup contains separable paragraph or block structure.

#### Scenario: Ficbook chapter shows multiple paragraphs
- **WHEN** the user opens a Ficbook EPUB chapter whose source markup contains multiple paragraph or block elements
- **THEN** the reader MUST render those as separate paragraphs (blank-line or equivalent separation)
