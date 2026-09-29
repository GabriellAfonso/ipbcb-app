# Feature Specification: Gallery Album Tree and Sync

**Feature Branch**: `007-gallery-album-sync`

**Created**: 2026-09-29

**Status**: Draft

**Input**: User description: "Rebuild the member-facing gallery around the backend's album tree and change feed: nested
albums with covers, manual order, and a local copy that follows the server (new, changed and deleted albums and
photos), checked on app open, on gallery open and every 6 hours. Read-only for every member; management, trash and
member tags are later features." (full request — current state of backend and app, local copy, sync, thumbnails,
screens, specs and tests, out of scope, open decisions — in the `/speckit-specify` invocation that created this
directory)

## Context

Today the gallery downloads every photo once and never asks the server again. Albums are guessed from disk folders,
photos are sorted by file name, a photo renamed or moved on the server is never updated, and a photo deleted on the
server stays on the device forever. There are no nested albums, no covers and no manual order.

Backend features 013 (album tree, covers, thumbnails, `position`), 014 (trash, change feed) and 015 (the `members`
field on photos) are already live on `dev`. This feature makes the app's gallery a faithful, offline-readable copy of
the server's gallery and keeps it current.

Feature 1 of 4 of the app's gallery work: (1) tree and sync — this one; (2) management; (3) trash; (4) member tags.

### Source of truth on the backend (not redefined here)

- `backend/specs/gallery/spec.md` — final state of the gallery domain.
- `backend/specs/013-gallery-write-api/contracts/gallery-api.md` — Album and Photo resources, tree order, covers,
  thumbnails.
- `backend/specs/014-gallery-trash-sync/contracts/gallery-trash-api.md` — change feed, cursor, `full_sync_required`,
  deletions, media rules for trashed items.
- `backend/specs/015-gallery-member-tags/contracts/gallery-tags-api.md` — the `members` field on Photo.

### Backend facts this feature relies on

- **Album**: `id`, `name`, `parent_id` (null = root), `description`, `event_date`, `cover_url`,
  `cover_source_album_id`, `position`. An album may hold photos and sub-albums at once; no depth limit; empty albums
  exist.
- **Photo**: `id`, `name`, `description`, `album_id`, `album_name`, `image_url`, `thumbnail_url` (null until
  backfilled), `date_taken`, `uploaded_at`, `position`, `members`.
- **Change feed** (member): `albums`, `photos`, `deleted_album_ids`, `deleted_photo_ids`, `cursor`,
  `full_sync_required`. Without a cursor: every live item. With a cursor: items changed since shortly before it, so
  duplicates are possible. A deleted album lists its whole subtree (sub-albums and their photos). A restore brings
  items back as changed. An old, unreadable or future cursor answers `full_sync_required: true` with empty lists. The
  cursor is opaque.
- **Media** requires the member's session. A trashed item's files answer "not found" to members. A photo's file never
  moves and its `image_url` never changes. A replaced cover gets a new URL.
- **Covers**: square, resolved by the server (own or inherited from a descendant). `cover_url` and
  `cover_source_album_id` both null = no cover anywhere below.

## Resolved Decisions

The six open decisions of the request, settled here so planning can start. Each is a default the user may overturn in
`/speckit-clarify`.

| # | Decision | Chosen | Why |
|---|----------|--------|-----|
| 1 | Where the cursor lives | Inside the gallery index, saved in the same write | A crash can never leave the cursor ahead of the data it describes |
| 2 | One or two background jobs | Two: a sync job (any network, also periodic) that queues the existing original-download job (WiFi only) | The two have different network rules; the download job and its rules stay as they are |
| 3 | "Gallery empty" and first-sync display | No separate "empty gallery" trigger: every sync queues the originals missing from disk, which covers the first full download. While the first sync has not answered, the root shows a loading indicator, never the empty state | One rule instead of two; no flash of "nothing here" before the server answers |
| 4 | First open with no index and no network | Error state "Não foi possível carregar a galeria. Verifique sua conexão." with "Tentar novamente" | Nothing to show, and the cause is the connection, not the session |
| 5 | Photo name in the viewer's top bar | `name` without extension, as today | No visible change for members |
| 6 | Sync on screen resume | Only on gallery open; a return to the app from the background is already covered by the foreground trigger | Avoids a sync on every back navigation inside the gallery |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse the album tree as the church organized it (Priority: P1)

A member opens the gallery and sees the top-level albums, each with its cover (or black when there is none) and its
name, in the order the church chose. Opening an album shows its sub-albums first, then its photos, in the chosen
order, with the album's date and description when they exist and the parent album's name as a subtitle. Back goes up
one level at a time.

**Why this priority**: The tree, covers and order are what the backend now offers and what the app cannot show today.
Without it the gallery keeps showing a flat, guessed view that no longer matches the server.

**Independent Test**: With a server holding roots A and B (B before A by `position`), A containing sub-album A1 and
three photos, open the gallery: B then A appear with their covers; open A: A1 on top, then the three photos in
`position` order; open A1, press back twice: A, then the root.

**Acceptance Scenarios**:

1. **Given** root albums with positions 2, 0, 1, **When** the gallery opens, **Then** they appear in position order;
   albums with equal position appear by id.
2. **Given** an album with no cover anywhere below it, **When** it appears in a grid, **Then** its tile is black with
   its name.
3. **Given** an album holding sub-albums and photos, **When** it opens, **Then** sub-albums appear first in a
   2-column grid and photos after them in a 3-column grid, in a single scroll, each group in position-then-id order.
4. **Given** an album with an event date and a description, **When** it opens, **Then** both are shown; when absent,
   nothing takes their place.
5. **Given** a sub-album, **When** it opens, **Then** the title is its name and the subtitle is its parent's name; a
   root album has no subtitle.
6. **Given** an album with neither photos nor sub-albums, **When** it opens, **Then** it shows "Nenhuma foto neste
   álbum."
7. **Given** a member three levels deep, **When** they press back, **Then** they go up exactly one level each time.
8. **Given** a photo tapped in an album, **When** the viewer opens, **Then** it shows that photo and pages through the
   album's photos in the same order as the grid, with zoom, save to device and share.

---

### User Story 2 - The copy on the phone follows the server (Priority: P1)

What the church adds, renames, reorders, moves or deletes in the gallery reaches every member's phone: when the app
opens or returns from the background, when the gallery opens, and every 6 hours in the background.

**Why this priority**: Today a deleted photo never leaves the device and a new photo only arrives on a fresh install
or login. A gallery restricted to members must drop what the church removed.

**Independent Test**: With the app synced, on the server add a photo, rename an album, move a photo to another album,
reorder two albums and delete a photo; bring the app to the foreground: all five changes are visible, the moved
photo's file was not downloaded again, and the deleted photo's file is gone from the device.

**Acceptance Scenarios**:

1. **Given** a synced device, **When** an album or photo is created, renamed, reordered or has its description, date
   or cover changed on the server, **Then** the next sync shows the change.
2. **Given** a synced device, **When** a photo is deleted on the server, **Then** after the next sync it no longer
   appears and its file is no longer on the device.
3. **Given** a synced device, **When** an album is deleted on the server, **Then** after the next sync the album, its
   sub-albums, their photos, their files and their covers are gone from the device.
4. **Given** a deleted album restored on the server, **When** the next sync runs, **Then** it reappears with its
   contents, and missing originals are queued for download.
5. **Given** a photo moved to another album on the server, **When** the next sync runs, **Then** it appears in the new
   album and its file is kept as is, with no new download.
6. **Given** the same change received twice (feed duplicates), **When** it is applied, **Then** the result is the same
   as applying it once.
7. **Given** the server answers that a full sync is required, **When** the sync runs, **Then** the device's copy is
   replaced by the server's current gallery and every local file not in it is deleted.
8. **Given** a sync that brings new photos, **When** it ends, **Then** their originals are queued for download on
   WiFi, with the existing "Usar dados móveis" shortcut, banners and error rules.
9. **Given** an album cover replaced on the server, **When** the next sync runs, **Then** the new cover replaces the
   old one on the device; a cover removed (or whose album was deleted) is deleted from the device.
10. **Given** a member with an active session and the app closed, **When** 6 hours pass, **Then** a background sync
    runs when any network is available.
11. **Given** a signed-out user, **When** the app opens or returns to the foreground, **Then** no sync runs.

---

### User Story 3 - See new photos before their download finishes (Priority: P2)

A photo that is not yet on the device (just added, waiting for WiFi, or part of the first download) appears in the
album right away as a smaller preview, and switches to the full photo once it is downloaded.

**Why this priority**: Without it, new photos synced on mobile data would show as grey squares until WiFi, which
reads as a broken gallery.

**Independent Test**: On mobile data, sync a device after a photo was added on the server: the album shows the photo's
preview; connect to WiFi and let the download finish: the tile now uses the original.

**Acceptance Scenarios**:

1. **Given** a photo whose original is not on the device and which has a preview on the server, **When** its album
   opens online, **Then** the tile shows the preview.
2. **Given** a photo whose original is on the device, **When** its album opens, **Then** the tile uses the original,
   with no network request.
3. **Given** a photo with neither the original on the device nor a loadable preview (none on the server, or offline
   with nothing cached), **When** its album opens, **Then** the tile is a grey placeholder.
4. **Given** previews already seen once, **When** the same album opens offline, **Then** they are shown from the
   device's preview cache.

---

### User Story 4 - Existing installs move to the new layout without re-downloading (Priority: P2)

A member who already has the gallery downloaded updates the app. Their photos stay on the phone, nothing is downloaded
again, and photos the server no longer has are removed.

**Why this priority**: Re-downloading the whole gallery on update would cost every member time and data; keeping
orphans would leave deleted photos on the phone.

**Independent Test**: Install the current version, download the gallery, delete one photo on the server, update to the
new version and open the app online: every other photo is shown without any original being downloaded; the deleted
photo's file is gone.

**Acceptance Scenarios**:

1. **Given** photos downloaded by the previous version, **When** the new version runs for the first time, **Then**
   every photo file is kept and none is downloaded again.
2. **Given** a photo downloaded by the previous version and deleted on the server since, **When** the first full sync
   of the new version completes, **Then** its file is deleted.
3. **Given** the migration already completed, **When** the app runs again, **Then** the migration does not run again.
4. **Given** the migration interrupted halfway (app killed), **When** the app runs again, **Then** it completes with
   the same result as an uninterrupted run.

---

### User Story 5 - A photo removed while it is being viewed (Priority: P3)

A member is looking at a photo when a sync removes it. The viewer moves on instead of showing something that is gone.

**Why this priority**: Rare, but without it the viewer would show a deleted photo or crash on a missing item.

**Independent Test**: Open a photo in the viewer, delete it on the server, trigger a sync: the viewer shows the next
photo and the message "Esta foto foi removida".

**Acceptance Scenarios**:

1. **Given** a photo open in the viewer, **When** a sync deletes it, **Then** the viewer shows the next photo of the
   album (the previous one when it was the last) and the message "Esta foto foi removida".
2. **Given** the only photo of an album open in the viewer, **When** a sync deletes it, **Then** the viewer closes back
   to the album and the message is shown.
3. **Given** an album open, **When** a sync deletes it, **Then** the screen goes up to the nearest album that still
   exists (or the root) and shows "Este álbum foi removido".

---

### User Story 6 - Logout leaves nothing behind (Priority: P1)

When a member signs out, the gallery — its list, its photos, its covers and its previews — leaves the phone, and no
background gallery work keeps running.

**Why this priority**: The gallery is restricted to members. It must not survive the end of the session.

**Independent Test**: Sync and download the gallery, open a few albums online, sign out: the gallery's storage on the
device is empty and no gallery background work is scheduled.

**Acceptance Scenarios**:

1. **Given** a synced gallery, **When** the member signs out, **Then** the index, cursor, originals, covers and preview
   cache are deleted.
2. **Given** a sync or download running, **When** the member signs out, **Then** it is cancelled and nothing it was
   writing survives.
3. **Given** a signed-out user, **When** 6 hours pass, **Then** no background sync runs.

---

### Edge Cases

- **Non-member (403 on the feed)**: the local copy is kept and shown, with the notice "Disponível apenas para
  membros." above the grid; with no local copy, the 403 placeholder without a login button. Same rule as today's 403 on
  the photo list.
- **Session expired (401)**: follows the existing session path (refresh, then "Faça login para acessar a galeria.").
- **Network error during a sync**: the local copy stays as it was, no error takes over a screen that has data; the next
  trigger tries again.
- **First open, no index, no network**: resolved decision 4.
- **Two triggers at once** (app start and gallery open): only one sync runs; the second joins or is skipped.
- **Sign-out during a sync**: a sync that finishes after sign-out writes nothing.
- **A photo's original answers "not found"** (trashed on the server, not yet synced): not counted as downloaded, as
  today; the next sync removes the photo.
- **Cover download fails**: the tile is black until a later sync downloads it.
- **Two albums showing the same inherited cover**: each album's tile shows it; deleting one album does not remove the
  cover the other still uses.
- **A photo moved to another album while open in the viewer**: it leaves the pager like a deleted one, with the message
  "Esta foto foi movida para outro álbum".
- **An album moved under another parent while open**: its screen stays; its subtitle shows the new parent.
- **Photo without an original tapped in the grid**: the viewer includes it, showing the preview (or the grey
  placeholder) with save and share disabled; it switches to the original once downloaded.
- **Offline first run right after the update**: accepted limitation. The migration deletes the old per-photo metadata
  and the index is built by the first full sync, so a member offline right after updating sees the "no connection"
  state until the first connection, although the photos are on the device.

## Requirements *(mandatory)*

### Functional Requirements

**Local copy**

- **FR-001**: The app MUST keep one gallery index on the device holding every live album, every live photo (including
  its `members`, stored but not shown) and the feed cursor, written in a single save.
- **FR-002**: The tree, the order and every lookup MUST be derived from the index, never from disk folders or file
  names.
- **FR-003**: Originals MUST be stored by photo id only, independent of their album, so moving a photo between albums
  never touches its file.
- **FR-004**: An original MUST only count as present once it is fully written (atomic write, as today).
- **FR-005**: Album covers MUST be downloaded to the device during a sync, on any network, so the album grids work
  offline; a new `cover_url` MUST replace the previous file; a cover no album uses any more MUST be deleted.

**Sync**

- **FR-006**: A sync MUST read the change feed with the stored cursor, or without one when none is stored.
- **FR-007**: A sync MUST upsert the albums and photos it receives by id and remove the deleted ids from the index, and
  from the device (originals and covers no longer used).
- **FR-008**: Applying the same feed answer twice MUST give the same result as applying it once.
- **FR-009**: The new cursor MUST be saved in the same write as the index changes it describes.
- **FR-010**: When the feed answers `full_sync_required`, the app MUST read the whole gallery again, replace the index
  with it, and delete every local original and cover not in the result.
- **FR-011**: A sync MUST run on app start and on return to the foreground (only with an active session), on opening
  the gallery, and every 6 hours in the background with an active session, on any network.
- **FR-012**: Only one sync MUST run at a time; concurrent triggers MUST NOT apply the feed twice in parallel.
- **FR-013**: After each successful sync, originals of photos in the index that are not on the device MUST be queued
  for download on WiFi only, keeping the "Usar dados móveis" shortcut and every current download banner, error and
  retry rule.
- **FR-014**: A 403 on the feed MUST keep the local copy untouched; a 401 MUST follow the existing session path;
  any other failure MUST keep the local copy and let the next trigger retry.
- **FR-015**: A sync MUST NOT write anything once the session has ended.

**Migration**

- **FR-016**: On the first run of this version, the app MUST move every existing original into the id-only layout,
  delete the old per-photo metadata files and album folders, and download nothing again.
- **FR-017**: The first full sync after the migration MUST delete every local original whose photo id is not in its
  result.
- **FR-018**: The migration MUST be idempotent, resume correctly after an interruption, and not run again once
  complete.

**Previews**

- **FR-019**: A photo whose original is not on the device MUST be shown by its server preview (`thumbnail_url`),
  loaded with the member's session and kept in a preview cache on the device; with the original on the device, the
  original MUST be used; with neither, a grey placeholder. This is the only case where a screen loads a media URL.

**Screens**

- **FR-020**: The root MUST show the root albums in a 2-column grid with cover (black when none) and name, ordered by
  `position` then `id`, keeping today's banners and placeholders.
- **FR-021**: The album screen MUST show its sub-albums (2-column grid with cover) above its photos (3-column grid) in
  one scroll, each ordered by `position` then `id`; the album name as title, the parent's name as subtitle; the event
  date and description when present; "Nenhuma foto neste álbum." when it holds nothing.
- **FR-022**: Each album opened MUST be its own back stack entry, so back goes up one level.
- **FR-023**: The viewer MUST open by photo id and page through the photos of that album in the grid's order, with the
  photo name (without extension) in the top bar, zoom, save to device and share. Photos whose original is not on the
  device are included, shown by their preview (or grey placeholder), with save and share disabled.
- **FR-024**: A photo deleted while open MUST be replaced by the next one (or previous when last) with the message
  "Esta foto foi removida"; an album deleted while open MUST go up to the nearest existing ancestor (or the root) with
  "Este álbum foi removido".
- **FR-025**: Every gallery screen MUST handle loading, success and error; while the first sync has not answered, the
  root shows a loading indicator, never the empty state.
- **FR-026**: With no index and no network, the root MUST show "Não foi possível carregar a galeria. Verifique sua
  conexão." with "Tentar novamente".

**Session end**

- **FR-027**: Sign-out MUST cancel the sync and download work (periodic included) and delete the index, cursor,
  originals, covers and preview cache.

**Spec**

- **FR-028**: `specs/gallery/spec.md` MUST be rewritten to the state this feature leaves (endpoints, index, storage,
  sync, triggers, previews rule, screens), in the same commit as the code.

**Out of scope**: any write (create, rename, move, order, covers, upload, delete); trash and restore; showing,
adding or filtering member tags, "Minhas fotos", `member_id` on the profile and the tag endpoints; the admin panel's
"Galeria" card (stays disabled); compatibility with app versions released before this feature; push notifications;
any change to the original-download strategy beyond downloading the missing ones on WiFi.

### Key Entities

- **Gallery index**: the device's copy of the gallery — every live album, every live photo and the feed cursor, saved
  as one unit.
- **Album**: a node of the tree — name, parent (none for a root), description, event date, cover, position among its
  siblings. Holds photos and sub-albums.
- **Photo**: an image in exactly one album — name, description, album, original, optional preview, capture and upload
  dates, position in its album, tagged members (stored only).
- **Cursor**: an opaque marker from the server saying where the next sync resumes.
- **Original**: the full photo file on the device, identified by photo id only.
- **Cover**: the album's square image on the device, replaced when the server's cover changes.
- **Preview**: the server's reduced photo, loaded on demand and cached on the device until sign-out.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After a change on the server (add, rename, move, reorder, delete), a member who opens the app online sees
  it in the gallery within 10 seconds of opening, with no manual action.
- **SC-002**: A photo deleted on the server is off every active member's device within 6 hours, even if they never
  open the gallery.
- **SC-003**: Updating the app on a device with the full gallery downloaded (about 200 photos) downloads 0 originals
  again.
- **SC-004**: Moving a photo between albums on the server causes 0 original downloads on any device.
- **SC-005**: The album tree and every album screen open offline, with covers, for any gallery already synced.
- **SC-006**: Albums and photos appear in exactly the order set on the server, at every level, in 100% of cases.
- **SC-007**: After sign-out, the device holds 0 bytes of gallery data and 0 scheduled gallery jobs.
- **SC-008**: With 3,000 photos in the index, opening the gallery or an album shows content in under 1 second on a
  mid-range phone.
- **SC-009**: Every rule listed in the request's test list (delta applied, duplicates, full sync, migration, moved
  photo, tree and order, cover replaced and removed, 403 keeps data, logout clears) is covered by an automated test.

## Assumptions

- The gallery stays small (about 200 photos today, about 3,000 at most in ten years), so the whole index fits in memory
  and a full read of the feed is cheap.
- The sync is small JSON and may use any network, including mobile data; covers are few and small and follow the sync
  onto any network. Only originals are WiFi-first.
- Previews (up to about 1000 px each) load on whatever network is active, including mobile data; only the tiles on
  screen are loaded, and the cache avoids loading them twice.
- The preview cache is bounded in size; it is emptied on sign-out.
- The app has no foreground/background hook in `CoreViewModel` today; one is added for the foreground trigger.
- The feed answer without a cursor is used both for the first sync and for `full_sync_required`; it carries every live
  item and a fresh cursor, equivalent to the backend's advice to read the album and photo lists.
- Event dates are shown as `dd/MM/yyyy`.
- App and backend ship together; the app does not support servers without the change feed.
- Every member sees the whole gallery; there are no per-album permissions.
