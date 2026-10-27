# Feature Specification: Rota Reminders in the App

**Feature Branch**: `018-schedule-reminders`

**Created**: 2026-10-27

**Status**: Draft

**Input**: User description: "Show the rota (escala) reminders the server now pushes to members who serve: a
Monday-morning summary of the week, a reminder the day before each service, and a notice when the month's rota is
published. The app only shows the notification, lets the user turn them off, and opens the Escala screen on tap."
(full request — current state, decisions and their reasons — in `../TODO/specify_schedule_reminders_app.md`;
decided in session `d5ff356e-47e7-428f-963b-df92f028c532`, 2026-10-27.)

## Context

The monthly rota (escala) says which member leads each church service on each date. Today it is only visible when
someone opens the Escala screen, and people forget they lead the Thursday prayer meeting. The server now reminds
each member who serves, by push: every Monday morning with the week's entries, the evening before each service,
and when the month's rota is published. This feature makes the app show those reminders, lets the user turn them
off, and takes them to the Escala screen on the right service when they tap.

### Source of truth on the backend (not redefined here)

- `backend/specs/024-schedule-reminders/spec.md` — who receives what and when (Monday 08:00, eve 19:00,
  publication 30 minutes after the first save), recipients, sending once, payload.

### Backend facts this feature relies on

- Three data-only message types, every value a string:
  - `schedule_week` with `slots` — the member's entries from that Monday to Sunday.
  - `schedule_eve` with `slots` holding exactly one entry — the day before it.
  - `schedule_published` with `month` (`YYYY-MM`) and `slots` — the member's entries in that month dated today or
    later.
- `slots` is a JSON-encoded array of `{date: "YYYY-MM-DD", service: service name, time: "HH:MM"}`, ordered by date
  and then start time. No member name and no id of any person travels.
- The server only sends to accounts linked to the rota member that have member access; the app does not check the
  link again. There is no opt-out on the server: the server always sends, the app drops what the user turned off.
- The service name in a slot is the same name the rota endpoint uses as the section title.

### Relation to other specs

- `specs/schedule/spec.md` — the Escala screen; gains the optional date that picks the opening tab.
- `specs/settings/spec.md` — section "Notificações" gains the "Escala" switch; reuses the notification-access
  request of §2.1.
- `specs/011-sunday-setlist-push/`, `specs/016-access-requests/` — push delivery, the signed-out rule, one channel
  per subject, notification taps. Reused as is.

## Clarifications

### Session 2026-10-27 (decisions settled with the user before the spec)

- Q: Own notification channel or the "Repertório" one? → A: its own, "Escala", so it can be muted alone.
- Q: Is the opt-out stored on the server? → A: no. Client-side only: the app drops the message when the switch is
  off. No new endpoint; the system channel is a second way to mute.
- Q: Default of the switch? → A: on. The church wants people reminded, and only people who serve get it.
- Q: Who sees the switch? → A: everyone, like "Aniversários" — harmless for people who never get the push.
- Q: One switch or one per kind? → A: one switch for the three kinds.
- Q: Does a reminder refresh the schedule? → A: no. The Escala screen re-reads (ETag) when opened.
- Q: Shown while the app is in the foreground? → A: yes, there is no in-app place where it would otherwise appear.
- Q: Gender wording? → A: by the recipient's gender (follow-up asked by the user once both sides were built). The
  server sends `gender` (`M`/`F`) when the member has one recorded; `F` says "a responsável" / "escalada", `M` or
  no gender keeps "o responsável" / "escalado".

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The evening before, the member is reminded (Priority: P1)

Carlos leads the Thursday prayer meeting this week. On Wednesday at 19:00 his phone shows "Amanhã: Quinta de
Oração" — "Você é o responsável · 29/10 às 19h30". He taps it and the Escala screen opens on the Quinta tab.

**Why this priority**: the eve reminder is what fixes the forgetfulness the feature exists for.

**Independent Test**: deliver a `schedule_eve` message to a signed-in device with the switch on; check the
notification text and that tapping it opens Escala on the matching tab; repeat with the switch off and signed out.

**Acceptance Scenarios**:

1. **Given** a signed-in user with the switch on and permission to post notifications, **When** a `schedule_eve`
   message for Thursday 29/10, "Quinta de Oração", 19:30 arrives, **Then** a notification in channel "Escala" shows
   title "Amanhã: Quinta de Oração" and text "Você é o responsável · 29/10 às 19h30".
2. **Given** that notification, **When** the user taps it, **Then** the Escala screen opens with the tab of the
   service that has an entry on 29/10.
3. **Given** the app is open in the foreground, **When** the message arrives, **Then** the notification is shown
   anyway.
4. **Given** the device has no network, **When** the message arrives, **Then** the notification is shown from the
   message alone.
5. **Given** a second eve message arrives later, **When** it is shown, **Then** it replaces the earlier eve
   notification instead of stacking.

---

### User Story 2 - Monday morning, the member sees their week (Priority: P2)

Ana serves on Tuesday and Sunday. On Monday at 08:00 she gets "Você está na escala desta semana", collapsed as "2
cultos nesta semana"; expanded, one line per entry: "Terça de Oração · terça 27/10 · 19h30", "Culto Dominical ·
domingo 01/11 · 19h00".

**Why this priority**: helps plan the week; the eve reminder already covers forgetting.

**Independent Test**: deliver `schedule_week` with one slot and with two slots; check the collapsed and expanded
texts and the tap target.

**Acceptance Scenarios**:

1. **Given** a `schedule_week` message with one slot, **When** it is shown, **Then** the title is "Você está na
   escala desta semana" and the text is that slot's line ("Quinta de Oração · quinta 29/10 · 19h30").
2. **Given** a `schedule_week` message with two or more slots, **When** it is shown, **Then** the collapsed text is
   "{n} cultos nesta semana" and the expanded view lists one line per slot, in the message's order.
3. **Given** that notification, **When** the user taps it, **Then** Escala opens on the tab of the first slot's date.
4. **Given** a new week message, **When** it is shown, **Then** it replaces last week's notification.

---

### User Story 3 - The month's rota is published (Priority: P2)

The deacon saves November's rota. Thirty minutes later Carlos gets "Escala de novembro publicada" — "Você está
escalado em 2 datas", and expanded, one line per entry.

**Why this priority**: tells people the rota exists as soon as it is final; the eve reminder reaches them anyway.

**Independent Test**: deliver `schedule_published` for `2026-11` with one and with three slots; check title, text,
expanded lines and tap.

**Acceptance Scenarios**:

1. **Given** a `schedule_published` message with `month` `2026-11` and one slot, **When** it is shown, **Then** the
   title is "Escala de novembro publicada" and the text is "Você está escalado em 1 data".
2. **Given** the same with three slots, **Then** the text is "Você está escalado em 3 datas" and the expanded view
   lists one line per slot, as in the week summary.
3. **Given** that notification, **When** the user taps it, **Then** Escala opens on the tab of the first slot's date
   when the loaded month has an entry on it; otherwise on the default tab (a month that has not started yet shows
   the current month — accepted).

---

### User Story 4 - The user turns the reminders off (Priority: P2)

Someone who does not want the reminders opens Configurações and turns off "Escala" under "Notificações". From then
on no rota reminder appears. Turning it back on asks for notification access if the app cannot post notifications.

**Why this priority**: the opt-out is required for a push sent by default; it is small and independent.

**Independent Test**: toggle the switch and deliver each message type; check nothing is shown while off and that
the switch survives an app restart.

**Acceptance Scenarios**:

1. **Given** a fresh install, **When** the user opens Configurações, **Then** "Escala" appears under
   "Notificações", below "Aniversários", on, with "Ativado".
2. **Given** the switch is off ("Desativado"), **When** any of the three messages arrives, **Then** nothing is shown.
3. **Given** the switch is off, **When** the user turns it on and the app cannot post notifications, **Then** the
   same notification-access request as "Aniversários" runs (`specs/settings/spec.md` §2.1).
4. **Given** the user changed the switch, **When** the app restarts, **Then** the switch keeps its value.
5. **Given** a signed-out user, **Then** the switch is still shown and can be changed.

---

### Edge Cases

- **Signed out**: every message is ignored (existing rule for all pushes).
- **Malformed message**: `slots` that is not valid JSON, an empty list, a slot with a missing or invalid date, a
  missing service name or a time not in `HH:MM`; a `schedule_eve` with more than one slot; a `schedule_published`
  with a missing or invalid `month`: the message is ignored, nothing is shown, nothing fails.
- **Unknown extra fields** in a slot or the message: ignored.
- **No permission to post notifications**: nothing is shown; no prompt is raised from a push.
- **Channel "Escala" muted by the user in the system**: the system hides it; the app does nothing special.
- **Tap while signed out or without member access**: Escala opens and shows its own placeholders; no extra check.
- **Tap on a date not in the loaded month** (cached snapshot of another month, or a future month): the default tab
  rule applies.
- **A date with entries in two services**: the first tab (in the screen's tab order) with an entry on that date
  opens.
- **Tap before the app finished starting**: the target waits until boot is ready, like other notification taps.
- **Empty service time in the rota**: not possible in a slot (time is required); a slot without a valid time is
  malformed.

## Requirements *(mandatory)*

### Functional Requirements

**Messages**

- **FR-001**: The app MUST recognize the three message types `schedule_week`, `schedule_eve` and
  `schedule_published`, each carrying a list of slots (date, service name, start time) read from `slots`, and, for
  `schedule_published`, the month.
- **FR-002**: A message whose `slots` is not a valid non-empty list of well-formed slots, a `schedule_eve` with
  other than exactly one slot, or a `schedule_published` without a valid month MUST be ignored.
- **FR-003**: Messages MUST be ignored while signed out, and while the "Escala" switch is off.
- **FR-004**: A recognized message MUST be shown at once from its own content, with no network call, both in the
  background and in the foreground.
- **FR-005**: Receiving a rota message MUST NOT trigger any refresh of the schedule.

**Notifications**

- **FR-006**: The three notifications MUST use their own channel named "Escala", separate from every other
  channel, so the user can mute them alone.
- **FR-007**: Times MUST be shown as "19h30" (hours and minutes, "h" between; "19h00" for the full hour), dates as
  dd/MM, and weekdays, where shown, as the lowercase day name without "-feira" used on the Escala screen's "Próximo"
  card ("terça", "quinta", "domingo").
- **FR-008**: The week notification MUST have title "Você está na escala desta semana"; with one slot, text
  "{service} · {weekday} {dd/MM} · {time}"; with more, collapsed text "{n} cultos nesta semana" and one line per
  slot, in the message's order, when expanded.
- **FR-009**: The eve notification MUST have title "Amanhã: {service}" and text "Você é o responsável · {dd/MM} às
  {time}" ("a responsável" when the message's `gender` is `F`).
- **FR-010**: The published notification MUST have title "Escala de {month name in lowercase} publicada", text
  "Você está escalado em {n} data" (n = 1) or "… {n} datas" (n > 1) ("escalada" when `gender` is `F`), and,
  expanded, one line per slot as in FR-008.
- **FR-010a**: `gender` is optional on all three kinds: `M`, absent or any other value keeps the masculine wording
  and never invalidates the message.
- **FR-011**: Each kind MUST have one fixed notification identity, so a newer notification of the same kind
  replaces the older one; different kinds do not replace each other.
- **FR-012**: Nothing MUST be shown without permission to post notifications, and a push MUST never ask for it.

**Opening**

- **FR-013**: Tapping any of the three notifications MUST open the Escala screen, carrying the date of the
  message's first slot.
- **FR-014**: When a date is carried and the loaded month has an entry on that date, the Escala screen MUST open on
  the first tab whose service has an entry on that date; otherwise it MUST use the existing rule (the next
  service). The date MUST NOT change the "Próximo" card nor the highlighted row.
- **FR-015**: Opening Escala from the home screen (no date) MUST behave exactly as today.

**Settings**

- **FR-016**: Configurações MUST show a switch "Escala" under "Notificações", below "Aniversários", with
  "Ativado"/"Desativado", on by default, visible to everyone.
- **FR-017**: The switch value MUST persist on the device across restarts, and nowhere else.
- **FR-018**: Turning the switch on MUST run the same notification-access request as "Aniversários"
  (`specs/settings/spec.md` §2.1).

### Key Entities

- **Rota slot**: one rota entry as a reminder carries it — date, service name, start time (hours and minutes).
- **Rota reminder**: one of the three kinds (week, eve, published) with its ordered slots; the published one also
  carries the month (year and month).
- **Rota reminder preference**: on/off, per device, default on.
- **Escala tap target**: the Escala screen plus the date that picks its opening tab.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every well-formed rota message received while signed in with the switch on and permission granted is
  shown within 1 second, online or offline.
- **SC-002**: No rota notification is ever shown while the switch is off or the user is signed out.
- **SC-003**: Tapping an eve or week notification opens Escala on the service of that date in 100% of cases where
  the loaded month contains the date.
- **SC-004**: A malformed message never crashes the app nor shows a notification.
- **SC-005**: Members can mute the rota reminders without muting any other notification of the app.

## Assumptions

- The backend contract is `backend/specs/024-schedule-reminders/spec.md` (Payload section). It was still a draft
  without code when this spec was written; the field names, the `slots` encoding and the `HH:MM` time are taken from
  it and must be re-checked against the backend's `contracts/push-messages.md` once it exists.
- Push delivery, token registration and the signed-out rule come from `specs/011-sunday-setlist-push/` unchanged.
- The service name in a slot matches the Escala section titles, so matching by date is enough; the date alone picks
  the tab.
- Month names are the Portuguese ones already used by the schedule's share text ("novembro").
- Reminder timing, recipients and sending once are the server's job; the app schedules nothing on the device.

## Out of Scope

- Any reminder scheduling on the device.
- Refreshing the schedule snapshot on push.
- Showing other months on the Escala screen.
- Per-kind switches (week / eve / published).
- Notifying leaders about the rota, or anyone not on it.
- Storing the opt-out on the server.
