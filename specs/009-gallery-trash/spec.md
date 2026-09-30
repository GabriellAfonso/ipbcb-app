# Feature Specification: Gallery Trash

**Feature Branch**: `009-gallery-trash`

**Created**: 2026-09-29

**Status**: Draft

**Input**: User description: "Add the gallery trash to the app: users with `owner` on scope `gallery` see what was
deleted in the last 30 days, who deleted it and when it will be purged, and restore it. Member tags are feature 4."
(full request — current state of backend and app, what to build, specs and tests, out of scope, open decisions — in
the `/speckit-specify` invocation that created this directory)

## Context

Since feature 008, whoever holds `owner` on the gallery can delete albums and photos from the app. A delete sends the
item to the server's trash for 30 days, and the confirmation already says so ("Fica 30 dias na lixeira"), but the app
offers no way to see the trash or bring anything back: a mistaken delete can only be undone with another tool.

This feature adds the trash screen for owners and a short "Desfazer" right after a single delete.

Feature 3 of 4 of the app's gallery work: (1) tree and sync — done (007); (2) management — done (008); (3) trash —
this one; (4) member tags.

### Source of truth on the backend (not redefined here)

- `backend/specs/gallery/spec.md` — sections Trash and Purge.
- `backend/specs/014-gallery-trash-sync/spec.md` and `contracts/gallery-trash-api.md` — trash listing, restore,
  refusals, media rule.

### Backend facts this feature relies on

- **Trash listing** (owner only): one entry per delete action (a "batch"), most recent first. Each entry has its kind
  (album or photo), id, name, when it was deleted and by whom, who uploaded it (photos only, when known), the day it
  will be purged (deleted + 30 days), how many sub-albums and photos went with it (albums; 0 for photos) and a preview
  (the photo's thumbnail or the album's own cover; none when the album's cover was inherited or absent). "By whom" and
  "uploaded by" are display names or empty when the user was removed. Not paginated; an empty trash is an empty list.
- **Items that went with an album** are not listed on their own and cannot be restored alone: they come back only
  with their album.
- **Restore** (owner only) of an album brings back exactly its batch — the sub-albums and photos deleted with it, at
  their old positions — and answers with the album; restore of a photo answers with the photo.
- **Restore refusals**, with a Portuguese message meant for the user:
  - *not in the trash any more* (restored by someone else, purged, or not the root of its batch);
  - *parent in the trash*: the album's parent (or the photo's album) is itself in the trash, named by id; restore
    that one first;
  - *name conflict*: a live sibling album holds the album's name, named by id; rename it first. Nothing is ever
    renamed or moved automatically.
- **No permanent delete from the app**: the daily purge is the only permanent delete.
- **Media**: preview files of trashed items are readable by owners only.
- **Every restore reaches every device through the change feed**: restored items come back as changed and leave the
  deleted lists.

## Resolved Decisions

The five open decisions of the request, answered by the gallery-rework conversation that wrote the request (the user
delegated them to it). Each is a default the user may overturn in `/speckit-clarify`.

| # | Decision | Chosen | Why |
|---|----------|--------|-----|
| 1 | Undo right after a delete | After deleting **one** album or **one** photo, the confirmation becomes a snackbar with "Desfazer" that restores it; not offered after a multi-photo delete from the selection (each photo is its own batch: N restores that could fail partway — the trash covers it). Offered only while the user still has `owner` | Covers the most common mistake without opening the trash |
| 2 | Previews in the trash | The gallery's existing preview loader and its disk cache | Owners can read these files anyway; logout already clears the cache |
| 3 | Full-size view of a trashed photo | No — preview only | Keeps the screen small; after restoring, the photo is seen in the gallery |
| 4 | How "deleted at" is shown | Absolute, `dd/MM/yyyy HH:mm` in the device's local time; purge day `dd/MM/yyyy` | Precise and unambiguous next to the purge date |
| 5 | Count badge on the trash icon | None | Would need a trash read on every gallery open; the screen is rarely needed |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See what is in the trash (Priority: P1)

An owner opens the gallery, taps the trash icon in the top bar and sees every item deleted in the last 30 days, most
recent first: its preview, name, whether it is an album or a photo, who deleted it and when, what went with an album,
who uploaded a photo, and the day it will disappear for good.

**Why this priority**: Without the list there is nothing to restore; it also answers "who deleted this?".

**Independent Test**: With a trash holding an album (with sub-albums and photos) and a photo, open the trash as an
owner and check every row's texts; open the gallery as a non-owner and check there is no icon.

**Acceptance Scenarios**:

1. **Given** a user with `owner` on the gallery, **When** the gallery root opens, **Then** a trash icon shows in the
   top bar.
2. **Given** a user with `manage` only, or no level, **When** the gallery root opens, **Then** no trash icon shows.
3. **Given** the trash icon is shown, **When** the user loses `owner` while the gallery is open, **Then** the icon
   disappears without reopening the screen.
4. **Given** the trash holds entries, **When** the trash screen opens, **Then** it shows one row per entry in the
   server's order, each with: preview (grey when there is none or it fails to load), name, kind ("Álbum" / "Foto"),
   "Apagado por {nome} em {dd/MM/yyyy HH:mm}" ("Apagado por usuário desconhecido em …" when the name is empty), for
   albums "{n} subálbuns · {m} fotos" (singular "1 subálbum", "1 foto"; a zero part omitted; nothing when both are
   zero), for photos "Enviada por {nome}" when known, and "Some em {dd/MM/yyyy}".
5. **Given** the trash screen, **Then** a short explanation sits above the list: items stay 30 days and are then
   deleted for good; restoring an album brings back what was deleted with it.
6. **Given** the trash is empty, **When** the screen opens, **Then** it says "A lixeira está vazia.".
7. **Given** the list is shown, **When** the user pulls down, **Then** it is read again from the server.

---

### User Story 2 - Restore an item (Priority: P1)

The owner taps "Restaurar" on a row. The row shows progress, then leaves the list; the album or photo is back in the
gallery at once, and an album's sub-albums and photos follow within moments.

**Why this priority**: The whole point of the trash.

**Independent Test**: Restore a photo and an album with content from the trash; check both reappear in the gallery
tree at their old place, the rows leave the list and the messages show.

**Acceptance Scenarios**:

1. **Given** a row, **When** the user taps "Restaurar", **Then** no confirmation is asked and that row shows progress
   while the request runs; the other rows' "Restaurar" are disabled until it ends (one restore at a time).
2. **Given** the restore succeeds, **Then** the row leaves the list, "Álbum restaurado" / "Foto restaurada" shows,
   the restored item appears in the gallery immediately, and a sync brings back what went with it.
3. **Given** a restored photo whose original is not on the device, **Then** it appears by its preview and its original
   is queued for download on WiFi, like any new photo.
4. **Given** the restore is refused because the item is no longer in the trash, **Then** the list is read again and
   "Este item não está mais na lixeira." shows.
5. **Given** the restore is refused for any other reason not covered by User Story 3, **Then** the server's message
   shows and the list is unchanged.
6. **Given** the device is offline, **When** the user taps "Restaurar", **Then** "Sem conexão" shows and the list is
   unchanged.

---

### User Story 3 - Resolve a refused restore (Priority: P2)

A restore can be refused because the item's parent album is itself in the trash, or because a live album already
uses the name. The screen tells the owner what to do and takes them there.

**Why this priority**: Both refusals are expected in normal use (delete a sub-album, then its parent; create a new
album with the name of a deleted one), and without guidance the owner is stuck.

**Independent Test**: Force each refusal on a test server; check the message, the highlighted parent row and the
"Abrir álbum" navigation.

**Acceptance Scenarios**:

1. **Given** a restore refused because the parent is in the trash, and the parent is a row in the list, **Then** the
   server's message shows, the list scrolls to the parent row and highlights it; its own "Restaurar" works as usual.
2. **Given** the same refusal and the parent is **not** a row (it went with a bigger batch), **Then** only the
   server's message shows.
3. **Given** a restore refused because a live sibling album holds the name, **Then** the server's message shows with
   an "Abrir álbum" button that opens the conflicting album in the gallery, where the owner renames it with the
   existing "Editar álbum".
4. **Given** the owner renamed the conflicting album and came back, **When** they tap "Restaurar" again, **Then** the
   restore is sent again.
5. **Given** the conflicting album is not yet on the device (the local copy is behind), **Then** the gallery is
   synced before the message shows; if the album is still not on the device, only the server's message shows,
   without "Abrir álbum".

---

### User Story 4 - Undo a delete (Priority: P2)

Right after deleting one album or one photo, the owner sees "Álbum enviado para a lixeira" / "Foto enviada para a
lixeira" with a "Desfazer" button that brings it back without opening the trash.

**Why this priority**: Most mistaken deletes are noticed immediately.

**Independent Test**: Delete one photo, tap "Desfazer"; check it is back in the album. Delete several photos from the
selection; check no "Desfazer" shows.

**Acceptance Scenarios**:

1. **Given** the owner deletes one album (album menu) or one photo (viewer menu, or a selection of exactly one photo),
   **Then** the confirmation shows as a snackbar with "Desfazer" on the screen now on top.
2. **Given** the snackbar, **When** "Desfazer" is tapped, **Then** the item is restored exactly as from the trash
   ("Álbum restaurado" / "Foto restaurada"), and a refusal is reported with the same texts as in the trash.
3. **Given** a delete of two or more photos from the selection, **Then** the result message shows without "Desfazer".
4. **Given** the user lost `owner` before tapping, **Then** "Desfazer" is not offered.

---

### Edge Cases

- The trash is read while offline: the error state says the trash needs a connection ("Sem conexão. A lixeira precisa
  de internet.") with "Tentar novamente".
- Any other read failure: the category's generic text with "Tentar novamente".
- The owner loses `owner` while on the trash screen: the screen closes and "Você não tem mais acesso à lixeira." shows.
- A read refused with "forbidden" (level lost on the server first): same as above once the profile is re-read; until
  then the server's message shows as the error state.
- Two owners restore the same item: the second gets "not in the trash any more", handled as User Story 2 scenario 4.
- An album restored while one of its photos had been trashed on its own earlier: that photo stays in the trash as its
  own row (backend rule); nothing special in the app.
- The restored item was already brought back by a sync before the response arrived: applying it again changes
  nothing.
- The user leaves the trash screen while a restore runs: the app stops waiting for the answer; if the server
  restored the item, the next sync brings it back.
- Pull-to-refresh while a restore runs: disabled until it ends, so the row being restored never disappears under the
  request.
- A highlighted parent row stays highlighted until the next restore, refresh or leaving the screen.
- "Desfazer" tapped after the snackbar's item was already restored from another device: "Este item não está mais na
  lixeira.".
- Sign-out while on the trash screen: the gallery graph leaves as today; nothing is cached, so nothing to clear.

## Requirements *(mandatory)*

### Functional Requirements

**Entry point**

- **FR-001**: The gallery root MUST show a trash icon in the top bar only while the user has `owner` on the gallery,
  following level changes live; it MUST NOT show while "Organizar" is open.
- **FR-002**: The trash icon MUST open the trash screen, which is part of the gallery's navigation (back returns to
  the gallery root).
- **FR-003**: The trash screen MUST close with "Você não tem mais acesso à lixeira." when the user loses `owner`.

**Trash list**

- **FR-004**: The trash screen MUST read the trash from the server each time it opens and on pull-to-refresh; it MUST
  NOT cache the list or work offline.
- **FR-005**: The screen MUST show a loading state before the first answer, an error state with "Tentar novamente"
  (offline: "Sem conexão. A lixeira precisa de internet."), an empty state "A lixeira está vazia." and the list.
- **FR-006**: The list MUST keep the server's order and show one row per entry with the texts of User Story 1,
  scenario 4. Dates MUST be shown in the device's local time; the purge day as given by the server.
- **FR-007**: A row's preview MUST load through the gallery's authenticated preview loader; no preview or a failed
  load MUST show grey. A row MUST NOT open a full-size view.
- **FR-008**: The screen MUST show above the list: "Os itens ficam 30 dias na lixeira e depois são apagados para
  sempre. Restaurar um álbum traz de volta tudo o que foi apagado com ele."

**Restore**

- **FR-009**: Every row MUST offer "Restaurar", without confirmation. While a restore runs, its row MUST show
  progress and every "Restaurar" and the pull-to-refresh MUST be disabled.
- **FR-010**: On success the row MUST leave the list, the returned album or photo MUST be applied to the gallery's
  local copy at once without moving the sync cursor, a sync MUST run afterwards (queuing missing originals on WiFi as
  any sync does), and "Álbum restaurado" / "Foto restaurada" MUST show.
- **FR-011**: "Not in the trash any more" MUST reload the list and show "Este item não está mais na lixeira.".
- **FR-012**: "Parent in the trash" MUST show the server's message; when the parent album is a row of the list, the
  list MUST scroll to it and highlight it until the next restore, refresh or leaving the screen.
- **FR-013**: "Name conflict" MUST show the server's message with "Abrir álbum", which opens the conflicting album.
  When that album is not on the device, the gallery MUST be synced first; if it is still missing, only the message
  shows.
- **FR-014**: Offline MUST show "Sem conexão"; any other refusal MUST show the server's message (or the category's
  generic text); in both cases the list MUST stay unchanged.
- **FR-015**: A restore's error texts MUST be the same whether it started in the trash or from "Desfazer".

**Undo**

- **FR-016**: After a successful delete of exactly one album or one photo, the "sent to the trash" message MUST show
  as a snackbar with "Desfazer", on whichever gallery screen is on top, while the user still has `owner`.
- **FR-017**: "Desfazer" MUST restore that item with the behaviour of FR-010 to FR-015 (no list to reload or
  highlight; a name conflict offers "Abrir álbum").
- **FR-018**: Deletes of two or more photos MUST NOT offer "Desfazer".
- **FR-019**: Every other gallery message keeps its current behaviour (shown once by the screen on top); the gallery's
  messages move from a toast to a snackbar so the "Desfazer" action can be offered.

### Key Entities

- **Trash entry**: one delete action waiting in the trash — kind (album / photo), id, name, deleted at, deleted by
  (optional), uploaded by (optional, photos), purge day, sub-album count, photo count, preview (optional).
- **Restore refusal**: why a restore did not happen — not in the trash, parent in the trash (with the parent album's
  id), name conflict (with the conflicting album's id), offline, other (with a message).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An owner restores a mistakenly deleted item in at most 2 taps from the gallery root (trash icon,
  "Restaurar"), or 1 tap ("Desfazer") right after the delete.
- **SC-002**: After a successful restore, the item is visible in the gallery before the user leaves the trash screen,
  without waiting for a sync.
- **SC-003**: 100% of the refusals the server defines lead to a message the user can act on; none leaves a row
  spinning or the list in a state that differs from the server's after a refresh.
- **SC-004**: Users without `owner` never see the trash icon, the trash screen or "Desfazer".
- **SC-005**: Every behaviour in the request's test list is covered by an automated unit test.

## Assumptions

- The server's rules (who may read and restore, batch contents, refusals, purge) are final and not repeated in the
  app; the app is a UI filter only.
- "Sem conexão" comes from the gallery's existing write error handling; the generic texts per category come from the
  app's shared error texts.
- A trash of a church gallery is small (tens of entries); no paging is needed.
- The existing handling of "forbidden" (profile re-read, controls follow the new level) applies to the trash as to any
  gallery write.
- Out of scope: deleting forever or emptying the trash; restoring on its own an item that went with an album;
  offline trash or caching its list; member tags, filter and "Minhas fotos" (feature 4); any backend change.
