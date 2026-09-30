# Feature Specification: Gallery Management

**Feature Branch**: `008-gallery-management`

**Created**: 2026-09-29

**Status**: Draft

**Input**: User description: "Add gallery management to the app: the Admin, Liderança and Mídia roles build the
gallery from the same screens members browse — create, rename, move and reorder albums; set and remove covers;
upload, edit, move and delete photos; delete albums. Buttons appear by the user's level on scope `gallery`. Trash and
restore are feature 3; member tags are feature 4." (full request — current state of backend and app, what to build,
specs and tests, out of scope, open decisions — in the `/speckit-specify` invocation that created this directory)

## Context

Since feature 007 the app's gallery is a faithful, offline-readable copy of the server's album tree, but it is
read-only: whoever takes care of the gallery (today Admin, Liderança and Mídia) has to use another tool to add an
album, upload the photos of a Sunday service or fix an album's cover. The backend already exposes every write.

This feature puts those writes inside the gallery screens members already know. A member with no level on `gallery`
sees exactly today's screens; a user with `manage` or `owner` sees the extra controls their level allows.

Feature 2 of 4 of the app's gallery work: (1) tree and sync — done (007); (2) management — this one; (3) trash;
(4) member tags.

### Source of truth on the backend (not redefined here)

- `backend/specs/gallery/spec.md` — final state of the gallery domain.
- `backend/specs/013-gallery-write-api/contracts/gallery-api.md` — every album and photo write, covers, order, errors.
- `backend/specs/014-gallery-trash-sync/contracts/gallery-trash-api.md` — `DELETE` of albums and photos (sends to the
  trash) and the change feed.
- `backend/specs/016-photo-upload-idempotency/contracts/photo-upload-api.md` — `client_upload_id`.
- `backend/specs/012-feature-role-permissions/spec.md` — levels and the `permissions` field of the profile.

### Backend facts this feature relies on

- **Levels on `gallery`**: `manage` allows create, edit, move, order, set cover and upload; `owner` also allows
  deleting albums and photos and removing a cover. The app follows the levels, never the role names. Reading the
  gallery still requires being flagged as a member; a manager who is not a member cannot see the gallery (operational
  rule: every manager is flagged; the app adds no workaround).
- **Permission is checked before existence**: without the level, any write answers "forbidden", even for an id that
  no longer exists.
- **Albums**: created last among their siblings; a move places the album last among its new siblings; a reorder must
  list exactly the current live siblings, otherwise it is refused with the ids missing, unexpected and repeated.
  Sibling names are unique (refused with a Portuguese message). Moving an album inside its own subtree is refused.
- **Covers**: an uploaded cover is stored as a square crop; removing an album's own cover makes its resolved cover
  come from a sub-album again, or none. The first photo uploaded into an album with no photos and no own cover becomes
  its cover.
- **Photos**: one upload request may carry a client upload id with exactly one file. The same id of a live photo
  answers "created" with that photo (possibly in another album now); the same id of a photo in the trash answers
  "conflict" with "Esta foto já foi enviada e depois apagada; ela está na lixeira.". Rejected files come back with a
  Portuguese reason. Accepted formats JPEG, PNG, WEBP and GIF, at most 10 MB and 50 MP; the capture date is read from
  the image's EXIF; the file is stored exactly as sent. A move places the photo last in its new album and never moves
  its file.
- **Deleting** an album sends it, its sub-albums and all their photos to the trash for 30 days; deleting a photo sends
  it to the trash. Any write naming a trashed item, or under a trashed album, answers "not found".
- **Every write reaches other devices through the change feed**, including derived changes: a rename updates the
  photos' album name, a cover change updates ancestors' resolved covers, a reorder updates positions.

## Resolved Decisions

The eight open decisions of the request, settled here so planning can start. Each is a default the user may overturn
in `/speckit-clarify`.

| # | Decision | Chosen | Why |
|---|----------|--------|-----|
| 1 | Upload network | Any network, including mobile data | The manager chose to send now; uploads are prepared (downscaled) and a Sunday's photos are a few tens of MB. Unlike the originals' download, nobody else pays for this data |
| 2 | Long queues and notifications | The upload runs as a foreground, user-visible data transfer, so a long queue keeps running with the app in the background. The app already asks for the notification permission at launch (Android 13+); nothing new is asked. If it was refused, uploads still run and progress stays visible in the album screen | A queue of 50 photos must not stop when the screen turns off; a refused permission must never block the upload |
| 3 | Orientation | Rotate the pixels to upright and write the orientation as "normal" | The photo looks upright everywhere — server thumbnail, cover crop, any viewer — without trusting each reader to honour the tag |
| 4 | Animated GIFs | Sent as picked when within 10 MB and 50 MP (animation kept); otherwise converted like any other image (first frame, JPEG) | Keeps animations when possible; never sends a file the server would refuse for size |
| 5 | Where the queue lives | A queue file on the device, next to the gallery index and cleared with it, holding each item's state; the album screen observes it for progress and failures | Survives process death and is readable by the screen without asking the background job |
| 6 | JPEG quality | 90; if the result still exceeds 10 MB, re-encoded at lower quality until it fits | Visually lossless for church photos; the step-down guarantees the size limit |
| 7 | "Usar como capa" | In the viewer only | It concerns exactly one photo; the selection bar is for batches |
| 8 | Batch move and delete | Try every photo, report at the end | One bad photo must not leave the rest of the batch undone |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Controls follow the user's level (Priority: P1)

A member with no level on `gallery` browses exactly as today. A user with `manage` sees the controls to create, edit,
move, organize, set covers and add, edit and move photos. A user with `owner` also sees the controls to delete albums
and photos and to remove a cover. The admin panel's "Galeria" card, shown to users with `manage` or more, is now
enabled and opens the gallery.

**Why this priority**: Every other story depends on it, and it guarantees members see no control they cannot use.

**Independent Test**: Sign in as a member with no level, then as a `manage` user, then as an `owner` user, and open
the root, an album and a photo each time: the controls visible match the table in FR-001 exactly.

**Acceptance Scenarios**:

1. **Given** a member with no level on `gallery`, **When** they open the root, an album and a photo, **Then** no
   management control is visible and the screens are the same as before this feature.
2. **Given** a user with `manage`, **When** they open an album, **Then** they see "Novo álbum", "Adicionar fotos",
   "Editar álbum", "Mover álbum", "Organizar" and "Trocar capa", and do not see "Apagar álbum" nor "Remover capa".
3. **Given** a user with `owner`, **When** they open an album that has its own cover, **Then** they also see
   "Apagar álbum" and "Remover capa".
4. **Given** a user with `manage` or more, **When** they tap the "Galeria" card in the admin panel, **Then** the
   gallery root opens.
5. **Given** a user whose level changes on the server, **When** the app refreshes access, **Then** the controls
   appear or disappear without reopening the gallery.
6. **Given** a user whose level was removed on the server, **When** they try any write, **Then** the app refreshes
   access, hides the controls they lost and shows the server's message.

---

### User Story 2 - Upload the photos of an event (Priority: P1)

A manager opens an album, taps "Adicionar fotos", picks many photos at once from the phone and leaves. The photos are
prepared on the phone and sent one by one in the background, with a notification "Enviando 3 de 20" and the same
progress in the album. Each accepted photo appears in the album. Photos the server refuses are listed in the album with
the reason, and each can be dismissed.

**Why this priority**: Adding photos is the task the gallery exists for; today it cannot be done from the app.

**Independent Test**: In an album, add 20 photos (one HEIC, one PNG, one 48 MP JPEG taken in portrait), turn the
screen off: all 20 appear in the album on this device and on another member's device, upright, each with its capture
date, and none was downloaded back by this device.

**Acceptance Scenarios**:

1. **Given** a `manage` user in an album, **When** they tap "Adicionar fotos", **Then** the system photo picker opens
   with multi-select and no storage permission is asked.
2. **Given** 20 photos picked, **When** the picker closes, **Then** they are copied into the app at once and queued,
   and the album shows "Enviando 0 de 20" before any network activity.
3. **Given** a queue running, **When** the user leaves the album, the app goes to the background or the process is
   killed, **Then** the queue continues (or resumes) until every item is sent or failed.
4. **Given** a queued photo, **When** it is sent, **Then** its longest side is at most 4000 px, it is a JPEG (except a
   GIF within limits), it is at most 10 MB and 50 MP, it looks upright, and the server reads the same capture date as
   the picked photo.
5. **Given** a photo accepted by the server, **When** its answer arrives, **Then** it appears in the album at once and
   its prepared file becomes the photo's original on this device, with no download.
6. **Given** the network drops or the server answers with a server error, **When** the send fails, **Then** the item
   is retried automatically later with the same upload id, and the queue waits for a network.
7. **Given** an item sent, whose answer was lost, and retried, **When** the server answers "created" with the photo it
   already had, **Then** the item counts as sent and the photo appears once.
8. **Given** a file the server refuses, **When** its answer arrives, **Then** the item is not retried and appears in
   the album's failed list with the server's Portuguese reason.
9. **Given** an item whose earlier send was later deleted to the trash, **When** it is retried, **Then** it is not
   retried again and the failed list shows "Esta foto já foi enviada e depois apagada; ela está na lixeira.".
10. **Given** the album deleted by someone else while its photos are queued, **When** the next item is sent, **Then**
    that item and every other queued item of that album fail with "O álbum foi apagado".
11. **Given** a failed item in the list, **When** the user dismisses it, **Then** it leaves the list and its file is
    deleted.
12. **Given** a picked image the phone cannot read, **When** it is prepared, **Then** it goes to the failed list with
    "Não foi possível ler esta imagem." and the rest of the queue continues.

---

### User Story 3 - Create and edit albums (Priority: P1)

A manager creates an album on the root or a sub-album inside the album they are in, with a name and, optionally, a
description and an event date. They can later edit the same fields.

**Why this priority**: Without albums there is nowhere to upload; with this and Story 2 the gallery can be built from
the app.

**Independent Test**: On the root, create "Retiro 2026" with an event date; inside it create "Sábado"; edit "Sábado"
to "Sábado à noite" and clear nothing else: both albums appear at once, in last position, on this device and after
the next sync on another member's device.

**Acceptance Scenarios**:

1. **Given** a `manage` user on the root, **When** they create an album, **Then** it is a root album placed last.
2. **Given** a `manage` user inside an album, **When** they create an album, **Then** it is a sub-album of that album,
   placed last among its sub-albums.
3. **Given** the name form, **When** the name is empty or only spaces after trimming, or longer than 100 characters,
   **Then** "Salvar" is disabled and the field says why.
4. **Given** a name already used by a sibling, **When** the user saves, **Then** the form stays open with the
   server's Portuguese message under the name.
5. **Given** an album's edit form, **When** the user changes the name, description or event date (or clears the
   date), **Then** the album shows the new values at once, and its photos' album name follows after the next sync.
6. **Given** no network, **When** the user saves, **Then** the form stays open with "Sem conexão" and nothing changes
   on the device.

---

### User Story 4 - Delete albums and photos (Priority: P2)

An owner deletes an album (with everything inside it) or one or more photos. The confirmation says what will go and
that it stays 30 days in the trash.

**Why this priority**: Mistaken or duplicate uploads must be removable; trash and restore come in feature 3.

**Independent Test**: As owner, delete an album with 2 sub-albums and 41 photos, then long-press 3 photos in another
album and delete them: the confirmations read "Apagar 'Retiro' com 2 subálbuns e 41 fotos? Fica 30 dias na lixeira."
and "Apagar 3 fotos? Ficam 30 dias na lixeira.", and everything deleted disappears from this device at once and from
other devices after their next sync.

**Acceptance Scenarios**:

1. **Given** an owner in an album, **When** they choose "Apagar álbum", **Then** the confirmation shows the counts of
   sub-albums (all levels) and photos (all levels) computed from the device's copy.
2. **Given** an album with no sub-albums, **When** the confirmation is shown, **Then** it omits the sub-album count
   (e.g. "Apagar 'Culto' com 12 fotos? Fica 30 dias na lixeira."); with nothing inside, it reads "Apagar 'Culto'? Fica
   30 dias na lixeira.".
3. **Given** a confirmed album delete, **When** the server accepts it, **Then** the album, its whole subtree, their
   photos and their files leave the device, and the screen goes up to the parent (or the root) with "Álbum enviado
   para a lixeira".
4. **Given** a photo open in the viewer, **When** an owner deletes it, **Then** the viewer shows the next photo (the
   previous when it was the last, the album when it was the only one) with "Foto enviada para a lixeira".
5. **Given** 3 selected photos, **When** the owner confirms the delete, **Then** the photos are deleted one at a
   time, every one is attempted, and the result reports "3 fotos apagadas" or, on partial failure, how many were
   deleted and why the others were not.
6. **Given** an item already deleted by someone else, **When** the delete answers "not found", **Then** it is counted
   as gone and removed from the device.

---

### User Story 5 - Move albums and photos (Priority: P2)

A manager moves an album under another album (or to the root), or moves one or more photos to another album, choosing
the destination in a tree of albums.

**Why this priority**: Uploads go to the album being viewed; moving fixes a wrong album without re-uploading.

**Independent Test**: Move album "Sábado" from "Retiro 2025" to "Retiro 2026"; select 5 photos in "Culto" and move
them to "Sábado": the tree offered for the album hides "Sábado" and its descendants; after the moves, both appear in
their new places, last, and no photo file was downloaded again.

**Acceptance Scenarios**:

1. **Given** "Mover álbum", **When** the tree picker opens, **Then** it offers "Raiz" and every album except the
   album itself and all its descendants, with the current parent shown but not selectable.
2. **Given** a destination chosen, **When** the server accepts, **Then** the album appears last among its new
   siblings and its screen's subtitle shows the new parent.
3. **Given** the move refused because someone else changed the tree meanwhile (cycle or unknown album), **When** the
   answer arrives, **Then** a sync runs and the server's message is shown.
4. **Given** photos selected (by long press in the grid) or one photo in the viewer, **When** the user chooses
   "Mover", **Then** the tree picker shows every album, with the current album not selectable and no "Raiz".
5. **Given** several photos moved, **When** the batch ends, **Then** every photo was attempted in order and the result
   reports "5 fotos movidas para 'Sábado'" or, on partial failure, how many moved and why the others did not.
6. **Given** a photo moved from the viewer, **When** the move succeeds, **Then** the viewer shows the next photo of
   the current album with "Foto movida para 'Sábado'".

---

### User Story 6 - Organize the order (Priority: P2)

A manager enters "Organizar" on the root or in an album, drags root albums, sub-albums and photos into a new order,
and taps "Salvar". "Cancelar" discards the changes.

**Why this priority**: The church chooses the order members see; without it every new album and photo only lands last.

**Independent Test**: In an album with 3 sub-albums and 10 photos, enter "Organizar", move the last sub-album to the
top and the fifth photo to the first place, save: this device shows the new order at once and another member's
device after its next sync.

**Acceptance Scenarios**:

1. **Given** "Organizar" on the root, **When** the mode opens, **Then** the root albums can be dragged among
   themselves; every other control is hidden until "Salvar" or "Cancelar".
2. **Given** "Organizar" in an album, **When** the mode opens, **Then** sub-albums can be dragged among sub-albums and
   photos among photos; an item never crosses from one group to the other.
3. **Given** only the photos' order changed, **When** the user saves, **Then** one order request is sent, for the
   photos only; with both groups changed, two; with nothing changed, none, and the mode closes.
4. **Given** someone else added, moved or deleted a sibling meanwhile, **When** the server refuses the order as
   mismatched, **Then** a sync runs, the mode stays open showing the updated list, and the message "A ordem mudou
   enquanto você editava. Confira e salve de novo." is shown.
5. **Given** "Cancelar", **When** tapped, **Then** the mode closes and the order is the one before entering it.

---

### User Story 7 - Set and remove covers (Priority: P3)

A manager changes an album's cover with an image from the phone ("Trocar capa") or with one of the album's photos
("Usar como capa" in the viewer). An owner removes an album's own cover ("Remover capa").

**Why this priority**: The automatic cover (first photo) is usually acceptable; a chosen cover is a refinement.

**Independent Test**: In an album, choose "Usar como capa" on its third photo, then "Trocar capa" with an image from
the phone, then "Remover capa" as owner: the album's tile, and its parent's tile when it inherits it, show each cover
in turn, and after removal the tile shows a sub-album's cover or black.

**Acceptance Scenarios**:

1. **Given** "Trocar capa", **When** the user picks one image, **Then** it is prepared like an upload and sent, and
   the album's tile shows the new cover once the server answers.
2. **Given** "Usar como capa" on a photo whose original is on the device, **When** chosen, **Then** that file is sent
   as the album's cover; with the original not on the device, it is downloaded first, and without network the action
   fails with "Sem conexão".
3. **Given** an album whose cover is its own, **When** an owner opens it, **Then** "Remover capa" is offered; with an
   inherited cover or none, it is not.
4. **Given** "Remover capa" confirmed, **When** the server accepts, **Then** the album's tile shows the cover resolved
   by the next sync (from a sub-album or black), and the ancestors' tiles follow.
5. **Given** a cover image the server refuses, **When** the answer arrives, **Then** the previous cover stays and the
   server's message is shown.

---

### User Story 8 - Edit a photo (Priority: P3)

A manager edits a photo's name, description and date taken from the viewer.

**Why this priority**: Names come from file names ("IMG_0042"); fixing them is useful but not urgent.

**Independent Test**: In the viewer, rename a photo to "Batismo da Ana", set a description, clear the date taken:
the top bar shows the new name at once and another member's device after its next sync.

**Acceptance Scenarios**:

1. **Given** the edit form, **When** the name is empty after trimming or longer than 100 characters, **Then** "Salvar"
   is disabled and the field says why.
2. **Given** a valid edit, **When** the server accepts, **Then** the viewer shows the new name at once; the photo's
   file on the device is untouched.
3. **Given** the photo deleted by someone else meanwhile, **When** the edit answers "not found", **Then** a sync runs
   and the viewer behaves as for a photo removed while open.

---

### Edge Cases

- **Offline write**: every write except the upload queue fails at once with "Sem conexão"; nothing changes on the
  device. The upload queue waits for a network.
- **Forbidden (403) on any write**: access is refreshed, the controls the user lost disappear, the server's message is
  shown; nothing changes on the device. On the upload queue, the item and every remaining item fail with that message.
- **Session expired (401)**: follows the existing session path; if the session ends, the queue is cleared like on
  sign-out.
- **Double tap on "Salvar" or a confirmation**: only one request is sent; controls are disabled while a write is in
  flight.
- **A sync arriving while a form is open**: the form keeps the user's typing; the save answer wins.
- **A sync arriving in "Organizar"**: the dragged order is kept on screen; the server decides at save (Story 6,
  scenario 4).
- **Change feed repeating a write already applied locally**: applying it again changes nothing.
- **Selection mode and "Organizar"**: mutually exclusive; entering one closes the other. Back leaves either mode
  without leaving the screen.
- **Selected photo removed by a sync**: it leaves the selection silently.
- **Upload of a photo already sent from this device and still live** (same queued item retried after success was
  lost): the server returns the existing photo, possibly in another album; it is applied where the server says.
- **Queued photos for several albums**: one queue for the whole gallery; the notification counts every item; each
  album shows only its own progress and failures.
- **Prepared image still too large at the lowest quality or above 50 MP after downscaling**: fails before sending with
  "Imagem grande demais para enviar." (should not happen with the 4000 px limit; listed for completeness).
- **PNG or WEBP with transparency**: transparent areas become white in the JPEG.
- **Picked item that is a video**: the picker offers images only.
- **Name of an uploaded photo**: the picked file's name with its extension changed to match what is sent (e.g.
  `IMG_0042.HEIC` becomes `IMG_0042.jpg`); when the picker gives no name, `foto_<data e hora>.jpg`.
- **Manager not flagged as member**: sees the gallery's "Disponível apenas para membros." state like any non-member,
  also when coming from the admin card (operational rule, no workaround).
- **Moving or deleting the album currently open from its own screen**: after a move the screen stays with the new
  subtitle; after a delete it goes up (Story 4, scenario 3), without the "Este álbum foi removido" message reserved for
  removals by others.

## Requirements *(mandatory)*

### Functional Requirements

**Access**

- **FR-001**: Every management control MUST be shown only when the user's current level on `gallery` allows it:

  | Level | Controls |
  |-------|----------|
  | none | none (today's screens) |
  | `manage` | "Novo álbum", "Editar álbum", "Mover álbum", "Organizar", "Trocar capa", "Adicionar fotos", "Usar como capa", "Editar foto", photo selection with "Mover" |
  | `owner` | everything in `manage`, plus "Apagar álbum", "Remover capa" (own cover only), "Apagar" on photos |

- **FR-002**: The controls MUST follow the level live: a level refreshed while the gallery is open shows or hides them
  without reopening any screen. The level used offline is the last one known.
- **FR-003**: A "forbidden" answer to any write MUST refresh the user's access, hide the controls lost and show the
  server's message, leaving the device's copy unchanged.
- **FR-004**: The admin panel's "Galeria" card MUST be enabled and open the gallery root.

**Writes in general**

- **FR-005**: Every write except the upload queue MUST require a network; offline it MUST fail with "Sem conexão" and
  change nothing on the device.
- **FR-006**: After a successful write, the app MUST apply its result to the device's copy at once — upsert the
  returned album or photo; remove a deleted photo, or a deleted album with its whole subtree and their files; apply
  the positions of a saved order — without advancing the sync cursor, and then run a sync to pick up derived changes.
- **FR-007**: Applying a write's result and later receiving the same change from the feed MUST give the same result as
  applying it once.
- **FR-008**: While a write is in flight, the control that started it MUST be disabled; a single user action MUST send
  a single request.
- **FR-009**: Server messages in Portuguese (duplicate name, cycle, rejected file, trashed original) MUST be shown as
  sent; English technical messages (order mismatch) MUST be replaced by the Portuguese texts of this spec.

**Albums**

- **FR-010**: "Novo álbum" MUST create a root album from the root and a sub-album from inside an album, with a name
  (trimmed, 1–100 characters, required), an optional description and an optional event date.
- **FR-011**: "Editar álbum" MUST edit the same fields, allowing the description and event date to be cleared.
- **FR-012**: "Mover álbum" MUST offer a tree picker with "Raiz" and every album except the album itself and all its
  descendants; the current parent MUST be shown but not selectable.
- **FR-013**: "Apagar álbum" MUST ask for confirmation stating the album's name and the counts of sub-albums and
  photos in its whole subtree, computed from the device's copy, and that it stays 30 days in the trash.
- **FR-014**: A refused album write (duplicate name, cycle, unknown album) MUST keep the form or picker open where it
  makes sense, show the server's message and, for cycle or unknown album, run a sync.

**Covers**

- **FR-015**: "Trocar capa" MUST let the user pick one image from the phone, prepare it as in FR-030, and send it as
  the album's cover.
- **FR-016**: "Usar como capa" in the viewer MUST send the photo's original from the device as its album's cover,
  downloading it first when it is not on the device.
- **FR-017**: "Remover capa" MUST be offered to `owner` only when the album's cover is its own (not inherited, not
  absent), after confirmation.

**Order**

- **FR-018**: "Organizar" MUST be available on the root (root albums) and in an album (sub-albums and photos, each
  group reordered within itself).
- **FR-019**: "Salvar" MUST send one order request per group whose order changed, listing every item of the group;
  "Cancelar" MUST discard the draft.
- **FR-020**: An order refused as mismatched MUST trigger a sync, keep the mode open with the updated list and show "A
  ordem mudou enquanto você editava. Confira e salve de novo.".

**Photos**

- **FR-021**: A long press on a photo in the album grid MUST start selection mode; taps then toggle selection; the
  selection bar MUST show the count and offer "Mover" and, for `owner`, "Apagar". No tag action.
- **FR-022**: The viewer MUST offer, by level, "Editar foto" (name trimmed 1–100, description, date taken — clearable),
  "Mover", "Usar como capa" and "Apagar".
- **FR-023**: "Mover" for photos MUST offer a tree picker of every album with the current album not selectable.
- **FR-024**: "Apagar" for photos MUST ask for confirmation ("Apagar 1 foto? Fica 30 dias na lixeira." / "Apagar N
  fotos? Ficam 30 dias na lixeira.").
- **FR-025**: A batch move or delete MUST send one request per photo, in sequence, attempt every photo, and report at
  the end how many succeeded and, when some failed, how many and why (grouped by reason). A "not found" on delete
  counts as done.

**Upload**

- **FR-026**: "Adicionar fotos" MUST open the system photo picker for images, with multi-select and no storage
  permission; the destination is always the album open.
- **FR-027**: Each picked image MUST be copied into the app's private storage before the picker result is released,
  and queued with a new random upload id generated once for it.
- **FR-028**: The upload queue MUST persist on the device and survive leaving the screen, the app going to the
  background and the process being killed; it MUST process one photo per request, in sequence, on any network.
- **FR-029**: While the queue runs, a notification MUST show "Enviando X de N" for the whole queue, and each album with
  queued items MUST show its own progress; a refused notification permission MUST NOT stop the upload.
- **FR-030**: Before sending, each image MUST be prepared: decoded (HEIF included), downscaled so its longest side is
  at most 4000 px, rotated to upright with orientation "normal", encoded as JPEG at quality 90 (lowered until it fits
  10 MB), keeping the original EXIF capture date; a GIF within 10 MB and 50 MP is sent unchanged.
- **FR-031**: Every retry of an item MUST resend the same upload id.
- **FR-032**: Network failures and server errors MUST be retried automatically, with growing delay, until the item
  is sent or fails permanently.
- **FR-033**: An item MUST fail permanently, without retry, and go to its album's failed list with its reason when:
  the file is rejected (server's reason); the original was trashed (server's detail); the album no longer exists ("O
  álbum foi apagado", also failing every other queued item of that album); access was lost (server's message, also
  failing every remaining item); the image cannot be read or prepared ("Não foi possível ler esta imagem." / "Imagem
  grande demais para enviar.").
- **FR-034**: An accepted photo (first upload or recognized repeat) MUST be applied to the device's copy like any
  other write, and its prepared file MUST become the photo's original on the device, so it is never downloaded back.
- **FR-035**: Each failed item MUST be dismissible from the album's failed list, deleting its file.
- **FR-036**: The queue MUST hold no file longer than needed: an item's file is deleted when it is sent (moved to the
  original), dismissed, or the queue is cleared.

**Session end**

- **FR-037**: Sign-out (and a session that ends) MUST cancel the upload queue, delete its files and its state, along
  with everything 007 already clears.

**Specs**

- **FR-038**: `specs/gallery/spec.md` MUST be updated to the state this feature leaves (management controls by level,
  writes and their local apply, upload queue and preparation, covers, order, errors and messages), and
  `specs/admin/spec.md` MUST list the "Galeria" card as enabled and opening the gallery, in the same commit as the
  code.

**Out of scope**: trash screen and restore (deleting only sends to the trash); member tags (showing, tagging,
filtering, "Minhas fotos", `member_id`) and any tag action in the selection bar; any backend change; editing a
photo's image (crop, rotate, filters); choosing an upload destination other than the album open; undo other than
through the trash (feature 3); moving an album by drag between levels; uploading videos.

### Key Entities

- **Access level on `gallery`**: none, `manage` or `owner`, from the user's profile; decides which controls exist.
- **Write**: one change sent to the server (create, edit, move, order, cover, delete); on success its result is
  applied to the device's copy before the next sync.
- **Upload item**: one picked image waiting to be sent — its album, its private file, its upload id (fixed for life),
  its display name, and its state (waiting, preparing, sending, sent, failed with reason).
- **Upload queue**: the persistent list of upload items across albums, processed one at a time; cleared at sign-out.
- **Order draft**: the order dragged in "Organizar", per group (root albums, sub-albums, photos), discarded on cancel.
- **Selection**: the set of photos chosen in an album's grid for a batch move or delete.
- **Batch result**: for a batch move or delete, how many succeeded and the failures grouped by reason.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A manager can create an album and start uploading 20 photos to it in under 1 minute from opening the
  gallery.
- **SC-002**: 50 photos queued with the screen off are all sent (or listed as failed with a reason) without the user
  reopening the app.
- **SC-003**: 100% of photos uploaded from the app look upright and keep their capture date, on the server and on
  every device.
- **SC-004**: An upload retried any number of times after a lost answer creates exactly one photo on the server.
- **SC-005**: A photo uploaded from a device is downloaded 0 times by that device.
- **SC-006**: Every write is visible on the device that made it immediately after the server's answer, and on other
  members' devices after their next sync.
- **SC-007**: A member with no level on `gallery` sees 0 management controls anywhere in the gallery.
- **SC-008**: No photo sent by the app is refused by the server for size or pixel count.
- **SC-009**: After sign-out, the device holds 0 queued uploads and 0 bytes of upload files.
- **SC-010**: Every rule in the request's test list (gating per level, local apply of each write without moving the
  cursor, album delete removing its subtree, move picker hiding self and descendants, order mismatch, upload queue
  outcomes and retries, image preparation, 403 refreshing access, logout clearing the queue) is covered by an
  automated test.

## Assumptions

- Admin, Liderança and Mídia all hold `owner` on `gallery` today, but nothing in the app depends on role names.
- Every manager is flagged as a member (operational rule); the app does not work around a manager who is not.
- A church event uploads tens of photos, occasionally up to about 200; one-at-a-time sending is fast enough.
- The phone's system photo picker is available on every supported Android version (through the platform or Google
  Play services back-port).
- The upload queue is per device and per session; queued items of a previous session are never sent by another user.
- Descriptions have no length limit on the client beyond what the server enforces.
- Event dates and dates taken are entered with a date picker and shown as `dd/MM/yyyy`.
- The existing sync (007) is the only way derived changes (ancestor covers, album names on photos, positions of
  others) reach the device; the app does not compute them itself.
- A drag-to-reorder library compatible with the project's Compose and Kotlin versions exists; checking it is part of
  planning.
