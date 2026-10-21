# Implementation Plan: User Account

**Branch**: `015-user-account` | **Date**: 2026-10-21 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/015-user-account/spec.md`

## Summary

The profile screen becomes an account screen in three sections. Photo: a menu with "Trocar foto" (current flow) and
"Remover foto" (existing `DELETE` endpoint, then a profile refresh). Conta: editable name (`PATCH`), login name, and
"No app desde" from three new nullable DTO fields. Ações: "Trocar senha" (new endpoint, the new token pair is written
through a new core port before returning) and "Excluir conta" (new endpoint, password or a fresh Google ID token).

A successful deletion emits `AuthEventBus.Event.AccountDeleted`. `CoreViewModel` (Activity scope, always alive)
ends the session through the same private cleanup body as `logout()`, without the network unregister. The screen
pops to the home and shows a toast. `TokenAuthenticator` becomes compare-and-set: a renewal clears or overwrites the
stored pair only when the pair is still the one it started from. This closes the race with a password change.
Decisions are in [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (JVM 17), Android, Jetpack Compose (Material3)

**Primary Dependencies**: Hilt, Retrofit + OkHttp, kotlinx.serialization, Credential Manager + Google ID
(`androidx.credentials`, `googleid`, already used by `AuthScreen`), uCrop (photo flow, unchanged)

**Storage**: `@AuthPrefs` sealed DataStore (`TokenStorage`), profile snapshot `JsonSnapshotStorage` key `me_profile`

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + turbine; fakes preferred

**Target Platform**: Android (minSdk 24, desugaring on, `java.time` available)

**Project Type**: mobile app, single `:app` module

**Performance Goals**: none beyond the usual; every action is one request plus a profile refresh.

**Constraints**:
- the new pair is stored before any other request;
- deletion never calls logout/unregister;
- no e-mail anywhere;
- 120-char lines;
- user-visible strings in Portuguese.

**Scale/Scope**: one screen reorganized, 4 dialogs, 3 endpoints, 1 core port, 1 auth event, an authenticator change.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Rule (`specs/constitution.md`, `CLAUDE.md`) | Status |
|---|---|
| `AppError` is the only error type crossing data → presentation | Pass: repositories `mapError()`; the VM reads `AppError.Server.fieldErrors` / `toUserMessage()` |
| Only `ResponseExt.kt` parses error bodies | Pass: nothing new parses bodies; field errors and codes come from `AppError` |
| `userMessage` only from `detail` of a structured error, or app text | Pass: dialog messages are the server `detail` or the generic category text |
| 403 is a permission refusal, not a login problem | Pass: `GOOGLE_ACCOUNT_MISMATCH` is a 403 with `detail`, shown as is; no logout |
| Features never import each other | Pass: profile writes tokens through `core/domain/auth/SessionTokensWriter` and requests the Google token through a `core/presentation` helper |
| `domain/` has no Android | Pass: use cases and ports are plain Kotlin |
| Screen/content split, dumb composables, `StateFlow` + `SharedFlow` events | Pass: `ProfileScreen` collects; `ProfileContent` and dialogs are pure |
| Session data never survives the session | Pass: deletion uses the logout cleanup body |

No violations; Complexity Tracking left empty.

## Project Structure

### Documentation (this feature)

```text
specs/015-user-account/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/profile-screen.md   # UI contract: sections, dialogs, events, texts
├── checklists/requirements.md
└── tasks.md
specs/profile/spec.md              # new domain spec (none existed)
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── domain/auth/AuthEventBus.kt                 # + Event.AccountDeleted
│   ├── domain/auth/SessionTokensWriter.kt          # new port
│   ├── data/auth/TokenStorageSessionTokensWriter.kt# new impl over TokenStorage
│   ├── di/AppInfoModule.kt                         # bind the writer
│   ├── network/TokenAuthenticator.kt               # compare-and-set
│   ├── domain/push/DeviceRegistrationUseCases.kt   # UnregisterDeviceUseCase(notifyServer)
│   ├── presentation/auth/GoogleIdToken.kt          # new helper, shared with AuthScreen
│   ├── presentation/viewmodel/CoreViewModel.kt     # endSession() shared by logout and AccountDeleted
│   └── presentation/navigation/AppNavHost.kt       # onAccountDeleted → popUpTo CORE + toast
├── features/auth/
│   ├── data/local/TokenStorage.kt                  # + swapIfRefresh()
│   └── presentation/screens/AuthScreen.kt          # uses the helper (same behavior)
└── features/profile/
    ├── data/api/ProfileApi.kt, ProfileEndpoints.kt # PATCH profile, POST password, POST delete
    ├── data/dto/MeProfileDto.kt                    # + username, has_password, date_joined (nullable)
    ├── data/dto/AccountDtos.kt                     # request/response bodies
    ├── data/repository/AccountRepositoryImpl.kt    # new
    ├── data/snapshot/ProfileSnapshotRepository.kt  # map new fields
    ├── di/ProfileModule.kt                         # bind AccountRepository
    ├── domain/model/MeProfile.kt                   # + username, hasPassword, dateJoined
    ├── domain/model/AccountDeletionProof.kt        # Password | Google
    ├── domain/repository/AccountRepository.kt      # new
    ├── domain/usecase/UpdateProfileNameUseCase.kt, RemoveProfilePhotoUseCase.kt,
    │   ChangePasswordUseCase.kt, DeleteAccountUseCase.kt
    └── presentation/
        ├── state/ProfileUiState.kt                 # sections + dialog states, isMember removed
        ├── state/ProfileUiEvent.kt                 # toasts, Google request, account deleted
        ├── viewmodel/ProfileViewModel.kt
        ├── format/JoinedSinceFormat.kt             # "março de 2025"
        └── screens/ProfileScreen.kt + components/AccountSections.kt, AccountDialogs.kt
```

Tests mirror under `app/src/test/java/...`: DTO backward compatibility, `AccountRepositoryImpl`, each use case,
`ProfileViewModel`, `TokenAuthenticator` (compare-and-set), `CoreViewModel` (deletion cleanup),
`UnregisterDeviceUseCase` (local only).

**Structure Decision**: everything account-related stays in `features/profile`; the only `core/` additions are the
ones a feature cannot own (token writer, auth event, authenticator, shared Google helper, session cleanup).

## Implementation Order

1. Domain spec `specs/profile/spec.md` (current state + account).
2. DTO/model fields + mapper + backward-compatibility test.
3. Core: `SessionTokensWriter`, `TokenStorage.swapIfRefresh`, authenticator compare-and-set + tests.
4. Account API + `AccountRepositoryImpl` + use cases + tests.
5. Core: `AccountDeleted` event, `CoreViewModel.endSession`, `UnregisterDeviceUseCase(notifyServer)` + tests.
6. Google helper; `AuthScreen` switched to it.
7. ViewModel + UI state/events + tests; screen, sections, dialogs; `AppNavHost` wiring.
8. Full unit test run; debug build compiles.

## Complexity Tracking

None.
