# Feature Specification: Role-Scoped Permissions in the App

**Feature Branch**: `006-role-scoped-permissions`

**Created**: 2026-09-28

**Status**: Draft

**Input**: User description: "Adequar o app Android à feature 012 do backend (permissões por escopo com papéis), que
remove `is_admin` do `/profile` e quebra o painel de gestão atual. Fonte da verdade no backend:
`specs/012-feature-role-permissions/spec.md`, `contracts/profile-api.md` e `contracts/management-access.md`." (full
request — context, new profile shape, display rules per area, 403 handling, architecture constraints, app specs to
update, known limitation, out of scope — in the `/speckit-specify` invocation that created this directory)

## Context

Today the app decides who sees the management panel and the management buttons from a single yes/no in the user's
profile: `is_admin`. Backend feature 012 removes that flag and replaces it with **roles** that grant a **level per
scope**. The current app therefore breaks as soon as the backend ships: the panel disappears for everyone, and the
worship hub loses its create and edit buttons.

This feature teaches the app to read roles and levels and to show each person only what they can use. The backend
stays the only authority: the app hides, the backend authorizes. App and backend ship together; there is no support
for the old profile shape.

### Terms (from backend 012, not redefined here)

- **Levels**, hierarchical: `view` < `manage` < `owner`. Each includes the lower ones.
- **Scopes**: `members`, `schedule`, `songs`, `gallery`, `events`, `notices`, `reports.hymnal_history`.
- **Roles**: Admin (`admin`), Liderança (`leader`), Mídia (`media`). A user may hold several; the profile already
  carries the resulting level per scope.
- **Default required level per method**: read → `view`; create/change → `manage`; delete → `owner`. Some endpoints
  require more (backend 012, "Endpoint Classification").

### What the profile now carries

| Field         | Content                                                                                     |
|---------------|---------------------------------------------------------------------------------------------|
| `is_admin`    | Removed                                                                                     |
| `roles`       | List of `{id, name}`; empty when the user has no role                                       |
| `permissions` | Always the 7 scope keys; each value is `"view"`, `"manage"`, `"owner"` or `null` (no access) |
| `is_member`   | Unchanged — governs the common content of the app, independent of roles                     |

### Visibility matrix (app side)

What each area requires, and what each role therefore sees with the backend's initial levels.

| Surface                                   | Requirement                               | Admin | Liderança | Mídia |
|-------------------------------------------|-------------------------------------------|-------|-----------|-------|
| Panel entry ("Painel de Gestão")          | at least one role                         | yes   | yes       | yes   |
| Card Gestão do Louvor                     | `songs` ≥ `manage`                        | yes   | yes       | no    |
| Card Gerar Escala                         | `schedule` ≥ `manage`                     | yes   | yes       | no    |
| Card Membros                              | `members` ≥ `view`                        | yes   | yes       | no    |
| Card Relatórios                           | `reports.hymnal_history` ≥ `view`         | yes   | yes       | yes   |
| Cards Galeria / Eventos / Avisos (grey)   | `gallery` / `events` / `notices` ≥ `manage` | yes | yes       | yes   |
| Cards Marcar Presença / Notificações (grey) | holds the Admin role                    | yes   | no        | no    |
| Members: list, profile, history           | `members` ≥ `view`                        | yes   | yes       | —     |
| Members: add, edit, change photo          | `members` ≥ `manage`                      | yes   | yes       | —     |
| Members: delete member, remove photo      | `members` = `owner`                       | yes   | no        | —     |
| Hymnal report and service windows (read)  | `reports.hymnal_history` ≥ `view`         | yes   | yes       | yes   |
| Service windows: create, edit, delete, activate toggle | `reports.hymnal_history` = `owner` | yes | no   | no    |
| Collection settings screen ("Parâmetros de coleta") | `reports.hymnal_history` = `owner` | yes   | no        | no    |
| Worship hub: create/edit chord chart or lyrics | `songs` ≥ `manage`                   | yes   | yes       | no    |

"—" = the area is not reachable at all for that role.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Managers keep reaching the panel after the backend switch (Priority: P1)

A person who manages the church app today opens the new app version after backend 012 is live. The menu shows
"Painel de Gestão", the panel opens, and every card they used before is still there and working.

**Why this priority**: Without it, the release locks every current manager out of the panel — the backend no longer
sends the flag the app relies on.

**Independent Test**: Sign in as an Admin, a Liderança and a user without role against the 012 backend; the first two
see the menu entry and the panel, the third does not.

**Acceptance Scenarios**:

1. **Given** a signed-in user whose profile lists at least one role, **When** they open the side menu, **Then** the
   item "Painel de Gestão" is shown.
2. **Given** a signed-in user whose profile has an empty role list, **When** they open the side menu, **Then** no
   panel item is shown — even if they are a church member.
3. **Given** a user opens the panel, **When** it renders, **Then** its title reads "Painel de Gestão".
4. **Given** an Admin, **When** the panel renders, **Then** every card from today is shown, with the same enabled or
   grey state as today.
5. **Given** the app starts with a profile saved on disk in the old shape (with `is_admin`, without `roles`),
   **When** it boots, **Then** it does not crash, treats the user as having no role, and shows the panel entry only
   once a fresh profile arrives from the server and lists a role.

---

### User Story 2 - Each role sees only the cards it can use (Priority: P1)

A Liderança sees the management cards for songs, schedule, members and reports, but not the Admin-only placeholder
cards. A Mídia sees only Relatórios and the grey Galeria, Eventos and Avisos cards.

**Why this priority**: The whole point of backend 012 is separation between areas; showing a Mídia the Membros card
invites a guaranteed 403 on the most sensitive data in the app.

**Independent Test**: Sign in as each role and compare the visible cards with the visibility matrix.

**Acceptance Scenarios**:

1. **Given** a Liderança, **When** the panel renders, **Then** Gestão do Louvor, Gerar Escala, Membros, Relatórios and
   the grey Galeria, Eventos and Avisos cards are shown, and Marcar Presença and Notificações are not.
2. **Given** a Mídia, **When** the panel renders, **Then** only Relatórios and the grey Galeria, Eventos and Avisos
   cards are shown.
3. **Given** a user holding Liderança and Mídia, **When** the panel renders, **Then** the cards shown are those
   allowed by the combined levels the profile reports.
4. **Given** any user, **When** a card is shown, **Then** it keeps its current accent colour and enabled or grey
   state; permissions only decide whether it appears.

---

### User Story 3 - Members area respects the member scope level (Priority: P1)

A Liderança can list, open, add and edit members and change a member's photo, but sees no "Excluir membro" or
"Remover foto" button. An Admin sees both.

**Why this priority**: Member deletion is irreversible and is the main reason the safety tier exists.

**Independent Test**: As a Liderança, walk list → profile → edit → photo; confirm the delete and remove-photo actions
never appear. Repeat as Admin; confirm they appear and work.

**Acceptance Scenarios**:

1. **Given** a user with `members` at `view`, **When** they open the members area, **Then** list, profile and
   history are available, and add, edit, change photo, delete and remove photo are hidden.
2. **Given** a user with `members` at `manage`, **When** they open a member, **Then** add, edit and change photo are
   available, and "Excluir membro" and "Remover foto" are hidden.
3. **Given** a user with `members` at `owner`, **When** they open a member, **Then** every action is available,
   "Excluir membro" and "Remover foto" included.

---

### User Story 4 - A denied action explains itself and the app catches up (Priority: P2)

A Liderança's role is reduced in the Django admin while the app is open. Their next action is refused. The app shows
"Você não tem permissão para esta ação.", refreshes the profile, and the actions they lost disappear. If the refused
request was a read, they are taken out of the area; if it was a write, they stay on the screen.

**Why this priority**: Roles can change at any time on the server and the profile held by the app can be stale;
today a 403 shows a login message or ejects the user even from a failed write.

**Independent Test**: With the app open on a member's profile as Admin, demote the account to Liderança in the Django
admin, tap "Excluir membro": the message appears, the user stays on the profile, and the delete button disappears.

**Acceptance Scenarios**:

1. **Given** any management request refused with the backend's structured permission-denied response, **When** the
   error is shown, **Then** the text is the backend's message ("Você não tem permissão para esta ação.").
2. **Given** a 403 without a structured body, **When** the error is shown, **Then** the text is the app's own
   "no permission" message, never "Faça login para continuar.".
3. **Given** a structured permission-denied response, **When** it is received, **Then** the app fetches the profile
   again, and every screen updates what it shows to the new levels.
4. **Given** a read is refused (e.g. the member list, a member profile, the hymnal report), **When** the refusal
   arrives, **Then** the message is shown and the user leaves that area back to the panel.
5. **Given** a write is refused (e.g. delete member, save a member, save a service window), **When** the refusal
   arrives, **Then** the message is shown and the user stays on the current screen with their input intact.

---

### User Story 5 - Hymnal report configuration is Admin-only (Priority: P2)

A Liderança or Mídia opens Relatórios, reads the hymnal report and the list of service windows, but cannot create,
edit, delete or toggle a window, and never sees "Parâmetros de coleta".

**Why this priority**: The backend refuses every configuration write to non-owners; offering the buttons leads only
to refusals.

**Independent Test**: As Mídia, open the report and the windows screen; confirm the report loads, the windows list
loads read-only, and the settings entry is absent. As Admin, confirm all actions are present.

**Acceptance Scenarios**:

1. **Given** a user with `reports.hymnal_history` at `view`, **When** they open the report, **Then** it loads as
   today.
2. **Given** the same user, **When** they open "Janelas de culto", **Then** the list is shown without create, edit,
   delete or activate controls.
3. **Given** the same user, **When** they look for "Parâmetros de coleta", **Then** the entry does not exist.
4. **Given** a user with `reports.hymnal_history` at `owner`, **When** they open those screens, **Then** all current
   actions are present.

---

### User Story 6 - Worship hub edit buttons follow the songs scope (Priority: P2)

A Liderança browsing chord charts or lyrics sees the create and edit buttons, as an admin did before. A Mídia or a
member without role sees the content only.

**Why this priority**: Today those buttons depend on the removed flag and would vanish for everyone.

**Independent Test**: Open a chord chart as Liderança (buttons present) and as Mídia (buttons absent).

**Acceptance Scenarios**:

1. **Given** a user with `songs` at `manage` or above, **When** they open chord charts or lyrics, **Then** the
   create and edit buttons are shown.
2. **Given** a user with `songs` below `manage`, **When** they open the same screens, **Then** those buttons are not
   shown and reading works as today.

---

### Edge Cases

- **Unknown value**: a level other than `view`/`manage`/`owner`, an unknown scope key, or an unknown role id counts
  as no access. The app never grants more than it understands. A missing scope key counts as `null`.
- **Unknown role only**: a profile whose only role is unknown to the app still lists a role, so the panel entry
  appears; with no known level it shows no enabled card (see FR-012).
- **Role with every level null** (e.g. a role emptied in the Django admin): the panel entry appears because the role
  list is not empty; the panel shows no card and an empty-state message instead of a blank grid.
- **Old profile on disk**: treated as "no role" until the next successful profile fetch; it never crashes the app
  and is never interpreted through `is_admin`.
- **Profile refresh fails after a 403** (offline, server error): the message still shows; the stale levels stay
  until the next successful fetch; the backend keeps refusing, so nothing unsafe happens.
- **Several 403s in a row**: they must not trigger an unbounded number of profile fetches; one refresh in flight at a
  time is enough.
- **Role lost while inside the panel**: after the refresh, cards the user can no longer use disappear; the panel
  entry disappears from the menu if no role remains. A user already on an inner screen leaves it at their next
  refused read.
- **401 is not 403**: an expired or invalid session keeps today's behaviour (token refresh, then login); only 403 is
  treated as a permission refusal.
- **Membership vs role**: a Mídia who is not a member sees the panel but not member-only content; a member without
  a role sees member content but not the panel.
- **Logout**: roles and levels are cleared with the rest of the session; the next user never inherits them.
- **Known limitation — Gerar Escala for non-members**: the screen loads its member list from an endpoint that
  requires membership, so a Liderança who is not a church member gets a refusal there. Recorded; the fix belongs to
  the backend and is out of scope.

## Requirements *(mandatory)*

### Functional Requirements

**Profile**

- **FR-001**: The app MUST read `roles` and `permissions` from the profile and MUST NOT read or require `is_admin`.
- **FR-002**: The app MUST accept only the new profile shape; there is no fallback to `is_admin`.
- **FR-003**: Any unknown level, scope or role id MUST count as no access; a missing scope MUST count as no access.
- **FR-004**: A profile saved on disk in the old shape MUST NOT crash the app; it MUST be read as "no role, no
  levels" until the next successful profile fetch replaces it.
- **FR-005**: Roles and levels MUST be available from the moment the saved profile is loaded at startup, and MUST be
  updated whenever a fresh profile arrives.
- **FR-006**: Roles and levels MUST be cleared on logout and on session loss, like the rest of the profile.
- **FR-007**: `is_member` MUST keep governing the common content of the app exactly as today, independently of roles.
- **FR-008**: The app MUST NOT display the user's role or levels anywhere.

**Access decision**

- **FR-009**: Level comparison ("has at least level X on scope Y", "holds role Z") MUST be decided in one shared
  place available to the panel, the administration area and the worship hub, without those areas depending on each
  other.
- **FR-010**: Screens MUST receive ready-made decisions (e.g. "can delete", "can edit") and MUST NOT compare levels
  themselves.

**Panel**

- **FR-011**: The panel entry MUST appear in the side menu when the user is signed in and their role list is not
  empty; it MUST be labelled "Painel de Gestão", and the panel's title MUST be "Painel de Gestão".
- **FR-012**: Each panel card MUST be shown only when the visibility matrix allows it: Gestão do Louvor (`songs` ≥
  `manage`), Gerar Escala (`schedule` ≥ `manage`), Membros (`members` ≥ `view`), Relatórios
  (`reports.hymnal_history` ≥ `view`), Galeria / Eventos / Avisos (`gallery` / `events` / `notices` ≥ `manage`),
  Marcar Presença and Notificações (holds the Admin role).
- **FR-013**: Cards MUST keep their current order, accent colour and enabled/grey state; Galeria, Eventos, Avisos,
  Marcar Presença and Notificações stay grey and inert.
- **FR-014**: When no card is allowed, the panel MUST show an empty-state message instead of an empty grid.

**Members area**

- **FR-015**: List, profile and history MUST require `members` ≥ `view`.
- **FR-016**: Add member, edit member and change photo MUST be offered only with `members` ≥ `manage`.
- **FR-017**: "Excluir membro" and "Remover foto" MUST be offered only with `members` = `owner`.

**Hymnal report**

- **FR-018**: The report and the "Janelas de culto" list MUST require `reports.hymnal_history` ≥ `view`.
- **FR-019**: Creating, editing, deleting and activating/deactivating a service window MUST be offered only with
  `reports.hymnal_history` = `owner`; below that the windows screen is read-only.
- **FR-020**: The "Parâmetros de coleta" entry MUST be hidden unless `reports.hymnal_history` = `owner`.

**Worship hub**

- **FR-021**: The create and edit buttons for chord charts and lyrics MUST be shown only with `songs` ≥ `manage`.

**Refusals (403)**

- **FR-022**: A 403 carrying the backend's structured permission-denied error MUST show the backend's message.
- **FR-023**: A 403 without a structured body MUST show an app-authored "no permission" message and MUST NOT show
  the login message.
- **FR-024**: After a structured permission-denied 403, the app MUST fetch the profile again so every screen reflects
  the current levels; concurrent refusals MUST NOT cause more than one fetch at a time.
- **FR-025**: A 403 on a read MUST take the user out of the area it belongs to, back to the panel.
- **FR-026**: A 403 on a write MUST show the message and keep the user on the same screen, preserving what they
  entered. This replaces the current members-area rule that any 403 leaves the area.
- **FR-027**: A 401 MUST keep today's session handling; only 403 is a permission refusal.

**Specs**

- **FR-028**: In the same change, the app specs MUST be updated: `specs/core/spec.md` (profile contract table and
  drawer item), `specs/admin/spec.md` §2 and §5 (panel name and display rules, members 403 rule in §7.4),
  `specs/worshiphub/spec.md` (buttons follow the `songs` level), `specs/004-protected-media-downloads/contracts/
  media-download.md` (`members/` requires `view` on `members`), and every text in 005 that says "leader
  (`is_admin`)". `specs/constitution.md` MUST state how a 403 is shown, distinct from a 401.

### Key Entities

- **Role**: an assignment the user holds, identified by `admin`, `leader` or `media`, with a pt-BR display name. The
  app only uses it to know whether the panel is offered and whether Admin-only placeholder cards appear.
- **Scope level**: for each of the 7 scopes, the user's level (`view`, `manage`, `owner`) or no access. Drives every
  card and button decision.
- **Profile** (existing): gains roles and scope levels, loses the admin flag, keeps name, membership and photo.
- **Permission refusal**: a 403 answer, with or without the backend's structured message; classified as read or write
  by the action that caused it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the users who reached the panel before the backend switch still reach it, with no action on
  their part beyond updating the app.
- **SC-002**: For each role (Admin, Liderança, Mídia, none) and each surface in the visibility matrix, what the app
  shows matches the matrix in 100% of cases, each combination covered by an automated check.
- **SC-003**: A user holding only Liderança sees 0 delete or remove-photo actions and 0 hymnal configuration actions
  anywhere in the app.
- **SC-004**: A user holding only Mídia sees 0 entries leading to the membership roll.
- **SC-005**: 0 crashes at startup for users upgrading with an old-shape profile saved on the device.
- **SC-006**: After a role is reduced on the server, at most one refused action is needed before the app stops
  offering the actions the user lost.
- **SC-007**: 0 permission refusals are shown to the user as a login problem.

## Assumptions

- **Backend 012 ships together with this app version**; the profile always returns the new shape, including all 7
  scope keys. Compatibility with the old shape is intentionally dropped.
- **The profile's levels are already combined** across the user's roles by the backend; the app does not recompute
  levels from roles.
- **Admin-only placeholder cards** (Marcar Presença, Notificações) are decided by holding the Admin role because they
  have no backend scope; this is the only decision made from a role id rather than a level.
- **Read vs write** is decided by the action the user took (loading data vs submitting a change), consistent with the
  backend's method-based default.
- **Leaving the area on a read refusal** returns the user to the panel; if the refreshed profile no longer lists any
  role, the menu no longer offers the panel.
- **The empty-state text** of the panel is app-authored Portuguese copy, decided at implementation.
- **Existing surfaces** — panel layout, card colours, members privacy rules (online-only, memory-only), report
  behaviour — are unchanged except where this spec says otherwise.
- **Known limitation**: "Gerar Escala" depends on a member-only list; a Liderança who is not a member gets a refusal
  there until the backend changes. Recorded, not solved here.

## Out of Scope

- Assigning or removing roles from the app (Django admin only).
- Screens for Galeria, Eventos, Avisos, Marcar Presença and Notificações.
- Compatibility with the old profile shape.
- Showing the user's role anywhere in the app.
- Any backend change, including the Gerar Escala membership limitation.
