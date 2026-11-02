# Tasks: Password Recovery by E-mail

**Input**: Design documents from `specs/021-password-recovery/`

**Tests**: Required by `CLAUDE.md`: happy path plus at least one error per use case, fakes preferred.

**Paths**: `features/` and `core/` are under `app/src/main/java/com/ipb/castelobranco/`; tests mirror them under
`app/src/test/java/com/ipb/castelobranco/`.

## Phase 1: Setup (specs first)

- [X] T001 [P] Describe the recovery route, the login link and the registration e-mail in `specs/auth/spec.md`
- [X] T002 [P] Describe the recovery section, its dialogs, its model fields and the sealed cache in `specs/profile/spec.md`, removing "o e-mail não é editável"
- [X] T003 [P] Describe the notice in the highlight slot in `specs/core/spec.md`

## Phase 2: Foundational

- [X] T004 Add `EmailRules.isValid` in `features/auth/domain/model/RegisterRules.kt` with cases in `features/auth/domain/usecase/PasswordRecoveryUseCasesTest.kt`

## Phase 3: User Story 1 - Recover a forgotten password (P1)

**Goal**: A signed-out person sets a new password with a code received by e-mail.

**Independent Test**: Follow the link from the login, request a code, type it with a new password, sign in.

- [X] T005 [US1] Add recovery paths in `features/auth/data/api/AuthEndpoints.kt`, DTOs in `features/auth/data/dto/PasswordRecoveryRequests.kt` and calls in `features/auth/data/api/AuthApi.kt`
- [X] T006 [US1] Add `requestPasswordRecovery` and `confirmPasswordRecovery` to `features/auth/domain/repository/AuthRepository.kt` and `features/auth/data/repository/AuthRepositoryImpl.kt`, storing no tokens
- [X] T007 [P] [US1] Create `features/auth/domain/usecase/RequestPasswordRecoveryUseCase.kt` and `ConfirmPasswordRecoveryUseCase.kt` with tests for success and failure
- [X] T008 [US1] Create `features/auth/presentation/state/PasswordRecoveryState.kt` (data-model.md)
- [X] T009 [US1] Create `features/auth/presentation/viewmodel/PasswordRecoveryViewModel.kt`: steps, local checks, 60 s countdown, field and banner errors, `Done` event
- [X] T010 [US1] Test the view model in `features/auth/presentation/viewmodel/PasswordRecoveryViewModelTest.kt`: same answer for any address, wrong code keeps input, field errors, network error, countdown, resend, back keeps address, a local check refuses before the network
- [X] T011 [US1] Create `features/auth/presentation/screens/PasswordRecoveryScreen.kt` with both steps and a stateless content composable, reusing `AuthLayout` and `TonalField`
- [X] T012 [US1] Add `AuthRoutes.PASSWORD_RECOVERY` with an entry-scoped view model in `features/auth/presentation/navigation/AuthNavGraph.kt`, sending "Senha alterada" to the graph-scoped `AuthViewModel`
- [X] T013 [US1] Add the "Esqueci minha senha" link and the success banner in `features/auth/presentation/screens/AuthScreen.kt`, the notice state in `features/auth/presentation/viewmodel/AuthViewModel.kt`, the banner in `features/auth/presentation/components/AuthComponents.kt`, and its test in `AuthViewModelTest.kt`

## Phase 4: User Story 2 - Required e-mail at registration (P2)

**Goal**: Every new password account has a recovery e-mail.

**Independent Test**: Register with a valid e-mail and enter the app; an empty or malformed one does not reach the server.

- [X] T014 [US2] Add `recovery_email` to `features/auth/data/dto/RegisterRequest.kt` and an `email` parameter through `AuthRepository`, `AuthRepositoryImpl` and `features/auth/domain/usecase/RegisterUseCase.kt`
- [X] T015 [US2] Add `email` to `features/auth/domain/model/RegisterErrors.kt` and map `recovery_email` in both branches of `parseRegisterError` in `features/auth/data/mapper/AuthErrorMapper.kt`
- [X] T016 [US2] Add the field, help line and validity rule in `features/auth/presentation/state/RegisterFormState.kt` and `onEmailChange`, trimmed sending, in `AuthViewModel.kt`
- [X] T017 [US2] Add the "E-mail" field with an e-mail keyboard in `features/auth/presentation/screens/RegisterScreen.kt`
- [X] T018 [US2] Update and extend `RegisterFormStateTest.kt`, `AuthViewModelTest.kt`, `RegisterUseCaseTest.kt`, `AuthRepositoryImplTest.kt` and the mapper test

## Phase 5: User Story 3 - Manage the recovery e-mail in the profile (P3)

**Goal**: A signed-in person with a password sees, adds, changes and confirms the address.

**Independent Test**: Add or change with the password, confirm with the code, see the status change. Google-only: nothing new.

- [X] T019 [US3] Add `recovery_email` and `recovery_email_confirmed` to `features/profile/data/dto/MeProfileDto.kt`; create `features/profile/domain/model/RecoveryEmailStatus.kt`; add it to `MeProfile.kt` and map it in `features/profile/data/snapshot/ProfileSnapshotRepository.kt`
- [X] T020 [US3] Create `features/profile/data/local/RecoveryEmailVault.kt` on `@AuthPrefs` behind a small interface
- [X] T021 [US3] Create `features/profile/data/snapshot/SealedProfileSnapshotCache.kt` (research R-01, R-02) and wrap the cache in `features/profile/di/ProfileSnapshotModule.kt`
- [X] T022 [US3] Contribute the vault as a `SessionScopedCache` in `features/profile/di/ProfileModule.kt` (research R-03)
- [X] T023 [US3] Test in `features/profile/data/snapshot/SealedProfileSnapshotCacheTest.kt`: address never reaches the inner cache, load restores it, old snapshot is unknown, wiped vault is unknown with no ETag, clear erases both; and status mapping in `RecoveryEmailStatusTest.kt`
- [X] T024 [US3] Add endpoints to `features/profile/data/api/ProfileEndpoints.kt` and `ProfileApi.kt`, DTOs to `features/profile/data/dto/AccountDtos.kt`, and methods to `AccountRepository.kt`, `AccountRepositoryImpl.kt` and the test fake `FakeAccountRepository.kt`
- [X] T025 [P] [US3] Create `ChangeRecoveryEmailUseCase.kt`, `SendRecoveryEmailCodeUseCase.kt` and `ConfirmRecoveryEmailUseCase.kt` in `features/profile/domain/usecase/` with tests
- [X] T026 [US3] Add `recoveryEmail`, `recoveryEmailForm` and `recoveryCodeDialog` to `features/profile/presentation/state/ProfileUiState.kt`
- [X] T027 [US3] Add the form and code flows in `features/profile/presentation/viewmodel/ProfileViewModel.kt` per contracts/recovery-client.md, and test them in `ProfileViewModelTest.kt`
- [X] T028 [US3] Create `features/profile/presentation/components/RecoveryEmailSection.kt` with the card and both dialogs
- [X] T029 [US3] Wire them in `features/profile/presentation/screens/ProfileScreen.kt`, hidden unless `hasPassword == true`

## Phase 6: User Story 4 - One-time notice for existing accounts (P4)

**Goal**: An existing account with a password and no e-mail learns about it once, unblocked.

**Independent Test**: The card shows; "Agora não" and reopening: it does not return.

- [X] T030 [US4] Create `features/profile/domain/repository/RecoveryNoticeRepository.kt` and `features/profile/data/local/RecoveryNoticePreferences.kt` on `@SettingsPrefs`, bound in `ProfileModule.kt`
- [X] T031 [US4] Add `showRecoveryNotice` and the answer action in `ProfileUiState.kt` and `ProfileViewModel.kt`, with tests for each condition in `ProfileViewModelTest.kt`
- [X] T032 [US4] Add the optional form argument to the profile route in `core/presentation/navigation/AppRoutes.kt` and `AppNavHost.kt`, opening the form from `ProfileScreen.kt`
- [X] T033 [US4] Draw the card in the highlight slot, with internal scroll, in both layouts in `core/presentation/screens/CoreScreen.kt`

## Phase 7: Polish

- [X] T034 Run `./gradlew :app:testDebugUnitTest` and `./gradlew :app:assembleDebug`
- [X] T035 Confirm no log line carries the address with a search for `Timber` in the changed files
- [ ] T036 Run the manual and privacy checks in `specs/021-password-recovery/quickstart.md` against a running backend (needs a device and the Brevo key)

## Dependencies

- Phase 1 comes before all code (spec first). T004 comes before US1 and US2.
- US1 and US2 are independent. Both touch `AuthRepository` and `AuthViewModel`, so they run in sequence.
- US4 depends on US3: it uses `RecoveryEmailStatus` and the form.

## Parallel Opportunities

- T001, T002 and T003 touch different files.
- T007 and T025 only need their repository method.

## Implementation Strategy

**MVP**: US1 plus US2: the recovery flow and new accounts that can use it. The backend refuses registration without an e-mail
from the moment it is live, so the first release needs US2 anyway. US3 and US4 give existing accounts a way in and go in the same
release.
