# Feature Specification: Access Requests

**Feature Branch**: `016-access-requests`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "Acesso de membro e pedidos de acesso no app (backend 022-access-requests). Renomear
is_member -> memberAccess (lê member_access, fallback is_member; snapshot antigo abre). Novos campos do /me:
access_status, access_requests_blocked, data_correction_pending. Telas bloqueadas (Gallery, Schedule, Birthdays):
'Pedir acesso' com mensagem opcional; pendente = 'Pedido enviado. Aguardando a liderança.'; bloqueado = 'Pedidos de
acesso indisponíveis. Fale com a liderança.'; deslogado continua 'Conectar à sua conta'. Perfil: card de acesso
(Liberado / Aguardando / Não liberado + Pedir acesso), dados do rol só leitura e só em memória (GET api/me/member/),
'Solicitar correção' com mensagem obrigatória. Gestão: 'Perfis' (escopo profiles), abas Pendentes / Todos, detalhe com
aprovar, negar com motivo, revogar, bloquear, vincular ao rol. Push: novo pedido para líderes, aprovado/negado para
quem pediu. Nunca mostrar ou editar e-mail."

## Context

Today a signed-in person without member access sees "Disponível apenas para membros." on the gallery, the schedule
and the birthdays, with no way forward: they have to find a leader in person. Leaders, in turn, can only grant access
through the Django admin. The backend now has access requests, a "Perfis" management scope and the person's own roll
data. This feature brings all three to the app.

### Source of truth on the backend (not redefined here)

- `backend/specs/022-access-requests/contracts/access-api.md` — endpoints, fields, error codes and their order, push
  payloads.
- `backend/specs/022-access-requests/spec.md` — business rules (who may decide, the 24 h limit, what block and revoke
  do).

### Backend facts this feature relies on

- **Member access** in the profile is the *effective* access: the stored flag **or** the Admin/Liderança role. A
  leader never sees a locked screen. The old field name is still sent with the same value for compatibility.
- The profile says whether access is **granted, pending or none**, whether the person is **blocked from requesting**,
  and whether a **data correction is pending**.
- A request is **access** (optional message) or **data correction** (required message). One pending per type; one
  request of any type per rolling 24 hours, denied ones included. Refusals, in order: blocked → already granted →
  already pending → 24 h limit. Every refusal carries a Portuguese message meant for the screen.
- The person's **roll data** (role, ministries, birthday, baptism date, gender) comes from its own address, never
  cached by the server, and only when the account is linked to a roll member.
- **Perfis** lists app accounts (not the church roll). Viewing needs `view` on `profiles`; acting needs `manage`.
  Admin and Liderança have it; Mídia does not. In Perfis, access shows the **stored flag** plus whether a **role
  grants it anyway** (revoking changes nothing for them), plus who set the flag and when (the grant record, empty
  for flags set in the Django admin).
- Leaders can **grant** access directly, without a request: it sets the flag, records the grant, approves the
  account's pending access request in the same step and sends the person the same "Acesso liberado" push as an
  approval. Granting an account already flagged changes nothing. Revoking clears the grant record.
- "Approve" on a data correction only closes it (shown as **resolvida**). Blocking and revoking do not cancel pending
  requests. Revoke, block and link send no push.
- Push: leaders on a new request; the requester on approval and on denial (with the reason, when given). No name,
  roll field or e-mail travels in a push.

### Relation to other specs

- `specs/profile/spec.md` — the profile domain; this feature adds the access card and the roll data.
- `specs/006-role-scoped-permissions/` — scopes and the panel; `profiles` is a new scope.
- `specs/005-admin-members-management/` — the roll ("Membros"), reused to pick the member an account is linked to.
- `specs/011-sunday-setlist-push/` — push delivery and notification taps, reused for the three new pushes.

## Clarifications

### Session 2026-10-21 (decisions settled with the user before the spec)

- Q: Where is "Pedir acesso" offered? → A: on every locked screen (gallery, schedule, birthdays) for a signed-in
  person without access, and on the profile's access card. Signed-out people keep "Conectar à sua conta".
- Q: Is the roll data stored on the device? → A: no. It lives only in memory while the profile is open; never in the
  profile snapshot on disk, never in logs.
- Q: What does a blocked person see? → A: "Pedidos de acesso indisponíveis. Fale com a liderança." and no request
  buttons, on the locked screens and on the profile.
- Q: Who can link an account to a roll member? → A: anyone with `manage` on `profiles`, picking from the existing
  roll list.
- Q: Where does the "Novo pedido de acesso" push lead? → A: to Perfis, on the Pendentes tab.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ask for access from a locked screen (Priority: P1)

Ana created an account last week. She opens the gallery and sees "Disponível apenas para membros." with a "Pedir
acesso" button. She taps it, writes "Sou membro desde 2010." and sends. The screen now reads "Pedido enviado.
Aguardando a liderança." — on the gallery, the schedule and the birthdays alike.

**Why this priority**: It is the only way forward for a new account today, and it feeds everything else.

**Independent Test**: Sign in with an account without access, open the gallery, send a request, and check the
pending text on the three locked screens; check that a blocked account sees the blocked text and no button.

**Acceptance Scenarios**:

1. **Given** a signed-in account without access, not blocked, with no pending access request, **When** it opens a
   locked screen, **Then** it sees "Disponível apenas para membros." and "Pedir acesso".
2. **Given** that screen, **When** the person taps "Pedir acesso", writes an optional message (up to 1000 characters)
   and sends, **Then** the request is created, a short notice confirms it, and every locked screen shows "Pedido
   enviado. Aguardando a liderança." without a button.
3. **Given** a blocked account, **When** it opens a locked screen, **Then** it sees "Pedidos de acesso indisponíveis.
   Fale com a liderança." and no request button.
4. **Given** a signed-out person, **When** they open a locked screen, **Then** nothing changes: "Conectar à sua
   conta".
5. **Given** the server refuses the request (blocked, already granted, already pending, 24 h limit), **When** the
   answer arrives, **Then** the dialog shows the server's message, the person stays signed in, and the profile is
   refreshed so the screen reflects the real state.

---

### User Story 2 - Leaders decide requests in "Perfis" (Priority: P1)

Pr. João opens Gestão and finds "Perfis", next to "Membros". The Pendentes tab lists Ana's request with her message.
He opens it, sees her account (photo, name, login, since when she uses the app, access, linked member) and taps
"Aprovar". Ana leaves the list. On another request he taps "Negar" and writes "Procure o pastor após o culto.".

**Why this priority**: Without a decision the request is useless; together with story 1 it is the whole loop.

**Independent Test**: With a Liderança account, open Perfis, approve one request and deny another with a reason;
check the lists and the account details; check that a Mídia account does not see Perfis.

**Acceptance Scenarios**:

1. **Given** an account with `view` on `profiles`, **When** it opens Gestão, **Then** "Perfis" is offered; without
   it, "Perfis" is hidden.
2. **Given** Perfis, **When** it opens, **Then** it shows the tabs Pendentes (accounts with a pending request, oldest
   first) and Todos (every account, by name), each row with photo, name and a pending-request marker.
3. **Given** an account's detail, **Then** it shows photo, name, login, "No app desde", access ("Liberado" /
   "Não liberado", plus "Acesso pelo papel" when a role grants it), the linked roll member or "Sem vínculo", the
   pending requests with their messages, and the history newest first with status, who decided and when, and the
   denial reason.
4. **Given** a pending request and `manage`, **When** the leader taps "Aprovar", **Then** the request is approved
   (an access request grants access; a data correction is shown as "Resolvida") and the detail and lists refresh.
5. **Given** a pending request and `manage`, **When** the leader taps "Negar", optionally writes a reason (up to 500
   characters) and confirms, **Then** the request is denied and the detail and lists refresh.
6. **Given** only `view`, **When** the detail opens, **Then** no action is offered.
7. **Given** the request was already decided by someone else, **When** the leader acts on it, **Then** the server's
   message is shown and the detail refreshes.
8. No e-mail is shown or editable anywhere in Perfis.

---

### User Story 3 - The access card and roll data on the profile (Priority: P2)

Ana opens her profile. Under her name a card says "Acesso: Aguardando". After the approval it says "Liberado". Once a
leader links her account to the roll, a "Meus dados na igreja" section shows her role, ministries, birthday, baptism
date and gender, with "Solicitar correção" to tell the leaders something is wrong.

**Why this priority**: Gives the person a single place to see where they stand; the request loop already works from
the locked screens.

**Independent Test**: Open the profile with each access status and with/without a linked member; send a correction;
check that nothing of the roll data is written to the device.

**Acceptance Scenarios**:

1. **Given** the profile, **Then** the access card shows "Liberado", "Aguardando" or "Não liberado"; with "Não
   liberado" and not blocked it offers "Pedir acesso" (same dialog as story 1); blocked, it shows the blocked text.
2. **Given** an account linked to a roll member, **When** the profile opens, **Then** "Meus dados na igreja" shows
   cargo, ministérios, aniversário (day and month), batismo and gênero; unknown fields are hidden.
3. **Given** an account not linked, **When** the profile opens, **Then** the section is hidden.
4. **Given** the section, **When** the person taps "Solicitar correção", writes what is wrong (required, up to 1000
   characters) and sends, **Then** the request is created and the button becomes "Correção solicitada" (disabled).
5. **Given** a blocked account, **Then** neither "Pedir acesso" nor "Solicitar correção" is offered.
6. **Given** the profile was opened and closed, **Then** the roll data exists nowhere on the device: not in the
   profile snapshot, not in any file, not in logs.

---

### User Story 4 - Manage an account: grant, revoke, block, link (Priority: P2)

Pr. João welcomed Marta last Sunday; she installed the app but never opened a locked screen. He opens her account in
Perfis, turns on "Acesso de membro" and confirms: her phone shows "Acesso liberado". Carlos left the church: he turns
his switch off and confirms. Another
account keeps sending requests: he turns on "Bloquear pedidos". For Ana he taps "Vincular ao rol", searches "Ana" in
the roll and picks "Ana Souza".

**Why this priority**: Day-to-day upkeep; less frequent than deciding requests.

**Independent Test**: With `manage`, grant, revoke, block/unblock and link/unlink on test accounts; check the grant
note ("Liberado por …"), that granting clears a pending access request and that a role-granted account explains why
revoking changes nothing.

**Acceptance Scenarios**:

1. **Given** an account whose stored flag is off, **When** the leader turns "Acesso de membro" on and confirms,
   **Then** the access is granted, the detail shows "Liberado por {líder} em {data}" and a pending access request
   leaves Pendentes as approved.
2. **Given** an account whose stored flag is on, **When** the leader turns "Acesso de membro" off and confirms,
   **Then** the flag and the grant note are cleared and the detail refreshes.
3. **Given** an account whose access comes from a role, **Then** the revoke confirmation explains that the role
   grants access anyway.
4. **Given** an account, **When** the leader toggles "Bloquear pedidos", **Then** the block is saved and the detail
   refreshes; pending requests stay pending.
5. **Given** an account, **When** the leader taps "Vincular ao rol" and picks a member, **Then** the link is saved; a
   member already linked to another account shows the server's message; "Remover vínculo" unlinks after
   confirmation.

---

### User Story 5 - Push notifications for requests (Priority: P3)

When Ana sends her request, Pr. João's phone shows "Novo pedido de acesso"; tapping it opens Perfis on Pendentes.
When he approves, Ana's phone shows "Acesso liberado" and her locked screens open. When he denies with a reason, she
sees "Pedido de acesso negado" with the reason.

**Why this priority**: Speeds up the loop; without it people find out the next time they open the app.

**Independent Test**: Send a request, approve one and deny one, with the target devices in background; check the
notifications, the tap destination and the refreshed access.

**Acceptance Scenarios**:

1. **Given** a leader's device, **When** a request is created, **Then** it shows "Novo pedido de acesso" (or "Novo
   pedido de correção" for a data correction); a tap opens Perfis on Pendentes.
2. **Given** the requester's device, **When** the request is approved (or a leader grants access directly),
   **Then** it shows "Acesso liberado" (or
   "Correção resolvida") and the profile is refreshed.
3. **Given** the requester's device, **When** the request is denied, **Then** it shows "Pedido de acesso negado" (or
   "Pedido de correção negado") with the reason as body when there is one, and the profile is refreshed.
4. **Given** a push for an account that is no longer signed in on the device, **Then** it is ignored.

### Edge Cases

- An old profile saved on disk before this feature (old field name only, no status) opens without error; access
  status is derived from the old field until the server answers.
- An older server that does not send the new fields: the app reads the old field, treats status as unknown and hides
  the request buttons rather than offering a request the server cannot take.
- The request succeeds but the profile refresh fails: the screen shows the pending text from the request's own
  answer; the next refresh confirms it.
- Access is granted while a locked screen is open: after the profile refresh (push or resume) the screen loads its
  content.
- A leader acts on a request that another leader decided a moment ago: the server's message is shown, then the
  detail is reloaded.
- The 24 h limit: the server's message is shown as is (it says when the person can try again); nothing is retried.
- The roll data call fails (network): the section shows an inline error with "Tentar novamente"; the rest of the
  profile works.
- A leader opens their own account in Perfis: same detail; access shows "Acesso pelo papel".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST read member access from the new field, falling back to the old one, and MUST keep opening
  a profile saved on disk by a previous version.
- **FR-002**: The app MUST read the access status (granted / pending / none), the request block and the pending data
  correction from the profile; unknown values MUST hide the request buttons.
- **FR-003**: Every locked "Disponível apenas para membros." screen MUST, for a signed-in person, offer "Pedir acesso"
  when status is none and not blocked, show "Pedido enviado. Aguardando a liderança." when pending, and show
  "Pedidos de acesso indisponíveis. Fale com a liderança." when blocked. Signed-out behavior is unchanged.
- **FR-004**: "Pedir acesso" MUST open a dialog with an optional message of up to 1000 characters and send an access
  request; "Solicitar correção" MUST require a message of up to 1000 characters and send a data-correction request.
- **FR-005**: A refused request MUST show the server's message in the dialog, MUST NOT sign the person out, and MUST
  refresh the profile.
- **FR-006**: After any request is created the app MUST refresh the profile so every screen reflects the new status.
- **FR-007**: The profile MUST show an access card ("Liberado" / "Aguardando" / "Não liberado") under the name, with
  "Pedir acesso" or the blocked text as in FR-003.
- **FR-008**: When the account is linked to a roll member, the profile MUST show "Meus dados na igreja" (cargo,
  ministérios, aniversário dia/mês, batismo, gênero), hiding unknown fields, and "Solicitar correção" (or "Correção
  solicitada" when one is pending; nothing when blocked). Not linked → section hidden.
- **FR-009**: Roll data MUST be held in memory only — never written to disk (snapshot, cache, files) and never
  logged.
- **FR-010**: Gestão MUST offer "Perfis" only with `view` on `profiles`, with the tabs Pendentes and Todos.
- **FR-011**: The account detail MUST show photo, name, login, "No app desde", access (stored flag, "Liberado por
  {nome} em {data}" when the grant record exists, and "Acesso pelo papel"), the linked member, pending requests and the history; it MUST NOT show or edit an e-mail.
- **FR-012**: With `manage` on `profiles`, the detail MUST offer: approve and deny (optional reason up to 500
  characters) per pending request; an "Acesso de membro" switch over the stored flag — on grants access directly
  (confirmation saying the person is notified), off revokes it (confirmation; explained when access comes from a
  role);
  block/unblock requests; link to a roll member (picker over the existing roll with search) and unlink (with
  confirmation). With `view` only, no action is offered.
- **FR-013**: After each management action the detail and the lists MUST reflect the server's answer; server refusals
  MUST show the server's message.
- **FR-014**: Data corrections MUST be labeled as such everywhere, and an approved correction MUST read "Resolvida".
- **FR-015**: The app MUST show a notification for each of the three request pushes, with the texts of story 5; the
  leader's tap MUST open Perfis on Pendentes; the requester's pushes MUST refresh the profile; pushes for an account
  not signed in on the device MUST be ignored.
- **FR-016**: The first push of each kind MUST NOT need any new permission beyond the one already asked for the
  setlist push.

### Key Entities

- **Member access**: whether the account may see member content; with the status (granted / pending / none), the
  request block and the pending correction flag, it lives in the profile.
- **Access request**: type (access / data correction), message, status (pending / approved / denied), who decided,
  when, denial reason, creation time.
- **Managed account** (Perfis): an app account seen by leaders — name, login, photo, since when, stored access flag,
  role-granted access, block, linked roll member, pending requests and history.
- **Roll data** (own): role, ministries, birthday (day/month), baptism date, gender of the linked member. Memory only.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new account can ask for access from a locked screen in under 30 seconds, without leaving the screen.
- **SC-002**: A leader can find and decide a pending request in under 1 minute from opening Gestão.
- **SC-003**: After approval, the requester's locked screens open without reinstalling or signing in again — at the
  latest on the next profile refresh.
- **SC-004**: No roll data is found in any file on the device after the profile is opened (verified by inspecting the
  app's storage).
- **SC-005**: No screen of the feature shows or accepts an e-mail.

## Assumptions

- The backend 022 is deployed before this app version ships; with an older backend the request buttons stay hidden.
- Gender is shown as "Feminino" / "Masculino" from the roll's single-letter code; anything else is hidden.
- Birthday shows day and month only ("14 de março"), even if the year is known.
- The roll list for the link picker reuses the "Membros" list; a leader with `manage` on `profiles` but no access to
  "Membros" sees the server's refusal in the picker.
- Request history in the detail is shown in full (the backend returns all of it; volumes are small for one church).
- Notifications use the existing channel and permission from the setlist push.
