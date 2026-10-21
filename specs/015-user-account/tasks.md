# Tasks: User Account

**Input**: Design documents from `specs/015-user-account/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/profile-screen.md

**Tests**: requested (use cases and ViewModels: happy path + at least one error; token swap and post-deletion
cleanup get dedicated cases).

Paths are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and `app/src/test/java/com/ipb/castelobranco/`
(test) unless they start with `specs/`.

## Phase 1: Setup

- [X] T001 Write the profile domain spec (current state + account) in specs/profile/spec.md

## Phase 2: Foundational (blocks every story)

- [X] T002 Add nullable `username`, `has_password`, `date_joined` to features/profile/data/dto/MeProfileDto.kt
- [X] T003 Add `username`, `hasPassword`, `dateJoined: LocalDate?` to features/profile/domain/model/MeProfile.kt and
  map them (safe date parse) in features/profile/data/snapshot/ProfileSnapshotRepository.kt
- [X] T004 [P] Extend the old-profile decode test and cover the new fields in
  features/profile/data/dto/MeProfileDtoBackwardCompatibilityTest.kt (test)
- [X] T005 Add request/response DTOs in features/profile/data/dto/AccountDtos.kt and the PATCH profile, POST password,
  POST delete (password and Google bodies) endpoints in features/profile/data/api/ProfileApi.kt and ProfileEndpoints.kt;
  make features/profile/data/api/FakeProfileApi.kt (test) compile
- [X] T006 Create `AccountDeletionProof` in features/profile/domain/model/AccountDeletionProof.kt and
  `AccountRepository` in features/profile/domain/repository/AccountRepository.kt
- [X] T007 Create `SessionTokensWriter` in core/domain/auth/SessionTokensWriter.kt, implement it in
  core/data/auth/TokenStorageSessionTokensWriter.kt and bind it in core/di/AppInfoModule.kt
- [X] T008 Implement features/profile/data/repository/AccountRepositoryImpl.kt (writes the new pair through
  `SessionTokensWriter` before returning) and bind it in features/profile/di/ProfileModule.kt
- [X] T009 [P] Test AccountRepositoryImpl (name, password pair stored on 200 and untouched on error, both deletion
  bodies, error mapping) in features/profile/data/repository/AccountRepositoryImplTest.kt (test)
- [X] T010 Restructure features/profile/presentation/state/ProfileUiState.kt (sections, dialog states, `isMember`
  removed) and add features/profile/presentation/state/ProfileUiEvent.kt

## Phase 3: User Story 1 — Delete my account (P1)

**Goal**: delete with password or Google + "EXCLUIR"; session and caches cleared; home signed out.

**Independent test**: CoreViewModel test (event → full cleanup, no server unregister) + DeleteAccountUseCase test +
ProfileViewModel deletion tests.

- [X] T011 [US1] Add `AccountDeleted` to core/domain/auth/AuthEventBus.kt
- [X] T012 [US1] Add `notifyServer` to `UnregisterDeviceUseCase` in core/domain/push/DeviceRegistrationUseCases.kt
- [X] T013 [US1] Extract `endSession(notifyServer)` shared by `logout()` and the `AccountDeleted` handler in
  core/presentation/viewmodel/CoreViewModel.kt
- [X] T014 [P] [US1] Test the AccountDeleted cleanup (every logout step, no network unregister, local store cleared)
  in core/presentation/viewmodel/CoreViewModelTest.kt (test)
- [X] T015 [US1] Create features/profile/domain/usecase/DeleteAccountUseCase.kt (emits `AccountDeleted` on success);
  the "EXCLUIR" check lives in features/profile/domain/model/AccountDeletionProof.kt
- [X] T016 [P] [US1] Test DeleteAccountUseCase in features/profile/domain/usecase/DeleteAccountUseCaseTest.kt (test)
- [X] T017 [US1] Extract the Credential Manager request to core/presentation/auth/GoogleIdToken.kt and use it in
  features/auth/presentation/screens/AuthScreen.kt (same behavior)
- [X] T018 [US1] Deletion flow in features/profile/presentation/viewmodel/ProfileViewModel.kt (open/dismiss, password
  path, Google request event, token result, errors, AccountDeleted event)
- [X] T019 [US1] Delete dialog in features/profile/presentation/components/AccountDialogs.kt; Ações section and
  event handling (Google request, AccountDeleted) in features/profile/presentation/screens/ProfileScreen.kt
- [X] T020 [US1] `onAccountDeleted` → popUpTo CORE + toast "Conta excluída" in core/presentation/navigation/AppNavHost.kt
- [X] T021 [P] [US1] Deletion tests in features/profile/presentation/viewmodel/ProfileViewModelTest.kt (test)

## Phase 4: User Story 2 — Change my password (P2)

**Goal**: change the password and keep this device signed in, race-safe.

**Independent test**: TokenAuthenticator compare-and-set tests + ChangePasswordUseCase + VM tests.

- [X] T022 [US2] Add `swapIfRefresh` to features/auth/data/local/TokenStorage.kt
- [X] T023 [US2] Use compare-and-set on failure and on success in core/network/TokenAuthenticator.kt
- [X] T024 [P] [US2] Test: blacklisted old refresh + new pair already stored does not sign out; a concurrent refresh
  returning a dead pair after the password pair was saved keeps the good pair; in
  core/network/TokenAuthenticatorTest.kt (test)
- [X] T025 [US2] Create features/profile/domain/usecase/ChangePasswordUseCase.kt
- [X] T026 [P] [US2] Test ChangePasswordUseCase in features/profile/domain/usecase/ChangePasswordUseCaseTest.kt (test)
- [X] T027 [US2] Password flow in ProfileViewModel.kt and features/profile/presentation/components/AccountDialogs.kt
- [X] T028 [P] [US2] Password tests in ProfileViewModelTest.kt (test)

## Phase 5: User Story 3 — Account details and name (P2)

- [X] T029 [US3] Create features/profile/domain/usecase/UpdateProfileNameUseCase.kt (trim, blank refused, refresh)
- [X] T030 [P] [US3] Test it in features/profile/domain/usecase/UpdateProfileNameUseCaseTest.kt (test)
- [X] T031 [US3] Conta section (name, login, "No app desde") and name dialog in
  features/profile/presentation/components/AccountSections.kt and AccountDialogs.kt; VM name flow; "Membro" removed
- [X] T032 [P] [US3] Name + details tests in ProfileViewModelTest.kt (test)

## Phase 6: User Story 4 — Remove my photo (P3)

- [X] T033 [US4] Create features/profile/domain/usecase/RemoveProfilePhotoUseCase.kt (delete, then refresh)
- [X] T034 [P] [US4] Test it in features/profile/domain/usecase/RemoveProfilePhotoUseCaseTest.kt (test)
- [X] T035 [US4] Photo menu in AccountSections.kt, confirmation in AccountDialogs.kt; VM remove flow
- [X] T036 [P] [US4] Remove photo tests in ProfileViewModelTest.kt (test)

## Phase 7: Polish

- [X] T037 Run `./gradlew :app:testDebugUnitTest` and `:app:compileDebugKotlin`; fix failures
- [X] T038 Update specs/015-user-account/tasks.md checkboxes and specs/profile/spec.md with anything that changed

## Dependencies

- Phase 2 blocks everything. US1 needs T011–T013 (core) before T018. US2 needs T022–T023 before T027 only for safety
  (the flow works without them). US3 and US4 depend only on Phase 2.
- ProfileViewModel/ProfileScreen tasks (T018, T019, T027, T031, T035) touch the same files: run sequentially.

## Parallel examples

- After T008: T009, T016, T026, T030, T034 (different test files).
- T014 and T024 (core tests) in parallel with the profile UI work.

## Implementation strategy

MVP = Phase 2 + US1 (the Play Store requirement). Then US2, US3, US4. All on one branch; each commit carries its spec
changes.
