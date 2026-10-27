# Data Model: Rota Reminders in the App

## ScheduleSlot (`core/domain/push`)

| Field | Type | Rule |
|-------|------|------|
| `date` | `LocalDate` | from `"YYYY-MM-DD"`; invalid → message ignored |
| `service` | `String` | trimmed, non-blank |
| `time` | `LocalTime` | from `"HH:MM"`; invalid → message ignored |

## PushMessage additions (`core/domain/push/PushMessage.kt`)

| Type (`type`) | Class | Fields | Extra rule |
|---------------|-------|--------|------------|
| `schedule_week` | `ScheduleWeek` | `slots: List<ScheduleSlot>` | non-empty |
| `schedule_eve` | `ScheduleEve` | `slot: ScheduleSlot` | exactly one in `slots` |
| `schedule_published` | `SchedulePublished` | `month: YearMonth`, `slots` | `month` `"YYYY-MM"`, slots non-empty |

Slots keep the payload order. Keys: `KEY_SLOTS = "slots"`, `KEY_MONTH = "month"`, `KEY_GENDER = "gender"`, slot
keys `date`, `service`, `time`.

All three also carry `gender: RecipientGender` (`MALE`, `FEMALE`), parsed from `"M"`/`"F"`; absent or any other value
→ `MALE`, the wording the church used before (never invalidates the message).

## Preference

`schedule_notifications_enabled` (boolean, `@SettingsPrefs`, default `true`), exposed as
`ThemePreferences.scheduleNotificationsFlow` / `setScheduleNotifications`, `SettingsRepository` mirrors it, and the
port `ScheduleRemindersPreference.isEnabled()` reads it for the handler.

## NotificationTarget.Schedule

| Extra | Value |
|-------|-------|
| `EXTRA_TARGET` | `"schedule"` |
| `EXTRA_DATE` | first slot's date, ISO |

Missing/invalid date → `Schedule` is still not created (target `null`, plain launch), like `ConfirmPlays`.

## Navigation

Route pattern `schedule?date={date}`; argument `date` nullable string. `ScheduleViewModel` reads it from
`SavedStateHandle` (key `ScheduleArgs.DATE = "date"`).

## ScheduleUiState.Success

Unchanged shape. `initialTabIndex` = section for the given date when it is in the loaded month, else the next
service.
