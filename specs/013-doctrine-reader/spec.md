# Feature Specification: Doctrine Reader (Westminster Standards in the Estudos tab)

**Feature Branch**: `013-doctrine-reader`

**Created**: 2026-10-20

**Status**: Draft

**Input**: User description: "Doctrine section in the Estudos (Studies) tab of the Android app: read the three
Westminster documents served by the backend doctrine API." (full request — hub, document list, catechism reader,
confession reader, Bible links, offline, out of scope — in the `/speckit-specify` invocation that created this
directory)

## Context

The Estudos tab is a placeholder ("Em Breve"). The church wants members to read its doctrinal standards in the app:
the Breve Catecismo (107 questions), the Catecismo Maior (196 questions) and the Confissão de Fé (33 chapters), as
published by the IPB.

The text is already served by the backend (backend feature `019-doctrine-documents`, contract
`contracts/doctrine-api.md`): one public list of documents and one public endpoint per document, both with content
tags so a client can ask "has this changed?" and get a cheap "no". The documents rarely change.

Each catechism question carries **proof texts**: the Bible references printed under it. (The Confession, as served,
has none — the IPB edition prints it without proof texts — so its reader shows text only.)
Each proof text has the printed form (always shown as printed) and, when the server could interpret it, a structured
reference (book, chapter, verse spans) that the app's Bible already knows how to open. A proof text without a
structured reference is shown but is not a link. The app never tries to interpret the printed form itself.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Read a catechism (Priority: P1)

A member opens Estudos, taps "Doutrina", picks "Breve Catecismo" and reads the questions in order: each card shows the
question number, the question, the answer and its Bible references. The Catecismo Maior opens in the same reader.

**Why this priority**: It is the core of the feature; the catechisms are the most used of the three documents (Sunday
school, membership classes, family worship).

**Independent Test**: With network, open Estudos › Doutrina › Breve Catecismo; confirm all 107 questions appear in
order with their answers and references. Repeat with Catecismo Maior (196).

**Acceptance Scenarios**:

1. **Given** the Estudos tab, **When** the member opens it, **Then** they see a "Doutrina" card with the subtitle
   "Catecismos e Confissão de Westminster", and "Em breve" cards (not tappable) for Escola Bíblica Dominical,
   Personagens Bíblicos and História da Igreja.
2. **Given** the Doutrina screen, **When** it loads, **Then** it lists the three documents in the order the server
   gives, each with its size ("107 perguntas e respostas", "196 perguntas e respostas", "33 capítulos").
3. **Given** a catechism is open, **When** the member scrolls, **Then** every question appears once, in numeric order,
   as a card with "Pergunta N", the question, the answer and a "Referências" row of reference chips (no row when the
   question has none).
4. **Given** an answer that has line breaks (numbered rules, e.g. Catecismo Maior 99), **When** it is shown,
   **Then** each rule starts on its own line.
5. **Given** a proof text the server did not interpret, **When** it is shown, **Then** it appears as printed with a
   look that makes clear it is not a link, and tapping it does nothing.

---

### User Story 2 - Open a reference in the Bible (Priority: P1)

While reading, the member taps a reference chip (e.g. "Rm 11.36"). The Bible reader opens on that book and chapter, in
the translation the member already uses, positioned at the first verse of the reference. Going back returns to the same
question, at the same scroll position.

**Why this priority**: The references are the reason to read the standards with an open Bible; without the link the
member has to look each one up by hand.

**Independent Test**: Open Breve Catecismo, tap "Rm 11.36" on question 1; confirm Romanos 11 opens at verse 36 in the
current translation; press back and confirm question 1 is still on screen.

**Acceptance Scenarios**:

1. **Given** a reference with verses, **When** it is tapped, **Then** the Bible opens on that book and chapter with the
   first verse of the first span visible at the top area of the screen.
2. **Given** a whole-chapter reference (e.g. "Sl 125"), **When** it is tapped, **Then** the Bible opens at the start of
   that chapter.
3. **Given** the Bible is opened from a reference, **When** the member presses back, **Then** the doctrine screen shows
   the same document at the same position it had before.
4. **Given** the Bible text is not on the device yet, **When** a reference is tapped, **Then** the Bible behaves as it
   does when opened from its own tab (it downloads or shows its own error); the doctrine screen is unaffected.
5. **Given** the member opened the Bible from a reference, **When** they later open the Bible from its own tab,
   **Then** it opens where they left it (the reference opening counts as normal reading).

---

### User Story 3 - Read the Confession (Priority: P2)

The member opens "Confissão de Fé", sees the 33 chapters (number and title), taps one and reads its paragraphs, each
numbered. Chapter 1, paragraph 2 shows the lists of the books of the Old and New
Testament and the sentence that follows them.

**Why this priority**: Same value as the catechisms but a different, less used shape; it can ship right after them.

**Independent Test**: Open Confissão de Fé; confirm 33 chapters in order with readable titles; open chapter 1 and
confirm paragraph 2 shows both book lists and the closing sentence.

**Acceptance Scenarios**:

1. **Given** the Confession is open, **When** it loads, **Then** the chapter list shows number and title for all 33
   chapters, titles in sentence case (not all capitals, as printed in the source).
2. **Given** a chapter is open, **When** the member scrolls, **Then** every paragraph appears in order with its number,
   its text and, when present, its lists (each with its heading and items) and the text after them.
3. **Given** a chapter is open, **When** the member reaches its end, **Then** they can go to the next (or previous)
   chapter without returning to the list.

---

### User Story 4 - Find a question or passage (Priority: P2)

In a long document the member jumps straight to "Pergunta 87" or searches for a word ("justificação") and opens a
result.

**Why this priority**: The Catecismo Maior has 196 questions; scrolling to a number is slow. Teachers often say "let's
read question 33".

**Independent Test**: In the Catecismo Maior, use "Ir para nº…" with 150 and confirm question 150 is shown; search for
"adoção" and confirm the results include question 74, and that tapping it shows question 74.

**Acceptance Scenarios**:

1. **Given** a catechism is open, **When** the member uses "Ir para nº…" with a valid number, **Then** that question
   is shown at the top.
2. **Given** "Ir para nº…", **When** the number is out of range or not a number, **Then** the member is told the valid
   range (e.g. "Digite um número de 1 a 107") and nothing moves.
3. **Given** a document is open, **When** the member types a search term, **Then** results list the matching questions
   (catechism) or paragraphs (confession, with chapter and paragraph number), matching the question, answer or
   paragraph text, ignoring upper/lower case and accents ("adocao" finds "adoção").
4. **Given** a search term that is only a number, **When** searching a catechism, **Then** the question with that number
   is the first result.
5. **Given** a search result, **When** it is tapped, **Then** the reader shows that question or paragraph, and the
   search closes.
6. **Given** a search with no matches, **When** results are shown, **Then** the member sees "Nenhum resultado".

---

### User Story 5 - Read offline (Priority: P2)

A member who opened a document once can read it again with no connection (on the bus, in a church without signal). When
online, the app checks for a newer version each time a document is opened and updates it without the member noticing.

**Why this priority**: Sunday school rooms and homes often have poor signal; a reader that needs network fails exactly
when it is used.

**Independent Test**: Open the three documents online; turn on airplane mode, kill and reopen the app; confirm the
Doutrina list and the three documents open and read normally, including search.

**Acceptance Scenarios**:

1. **Given** the list or a document was opened once, **When** it is opened again with no network, **Then** it shows the
   saved copy with no error.
2. **Given** a saved copy and network, **When** the content has not changed on the server, **Then** the saved copy is
   kept and nothing is downloaded again beyond the "has it changed?" check.
3. **Given** a saved copy and network, **When** the content changed on the server, **Then** the new version replaces
   the saved one and the screen shows it.
4. **Given** no saved copy and no network, **When** the list or a document is opened, **Then** the member sees a
   message that the content could not be loaded and a "Tentar novamente" action.
5. **Given** the check for a newer version fails while a saved copy is on screen, **When** that happens, **Then** the
   member keeps reading the saved copy with no error message.

---

### User Story 6 - Adjust text size (Priority: P3)

The member makes the doctrine text bigger or smaller; the choice is kept the next time they open any doctrine document.

**Why this priority**: Many readers are older members; the Bible already offers this and they will expect it here.

**Independent Test**: Increase the font size in the Breve Catecismo, close the app, open the Confissão de Fé; confirm
the larger size is in use.

**Acceptance Scenarios**:

1. **Given** a reader is open, **When** the member uses the "Aa" control, **Then** they can step the text size up or
   down within a fixed range, and the text resizes at once.
2. **Given** a size was chosen, **When** any doctrine document is opened later, including after restarting the app,
   **Then** the same size is used.
3. **Given** the size control, **When** it is at the minimum or maximum, **Then** the corresponding step is disabled.

### Edge Cases

- A document slug in the list that the app has no reader for (a new `kind` added on the server): the list does not
  show it, so the member never reaches a screen that cannot render it.
- The list changes on the server (a document added, removed or reordered): the next successful check updates the list.
- A document's saved copy is unreadable (corrupt file): treated as no saved copy — it is fetched again, or the error
  with retry is shown when offline.
- A reference points to a book/chapter the Bible on the device cannot show: the Bible handles it as it does any unknown
  position (opens at its default); the server guarantees every interpreted reference exists in every translation.
- A proof text list with many chips: chips wrap onto several lines; none is cut off.
- Very long answers (Catecismo Maior): the card grows; no text is truncated.
- The member rotates the device or the app goes to the background: the reader keeps its position.
- Search typed very fast in the Catecismo Maior: results stay responsive and reflect the latest term.

## Requirements *(mandatory)*

### Functional Requirements

**Hub and list**

- **FR-001**: The Estudos tab MUST show a "Doutrina" entry (subtitle "Catecismos e Confissão de Westminster") that
  opens the Doutrina screen, and non-interactive "Em breve" entries for Escola Bíblica Dominical ("Lições por
  trimestre"), Personagens Bíblicos ("Fichas, famílias e linha do tempo") and História da Igreja ("Da Reforma à IPB no
  Brasil").
- **FR-002**: The Doutrina screen MUST list the documents returned by the server, in server order, showing each title in
  its short form (title without the trailing "de Westminster"/leading article, e.g. "Breve Catecismo", "Catecismo
  Maior", "Confissão de Fé") and its size: "N perguntas e respostas" for a catechism, "N capítulos" for a confession.
- **FR-003**: Documents of a kind the app does not know MUST NOT be listed.

**Catechism reader**

- **FR-004**: The catechism reader MUST show every question of the document in numeric order, each with "Pergunta N",
  the question, the answer (keeping its line breaks) and a "Referências" row with one chip per proof text, in printed
  order; the row is omitted when there are no proof texts.
- **FR-005**: Both catechisms MUST use the same reader.

**Confession reader**

- **FR-006**: The confession MUST open on a chapter list showing number and title of every chapter; titles MUST be
  shown in sentence case, keeping the words "Deus", "Cristo", "Mediador", "Espírito Santo", "Sagradas Escrituras",
  "Santíssima Trindade", "Lei", "Igreja" and "Senhor" capitalized (e.g. "DA CEIA DO SENHOR" → "Da ceia do Senhor").
- **FR-007**: A chapter MUST show its paragraphs in order, each with its number and text; when a paragraph has lists,
  each list's heading and items MUST follow the text, then the text after them.
- **FR-008**: The chapter reader MUST let the member move to the previous and next chapter.

**Proof texts and the Bible**

- **FR-009**: Every proof text MUST be shown exactly as printed.
- **FR-010**: A proof text with a structured reference MUST be a link; one without MUST look different and do nothing
  when tapped. The app MUST NOT interpret the printed form.
- **FR-011**: Tapping a link MUST open the Bible reader on the reference's book and chapter, in the member's current
  translation, positioned at the first verse of the first span (chapter start when the reference has no verses).
- **FR-012**: Returning from the Bible MUST show the doctrine screen at the position it had.
- **FR-013**: Opening the Bible from a reference MUST update the member's last reading position as normal Bible reading
  does.

**Navigation inside a document**

- **FR-014**: Catechism readers MUST offer "Ir para nº…", accepting a question number in range and showing that question;
  out-of-range or non-numeric input MUST show the valid range and change nothing.
- **FR-015**: Every reader MUST offer search over the open document: question and answer text (catechism) or paragraph
  text, list items and text after (confession), case- and accent-insensitive, matching all words of the term in any
  order.
- **FR-016**: In a catechism, a purely numeric term MUST put the question with that number first in the results.
- **FR-017**: Search results MUST show enough to recognise the match (question number and question; or chapter,
  paragraph number and the start of the paragraph) and, when tapped, show that item in the reader.
- **FR-018**: A search with no matches MUST show "Nenhum resultado".

**Text size**

- **FR-019**: The readers MUST offer an "Aa" control to step the text size up and down within a fixed range, applied at
  once to all doctrine text; the steps at the ends of the range MUST be disabled.
- **FR-020**: The chosen size MUST be kept across documents and app restarts, independently of the Bible's text size.

**Offline and freshness**

- **FR-021**: The document list and each opened document MUST be kept on the device after the first successful load and
  shown from there when there is no network.
- **FR-022**: Each time the list or a document is opened with network, the app MUST check with the server whether the
  content changed, using the content tag of the saved copy, and replace the saved copy only when it changed.
- **FR-023**: A failed check MUST NOT replace or hide a saved copy, and MUST NOT show an error while a saved copy is on
  screen.
- **FR-024**: With no saved copy and no successful load, the screen MUST show an error message and a "Tentar novamente"
  action that retries.
- **FR-025**: Every screen of this feature MUST show a loading state while it has nothing to show yet.
- **FR-026**: Saved doctrine content MUST stay on the device until a newer version replaces it; signing in or out MUST NOT
  remove it (the content is public).

**Access**

- **FR-027**: The whole feature MUST work without signing in.

### Key Entities

- **Doctrine document summary**: slug, title, kind (catechism or confession), item count. Ordered as the server lists.
- **Catechism**: a document made of questions; each question has a number, question text, answer text and proof texts.
- **Confession**: a document made of chapters (number, title); each chapter has paragraphs (number, text, optional
  lists with heading and items, optional text after).
- **Proof text**: printed form; optional reference (Bible book abbreviation, chapter, list of verse spans; no spans =
  whole chapter).
- **Doctrine reading preference**: text size, shared by all doctrine documents.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the questions (107 + 196) and paragraphs (all 33 chapters) served by the backend are reachable in
  the app, with their text identical to the server's (chapter titles aside, which change only in letter case).
- **SC-002**: Every proof text with a structured reference (all of them except the ones the server leaves
  uninterpreted) opens the Bible at the right book, chapter and verse — checked on a sample of at least 30 references
  across the two catechisms.
- **SC-003**: A member reaches any catechism question by number in at most 3 taps from the document's screen.
- **SC-004**: A document opened once opens with no network in under 2 seconds on a mid-range phone.
- **SC-005**: When online and the content has not changed, reopening a document downloads no document content.
- **SC-006**: Search results appear in under 1 second for any term in the Catecismo Maior.
- **SC-007**: After choosing a text size, 100% of later doctrine reading sessions use it until changed.

## Assumptions

- The backend contract `backend/specs/019-doctrine-documents/contracts/doctrine-api.md` is the source of truth for the
  data; it is public and needs no sign-in.
- The short display titles ("Breve Catecismo", "Catecismo Maior", "Confissão de Fé") are derived in the app from the
  slug; the full title from the server is shown as the reader's heading.
- The text size range and steps match the Bible reader's for familiarity, but the preference is stored separately.
- Search runs on the device over the saved document; no server search is needed.
- PDF export, Escola Bíblica Dominical lessons, biblical characters, church history and any editing or admin action
  are not part of this delivery; the Estudos hub only shows "Em breve" placeholders for the other sections.
- No new reset option is added in Settings; doctrine content is small (well under 1 MB in total).
