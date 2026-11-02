# Feature Specification: Password Recovery by E-mail

**Feature Branch**: `021-password-recovery`

**Created**: 2026-11-02

**Status**: Draft

**Input**: User description: "Add 'Esqueci minha senha' to the login, a required e-mail to password registration, and a
recovery e-mail section to the profile." (full request — current state, decisions and their reasons — in
`../TODO/specify_password_recovery_app.md`; decided in session `5ea87f01-d455-45d6-a6f0-13a62f8ae10c`, 2026-11-02.
Server side: `backend/specs/028-password-recovery`.)

## Context

A person who forgets the password of a username-and-password account has no way back today: those accounts have no
e-mail, and signing in with Google creates a different account, without their member access or history. This feature
gives every password account a recovery e-mail and a self-service way to set a new password with a code sent to it.

The e-mail is **only for recovery**. It is never a login and never appears as a way to sign in; this is what keeps the
known Google takeover closed. Accounts that sign in only with Google see none of this: Google handles their access.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recover a forgotten password (Priority: P1)

A signed-out person who forgot their password taps "Esqueci minha senha" on the login, types their e-mail, receives a
message with their username and a 6-digit code, types the code and a new password, and returns to the login to sign in.

**Why this priority**: This is the problem the feature exists to solve. A locked-out member today depends on the owner
changing the password by hand on the server.

**Independent Test**: With an account that has a recovery e-mail, follow the link on the login, request the code, type
it with a new password, and sign in with that password.

**Acceptance Scenarios**:

1. **Given** the login screen, **When** the person taps "Esqueci minha senha", **Then** a screen asks only for the
   e-mail and also says "Se você entra pelo Google, use o botão do Google".
2. **Given** a well-formed e-mail, whether or not an account uses it, **When** the person sends it, **Then** the app
   answers "Se houver conta com esse e-mail, enviamos um código" and moves to the code step.
3. **Given** the code step, **When** it opens, **Then** it explains that the e-mail shows the username and that, with
   several accounts on one address, each has its own code, so the person types the code of the account to recover.
4. **Given** a valid code and a valid new password typed twice, **When** the person submits, **Then** they return to
   the login screen with the message "Senha alterada", and must sign in normally.
5. **Given** a wrong, expired or already used code, **When** the person submits, **Then** the screen shows only
   "Código inválido ou expirado." and keeps what was typed.
6. **Given** a new password that breaks the rules or a confirmation that differs, **When** the person submits, **Then**
   the error appears under the field, with the same rules as registration, and the code is not spent.
7. **Given** no connection, **When** any step is submitted, **Then** a network error distinct from the code error is
   shown and the person can try again without losing what was typed.
8. **Given** the code step, **When** the person taps "Reenviar código", **Then** a new request goes out and the action
   stays disabled during a short countdown.

---

### User Story 2 - Required e-mail at registration (Priority: P2)

A person creating an account with username and password also types an e-mail, once, so the new account can always be
recovered.

**Why this priority**: Without it, every new password account grows the group that cannot recover. It is also required
by the server as soon as the server change is live.

**Independent Test**: Register with a valid e-mail and confirm the app signs in as today, with the e-mail shown on the
profile as "Não confirmado". Try again with an empty or malformed e-mail and confirm nothing is sent.

**Acceptance Scenarios**:

1. **Given** the registration form, **When** it opens, **Then** it has a required "E-mail" field, with an e-mail
   keyboard, typed a single time.
2. **Given** an empty or malformed e-mail, **When** the person submits, **Then** an error appears under the field and
   the form does not reach the server.
3. **Given** the server refuses the e-mail, **When** the response returns, **Then** its message appears under the field.
4. **Given** a valid form, **When** registration succeeds, **Then** the person enters the app as today: no code is asked.
5. **Given** "Criar com Google", **When** it is used, **Then** nothing changes and no e-mail is asked.

---

### User Story 3 - Manage the recovery e-mail in the profile (Priority: P3)

A signed-in person with a password sees their recovery e-mail and its status, confirms it with a code, adds one if they
have none, and changes it if it is wrong. This is the way to fix a typo made at registration.

**Why this priority**: The e-mail is typed once and unverified, so a mistake must be fixable. It also gives existing
accounts a place to add the address.

**Independent Test**: On a password account, add or change the e-mail with the current password, confirm it with the
received code, and see the status move from "Não confirmado" to "Confirmado". On a Google-only account, see nothing new.

**Acceptance Scenarios**:

1. **Given** an account with a password and an e-mail, **When** the profile opens, **Then** a section shows the address
   and "Confirmado" or "Não confirmado".
2. **Given** an account with a password and no e-mail, **When** the profile opens, **Then** the section offers
   "Adicionar e-mail".
3. **Given** a Google-only account, or one whose password state is unknown, **When** the profile opens, **Then** the
   section does not appear.
4. **Given** "Não confirmado", **When** the person taps "Confirmar", **Then** a code is sent and a dialog asks for it,
   with a resend countdown; a valid code changes the status to "Confirmado".
5. **Given** a wrong code in that dialog, **When** submitted, **Then** "Código inválido ou expirado." appears and the
   dialog stays open.
6. **Given** "Alterar" or "Adicionar e-mail", **When** the person submits the new address and their current password,
   **Then** the address is saved, the status is "Não confirmado" and the session is kept.
7. **Given** a wrong current password, **When** submitted, **Then** "Senha incorreta." appears under the password field
   and the person is not signed out.

---

### User Story 4 - One-time notice for existing accounts (Priority: P4)

An existing person with a password and no e-mail learns, once, why adding one matters, without being blocked.

**Why this priority**: Existing accounts stay unrecoverable until they add an address, but stopping anyone from using
the app, even during a service, is not acceptable.

**Independent Test**: Open the app on a password account with no e-mail: the card appears. Tap "Agora não", reopen:
it does not return, and the profile still offers the option.

**Acceptance Scenarios**:

1. **Given** a signed-in account with a password and no e-mail, **When** the app opens on the home, **Then** a card
   explains that an e-mail is what lets them recover the password, with "Adicionar e-mail" and "Agora não".
2. **Given** the card, **When** the person taps "Agora não", **Then** it disappears and never returns on this device.
3. **Given** the card, **When** the person taps "Adicionar e-mail", **Then** they reach the same form as the profile,
   which asks for the current password, and the card never returns on this device.
4. **Given** a Google-only account, an account with an e-mail, or an unknown password state, **When** the home opens,
   **Then** no card appears.
5. **Given** a small screen or a large system font, **When** the card is showing, **Then** the home buttons stay
   reachable.

---

### Edge Cases

- A person with no account, or with a Google-only account, types their e-mail: the same answer appears, and no screen
  reveals whether the address exists.
- Code requests past the server limit: recovery still answers the same; the profile "Confirmar" shows how long to wait.
- The profile section opens with an e-mail already confirmed elsewhere: "Confirmar" reports the current state and the
  status refreshes instead of showing an error.
- Saving the same address again keeps the confirmed status.
- The account loses its password state between loading and submitting (no usable password): the form shows a clear
  message and the section is refreshed.
- Too many wrong current passwords lock the account: the form shows the lockout message; nothing signs the person out.
- A family shares one address: recovery sends one code per account, and the app never says "e-mail já em uso".
- A signed-out person with a wrong address cannot recover; the owner solves it by hand on the server (accepted limit).
- A person going back from the code step keeps the typed e-mail, and can request a new code.
- An older cached profile, with no e-mail data, still opens.
- Logging out or deleting the account leaves no e-mail on the device.
- An old app version, once the server is live, cannot register until it updates; signing in keeps working.

## Requirements *(mandatory)*

### Functional Requirements

**Recovery**

- **FR-001**: The login screen MUST offer "Esqueci minha senha", near the password field, opening the recovery flow.
- **FR-002**: The flow MUST ask only for the e-mail to request a code, never the username.
- **FR-003**: The answer to a request MUST be the same whatever the server found, and MUST also point Google users to
  the Google button.
- **FR-004**: The code step MUST ask for the 6-digit code, the new password and its confirmation, MUST explain that the
  message carries the username and one code per account, and MUST reuse the visibility toggle and password rules of the
  existing password fields.
- **FR-005**: The flow MUST offer "Reenviar código", disabled during a short countdown.
- **FR-006**: A successful reset MUST return to the login screen with "Senha alterada" and MUST NOT sign the person in.
- **FR-007**: A wrong, expired or used code MUST show exactly one message, "Código inválido ou expirado.". Network
  errors MUST be distinct and offer a retry.
- **FR-008**: The flow MUST work for signed-out people: no session or expired session may break it.

**Registration**

- **FR-009**: Password registration MUST require an e-mail, typed once, with an e-mail keyboard, a local format check,
  and server errors shown under the field.
- **FR-010**: Registration MUST NOT involve a code, and "Criar com Google" MUST NOT change.

**Profile**

- **FR-011**: The recovery section MUST appear only for accounts known to have a password. An unknown state shows nothing.
- **FR-012**: The section MUST show the address with "Confirmado" or "Não confirmado", or "Adicionar e-mail" when empty.
- **FR-013**: "Confirmar" MUST send a code and open a dialog to type it, with a resend countdown.
- **FR-014**: "Alterar" and "Adicionar e-mail" MUST require the current password. A wrong password MUST be a form error
  ("Senha incorreta.") under the field and MUST NOT sign the person out.
- **FR-015**: After a change, the status MUST read "Não confirmado", unless the address is the one already stored.
- **FR-016**: The app MUST never show "e-mail já em uso": the address is not unique.

**Notice**

- **FR-017**: A signed-in account with a password and no e-mail MUST see a card on the home when the app opens,
  explaining the purpose, with "Adicionar e-mail" and "Agora não".
- **FR-018**: The card MUST appear only once per device: "Agora não" or "Adicionar e-mail" ends it for good. It MUST
  never block anything, and the option stays in the profile.
- **FR-019**: The card MUST NOT push the home buttons out of reach on a small screen or a large system font.

**Privacy and wording**

- **FR-020**: No screen may present the e-mail as a way to sign in.
- **FR-021**: The address MUST NOT be stored in plain text on the device. It may be cached encrypted, and MUST be erased
  on logout and on account deletion.
- **FR-022**: The address MUST never appear in logs, including logs the person can share from the app.
- **FR-023**: Older cached profiles without e-mail data MUST keep working.

### Out of Scope

E-mail as a login and any change to Google sign-in; notifying the old address when the e-mail changes; creating a
password for a Google-only account; reset by a leader; SMS or WhatsApp codes; deep links.

### Key Entities

- **Recovery e-mail**: an address attached to one password account, with a confirmed status. Not unique, not a login.
- **Recovery code**: a 6-digit, short-lived code, one per account, delivered by e-mail. Issued and checked by the server.
- **Notice seen**: a per-device record that the one-time card has been answered.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A person who has access to their recovery e-mail goes from "Esqueci minha senha" to signed in with a new
  password in under 3 minutes, with no help from the owner.
- **SC-002**: 100% of accounts created through password registration in the updated app have a recovery e-mail.
- **SC-003**: No recovery screen lets anyone tell whether an address has an account: the same answer in 100% of cases.
- **SC-004**: Google-only accounts see no new screen, field or card in any of the flows.
- **SC-005**: The notice appears at most once per device and never blocks a screen; the home stays fully reachable on
  the smallest supported screen with the largest system font.
- **SC-006**: Reading the device's plain storage and the shareable logs after using every flow reveals no e-mail
  address, and none remains after logout.

## Assumptions

- The server side (`backend/specs/028-password-recovery`) is implemented first, and its contract is the source of truth.
- Recovery works with an unconfirmed e-mail: receiving the code already proves access to the address.
- A code, not a link: the app has no App Links, and a link opened on a computer would not reach the app.
- The first e-mail on an existing account also needs the current password, so a stolen session cannot add its own
  address and take the account.
- "Seen" is per device, not per account.
- Release order: submit to Play with managed publishing, wait for approval, deploy the backend, then publish the app at
  once. The server refuses registration without an e-mail from the moment it is live. Done by the user.
