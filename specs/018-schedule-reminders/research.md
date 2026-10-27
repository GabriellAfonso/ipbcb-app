# Research: Rota Reminders in the App

## R1 — Decoding `slots` in the domain

- **Decision**: `Json.parseToJsonElement(raw).jsonArray`, reading `date`, `service`, `time` as primitives; any
  exception or missing/invalid field → the whole message is `null`.
- **Rationale**: `PushMessage.parse` lives in `core/domain` (no Android). `org.json` is an Android class (stubbed in
  JVM tests); kotlinx.serialization is already on the classpath and pure Kotlin. Element parsing avoids adding
  `@Serializable` DTOs to the domain for three fields.
- **Alternatives**: a `@Serializable` DTO in `core/data` with mapping — more files for no gain; `org.json` — breaks
  JVM tests.

## R2 — Where the switch is read

- **Decision**: a `fun interface ScheduleRemindersPreference { suspend fun isEnabled(): Boolean }` in
  `core/domain/push/PushPorts.kt`, provided in `PushModule` from `ThemePreferences.scheduleNotificationsFlow.first()`.
- **Rationale**: `PushMessageHandler` tests build it with lambdas (`SessionPresenceProvider { … }`); a port keeps
  that style without a DataStore in the test. The key sits next to the birthday flag, as the spec asks.
- **Alternatives**: inject `ThemePreferences` directly — needs a DataStore fake in every handler test.

## R3 — Text and formats without importing the schedule feature

- **Decision**: `ScheduleNotificationText` (pure object in `core/data/push`) builds titles and lines. Time
  "19:30" → "19h30"; weekday from `LocalDate.dayOfWeek.getDisplayName(FULL, pt-BR)` lowercased with "-feira"
  removed ("quinta"); month from `Month.getDisplayName(FULL_STANDALONE, pt-BR)` lowercased ("novembro").
- **Rationale**: `MonthScheduleWhatsappFormatter.monthPtBr` and the "Próximo" card helpers are in
  `features/schedule`; `core` cannot import them. `java.time` gives the same Portuguese names.
- **Alternatives**: move the helpers to `core` — a refactor outside this feature.
- **Note**: `FULL_STANDALONE` may come back as a number on some JVM locale data; fall back to `FULL` when the result
  is all digits. Tests run on the JVM, so they check the real output.

## R4 — One notification per kind

- **Decision**: ids `3201` (week), `3202` (eve), `3203` (published), request code = id; channel id `schedule`, name
  "Escala", description "Lembretes de quando você está na escala".
- **Rationale**: same pattern as setlist (3001/3002) and access requests (3101/3102); the server sends at most one
  eve per day, so one id suffices.

## R5 — Expanded text

- **Decision**: week with 2+ slots and published always: `InboxStyle` with one line per slot (summary text = the
  collapsed text). Week with one slot and eve: `BigTextStyle` with the same text.
- **Rationale**: matches the spec; `InboxStyle` shows up to ~5–7 lines, enough for a month of one person's entries
  (at most a dozen; the system truncates beyond).

## R6 — Tap target and route

- **Decision**: `NotificationTarget.Schedule(date)` with `TARGET_SCHEDULE = "schedule"` + existing `EXTRA_DATE`. The
  inline route becomes `schedule?date={date}` (nullable string argument, default `null`); `AppRoutes.SCHEDULE` keeps
  `"schedule"` for the home navigation (Navigation Compose matches a route without the optional query), and
  `AppRoutes.schedule(date)` builds the deep one. `launchSingleTop` is not used: same as other targets.
- **Rationale**: optional query argument keeps every existing `navigate(AppRoutes.SCHEDULE)` unchanged.
- **Alternatives**: pass the date through a shared holder in core — hidden state; a second route — duplication.

## R7 — Choosing the tab from the date

- **Decision**: `ScheduleViewModel` reads `SavedStateHandle["date"]` once (`LocalDate` or `null`). In
  `mapSnapshotToUiState`, if the date is in the schedule's year/month, `initialTabIndex` = index of the first
  section (screen order) with a row on that day; else the existing `findNextSection` rule. `nextSection` (the
  "Próximo" card) is untouched.
- **Rationale**: the screen already keys `rememberSaveable` on `initialTabIndex`, so no UI change is needed. A date
  with two services → first in tab order (spec edge case).
- **Constructor**: `SavedStateHandle` added with a default `SavedStateHandle()` so existing tests keep compiling.

## R8 — Foreground

- **Decision**: rota messages are shown regardless of `AppForegroundState` (unlike `setlist_saved`).
- **Rationale**: spec FR-004 — no in-app surface shows them.
