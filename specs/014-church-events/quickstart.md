# Quickstart: Church Events

## Prerequisites

- The backend runs with feature 020 (`backend` branch `020-church-events`) and serves `api/events/` and
  `api/admin/events/`.
- Accounts: an Admin (`owner` on `events`), a member with no role, and a user whose `events` level was lowered to
  `view` (and one to `manage`) in the Django admin.

## Unit tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.events.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.admin.events.*"
./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.presentation.format.*"
```

They cover:
- the date label formats;
- the active filter and the limit of 3;
- the DTO mapping;
- the draft validation;
- the changes → multipart fields mapping (create, edit, clear);
- the field-error placement;
- the session cache clear;
- the login-change refresh.

## Manual scenarios

1. **Logged out, three or more public events**: the carousel shows birthdays, schedule, then 3 event pages in date
   order. Tapping one opens the detail.
2. **Members-only event**: logged out, it is not shown. After logging in as a member it appears. After logging out
   it disappears at once, and also with airplane mode on after the logout.
3. **Offline**: load the home screen, turn on airplane mode, kill and reopen the app. The events show with their
   images.
4. **Expiry offline**: with a saved one-day event dated yesterday (set the device date forward a day), it does not
   appear.
5. **Create** (Admin): Painel → Eventos → Novo evento. Pick a portrait photo; the crop is locked to 16:9. Save with
   title, date and image, return home, and the event shows.
6. **Field errors**: save with the end date before the start date (local), then with an image under 640×360
   (server). Each message shows under its field, and the typed values stay.
7. **Edit / clear**: remove the time and location of an event and save. The detail no longer shows them.
8. **Levels**: a `view` user sees the list and a read-only form. A `manage` user can save but sees no "Excluir". An
   `owner` deletes after the confirmation.
9. **Gone**: delete an event on another device, then open it in management. The app goes back to the list with
   "Este evento não existe mais.".
