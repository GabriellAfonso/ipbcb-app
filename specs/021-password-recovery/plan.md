# Implementation Plan: Password Recovery by E-mail

**Branch**: `021-password-recovery` | **Date**: 2026-11-02 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/021-password-recovery/spec.md`

## Summary

Add the recovery flow to the login, a required e-mail to password registration, a recovery e-mail card to the profile
and a one-time card on the home. The server side (`backend/specs/028-password-recovery`) owns codes, limits and
delivery. The app's one real design problem is keeping the address out of the plain profile snapshot: a decorator around
the snapshot cache moves it to the sealed `@AuthPrefs` store on save and puts it back on load.

## Technical Context

**Language/Version**: Kotlin, Jetpack Compose

**Primary Dependencies**: Hilt, Retrofit + kotlinx.serialization, DataStore, Navigation Compose. No new library.

**Storage**: `@AuthPrefs` (sealed, Keystore AES-GCM) for the address; `@SettingsPrefs` for the notice flag; the existing
`me_profile` snapshot keeps only the confirmed flag.

**Testing**: JUnit4, MockK, kotlinx-coroutines-test, turbine; fakes preferred.

**Target Platform**: Android

**Project Type**: Mobile app, single module `:app`

**Performance Goals**: None specific; no new work on the boot beyond one extra DataStore read.

**Constraints**: No plain e-mail on disk or in logs (FR-021, FR-022). Older cached profiles must decode (FR-023). The home
does not scroll (FR-019).

**Scale/Scope**: One new auth route with two steps, one profile card, two dialogs, one home card.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`specs/constitution.md` covers error handling; `CLAUDE.md` holds the architecture rules.

| Rule | How this plan meets it |
|------|------------------------|
| `AppError` is the only error crossing layers | Repositories use `toAppError()` / `mapError()`; form errors read `AppError.Server.fieldErrors` and `errorCode` |
| One parsing point for HTTP errors | `retry_after_seconds` is read from `AppError.Server.extras`, filled by `ResponseExt.kt` |
| `userMessage` only from a structured `detail` | "Código inválido ou expirado." and "Senha incorreta." come from the server's `detail` through `toUserMessage()` |
| Features never import each other | The home reads the profile through `ProfileViewModel`, as it already does; the auth feature imports nothing from profile |
| `domain/` has no Android | `RecoveryEmailStatus` and the validators are plain Kotlin |
| Secrets and PII | Address sealed in `@AuthPrefs`, erased with the session, never logged. HTTP logging stays `BASIC` in debug and `NONE` in release, which shows no bodies |
| Screen and content split | Every new screen has a stateless content composable |

**Result**: pass, before and after design. No violation to justify.

## Project Structure

### Documentation (this feature)

```text
specs/021-password-recovery/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/recovery-client.md
└── tasks.md
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/presentation/
│   ├── navigation/{AppRoutes,AppNavHost}.kt          # optional "open the e-mail form" argument on the profile route
│   └── screens/CoreScreen.kt                         # notice card in the highlight slot
└── features/
    ├── auth/
    │   ├── data/{api,dto,mapper,repository}          # recovery endpoints, recovery_email at register
    │   ├── domain/{model,repository,usecase}         # EmailRules, recovery use cases
    │   └── presentation/
    │       ├── navigation/AuthNavGraph.kt            # AuthRoutes.PASSWORD_RECOVERY
    │       ├── screens/{AuthScreen,RegisterScreen,PasswordRecoveryScreen}.kt
    │       ├── state/{RegisterFormState,PasswordRecoveryState}.kt
    │       └── viewmodel/{AuthViewModel,PasswordRecoveryViewModel}.kt
    └── profile/
        ├── data/
        │   ├── api, dto, repository                  # recovery e-mail endpoints and fields
        │   ├── local/{RecoveryEmailVault,RecoveryNoticePreferences}.kt
        │   └── snapshot/SealedProfileSnapshotCache.kt
        ├── di/{ProfileModule,ProfileSnapshotModule}.kt
        ├── domain/{model/RecoveryEmailStatus,repository,usecase}
        └── presentation/
            ├── components/RecoveryEmailSection.kt    # card and both dialogs
            ├── screens/ProfileScreen.kt
            ├── state/ProfileUiState.kt
            └── viewmodel/ProfileViewModel.kt
```

**Structure Decision**: The existing feature packages. The notice is decided in `ProfileViewModel`, which the home already
holds for the greeting; `CoreScreen` only draws it.

## Implementation order

1. Spec updates (`specs/auth`, `specs/profile`, `specs/core`), then the code.
2. US2 registration, the smallest slice and a server requirement.
3. US1 recovery flow.
4. Foundation for US3 and US4: DTO fields, `RecoveryEmailStatus`, vault, sealed cache.
5. US3 profile card and dialogs.
6. US4 notice.

## Release

Done by the user, never by Claude. Order: submit to Play with managed publishing, wait for approval, deploy the backend,
publish the app at once.
