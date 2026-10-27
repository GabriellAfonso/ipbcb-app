# Implementation Plan: Rota Reminders in the App

**Branch**: `018-schedule-reminders` | **Date**: 2026-10-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/018-schedule-reminders/spec.md`

## Summary

`PushMessage` gains `ScheduleWeek`, `ScheduleEve` and `SchedulePublished`, each with a parsed `List<ScheduleSlot>`
(pure Kotlin, `slots` decoded with `kotlinx.serialization`'s `Json.parseToJsonElement`, no annotations).
`PushMessageHandler` drops them when signed out or when the new "Escala" switch is off (read through a core port
`ScheduleRemindersPreference`, backed by `ThemePreferences`), otherwise hands them to a new `ScheduleNotifier`
(`ScheduleNotifications`, channel "Escala", one fixed id per kind), foreground or not, with no refresh. A tap
carries `NotificationTarget.Schedule(date)`; `AppNavHost` navigates to `schedule?date=…` (optional argument on the
existing inline route), and `ScheduleViewModel` reads the date from `SavedStateHandle` to pick the opening tab.
Settings gets the "Escala" switch next to "Aniversários". Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (JVM 17), Android, Jetpack Compose (Material3)

**Primary Dependencies**: Hilt, kotlinx.serialization (JSON), Firebase Messaging, DataStore, Navigation Compose
(all present; no new library)

**Storage**: one new boolean in the `@SettingsPrefs` DataStore (`schedule_notifications_enabled`, default `true`)

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + turbine; fakes preferred

**Target Platform**: Android (minSdk 24, desugaring on)

**Project Type**: mobile app, single `:app` module

**Performance Goals**: notification shown from the payload alone, no I/O besides one DataStore read.

**Constraints**:
- `core` must not import `features/schedule` (no schedule refresh, own formatting of time and month);
- `domain/` stays Android-free (slot parsing in pure Kotlin, testable on the JVM);
- 120-char lines; user-visible strings in Portuguese; no magic strings.

**Scale/Scope**: 3 push types, 1 notifier, 1 notification target, 1 optional route argument, 1 settings switch.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Rule (`specs/constitution.md`, `CLAUDE.md`) | Status |
|---|---|
| Features never import each other; `core` never imports a feature | Pass: notifier formats time/month itself; no schedule refresh |
| `domain/` has no Android knowledge | Pass: `ScheduleSlot` + parsing use `java.time` and kotlinx JSON only |
| Errors as `AppError` / `Result` | N/A: no network; malformed messages are `null` and ignored |
| Screen/content split; `StateFlow` | Pass: date enters through `SavedStateHandle`; content composable unchanged in shape |
| `safePopBackStack()` | Pass: back from Escala unchanged |
| No PII in logs | Pass: slots carry no person; nothing logged beyond "unknown shape" |
| Spec and code in the same commit; domain specs updated | Pass: `specs/schedule/spec.md`, `specs/settings/spec.md` updated with the code |

No violations; Complexity Tracking left empty.

## Project Structure

### Documentation (this feature)

```text
specs/018-schedule-reminders/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/ui.md
├── checklists/requirements.md
└── tasks.md
specs/schedule/spec.md, specs/settings/spec.md     # domain specs updated with the feature
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── domain/push/PushMessage.kt                 # + ScheduleSlot, ScheduleWeek/Eve/Published, slots parsing
│   ├── domain/push/PushPorts.kt                   # + ScheduleRemindersPreference
│   ├── data/push/ScheduleNotifications.kt         # new: ScheduleNotifier + channel "Escala"
│   ├── data/push/PushMessageHandler.kt            # + three branches, switch check
│   ├── data/local/ThemePreferences.kt             # + scheduleNotificationsFlow / setScheduleNotifications
│   ├── di/PushModule.kt                           # + notifier binding, preference provider
│   └── presentation/navigation/
│       ├── NotificationTarget.kt                  # + Schedule(date), TARGET_SCHEDULE
│       ├── AppRoutes.kt                           # + SCHEDULE_ROUTE with optional date, schedule(date)
│       └── AppNavHost.kt                          # + target branch; route argument
├── features/schedule/presentation/viewmodel/
│   └── ScheduleViewModel.kt                       # + SavedStateHandle date, findSectionForDate
└── features/settings/
    ├── domain/repository/SettingsRepository.kt    # + scheduleNotificationsFlow / setter
    ├── data/repository/SettingsRepositoryImpl.kt
    ├── presentation/viewmodel/SettingsViewModel.kt # + scheduleNotifications, toggle
    └── presentation/screens/SettingsScreen.kt     # + "Escala" switch

app/src/test/java/com/ipb/castelobranco/
├── core/domain/push/PushMessageTest.kt            # + parsing cases
├── core/data/push/PushMessageHandlerTest.kt       # + switch / signed out / foreground cases
├── core/data/push/ScheduleNotificationTextTest.kt # new: pure text builders
├── core/presentation/navigation/NotificationTargetTest.kt (if present, else new)
└── features/schedule/presentation/viewmodel/ScheduleViewModelTest.kt  # + date picks the tab
```

**Structure Decision**: everything push-related stays in `core` beside the setlist and access-request pushes; the
only feature code touched is the schedule ViewModel (tab choice) and settings (switch).

## Implementation Order

1. Domain: `ScheduleSlot`, the three messages and their parsing (+ tests).
2. Preference: `ThemePreferences` key, `ScheduleRemindersPreference` port and provider.
3. Notifier: text builders (pure, tested) + `ScheduleNotifications`.
4. Handler branches (+ tests).
5. Tap: `NotificationTarget.Schedule`, route argument, `AppNavHost` branch, `ScheduleViewModel` date (+ tests).
6. Settings switch.
7. Domain specs (`specs/schedule/spec.md`, `specs/settings/spec.md`), build and unit tests.

## Complexity Tracking

None.
