# Research: Sunday Setlist — Draft, Save, Push and Play Confirmation

Decisions taken while planning spec 011. Each: decision, rationale, alternatives.

## R1 — Where the setlist lives: core holds the stored copy, features own their calls

**Decision**: the device's stored "Repertório de domingo" and the read of `GET api/setlists/current/` live in core:
`core/domain/setlist/` (`SundaySetlist`, `SundaySetlistRepository`, use cases) and `core/data/setlist/`
(`SetlistApi`, `SetlistDto` + mapper, `SundaySetlistRepositoryImpl`). The other calls stay in the feature that uses
them: `PUT api/setlists/{date}/` in `features/worshiphub/tables`, `GET api/setlists/{date}/` and
`GET api/setlists/pending-confirmation/` in `features/admin/register`. Both features reuse core's `SetlistDto` and
its mapper, since the server returns the same Setlist object everywhere.

**Rationale**: the stored copy is written from three places that cannot import each other — the push worker and the
foreground refresh (core), and a successful save (worshiphub/tables) — and read by Letras and Cifras (worshiphub).
Features never import each other, so the shared store is a core port, like `AllSongsRepository`. The admin reads are
only used by admin and are not stored; nothing gains from moving them to core.

**Alternatives**: everything in worshiphub (core's push code would import a feature); everything in core (admin-only
calls in core for no reuse).

## R2 — Stored setlist: one JSON file, in memory as a `StateFlow`

**Decision**: `SundaySetlistRepositoryImpl` keeps `StateFlow<SundaySetlist?>`, loaded from `JsonSnapshotStorage`
(key `sunday_setlist`) as a `Preloadable`, written on every successful current read or save, deleted when the server
answers `{"setlist": null}` or on clear. No ETag, no `BaseSnapshotRepository`.

`ObserveSundaySetlistUseCase` combines the stored value with `DateProvider.today()` and emits `null` once
`today > setlist.date`, so the section disappears by itself after its Sunday without any fetch (FR-021). The stored
file is not deleted by the date check — the next current read replaces it.

**Rationale**: the endpoint is `no-store`/`private` and returns one small object; the `BaseSnapshotRepository`
machinery (ETag, loading/error states) does not apply, and the spec wants no visible loading or error for the section
(US3-8). Preloading from disk makes it available offline from boot.

**Alternatives**: DataStore (a JSON string in preferences works but `JsonSnapshotStorage` is the project's pattern for
cached server objects, constitution of `CLAUDE.md` Pitfall 3); `BaseSnapshotRepository` (states the UI would ignore).

**Accepted limit**: a Letras list left open across Sunday midnight keeps the section until its state recomputes
(any list change, reopening, or the foreground refresh). Not worth a ticking clock.

## R3 — Worship flags reach features through a core port

**Decision**: `MeProfileDto` gains `is_worship_member` and `can_save_setlist` (both default `false`, so a cached
profile from before this version decodes). `MeProfile` gains the two booleans. A new core port
`core/domain/worship/WorshipAccessRepository` (`val worshipAccess: Flow<WorshipAccess>`, `WorshipAccess(isWorshipMember,
canSaveSetlist)`, `WorshipAccess.NONE`) with `ObserveWorshipAccessUseCase`; the profile feature implements it
(`ProfileWorshipAccessRepository`) from the same `/me` snapshot, like `ProfileCurrentMemberRepository`.

**Rationale**: same reasoning as 010 R1. The flags are not scope levels: they come from ministry membership, which
`Access` does not model. Reading the snapshot means they are known from the disk preload and follow every re-read
(including the one after a 403).

**Alternatives**: adding fields to `Access` (mixes ministry membership into the permission model; every `Access` test
and fake changes); computing membership in the app (the app has no ministry data).

## R4 — Push library and token source

**Decision**: add `com.google.firebase:firebase-messaging` under the existing Firebase BOM (34.15.0). No other library.
The token is read with `FirebaseMessaging.getInstance().token` and awaited with `Tasks.await` inside a worker (already
off the main thread). `FcmTokenSource` (core/data/push) wraps it behind `PushTokenSource` (domain port, `suspend fun
currentToken(): String?`), so tests never touch Firebase.

**Rationale**: the app already ships `google-services.json` and the Google Services plugin; messaging is the only
missing part. `Tasks.await` avoids adding `kotlinx-coroutines-play-services` for a single call ("check compatibility
before adding libs").

**Alternatives**: `kotlinx-coroutines-play-services` (`await()`): one more dependency for one call site.

## R5 — Token registration runs in a unique worker; unregister runs inline at logout

**Decision**: `PushTokenSyncWorker` (`@HiltWorker`, `CoroutineWorker`, network constraint, exponential backoff), enqueued
as unique work `push_token_sync` with `REPLACE` by `PushRegistrationScheduler` (core/domain port, WorkManager impl)
on: every app start while logged in (`CoreViewModel.initialize`), `AuthEventBus.Event.LoginSuccess`, and
`onNewToken`. The worker returns early when `SessionPresenceProvider.isLoggedIn()` is false, reads the current token,
calls `POST api/me/devices/` and stores the token sent in `PushPrefs` (DataStore, new qualifier `@PushPrefs`).
Registration is unconditional on start (the call is idempotent and cheap), so a user logged in before this version
registers on the first start of the new version (FR-012).

On logout, `CoreViewModel.logout()` calls `UnregisterDeviceUseCase` first: it cancels the unique work, reads the
stored token, and calls `POST api/me/devices/unregister/` inside `withTimeoutOrNull(UNREGISTER_TIMEOUT_MS = 5_000)`
with every failure swallowed and logged; the stored token is cleared regardless. Only then does the existing logout run
(`logoutUseCase()` clears the tokens). The app has no server logout call today: "before the server logout" means
"while the access token is still stored".

**Rationale**: registration must survive no network at login and retry by itself without a manual scope; WorkManager
already exists (`BirthdayNotificationWorker`, gallery upload). Unregister cannot be deferred to a worker: once tokens
are cleared the call would be unauthenticated. A 5 s cap keeps logout responsive offline (SC-006).

**Alternatives**: register in `CoreViewModel` coroutines (no retry, lost if the app closes); `FirebaseMessaging.deleteToken()`
on logout (forces a new token on every login; the server already forgets the old one and the app ignores stray pushes
— R7).

## R6 — `FirebaseMessagingService` is a second `@AndroidEntryPoint`

**Decision**: `core/data/push/IpbMessagingService : FirebaseMessagingService`, annotated `@AndroidEntryPoint`, declared
in the manifest with the `com.google.firebase.MESSAGING_EVENT` intent filter, `exported="false"`. It injects
`PushMessageHandler` (core/data/push) and does no work of its own:

- `onNewToken(token)` → `PushRegistrationScheduler.schedule()`.
- `onMessageReceived(message)` → `PushMessageHandler.handle(message.data)`.

**Rationale**: Hilt injection into an Android service requires `@AndroidEntryPoint`; this is a documented exception to
the `CLAUDE.md` rule "only `CoreActivity`" (Complexity Tracking). Keeping the service a thin adapter keeps everything
testable without Firebase.

**Alternatives**: `EntryPointAccessors.fromApplication` inside the service (works, but hides the dependency list and is
not used anywhere in the project).

## R7 — Message handling: parse, gate, notify now, fetch in a worker

**Decision**: `PushMessage.parse(data: Map<String, String>): PushMessage?` (core/domain/push, pure) returns
`SetlistSaved(date)` / `ConfirmPlays(date)` for the two known types with a `YYYY-MM-DD` date, `null` otherwise (FR-015,
US4-8). `PushMessageHandler.handle(data)`:

1. `parse`; `null` → ignore (logged at debug, never the payload).
2. `SessionPresenceProvider.isLoggedIn()` false → ignore (decision 7). `onMessageReceived` runs on a Firebase worker
   thread, so the handler uses `runBlocking` for these short local reads — the only blocking call, allowed there
   because the platform gives the service ~10 s off the main thread.
3. `SetlistSaved`: if `WorshipAccess.isWorshipMember` (from the stored profile) is false → ignore. Otherwise enqueue
   `SetlistRefreshWorker` (unique `setlist_refresh`, `REPLACE`, network constraint) and, unless
   `AppForegroundState.isForeground`, show the "Repertório de domingo dd/MM disponível" notification.
4. `ConfirmPlays`: show "Confirmar músicas de domingo" regardless of foreground (US5-1). The tap decides access (R9),
   so no profile check here.

`SetlistRefreshWorker` runs `SyncSundaySetlistUseCase` (R8) and gates on `AuthStatusProvider.hasValidAccessToken()`
like other background callers; with an expired token it does nothing and the next foreground refresh covers it.

**Rationale**: the notification needs only the date in the payload, so it is shown immediately and never waits for the
network. The fetch may need retries; a worker gives them without a hand-made scope. A session ended by a failed
refresh never unregistered its token, so the device must ignore messages after it (decision 7).

**Alternatives**: fetching inside `onMessageReceived` (time-limited, no retry); showing the notification only after the
fetch (no notification offline).

## R8 — One sync use case for start, resume, push and flag changes

**Decision**: `SyncSundaySetlistUseCase` (core/domain/setlist): when not logged in or not a worship member →
`repository.clear()`; otherwise `repository.refreshCurrent()` (current read; `setlist: null` or a `403` clears; any other failure keeps
the stored copy and returns the `AppError`, which callers only log). Called from:

- `CoreViewModel`: after the profile refresh in the boot cascade, and in `onAppForeground()` (already wired to
  `ON_START` in `AppNavHost`);
- `SetlistRefreshWorker` (R7);
- a `CoreViewModel` collector on `ObserveWorshipAccessUseCase` that clears when `isWorshipMember` turns false
  (FR-020).

Logout and the session-loss path clear it through `SessionScopedCache` (R10).

**Rationale**: one rule for "should this device hold a setlist", tested once.

## R9 — Notification taps: `singleTop` `CoreActivity` → pending target → `AppNavHost`

**Decision**: notifications carry a `PendingIntent` to `CoreActivity` with extras `EXTRA_TARGET`
(`"sunday_setlist"` | `"confirm_plays"`) and `EXTRA_DATE`. `CoreActivity` becomes `launchMode="singleTop"` and reads
the extras in `onCreate` (fresh launch) and `onNewIntent` (app already open) into
`MutableStateFlow<NotificationTarget?>`, passed to `AppNavHost(navController, notificationTarget, onTargetHandled)`.
`NotificationTarget` (core/presentation/navigation) is `SundaySetlist` | `ConfirmPlays(date)`; parsing is a pure
function (`NotificationTarget.fromExtras`).

`AppNavHost` collects the target after `coreViewModel.isPreloading` is false (so the stored profile and access are
loaded) and:

- `SundaySetlist` → `navController.navigateToLyrics()` (exported by `LyricsNavGraph.kt`; the lyrics graph route).
- `ConfirmPlays(date)` → if logged in and `Access.allows(SONGS, MANAGE)`: `navController.navigateToSundayConfirmation(date)`
  (exported by `AdminNavGraph.kt`: navigates to the admin graph, then to `AdminRegister?date=…`, so back returns to
  the panel). Otherwise a `Toast` "Você não tem mais acesso ao registro de músicas." and the app stays on home
  (FR-025, US5-4).

Then `onTargetHandled()` clears the flow, so rotation or recomposition never repeats it. `CoreActivity` already passes
messages to the user only through `restartApp`; this is a new, separate path.

**Rationale**: the notification deep link goes through the single Activity into the graphs, as the request asks; the
access check uses what the app already holds, so it works offline and never shows an empty admin screen.
`singleTop` makes a tap with the app open reuse the activity instead of stacking a second one.

**Alternatives**: Navigation deep-link URIs (`navDeepLink`) — they would need public URI patterns for internal routes
and bypass the access check; a `TaskStackBuilder` (rebuilds the whole stack, loses the open screen).

## R10 — Clearing the stored setlist on logout via `SessionScopedCache`

**Decision**: `SundaySetlistRepositoryImpl` also binds `@IntoSet SessionScopedCache`; its `clear()` empties memory and
deletes the file. `CoreViewModel` already calls every `SessionScopedCache` on `logout()` and when `isLoggedInFlow`
drops to `false` after a failed refresh.

**Rationale**: covers both logout paths with no new wiring. `specs/core/spec.md` §4.4.1 says "só em memória"; the
wording is widened to "data that only holds for the session, in memory or on disk".

**Alternatives**: explicit call in `logout()` like `scheduleRepository.clearScheduleCache()` (misses the failed-refresh
path).

## R11 — Repertoire draft: DataStore with a wall clock

**Decision**: `features/worshiphub/tables/data/local/RepertoireDraftStorage` writes the rows as JSON
(`RepertoireDraftDto`, kotlinx.serialization) plus `updated_at` epoch millis into the existing `@SetlistPrefs`
DataStore under new keys (`repertoire_draft_v1`, `repertoire_draft_updated_at`). Domain:
`RepertoireDraft(rows, updatedAtMillis)`, `RepertoireDraftRepository`, and the pure rule
`DraftExpiry.isExpired(updatedAt, now)` with `DRAFT_TTL_MS = 3_600_000`.

A new core primitive `WallClock` (`fun interface`, `nowMillis()`, provided in `AppInfoModule` as
`System.currentTimeMillis()`) gives the time, so the TTL is testable. `MonotonicClock` cannot be used: it resets on
reboot and the draft must survive app and device restarts.

`SongsTableViewModel` restores once, after the song catalog first reaches `SnapshotState.Data` (a row is emptied only
when the catalog is known and lacks the song — edge case "catalog not loaded"). Every row change (select, key, pin,
generate) saves the whole draft with the current time; "Limpar repertório" resets rows and clears the draft. Saving is
triggered by the user actions only — never by the restore itself, so restoring does not renew the hour — and each
action writes the latest rows (DataStore `edit` is serialized, last write wins).

**Rationale**: four small rows; DataStore is the project's store for small device-local preferences, and
`@SetlistPrefs` already holds the related manual pins. The TTL lives in a pure domain function, tested with a fake
clock (decision 1: checked only on restore).

**Alternatives**: a `JsonSnapshotStorage` file (meant for server snapshots); in-memory `SavedStateHandle` (does not
survive the app being killed — the bug being fixed).

## R12 — Save: date rule, request, result mapping

**Decision**:

- `setlistDateFor(today: LocalDate): LocalDate` (core/domain/setlist, pure): today when Sunday, else
  `today.with(TemporalAdjusters.next(SUNDAY))`. `today` from `DateProvider` (church zone; matches the server's
  zone — spec assumption).
- `features/worshiphub/tables`: `SetlistSaveApi.put(date, body)`, `SetlistSaveRepository.save(date, items):
  SaveSetlistResult`, `SaveSundaySetlistUseCase` which builds items from filled rows (position kept, tone trimmed),
  calls the repository and on success stores the returned setlist in core (`SundaySetlistRepository.store`).
- `SaveSetlistResult`: `Saved(setlist)` | `Failed(SaveSetlistFailure)` with `NoPermission`, `Invalid(userMessage)`,
  `MissingSongs(count)`, `NoConnection`, `Other(AppError)`. `MissingSongs.count` comes from
  `AppError.Server.extras["missing_song_ids"]` (JSON array text, parsed with kotlinx.serialization in the repository)
  — the error body itself is still parsed only by `ResponseExt` (constitution).
- Texts in `RepertoireTexts` (presentation): "Repertório de domingo dd/MM salvo.", "Você não tem permissão para salvar
  o repertório.", the server's `detail` for `Invalid` (via `toUserMessage()`), "N música(s) não encontrada(s). Gere o
  repertório de novo.", "Sem conexão. Tente novamente.", generic via `toUserMessage()`.
- `canSave` in the UI state = `worshipAccess.canSaveSetlist`; `isSaveEnabled` = at least one filled row, every filled
  row with a trimmed tone of 1–3 chars, not saving (FR-009). `Validation` is a pure function
  (`RepertoireValidation.canSave(rows)`).

**Rationale**: every rule is a pure function or a mapping with one test each; the core store is updated by the use
case, not the ViewModel, so the screen never touches core data.

## R13 — Letras and Cifras: section built by one shared pure function

**Decision**: `features/worshiphub/shared/domain/SundaySection.kt`:
`buildSundaySection(setlist: SundaySetlist?, contentBySongId: Map<Int, Int>): SundaySection?` returns the setlist's
items in position order that have content in that list (lyrics id / chord chart id), each with its key, or `null` when
there is no setlist or none of its songs has content. `LyricsViewModel` and `ChordChartsViewModel` add
`ObserveSundaySetlistUseCase()` to their `combine`, put the section in the UI state and drop its song ids from the
pinned/rest list (decision 9). `SongContentListScreen` gains an optional `sundaySection` parameter rendered above the
rows with the title "Repertório de domingo dd/MM" and a tone chip per row. Tap → the existing `onItemClick(id)`
(decision 10).

**Rationale**: same component and the same rule for both lists; the section is local data, so it has no loading or
error of its own (US3-8) — it simply exists or not.

## R14 — Register screen: optional date argument and prefill state

**Decision**: route `AdminRegister?date={date}` (nullable string arg, default `null`); `MusicRegistrationViewModel`
reads it from `SavedStateHandle`. With a date: `registrationType = SUNDAY`, `selectedDate` fixed, date picker disabled,
and a `prefill: PrefillState` (`Loading` | `Loaded` | `NotFound` | `Failed(message)`). `GetSetlistForDateUseCase` →
`SetlistConfirmationRepository.byDate(date)`: `200` → rows `max(4, items)` with `selectedSongId`, label from
`SongLabelFormatter` (title — artist from the item) and tone; `404` → empty rows + snackbar "Repertório de dd/MM não
encontrado. Preencha as músicas."; other failure → `Failed` with "Tentar novamente" (re-runs the read) while rows stay
editable (US5-5). Without a date, the screen behaves exactly as today.

**Rationale**: reuses the existing form and validation untouched (spec assumption); the route argument is how other
graphs pass ids (e.g. lyrics detail).

## R15 — Pending card in the admin panel

**Decision**: `features/admin/register/domain` gains `SetlistConfirmationRepository.pending()` and
`GetPendingConfirmationsUseCase`. `AdminPanelViewModel` gains `pending: PendingConfirmationsUi` (`Hidden` | `Loading` |
`Failed(message)` | `Dates(List<LocalDate>)`), `Hidden` unless `Access.allows(SONGS, MANAGE)`; `Dates(empty)` maps to
`Hidden` (FR-026). `AdminScreen` calls `refreshPending()` on `ON_RESUME` (`LifecycleEventEffect`), so returning from the
register screen re-reads it (FR-027). The card is a full-width card above the grid, outside `PanelCard` (it is not an
area entry); tapping a date calls `nav.confirmSunday(date)`.

**Rationale**: `PanelCard` is the grid of areas with a fixed requirement; a data-driven list is a different thing. The
"no badge without real data" rule of admin §2.1 is respected: the card shows real dates.

## R16 — Foreground state

**Decision**: `core/data/app/AppForegroundState` (`@Singleton`, `@Volatile var isForeground`), set by the existing
`LifecycleEventEffect(ON_START)` / a new `ON_STOP` effect in `AppNavHost` through `CoreViewModel`. Read by
`PushMessageHandler` (decision 8).

**Rationale**: single Activity, so its start/stop is the app's foreground; avoids adding `lifecycle-process` for
`ProcessLifecycleOwner`.

**Alternatives**: `ProcessLifecycleOwner` (new dependency for one boolean).
