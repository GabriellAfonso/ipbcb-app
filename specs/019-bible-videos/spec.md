# Feature Specification: Bible Videos and Reading Progress

**Feature Branch**: `019-bible-videos`

**Created**: 2026-10-27

**Status**: Draft

**Input**: User description: "Add two things to the Bible reader: YouTube videos and playlists linked to chapters
and whole books, shown in a sheet from a play icon and managed by Admin and Liderança; and personal progress — a
signed-in user marks chapters (or a whole book) as read and videos as watched, saved on the account, working
offline through a local queue." (full request — current state, decisions and their reasons — in
`../TODO/specify_bible_videos_app.md`; decided in session `880f5e23-1e97-4c5f-baed-23d3ecd24b2b`, 2026-10-27.)

## Context

The Bible reader is fully offline and needs no login. The church wants to point members to good teaching videos
about what they are reading (a sermon series on Romans, an overview of Genesis), and members want to keep track of
what they have already read. This feature adds both, on top of the existing reader and index.

### Source of truth on the backend (not redefined here)

- `backend/specs/` feature "bible videos" (planned in `../TODO/specify_bible_videos_backend.md`) — video and place
  model, oEmbed lookup, conflict resolution of progress, permissions of Liderança.

### Backend facts this feature relies on

- The video list is public (no token) and returns every video with its places in one response, with ETag support.
  A video has `id`, `kind` (`video` | `playlist`), `youtube_id`, `url`, `title`, `thumbnail_url` and `places`
  (`book`, `chapter` or `null` for the whole book, `position`).
- Adding a link takes the URL and at least one place; the server reads title and thumbnail from YouTube. A link
  already registered answers `409` `BIBLE_VIDEO_EXISTS` with the existing video in the body. Invalid URL answers
  `BIBLE_VIDEO_INVALID_URL`; private/removed video `BIBLE_VIDEO_UNAVAILABLE`; YouTube lookup failure
  `BIBLE_VIDEO_LOOKUP_FAILED`.
- Places are replaced as a whole list; a single place can be removed; a video is deleted from everywhere; the order
  of one place is set as the full list of its video ids. A video with no places is deleted by the server.
- New scope `bible_videos`: Admin owner, Liderança owner, Mídia nothing.
- Progress needs a signed-in account (no scope). The read endpoint returns only what is done
  (`chapters` by book, `videos` ids). The sync endpoint takes a batch of changes, each a state with the time of the
  action (with UTC offset); the server keeps the latest by that time, and answers which items were accepted and
  which rejected.
- The backend was not implemented when this spec was written; the contract is the planned one (see Assumptions).

### Relation to other specs

- `specs/bible/spec.md` — the reader, index and passage entry; gains the play icon, the videos sheet, the "Locais
  do vídeo" screen, the progress marks and the end-of-chapter button.
- `specs/hymnal/spec.md` §3.2–3.5 — the offline queue pattern reused by the progress queue.
- `specs/006-role-scoped-permissions/` — scopes and levels; gains `bible_videos`.

## Clarifications

### Session 2026-10-27 (decisions settled with the user before the spec)

- Q: Where do videos appear? → A: a play icon in the reader toolbar opens a sheet with the current chapter's and the
  current book's videos.
- Q: Who manages? → A: only Admin and Liderança. Mídia does not.
- Q: Can a video be in many places? → A: yes, any chapters of any books, plus whole books. A range is just several
  chapters.
- Q: Order? → A: manual, per place, by dragging. New links go to the end.
- Q: Title editing? → A: no, always YouTube's title. No "all videos" admin screen.
- Q: Same video added twice? → A: never duplicated; the app opens the existing video's places.
- Q: Signed-out users? → A: see videos; no progress at all (no local mode merged at login).
- Q: Online-only marking or offline queue? → A: offline queue.
- Q: Automatic marking (scroll to end, opening a video)? → A: no, manual only.
- Q: Marking a whole book? → A: long-press on the book in the index, always with a confirmation dialog (accidental
  long-press).
- Q: Logout? → A: try to send pending marks, then clear the queue and the cached progress.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A member watches a video about the chapter (Priority: P1)

Maria reads Romanos 5. The toolbar shows a play icon. She taps it and a sheet shows "Romanos 5" with one video
("Justificados pela fé — Romanos 5") and "Romanos" with a playlist (badge "Playlist"). She taps the first card and
the video opens in the YouTube app.

**Why this priority**: it is the visible value of the feature for every member, signed in or not.

**Independent Test**: with a cached video list containing videos on Rm 5 and on the whole book Rm, open the reader
on Rm 5 as a signed-out user and check the icon, the two sections, the badge and that the card opens YouTube.

**Acceptance Scenarios**:

1. **Given** a video placed on Rm 5 and a playlist placed on the whole book Rm, **When** a member opens Rm 5,
   **Then** the play icon shows next to the font-size icon.
2. **Given** no video on Rm 6 nor on the book Rm except the playlist, **When** the member opens Rm 6, **Then** the
   icon still shows (the book has a video) and the sheet has only the "Livro inteiro" section.
3. **Given** no video on the chapter nor on the book, **When** a member opens it, **Then** no play icon is shown.
4. **Given** the sheet, **When** the member taps a card, **Then** its URL opens in the YouTube app if installed,
   otherwise in the browser, keeping any start time in the link.
5. **Given** a video placed on both Rm 5 and the whole book Rm, **When** the sheet opens on Rm 5, **Then** it
   appears only in the "Deste capítulo" section.
6. **Given** the device is offline after the list was fetched once, **When** the member opens the sheet, **Then**
   the cards show with their titles (and cached thumbnails).

---

### User Story 2 - A leader adds a video and chooses its places (Priority: P1)

Pastor João (Liderança) reads Gênesis 1. The play icon is always visible for him. He opens the sheet, taps
"+ Adicionar link", pastes a YouTube URL and confirms. The video appears under "Deste capítulo" and the "Locais do vídeo"
screen opens with "Gênesis 1" in the list. He taps "+ Local", picks Gênesis, selects chapters 2 and 3 and saves;
the video is now in Gn 1, 2 and 3.

**Why this priority**: without management there are no videos to show.

**Independent Test**: as an Admin or Liderança account, add a link on a chapter, add more places in another book,
save, and check the sheet on each place.

**Acceptance Scenarios**:

1. **Given** a manager on a chapter with no videos, **When** the sheet opens, **Then** it shows an empty state with
   "+ Adicionar link".
2. **Given** a valid link, **When** the manager confirms, **Then** the video is created on the current chapter, the
   list is refreshed and "Locais do vídeo" opens for it.
3. **Given** a link already registered, **When** the manager confirms, **Then** the message "Esse vídeo já está
   cadastrado" shows and "Locais do vídeo" opens for the existing video with the current chapter pre-added, not yet
   saved.
4. **Given** an invalid link, a private/removed video or a lookup failure, **When** the manager confirms, **Then**
   the dialog stays open with the server's message.
5. **Given** "Locais do vídeo", **When** the manager taps "+ Local", picks a book and selects several chapters
   and/or "Livro inteiro", **Then** those places are added to the list (duplicates ignored).
6. **Given** the manager removes every place and saves, **Then** a confirmation asks before the video is deleted.
7. **Given** a member (or Mídia), **Then** no "+ Adicionar link", no card menu and no "Reordenar" are shown.

---

### User Story 3 - A leader orders and removes videos (Priority: P2)

João wants the overview video first in Gn 1. He taps "Reordenar" on the "Deste capítulo" section, drags it to the top
and the order is saved for Gn 1 only. Later he removes another video from Gn 1 with the card menu.

**Why this priority**: order and cleanup matter once there are several videos; the feature works without them.

**Independent Test**: with three videos on a chapter, reorder, reopen the sheet and check the order; check another
place of the same video kept its order; remove from chapter and delete from everywhere.

**Acceptance Scenarios**:

1. **Given** a section with two or more videos, **When** the manager taps "Reordenar", **Then** drag handles show
   and dropping a card saves the new order of that place only.
2. **Given** the card menu, **When** the manager chooses "Remover deste capítulo" (chapter section) or "Remover
   deste livro" (book section), **Then** the video leaves only that place.
3. **Given** the card menu, **When** the manager chooses "Excluir vídeo" and confirms, **Then** the video leaves
   every place.
4. **Given** "Editar locais", **Then** "Locais do vídeo" opens for that video.
5. **Given** any management action fails (no network, server error), **Then** a snackbar shows the error and the
   previous state stays.

---

### User Story 4 - A member marks chapters as read (Priority: P1)

Maria finishes Romanos 5 and taps "Marcar como lido" below the last verse. The button turns into "Lido ✓". In the
index, chapter 5 of Romanos shows a check and the book row shows "5/16". She is on the bus without signal; the mark
shows anyway and reaches her account when the phone is back online.

**Why this priority**: tracking what was read is the second core value, and it must be reliable offline.

**Independent Test**: signed in, mark a chapter online and offline; check the button, the index cell, the book
counter, and that after reconnecting the server has the mark (and a fresh install shows it after login).

**Acceptance Scenarios**:

1. **Given** a signed-in user on an unread chapter, **When** they reach the end of the chapter, **Then** a
   "Marcar como lido" button shows after the last verse.
2. **Given** they tap it, **Then** it turns into "Lido ✓" at once, online or offline; tapping "Lido ✓" unmarks.
3. **Given** read chapters, **Then** the Capítulo tab of the index shows them with a check and a distinct color,
   and the book row shows "{read}/{total}", with a check when every chapter is read.
4. **Given** marks made offline, **When** the device gets network, **Then** they are sent to the account and stay
   shown.
5. **Given** a signed-out user, **Then** no button, counter or check is shown anywhere.
6. **Given** another device of the same account marked chapters, **When** the user opens the Bible, **Then** the
   marks from the other device appear.

---

### User Story 5 - A member marks a whole book (Priority: P2)

Maria already read Gênesis before installing the app. She long-presses Gênesis in the index, chooses "Marcar livro
como lido" and confirms "Marcar os 50 capítulos de Gênesis como lidos?". The row shows "50/50 ✓".

**Why this priority**: convenience for people catching up; chapter marking already covers the core.

**Independent Test**: long-press a book, confirm and cancel both actions, check counters and the sent changes.

**Acceptance Scenarios**:

1. **Given** a book not fully read, **When** the user long-presses it, **Then** "Marcar livro como lido" is offered;
   for a fully read book, "Desmarcar livro".
2. **Given** they choose an action, **Then** a confirmation dialog always shows ("Marcar os {n} capítulos de {livro}
   como lidos?" / "Desmarcar os {n} capítulos de {livro}?"); cancelling changes nothing.
3. **Given** they confirm, **Then** only the chapters whose state changes are marked/unmarked, and all of them are
   sent together.
4. **Given** a signed-out user, **Then** long-press does nothing.

---

### User Story 6 - A member marks a video as watched (Priority: P3)

After watching a video, Maria taps the check on its card. The card shows a "Visto" badge and a slightly dimmed
thumbnail.

**Why this priority**: nice to have; reading progress is the main tracking.

**Independent Test**: signed in, toggle the check on a card online and offline; check the badge, the dim, and the
state on another device.

**Acceptance Scenarios**:

1. **Given** a signed-in user, **Then** each card has a check control; tapping marks or unmarks it at once.
2. **Given** a watched video, **Then** its card shows "Visto" and a dimmed thumbnail in every place it appears.
3. **Given** a user opens a video, **Then** it is not marked as watched automatically.

---

### Edge Cases

- **Video list never fetched and device offline**: the play icon is hidden for members; managers still see it, and
  the sheet shows the empty state (adding needs network and fails with a snackbar).
- **Cached list corrupted**: treated as empty, refetched on the next open.
- **Places for a book/chapter that don't exist in the local Bible index**: ignored when building the sheet.
- **Video deleted on the server while marked watched or queued**: the server rejects the queued item; the app drops
  it; the mark disappears after the next progress fetch.
- **Mark + unmark offline**: only the last state is sent.
- **Two devices change the same chapter offline**: the change made later (by action time) wins on the server.
- **Logout with pending marks and no network**: the pending marks are lost (accepted); the next account starts
  clean.
- **Login**: progress is fetched right after login.
- **Bible not downloaded yet**: no reader, no index; videos and progress have nothing to attach to.
- **Account loses `bible_videos` while the sheet is open**: the next management call fails with permission refused
  (snackbar); controls hide when the profile refreshes.
- **Very long title**: two lines, ellipsis.
- **Thumbnail fails to load**: a neutral placeholder with the play symbol.
- **"Locais do vídeo" opened for a video that disappeared**: the save fails with the server message; leaving the
  screen keeps nothing.

## Requirements *(mandatory)*

### Functional Requirements

**Videos — data**

- **FR-001**: The app MUST fetch the full video list without authentication and keep it on the device, refreshing
  it (conditional request) each time the Bible opens and after any management change.
- **FR-002**: Signed-in and signed-out users MUST see the same videos.
- **FR-003**: The app MUST recognize the new permission scope `bible_videos`; management controls show only when
  the user has at least the manage level on it; deleting a video needs the level the server requires for deletion.

**Videos — reader**

- **FR-004**: The reader toolbar MUST show a play icon next to the font-size icon when the current chapter or the
  current book has at least one video; for managers, always.
- **FR-005**: The play icon MUST open a sheet with a book section ("Livro inteiro") first and a chapter section
  ("Deste capítulo") below it, each in the order of its place; a video on both shows only in the chapter section; an empty section
  is hidden.
- **FR-006**: Each card MUST show the 16:9 thumbnail, the title in up to two lines, and a "Playlist" badge for
  playlists. Tapping opens the link outside the app (YouTube app or browser).
- **FR-007**: Thumbnails MUST stay visible offline once loaded.

**Videos — management**

- **FR-008**: Managers MUST be able to add a link from the sheet; the current chapter is its first place; on success
  "Locais do vídeo" opens for the new video.
- **FR-009**: An already registered link MUST show "Esse vídeo já está cadastrado" and open "Locais do vídeo" of
  the existing video with the current chapter pre-added and unsaved.
- **FR-010**: Invalid link, unavailable video and lookup failure MUST show their message inside the add dialog.
- **FR-011**: "Locais do vídeo" MUST show thumbnail and title, the list of places ("Gênesis 1", "Êxodo (livro
  inteiro)") each removable, and "+ Local" with a picker: book (with search), then a multi-select chapter grid plus
  "Livro inteiro". Saving sends the whole list. An empty list asks for confirmation and deletes the video.
- **FR-012**: Each card MUST have, for managers, a menu with "Editar locais", "Remover deste capítulo" / "Remover
  deste livro" (by section) and "Excluir vídeo" (with confirmation).
- **FR-013**: Each section MUST offer "Reordenar" to managers when it has two or more videos; dragging saves the
  order of that place only.
- **FR-014**: Management MUST need network; a failure MUST show a snackbar and keep the previous state.

**Progress**

- **FR-015**: Progress MUST exist only for signed-in users; signed-out users see no marking control.
- **FR-016**: The displayed progress MUST be the account's state (fetched from the server and kept on the device)
  with the pending local changes applied on top, so a mark shows instantly, also offline.
- **FR-017**: Each change MUST be kept as a state (target, done, time of the action with UTC offset); only the
  latest per target is kept.
- **FR-018**: Pending changes MUST be sent in the background whenever there is network, after each change and on
  app start; every item sent is removed from the queue whether accepted or rejected; the state is fetched again
  after each send.
- **FR-019**: The account's state MUST be fetched when the Bible opens, after login and after each send.
- **FR-020**: On logout the app MUST try to send pending changes, then clear the pending changes and the stored
  progress.
- **FR-021**: The end of each chapter MUST show "Marcar como lido" / "Lido ✓" for signed-in users; reaching the end
  MUST NOT mark it.
- **FR-022**: The index MUST show read chapters with a check (Capítulo tab), and each book row its "{lidos}/{total}"
  counter with a check when complete.
- **FR-023**: Long-pressing a book row MUST offer marking or unmarking the whole book, always behind a confirmation
  that states the chapter count; only changing chapters are queued.
- **FR-024**: A book is read when every chapter is read; this is derived, never stored.
- **FR-025**: Each video card MUST offer a watched check for signed-in users; a watched card shows "Visto" and a
  dimmed thumbnail; opening the video MUST NOT mark it.

### Key Entities

- **Bible video**: a YouTube video or playlist — id, kind, link, title, thumbnail — with its places.
- **Video place**: a book and optionally a chapter (none = whole book) with the video's position in that place.
- **Chapter progress**: book + chapter read by the account.
- **Video progress**: video watched by the account.
- **Pending progress change**: target (chapter or video), done, time of the action; one per target.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The play icon shows (or not) as soon as the chapter is shown, with no wait for the network.
- **SC-002**: A mark (chapter, book or video) shows on screen in under 200 ms, online or offline.
- **SC-003**: 100% of marks made offline reach the account once the device is online again, unless the user signed
  out before that.
- **SC-004**: A leader can add a video to three chapters in under one minute.
- **SC-005**: Members, signed-out users and Mídia never see a management control.
- **SC-006**: After logout, the next account on the device sees none of the previous account's progress.

## Assumptions

- The backend contract is the planned one in `../TODO/specify_bible_videos_backend.md`; the backend had no code when
  this spec was written. Field names, error codes, the place-removal shape and the batch limit must be re-checked
  against the backend's spec/contracts once implemented.
- Removing one place is `DELETE api/bible/videos/{id}/places/?book=..&chapter=..` (query; no `chapter` = whole
  book). The sync batch limit is 500 items.
- Videos are translation-independent: places use the book abbreviation and the chapter numbers shared by NAA and
  ARA.
- Thumbnails come from YouTube's image servers and are kept by the image cache already used in the app.

## Out of Scope

- Reading plans, overall Bible percentage, streaks, statistics.
- Progress for signed-out users; a queue that survives logout.
- Editing video titles; an "all videos" admin screen; a marker on the index for books with videos.
- Automatic marking (scrolling to the end, opening a video).
- Push notifications about new videos.
