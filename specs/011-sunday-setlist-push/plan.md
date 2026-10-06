# Implementation Plan: Sunday Setlist — Draft, Save, Push and Play Confirmation

**Branch**: `011-sunday-setlist-push` | **Date**: 2026-10-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/011-sunday-setlist-push/spec.md`

## Summary

The Repertório rows become a device-local draft with a sliding one-hour expiry. Users with `can_save_setlist` save the
filled rows as the Sunday setlist; the answer goes straight into a core-held copy of the "current setlist", which
Letras and Cifras show as a "Repertório de domingo" section for worship members until the end of that Sunday. The app
joins Firebase Cloud Messaging: it registers its token after login, on every start and on rotation, unregisters it at
logout before clearing the session, and turns the two data messages into notifications — `setlist_saved` refreshes the
copy (also refreshed on start and resume as fallback), `confirm_plays` deep-links through `CoreActivity` into the admin
graph's register screen, pre-filled from the setlist of that date. The admin panel lists Sundays pending confirmation.

Key technical choices (details in [research.md](research.md)):

- **Stored setlist and current read in core** (`SundaySetlistRepository`), save in worshiphub/tables, by-date and
  pending in admin/register; `SetlistDto` shared from core (R1, R2).
- **`WorshipAccessRepository` core port** implemented by the profile feature from `/me` (R3).
- **`firebase-messaging`** under the existing BOM; no other library (R4).
- **Registration in a unique `PushTokenSyncWorker`**; unregister inline at logout with a 5 s cap (R5).
- **`IpbMessagingService` as a second `@AndroidEntryPoint`**, a thin adapter over `PushMessageHandler` (R6, R7).
- **One `SyncSundaySetlistUseCase`** for start, resume, push and flag changes; logout via `SessionScopedCache` (R8, R10).
- **Notification taps: `singleTop` `CoreActivity` → `NotificationTarget` → `AppNavHost`** with the access check (R9).
- **Draft in `@SetlistPrefs` DataStore with a new `WallClock`** (R11); save rules as pure functions (R12).
- **Shared `buildSundaySection`** for Letras and Cifras (R13); register `?date=` prefill (R14); pending card (R15).

## Technical Context

**Language/Version**: Kotlin 2.3.10 (JVM 17 target), Android, `minSdk 24`, `targetSdk 36`

**Primary Dependencies**: existing — Jetpack Compose (Material 3), Hilt (+ hilt-work), Retrofit 3 + OkHttp 5,
kotlinx.serialization, DataStore, WorkManager 2.10, Firebase BOM 34.15.0 (Crashlytics, Analytics). **New**:
`com.google.firebase:firebase-messaging` (BOM-managed).

**Storage**: `JsonSnapshotStorage` key `sunday_setlist` (new); `@SetlistPrefs` DataStore gains draft keys; new
`@PushPrefs` DataStore (`push_prefs`) for the registered token; `MeProfileDto` gains two optional fields.

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; fakes preferred (see [quickstart.md](quickstart.md)).
Workers tested through their use cases; Firebase never touched in tests (`PushTokenSource` port).

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: notification shown without waiting for the network; setlist section built in memory from at
most ten items; draft restore once per ViewModel.

**Constraints**: section and draft offline; logout never blocked (5 s cap on unregister); `domain/` without Android
types; only `ResponseExt` parses error bodies; Portuguese strings hardcoded; 120-char lines; no manual
`CoroutineScope` (workers instead).

**Scale/Scope**: ~30 new production files, ~22 touched; ~18 test classes new or extended; `specs/worshiphub`,
`specs/admin`, `specs/core` updated.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is the unfilled template; the rules are `specs/constitution.md` and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing layers | ✅ repositories return `Result<T>` / `SaveSetlistResult` carrying `AppError` |
| Conversion in the data layer | ✅ `SundaySetlistRepositoryImpl`, `SetlistSaveRepositoryImpl`, `SetlistConfirmationRepositoryImpl`, `DevicesRepository` |
| `message` technical, `userMessage` for screen | ✅ server `detail` reaches the screen only via `toUserMessage()` (`Invalid`) |
| One HTTP error parsing point | ✅ `missing_song_ids` read from `AppError.Server.extras`, filled by `ResponseExt` |
| 403 is permission, not login | ✅ save 403 → permission text; prefill 403 → `toUserMessage()` |
| Screen text via `toUserMessage()` | ✅ app-authored texts live in `RepertoireTexts` / register texts; generic via `toUserMessage()` |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| Single Activity, only `CoreActivity` is `@AndroidEntryPoint` | ⚠️ `IpbMessagingService` also is — justified below |
| UI → ViewModel → UseCase → Repository | ✅ new use cases for save, draft, sync, prefill, pending, register/unregister |
| Features don't import each other | ✅ stored setlist and worship flags via core ports; DTO shared from core |
| `domain/` without Android | ✅ `java.time` only; `PushMessage.parse` takes a `Map` |
| Screen/Content split, dumb composables, three states | ✅ section, save, prefill and pending card each have loading/success/error |
| `StateFlow` + `SharedFlow` events | ✅ `RepertoireEvent`; register keeps its `snackbarMessage` pattern |
| `viewModelScope`, no manual scopes | ✅ background work in `CoroutineWorker`s; the service only schedules |
| Graph-scoped ViewModel | ✅ unchanged: register and tables keep per-entry VMs (as today) |
| `safePopBackStack`, routes in local `XRoutes` | ✅ `AdminRoutes.register(date)`; navigation helpers exported by the graph files |
| Snapshot cache pattern | ✅ `JsonSnapshotStorage` for the server object |
| No secrets in code | ✅ `google-services.json` already present; no server key in the app |
| Tests: happy path + 1 error per use case, fakes | ✅ see quickstart |
| Spec and code in the same commit | ✅ domain specs updated with the code |

**Gate: PASS** with the exception in Complexity Tracking. Post-design re-check: PASS — core gains two ports
(setlist, worship access) mirroring `AccessRepository`/`CurrentMemberRepository`, and push infrastructure that no
feature imports.

## Project Structure

### Documentation (this feature)

```text
specs/011-sunday-setlist-push/
├── spec.md
├── plan.md              # this file
├── research.md          # R1–R16
├── data-model.md
├── quickstart.md
├── contracts/
│   └── setlist-client.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
gradle/libs.versions.toml                                  # + firebase-messaging
app/build.gradle.kts                                       # + implementation(libs.firebase.messaging)
app/src/main/AndroidManifest.xml                           # IpbMessagingService; CoreActivity singleTop

app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── domain/
│   │   ├── setlist/ (new)   SundaySetlist.kt, SundaySetlistRepository.kt, SetlistDate.kt,
│   │   │                    ObserveSundaySetlistUseCase.kt, SyncSundaySetlistUseCase.kt
│   │   ├── worship/ (new)   WorshipAccess.kt, WorshipAccessRepository.kt, ObserveWorshipAccessUseCase.kt
│   │   ├── push/ (new)      PushMessage.kt, PushTokenSource.kt, PushRegistrationScheduler.kt,
│   │   │                    DevicesRepository.kt, RegisterDeviceUseCase.kt, UnregisterDeviceUseCase.kt
│   │   └── util/WallClock.kt (new)
│   ├── data/
│   │   ├── setlist/ (new)   SetlistApi.kt, SetlistDto.kt, SetlistMapper.kt, SundaySetlistRepositoryImpl.kt
│   │   ├── push/ (new)      DevicesApi.kt, DevicesRepositoryImpl.kt, FcmTokenSource.kt, PushTokenStore.kt,
│   │   │                    PushTokenSyncWorker.kt, WorkManagerPushRegistrationScheduler.kt,
│   │   │                    IpbMessagingService.kt, PushMessageHandler.kt, SetlistRefreshWorker.kt,
│   │   │                    SetlistNotifications.kt
│   │   └── app/AppForegroundState.kt (new)
│   ├── di/
│   │   ├── SetlistModule.kt (new), PushModule.kt (new)
│   │   ├── DataStoreModule.kt                             # + @PushPrefs
│   │   └── AppInfoModule.kt                               # + WallClock
│   └── presentation/
│       ├── CoreActivity.kt                                # extras → NotificationTarget; onNewIntent
│       ├── navigation/NotificationTarget.kt (new)
│       ├── navigation/AppNavHost.kt                       # target handling; ON_STOP foreground flag
│       └── viewmodel/CoreViewModel.kt                     # register on start/login, unregister on logout,
│                                                          # setlist sync on boot/foreground/flag change
├── features/profile/
│   ├── data/dto/MeProfileDto.kt                           # + is_worship_member, can_save_setlist
│   ├── domain/model/MeProfile.kt                          # + two flags
│   ├── data/snapshot/ProfileSnapshotRepository.kt         # maps them
│   ├── data/access/ProfileWorshipAccessRepository.kt (new)
│   └── di/ProfileModule.kt                                # binds the port
├── features/worshiphub/
│   ├── tables/
│   │   ├── data/local/RepertoireDraftStorage.kt (new), data/dto/RepertoireDraftDto.kt (new)
│   │   ├── data/api/SetlistSaveApi.kt (new), data/repository/SetlistSaveRepositoryImpl.kt (new)
│   │   ├── domain/model/RepertoireDraft.kt (new), domain/model/SaveSetlistResult.kt (new)
│   │   ├── domain/repository/RepertoireDraftRepository.kt (new), SetlistSaveRepository.kt (new)
│   │   ├── domain/usecase/ RepertoireDraftUseCases.kt (new), SaveSundaySetlistUseCase.kt (new),
│   │   │                    RepertoireValidation.kt (new), DraftExpiry.kt (new)
│   │   ├── di/SongsTable.kt                               # binds + API
│   │   └── presentation/
│   │       ├── viewmodel/SongsTableViewModel.kt           # draft restore/save, clear, save flow, events
│   │       ├── viewmodel/RepertoireTexts.kt (new)
│   │       ├── screens/SongsTableScreen.kt                # new actions, event collection, dialog
│   │       └── tabs/SuggestionsTab.kt                     # Gerar | Salvar; clear + share icons right, below rows
│   ├── shared/
│   │   ├── domain/SundaySection.kt (new)
│   │   └── presentation/components/SongContentListScreen.kt, SundaySectionList.kt (new)
│   ├── lyrics/presentation/  viewmodel/LyricsViewModel.kt, state/LyricsUiState.kt, screens/LyricsScreen.kt,
│   │                         navigation/LyricsNavGraph.kt (+ navigateToLyrics)
│   └── chordcharts/presentation/ viewmodel/ChordChartsViewModel.kt, state/ChordChartsUiState.kt,
│                                 screens/ChordChartsScreen.kt
└── features/admin/
    ├── register/
    │   ├── data/api/SetlistAdminApi.kt (new), data/repository/SetlistConfirmationRepositoryImpl.kt (new)
    │   ├── domain/repository/SetlistConfirmationRepository.kt (new)
    │   ├── domain/usecase/GetSetlistForDateUseCase.kt (new), GetPendingConfirmationsUseCase.kt (new)
    │   ├── di/WorshipRegisterModule.kt                    # binds + API
    │   └── presentation/ viewmodel/MusicRegistrationViewModel.kt (SavedStateHandle date, prefill),
    │                     state/MusicRegistrationContract.kt (prefill state, RetryPrefill event),
    │                     screens/MusicRegistrationScreen.kt, components/SundayRegistrationForm.kt (locked date)
    └── panel/
        ├── presentation/navigation/AdminNavGraph.kt       # REGISTER?date, AdminNav.confirmSunday,
        │                                                  # NavController.navigateToSundayConfirmation
        ├── presentation/state/AdminPanelUiState.kt        # + pending
        ├── presentation/viewmodel/AdminPanelViewModel.kt  # + pending read, refreshPending()
        └── presentation/screens/AdminScreen.kt            # pending card, ON_RESUME refresh

app/src/test/java/com/ipb/castelobranco/
├── core/testing/ FakeSundaySetlistRepository.kt, FakeWorshipAccessRepository.kt, FakeWallClock.kt (new)
├── core/domain/setlist/ SetlistDateTest, ObserveSundaySetlistUseCaseTest, SyncSundaySetlistUseCaseTest (new)
├── core/domain/push/ PushMessageTest, RegisterDeviceUseCaseTest, UnregisterDeviceUseCaseTest (new)
├── core/data/setlist/SundaySetlistRepositoryImplTest (new)
├── core/data/push/PushMessageHandlerTest (new)
├── core/presentation/ navigation/NotificationTargetTest (new), viewmodel/CoreViewModelLogoutTest (new or extended)
├── features/profile/ MeProfileDtoBackwardCompatibilityTest (+ flags), ProfileWorshipAccessRepositoryTest (new)
├── features/worshiphub/tables/ DraftExpiryTest, RepertoireValidationTest, SaveSundaySetlistUseCaseTest,
│   SetlistSaveRepositoryTest, RepertoireDraftStorageTest, SongsTableViewModelDraftTest,
│   SongsTableViewModelSaveTest, RepertoireTextsTest (new)
├── features/worshiphub/shared/domain/SundaySectionTest (new)
├── features/worshiphub/lyrics|chordcharts/ …ViewModelTest (+ section)
└── features/admin/ register/MusicRegistrationViewModelPrefillTest, panel/AdminPanelViewModelPendingTest (new)

specs/worshiphub/spec.md, specs/admin/spec.md, specs/core/spec.md   # updated with the code
```

**Structure Decision**: core gets the shared pieces (stored setlist, worship flags, push infrastructure, notification
target); each feature keeps its own calls and screens, following its data/domain/presentation split. No new graph;
the register route gains an optional argument.

## Implementation Order

1. **Profile flags** — DTO fields, `MeProfile`, `WorshipAccess` port + use case, profile implementation; tests.
2. **Core setlist** — `SetlistDto`/mapper, `SetlistApi`, `SundaySetlistRepositoryImpl` (preload, store, clear,
   session cache), `setlistDateFor`, observe/sync use cases; tests.
3. **Draft** — `WallClock`, draft storage/repository/use cases, ViewModel restore/save/clear, "Limpar repertório";
   tests. (Delivers US1 alone.)
4. **Save** — API, repository with failure mapping, use case, validation, texts, buttons + dialog + events; tests.
5. **Section in Letras/Cifras** — `buildSundaySection`, ViewModels, `SongContentListScreen`; `CoreViewModel` sync on
   boot/foreground/flag change; tests. (US2 + US3 work without push from here.)
6. **Push** — dependency, manifest, service, handler, workers, notifications, register/unregister, `CoreViewModel`
   wiring, foreground state; tests.
7. **Notification taps** — `NotificationTarget`, `CoreActivity` `singleTop` + `onNewIntent`, `AppNavHost` handling,
   `navigateToLyrics`, `navigateToSundayConfirmation`; tests.
8. **Prefill + pending card** — admin API/repository/use cases, register `?date=`, panel card; tests.
9. **Specs** — `specs/worshiphub`, `specs/admin`, `specs/core`; build and run every unit test.

## Risks

| Risk | Mitigation |
|------|-----------|
| Unregister slow offline makes logout feel stuck | `withTimeoutOrNull(5_000)`; the drawer item already shows no progress, and the result is the same either way |
| Push arrives after a failed-refresh session end (token never unregistered) | Handler ignores everything when not logged in (decision 7); the next login re-registers and moves the token |
| `onMessageReceived` blocks on DataStore reads | Only two local reads via `runBlocking` on Firebase's worker thread; all network work in a worker |
| Notification tap during boot navigates before access is known | Target handled only after `isPreloading` is false (profile preloaded from disk) |
| `singleTop` changes how other intents (restartApp) behave | `restartApp` uses `NEW_TASK | CLEAR_TASK`, unaffected by `singleTop`; checked manually |
| Device and server dates disagree near midnight Sunday | `DateProvider` already uses the church zone; same zone as the server |
| Draft restore races the user's first edit | Restore happens once, before rows are interactive (rows hidden behind the catalog loading state already) |
| Old cached profile lacks the flags | Defaults `false`; corrected by the profile refresh at boot |

## Implementation Notes

Where the code settled differently from the design above (the spec is unchanged):

- **`WorshipAccessRepository.current()`**: a process started by a push or a worker never ran the boot preload, so
  reading the flags from the flow would see `NONE` and drop the message (or clear the setlist). `current()` loads the
  stored profile from disk first; the handler and `SyncSundaySetlistUseCase` use it.
- **`isBootReady` instead of `isPreloading`** gates notification taps: `PreloadDataUseCase` gained an
  `onDiskLoaded` callback between its disk and network phases, so a tap is routed as soon as the stored profile is
  in memory, without waiting for the network.
- **Boot sync does not hold the boot**: the setlist sync runs after the profile refresh in its own coroutine, so
  `isPreloading` ends as before.
- **Section during search**: the Sunday section hides while a search is active, and its songs come back to the
  filtered list (otherwise a searched setlist song would be missing).
- **File grouping**: ports in `core/domain/push/PushPorts.kt`, register/unregister in
  `DeviceRegistrationUseCases.kt`, by-date/pending use cases in `SetlistConfirmationUseCases.kt`;
  `SetlistNotifier`/`SetlistRefreshScheduler` are small interfaces in `core/data/push` so the handler is tested
  without Android.
- **Navigation helpers go through the parent graph** (`WORSHIP_HUB_GRAPH` then lyrics; `ADMIN_GRAPH` then register),
  so back from a notification lands on the hub or the panel.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Second `@AndroidEntryPoint` (`IpbMessagingService`) besides `CoreActivity` | FCM delivers tokens and messages to a `FirebaseMessagingService`; Hilt injection into a service requires the annotation | `EntryPointAccessors` inside the service hides dependencies and is used nowhere in the project |
| `CoreActivity` `launchMode="singleTop"` | A notification tap with the app open must reach the running activity (`onNewIntent`) instead of stacking a second `CoreActivity` | `TaskStackBuilder` / `CLEAR_TASK` would destroy the screen the user is on |
| `SessionScopedCache` used for on-disk data | Clears the stored setlist on both logout paths with no new wiring | An explicit call in `logout()` misses the failed-refresh path; core spec §4.4.1 wording is widened accordingly |
