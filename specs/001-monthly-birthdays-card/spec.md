# Feature Specification: Monthly Birthdays Highlight Card

**Feature Branch**: `001-monthly-birthdays-card`

**Created**: 2026-06-20

**Status**: Implemented

**Input**: User description: "Monthly Birthdays highlight card on the home screen (CoreScreen). Fetches monthly birthdays from API, displays in existing Highlight carousel, shows member name and birth day ordered by day ascending."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Current Month Birthdays (Priority: P1)

As a church member, I want to see who has birthdays this month on the home screen so I can congratulate them and feel connected to the community.

**Why this priority**: Core value of the feature — without this, the feature has no purpose.

**Independent Test**: Can be fully tested by opening the app home screen and verifying birthday cards appear in the highlight carousel with correct names and days for the current month.

**Acceptance Scenarios**:

1. **Given** the current month has birthdays registered, **When** the user opens the home screen, **Then** a birthday highlight card displays all members with birthdays this month, showing each member's name and day of birth, ordered by day ascending.
2. **Given** the current month has no birthdays registered, **When** the user opens the home screen, **Then** the birthday highlight card shows an empty state message indicating no birthdays this month.
3. **Given** the user is not authenticated, **When** they open the home screen, **Then** the birthday card is not shown (feature requires authentication).

---

### User Story 2 - Offline Birthday Access (Priority: P2)

As a church member with unreliable connectivity, I want to see birthdays even when offline so the information is always available.

**Why this priority**: Enhances reliability but the feature is still valuable without offline support on first use.

**Independent Test**: Can be tested by loading birthdays once with connectivity, then enabling airplane mode and reopening the app to verify cached birthdays still appear.

**Acceptance Scenarios**:

1. **Given** birthdays were previously loaded and cached, **When** the user opens the app without internet, **Then** the cached birthdays for the current month are displayed.
2. **Given** no cached birthdays exist and the user is offline, **When** they open the app, **Then** the birthday card shows the empty state placeholder.

---

### Edge Cases

- What happens when the API returns an error (500, timeout)? The card shows cached data if available, otherwise shows the empty state — no error is surfaced to the user for this non-critical feature.
- What happens on the 1st of a new month? The cache holds the whole year, so the card switches to the new month straight from the cache — no request is needed for that.
- What happens with a cache saved by an older version (current month only, no `birth_month`)? It fails to decode, is deleted and the next refresh downloads the year.
- What happens when a member's name is very long? The name is truncated with ellipsis to maintain card layout.

### User Story 3 - Birthday Day Notification (Priority: P2)

As a church member, I want to receive a notification on the day of a member's birthday so I can congratulate them without needing to open the app.

**Why this priority**: Adds engagement value but the feature is fully usable without notifications.

**Independent Test**: Can be tested by granting notification permission, ensuring cached birthday data exists for today's date, and waiting for or manually triggering the daily Worker to verify a notification appears.

**Acceptance Scenarios**:

1. **Given** cached birthday data exists and today matches a member's birth day, **When** the daily Worker executes, **Then** a notification is shown with a personalized message including the member's name and gender-appropriate prefix.
2. **Given** multiple members have birthdays today, **When** the Worker executes, **Then** individual notifications are grouped under a summary notification showing the total count.
3. **Given** the user has denied notification permission, **When** the Worker executes, **Then** no notification is shown and no error occurs.
4. **Given** no cached birthday data exists (e.g., first install, never opened), **When** the Worker executes, **Then** it completes silently without error.

---

### User Story 4 - Year Birthdays Calendar (Priority: P2)

As a church member, I want to tap the birthdays card and see every month of the year with its birthdays, so I can look ahead (or back) beyond the current month.

**Why this priority**: Extends the card; the home screen is useful without it.

**Independent Test**: Tap the birthdays page of the highlight carousel and verify a screen opens with twelve month cards (January to December), each listing that month's birthdays.

**Acceptance Scenarios**:

1. **Given** the user is on the home screen, **When** they tap the birthdays page of the carousel (with or without birthdays this month), **Then** the year birthdays screen opens. Other carousel pages are not clickable.
2. **Given** the screen is open, **Then** it shows one card per month, January to December, in a two-column grid, and starts scrolled to the current month.
3. **Given** a month has birthdays, **Then** its card lists them with the same layout as the highlight card: a day badge followed by the name, ordered by day ascending.
4. **Given** a month has no birthdays, **Then** its card shows "Nenhum aniversariante".
5. **Given** the current month, **Then** its card header is highlighted; a birthday that falls on today has a highlighted day badge.
6. **Given** no cached data and the request fails, **Then** the screen shows the error message; when the cause is authentication (not a 403), it offers the "Conectar à sua conta" button.
7. **Given** the screen is open, **When** the user pulls to refresh, **Then** the year is fetched again.
8. Age is not shown.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST fetch the birthdays of the whole year (`?month=1-12`) from the API on home screen load, in a single request and a single cache shared by the card, the notification worker and the year screen. The card and the worker filter the current month locally.
- **FR-002**: System MUST display each birthday entry with the member's name and day of the month.
- **FR-003**: System MUST order birthday entries by day ascending (earliest day first).
- **FR-004**: System MUST show an empty state with a placeholder message when no birthdays exist for the current month.
- **FR-005**: System MUST cache birthday data locally for offline access following the existing snapshot cache pattern.
- **FR-006**: System MUST require user authentication to fetch birthday data.
- **FR-007**: System MUST display the birthday card within the existing highlight carousel on the home screen, replacing the current static placeholder.
- **FR-008**: System MUST send a daily notification for members whose birthday is today, using cached data (no network required).
- **FR-009**: System MUST personalize notification messages by gender (do/da/de prefix) and randomly select from a pool of message templates.
- **FR-010**: System MUST group multiple birthday notifications under a summary when more than one member has a birthday on the same day.
- **FR-011**: System MUST request POST_NOTIFICATIONS runtime permission on Android 13+ and degrade gracefully if denied.
- **FR-012**: The birthdays page of the highlight carousel MUST open the year birthdays screen when tapped.
- **FR-013**: The year birthdays screen MUST show the twelve months, January to December, each with its birthdays ordered by day, scrolled to the current month on open, and handle loading, error and pull-to-refresh.
- **FR-014**: The notification worker MUST match both month and day of today.

### Key Entities

- **Birthday**: Represents a member's birthday entry — contains the member's name (text), birth month (integer 1-12), birth day of the month (integer 1-31), and gender (MALE, FEMALE, or UNKNOWN). Birth year (and so age) is not exposed by the API.
- **Gender**: Enum representing member gender — mapped from API values `"M"`, `"F"`, or `null`.
- **BirthdayNotificationWorker**: Daily WorkManager worker that reads cached birthdays, filters for today, and dispatches notifications.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users see current month birthdays on the home screen within 2 seconds of opening the app (with network).
- **SC-002**: Birthday information is available offline after at least one successful load.
- **SC-003**: All birthdays for the current month are displayed in correct day-ascending order.
- **SC-004**: Empty months display a clear, friendly placeholder message.

## Assumptions

- Users are authenticated before viewing the home screen (birthday data requires JWT auth).
- The API endpoint `GET /ipbcb/members/birthdays/?month={M | M-M}` is available and returns the documented response format.
- The existing highlight carousel on CoreScreen supports dynamic content cards.
- Birthday data is read-only — no CRUD operations needed.
- The home card shows only the current month; the other months live in the year birthdays screen.
- No admin panel integration or member detail screens are in scope.
