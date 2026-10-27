# Tasks: Rota Reminders in the App

**Input**: Design documents from `specs/018-schedule-reminders/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui.md

**Tests**: included — the project requires happy path + one error per unit (`CLAUDE.md`, Tests).

Paths are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and `app/src/test/java/com/ipb/castelobranco/`
(test) unless they start with `specs/`.

## Phase 1: Setup

None — no new dependency or module.

## Phase 2: Foundational (blocks every story)

- [X] T001 Add `ScheduleSlot(date: LocalDate, service: String, time: LocalTime)` and `ScheduleWeek`, `ScheduleEve`,
  `SchedulePublished(month: YearMonth, slots)` to `PushMessage`, with `KEY_SLOTS`, `KEY_MONTH`, the three type
  constants and `slots` parsing via `Json.parseToJsonElement` (malformed, empty, eve ≠ 1 slot, bad month → `null`)
  in main `core/domain/push/PushMessage.kt`
- [X] T002 [P] Tests for T001 (three valid payloads, order kept, bad JSON, empty list, bad date, blank service,
  bad time, eve with two slots, bad/missing month, unknown extra fields ignored) in test
  `core/domain/push/PushMessageTest.kt`
- [X] T003 [P] Add `schedule_notifications_enabled` key, `scheduleNotificationsFlow` (default `true`) and
  `setScheduleNotifications` in main `core/data/local/ThemePreferences.kt`
- [X] T004 Add `fun interface ScheduleRemindersPreference { suspend fun isEnabled(): Boolean }` in main
  `core/domain/push/PushPorts.kt` and provide it from `ThemePreferences.scheduleNotificationsFlow.first()` in main
  `core/di/PushModule.kt`
- [X] T005 Add pure `ScheduleNotificationText` (slot line, time "19h30", weekday without "-feira", month name,
  titles and texts per `contracts/ui.md`) in main `core/data/push/ScheduleNotificationText.kt`
- [X] T006 [P] Tests for T005 in test `core/data/push/ScheduleNotificationTextTest.kt`
- [X] T007 Add `ScheduleNotifier` (`showWeek`, `showEve`, `showPublished`) and `ScheduleNotifications` (channel
  `schedule` / "Escala", ids 3201/3202/3203, InboxStyle for lists, permission check, tap intent with
  `TARGET_SCHEDULE` + `EXTRA_DATE` of the first slot) in main `core/data/push/ScheduleNotifications.kt`; bind it in
  main `core/di/PushModule.kt`
- [X] T008 Add `NotificationTarget.Schedule(date)` and `TARGET_SCHEDULE = "schedule"` (invalid date → `null`) in main
  `core/presentation/navigation/NotificationTarget.kt`

## Phase 3: User Story 1 — Eve reminder (P1) 🎯 MVP

**Goal**: the eve push is shown (foreground too, offline) and its tap opens Escala on the service of that date.

**Independent test**: handler test with a `schedule_eve` payload; ViewModel test with a date in the loaded month.

- [X] T009 [US1] Inject `ScheduleNotifier` and `ScheduleRemindersPreference` in `PushMessageHandler`; for the three
  schedule messages return when the preference is off, otherwise show (no foreground check, no refresh) in main
  `core/data/push/PushMessageHandler.kt`
- [X] T010 [P] [US1] Handler tests: eve shown in background and foreground, ignored when switch off, ignored signed
  out, week and published dispatched, no other notifier touched, in test `core/data/push/PushMessageHandlerTest.kt`
- [X] T011 [US1] Add `SCHEDULE_ARG_DATE`, `SCHEDULE_ROUTE = "schedule?date={date}"` and `schedule(date)` in main
  `core/presentation/navigation/AppRoutes.kt`; register the inline composable with the nullable `date` argument and
  handle `NotificationTarget.Schedule` (navigate to `AppRoutes.schedule(date)`) in main
  `core/presentation/navigation/AppNavHost.kt`
- [X] T012 [US1] Read the date from `SavedStateHandle` in `ScheduleViewModel` and pick `initialTabIndex` as the first
  section with a row on that day when the date is in the loaded year/month; otherwise keep `findNextSection` in main
  `features/schedule/presentation/viewmodel/ScheduleViewModel.kt`
- [X] T013 [P] [US1] ViewModel tests: date in month picks its section, date with two services picks the first tab,
  date of another month keeps the default, no date keeps the default, in test
  `features/schedule/presentation/viewmodel/ScheduleViewModelTest.kt`

## Phase 4: User Story 2 — Week summary (P2)

**Goal**: week notification with one line or "{n} cultos nesta semana" plus lines.

- [X] T014 [US2] Verify week text/expanded lines in `ScheduleNotifications.showWeek` match `contracts/ui.md`
  (covered by T005–T007) in main `core/data/push/ScheduleNotifications.kt`

## Phase 5: User Story 3 — Published (P2)

**Goal**: "Escala de {mês} publicada" with the date count and lines.

- [X] T015 [US3] Verify published title/count/lines in `ScheduleNotifications.showPublished` match
  `contracts/ui.md` (covered by T005–T007) in main `core/data/push/ScheduleNotifications.kt`

## Phase 6: User Story 4 — Opt-out switch (P2)

**Goal**: "Escala" switch under "Notificações", default on, persisted, permission request on enable.

- [X] T016 [US4] Add `scheduleNotificationsFlow` / `setScheduleNotifications` to main
  `features/settings/domain/repository/SettingsRepository.kt` and main
  `features/settings/data/repository/SettingsRepositoryImpl.kt`
- [X] T017 [US4] Add `scheduleNotifications` to the UI state and `toggleScheduleNotifications()` in main
  `features/settings/presentation/viewmodel/SettingsViewModel.kt`
- [X] T018 [US4] Add the "Escala" switch below "Aniversários" (turning on runs `requestNotificationAccess()`) in main
  `features/settings/presentation/screens/SettingsScreen.kt`
- [X] T019 [P] [US4] Update any settings test fakes of `SettingsRepository` and add a toggle test in test
  `features/settings/` (if a ViewModel test exists)

## Phase 7: Polish

- [X] T020 [P] Update `specs/schedule/spec.md` (reminders, opening date picks the tab) and `specs/settings/spec.md`
  (Escala switch)
- [X] T021 Run `./gradlew :app:testDebugUnitTest` and `:app:compileDebugKotlin`; fix failures and >120-char lines

## Dependencies

- T001 → T002, T005 → T006, T005 → T007, T003 → T004, T004 + T007 + T001 → T009 → T010.
- T008 → T011; T011 → T012 → T013.
- T003 → T016 → T017 → T018 → T019.
- US2/US3 depend only on Phase 2; US4 independent of US1–US3 except T003.

## Parallel examples

- After T001: T002, T003, T005 in parallel.
- After T009: T010, T011 in parallel; T016 any time after T003.

## Implementation Strategy

MVP = Phase 2 + US1 (eve reminder and tap). Week and published come with the same notifier; the switch last.
