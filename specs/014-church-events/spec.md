# Feature Specification: Church Events

**Feature Branch**: `014-church-events`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "Eventos da igreja no app. Substituir o placeholder `HighlightEvents()` do carrossel de
destaques por eventos reais e adicionar a gestão de eventos no Painel de Gestão. O backend já está implementado
(feature 020)..." (full request — highlight, detail, offline, management form, errors, out of scope — in the
`/speckit-specify` invocation that created this directory)

## Context

The home screen's highlight carousel has a third page, "Eventos", that is a fixed placeholder ("Nenhum evento esse
mês") with no data behind it, and the management panel has a grey "Eventos" card that does nothing. The church
announces its events (a Sunday lunch, a youth conference, a camp) through flyers passed around outside the app.

This feature makes the carousel show the church's upcoming events, each as its own page with the event's flyer, and
lets whoever runs events (today Admin, Liderança and Mídia) create, edit and delete them from the management panel.

### Source of truth on the backend (not redefined here)

- `backend/specs/020-church-events/contracts/events-api.md` — public and management endpoints, fields, ordering,
  validation errors, media access rules and the `events` scope in the profile.

### Backend facts this feature relies on

- **Event**: title, start date, optional end date (none = one-day event), optional start time, optional location,
  optional description, an image (always 16:9) and a visibility (`public` or members-only).
- **Active** = from creation until the end of its last day (end date, or start date when there is none), Brasília
  time. The public list returns only active events, already ordered by start date then start time; past events stay
  on the server and are listed only in management.
- **Visibility**: anyone, logged in or not, receives public events. A logged-in member also receives members-only
  events. A members-only event, and its image, are invisible (not found) to anyone else — never shown as a teaser.
- **Levels on `events`**: `view` reads the management list and detail, `manage` also creates and edits, `owner` also
  deletes. Admin, Liderança and Mídia hold `owner` today; the app follows levels, never role names.
- **Writes** carry the image as a file; on edit the image is optional and an optional field is cleared by sending it
  empty. The server re-encodes the image and replaces its address when a new one is sent; the old address stops
  answering.
- **Validation errors** come back per field (title, start date, end date, start time, visibility, image) with a
  Portuguese message. Deleting is permanent: the event and its image are gone, there is no trash.
- **Image rules**: JPEG, PNG or WEBP, up to 10 MB, at least 640×360, 16:9 within 2%.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Upcoming events in the highlight carousel (Priority: P1)

Anyone who opens the app — logged in or not — sees the church's next events in the highlight carousel, one page per
event, each with the event's flyer, title and date. Tapping a page opens the event's detail.

**Why this priority**: This is the whole point for the congregation; without it the feature has no visible value.

**Independent Test**: With three or more active public events on the server, open the app logged out and confirm the
carousel shows exactly three event pages, in date order, with the right images and dates; tap one and see its detail.

**Acceptance Scenarios**:

1. **Given** five active public events, **When** anyone opens the home screen, **Then** the carousel shows one page
   for each of the first three (earliest start date, then earliest time), in that order, after the birthdays and
   schedule pages.
2. **Given** no active event visible to the user, **When** the home screen opens, **Then** the carousel shows the
   current "Eventos — Nenhum evento esse mês" page in place of event pages.
3. **Given** a one-day event with a start time, **When** its page shows, **Then** it reads the day and month and the
   time (e.g. "14 nov · 19:30").
4. **Given** an event spanning several days, **When** its page shows, **Then** it reads the range ("14 a 16 nov";
   across months "30 nov a 2 dez"; across years the year is added to both ends).
5. **Given** an event page, **When** the user taps it, **Then** the event's detail opens.
6. **Given** an event whose last day has passed, **When** the list is next refreshed, **Then** its page is gone.

---

### User Story 2 - Event detail (Priority: P1)

The detail screen shows the whole flyer and everything known about the event, so the member knows when and where
to go.

**Why this priority**: The card has room only for title and date; place, time and description live here.

**Independent Test**: Open an event with every field filled and one with only the required fields; the first shows
image, title, date, time, location and description; the second shows only image, title and date, with no empty
labels.

**Acceptance Scenarios**:

1. **Given** an event with all fields, **When** the detail opens, **Then** it shows the image (whole, 16:9), title,
   date or range, time, location and description.
2. **Given** an event without time, location or description, **When** the detail opens, **Then** those rows do not
   appear at all.
3. **Given** the detail opened from the carousel offline, **When** the event was cached, **Then** it shows from the
   cached data and image.

---

### User Story 3 - Members-only events (Priority: P2)

A logged-in member also sees the events marked "Só membros"; nobody else ever sees them, and they vanish from the
device on logout.

**Why this priority**: Some events (assembly, members' meeting) are not for the public; leaking them is worse than
not having them.

**Independent Test**: With one public and one members-only event, open the app logged out (one event page), log in as
a member (two pages), log out (one page again, immediately, without network).

**Acceptance Scenarios**:

1. **Given** a members-only event, **When** a logged-out user or a logged-in non-member opens the home screen,
   **Then** it never appears.
2. **Given** a members-only event, **When** a logged-in member opens the home screen, **Then** it appears among the
   others in date order.
3. **Given** a member who sees a members-only event, **When** they log out, **Then** the event disappears from the
   carousel at once, also offline, and its saved data is no longer on the device.
4. **Given** a logged-out user who sees public events, **When** they log in as a member, **Then** the list is fetched
   again and members-only events appear.

---

### User Story 4 - Create and edit events (Priority: P1)

A user with `manage` or `owner` on `events` opens "Eventos" in the management panel, sees all events (active and
past), creates a new one with its flyer cropped to 16:9, and edits an existing one.

**Why this priority**: Without it the carousel stays empty; the events have to come from somewhere.

**Independent Test**: As a Mídia user, create an event with title, date and image; it appears in the management list
and, after a refresh, in the carousel. Edit it to add an end date and remove the time; the changes show in both.

**Acceptance Scenarios**:

1. **Given** a user with a level on `events`, **When** they open the management panel, **Then** the "Eventos" card
   is enabled and opens the events management list.
2. **Given** the management list, **When** it opens, **Then** it shows all events (filter "Todos"), most recent start
   date first, each with image, title, date and a "Só membros" mark when applicable; the filter switches between
   "Ativos", "Passados" and "Todos".
3. **Given** a user with `manage`, **When** they tap "Novo evento", fill title and start date and pick an image,
   **Then** the image opens in the crop screen locked to 16:9, and saving creates the event and returns to the list
   showing it.
4. **Given** the form, **When** the user saves without title, start date or image, or with an end date before the
   start date, **Then** the save is blocked and the problem is shown under the field, without calling the server.
5. **Given** the server refuses a field (e.g. image too small), **When** the save returns, **Then** the server's
   message shows under that field and every typed value is kept.
6. **Given** an existing event, **When** the user edits it and clears its location and time, **Then** saving removes
   them on the server; leaving the image untouched keeps the current image.
7. **Given** an existing event, **When** the user picks a new image and saves, **Then** the new image shows in the
   list, the detail and the carousel.
8. **Given** a user with only `view`, **When** they open the list and an event, **Then** they see everything but no
   create, edit or delete control.

---

### User Story 5 - Delete events (Priority: P2)

A user with `owner` deletes an event after confirming; it is gone for good.

**Why this priority**: Mistakes and cancelled events must be removable, but it is rarer than creating.

**Independent Test**: As Admin, delete an event, confirm, and see it gone from the management list and, after a
refresh, from the carousel.

**Acceptance Scenarios**:

1. **Given** a user with `owner`, **When** they choose "Excluir" on an event, **Then** a confirmation says the
   deletion is permanent; confirming deletes it and returns to the list without it.
2. **Given** a user with `manage` but not `owner`, **When** they look at an event, **Then** no delete control shows.
3. **Given** the confirmation, **When** the user cancels, **Then** nothing changes.

---

### Edge Cases

- **Offline at launch**: the carousel shows the last saved list of events and their cached images. Saved events whose
  last day has passed are dropped locally before showing, so an expired event never appears even without network.
  With nothing saved and no network, the "Nenhum evento esse mês" page shows; no error page in the carousel.
- **Image fails to load** (no cache, no network, or the image changed): the event page still shows title and date on a
  neutral background.
- **Members-only image after logout**: never reachable — the event itself is gone from the device (Story 3).
- **Session expired while browsing public events**: the app renews the session as it does elsewhere; if renewal fails
  the user is logged out, members-only events are dropped and the public list is fetched again.
- **Event deleted or turned members-only while the user has its detail open**: the detail keeps what it showed; the
  next refresh removes the event from the carousel.
- **Opening an event in management that no longer exists** (deleted on another device): the app returns to the list
  with "Este evento não existe mais." and refreshes the list.
- **Permission lost while in management** (403 permission denied): the existing permission-denied handling applies
  (profile refresh, "no permission" message); the card disappears from the panel if no level remains.
- **Crop cancelled**: the form keeps its previous image (or none) and nothing else changes.
- **Leaving the form with unsaved changes**: the app asks whether to discard them.
- **Saving twice**: the save button is disabled while a save is in progress, so a double tap never creates two events.
- **More than three active events**: only the first three appear in the carousel; the rest show as their time comes.
- **End date equal to start date**: treated as a one-day event in every label.

## Requirements *(mandatory)*

### Functional Requirements

**Highlight and detail**

- **FR-001**: The carousel MUST show one page per active event visible to the user, at most 3, in the server's order,
  in the position of the current "Eventos" page.
- **FR-002**: With no event to show, the carousel MUST show the existing "Eventos — Nenhum evento esse mês" page.
- **FR-003**: Each event page MUST show the event image filling the page, with title and date label over a gradient at
  the bottom; the date label MUST follow the formats of Story 1 (single day with optional time; range within a month,
  across months and across years).
- **FR-004**: Tapping an event page MUST open the event detail, showing the whole image, title, date label, and time,
  location and description only when present.
- **FR-005**: The events list MUST be available without login; when logged in, the request MUST carry the session so
  members receive members-only events.
- **FR-006**: The app MUST keep the last received events list and their images on the device and show them when
  offline, after dropping any event whose last day has passed (Brasília time).
- **FR-007**: On login and on logout the saved events list MUST be discarded and fetched again; after logout no
  members-only event or its image may remain visible or stored.
- **FR-008**: The list MUST be refreshed at app start, every time the home screen is opened or returned to (so a
  create, edit or delete made in management shows as soon as the user is back home), and whenever the user logs in
  or out.

**Management**

- **FR-009**: The "Eventos" card in the management panel MUST appear for any level on `events` (`view` or higher) and
  MUST open the management list.
- **FR-010**: The management list MUST show all events with a filter "Ativos" / "Passados" / "Todos" (default
  "Todos"), most recent start date first, each with image, title, date label and a "Só membros" mark when applicable.
- **FR-011**: Users with `manage` or `owner` MUST be able to create an event with title (required, up to 120
  characters), start date (required), end date (optional, not before start date), start time (optional), location
  (optional, up to 200 characters), description (optional), visibility ("Público" default, or "Só membros") and image
  (required).
- **FR-012**: Picking an image MUST open the crop screen locked to 16:9; only the cropped image is sent.
- **FR-013**: Users with `manage` or `owner` MUST be able to edit any field of an event; the image is optional on
  edit, and clearing an optional field MUST remove it on the server.
- **FR-014**: The form MUST check required fields, length limits and the date order before sending, and MUST show
  server field errors under the matching field, keeping the typed values.
- **FR-015**: Users with `owner` MUST be able to delete an event after a confirmation stating the deletion is
  permanent.
- **FR-016**: Create, edit and delete controls MUST appear only for the levels that allow them (FR-011, FR-013,
  FR-015); a `view` user sees list and detail read-only.
- **FR-017**: A permission refusal MUST follow the existing permission-denied handling; a not-found answer on an
  event MUST return to the list with "Este evento não existe mais.".
- **FR-018**: Every screen of this feature MUST show loading, content and error states; network and server errors use
  the app's generic messages with a retry.

### Key Entities

- **Event**: id, title, start date, optional end date, optional start time, optional location, optional description,
  image address, visibility (public or members-only). Management records add who created it and when it was created
  and updated.
- **Saved events list**: the last public list received for the current session state (logged out, or the logged-in
  user), with the moment it was received; replaced on each fetch, discarded on login and logout.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A logged-out user sees up to 3 upcoming public events in the carousel within the normal home loading
  time, and 0 members-only events in every test case.
- **SC-002**: After logout, 0 members-only events are visible or stored, checked with the device offline.
- **SC-003**: A manager creates an event (title, date, image) in under 2 minutes, and it shows in the carousel on the
  next home screen visit.
- **SC-004**: With the device offline and a previously loaded list, the carousel shows the saved events with images,
  and never an event whose last day has passed.
- **SC-005**: A user with only `view` on `events` sees 0 create, edit or delete controls; a user with `manage` sees 0
  delete controls.
- **SC-006**: Every server validation error appears under the field it belongs to, with no typed value lost.

## Assumptions

- The highlight carousel is shown to logged-out users; event pages follow the carousel's existing 5-second auto
  scroll and page order (birthdays, schedule, events).
- The "Nenhum evento esse mês" text stays as it is today even though the list is not month-bound.
- Events are few (tens per year), so the management list loads in one request without pagination.
- The flyer is produced 16:9 by whoever manages events; the crop screen exists for flyers in other shapes.
- Dates and times are shown in Brasília time, in Portuguese month abbreviations ("jan", "fev", ...).
- The management form uses the device's date and time pickers.
- Out of scope, recorded as future: push notification when an event is published, "Adicionar à agenda", a
  registration link, a "Todos os eventos" screen and a members-facing "Eventos passados" screen.
