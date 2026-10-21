# Feature Specification: User Account

**Feature Branch**: `015-user-account`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "Conta do usuário no app (backend 021-user-account): reorganizar a ProfileScreen em
seções. Foto (menu Trocar foto / Remover foto com confirmação). Conta: nome editável (trim, 1-100 chars), Login
(username), 'No app desde {mês/ano}' de date_joined; campos novos aditivos no DTO com default. Ações: Trocar senha
(só com has_password; senha atual + nova 2x; salva o par de tokens da resposta imediatamente), Excluir conta (com
senha: senha + digitar EXCLUIR; sem senha: novo login Google pelo Credential Manager para id_token + digitar EXCLUIR;
ACCOUNT_DELETION_BLOCKED = 'Fale com a liderança'; após 204 limpa sessão e caches como no logout, sem chamar
logout/unregister). Remover o texto 'Membro: Ativo/Desativado'. Fora: e-mail, is_member rename, pedidos de acesso,
criar senha."

## Context

The profile screen shows only the photo, the name and "Membro: Ativo/Desativado". Google Play requires an app that
creates accounts to let people delete them from inside the app, and the backend now offers account deletion,
password change, name editing and three new read-only profile fields. This feature turns the profile screen into an
account screen with three sections — photo, account, actions — and removes the member status line (the next feature
replaces it with an access card).

### Source of truth on the backend (not redefined here)

- `backend/specs/021-user-account/contracts/account-api.md` — endpoints, fields, error codes and their order.
- `backend/specs/021-user-account/spec.md` — business rules (what deletion removes and keeps, lockout, throttle).

### Backend facts this feature relies on

- The profile carries the **login name** (`username`), whether the **account has a password** (`has_password`) and
  the **date the account was created** (`date_joined`, a calendar date in Brasília time). All read-only.
- The name is trimmed by the server; empty or longer than 100 characters is refused per field.
- **Changing the password** answers with a new pair of session tokens. The token used for the call stops working at
  once, and every other session of the account (other devices and this one's old pair) is signed out.
- **Deleting the account** is immediate and irreversible. An account with a password confirms with it; an account
  without one confirms with a fresh Google sign-in of the same account. After success the server has already
  forgotten the session and the push device: the app only forgets locally.
- A wrong password is a **form error, not a session error**: it never signs the person out.
- Superuser accounts cannot be deleted from the app ("Fale com a liderança da igreja").

### Relation to other specs

- `specs/profile/spec.md` — the profile domain spec, created by this feature (no domain spec existed).
- `specs/012-encrypted-session-photo-cache/` — the sealed session store whose tokens a password change replaces.
- `specs/011-sunday-setlist-push/` — push registration, which logout undoes on the server and deletion does not.

## Clarifications

### Session 2026-10-21 (with the app's Claude Code session)

- Q: Where does the app go after a deletion? → A: like logout, back to the home screen signed out (everything above
  it popped), with the existing short notice mechanism (toast) saying "Conta excluída".
- Q: How is the session cleared after a deletion? → A: by the same private cleanup body as logout, so a cache added
  later cannot be forgotten in one path. The only difference: deletion skips the server call that unregisters the
  push device (the server already removed it) and only forgets the device locally.
- Q: A concurrent request may fail with the old token during a password change and try to renew the session with a
  refresh token the server just revoked, signing the person out. → A: the session is cleared on a failed renewal only
  if the stored pair is still the one that failed; if a new pair was saved meanwhile, the request is retried with it.
- Q: How does the profile replace the tokens without depending on the auth feature? → A: through a core port that
  writes the session pair.
- Q: The Google sign-in lives inside the login screen. → A: extracted to a shared helper used by both, with the login
  screen's behavior unchanged. Cancelling the Google picker during deletion just closes it, with no error message.
- Q: What else depends on "Membro: Ativo/Desativado"? → A: nothing on screen; only the screen state loses it. The
  profile model keeps `is_member`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Delete my account (Priority: P1)

A signed-in person opens the profile, chooses "Excluir conta", reads what happens, types "EXCLUIR" and confirms who
they are — with their current password if the account has one, or by signing in with Google again if it does not.
The account is deleted, the app forgets the session and everything tied to it, and the person lands on the home
screen signed out.

**Why this priority**: Google Play blocks publishing without in-app account deletion.

**Independent Test**: With a password account, delete it with the right password and "EXCLUIR"; confirm the home
shows signed out, the profile photo and member-only data are gone, and signing in with the old credentials fails.
Repeat with a Google-only account through the Google confirmation.

**Acceptance Scenarios**:

1. **Given** an account with a password, **When** the person types the correct password and "EXCLUIR" and confirms,
   **Then** the account is deleted, the session and all session caches are cleared, the app returns to the home
   signed out and shows "Conta excluída".
2. **Given** the deletion dialog, **When** the confirmation text is not exactly "EXCLUIR" (case and surrounding
   spaces ignored), **Then** the confirm button stays disabled.
3. **Given** an account with a password, **When** the password is wrong, **Then** the dialog stays open with
   "Senha incorreta." under the password field and the person stays signed in.
4. **Given** an account without a password, **When** the person types "EXCLUIR" and confirms, **Then** the Google
   account picker opens; choosing the same account deletes it as in scenario 1.
5. **Given** an account without a password, **When** the person cancels the Google picker, **Then** the dialog stays
   as it was, with no error message, and nothing is deleted.
6. **Given** an account without a password, **When** the Google account chosen is a different one, **Then** the
   dialog shows "A conta Google não corresponde a esta conta." and nothing is deleted.
7. **Given** a superuser account, **When** deletion is confirmed, **Then** the dialog shows the server's message
   telling the person to talk to the church leadership, and nothing is deleted.
8. **Given** too many wrong passwords, **When** the lockout is reached, **Then** the dialog shows the lockout message
   and the person stays signed in.
9. **Given** no connection, **When** deletion is confirmed, **Then** the dialog shows the connection error and
   nothing changes locally.

---

### User Story 2 - Change my password (Priority: P2)

A person whose account has a password opens "Trocar senha", types the current password and the new one twice. The
password changes; this device stays signed in with a fresh session and every other device is signed out.

**Why this priority**: Useful and expected, but not a store requirement.

**Independent Test**: Change the password on a device signed in on two devices; the first keeps working (open a
members-only screen), the second is sent to the signed-out state on its next request; the old password no longer
signs in and the new one does.

**Acceptance Scenarios**:

1. **Given** an account with a password, **When** the profile opens, **Then** "Trocar senha" is offered; **Given** an
   account without one, **Then** it is not shown.
2. **Given** correct current password and matching new passwords of at least 6 characters, **When** the person
   confirms, **Then** the new session pair is stored before any other request, the dialog closes, "Senha alterada"
   is shown and the app keeps working signed in.
3. **Given** a wrong current password, **When** the person confirms, **Then** "Senha incorreta." appears under the
   current password field and the person stays signed in.
4. **Given** the new password and its confirmation differ, or the new one is shorter than 6 characters, **When** the
   person confirms, **Then** the server's message appears under the matching field.
5. **Given** other requests running while the password changes, **When** one of them fails with the old token,
   **Then** the person is not signed out: the request is retried with the new pair.
6. **Given** the lockout or no connection, **When** the person confirms, **Then** the dialog shows the message and
   the stored session is unchanged.

---

### User Story 3 - See and edit my account details (Priority: P2)

The profile shows the name under the photo (editable from the top bar menu) and, as the last line of the
screen, how long the person has used the app ("No app desde março de 2025"). The login name is not shown.

**Why this priority**: Makes the screen an account screen; the deletion and password actions live next to it.

**Independent Test**: Open the profile; check "No app desde"; edit the name to "  Ana Maria  " and see
"Ana Maria" on the screen and on the next app start.

**Acceptance Scenarios**:

1. **Given** a profile from the server, **When** the screen opens, **Then** it shows the name and "No app desde
   {mês} de {ano}" from the account creation date.
2. **Given** a profile cached before this feature (no username, no date), **When** the screen opens offline, **Then**
   it opens without error; the date line is hidden until the server answers.
3. **Given** the name editor, **When** the person saves a name with spaces around it, **Then** the saved name is
   trimmed and the screen shows it.
4. **Given** the name editor, **When** the name is empty or blank, **Then** saving is disabled; **When** it passes
   100 characters, **Then** the field does not accept more.
5. **Given** the server refuses the name, **When** saving, **Then** the server's message appears under the field and
   the old name stays.
6. **Given** "Membro: Ativo/Desativado", **When** the screen opens, **Then** it is no longer shown.

---

### User Story 4 - Remove my photo (Priority: P3)

Tapping the photo opens a menu with "Trocar foto" (the current crop-and-upload flow) and, when there is a photo,
"Remover foto". Removing asks for confirmation, then the placeholder returns.

**Why this priority**: Small privacy improvement; the change flow already exists.

**Independent Test**: With a photo, remove it; the placeholder shows at once and stays after restarting the app.

**Acceptance Scenarios**:

1. **Given** a photo, **When** the person taps it, **Then** the menu offers "Trocar foto" and "Remover foto";
   **Given** no photo, **Then** only "Trocar foto".
2. **Given** "Remover foto" confirmed, **When** the server accepts, **Then** the local photo is deleted, the
   placeholder shows and the profile is refreshed so the photo address is gone.
3. **Given** "Remover foto" cancelled, **Then** nothing changes.
4. **Given** the server refuses or there is no connection, **When** removing, **Then** the error is shown and the
   photo stays.

### Edge Cases

- Password change when the access token had already expired: the call renews it first (normal flow), then the
  response's pair replaces everything.
- The app is killed between the password change answer and the save: the old pair is dead; the next start fails to
  renew and lands signed out (acceptable; the person signs in with the new password).
- Deletion retried after a success that the app missed (timeout after the server deleted): the server answers 401,
  the renewal fails and the session is cleared — the person ends signed out, as intended.
- A Google account picker that returns a credential type the app does not know is treated as a cancel.
- Busy state: while a dialog's request runs, its buttons are disabled and it cannot be dismissed by tapping outside.
- A field error for a field the dialog does not show (e.g. a generic body error) appears as the dialog's message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The profile screen MUST show the photo and the name, with "Excluir conta" and "No app desde" in a footer
  pinned to the bottom and "Editar nome" and "Trocar senha" in the top bar's three-dot menu. "Membro: Ativo/Desativado"
  MUST NOT be shown.
- **FR-002**: Tapping the photo MUST open a menu with "Trocar foto" and, only when a photo exists, "Remover foto".
- **FR-003**: "Remover foto" MUST ask for confirmation, then delete the photo on the server, clear the local copy,
  show the placeholder and refresh the profile.
- **FR-004**: The name MUST be centered under the photo, edited from the top bar menu and not repeated; the login
  MUST NOT be shown; "No app desde {mês} de {ano}" MUST be the last line and hidden while unknown.
- **FR-005**: Editing the name MUST trim it, refuse blank, cap input at 100 characters, show the server's field
  message on refusal and refresh the profile on success.
- **FR-006**: The three new profile fields MUST be optional when reading a stored profile, so a profile saved before
  this feature still loads.
- **FR-007**: "Trocar senha" MUST be shown only when the account has a password and ask for the current password and
  the new one twice.
- **FR-008**: On a successful password change the app MUST store the new session pair before making any other
  request, and keep the person signed in.
- **FR-009**: A failed session renewal MUST clear the session only when the stored pair is still the one that
  failed; otherwise the request MUST be retried with the stored pair.
- **FR-010**: "Excluir conta" MUST require typing "EXCLUIR" and, depending on the account, the current password or
  a fresh Google sign-in of the same account.
- **FR-011**: Cancelling the Google picker MUST leave the dialog untouched with no error.
- **FR-012**: After a successful deletion the app MUST clear the session and every session cache (the same cleanup
  as logout), forget the push device locally without calling the server, return to the home signed out and show
  "Conta excluída".
- **FR-013**: Wrong password, Google mismatch, deletion blocked, lockout, throttle and validation errors MUST be shown
  in the dialog and MUST NOT sign the person out.
- **FR-014**: Every action MUST show a busy state while running and an error state on failure; a successful name,
  photo or password change MUST show a short confirmation.
- **FR-015**: The app MUST NOT show or edit the e-mail.

### Key Entities

- **Profile (me)**: name, member flag, photo address, access, member link, worship flags, and now login name,
  has-password flag and account creation date.
- **Session pair**: access and refresh tokens; replaced as a whole by a password change, cleared by deletion.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A person can delete their account from the profile in under 1 minute, in at most 3 steps after
  opening the profile (open action, confirm identity and type "EXCLUIR", confirm).
- **SC-002**: After a deletion, 0 items of the deleted session remain on the device (tokens, profile, photo, member
  data, gallery, schedule, birthdays, setlist).
- **SC-003**: After a password change, the device that changed it keeps working with no new sign-in in 100% of
  attempts, including with other requests in flight.
- **SC-004**: A wrong password in either dialog never signs the person out.
- **SC-005**: A profile cached by the previous app version opens without error after the update.

## Assumptions

- Month names are written in Portuguese, lowercase ("março de 2025").
- Password visibility toggles follow the login screen's.
- Success confirmations use the short toast the app already shows for similar notices.
- No "Criar senha" for Google-only accounts, no e-mail, no access card (next feature).
