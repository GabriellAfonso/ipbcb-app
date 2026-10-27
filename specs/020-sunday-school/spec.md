# Feature Specification: Sunday School Lessons

**Feature Branch**: `020-sunday-school`

**Created**: 2026-10-27

**Status**: Draft

**Input**: User description: "Show the Sunday school (Escola Bíblica Dominical, EBD) lessons in the Estudos tab,
readable on a phone and offline, and let Admin and Liderança write and organize them from the management panel."
(full request — current state, decisions and their reasons — in `../TODO/specify_sunday_school_app.md`; decided in
session `2041feef-c82e-463c-b6c8-917c81b7498b`, 2026-10-27; approved visual prototype: lesson on Daniel, artifact
`https://claude.ai/artifact/PkiguhjiGdNQBSxR7FbjVV`.)

## Context

The Estudos hub shows "Escola Bíblica Dominical" as a non-clickable "Em breve" card. The church has a year of
lessons (2026, "O Drama das Escrituras", 18 lessons) that today exist only as PDFs. This feature makes them a
pleasant, structured read on the phone — not "só um blocão de texto" — and lets leadership keep writing them.

### Source of truth on the backend (not redefined here)

- `backend/specs/sunday-school/spec.md` and `backend/specs/026-sunday-school/contracts/api.md` — data model,
  content document, validation, ordering rules, error codes.

### Backend facts this feature relies on

- Public index (no token, ETag): the years with at least one published lesson (newest first, each with its
  theme), the default year (the newest of them, or none), the requested year and its published lessons in order
  (`id`, `title`, `date` or none, `teacher` possibly blank, `position`). A year without published lessons answers
  `SUNDAY_SCHOOL_YEAR_NOT_FOUND`.
- Public lesson (no token, ETag): the lesson summary plus `year {year, theme}`, `content` and `books` (for each
  book named by a `book_facts` block: name, position in the Bible, chapter count, verse count — computed by the
  server). A draft or unknown id answers `SUNDAY_SCHOOL_LESSON_NOT_FOUND`.
- Content: `sections` (1..30), each with `title`, `icon` (closed list or none) and `blocks` (1..60). Block types:
  `paragraph`, `list` (`bullet` | `numbered` | `roman`), `key_verse`, `cards`, `comparison` (exactly 2, one may be
  highlighted), `steps`, `table`, `stats`, `timeline`, `book_facts`. Texts accept only `**bold**` and `*italic*`.
  A Bible reference is `{printed, reference}` — the doctrine proof-text shape.
- Icons (closed list): `heart`, `scales`, `shield`, `plant`, `hands_praying`, `crown`, `cross`, `book_open`,
  `scroll`, `flame`, `bird`, `church`, `hourglass`, `sword`, `mountains`, `lightning`.
- Standard sections (new-lesson template, applied by the app): Contexto no Drama das Escrituras (`hourglass`),
  Objetivo teológico (`crown`), Estrutura / Esboço (`book_open`), Verdades teológicas (`shield`), Versículos-chave
  (`scroll`), Exortação devocional (`hands_praying`).
- Panel (scope `sunday_school`): list lessons of a year (drafts included), create, read, partial update, delete
  (owner), reorder a year (exactly its lessons, each once), list years with counts, set a year's theme. Years are
  created by the server when a lesson is saved for them. Admin and Liderança are `owner`; Mídia has nothing.
- Invalid content answers `SUNDAY_SCHOOL_CONTENT_INVALID` with `errors: [{path, message}]`; a stale order answers
  `SUNDAY_SCHOOL_ORDER_MISMATCH`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Read a lesson (Priority: P1)

A member opens Estudos → Escola Bíblica Dominical, sees the current year's lessons and opens one. The lesson reads
like a well-printed booklet: a parchment-style header, sections with an icon, highlighted key verses, cards,
comparisons, tables and timelines — and every Bible reference opens the Bible on the cited verses.

**Why this priority**: it is the whole point for the congregation; the panel only matters once reading exists
(the 2026 lessons are loaded by the server).

**Independent Test**: with the server's 2026 lessons, open the hub, tap the EBD card, open "Panorama de Daniel"
and check every block renders and a reference opens the Bible.

**Acceptance Scenarios**:

1. **Given** published lessons in 2026 only, **When** the member opens the EBD screen, **Then** the 2026 tab is
   open, the theme "O Drama das Escrituras" is shown with the year, and the lessons appear in the server's order
   with title, date when known and teacher when known.
2. **Given** published lessons in 2026 and 2027, **When** the member opens the EBD screen, **Then** 2027 is open by
   default and 2026 is one tap away in its own tab.
3. **Given** a lesson, **When** the member opens it, **Then** each section shows its title and icon and each block
   is drawn in its own style; `**bold**` and `*italic*` are rendered and any other text is shown literally.
4. **Given** a reference chip with a resolved reference, **When** tapped, **Then** the Bible opens on that passage
   with the verses highlighted; a chip without a resolved reference is shown but does nothing.
5. **Given** a lesson containing a block type or icon this app version does not know, **When** it opens, **Then**
   the unknown block is skipped (the unknown icon is not drawn) and the rest renders normally.
6. **Given** the reader, **When** the member taps "Aa", **Then** they can make the text larger or smaller and the
   choice is kept.

---

### User Story 2 - Read offline (Priority: P1)

A member who opened the lessons once can open them again without internet, e.g. in the church hall.

**Why this priority**: lessons are read during class, where the connection is unreliable.

**Independent Test**: open the year list and a lesson online, switch to airplane mode, reopen both.

**Acceptance Scenarios**:

1. **Given** a year and a lesson opened before, **When** opened offline, **Then** the saved copy is shown without
   an error.
2. **Given** a saved copy and a newer version on the server, **When** opened online, **Then** the saved copy shows
   at once and is replaced by the new version once checked.
3. **Given** a lesson never opened, **When** opened offline, **Then** an error with "Tentar novamente" is shown.

---

### User Story 3 - Write and publish a lesson (Priority: P2)

A leader (Admin or Liderança) opens the panel area "EBD", picks a year, creates a lesson from the six standard
sections, writes paragraphs, lists and key verses with Bible references, previews it as members will see it and
publishes it (or keeps it as a draft).

**Why this priority**: lessons for the rest of the year and the next years must be written in the app.

**Independent Test**: as Liderança, create a draft lesson in 2026, check it does not appear for members, publish
it and check it appears at the end of the 2026 list.

**Acceptance Scenarios**:

1. **Given** a user with the `sunday_school` scope, **When** they open the panel, **Then** the "EBD" area is
   visible; without the scope it is not.
2. **Given** the lessons of a year in the panel, **When** listed, **Then** drafts appear with a "Rascunho" mark
   and a year selector switches years.
3. **Given** a new lesson, **When** the form opens, **Then** the content starts with the six standard sections and
   their default icons, each with one empty paragraph.
4. **Given** the editor, **When** the leader adds, edits, moves or deletes a section (title, icon from the closed
   list), a paragraph, a list (bullet, numbered or roman) or a key verse, and adds reference chips (printed text
   plus book, chapter and verses), **Then** the change is kept on save.
5. **Given** a lesson with cards, comparison, steps, table, stats, timeline or book facts blocks, **When** edited,
   **Then** those blocks are shown as they render, can be moved or deleted but not edited, and are sent back
   unchanged on save.
6. **Given** the form, **When** the leader taps "Pré-visualizar", **Then** the lesson is shown exactly as the
   member reader would show it.
7. **Given** content the server rejects (bad reference, unknown icon or type), **When** saved, **Then** each error
   is shown next to the offending field and nothing is lost.

---

### User Story 4 - Organize lessons and years (Priority: P3)

A leader reorders the lessons of a year by dragging (e.g. puts a new Gênesis lesson before the imported ones),
deletes a lesson, and sets the theme of each year in "Anos da EBD".

**Why this priority**: useful but not needed to read or write a lesson.

**Independent Test**: drag the last 2026 lesson to the top, reopen the member list and see it first; change the
2026 theme and see the new theme in the reader.

**Acceptance Scenarios**:

1. **Given** the panel list of a year, **When** the leader drags a lesson to another place, **Then** the new order
   is saved and members see it.
2. **Given** someone else changed the year's lessons meanwhile, **When** the reorder is rejected as stale,
   **Then** the list reloads with a message and nothing is half-saved.
3. **Given** a lesson, **When** the leader deletes it and confirms, **Then** it disappears from the list.
4. **Given** "Anos da EBD", **When** opened, **Then** every year with lessons appears (newest first) with its
   theme, lesson count and published count; editing the theme saves it; an empty theme removes it.

### Edge Cases

- No published lesson at all: the EBD screen shows "Nenhuma aula publicada ainda".
- A year disappears from the server (all its lessons unpublished) while its saved copy exists: the year list is
  refreshed from the server; opening that year's saved copy still works offline; online it shows the server's
  current years.
- A lesson saved offline is later unpublished: online the reader shows "Esta aula não está mais disponível" and
  drops the copy.
- The theme does not fit the tab: the tab shows only the year, and the theme appears above the lesson list.
- A year without theme: shows only the year.
- Very long lesson (8 thousand characters): scrolling stays smooth and the font size bar works throughout.
- Dark mode: dark brown background, readable contrast; the parchment texture only on header and key verses.
- A `book_facts` block whose book is missing from `books`: the card shows only the facts it has.
- Panel: leaving the form with unsaved changes asks "Descartar alterações?".
- Panel: a year with no row yet (no lessons) does not appear in the year selector; a new lesson can be created for
  any year 2000..2100 and the year then appears.

## Requirements *(mandatory)*

### Functional Requirements

**Reading**

- **FR-001**: The EBD card in the Estudos hub MUST be clickable and open the EBD screen; the other "Em breve"
  cards stay as they are.
- **FR-002**: The EBD screen MUST show one tab per year returned by the server, newest first, and open the
  server's default year; no year is hard-coded.
- **FR-003**: The year's theme MUST be shown with the year ("2026 — O Drama das Escrituras"); with no theme only
  the year is shown.
- **FR-004**: The lesson list MUST follow the server's order and show title, date (when known) and teacher (when
  not blank).
- **FR-005**: The lesson reader MUST render every block type listed in the backend contract, with section titles
  and icons, and `**bold**` / `*italic*` inline formatting; no other markup is interpreted.
- **FR-006**: An unknown block type MUST be skipped and an unknown icon not drawn, never failing the screen.
- **FR-007**: Reference chips MUST behave like the doctrine chips: open the Bible on the passage with the verses
  highlighted, or be inert when unresolved.
- **FR-008**: The `book_facts` card MUST show the book name, author, date, style and reading time when present,
  plus chapter count, verse count and book number from the server's `books`.
- **FR-009**: The reader MUST look like aged parchment without hurting reading: plain cream background behind
  text, parchment texture only on the lesson header and key-verse cards, serif headings and text, and very dark
  brown instead of black in dark mode.
- **FR-010**: Section and card icons MUST be the Phosphor duotone icons for the closed list, bundled only as those
  drawings (no icon library added).
- **FR-011**: The reader MUST offer the "Aa" text size control (14–32, steps of 2, default 18).
- **FR-012**: The year list (per year) and each lesson MUST be kept on the device and revalidated with the server
  like the doctrine documents; a failed check with a saved copy keeps the copy without error.
- **FR-013**: Loading MUST show a progress indicator; failure without a copy MUST show the error and "Tentar
  novamente".

**Panel**

- **FR-014**: The panel MUST show an "EBD" area only to users with the `sunday_school` scope at view level or
  above; create/edit/reorder/theme need manage, delete needs owner.
- **FR-015**: The panel list MUST show every lesson of the selected year (drafts marked "Rascunho") in order, with
  a year selector of the years returned by the server, defaulting to the newest.
- **FR-016**: The lesson form MUST edit title (required), year (2000..2100, default the selected year), date
  (optional), teacher (optional free text), status (Rascunho/Publicado) and content.
- **FR-017**: A new lesson's content MUST start with the six standard sections and their default icons.
- **FR-018**: The editor MUST create, edit, move and delete sections (title, icon from the closed list or none)
  and `paragraph`, `list` and `key_verse` blocks, and the reference chips of paragraphs, list items and key
  verses.
- **FR-019**: Other block types MUST be shown read-only in the editor, movable and deletable, and sent back
  unchanged.
- **FR-020**: The form MUST offer a preview that uses the same renderer as the member reader.
- **FR-021**: Server content errors MUST be shown next to the field their path points to; errors whose path
  cannot be placed are shown at the top of the form.
- **FR-022**: Lessons of a year MUST be reorderable by drag and drop, saving the whole order of the year.
- **FR-023**: A lesson MUST be deletable after confirmation, only by owner level.
- **FR-024**: "Anos da EBD" MUST list the years (theme, lesson count, published count) and let the theme be set or
  cleared; there is no action to create or delete a year.
- **FR-025**: Panel data MUST NOT be stored on disk (drafts are not public content).

### Key Entities

- **Year**: a calendar year with an optional theme; public only while it has a published lesson.
- **Lesson**: title, year, optional date, optional teacher, status (draft/published), position in its year and a
  structured content of sections and blocks.
- **Section**: titled group of blocks with an optional icon from the closed list.
- **Block**: one of the ten content types; carries texts and, for some types, Bible references.
- **Bible reference**: printed text plus an optional resolved passage (book, chapter, verse ranges).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A member reaches any 2026 lesson from the Estudos tab in at most 3 taps.
- **SC-002**: A lesson opened once opens offline in under 1 second.
- **SC-003**: All 18 lessons of 2026 render every block without a missing or broken part.
- **SC-004**: A leader writes and publishes a new lesson with the standard sections from the phone without
  touching any other tool.
- **SC-005**: Moving a lesson to another position takes one drag and is visible to members on their next refresh.
- **SC-006**: Body text keeps a contrast ratio of at least 7:1 in light and dark mode.

## Assumptions

- The backend feature `026-sunday-school` is deployed with the contract above; the 2026 lessons are loaded by it.
- The reader text size shares the doctrine preference (one "reading size" for Estudos) unless the plan finds a
  reason to split it.
- Serif fonts are the prototype's (Cormorant Garamond for titles, Literata for text), loaded the way the plan
  decides (downloadable or bundled), with a system serif fallback.
- The reorder handle uses long-press drag; there is no move-up/down fallback in this version.
- The reference editor picks book, chapter and verse ranges from the Bible's book list, so most server reference
  errors are avoided before saving.

## Out of Scope

- Images, uploads, PDF viewing or download (only the structured version — user decision).
- Markdown or HTML editing.
- Categories or several series in one year.
- Search inside lessons.
- Notifications about new lessons.
- Creating or editing `cards`, `comparison`, `steps`, `table`, `stats`, `timeline`, `book_facts` in the panel.
