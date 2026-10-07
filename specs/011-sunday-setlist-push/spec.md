# Feature Specification: Sunday Setlist — Draft, Save, Push and Play Confirmation

**Feature Branch**: `011-sunday-setlist-push`

**Created**: 2026-10-01

**Status**: Draft

**Input**: User description: "Sugestões de música: rascunho persistente, salvar e distribuir o repertório do domingo
para o ministério de Louvor, e lembrete para confirmar as músicas tocadas." (full request — current app state, the five
deliveries, states rule and plan notes — in the `/speckit-specify` invocation that created this directory)

## Context

Today the Repertório tab of the worship hub builds a Sunday repertoire of four rows (song, key, pinned) from the song
suggestions. The rows live only in memory: closing the app loses them. The only way out is "Compartilhar", which sends
the list as text through another app. Nothing reaches the server, so the band learns the setlist by word of mouth, and
nothing reminds anyone to register the played songs after the service.

The backend now stores one setlist per Sunday, pushes it to every worship ministry member and reminds the people
responsible to confirm the played songs on Sunday night. This feature makes the app use it: the repertoire draft
survives closing the app, whoever can save sends it to the server, the band sees it at the top of Letras and Cifras
even offline, and the Sunday-night reminder and an admin card lead straight to a pre-filled "Registrar domingo".

### Source of truth on the backend (not redefined here)

- `backend/specs/017-sunday-setlist-push/spec.md` — setlist, worship membership, reminder windows, recipients.
- `backend/specs/017-sunday-setlist-push/contracts/setlist-api.md` — save, read by date, current, pending confirmation,
  and every refusal.
- `backend/specs/017-sunday-setlist-push/contracts/push-messages.md` — message types and payload.
- `backend/specs/017-sunday-setlist-push/contracts/me-api.md` — device token register/unregister and the profile flags
  `is_worship_member` and `can_save_setlist`.

### Backend facts this feature relies on

- **Setlist**: one per Sunday; items carry position, song, title, artist and key, ordered by position. Saving the same
  date again replaces it entirely. A save answers with the setlist as stored.
- **Who can do what**: the profile says whether the user is a worship member and whether they can save a setlist.
  Reading the current setlist needs worship membership. Reading a setlist by date and the pending-confirmation list
  need `manage` on scope `songs` — the same level that opens "Gestão do Louvor".
- **Current setlist**: the next setlist dated today or later; the server answers "none" explicitly, not as an error.
- **Pending confirmation**: setlists dated today or earlier with no played songs registered, newest first. Registering
  the played songs for a date removes it, and so does deleting its setlist.
- **Delete**: a setlist can be deleted by date, with the same permission as saving (`manage` on `songs` and worship
  member). Its items go with it; played songs already registered do not. No push follows: the band's devices drop
  their stored copy the next time they read the current setlist and get "none". A date with no setlist is "not found".
- **Push**: data-only messages carrying a type (`setlist_saved` or `confirm_plays`) and a date, no other data.
  Delivery is best effort; the server expects the app to re-read the current setlist on start and resume.
- **Device token**: registered after login and on rotation, unregistered on logout while the session is still valid;
  unregister always succeeds for a well-formed request.

### Relation to the app's domain specs

- `specs/worshiphub/spec.md` §2.4 (Repertório), §4.1 and §5.1 (Cifras and Letras lists) — gain the draft, the save and
  the "Repertório de domingo" section.
- `specs/admin/spec.md` §2 (panel cards) and §3 (Registro de louvor) — gain the pending card and the pre-filled entry.
- `specs/core/spec.md` §6.2 (`CoreViewModel` boot and logout), §4.2.1 (access from the profile) and the profile
  contract — gain the push token lifecycle, the two profile flags and the notification entry point.

## Resolved Decisions

Defaults taken while writing this spec, consistent with the request and features 007–010. Each may be overturned in
`/speckit-clarify`.

| # | Decision | Chosen | Why |
|---|----------|--------|-----|
| 1 | When the 1-hour draft expiry is checked | When the draft is restored (opening the tab or the app); an open screen is never emptied under the user | No rows vanish while someone is looking at them |
| 2 | What counts as an edit for the expiry | Any change to the rows: picking a song, changing a key, pinning/unpinning, "Gerar" | All are deliberate work on the draft |
| 3 | Number of rows | Stays four | The request does not change it; the server allows up to ten |
| 4 | How the save date is shown | A confirmation before sending: "Salvar repertório de domingo dd/MM?" | The user sees the date before it replaces anything on the server |
| 5 | Draft on logout | Kept; it is device-local song choices, not account data, and expires in one hour anyway | Matches "só local e por aparelho" |
| 6 | Stored "Repertório de domingo" on logout or loss of worship membership | Cleared on logout and whenever the profile says the user is not a worship member | It came from an access-restricted read |
| 7 | Push received while logged out or not a worship member | Ignored silently (no fetch, no notification) | A session ended by a failed refresh never unregisters the token; the device must not act on it |
| 8 | `setlist_saved` notification while the app is in the foreground | Not shown; the section just refreshes | The author, who usually has the app open, is not notified of their own save |
| 9 | A song in the "Repertório de domingo" section that is also pinned manually | Shown only in the "Repertório de domingo" section; the manual pinned list and the rest skip it | No duplicate lines in the same list |
| 10 | Tapping a song in the "Repertório de domingo" section | Opens the same lyrics or chord chart detail as anywhere else in the list | Nothing new to learn |
| 11 | Notification channel | One new channel, "Repertório", for both messages | User can mute it without muting birthdays or uploads |
| 12 | Notification permission | Reuse the existing request on launch (Android 13+); no extra prompt | The app already asks once at start |
| 13 | Pre-filled register screen | Rows are the setlist's songs and keys in position order, editable before sending; the date is fixed to the setlist's Sunday | What was played may differ from what was planned |
| 14 | Who sees the delete button on the pending card | Only users the profile reports as able to save a setlist; the others still see the dates | The server refuses the delete without worship membership, so a button that always fails is not offered |
| 15 | Deleting a setlist that is already gone (`404`) | Treated as done: the date leaves the card, no error | The outcome the user wanted is already true |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The repertoire draft survives closing the app (Priority: P1)

A worship leader picks the four songs and keys (by hand or with "Gerar"), leaves the app to check something and comes
back: the rows are still there. If they come back more than one hour after the last change, the tab is empty again.
"Limpar repertório" empties it on demand.

**Why this priority**: it is the most frequent pain today and needs no server, no push and no permission.

**Independent Test**: fill rows, kill the app, reopen within the hour and check the rows; repeat after more than an
hour without changes and check the tab is empty.

**Acceptance Scenarios**:

1. **Given** the tab with songs, keys and a pinned row, **When** the app is closed and reopened within one hour of the
   last change, **Then** every row comes back with the same song, key and pinned state, in the same position.
2. **Given** the last change was more than one hour ago, **When** the tab is opened, **Then** all rows are empty and the
   stored draft is gone.
3. **Given** a draft last changed 50 minutes ago, **When** the user changes one key, **Then** the draft is kept for
   another full hour from that change.
4. **Given** a draft whose song no longer exists in the song catalog, **When** it is restored, **Then** that row comes
   back empty and the other rows are untouched.
5. **Given** a filled draft, **When** the user taps "Limpar repertório", **Then** every row is empty and reopening the
   app shows an empty tab.
6. **Given** a setlist saved on the server, **When** the tab is opened with no draft, **Then** the rows stay empty — the
   draft is never filled from the server.

---

### User Story 2 - A leader saves the Sunday setlist (Priority: P1)

A user who can save sees "Salvar" next to "Gerar" (sharing is a small icon on the right, just below the rows).
Tapping "Salvar" asks to confirm the save for the
Sunday it applies to (today if today is Sunday, otherwise next Sunday), sends the filled rows in their positions, and
tells the user the result. A mistake is fixed by saving again.

**Why this priority**: every other server-side behavior starts from a saved setlist.

**Independent Test**: as a user whose profile allows saving, fill two rows with keys, save, and check the server holds a
two-item setlist for the right Sunday; save again with a different list and check it was replaced.

**Acceptance Scenarios**:

1. **Given** a profile without permission to save, **When** the tab is shown, **Then** there is no "Salvar"; "Gerar" and
   "Compartilhar" work as today.
2. **Given** a profile allowed to save and today is a Wednesday, **When** the user taps "Salvar", **Then** the
   confirmation names the next Sunday as dd/MM.
3. **Given** today is a Sunday, **When** the user taps "Salvar", **Then** the confirmation names today.
4. **Given** rows 1 and 3 filled and rows 2 and 4 empty, **When** the user confirms, **Then** only two items are sent,
   at positions 1 and 3.
5. **Given** no filled row, or a filled row with no key, **Then** "Salvar" is disabled.
6. **Given** a successful save, **Then** a one-shot message "Repertório de domingo dd/MM salvo." appears and the
   returned setlist becomes the device's "Repertório de domingo" immediately, without waiting for the push.
7. **Given** the server refuses with a permission error, invalid data, missing songs, or there is no connection,
   **Then** a one-shot message in Portuguese explains it (for missing songs, naming how many were not found), the draft
   is untouched and the user can try again.
8. **Given** a save in progress, **Then** "Salvar" shows progress and cannot be tapped again until it finishes.
9. **Given** any profile, **When** the user taps "Compartilhar", **Then** it shares the text as today, with or without a
   save, on any day of the week.

---

### User Story 3 - The band sees the Sunday setlist in Letras and Cifras (Priority: P1)

A worship member opens Letras or Cifras and sees, above their own pinned songs, a "Repertório de domingo dd/MM" section
with the setlist's songs in order and each key. It arrives by push, and also when the app starts or comes back to the
foreground, and stays available offline until the end of that Sunday.

**Why this priority**: distributing the setlist is the reason to save it.

**Independent Test**: as a worship member, save a setlist from another device, open Letras on this one and check the
section; turn off the network, restart the app and check it is still there; move the clock past that Sunday and check
it is gone.

**Acceptance Scenarios**:

1. **Given** a worship member and a current setlist on the server, **When** the app starts or returns to the foreground,
   **Then** the setlist is fetched and stored, and Letras and Cifras show the section above the manual pinned songs.
2. **Given** the section is stored, **When** the app opens with no connection, **Then** the section is still shown.
3. **Given** the server answers that there is no current setlist, **Then** the stored setlist is cleared and the section
   disappears.
4. **Given** a stored setlist for Sunday 04/10, **When** the device date becomes Monday 05/10, **Then** the section is
   no longer shown, even without any fetch.
5. **Given** a setlist with a song that has no lyrics, **Then** that song is absent from the section in Letras but
   present in Cifras when it has a chord chart, and vice versa.
6. **Given** a user who is not a worship member, **Then** no fetch is made and neither list shows the section.
7. **Given** manual pins, **Then** they keep working as today, below the section.
8. **Given** the fetch fails (no network, server error) and a setlist is already stored, **Then** the stored one keeps
   showing and no error interrupts the list; with nothing stored, the list looks as today.

---

### User Story 4 - Devices receive push notifications (Priority: P1)

Once logged in — including users who logged in before this version — the device registers itself to receive pushes, and
re-registers when its push token changes. Logging out unregisters it first, and logout never fails because of it. A
`setlist_saved` message refreshes the setlist and, when the app is not in the foreground, shows "Repertório de domingo
dd/MM disponível"; tapping it opens Letras.

**Why this priority**: without it the band only sees the setlist when they happen to open the app.

**Independent Test**: log in, save a setlist from another device with the app in background and check the notification
and that tapping it opens Letras with the section; log out and check the device no longer receives messages.

**Acceptance Scenarios**:

1. **Given** a fresh login, **Then** the device registers its push token.
2. **Given** a user already logged in before updating to this version, **When** the app starts, **Then** the device
   registers its push token without a new login.
3. **Given** the push provider rotates the token while logged in, **Then** the new token is registered.
4. **Given** the user taps "Sair", **Then** the token is unregistered before the server logout; if unregistering fails
   (no network, server error), logout still completes.
5. **Given** a worship member with the app in background, **When** a `setlist_saved` message arrives, **Then** the
   setlist is fetched and stored and a notification "Repertório de domingo dd/MM disponível" appears; tapping it opens
   Letras.
6. **Given** the app is in the foreground, **When** a `setlist_saved` message arrives, **Then** the section refreshes
   and no notification is shown.
7. **Given** the device is logged out or the user is not a worship member, **When** any message arrives, **Then**
   nothing happens.
8. **Given** a message with an unknown type or a missing or malformed date, **Then** it is ignored without a crash.

---

### User Story 5 - Sunday-night reminder opens a pre-filled register (Priority: P2)

On Sunday night, while the played songs are not registered, the people responsible get "Confirmar músicas de domingo".
Tapping it opens "Registrar domingo" with the date, songs and keys of the setlist already filled in; they adjust what
changed and send.

**Why this priority**: closes the gap of unrecorded Sundays, but the setlist is already useful without it.

**Independent Test**: deliver a `confirm_plays` message for a date with a setlist to a user with `manage` on `songs`,
tap it, and check the register screen in Sunday mode with that date and those rows; send it and check the date leaves
the pending list.

**Acceptance Scenarios**:

1. **Given** a `confirm_plays` message for 04/10, **Then** the notification "Confirmar músicas de domingo" is shown,
   whether the app is in the foreground or not.
2. **Given** a user with `manage` on `songs`, **When** they tap it, **Then** the register screen opens in Sunday mode,
   showing loading while the setlist is read, then with date 04/10 and one row per setlist item, in order, with its key.
3. **Given** the setlist for that date no longer exists, **When** the notification is tapped, **Then** the register
   screen opens in Sunday mode with date 04/10 and empty rows, and a message says the repertoire was not found.
4. **Given** the user no longer has `manage` on `songs` (or is logged out), **When** the notification is tapped,
   **Then** the app opens on the home screen with a message saying they no longer have access; nothing crashes.
5. **Given** the setlist read fails for network or server reasons, **Then** the screen shows the error with "Tentar
   novamente" and still allows filling the rows by hand.
6. **Given** the pre-filled rows, **When** the user edits, removes or adds rows and sends, **Then** what is registered is
   what the screen shows, not the setlist.

---

### User Story 6 - Admin card lists Sundays pending confirmation (Priority: P3)

In the admin panel, a user with `manage` on `songs` sees "Confirmar músicas de domingo" listing the Sundays whose played
songs were never registered, newest first. Tapping a Sunday opens the same pre-filled register. Once registered, that
Sunday leaves the list; with nothing pending, the card is hidden.

**Why this priority**: catch-up for the Sundays the reminder did not resolve.

**Independent Test**: with two past setlists without plays, open the panel, check both dates newest first, register one
from the card, return and check only the other remains.

**Acceptance Scenarios**:

1. **Given** a user with `manage` on `songs` and two pending Sundays, **When** the panel opens, **Then** the card lists
   both dates as dd/MM, newest first.
2. **Given** no pending Sunday, **Then** the card is not shown.
3. **Given** a user without `manage` on `songs`, **Then** the card is never shown and nothing is fetched for it.
4. **Given** the list is loading, **Then** the rest of the panel is usable and the card space shows loading; **given**
   the read fails, **Then** the card shows a short error with "Tentar novamente".
5. **Given** a tap on a date, **Then** the register screen opens pre-filled exactly as in User Story 5.
6. **Given** the user registered the played songs for one Sunday, **When** they return to the panel, **Then** the list
   is read again and that Sunday is gone.
7. **Given** a user who can save a setlist, **When** they tap "Remover" (trash icon and text, at the right end of a
   date's line), **Then** a dialog asks "Remover repertório de dd/MM?" with "As músicas planejadas para esse domingo
   serão apagadas.", "Remover" and "Cancelar"; "Cancelar" changes nothing.
8. **Given** the dialog, **When** the user taps "Remover" and the server deletes it (or answers that it no longer
   exists), **Then** that date leaves the card; when it was the last one, the card is hidden.
9. **Given** the delete fails (no connection, server error, refused), **Then** the date stays and a snackbar says
   "Não foi possível remover o repertório de dd/MM."
10. **Given** a user with `manage` on `songs` who cannot save a setlist (not in the worship ministry), **Then** the
    dates are listed with no "Remover" button.

---

### Edge Cases

- **Draft restored but the song catalog is not loaded yet** (first start offline with no catalog): rows wait for the
  catalog; a row is emptied only when the catalog is available and the song is absent from it.
- **Draft song exists but the key is empty**: restored as is; the save stays disabled until a key is set.
- **Key longer than three characters typed in a row**: save disabled for that row, same as an empty key.
- **Saving on Sunday after the service** (e.g. 22:00): the date is still today, as the rule says.
- **Two devices of the same user**: each registers its own token; each receives its own messages.
- **Two accounts on the same phone**: the device belongs to the last logged-in account (server rule); the app only
  registers after login and unregisters on logout.
- **`setlist_saved` for a date other than the current setlist** (e.g. a late correction of a past Sunday): the app
  always reads the current setlist; the stored section follows what the server calls current.
- **Notification permission denied**: no notifications are shown; the start/resume fetch keeps the section current, and
  the admin card keeps listing pending Sundays.
- **Section boundary**: shown through 23:59 of the setlist's Sunday in the device's time zone, gone from Monday 00:00.
- **Profile flag turns false while a setlist is stored**: the stored setlist is cleared on the next profile refresh.
- **The register screen opened from a notification while another admin screen is open**: it opens on top; back returns
  to where the user was, or to the panel when the app was opened by the notification.
- **Many `confirm_plays` messages in one night**: they replace the same notification instead of stacking.
- **Push token cannot be obtained** (no Google Play services): the app works as before, without push; nothing is shown
  to the user.

## Requirements *(mandatory)*

### Functional Requirements

**Repertoire draft**

- **FR-001**: The app MUST persist on the device the Repertório rows — song, key and pinned state per position — every
  time they change, whether chosen by hand or by "Gerar".
- **FR-002**: On opening the tab, the app MUST restore the stored rows when the last change was less than one hour ago;
  otherwise it MUST discard the draft and show empty rows. Every change renews the hour.
- **FR-003**: A restored row whose song is not in the song catalog MUST come back empty; other rows are unaffected.
- **FR-004**: "Limpar repertório" MUST empty every row and delete the stored draft. It is a small clear icon
  (content description "Limpar repertório") beside the share icon, enabled only when a row is filled.
- **FR-005**: The draft MUST be local to the device, never sent to the server, never filled from a server setlist, and
  kept across logout.

**Save**

- **FR-006**: "Salvar" MUST be shown only when the profile allows saving a setlist; "Gerar" and sharing stay
  available to everyone who sees the tab. Sharing is a small share icon (content description "Compartilhar") on the
  right, just below the rows, not a text button.
- **FR-007**: The setlist date MUST be today when today is Sunday, otherwise the next Sunday, using the device's calendar
  date; the user MUST see that date (dd/MM) and confirm before the save is sent.
- **FR-008**: The save MUST send only the filled rows, each with its own position and key.
- **FR-009**: "Salvar" MUST be disabled when no row is filled, when any filled row has no key or a key longer than three
  characters, or while a save is in progress.
- **FR-010**: A successful save MUST show a one-shot confirmation naming the date and MUST store the returned setlist as
  the device's "Repertório de domingo" at once.
- **FR-011**: A failed save MUST show a one-shot message in Portuguese for each case: no permission, invalid data, songs
  not found (with the count), no connection, and a generic server failure. The draft is never altered by a failure.

**Push registration**

- **FR-012**: The app MUST register the device's push token after every login, on every start while logged in, and
  whenever the token rotates while logged in.
- **FR-013**: On logout the app MUST unregister the token before the server logout; any failure of that call MUST be
  ignored and the logout MUST proceed.
- **FR-014**: A failure to register MUST NOT be shown to the user or block anything; the next start retries.

**Incoming messages**

- **FR-015**: The app MUST act only on the message types `setlist_saved` and `confirm_plays` with a valid date, and only
  while logged in; anything else is ignored.
- **FR-016**: On `setlist_saved`, for a worship member, the app MUST fetch and store the current setlist and, unless the
  app is in the foreground, show "Repertório de domingo dd/MM disponível"; tapping it opens Letras.
- **FR-017**: On `confirm_plays`, the app MUST show "Confirmar músicas de domingo"; repeated messages replace the same
  notification. Tapping it opens the pre-filled register for that date (FR-024).
- **FR-018**: Both notifications MUST use a dedicated "Repertório" channel and be shown only when notification
  permission is granted.

**"Repertório de domingo" section**

- **FR-019**: For worship members only, the app MUST fetch the current setlist on start and on every return to the
  foreground, store it for offline use, and clear it when the server says there is none.
- **FR-020**: The stored setlist MUST be cleared on logout and when the profile says the user is not a worship member.
- **FR-021**: Letras and Cifras MUST show a "Repertório de domingo dd/MM" section above the manual pinned songs, with the
  setlist's songs in position order and each key, from the moment it is stored until the end of its Sunday.
- **FR-022**: Songs with no lyrics are left out of the section in Letras; songs with no chord chart are left out in
  Cifras. A song shown in the section is not repeated in the pinned list or the rest of the list.
- **FR-023**: Manual pins MUST keep their current behavior, separate from the section.

**Play confirmation**

- **FR-024**: The register screen MUST accept an entry for a given Sunday that opens Sunday mode with that date, reads
  the setlist for it, and fills one row per item in position order with its key; rows remain editable.
- **FR-025**: When that setlist does not exist, the screen MUST open with the date and empty rows and say so; when the
  user lacks `manage` on `songs` or is logged out, the app MUST land on the home screen with a message instead.
- **FR-026**: The admin panel MUST show, to users with `manage` on `songs` only, a "Confirmar músicas de domingo" card
  listing pending Sundays newest first; the card is hidden when the list is empty; tapping a date opens FR-024.
- **FR-027**: The pending list MUST be read again whenever the panel is shown, so a Sunday registered meanwhile
  disappears.
- **FR-029**: Each date on the pending card MUST offer a delete action, shown only when the profile says the user can
  save a setlist. It MUST ask for confirmation, delete the setlist of that date on the server, and remove the date from
  the card on success or when the server says it no longer exists; any other failure MUST keep the date and show
  "Não foi possível remover o repertório de dd/MM." No other device is notified.

**States**

- **FR-028**: Every new screen or section — save in progress, the "Repertório de domingo" section, the pre-filled
  register, the pending card — MUST handle loading, success and error, following the app's error rules
  (`specs/constitution.md`). The pending card's loading is silent: it stays hidden until the first answer, since the
  list is usually empty and a card that flashes in and out pushes the grid for nothing.

### Key Entities

- **Repertoire draft**: device-local; up to four rows (position, song, key, pinned) and the time of the last change.
  Unrelated to any account or server setlist.
- **Sunday setlist (stored)**: device-local copy of the server's current setlist for worship members — date, items
  (position, song, title, artist, key), author's name, save time. Visible until the end of its date.
- **Device push registration**: the device's push token as last sent to the server, tied to the logged-in session.
- **Pending Sunday**: a server setlist with no registered plays, shown in the admin card; not stored.
- **Profile flags**: `is_worship_member` and `can_save_setlist`, read with the rest of the profile.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A draft reopened within one hour of the last change is restored identically in 100% of cases, including
  after the app is killed.
- **SC-002**: A worship member with the app closed sees the setlist notification within one minute of the save in at
  least 95% of saves, and sees the section on opening the app even when the push was lost.
- **SC-003**: The "Repertório de domingo" section is readable with no connection for the whole period until the end of
  its Sunday.
- **SC-004**: A leader goes from a filled draft to a saved setlist in at most two taps (Salvar, confirm).
- **SC-005**: From the Sunday-night notification, a responsible user registers the played songs without typing any song
  or key when nothing changed from the setlist.
- **SC-006**: Logout always completes, including with no connection, regardless of the push unregistration.
- **SC-007**: No message, notification tap or missing access leads to a crash or to an empty screen with no
  explanation.

## Assumptions

- The two profile flags arrive with the existing profile read and are kept with the rest of the profile snapshot, so
  they are available from boot like the other access data.
- "Today" for the save date and for the end of the section uses the device's date; the congregation is in a single time
  zone matching the server's.
- The notification permission request already made on launch for Android 13+ is enough; this feature adds no new
  prompt.
- The existing Firebase project of the app is used for push; the app adds only the messaging part.
- Pending Sundays and setlists read by date are not stored on the device; they need a connection.
- The register screen's existing rules for rows, dates and sending are unchanged; this feature only adds the pre-filled
  entry.
- Letras opened from the `setlist_saved` notification is the regular Letras list; no new screen is added.
- Out of scope: showing the setlist outside Letras and Cifras, editing a saved setlist except by saving again, more than
  four rows in the Repertório tab, and any notification other than the two message types.
