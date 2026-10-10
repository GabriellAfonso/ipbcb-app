# Feature Specification: Members Management for Church Leaders

**Feature Branch**: `005-admin-members-management`

**Created**: 2026-09-26

**Status**: Draft

**Input**: User description: "Gestão de membros para líderes (app Android). Contrato do backend:
specs/010-members-management do repositório backend (contracts/admin-members-api.md)." (full request — access,
status vs. validity, list, profile, form, photo, history, delete, privacy, errors, out of scope — in the
`/speckit-specify` invocation that created this directory)

## Overview

The church keeps its membership roll on the server, and today leaders can only change it through the Django admin
on a computer. The backend now offers a leader-only members API (backend feature `010-members-management`). This
feature gives leaders a **Membros** area inside the app's admin panel to browse the roll, open a member's profile,
create and edit members, manage a leader-only member photo, read who changed what, and delete a record.

Member data ties religious affiliation to a named person — sensitive personal data under LGPD art. 11. The app
therefore works **online only** for this area: no member data is ever written to the device's storage. Member photos
may be kept in an encrypted on-device cache that is erased on sign-out or loss of access
(`specs/012-encrypted-session-photo-cache`).

Chosen designs (previewed in `features/admin/members/presentation/screens/MembersDesignPreviews.kt`):

- List: **"Lista B · grade de cartões"**, without the summary filter tiles.
- Profile: **"Perfil A · cabeçalho com foto"**.

### Status vs. validity

Two different member attributes that the UI must never mix up:

| Attribute | What it is | Values | How the UI names it |
|---|---|---|---|
| Status (situação) | A roll category managed on the server | Today: Ativo, Inativo, Visitante — the list can change | Exactly the name the server sends; "Sem situação" when empty |
| Validity | Whether the record is a valid profile. Invalid records do not appear in the regular member list or in birthdays | valid / invalid | Always "Perfil válido" / "Perfil inválido" — never "ativo/inativo" |

A member can be "Inativo" and have a valid profile, or be "Ativo" with an invalid profile. The app never hardcodes
status names.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Leader browses the roll and opens a member (Priority: P1)

A leader opens the admin panel, taps **Membros**, and sees every member record — valid and invalid — as a grid of
cards with photo (or initials), name, status and an "Perfil inválido" tag when applicable. They type part of a name
to narrow the grid, tap a card and see the member's full profile.

**Why this priority**: Every other story starts from the list and the profile. Read access alone already replaces
asking whoever has Django admin access.

**Independent Test**: With a leader account and a roll containing valid and invalid members, with and without
status, photo and role, open the list, search, open profiles, and compare every field with the server data.

**Acceptance Scenarios**:

1. **Given** a leader, **When** they open the admin panel, **Then** the "Membros" card is active and opens the
   members list.
2. **Given** a roll of 3 valid and 1 invalid member, **When** the list loads, **Then** all 4 cards appear in name
   order, the invalid one faded and tagged "Perfil inválido".
3. **Given** members with status "Ativo", "Visitante" and no status, **When** the list loads, **Then** their chips
   read "Ativo", "Visitante" and "Sem situação".
4. **Given** the list is loaded, **When** the leader types "jose", **Then** only members whose name contains "José",
   "jose" or "JOSÉ" remain, without a new request to the server.
5. **Given** a search that matches nobody, **When** it is typed, **Then** a "nenhum membro encontrado" state is
   shown with the search term, instead of an empty grid.
6. **Given** a member born on 02/04/1990, baptised on 12/06/2005, **When** the profile is opened on 26/09/2026,
   **Then** it shows "36 anos" and "há 21 anos" next to the baptism date.
7. **Given** a member whose birthday is known but the birth year is not (day 2, month 4, no year), **When** the
   profile is opened, **Then** the birth date reads "02/04" and the age reads "Desconhecida".
7a. **Given** a member with only a birth year (1990), **When** the profile is opened on 26/09/2026, **Then** the
   birth date reads "1990" and the age reads "36 anos".
8. **Given** a member with no birth date, role, ministries or baptism date, **When** the profile is opened, **Then**
   those fields read "Não informado", "Sem cargo", "Nenhum ministério" and "Não informado" — never blank.
9. **Given** the list fails to load, **When** the error is shown, **Then** it offers "Tentar novamente", and
   retrying reloads the list.
10. **Given** a user who is not a leader, **When** they use the app, **Then** no entry point to the members area is
   shown anywhere.

---

### User Story 2 - Leader creates and edits a member (Priority: P1)

A leader adds a new member, or corrects an existing one: names, birth date, gender, status, role, ministries,
baptism date and validity. The status, role and ministry choices come from the server.

**Why this priority**: Keeping the roll current is the purpose of the area.

**Independent Test**: Create a member filling every field, reopen it and compare; edit two fields and confirm only
those were sent and changed; try each validation rule.

**Acceptance Scenarios**:

1. **Given** the list, **When** the leader taps "Novo membro", **Then** an empty form opens with "Perfil válido"
   on, and status, role and ministry choices loaded from the server.
2. **Given** the form with a name, **When** the leader saves, **Then** the member is created, the app shows the new
   profile, and the member appears in the list.
3. **Given** a blank name, a future date, or a baptism date before the birth date, **When** the leader tries to
   save, **Then** the form points at the offending field with a message and nothing is sent.
4. **Given** the server rejects a field, **When** the save returns, **Then** the server's message appears under
   that field and the form keeps what the leader typed.
5. **Given** an existing member, **When** the leader changes only the status and saves, **Then** only the status
   changes on the server and the profile shows the new value.
6. **Given** an existing member with a role, **When** the leader clears the role and saves, **Then** the profile
   shows "Sem cargo".
7. **Given** unsaved changes in the form, **When** the leader leaves it, **Then** the app asks for confirmation
   before discarding them.
8. **Given** a profile, **When** the leader turns "Perfil válido" off, **Then** the change is saved immediately,
   the profile and the list card show "Perfil inválido", and the explanatory text says the member no longer
   appears in the member list and birthdays.
9. **Given** the validity change fails, **When** the error returns, **Then** the switch goes back to its previous
   position and the error is shown.

---

### User Story 3 - Leader manages the member photo (Priority: P2)

A leader adds, replaces or removes a member's photo, picking it the same way they pick their own profile photo.
On the profile the photo has no button of its own: tapping it pops it in front of the screen as a square, tapping
that opens it full screen, where a "Trocar foto" button (pencil) opens the picker straight away, and "Apagar" and
"Baixar" buttons at the bottom remove the photo or save it to the device gallery. Without a photo, the initials go the same way. The create and edit forms also offer a camera to pick it
along with the other fields. Only leaders can see member photos.

**Why this priority**: Helps leaders recognise members, but the roll is usable without it.

**Independent Test**: On a member without a photo, add one; replace it; remove it; after each step reopen the
profile and the list and confirm what is shown. Try a file over 10 MB and a file in an unsupported format.

**Acceptance Scenarios**:

1. **Given** a member without a photo, **When** the leader taps the initials on the profile, **Then** they pop
   in front as a square like a photo would; **When** the leader opens them full screen, taps "Trocar foto", picks and
   crops an image, **Then** it is uploaded and shown on the profile and on the list card.
2. **Given** a member with a photo, **When** the leader taps it on the profile, **Then** it pops in front of the
   screen as a larger square over a dimmed background (a tap outside closes it); **When** they tap the square,
   **Then** it opens full screen with a "Trocar foto" button (pencil) in the top-left corner.
3. **Given** the full-screen photo, **When** the leader taps "Trocar foto", picks and
   crops an image, **Then** the new photo replaces the old one everywhere.
4. **Given** the full-screen photo, **When** the leader taps "Apagar" at the bottom and confirms, **Then** the full
   screen, the profile and the card show the initials.
4a. **Given** the full-screen photo and a leader with `manage`, **When** they tap "Baixar", **Then** the photo is saved
   to the device gallery in Pictures/IPB Castelo Branco (FR-026a).
5. **Given** the create or edit form, **When** the leader taps the camera on the photo at the top and
   picks an image, **Then** the form previews it and counts it as an unsaved change; **When** they save, **Then**
   the field changes are sent first and the photo is uploaded after; **When** they leave without saving, **Then**
   the current photo stays.
6. **Given** a file over 10 MB or not JPEG/PNG/WEBP/GIF, **When** the leader picks it, **Then** the app refuses it
   with a message before any upload, and the current photo stays.
7. **Given** a photo the server refuses or cannot find, **When** the screen shows it, **Then** the initials are
   shown instead, never a broken image.

---

### User Story 4 - Leader reads the edit history (Priority: P2)

A leader opens a member's history and reads, newest first, sentences like "Pr. João alterou Situação de Visitante
para Ativo — 25/09/2026 14:05".

**Why this priority**: Several leaders edit the same roll; the history answers "who changed this". Editing works
without it.

**Independent Test**: With a member whose history holds a creation, a photo change, a photo removal, field edits
(including ministries, dates, gender, validity and a cleared value) and an entry by a deleted account, open the
history and check each sentence.

**Acceptance Scenarios**:

1. **Given** a profile, **When** the leader opens "Histórico de alterações", **Then** the entries appear newest
   first, each with editor, sentence, date and time.
2. **Given** a creation entry, **Then** it reads "X cadastrou o membro".
3. **Given** photo entries, **Then** they read "X trocou a foto" or "X removeu a foto".
4. **Given** a field entry, **Then** it reads "X alterou <campo> de A para B", with the field named in Portuguese,
   dates as dd/MM/yyyy, gender spelled out (Masculino/Feminino), validity as Válido/Inválido and an empty value as
   "vazio".
5. **Given** an entry whose editor account was deleted, **Then** the editor reads "Usuário removido".
6. **Given** the profile, **Then** the history card shows the most recent entry as a one-line summary.

---

### User Story 5 - Leader deletes a member (Priority: P3)

A leader deletes a record — a duplicate or one created by mistake. Because the server deletes immediately and has
no recycle bin, the app makes the leader confirm by typing the member's name.

**Why this priority**: Rare. Marking a profile invalid covers most removals.

**Independent Test**: Open the delete dialog, check the button stays disabled until the exact name is typed,
confirm, and check the member is gone from the list.

**Acceptance Scenarios**:

1. **Given** a profile, **When** the leader taps "Excluir membro", **Then** a dialog warns that the record, its
   history and its photo will be deleted permanently, and asks the leader to type the member's name.
2. **Given** the dialog, **When** the typed text does not match the name (ignoring letter case and spaces at the
   ends), **Then** the delete button stays disabled.
3. **Given** a matching name, **When** the leader confirms, **Then** the member is deleted, the app returns to the
   list, and the member is no longer in it.
4. **Given** the delete fails, **When** the error returns, **Then** the dialog closes, the profile stays, and the
   error is shown.

---

### Edge Cases

- **Leader loses leader rights mid-session**: any members request refused as "not allowed" shows the server's
  message and takes the user out of the members area, back to the admin panel.
- **Session expired**: handled by the app's existing sign-in renewal; if renewal fails, the existing sign-in flow
  takes over.
- **Member deleted by another leader while open**: the profile, form or history shows "Este membro não existe
  mais" and returns to the list, which no longer shows the member.
- **Two leaders edit the same member**: last save wins (server rule); the app shows what the server returned after
  the save.
- **Status, role or ministry removed on the server while the form is open**: the server rejects the save; the
  message appears on that field and the choices are reloaded.
- **Stored dates that break the rules** (e.g. baptism before birth, created before the rules existed): shown as-is;
  the form only blocks saving when the leader edits and the result still breaks a rule.
- **Member without a birth date**: no age is shown ("Não informado"); the header shows only the gender, or nothing
  if that is also empty.
- **Partial birth dates**: the birth date is three independent parts — day, month and year — where day and month
  always go together. A member can have all three, day and month only (birthday known, year unknown), the year
  only, or nothing. 29 February is valid with no year or with a leap year.
- **Very long names**: cards cut them at two lines; the profile shows the full name.
- **Photo selected while offline**: the upload fails with the connection message and the current photo stays.
- **Opening the area without connection**: the list shows the connection error with "Tentar novamente"; no member
  data from a previous session is shown, because member data is not kept on the device (only photos are cached,
  encrypted — 012).
- **Sign-out**: all member data held in memory and the encrypted photo cache are discarded; signing in as another
  user never shows the previous user's members or photos.
- **Photo refused or rate-limited**: initials are shown in its place; opening the screen again tries again.
- **Empty roll**: the list shows an empty state inviting the leader to add the first member.

## Requirements *(mandatory)*

### Functional Requirements

**Access**

- **FR-001**: The members area MUST be reachable only from the admin panel's "Membros" card, which becomes active
  (blue accent, people icon) and opens the members list.
- **FR-002**: The members area MUST be offered only to users with at least `view` on the `members` scope (Admin
  and Liderança roles — see `specs/006-role-scoped-permissions`). The server remains the authority on every
  operation. Adding, editing and changing the photo need `manage`; deleting a member or removing a photo needs
  `owner` (Admin only). A refused **write** shows the message and keeps the user on the screen; only a refused read
  leaves the area (006 supersedes FR-003 for writes).
- **FR-003**: A "not allowed" answer from the server during any members operation MUST show the server's message
  and leave the members area.

**Status and validity**

- **FR-004**: Status names MUST be shown exactly as the server sends them; the app MUST NOT hardcode status names or
  offer filters based on them.
- **FR-005**: Validity MUST always be labelled "Perfil válido" / "Perfil inválido", never with words that could be
  confused with a status name.

**List**

- **FR-006**: The list MUST show every member record the server returns, valid and invalid, in the server's name
  order, as a two-column grid of cards.
- **FR-007**: Each card MUST show the photo (or the member's initials when there is none or it cannot be shown),
  the name (up to two lines), a status chip ("Sem situação" when empty) and, for invalid records, a "Perfil
  inválido" tag with the card faded.
- **FR-008**: The list MUST offer a name search applied on the device, ignoring accents and letter case, with no
  status or validity filters.
- **FR-009**: The list MUST show distinct loading, error-with-retry, empty-roll and no-search-result states, and a
  "Novo membro" button.
- **FR-010**: After a member is created, edited, has its photo changed or is deleted, the list MUST reflect the
  change when the leader returns to it.

**Profile**

- **FR-011**: The profile MUST show: photo (or initials), name, age computed from the
  birth date on the current day, gender, and status and role chips.
  The birth date MUST be shown as "dd/MM/yyyy" (full), "dd/MM" (year unknown), "yyyy" (year only) or "Não
  informado" (nothing). The age is exact for a full date, "N anos" with N the current year minus the birth year
  for a year only, "Desconhecida" for day and month only, and "Não informado" with no birth date.
- **FR-012**: The profile MUST show a "Dados pessoais" section (first name, last name, birth date, age, gender) and
  a "Vida na igreja" section (status, role, baptism date with "há N anos", ministries as chips).
- **FR-013**: Empty fields MUST read "Não informado", "Sem situação", "Sem cargo" or "Nenhum ministério" as
  appropriate — never blank.
- **FR-014**: The profile MUST have a "Perfil válido" switch that saves immediately, explains the effect of an
  invalid profile, and reverts with an error message if the save fails.
- **FR-015**: The profile MUST show a history card with the most recent change, the record's creation date
  ("Cadastrado em"), an edit action and a delete action.

**Create and edit**

- **FR-016**: Create and edit MUST use one form with: name (required, up to 255 characters), first name, last name,
  birth date, gender (Masculino / Feminino / Não informado), status, role, ministries (multiple choice), baptism
  date, validity.
- **FR-016a**: The birth date MUST be two optional inputs, not a date picker: the birthday (day and month, picked
  together — both or neither, with a "Limpar" action) and the year (a number). Clearing the year MUST keep day and
  month; clearing the birthday MUST keep the year. The month's day list follows the year: 29 February is offered
  with no year or a leap year.
- **FR-017**: Status, role and ministry choices MUST come from the server; any of them can be left empty.
- **FR-018**: Before sending, the form MUST reject: blank name; name over 255 characters; a year before 1 or after
  the current year; a day/month that is not a real date for the year (29/02 in a non-leap year); a full birth date
  after today; a baptism date in the future; a baptism before birth — compared with the full date when there is
  one, with the birth year when there is only a year, and not checked with day and month only. Each is shown on its
  field (birthday or year).
- **FR-019**: When editing, the app MUST send only the fields the leader changed; a save with no changes sends
  nothing and closes the form.
- **FR-020**: Server validation messages MUST appear on the matching field when the server names one, otherwise
  as a general form message, keeping everything the leader typed. A rule refusal with no `field_errors` shows the
  server's `detail` exactly as sent (it already names the offending value, in Portuguese).
- **FR-021**: Leaving the form with unsaved changes MUST ask for confirmation.
- **FR-022**: The create form MUST offer a photo the same way as the edit form (FR-022a). Its initials follow the
  name being typed. On "Salvar" the member is created first, then the photo is uploaded to the new id; if the upload
  fails, the member stays created and FR-022a's retry applies (saving again only retries the upload).
- **FR-022a**: The edit form MUST show the member's photo (or initials) at the top with a camera button to pick a
  new one. The picked photo is only previewed until "Salvar": the field changes are saved first, then the photo is
  uploaded. If the upload fails, the field changes stay saved, the form stays open with the picked photo and a
  message, and saving again retries the upload. A picked photo counts as an unsaved change (FR-021).

**Photo**

- **FR-023**: The leader MUST be able to add, replace and remove a member's photo. Picking and cropping MUST work
  the same way as the leader's own profile photo.
- **FR-023a**: On the profile the photo MUST NOT have a button of its own and a tap MUST NOT open the picker. A tap
  pops the photo in front of the screen: it grows out of the photo's own place into a large square above the
  middle of the screen, over a dimmed background, and shrinks back there when a tap outside or system back closes
  it; a tap on the square opens it full screen (dark background; top-left a "Trocar foto" button with a pencil
  that opens the picker directly; close button top-right; at the bottom, side by side and centred, "Apagar" (trash
  icon + text; `owner` and a photo) and "Baixar" (download icon + text; `manage` and a photo — FR-026a); system back
  also closes). Without a photo the initials stand in for it at every step and no bottom button is shown. Details in
  `specs/012-encrypted-session-photo-cache` (FR-016–FR-019).
- **FR-024**: The app MUST refuse, before uploading, files over 10 MB or not in JPEG, PNG, WEBP or GIF format, with
  a message.
- **FR-025**: Removing a photo MUST ask for confirmation.
- **FR-026**: Member photos MUST be fetched through the signed-in media path; a refused, missing or rate-limited
  photo MUST fall back to initials without an error dialog.
- **FR-026a**: A leader with `manage` on `members` MUST be able to save a member's photo, unencrypted, to the device
  gallery in Pictures/IPB Castelo Branco, from the full-screen viewer. The saved copy is the leader's responsibility:
  the app never removes or tracks it. Rules (permission on Android 9 and earlier, file name, messages, no partial
  file) in `specs/012-encrypted-session-photo-cache` (FR-020–FR-027).

**History**

- **FR-027**: The history MUST be its own screen listing entries newest first, each with editor, sentence and date
  and time (dd/MM/yyyy HH:mm, device local time).
- **FR-028**: Sentences MUST follow: creation — "X cadastrou o membro"; photo — "X trocou a foto" / "X removeu a
  foto"; other fields — "X alterou <campo> de A para B".
- **FR-029**: Field names MUST be shown in Portuguese (Nome, Primeiro nome, Sobrenome, Dia de nascimento, Mês de
  nascimento, Ano de nascimento, Nascimento, Sexo, Situação, Cargo, Ministérios, Batismo, Perfil); `birth_day` and
  `birth_year` values as the number, `birth_month` as the month name; dates as dd/MM/yyyy. Entries recorded before
  the birth date was split keep the field `birth_date` with a `YYYY-MM-DD` value: they read "Nascimento", and a
  year 0001 there reads "dd/MM". Gender as Masculino/Feminino; validity as
  Válido/Inválido; empty values as "vazio". Unknown field keys MUST still produce a readable sentence using the key.
- **FR-030**: An entry without an editor MUST show "Usuário removido".

**Delete**

- **FR-031**: Deleting MUST open a dialog that warns the record, its history and its photo are deleted permanently,
  and enables the delete button only when the typed text matches the member's name, ignoring letter case and
  surrounding spaces.
- **FR-032**: After a successful delete the app MUST return to the list without the deleted member.

**Data protection**

- **FR-033**: The members area MUST work online only. No member data may be written to the device's storage — no
  offline copy. Member photos MAY be cached on disk only encrypted, with their own key, under the rules of
  `specs/012-encrypted-session-photo-cache` (FR-008–FR-014); a photo saved with "Baixar" (FR-026a) is outside the
  app's control and not covered by this rule.
- **FR-034**: Member data MAY be kept in memory during the session and revalidated with the server so unchanged
  data is not downloaded again.
- **FR-035**: Signing out MUST discard all member data held in memory and erase the encrypted photo cache and its
  key from the device (012 FR-012).
- **FR-036**: App logs for these operations MUST NOT contain member data (names, dates, gender, status, role,
  ministries, photos); a member id is allowed.

**Errors**

- **FR-037**: Error messages from the server MUST be shown as the server words them when it provides one;
  otherwise the app's generic message for the error category is shown.
- **FR-038**: A member that no longer exists MUST show "Este membro não existe mais" and return to the list.

### Key Entities

- **Member (summary)**: what a list card needs — id, name, photo reference, status (or none), validity.
- **Member (record)**: the full profile — id, display name, first name, last name, birth date, gender, status,
  role, ministries, baptism date, validity, photo reference, creation time.
- **Status / Role / Ministry option**: a named choice from the server (id and name); managed only on the server.
- **History entry**: one change — editor (id and name, or none if the account was deleted), field, old value, new
  value (as text), time.
- **Member draft**: the form's working copy, used to validate locally and to work out which fields changed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A leader finds a member by name and opens their profile in under 15 seconds on a roll of hundreds of
  members.
- **SC-002**: A leader creates a member with name, dates, status and one ministry in under 2 minutes.
- **SC-003**: The list for a roll of 1,000 members loads in under 3 seconds on a typical mobile connection, and
  search narrows it with no noticeable delay.
- **SC-004**: 0 member records or member fields are found in the device's storage after using every screen of the
  area, and 0 readable member photos (outside those the leader saved with "Baixar") after signing out or a refused
  members request (012 SC-007).
- **SC-005**: 0 entry points to the members area are visible to non-leader users.
- **SC-006**: 100% of history entry types (creation, photo change, photo removal, each field, deleted editor) read
  as a Portuguese sentence with no raw codes such as `true`, `M` or ISO dates.
- **SC-007**: 0 members deleted without the leader typing the member's name.
- **SC-008**: 0 screens show a status name that the server did not send.

## Assumptions

- Leader = a user whose profile grants `view` or more on `members` (Admin or Liderança role, backend 012). The
  former `is_admin` flag no longer exists.
- The server contract is the one in the backend's `specs/010-members-management/contracts/admin-members-api.md`:
  full list without pagination, record, create, partial edit, delete, photo upload/removal, history, picker options;
  version tags on every read; errors as `{error_code, detail}` with Portuguese `detail`.
- The roll is small (hundreds), so the full list in one request and on-device search are enough.
- Statuses today are Ativo, Inativo and Visitante, managed in the Django admin; the app only reads them.
- The server stores the birth date as three nullable integers (`birth_day`, `birth_month`, `birth_year`) and
  enforces the rules of FR-018 itself; the app's checks only spare a round trip.
- Member photos are served by the protected media rules of `specs/004-protected-media-downloads`, which already
  restrict the `members/` area to `view` on `members`.
- Reusing the profile photo picking/cropping flow is acceptable for member photos (square crop).
- Validity changes from the profile switch take effect immediately, without an undo action; the history keeps the
  trace.
- The regular member list and birthdays that members see are unchanged by this feature.
- Out of scope: contact, address and family data; managing statuses, roles and ministries; offline access;
  exporting or sharing member data (saving a single photo with "Baixar" is the one exception — FR-026a).

## Dependencies

- Backend feature `010-members-management` deployed.
- Backend feature `009-protected-media-access` deployed with the `members/` rule (app side:
  `004-protected-media-downloads`).
