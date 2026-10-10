# Feature Specification: Encrypted Session, Encrypted Member Photo Cache and Member Photo Download

**Feature Branch**: `012-encrypted-session-photo-cache`

**Created**: 2026-10-09

**Status**: Draft

**Input**: User description: "Proteger dados de sessão e fotos de membros no aparelho, e permitir que líderes baixem a
foto de um membro." (full request — encrypted session, encrypted member photo cache, full-screen viewer with download,
and out of scope — in the `/speckit-specify` invocation that created this directory)

## Context

Two things the app keeps on the device are weaker than they should be, and one leader need is not met:

- **Session**: the sign-in tokens live in the app's session store as plain text. The store is already left out of
  cloud backup and device-to-device transfer, but anyone who can read the app's private files (rooted device,
  forensic copy) reads a working session.
- **Member photos** (members area, `specs/005-admin-members-management`): FR-033 of 005 forbids writing them to the
  device, so they live only in memory. Every time the app opens, every photo on the list and the profile downloads
  again and appears late.
- **Download**: a leader who needs a member's photo outside the app (a bulletin, a birthday card) has no way to get it.

This feature:

1. Encrypts the whole session store with a key held by the device's secure key store, migrating signed-in users
   without making them sign in again.
2. Adds an encrypted on-device cache for member photos only, with its own key, so photos open instantly and are
   revalidated with the server.
3. Moves the full-screen viewer's actions to the bottom ("Apagar" and a new "Baixar"), and lets leaders with `manage`
   on `members` save the photo, unencrypted, to the device gallery.

### Changes to `specs/005-admin-members-management`

This feature supersedes, in 005: FR-023a (viewer layout), FR-033 (photos may now be cached on disk, encrypted; member
data still may not), FR-035 (sign-out also clears the disk cache) and SC-004 (no **readable** photo on the device). It
adds FR-026a (download). 005 is updated in the same change to point here.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Session survives the update, now encrypted (Priority: P1)

A signed-in user updates the app. On first launch the app moves the session into encrypted storage and removes the
plain-text copy. The user notices nothing: they are still signed in and everything works as before.

**Why this priority**: The session is the most valuable secret on the device (it opens every leader screen without a
password). Getting the migration wrong signs out every user at once.

**Independent Test**: Sign in on the previous version, install the new one over it, open the app; confirm the user is
still signed in, authenticated screens load, and the app's private files no longer hold the tokens in readable form.

**Acceptance Scenarios**:

1. **Given** a user signed in on the previous version, **When** they open the updated app, **Then** they land where
   they normally would, signed in, without seeing the sign-in screen.
2. **Given** the migration has run, **When** the app's private files are inspected, **Then** no session token,
   refresh token or other session value is readable, and the old plain-text session file no longer exists.
3. **Given** a user who was signed out on the previous version, **When** they open the updated app, **Then** they see
   the sign-in screen as before, and no plain-text session file remains.
4. **Given** the migrated app, **When** the user signs in, signs out, or the session is renewed in the background,
   **Then** each behaves exactly as before for the user, and only encrypted session data is written.
5. **Given** the migration is interrupted (app killed mid-way), **When** the app opens again, **Then** it finishes the
   migration or, if the old file is already gone, uses the encrypted copy — the user is never left with two copies or
   with none while they were signed in before.

---

### User Story 2 - Session cannot be read: quiet sign-out (Priority: P1)

The encrypted session cannot be opened — the key was lost (screen-lock change on some devices, backup restored on
another phone, manufacturer key-store fault) or the file is corrupt. The app does not crash or show technical errors:
it clears the session and the member photo cache, prepares a new key, and shows the sign-in screen.

**Why this priority**: Without it, a key-store failure turns into a crash on every launch — the app becomes unusable
until reinstalled.

**Independent Test**: With a signed-in session, make the session unreadable (delete the key, corrupt the file);
open the app and confirm it shows the sign-in screen with no error message, that signing in works, and that the
member photo cache is empty.

**Acceptance Scenarios**:

1. **Given** the session key no longer exists, **When** the app opens, **Then** it shows the sign-in screen with no
   error message, and the app does not close.
2. **Given** the session file is corrupt, **When** the app opens, **Then** the same happens.
3. **Given** a quiet sign-out happened, **When** the user signs in again, **Then** the new session is saved encrypted
   with a new key, and the next launch keeps them signed in.
4. **Given** a quiet sign-out happened, **When** a leader opens the members area after signing in, **Then** every
   photo downloads again (nothing from before is shown).
5. **Given** the session becomes unreadable while the app is open, **When** the app next needs the session, **Then**
   it follows the same quiet sign-out instead of failing the screen.

---

### User Story 3 - Member photos open instantly (Priority: P2)

A leader opens the members list. Photos already seen in a previous session appear immediately, without the initials
flashing first. In the background the app checks each shown photo with the server; if one changed (another leader
replaced it), the new one replaces it on screen.

**Why this priority**: The visible gain of the feature for leaders, but the area already works without it.

**Independent Test**: Open the list and some profiles, close the app fully, reopen with a normal connection and
with no connection; confirm photos appear at once. Replace a photo from another device and reopen; confirm the old
one shows first and then the new one.

**Acceptance Scenarios**:

1. **Given** photos seen in an earlier session, **When** the leader opens the list after restarting the app, **Then**
   those photos appear with the cards, without a wait and without the initials showing first.
2. **Given** the list was loaded earlier in this session and the connection then drops, **When** the leader opens a
   profile, **Then** its cached photo still appears. Opening the area with no connection still follows 005 (connection
   error), because member data is not cached.
3. **Given** another leader replaced a member's photo, **When** this leader opens the list, **Then** the old photo
   shows first and is replaced by the new one as soon as the server answers, without any message.
4. **Given** another leader removed a member's photo, **When** this leader opens the list, **Then** the initials
   replace the old photo and the cached copy is discarded.
5. **Given** this leader replaces, removes a photo or deletes a member in the app, **When** the action succeeds,
   **Then** the old photo is discarded from the cache at once, and the next display never shows it.
6. **Given** the cache reached its size limit, **When** a new photo is cached, **Then** the photos used least
   recently are discarded first, and the app keeps working normally.

---

### User Story 4 - Losing access wipes the photo cache (Priority: P2)

A leader signs out, or loses access to the members area (role changed, session expired and could not be renewed).
The app erases the whole photo cache and its key, so anything that might remain on disk is unreadable.

**Why this priority**: The cache is only acceptable under LGPD art. 11 if it disappears together with the right to
see it.

**Independent Test**: Fill the cache, then (a) sign out, (b) remove the user's `members` access on the server and
reopen the list, (c) expire the session so renewal fails; after each, confirm the cache is empty and the next leader
to sign in downloads every photo again.

**Acceptance Scenarios**:

1. **Given** a filled cache, **When** the user signs out, **Then** the cache and its key are erased before the
   sign-in screen appears.
2. **Given** a filled cache, **When** a request for the member list or a member photo is refused as not signed in
   (after the app's usual renewal attempt) or not allowed, **Then** the cache and its key are erased.
3. **Given** another user signs in on the same device, **When** they open the members area, **Then** none of the
   previous user's cached photos is shown.
4. **Given** cached files somehow remain after the key was erased, **When** anyone reads them, **Then** they cannot
   be turned back into images.

---

### User Story 5 - Full-screen viewer with "Apagar" and "Baixar" at the bottom (Priority: P2)

On the full-screen photo, "Trocar foto" stays in the top-left corner and the close button in the top-right. The trash
moves to the bottom as an "Apagar" button (trash icon + text), and a new "Baixar" button (download icon + text) sits
next to it; the two are side by side, centred at the bottom.

**Why this priority**: Required to place "Baixar"; small visual change.

**Independent Test**: Open the viewer as Admin (owner), as Liderança (manage) and on a member with and without a
photo; check which buttons appear and where.

**Acceptance Scenarios**:

1. **Given** an Admin on a member with a photo, **When** the viewer opens, **Then** "Trocar foto" is top-left, close
   is top-right, and "Apagar" and "Baixar" are side by side, centred at the bottom.
2. **Given** a Liderança user (`manage`, not `owner`) on a member with a photo, **When** the viewer opens, **Then**
   only "Baixar" appears at the bottom, centred.
3. **Given** any user on a member without a photo (initials), **When** the viewer opens, **Then** no bottom button
   appears.
4. **Given** a user with only `view` on `members`, **When** the viewer opens, **Then** neither "Trocar foto",
   "Apagar" nor "Baixar" appears.
5. **Given** "Apagar" is tapped, **When** the confirmation appears and is accepted, **Then** removal follows 005
   (FR-025) exactly as the trash did.
6. **Given** the bottom buttons, **When** the device has gesture or button navigation, **Then** they stay above the
   system bars and are never covered.

---

### User Story 6 - Leader saves a member photo to the device gallery (Priority: P3)

A leader with `manage` on `members` taps "Baixar". The photo is saved to the device gallery, in the "IPB Castelo
Branco" album under Pictures, and a message says where. The saved copy is ordinary (unencrypted): it belongs to the
leader, and the app never removes it afterwards.

**Why this priority**: Useful, but occasional; the rest of the feature does not depend on it.

**Independent Test**: On Android 10+ and on Android 9 or earlier, download a photo; confirm it appears in the gallery
app in Pictures/IPB Castelo Branco and opens; deny the permission on Android 9 and confirm nothing is saved; cut the
connection mid-download and confirm no broken file remains.

**Acceptance Scenarios**:

1. **Given** a member with a photo, **When** the leader taps "Baixar", **Then** the photo is saved to Pictures/IPB
   Castelo Branco, appears in the gallery app, and the message "Foto salva em Imagens/IPB Castelo Branco" is shown.
2. **Given** a device on Android 9 or earlier that has not granted storage permission, **When** the leader taps
   "Baixar", **Then** the app asks for it; **When** it is granted, **Then** the download proceeds.
3. **Given** the permission is denied, **When** the request returns, **Then** a message explains that saving needs
   the storage permission, and nothing is saved.
4. **Given** the permission was denied permanently, **When** the leader taps "Baixar", **Then** a message explains
   that the permission must be allowed in the device settings, and nothing is saved.
5. **Given** the download or the write fails (no connection, full storage, refused photo), **When** it fails, **Then**
   an error message is shown and no partial or empty file is left in the gallery.
6. **Given** a download is in progress, **When** the leader taps "Baixar" again, **Then** no second download starts.
7. **Given** a photo was saved, **When** the leader signs out, the member is deleted or the photo is replaced,
   **Then** the saved copy stays in the gallery, untouched.
8. **Given** the same member's photo is downloaded twice, **When** the second one is saved, **Then** both copies
   exist; the first is not overwritten.

---

### Edge Cases

- **Device with no hardware-backed key store** (older or low-end Android 7–8 devices): the key is kept by the
  platform key store in whatever form the device supports; the feature still works.
- **Key store temporarily unavailable at launch** (rare manufacturer faults): treated as unreadable session — quiet
  sign-out (US2), never a crash.
- **Photo cache unreadable** (its key lost or a cached file corrupt): the affected entries are discarded and the photo
  downloads again; the user sees at most the initials briefly, never an error. The session is not affected.
- **Session renewed in the background while the migration runs**: the renewed tokens are the ones kept; the old
  plain-text values never overwrite them.
- **App downgraded** to a version without encryption: not supported (store releases only move forward); if it ever
  happens, the old version finds no session and shows sign-in.
- **Backup and transfer**: the encrypted session and the photo cache are left out of cloud backup and device
  transfer, like the session store today; a restored device starts signed out with an empty cache.
- **Photo refused or missing on revalidation** (404 or a refusal other than sign-in/permission): the cached copy is
  discarded and initials are shown, per 005 FR-026.
- **Rate-limited revalidation**: the cached photo keeps showing; the next display tries again.
- **"Baixar" while offline with the photo cached**: the shown photo is saved from the cache, decrypted, so the
  download works offline.
- **"Baixar" while the photo is still the cached copy and revalidation finds a newer one**: the photo saved is the one
  being shown when "Baixar" was tapped.
- **Storage full during "Baixar"**: error message, nothing saved.
- **Leader loses `manage` while the viewer is open**: "Baixar" follows the permissions the app currently knows; once
  the profile refresh removes `manage`, the button disappears. A refused photo or list request wipes the cache (US4)
  and leaves the area (005 FR-003).
- **Member name with characters not allowed in file names**: the saved file name drops or replaces them.

## Requirements *(mandatory)*

### Functional Requirements

**Session encryption**

- **FR-001**: Everything in the session store (tokens and any other value kept there) MUST be written to disk only in
  encrypted form; no session value may exist in readable form in the app's files.
- **FR-002**: The encryption key MUST be generated on the device and held by the platform's secure key store, never
  exported, never written to the app's files, and never sent anywhere.
- **FR-003**: On the first launch after the update, if a plain-text session exists, the app MUST read it, write it
  encrypted, confirm the encrypted copy reads back, and only then delete the plain-text file. The user stays signed
  in.
- **FR-004**: The migration MUST be safe to interrupt: on the next launch it resumes or recognises it is done, and
  never discards a session it could have kept.
- **FR-005**: If the session cannot be decrypted or read (key missing, invalidated or failing; file corrupt), the app
  MUST: discard the session, erase the member photo cache and its key (FR-012), create a new session key, and show
  the sign-in screen — with no technical message, without closing or freezing.
- **FR-006**: Sign-in, sign-out and background session renewal MUST behave for the user exactly as before.
- **FR-007**: The encrypted session MUST stay excluded from cloud backup and device transfer.

**Member photo cache**

- **FR-008**: Member photos MAY be cached on the device's storage, only in encrypted form, with a key of their own,
  separate from the session key and held the same way (FR-002). Only photos are cached; no member data (name,
  dates, gender, status, role, ministries, validity, history) may be written to the device (005 FR-033 still
  applies to data).
- **FR-009**: A member photo MUST show from the cache when present, and be revalidated with the server in the
  background on each display (at most once per photo per screen opening); when the server has a different photo, the
  new one replaces it on screen and in the cache; when it has none or refuses it (other than FR-012), the cached copy
  is discarded and initials are shown.
- **FR-010**: The cache MUST have a size limit of 50 MB; when a new photo would exceed it, the least recently used
  photos are discarded first.
- **FR-011**: Replacing a photo, removing a photo, or deleting a member through the app MUST discard that member's
  cached photo as soon as the action succeeds.
- **FR-012**: The whole photo cache and its key MUST be erased when: the user signs out; the session is discarded
  (FR-005 or a failed renewal); or a request for the member list or a member photo is refused as not signed in
  (after the usual renewal) or not allowed.
- **FR-013**: A cached photo that cannot be decrypted MUST be discarded and downloaded again without any message.
- **FR-014**: The photo cache MUST be excluded from cloud backup and device transfer.
- **FR-015**: Logs MUST NOT contain photo content or member data; 005 FR-036 applies to the cache and the download.

**Full-screen viewer**

- **FR-016**: The full-screen viewer (005 FR-023a) MUST keep "Trocar foto" (pencil) in the top-left corner and the
  close button in the top-right, and MUST show at the bottom, side by side and centred: "Apagar" (trash icon + text)
  and "Baixar" (download icon + text). A single visible button is centred alone. Bottom buttons stay clear of the
  system bars.
- **FR-017**: "Apagar" MUST appear only for users with `owner` on `members` and only when the member has a photo, and
  MUST ask for confirmation (005 FR-025).
- **FR-018**: "Baixar" MUST appear only for users with `manage` (or higher) on `members` and only when the member has
  a photo.
- **FR-019**: While an action is running (upload, removal or download), the viewer's buttons MUST be disabled and
  progress shown, as today for upload and removal.

**Download**

- **FR-020**: "Baixar" MUST save the photo being shown, in its original format, to the device's shared pictures in a
  folder named "IPB Castelo Branco", so it appears in the device's gallery app.
- **FR-021**: The saved copy MUST NOT be encrypted, and the app MUST NOT track, remove or change it afterwards —
  sign-out, member deletion and photo replacement leave it untouched.
- **FR-022**: Each download MUST create a new file named after the member and the date (e.g. "Maria Souza
  2026-10-09.jpg"), never overwriting an earlier one; characters not allowed in file names are removed.
- **FR-023**: On success the app MUST show "Foto salva em Imagens/IPB Castelo Branco".
- **FR-024**: On any failure (download refused or failed, write failed, storage full) the app MUST show an error
  message following the constitution's error rules, and MUST leave no partial or empty file in the gallery.
- **FR-025**: The download MUST work on every supported Android version (7 and later). Where the system requires a
  storage permission to save to shared pictures (Android 9 and earlier), the app MUST ask for it when "Baixar" is
  tapped; if denied, show "Para salvar a foto, permita o acesso ao armazenamento." and save nothing; if denied
  permanently, show a message pointing to the device settings and save nothing. Where no permission is needed, none
  is asked.
- **FR-026**: A second tap on "Baixar" while a download runs MUST NOT start another download.
- **FR-027**: Downloads MUST NOT be recorded in the member's history (out of scope).

### Key Entities

- **Session store**: the signed-in user's tokens and related session values; one per device; now stored encrypted.
- **Session key**: secret held by the platform key store that encrypts the session store; replaced on quiet sign-out.
- **Member photo cache entry**: the encrypted bytes of one member's photo, keyed by its photo reference, with the
  version information needed to revalidate it with the server and its last-use time for eviction.
- **Photo cache key**: secret held by the platform key store that encrypts the cache; erased with the cache.
- **Downloaded photo**: an ordinary image file in the device's shared pictures, owned by the leader, outside the app's
  control.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of users signed in before the update are still signed in after it, with no sign-in prompt.
- **SC-002**: 0 session values (tokens or others) found in readable form in the app's files after the migration,
  after sign-in and after session renewal.
- **SC-003**: 0 crashes or frozen screens caused by an unreadable session or photo cache; in 100% of those cases the
  user reaches the sign-in screen (session) or sees initials then the photo (cache) with no technical message.
- **SC-004**: Photos seen in an earlier session appear on the list and profile in under 300 ms after the screen's data
  is shown, with no initials flashing first, on a typical device.
- **SC-005**: A photo replaced by another leader is shown in its new version within 5 seconds of opening the screen on
  a typical connection.
- **SC-006**: The photo cache never exceeds 50 MB.
- **SC-007**: 0 readable member photos and 0 member data fields found in the app's files after signing out or after a
  refused members request (supersedes 005 SC-004, which forbade any photo file).
- **SC-008**: A leader saves a member photo to the gallery in 2 taps (open full screen, "Baixar") — plus the
  permission prompt the first time on Android 9 or earlier — and finds it in the gallery app on 100% of supported
  Android versions.
- **SC-009**: 0 partial or empty files left in the gallery after a failed download.

## Assumptions

- The session store today holds only the sign-in tokens; "everything in it" covers any value added later.
- The server's protected media responses carry version information (entity tag or modification date) that allows a
  conditional check, so revalidation of an unchanged photo costs no image download. If they do not, revalidation
  downloads the photo and compares it; behaviour for the user is the same.
- The member photo reference sent by the server stays the same when the photo is replaced, so revalidation (not a new
  reference) is what detects changes; if the reference changes, the old entry simply ages out or is discarded by
  FR-011.
- The platform key store is available on every supported version (Android 7+); hardware backing depends on the
  device and is used when present.
- The members area keeps the access levels of `specs/006-role-scoped-permissions`: `view` (see), `manage` (edit,
  change photo, now download), `owner` (delete member, remove photo).
- The gallery's existing "save to device" uses a different folder name ("ipb_castelobranco"); this feature uses
  "IPB Castelo Branco" as requested and does not change the gallery.
- No backend change is needed: download uses the same signed-in photo request as display.
- Out of scope: encrypting other stores (settings, setlist, push), caching gallery photos or the user's own profile
  photo, requiring biometrics or screen lock to open the app, recording downloads in the member's history.

## Dependencies

- `specs/005-admin-members-management` — members area, photo viewer, photo rules (updated by this feature).
- `specs/006-role-scoped-permissions` — `view` / `manage` / `owner` on `members`.
- `specs/004-protected-media-downloads` — signed-in path for member photos.
