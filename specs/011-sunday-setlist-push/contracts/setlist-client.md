# Contract: Sunday Setlist (app side)

What the app sends, how it reads each answer, which push messages it acts on, and what a notification tap opens. The
server contract is not redefined: see `backend/specs/017-sunday-setlist-push/contracts/` (`setlist-api.md`,
`push-messages.md`, `me-api.md`).

All calls on `@AuthedRetrofit` (JWT, `PermissionDeniedInterceptor`, `TokenAuthenticator`), paths relative to
`ApiConstants.BASE_PATH`. Error responses become `AppError` only through `Response.toAppError()`.

## 1. HTTP calls

| Owner | Interface · call | Method + path | Body | Success read |
|-------|------------------|---------------|------|--------------|
| core | `SetlistApi.getCurrent` | `GET api/setlists/current/` | — | `200` `{"setlist": SetlistDto?}` |
| core | `DevicesApi.register` | `POST api/me/devices/` | `{"token"}` | `204` |
| core | `DevicesApi.unregister` | `POST api/me/devices/unregister/` | `{"token"}` | `204` |
| worshiphub/tables | `SetlistSaveApi.save` | `PUT api/setlists/{date}/` | `{"items": [{"song_id","position","tone"}]}` | `200` `SetlistDto` |
| admin/register | `SetlistAdminApi.byDate` | `GET api/setlists/{date}/` | — | `200` `SetlistDto` |
| admin/register | `SetlistAdminApi.pending` | `GET api/setlists/pending-confirmation/` | — | `200` `List<SetlistDto>` |
| admin/register | `SetlistAdminApi.delete` | `DELETE api/setlists/{date}/` | — | `204` |

`{date}` always `YYYY-MM-DD` (`DateTimeFormatter.ISO_LOCAL_DATE`). `SetlistDto` and its mapper live in
`core/data/setlist` and are reused by both features.

`GET api/me/profile/` is unchanged as a call; its DTO gains `is_worship_member` and `can_save_setlist` (default
`false`).

## 2. Reading each answer

### Current setlist (`SyncSundaySetlistUseCase`)

| Answer | Effect on the stored copy | Returned |
|--------|---------------------------|----------|
| `200` with setlist | replaced | success |
| `200` `{"setlist": null}` | deleted | success |
| `403` | deleted (not a worship member any more); profile re-read by `PermissionDeniedInterceptor` | `AppError.Auth(403)` |
| network / `5xx` / unparsable | untouched | `AppError` (logged only) |

### Save (`SaveSundaySetlistUseCase`)

| Answer | `SaveSetlistFailure` | Text (one-shot) | Effect |
|--------|----------------------|-----------------|--------|
| `200` | — | "Repertório de domingo dd/MM salvo." | stored copy replaced with the answer |
| `403` | `NoPermission` | "Você não tem permissão para salvar o repertório." | profile re-read (interceptor) — "Salvar" may disappear |
| `400` | `Invalid` | server `detail` via `toUserMessage()` | none |
| `404` + `missing_song_ids` | `MissingSongs(n)` | "1 música não encontrada. …" / "n músicas não encontradas. Atualize a lista e gere o repertório de novo." | none |
| no network | `NoConnection` | "Sem conexão. Tente novamente." | none |
| other | `Other` | `toUserMessage()` generic | none |

The draft is never changed by a save, successful or not.

### Device registration

| Call | Answer | Effect |
|------|--------|--------|
| register | `204` | token stored in `@PushPrefs` |
| register | network / `5xx` | `Result.retry()` (worker backoff) |
| register | `400` / `401` / `403` | `Result.failure()`; next start tries again |
| unregister | anything, or 5 s timeout | ignored and logged; stored token cleared; logout continues |

### Register prefill (`GetSetlistForDateUseCase`)

| Answer | `PrefillState` | Screen |
|--------|----------------|--------|
| `200` | `Loaded` | rows from items, date fixed |
| `404` | `NotFound` | empty rows, snackbar "Repertório de dd/MM não encontrado. Preencha as músicas." |
| `403` | `Failed` | permission text (`toUserMessage()`); profile re-read |
| network / other | `Failed` | message + "Tentar novamente"; rows editable |

### Pending confirmations (`GetPendingConfirmationsUseCase`)

| Answer | Card |
|--------|------|
| `200` non-empty | dates dd/MM, newest first |
| `200` `[]` | hidden |
| `403` | hidden (access changed; profile re-read hides the panel entry too) |
| network / other | short error + "Tentar novamente" |

### Delete a pending setlist (`DeletePendingSetlistUseCase`)

The "Remover" button (trash icon and text) shows only with `WorshipAccess.canSaveSetlist`. The repository passes every refusal on as `AppError`;
the use case turns `404` into success.

| Answer | Use case | Card |
|--------|----------|------|
| `204` | success | date removed; card hidden when it was the last |
| `404` | success (already gone) | date removed; card hidden when it was the last |
| `403` | `AppError.Auth(403)`; profile re-read by `PermissionDeniedInterceptor` | date kept; snackbar "Não foi possível remover o repertório de dd/MM." |
| network / other | `AppError` | date kept; same snackbar |

## 3. Push messages acted on

| Payload | Gate | Action |
|---------|------|--------|
| `type=setlist_saved`, valid `date` | logged in **and** stored `isWorshipMember` | enqueue `SetlistRefreshWorker`; if not foreground, notify "Repertório de domingo dd/MM disponível" (id `SETLIST_SAVED_NOTIFICATION_ID`, replaced by the next) |
| `type=confirm_plays`, valid `date` | logged in | notify "Confirmar músicas de domingo" (id `CONFIRM_PLAYS_NOTIFICATION_ID`, replaced by the next) |
| anything else | — | ignored |

Channel: id `sunday_setlist`, name "Repertório", importance default. Notifications are posted only with notification
permission granted (Android 13+), and auto-cancel on tap.

## 4. Notification taps

`PendingIntent.getActivity(CoreActivity, requestCode per target, FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT)` with
`FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP` and extras:

| Extra | Values |
|-------|--------|
| `com.ipb.castelobranco.extra.TARGET` | `sunday_setlist` · `confirm_plays` |
| `com.ipb.castelobranco.extra.DATE` | `YYYY-MM-DD` (for `confirm_plays`) |

| Target | Condition (checked after boot preload) | Navigation |
|--------|----------------------------------------|------------|
| `sunday_setlist` | — | lyrics graph (`navigateToLyrics()`) |
| `confirm_plays` | logged in and `songs ≥ manage` | admin graph → `AdminRegister?date=…` (`navigateToSundayConfirmation(date)`) |
| `confirm_plays` | otherwise | stay on home; Toast "Você não tem mais acesso ao registro de músicas." |
| invalid / missing extras | — | nothing; normal launch |
