# Feature Specification: Gallery Member Tags

**Feature Branch**: `010-gallery-member-tags`

**Created**: 2026-09-29

**Status**: Draft

**Input**: User description: "Add member tags to the app's gallery: every member sees who is in a photo, filters the
gallery by people (AND) and opens \"Minhas fotos\"; users with `manage` on scope `gallery` tag people in one photo or
in many at once. This is the last of the four gallery deliveries." (full request — current state of backend and app,
what to build, specs and tests, out of scope, decisions — in the `/speckit-specify` invocation that created this
directory)

## Context

Since feature 007 every photo in the gallery's local copy already carries the members tagged in it, but the app shows
none of it: nobody can see who is in a photo, find the photos of a person, or tag anyone from the phone. Tagging is
only possible in the Django admin.

This feature shows who is in each photo, lets every member filter the gallery by people and open their own photos, and
lets whoever holds `manage` on the gallery tag people in one photo or in many at once.

Feature 4 of 4 of the app's gallery work: (1) tree and sync — done (007); (2) management — done (008); (3) trash —
done (009); (4) member tags — this one.

### Source of truth on the backend (not redefined here)

- `backend/specs/gallery/spec.md` — sections Tags, Photo resource, Change Feed.
- `backend/specs/015-gallery-member-tags/spec.md` and `contracts/gallery-tags-api.md` — tag writes, picker list,
  refusals, profile `member_id`.
- `backend/specs/012-feature-role-permissions/spec.md` — the picker list needs `manage`, above the gallery's read level.

### Backend facts this feature relies on

- **Photo people**: every photo lists the members tagged in it (id and name), ordered by name, then id; empty when
  untagged. Inactive members are tagged and shown like any other. This is the only member data the gallery returns.
- **Change feed**: a photo comes back as changed when its tags change, or when a member tagged in it is renamed or
  deleted (also through the Django admin), so the local copy stays current by the existing sync.
- **Tag one photo** (`manage`): replaces the photo's people with exactly the given set (empty clears) and answers with
  the photo. Unknown or trashed photo: "not found". Unknown member: "not found" naming the missing members.
- **Tag many photos** (`manage`): adds and removes only the given (photo, person) pairs and keeps every other tag;
  answers with the photos in request order. At most 200 distinct photos per request. Refused as invalid when the photo
  list is empty, when there is nothing to add or remove, or when a person is both added and removed. Atomic: any
  unknown or trashed photo or unknown member fails the whole request with one "not found" naming every missing photo
  and member.
- **People that can be tagged** (`manage`): every member record, active or not, id and name, ordered by name; must not
  be stored (the server marks it no-store). A few hundred records; not paginated, not searchable on the server.
- **Profile link**: the user's profile gains the id of the member record an admin linked to the user, or nothing when
  not linked. Read-only.
- The server also offers a list of tagged people and a server-side person filter; the app does not use them.

## Resolved Decisions

The eight decisions of the request, recommended by the gallery-rework conversation that wrote the request (the user
delegated them to it). Each is a default the user may overturn in `/speckit-clarify`.

| # | Decision | Chosen | Why |
|---|----------|--------|-----|
| 1 | How the people of a photo are shown | A details sheet opened by "ⓘ" in the viewer, not name chips always on screen | Keeps the photo unobstructed; the sheet also holds description and date |
| 2 | Source of the filter's people list | Derived locally from the gallery's copy, not the server's tagged-people list | Works offline and always agrees with the photos on the device |
| 3 | Bulk tagging | Two actions, "Adicionar pessoas" and "Remover pessoas"; no mixed "some photos have this person" state | Maps one-to-one to the server's add/remove request; simple to understand |
| 4 | Picker list storage | Not stored; read again every time the picker opens | The server marks it no-store; a few hundred names load fast |
| 5 | Result layout | One grid in the gallery's tree order, not grouped by album | Simple; the viewer pages straight through the result |
| 6 | Users not linked to a member | "Minhas fotos" is hidden; no "link your profile" prompt | Linking is an admin task outside the app |
| 7 | Search | Case- and accent-insensitive ("joao" finds "João") | Names in Portuguese; typing accents on a phone is slow |
| 8 | Where the user's member id lives | Its own source, separate from the user's permissions | It is not a permission; keeps the access model unchanged |

Decisions taken while writing this spec, consistent with the above and features 007–009:

| # | Decision | Chosen | Why |
|---|----------|--------|-----|
| 9 | "Marcar pessoas" in the sheet vs. the menu | Both open the same picker; the sheet closes while the picker is open | One flow, two entry points |
| 10 | Bulk-tagging partial failure | Chunks already applied stay applied; the message counts the photos updated and gives the first failure's reason; selection stays when anything failed | Same rule as batch moves and deletes in 008 (section 8.6) |
| 11 | "Remover pessoas" with no one tagged in the selection | The choice is shown disabled | Nothing to remove; avoids an empty picker |
| 12 | Where "Minhas fotos" sits on the gallery root | An entry above the album list, like the root's other entries; hidden while "Organizar" is open | Visible without a menu; out of the way while reordering |
| 13 | Filter selection lifetime | Kept while the gallery is open (back from the viewer returns to the same selection); cleared when the filter screen is left | No persistence needed; predictable |
| 14 | Picker order | The photo's current people first (in the photo's order), then everyone else by name; search filters both parts | "Listed first" from the request; stable while checking and unchecking |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See who is in a photo (Priority: P1)

Any member viewing a photo taps "ⓘ" in the top bar and sees the photo's description, the day it was taken and "Nesta
foto" with the names of the people tagged in it.

**Why this priority**: The tags already exist on the device; showing them is the base every other story builds on.

**Independent Test**: Open a tagged and an untagged photo, open the sheet on each and check the texts; swipe to the
next photo with the sheet open and check it follows.

**Acceptance Scenarios**:

1. **Given** the viewer is open, **Then** the top bar shows "ⓘ" for every user.
2. **Given** a photo tagged with people, **When** "ⓘ" is tapped, **Then** a sheet shows the description (only when
   not empty), "Tirada em {dd/MM/yyyy}" (only when the date is known) and "Nesta foto" followed by the names in the
   photo's order.
3. **Given** an untagged photo, **Then** "Nesta foto" is followed by "Ninguém marcado".
4. **Given** the sheet is open, **When** the user swipes to another photo, **Then** the sheet shows that photo.
5. **Given** the sheet is open, **When** a sync changes the photo's people (tagged elsewhere, a member renamed or
   deleted), **Then** the sheet updates without closing.
6. **Given** a name in the sheet, **When** tapped, **Then** nothing happens.

---

### User Story 2 - Tag people in one photo (Priority: P1)

A user with `manage` on the gallery taps "Marcar pessoas" (in the viewer's menu or inside the details sheet), checks
and unchecks people in a searchable list and saves.

**Why this priority**: Without writes from the app the tags only come from the Django admin.

**Independent Test**: Tag two people in an untagged photo, then remove one; check the sheet after each save and that a
save without changes sends nothing.

**Acceptance Scenarios**:

1. **Given** a user with `manage`, **Then** "Marcar pessoas" shows in the viewer's menu and as a button in the
   details sheet; without `manage` neither shows, and both follow level changes live.
2. **When** the picker opens, **Then** it reads the people list from the server, showing a loading state, and on
   failure an error with "Tentar novamente" (offline: "Sem conexão").
3. **Given** the list is loaded, **Then** the photo's current people are checked and listed first; everyone else
   follows by name.
4. **When** the user types in the search field, **Then** the list keeps the names containing the text, ignoring case
   and accents.
5. **When** "Salvar" is tapped with a set different from the photo's current people, **Then** one request sends the
   full set, the picker closes, the photo is updated on the device at once, a sync runs and "Marcações salvas" shows.
6. **When** "Salvar" is tapped with the same set, **Then** nothing is sent and the picker closes.
7. **Given** a save is running, **Then** "Salvar" shows progress and cannot be tapped again.

---

### User Story 3 - Filter the gallery by people (Priority: P1)

Any member taps "Pessoas" on the gallery root, picks one or more people and sees every photo in which all of them are
tagged, then opens one and swipes through the result.

**Why this priority**: The main reason members want tags: finding photos of someone.

**Independent Test**: With photos tagging A, B and A+B, filter by A, then by A and B; check the grids, the hint and the
viewer paging.

**Acceptance Scenarios**:

1. **Given** the gallery root, **Then** the top bar shows "Pessoas" for every user (hidden while "Organizar" is open).
2. **When** "Pessoas" is tapped, **Then** a screen lists every person tagged in at least one photo on the device, with
   "{n} fotos" ("1 foto"), ordered by name, with a search field (case- and accent-insensitive). It works offline.
3. **When** the user checks people, **Then** the result below updates at once: the photos in which every checked
   person is tagged, as one 3-column grid in the gallery's order (albums in tree order, photos in album order).
4. **Given** two or more people are checked, **Then** the hint "Fotos com todas as pessoas selecionadas" shows above
   the grid.
5. **Given** no photo has all the checked people, **Then** "Nenhuma foto com todas essas pessoas." shows.
6. **Given** nobody is tagged in any photo, **Then** the screen says "Ninguém foi marcado nas fotos ainda.".
7. **When** a result is tapped, **Then** the viewer opens on it and swipes through the result, not the album.
8. **Given** the viewer opened from the result, **When** the photo leaves the result (deleted, moved to the trash, or
   one of the people untagged), **Then** it is handled as a removed photo, as in the album viewer.
9. **Given** the viewer opened from the result, **Then** the details sheet and management menu work as in the album
   viewer.

---

### User Story 4 - Minhas fotos (Priority: P2)

A member whose profile is linked to a member record taps "Minhas fotos" on the gallery root and sees every photo they
are tagged in.

**Why this priority**: The most common filter in one tap, but only for linked users.

**Independent Test**: With a linked and an unlinked user, open the gallery root; check the entry shows only for the
linked one and its grid holds that member's photos.

**Acceptance Scenarios**:

1. **Given** the user's profile is linked to a member, **Then** "Minhas fotos" shows on the gallery root.
2. **Given** the profile is not linked, **Then** it does not show; there is no prompt to link.
3. **Given** the gallery is open, **When** the profile is read again and the link changes, **Then** the entry
   appears or disappears without reopening the screen.
4. **When** "Minhas fotos" is tapped, **Then** a screen titled "Minhas fotos" shows the member's photos as the same
   grid, and opening one pages through them.
5. **Given** the member is in no photo, **Then** it says "Você ainda não foi marcado em nenhuma foto.".

---

### User Story 5 - Tag people in many photos (Priority: P2)

A user with `manage` selects photos in an album, taps "Pessoas" in the selection bar and adds or removes people in all
of them at once.

**Why this priority**: Event albums have dozens of photos with the same people; one at a time is too slow.

**Independent Test**: Select 3 photos, add a person, check every photo; remove them again; select more than 200 and
check the requests are split.

**Acceptance Scenarios**:

1. **Given** selection mode in an album and `manage`, **Then** the selection bar shows "Pessoas" with two choices:
   "Adicionar pessoas" and "Remover pessoas".
2. **When** "Adicionar pessoas" is chosen, **Then** the picker opens with nobody checked; confirming adds the checked
   people to every selected photo, keeping their other tags.
3. **When** "Remover pessoas" is chosen, **Then** the picker lists only the people tagged in at least one selected
   photo, read from the device (no connection needed for the list); confirming removes them from every selected photo.
4. **Given** no selected photo has anyone tagged, **Then** "Remover pessoas" is disabled.
5. **Given** nobody is checked, **Then** the confirm button is disabled.
6. **Given** more than 200 photos are selected, **Then** they are sent in requests of at most 200, one after another,
   all attempted.
7. **Given** every request succeeded, **Then** the photos are updated on the device, one sync runs at the end,
   selection mode ends and "Marcações atualizadas em {n} fotos" ("1 foto") shows.
8. **Given** some request failed, **Then** "Marcações atualizadas em {n} de {total} fotos: {motivo}" shows, the
   photos of successful requests stay updated and selection mode stays.

---

### Edge Cases

- A tag write refused because a photo or a person no longer exists (deleted meanwhile, on the server or through the
  Django admin): nothing changed on the server; the gallery syncs, an open picker reloads its list, and "Algumas fotos
  ou pessoas não existem mais. Confira e tente de novo." shows. This does not depend on reading which ids were missing.
- Any other refusal (invalid request, server error): the server's message, or the category's generic text; offline:
  "Sem conexão".
- The user loses `manage` while tagging: the refusal is handled globally (profile read again); the controls and an
  open picker go away as the level changes.
- A member renamed or deleted in the Django admin: the next sync updates every photo they were in; the sheet, the
  filter list and the results follow.
- A checked person in the filter disappears from every photo (untagged or deleted): they leave the list and the
  selection; the result is recomputed with the remaining people.
- The photo shown in the viewer is removed by a sync while the picker is open: the picker closes with the viewer's
  removed-photo handling.
- A "Remover pessoas" chunk where a person is tagged in none of that chunk's photos: sent anyway; the server ignores
  pairs that do not exist.
- Picker search with no match: "Nenhuma pessoa encontrada.".
- The server has no member records: the picker says "Nenhuma pessoa cadastrada.".
- The user leaves the viewer while a save runs: the app stops waiting; if the server saved, the next sync brings it.
- Sign-out: the tags live in the gallery's local copy, which sign-out already deletes; the picker list is never
  stored.

## Requirements *(mandatory)*

### Functional Requirements

**Profile link**

- **FR-001**: The app MUST read the member id linked to the user's profile; an absent or empty value MUST mean "not
  linked", including for a profile stored before this feature.
- **FR-002**: The member id MUST be exposed to the gallery through its own source, separate from the user's
  permissions, and follow profile re-reads live.

**Details sheet**

- **FR-003**: The viewer MUST offer "ⓘ" to every user, opening a sheet with the description (when not empty), "Tirada
  em {dd/MM/yyyy}" (when known) and "Nesta foto" with the names in the photo's order, or "Ninguém marcado".
- **FR-004**: The sheet MUST follow the page being shown and the gallery's local copy, updating live.
- **FR-005**: Names in the sheet MUST be plain text.

**Picker**

- **FR-006**: The picker MUST read the people list from the server every time it opens and MUST NOT store it on disk;
  it MUST show loading, error with "Tentar novamente", empty, and the list.
- **FR-007**: The picker MUST support multi-select and a local search that ignores case and accents.

**Tag one photo**

- **FR-008**: "Marcar pessoas" MUST show in the viewer's menu and in the details sheet only while the user has
  `manage`, following level changes live.
- **FR-009**: The picker MUST open with the photo's people checked and listed first; "Salvar" MUST send the full set
  in one request, or nothing when the set is unchanged.
- **FR-010**: On success the returned photo MUST be applied to the local copy at once without moving the sync cursor,
  a sync MUST run afterwards, and "Marcações salvas" MUST show.

**Tag many photos**

- **FR-011**: The album selection bar MUST offer "Pessoas" with "Adicionar pessoas" and "Remover pessoas" while the
  user has `manage`.
- **FR-012**: "Adicionar pessoas" MUST open the picker with nobody checked; "Remover pessoas" MUST list only the
  people tagged in at least one selected photo, from the local copy, and MUST be disabled when there are none.
- **FR-013**: The request MUST be split into chunks of at most 200 photos, sent one after another, all attempted.
- **FR-014**: Returned photos MUST be applied to the local copy as each chunk succeeds, without moving the cursor; one
  sync MUST run at the end.
- **FR-015**: One final message MUST show: "Marcações atualizadas em {n} fotos" on full success (selection mode ends),
  or "Marcações atualizadas em {n} de {total} fotos: {motivo}" otherwise (selection mode stays).

**Tag-write errors**

- **FR-016**: A "not found" on a tag write MUST sync the gallery, reload an open picker and show "Algumas fotos ou
  pessoas não existem mais. Confira e tente de novo.", without depending on the missing-id lists.
- **FR-017**: Any other failure MUST show the server's message or the category's generic text ("Sem conexão"
  offline); "forbidden" follows the app's global handling.

**Filter**

- **FR-018**: The gallery root MUST show "Pessoas" to every user (not while "Organizar" is open), opening the people
  filter screen.
- **FR-019**: The filter's people list MUST be derived from the local copy: every person tagged in at least one photo,
  with the photo count, ordered by name (then id), with case- and accent-insensitive search; it MUST work offline.
- **FR-020**: The result MUST be the photos tagged with every selected person, as one 3-column grid in tree order
  (albums pre-order; photos by position, then id), updated live as the selection or the local copy changes, with the
  hint and empty texts of User Story 3.
- **FR-021**: The app MUST NOT call the server's tagged-people list or its person filter.

**Viewer from a result**

- **FR-022**: Opening a result MUST open the viewer paging through that result; the details sheet, management menu and
  removed-photo handling MUST work as in the album viewer, and a photo leaving the result MUST count as removed.

**Minhas fotos**

- **FR-023**: The gallery root MUST show "Minhas fotos" only while the user's member id is known, following changes
  live; it MUST open the result grid for that member, titled "Minhas fotos", with the empty text "Você ainda não foi
  marcado em nenhuma foto.".

**Sign-out**

- **FR-024**: Nothing new MUST be written to disk by this feature.

### Key Entities

- **Photo person**: a member tagged in a photo — id and name, in the photo's order.
- **Taggable person**: a member record that can be tagged — id and name; read on demand, never stored.
- **Tagged person summary**: a person tagged in at least one local photo, with the number of photos; derived locally.
- **People filter**: a set of member ids; its result is the local photos tagged with all of them, in tree order.
- **Own member id**: the member record linked to the user's profile, or none.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A member finds every photo of a given person in at most 3 taps from the gallery root ("Pessoas", the
  name, a result), offline included.
- **SC-002**: A linked member opens their own photos in 1 tap from the gallery root.
- **SC-003**: A manager tags the same people in a whole event album (up to several hundred photos) in one flow:
  select, "Pessoas", pick, confirm.
- **SC-004**: After a successful save, the change is visible on the device before the user leaves the screen, without
  waiting for a sync.
- **SC-005**: Users without `manage` never see "Marcar pessoas" or the selection bar's "Pessoas".
- **SC-006**: Every behaviour in the request's test list is covered by an automated unit test.

## Assumptions

- The server's rules (who may tag, atomicity, the 200 limit, change-feed coverage) are final; the app is a UI filter
  and never recomputes tags beyond applying the server's answers.
- The gallery's local copy already stores each photo's people (feature 007); only showing and writing them is new.
- "Sem conexão" and the generic texts per category come from the gallery's existing write error handling.
- The existing handling of "forbidden" (profile re-read, controls follow the new level) applies to tag writes and the
  picker as to any gallery write.
- Out of scope: a member removing their own tag or asking to be untagged; an OR filter; tagging people without a
  member record or free-text tags; face detection or suggestions; linking a profile to a member from the app; using
  the server's tagged-people list or person filter; tapping a name to open that person's photos; any backend change.
