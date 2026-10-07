# Tasks: Sunday Setlist — Draft, Save, Push and Play Confirmation

**Input**: Design documents from `specs/011-sunday-setlist-push/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/setlist-client.md, quickstart.md

**Tests**: required by `CLAUDE.md` (happy path + 1 error per use case, fakes preferred); the list is in quickstart.md.

Paths below are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and
`app/src/test/java/com/ipb/castelobranco/` (test) unless they start with `specs/`, `app/` or `gradle/`.

## Phase 1: Setup

No shared setup: the only new library (`firebase-messaging`) belongs to User Story 4 (T040).

## Phase 2: Foundational (blocking for every story)

**Purpose**: worship flags from the profile, the core-held Sunday setlist and the wall clock — used by US1–US6.

- [X] T001 [P] Add `@SerialName("is_worship_member") isWorshipMember: Boolean = false` and `@SerialName("can_save_setlist") canSaveSetlist: Boolean = false` to `features/profile/data/dto/MeProfileDto.kt`, the two fields (default `false`) to `features/profile/domain/model/MeProfile.kt`, and map them in `features/profile/data/snapshot/ProfileSnapshotRepository.kt`
- [X] T002 [P] Create `WorshipAccess(isWorshipMember, canSaveSetlist)` with `NONE` in `core/domain/worship/WorshipAccess.kt`, `WorshipAccessRepository` (`val worshipAccess: Flow<WorshipAccess>`) in `core/domain/worship/WorshipAccessRepository.kt` and `ObserveWorshipAccessUseCase` in `core/domain/worship/ObserveWorshipAccessUseCase.kt` (research R3)
- [X] T003 Create `ProfileWorshipAccessRepository` (from `ProfileSnapshotRepository.observe()`, `NONE` without data, `distinctUntilChanged`) in `features/profile/data/access/ProfileWorshipAccessRepository.kt` and bind it in `features/profile/di/ProfileModule.kt`
- [X] T004 [P] Create `WallClock` (`fun interface`, `nowMillis(): Long`) in `core/domain/util/WallClock.kt` and provide `WallClock { System.currentTimeMillis() }` in `core/di/AppInfoModule.kt` (research R11)
- [X] T005 [P] Create `SundaySetlist` and `SetlistItem` in `core/domain/setlist/SundaySetlist.kt` and `setlistDateFor(today: LocalDate): LocalDate` (today if Sunday, else next Sunday) in `core/domain/setlist/SetlistDate.kt` (data-model §Core)
- [X] T006 [P] Create `SetlistDto`, `SetlistItemDto`, `CurrentSetlistDto` (`setlist: SetlistDto?`) in `core/data/setlist/SetlistDto.kt` and `SetlistDto.toDomain()` (items sorted by position; unparsable date → throws `AppError.Unknown`) in `core/data/setlist/SetlistMapper.kt`
- [X] T007 [P] Create `SetlistApi` (`GET api/setlists/current/` → `Response<CurrentSetlistDto>`) in `core/data/setlist/SetlistApi.kt`; path constant next to it (contract §1)
- [X] T008 Create `SundaySetlistRepository` port (`val stored: Flow<SundaySetlist?>`, `suspend fun refreshCurrent(): Result<Unit>`, `suspend fun store(setlist: SundaySetlist)`, `suspend fun clear()`) in `core/domain/setlist/SundaySetlistRepository.kt`
- [X] T009 Implement `SundaySetlistRepositoryImpl` (`@Singleton`; `MutableStateFlow`; `JsonSnapshotStorage` key `sunday_setlist` holding the DTO JSON; `preload()` from disk; `refreshCurrent`: setlist → store, `null` → clear, `403` → clear + failure, other failure → keep + failure; errors via `toAppError()`) in `core/data/setlist/SundaySetlistRepositoryImpl.kt` (contract §2, research R2)
- [X] T010 Create `core/di/SetlistModule.kt`: provide `SetlistApi` from `@AuthedRetrofit`, bind `SundaySetlistRepository`, `@IntoSet Preloadable` and `@IntoSet SessionScopedCache` to the implementation (research R10)
- [X] T011 [P] Create `ObserveSundaySetlistUseCase` (stored setlist while `DateProvider.today() <= date`, else `null`) in `core/domain/setlist/ObserveSundaySetlistUseCase.kt`
- [X] T012 Create `SyncSundaySetlistUseCase` (not logged in via `SessionPresenceProvider` or not `isWorshipMember` → `clear()`; else `refreshCurrent()`) in `core/domain/setlist/SyncSundaySetlistUseCase.kt` (research R8)
- [X] T013 [P] Test fakes: `core/testing/FakeSundaySetlistRepository.kt`, `core/testing/FakeWorshipAccessRepository.kt`, `core/testing/FakeWallClock.kt`
- [X] T014 [P] Profile tests: flags absent → false, present → mapped in `features/profile/data/dto/MeProfileDtoBackwardCompatibilityTest.kt`; live flags and `NONE` without data in `features/profile/data/access/ProfileWorshipAccessRepositoryTest.kt`
- [X] T015 [P] Core setlist tests: every weekday in `core/domain/setlist/SetlistDateTest.kt`; visible through Sunday, hidden Monday in `core/domain/setlist/ObserveSundaySetlistUseCaseTest.kt`; logged out / not member clears, member refreshes in `core/domain/setlist/SyncSundaySetlistUseCaseTest.kt`; store/null/403/network/preload/clear in `core/data/setlist/SundaySetlistRepositoryImplTest.kt` (fake `SetlistApi`, temp-dir storage via `core/testing/TempDirContext.kt`)

**Checkpoint**: foundation ready — stories can start.

## Phase 3: User Story 1 — The repertoire draft survives closing the app (P1) 🎯 MVP

**Goal**: Repertório rows persist on the device with a sliding 1-hour expiry; "Limpar repertório" empties them.

**Independent test**: fill rows, kill the app, reopen within the hour (rows back) and after it (empty).

- [X] T016 [P] [US1] Create `RepertoireDraft` and `DraftRow` in `features/worshiphub/tables/domain/model/RepertoireDraft.kt` and `DraftExpiry` (`DRAFT_TTL_MS = 3_600_000`, `isExpired(updatedAt, now)`, negative difference = not expired) in `features/worshiphub/tables/domain/usecase/DraftExpiry.kt`
- [X] T017 [P] [US1] Create `RepertoireDraftRepository` (`load(): RepertoireDraft?`, `save(draft)`, `clear()`) in `features/worshiphub/tables/domain/repository/RepertoireDraftRepository.kt`
- [X] T018 [US1] Create `RepertoireDraftDto` in `features/worshiphub/tables/data/dto/RepertoireDraftDto.kt` and `RepertoireDraftStorage` (implements the repository on the `@SetlistPrefs` DataStore, keys `repertoire_draft_v1` and `repertoire_draft_updated_at`; unreadable JSON → `null` + clear) in `features/worshiphub/tables/data/local/RepertoireDraftStorage.kt`; bind it in `features/worshiphub/tables/di/SongsTable.kt`
- [X] T019 [US1] Create `RestoreRepertoireDraftUseCase` (load; expired → clear + `null`; rows with songs missing from the given catalog emptied), `SaveRepertoireDraftUseCase` (rows + `WallClock.nowMillis()`) and `ClearRepertoireDraftUseCase` in `features/worshiphub/tables/domain/usecase/RepertoireDraftUseCases.kt`
- [X] T020 [US1] In `features/worshiphub/tables/presentation/viewmodel/SongsTableViewModel.kt`: restore once when `observeAllSongs()` first emits `SnapshotState.Data`; save the draft after `selectSong`, `onToneChange`, `toggleFixed` and `syncRepertoireFromSuggestions` (never after the restore); add `clearRepertoire()` (empty rows + clear draft)
- [X] T021 [US1] Add the "Limpar repertório" action to `features/worshiphub/tables/presentation/tabs/SuggestionsTab.kt` and pass `onClearClick` through `SongsTableActions` in `features/worshiphub/tables/presentation/screens/SongsTableScreen.kt`
- [X] T022 [P] [US1] Tests: `features/worshiphub/tables/domain/usecase/DraftExpiryTest.kt`; restore within/after 1 h, missing song emptied, clear in `features/worshiphub/tables/domain/usecase/RepertoireDraftUseCasesTest.kt` (fake repository + `FakeWallClock`); storage round-trip and corrupt JSON in `features/worshiphub/tables/data/local/RepertoireDraftStorageTest.kt`; ViewModel waits for the catalog, saves on each edit, not on restore, clear in `features/worshiphub/tables/presentation/viewmodel/SongsTableViewModelDraftTest.kt`

**Checkpoint**: US1 deliverable on its own.

## Phase 4: User Story 2 — A leader saves the Sunday setlist (P1)

**Goal**: users with `can_save_setlist` confirm the Sunday date and save the filled rows; result shown one-shot; the stored setlist updates at once.

**Independent test**: save two rows as leader, read the server, save again and check it replaced.

- [X] T023 [P] [US2] Create `SaveSetlistResult` and `SaveSetlistFailure` (`NoPermission`, `Invalid(error)`, `MissingSongs(count)`, `NoConnection`, `Other(error)`) in `features/worshiphub/tables/domain/model/SaveSetlistResult.kt`
- [X] T024 [P] [US2] Create `RepertoireValidation.canSave(rows)` (≥1 filled row; every filled row with trimmed tone 1–3 chars) in `features/worshiphub/tables/domain/usecase/RepertoireValidation.kt`
- [X] T025 [P] [US2] Create `SetlistSaveApi` (`PUT api/setlists/{date}/`, body `{"items": [{"song_id","position","tone"}]}`, `Response<SetlistDto>`) with its body DTO in `features/worshiphub/tables/data/api/SetlistSaveApi.kt`
- [X] T026 [US2] Create `SetlistSaveRepository` port in `features/worshiphub/tables/domain/repository/SetlistSaveRepository.kt` and `SetlistSaveRepositoryImpl` (maps 200 → `Saved(toDomain())`; 403 → `NoPermission`; 400 → `Invalid`; 404 → `MissingSongs` counting the JSON array in `AppError.Server.extras["missing_song_ids"]`; `AppError.Network` → `NoConnection`; else `Other`) in `features/worshiphub/tables/data/repository/SetlistSaveRepositoryImpl.kt`; provide the API and bind in `features/worshiphub/tables/di/SongsTable.kt` (contract §2)
- [X] T027 [US2] Create `SaveSundaySetlistUseCase` (filled rows only, positions kept, tone trimmed; on `Saved` calls `SundaySetlistRepository.store`) in `features/worshiphub/tables/domain/usecase/SaveSundaySetlistUseCase.kt`
- [X] T028 [P] [US2] Create `RepertoireTexts` (saved "Repertório de domingo dd/MM salvo.", permission, missing songs singular/plural, no connection, `Invalid`/`Other` via `toUserMessage()`, dialog title "Salvar repertório de domingo dd/MM?") in `features/worshiphub/tables/presentation/viewmodel/RepertoireTexts.kt`
- [X] T029 [US2] In `features/worshiphub/tables/presentation/viewmodel/SongsTableViewModel.kt`: inject `ObserveWorshipAccessUseCase`, `DateProvider`, `SaveSundaySetlistUseCase`; expose `repertoireUi: StateFlow` with `canSave`, `isSaveEnabled`, `isSaving`, `pendingSaveDate`; add `requestSave()` (sets `setlistDateFor(today)`), `dismissSave()`, `confirmSave()`; emit `RepertoireEvent.Saved(date)` / `SaveFailed(message)` on a `SharedFlow`
- [X] T030 [US2] Buttons "Gerar | Salvar | Compartilhar" (Salvar only when `canSave`, disabled unless `isSaveEnabled`, progress while saving) in `features/worshiphub/tables/presentation/tabs/SuggestionsTab.kt`; confirmation dialog, snackbar for `RepertoireEvent` and new actions in `features/worshiphub/tables/presentation/screens/SongsTableScreen.kt`
- [X] T031 [P] [US2] Tests: `features/worshiphub/tables/domain/usecase/RepertoireValidationTest.kt`; each answer mapping incl. missing-song count in `features/worshiphub/tables/data/repository/SetlistSaveRepositoryTest.kt` (fake API); filled rows/positions sent and store on success, nothing stored on failure in `features/worshiphub/tables/domain/usecase/SaveSundaySetlistUseCaseTest.kt`; `features/worshiphub/tables/presentation/viewmodel/RepertoireTextsTest.kt`; Salvar hidden without flag, dialog date, events, draft untouched on failure in `features/worshiphub/tables/presentation/viewmodel/SongsTableViewModelSaveTest.kt`

**Checkpoint**: US1 + US2 — leaders save; the stored setlist exists on the saver's device.

## Phase 5: User Story 3 — The band sees the Sunday setlist in Letras and Cifras (P1)

**Goal**: worship members see "Repertório de domingo dd/MM" above manual pins, offline, until the end of that Sunday; refreshed on start and resume.

**Independent test**: save from another device, open Letras (section shown), airplane mode + restart (still shown), Monday (gone).

- [X] T032 [P] [US3] Create `SundaySection`, `SundaySectionEntry` and `buildSundaySection(setlist, contentBySongId)` (position order, only songs with content, `null` when empty) in `features/worshiphub/shared/domain/SundaySection.kt` (research R13)
- [X] T033 [P] [US3] Create `SundaySectionList` composable (title "Repertório de domingo dd/MM", one row per entry with tone chip, tap → `onItemClick(contentId)`, with `@Preview`) in `features/worshiphub/shared/presentation/components/SundaySectionList.kt` and add the optional `sundaySection: SundaySection? = null` parameter rendered above the rows in `features/worshiphub/shared/presentation/components/SongContentListScreen.kt`
- [X] T034 [US3] In `features/worshiphub/lyrics/presentation/viewmodel/LyricsViewModel.kt` and `features/worshiphub/lyrics/presentation/state/LyricsUiState.kt`: add `ObserveSundaySetlistUseCase()` to the `combine`, build the section from lyrics by song id, drop its song ids from the pinned/rest list; pass it in `features/worshiphub/lyrics/presentation/screens/LyricsScreen.kt`
- [X] T035 [US3] Same for chord charts in `features/worshiphub/chordcharts/presentation/viewmodel/ChordChartsViewModel.kt`, `features/worshiphub/chordcharts/presentation/state/ChordChartsUiState.kt`, `features/worshiphub/chordcharts/presentation/screens/ChordChartsScreen.kt`
- [X] T036 [US3] In `core/presentation/viewmodel/CoreViewModel.kt`: run `SyncSundaySetlistUseCase` after the profile refresh in the boot cascade and in `onAppForeground()` (failures only logged); collect `ObserveWorshipAccessUseCase()` and clear the stored setlist when `isWorshipMember` turns false (FR-019, FR-020)
- [X] T037 [P] [US3] Tests: order, content filter, empty → null in `features/worshiphub/shared/domain/SundaySectionTest.kt`; section above pins, no duplicates, hidden after Sunday in `features/worshiphub/lyrics/presentation/viewmodel/LyricsViewModelTest.kt` and `features/worshiphub/chordcharts/presentation/viewmodel/ChordChartsViewModelTest.kt` (create if absent); sync on boot/foreground and clear on flag loss in `core/presentation/viewmodel/CoreViewModelSetlistTest.kt`

**Checkpoint**: setlist distribution works by fallback (start/resume) without push.

## Phase 6: User Story 4 — Devices receive push notifications (P1)

**Goal**: FCM integrated; token registered after login, on start and rotation; unregistered before logout; `setlist_saved` refreshes and notifies; tap opens Letras.

**Independent test**: background device receives "Repertório de domingo dd/MM disponível" after a save; tap opens Letras; after logout nothing arrives.

- [X] T038 [P] [US4] Create `PushMessage` (`SetlistSaved(date)`, `ConfirmPlays(date)`, `parse(data: Map<String, String>): PushMessage?`) in `core/domain/push/PushMessage.kt`
- [X] T039 [P] [US4] Create ports `PushTokenSource` (`suspend fun currentToken(): String?`), `PushRegistrationScheduler` (`schedule()`, `cancel()`), `DevicesRepository` (`register(token): Result<Unit>`, `unregister(token): Result<Unit>`) and `RegisteredTokenStore` (`get()`, `set(token)`, `clear()`) in `core/domain/push/`
- [X] T040 [US4] Add `firebase-messaging = { module = "com.google.firebase:firebase-messaging" }` to `gradle/libs.versions.toml` and `implementation(libs.firebase.messaging)` to `app/build.gradle.kts`
- [X] T041 [P] [US4] Create `DevicesApi` (`POST api/me/devices/`, `POST api/me/devices/unregister/`, body `{"token"}`) in `core/data/push/DevicesApi.kt` and `DevicesRepositoryImpl` (errors via `toAppError()`) in `core/data/push/DevicesRepositoryImpl.kt`
- [X] T042 [P] [US4] Add `@PushPrefs` qualifier and `push_prefs` DataStore to `core/di/DataStoreModule.kt`; create `PushTokenStore` (implements `RegisteredTokenStore`, key `registered_push_token`) in `core/data/push/PushTokenStore.kt`
- [X] T043 [P] [US4] Create `FcmTokenSource` (`Tasks.await(FirebaseMessaging.getInstance().token)` on IO, failure → `null`) in `core/data/push/FcmTokenSource.kt`
- [X] T044 [US4] Create `RegisterDeviceUseCase` (not logged in → skip; token from source; `register`; store on success; returns retry/fail/done) and `UnregisterDeviceUseCase` (cancel scheduler; stored token → `unregister` inside `withTimeoutOrNull(5_000)`; swallow + log failures; always clear store) in `core/domain/push/`
- [X] T045 [US4] Create `PushTokenSyncWorker` (`@HiltWorker` `CoroutineWorker` running `RegisterDeviceUseCase`; network error → `retry()`) in `core/data/push/PushTokenSyncWorker.kt` and `WorkManagerPushRegistrationScheduler` (unique `push_token_sync`, `REPLACE`, `CONNECTED` constraint, exponential backoff) in `core/data/push/WorkManagerPushRegistrationScheduler.kt`
- [X] T046 [P] [US4] Create `AppForegroundState` (`@Singleton`, `@Volatile var isForeground`) in `core/data/app/AppForegroundState.kt`; set it from `CoreViewModel.onAppForeground()` and a new `onAppBackground()` called by `LifecycleEventEffect(ON_STOP)` in `core/presentation/navigation/AppNavHost.kt` (research R16)
- [X] T047 [US4] Create `SetlistNotifications` (channel `sunday_setlist` "Repertório"; `showSetlistSaved(date)` "Repertório de domingo dd/MM disponível" and `showConfirmPlays(date)` "Confirmar músicas de domingo", fixed ids, auto-cancel, permission check like `BirthdayNotificationWorker`; `PendingIntent` to `CoreActivity` with `EXTRA_TARGET`/`EXTRA_DATE`, `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`, `SINGLE_TOP | CLEAR_TOP`) in `core/data/push/SetlistNotifications.kt` (contract §3–4)
- [X] T048 [US4] Create `SetlistRefreshWorker` (`@HiltWorker`; gate `AuthStatusProvider.hasValidAccessToken()`; run `SyncSundaySetlistUseCase`; network → `retry()`) and its enqueue helper (unique `setlist_refresh`, `REPLACE`, `CONNECTED`) in `core/data/push/SetlistRefreshWorker.kt`
- [X] T049 [US4] Create `PushMessageHandler` (parse → ignore unknown; `runBlocking` local gates: logged in, stored `isWorshipMember` for `SetlistSaved`; enqueue refresh; notify unless `AppForegroundState.isForeground`; `ConfirmPlays` → notify always) in `core/data/push/PushMessageHandler.kt` (research R7)
- [X] T050 [US4] Create `IpbMessagingService` (`@AndroidEntryPoint`; `onNewToken` → `PushRegistrationScheduler.schedule()`; `onMessageReceived` → `PushMessageHandler.handle(message.data)`) in `core/data/push/IpbMessagingService.kt`; declare it in `app/src/main/AndroidManifest.xml` (`exported="false"`, `com.google.firebase.MESSAGING_EVENT`); create `core/di/PushModule.kt` binding the ports and providing `DevicesApi` from `@AuthedRetrofit`
- [X] T051 [US4] In `core/presentation/viewmodel/CoreViewModel.kt`: schedule registration in `initialize()` when logged in and on `AuthEventBus.Event.LoginSuccess`; in `logout()` call `UnregisterDeviceUseCase` first, before the cache clears and `logoutUseCase()` (FR-012, FR-013)
- [X] T052 [US4] Create `NotificationTarget` (`SundaySetlist`, `ConfirmPlays(date)`, `fromExtras(target: String?, date: String?)`, extra key constants) in `core/presentation/navigation/NotificationTarget.kt`
- [X] T053 [US4] In `core/presentation/CoreActivity.kt` and `app/src/main/AndroidManifest.xml`: `launchMode="singleTop"`; parse extras in `onCreate` and `onNewIntent` into a `MutableStateFlow<NotificationTarget?>`; pass it and an `onTargetHandled` lambda to `AppNavHost` (research R9)
- [X] T054 [US4] Export `fun NavController.navigateToLyrics()` (navigates to `WorshipHubRoutes.Button4`) from `features/worshiphub/lyrics/presentation/navigation/LyricsNavGraph.kt`; in `core/presentation/navigation/AppNavHost.kt` collect the target once `coreViewModel.isPreloading` is false and handle `SundaySetlist` → `navigateToLyrics()`, then call `onTargetHandled()`
- [X] T055 [P] [US4] Tests: payload variants in `core/domain/push/PushMessageTest.kt`; logged out skip, store on success, network failure → retry in `core/domain/push/RegisterDeviceUseCaseTest.kt`; success, failure, timeout all clear and return in `core/domain/push/UnregisterDeviceUseCaseTest.kt`; gates and foreground in `core/data/push/PushMessageHandlerTest.kt` (fake notifier/scheduler behind small interfaces); extras parsing in `core/presentation/navigation/NotificationTargetTest.kt`; unregister runs before `logoutUseCase` and logout completes on failure in `core/presentation/viewmodel/CoreViewModelLogoutTest.kt`

**Checkpoint**: all P1 stories done — setlist saved, distributed by push and by fallback.

## Phase 7: User Story 5 — Sunday-night reminder opens a pre-filled register (P2)

**Goal**: `confirm_plays` notification; tap opens "Registrar domingo" pre-filled from the setlist, or home + message without access.

**Independent test**: deliver `confirm_plays` to a `songs ≥ manage` user, tap, check date and rows; send; date leaves pending.

- [X] T056 [P] [US5] Create `SetlistAdminApi` (`GET api/setlists/{date}/` → `SetlistDto`; `GET api/setlists/pending-confirmation/` → `List<SetlistDto>`) in `features/admin/register/data/api/SetlistAdminApi.kt`
- [X] T057 [US5] Create `SetlistConfirmationRepository` (`byDate(date)`, `pending()`) in `features/admin/register/domain/repository/SetlistConfirmationRepository.kt` and `SetlistConfirmationRepositoryImpl` (core mapper, errors via `toAppError()`) in `features/admin/register/data/repository/SetlistConfirmationRepositoryImpl.kt`; provide API and bind in `features/admin/register/di/WorshipRegisterModule.kt`
- [X] T058 [US5] Create `GetSetlistForDateUseCase` in `features/admin/register/domain/usecase/GetSetlistForDateUseCase.kt`
- [X] T059 [US5] Add `prefillDate: LocalDate?`, `PrefillState` (`None`, `Loading`, `Loaded`, `NotFound`, `Failed(message)`) and `MusicRegistrationEvent.RetryPrefill` to `features/admin/register/presentation/state/MusicRegistrationContract.kt`
- [X] T060 [US5] In `features/admin/register/presentation/viewmodel/MusicRegistrationViewModel.kt`: inject `SavedStateHandle` and read `date`; with a date → `SUNDAY`, fixed `selectedDate`, read the setlist: `Loaded` → rows `max(4, n)` with `selectedSongId`, label `SongLabelFormatter` from title/artist, tone; `404` → empty rows + snackbar "Repertório de dd/MM não encontrado. Preencha as músicas."; other → `Failed` (message via `toUserMessage()`); `RetryPrefill` re-reads; without a date behave as today
- [X] T061 [US5] Lock the date picker when `prefillDate != null` and show prefill loading / error + "Tentar novamente" in `features/admin/register/presentation/components/SundayRegistrationForm.kt` and `features/admin/register/presentation/screens/MusicRegistrationScreen.kt`
- [X] T062 [US5] In `features/admin/panel/presentation/navigation/AdminNavGraph.kt`: route `AdminRegister?date={date}` (nullable string arg, default `null`), `AdminRoutes.register(date)`, `AdminNav.confirmSunday(date)`, and exported `fun NavController.navigateToSundayConfirmation(date: LocalDate)` (navigate to `AppRoutes.ADMIN_GRAPH`, then the register route)
- [X] T063 [US5] In `core/presentation/navigation/AppNavHost.kt`: handle `NotificationTarget.ConfirmPlays(date)` — logged in and `Access.allows(SONGS, MANAGE)` (expose `canManageSongs` from `CoreViewModel`) → `navigateToSundayConfirmation(date)`; otherwise `Toast` "Você não tem mais acesso ao registro de músicas." and stay home
- [X] T064 [P] [US5] Tests: repository 200/404/network in `features/admin/register/data/repository/SetlistConfirmationRepositoryImplTest.kt`; use case happy + error in `features/admin/register/domain/usecase/GetSetlistForDateUseCaseTest.kt`; prefill loaded (rows, order, tones, >4 items), not found, failed + retry, no date unchanged in `features/admin/register/presentation/viewmodel/MusicRegistrationViewModelPrefillTest.kt`; `ConfirmPlays` access decision (pure helper) in `core/presentation/navigation/NotificationTargetTest.kt`

**Checkpoint**: reminder closes the loop.

## Phase 8: User Story 6 — Admin card lists Sundays pending confirmation (P3)

**Goal**: `songs ≥ manage` users see pending Sundays newest first; tap opens the pre-filled register; refreshed on return.

**Independent test**: two pending Sundays shown; register one from the card; back shows only the other.

- [X] T065 [US6] Create `GetPendingConfirmationsUseCase` in `features/admin/register/domain/usecase/GetPendingConfirmationsUseCase.kt`
- [X] T066 [US6] Add `PendingConfirmationsUi` (`Hidden`, `Loading`, `Failed(message)`, `Dates(dates)`) and `pending` to `features/admin/panel/presentation/state/AdminPanelUiState.kt`; in `features/admin/panel/presentation/viewmodel/AdminPanelViewModel.kt` add `refreshPending()` (`Hidden` without `SONGS ≥ MANAGE`; empty → `Hidden`; `403` → `Hidden`; failure → `Failed`)
- [X] T067 [US6] Pending card ("Confirmar músicas de domingo", dates dd/MM, loading, error + "Tentar novamente", tap → `nav.confirmSunday(date)`) above the grid and `LifecycleEventEffect(ON_RESUME) { refreshPending() }` in `features/admin/panel/presentation/screens/AdminScreen.kt`; preview in `features/admin/panel/presentation/screens/AdminPanelDesignPreviews.kt`
- [X] T068 [P] [US6] Tests: hidden without access and with empty list, dates newest first, failure state, refresh re-reads in `features/admin/panel/presentation/viewmodel/AdminPanelViewModelPendingTest.kt`; use case happy + error in `features/admin/register/domain/usecase/GetPendingConfirmationsUseCaseTest.kt`
- [X] T075 [US6] Add `SetlistAdminApi.delete` (`DELETE api/setlists/{date}/`, `Response<Unit>`), `SetlistConfirmationRepository.delete(date)` and `DeletePendingSetlistUseCase` (`404` → success) in `features/admin/register/`
- [X] T076 [US6] `PendingConfirmationsUi.Dates.canDelete` from `WorshipAccess.canSaveSetlist`, `deleteSetlist(date)` (removes the date; last one → `Hidden`) and `AdminPanelEvent.ShowMessage` on failure in `features/admin/panel/presentation/`
- [X] T077 [US6] "Remover" button (trash icon + text) at the right of each date, with confirmation dialog in `PendingConfirmationsCard`, snackbar in `AdminScreen`; previews updated
- [X] T078 [P] [US6] Tests: repository delete (204, 404, network), use case (success, 404 = success, network), ViewModel (date removed, card hidden when empty, failure emits snackbar, `canDelete` follows the profile)
- [X] T079 Update `specs/admin/spec.md` §2 (pending card delete action)

## Phase 9: Polish & Cross-Cutting

- [X] T069 [P] Update `specs/worshiphub/spec.md` §2.4 (draft, Salvar, Limpar), §4.1 and §5.1 (Repertório de domingo section)
- [X] T070 [P] Update `specs/admin/spec.md` §2 (pending card) and §3 (prefilled entry `?date=`)
- [X] T071 [P] Update `specs/core/spec.md`: profile contract flags, worship access port, Sunday setlist store, push lifecycle (register/unregister, service, workers, channel), `NotificationTarget` and `singleTop`, §4.4.1 wording ("in memory or on disk"), §6.2 `CoreViewModel` boot/logout
- [X] T072 Update the "Single Activity" line of `CLAUDE.md` to note the `IpbMessagingService` exception
- [X] T073 Build and run all unit tests: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest -q`; fix failures
- [ ] T074 Run the manual checks 1–9 in `specs/011-sunday-setlist-push/quickstart.md` on a device against the test server

## Dependencies & Execution Order

- **Phase 2** blocks every story.
- **US1** (Phase 3): only Phase 2 (`WallClock`). MVP.
- **US2** (Phase 4): Phase 2; touches the same ViewModel/tab as US1 — do after US1 to avoid conflicts.
- **US3** (Phase 5): Phase 2. Independent of US1/US2 (testable with a setlist saved from another device).
- **US4** (Phase 6): Phase 2; T054 uses the lyrics graph; the refresh path reuses `SyncSundaySetlistUseCase` (Phase 2). Best after US3 so the notification lands on a visible section.
- **US5** (Phase 7): T063 needs T052–T054 (target plumbing from US4); the rest only Phase 2.
- **US6** (Phase 8): needs T057 (repository) and T062 (`confirmSunday`) from US5.
- **Polish**: after the stories it documents.

Within a story: models/ports → data → use cases → ViewModel → UI → tests (tests may be written first against the ports).

## Parallel Opportunities

- Phase 2: T001, T002, T004, T005, T006, T007 together; then T013–T015 together.
- US1: T016 ∥ T017; T022 once T019–T020 exist.
- US2: T023 ∥ T024 ∥ T025 ∥ T028.
- US3: T032 ∥ T033; T034 ∥ T035 after them.
- US4: T038 ∥ T039 ∥ T041 ∥ T042 ∥ T043 ∥ T046; T040 before T043/T050.
- US5: T056 ∥ T059.
- Polish: T069 ∥ T070 ∥ T071.

### Example (US4)

```text
Task: "T038 PushMessage in core/domain/push/PushMessage.kt"
Task: "T041 DevicesApi + DevicesRepositoryImpl in core/data/push/"
Task: "T042 @PushPrefs + PushTokenStore"
Task: "T046 AppForegroundState + AppNavHost ON_STOP"
```

## Implementation Strategy

1. **MVP**: Phase 2 + US1 — the draft fix ships alone (no server, no push).
2. **+ US2 + US3**: leaders save; the band sees the section via start/resume fallback.
3. **+ US4**: push makes distribution immediate.
4. **+ US5 + US6**: confirmation loop (reminder and catch-up card).
5. Polish: domain specs, `CLAUDE.md`, full test run, manual checks. Spec and code go in the same commits per story.
