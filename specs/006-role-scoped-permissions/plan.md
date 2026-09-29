# Implementation Plan: Role-Scoped Permissions in the App

**Branch**: `006-role-scoped-permissions` | **Date**: 2026-09-28 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/006-role-scoped-permissions/spec.md`

## Summary

Backend 012 replaces `is_admin` in `api/me/profile/` with `roles` and per-scope `permissions`. The app reads them
into a core `Access` model (`core/domain/access/`), fed by the profile snapshot, and every management surface —
drawer entry, panel cards, members actions, hymnal report configuration, worship hub edit buttons — decides
visibility from it. A 403 now reads as a permission problem, triggers one profile refresh, and only a refused
**read** ejects the user from the area.

Key technical choices (details in [research.md](research.md)):

- **Access in core, implementation in profile** (R1): `AccessRepository` interface in core, `ProfileAccessRepository`
  in `features/profile`. Worship hub stops importing `features.profile` directly.
- **Tolerant DTO** (R2): raw strings with empty defaults; unknown → no access; the old cache decodes as "no role".
- **Enum-ordinal comparison** (R3): `Access.allows(scope, minimum)`.
- **403 stays `AppError.Auth`**, only its generic text changes (R4) — no change to cache-clearing semantics.
- **Refresh via interceptor + event bus** (R5): `PermissionDeniedInterceptor` → `AuthEventBus.PermissionDenied` →
  `CoreViewModel` → single-flight refresh.
- **Read/write stated by the caller** (R6).
- **Panel gets a ViewModel** (R7) with card requirements in the domain.

## Technical Context

**Language/Version**: Kotlin (JVM 17 target), Android, `minSdk 24`

**Primary Dependencies**: Jetpack Compose (Material 3), Hilt, Retrofit + OkHttp (`@AuthedRetrofit`, `@Client`),
kotlinx.serialization, kotlinx.coroutines — all already in the project; **no new libraries**

**Storage**: existing profile snapshot (`JsonSnapshotStorage`, key `me_profile`); shape extended, key unchanged

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; fakes preferred (`FakeAccessRepository` for
ViewModel tests)

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: access available at boot from disk (no network wait); a refused action updates visible
actions after one profile round-trip

**Constraints**: features never import each other; level comparison in domain, composables get booleans; deny on
unknown; Portuguese UI strings hardcoded; 120-char lines; ships together with backend 012 (no `is_admin` fallback)

**Scale/Scope**: ~10 new production files (core access + interceptor + panel VM/domain), ~30 touched (profile,
core screen/VM, admin panel/members/reports, worship hub 4 VMs + 4 states + screens), ~8 test classes; 6 spec
documents updated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is the unfilled template; the rules are `specs/constitution.md` and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| Single error type `AppError` across layers | ✅ 403 stays `AppError.Auth(403)`; no raw exception reaches a ViewModel |
| `message` technical, `userMessage` for screen; server text only from structured `detail` | ✅ `ResponseExt` unchanged; interceptor reads the body but never produces text |
| Presentation decides the text (`toUserMessage`) | ✅ new 403 generic added there, not in ViewModels |
| Single HTTP error-parsing point (`ResponseExt`) | ⚠️ the interceptor peeks the body. **Justified**: it reuses `parseApiError` (the same parser `ResponseExt` uses), only reads `error_code` to fire an event, and produces no `AppError`. Mapping stays in one place. See Complexity Tracking |
| Status mapping 401/403 → `Auth` | ✅ kept; constitution gains the note that the generic text differs by code (FR-028) |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| Features never import each other; share via `core/` | ✅ access in `core/domain/access`; removes worship hub → profile import |
| `domain/` without Android | ✅ enums, data class, interface, use cases only |
| Presentation only knows the ViewModel | ✅ flags in UI state |
| StateFlow / SharedFlow, `viewModelScope` | ✅ single-flight uses a `Mutex` in a `@Singleton` repository; no manual scope |
| Screen / content split, dumb composables | ✅ `AdminScreen` → `AdminPanelContent(state, nav)` |
| Graph-scoped VM pitfall | ✅ panel VM is screen-scoped (no sharing); `CoreViewModel` stays Activity-scoped |
| `safePopBackStack` | ✅ area exits use `popBackStack(GRAPH, inclusive = true)` as `membersGraph` already does (targeted pop, not root) |
| Spec first, same commit | ✅ domain specs updated with the code (see Spec updates) |

**Result**: pass, one justified deviation.

## Project Structure

### Documentation (this feature)

```text
specs/006-role-scoped-permissions/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── profile-api.md
│   └── permission-denied.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── domain/access/                 # NEW
│   │   ├── AccessLevel.kt  Scope.kt  Role.kt  Access.kt
│   │   ├── AccessRepository.kt
│   │   └── ObserveAccessUseCase.kt  RefreshAccessUseCase.kt
│   ├── domain/auth/AuthEventBus.kt     # + Event.PermissionDenied
│   ├── network/PermissionDeniedInterceptor.kt   # NEW
│   ├── di/HttpClientModule.kt          # add interceptor to @Client
│   ├── presentation/error/AppErrorMessages.kt   # 403 generic text
│   ├── presentation/viewmodel/CoreViewModel.kt  # canOpenPanel; refresh on PermissionDenied
│   └── presentation/screens/CoreScreen.kt       # drawer "Painel de Gestão"
├── features/profile/
│   ├── data/dto/MeProfileDto.kt        # roles, permissions; − is_admin
│   ├── data/access/ProfileAccessRepository.kt  AccessMapper.kt   # NEW
│   ├── data/snapshot/ProfileSnapshotRepository.kt
│   ├── domain/model/MeProfile.kt       # − isAdmin
│   ├── presentation/state/ProfileUiState.kt, viewmodel/ProfileViewModel.kt   # − isAdmin
│   └── di/ProfileModule.kt             # bind AccessRepository
├── features/admin/
│   ├── panel/domain/PanelCard.kt  CardRequirement.kt  VisiblePanelCardsUseCase.kt   # NEW
│   ├── panel/presentation/viewmodel/AdminPanelViewModel.kt  state/AdminPanelUiState.kt   # NEW
│   ├── panel/presentation/screens/AdminScreen.kt   # title, filtered cards, empty state
│   ├── members/presentation/util/MembersFailure.kt # FailureKind
│   ├── members/presentation/{state,viewmodel,screens}/  # canAdd, canEdit, canDelete…
│   └── reports/
│       ├── hymnal/presentation/{state,viewmodel,screens}/  # canOpenSettings, canManage, LeaveArea
│       └── presentation/navigation/ReportsNavGraph.kt       # handle LeaveArea
└── features/worshiphub/
    ├── chordcharts/presentation/{state,viewmodel,screens}/   # isAdmin → canEdit
    ├── lyrics/presentation/{state,viewmodel,screens}/        # isAdmin → canEdit
    └── shared/presentation/components/SongContentListScreen.kt  # isAdmin → canEdit

app/src/test/java/com/ipb/castelobranco/
├── core/domain/access/AccessTest.kt                     # NEW — allows/holds, NONE
├── core/network/PermissionDeniedInterceptorTest.kt      # NEW — structured/unstructured/other codes
├── core/presentation/error/AppErrorMessagesTest.kt      # + 403 text
├── core/presentation/viewmodel/CoreViewModelTest.kt     # + refresh on PermissionDenied
├── features/profile/data/dto/MeProfileDtoBackwardCompatibilityTest.kt   # old cache → no role
├── features/profile/data/access/AccessMapperTest.kt     # NEW — unknown values, combos
├── features/profile/data/access/ProfileAccessRepositoryTest.kt   # NEW — single-flight
├── features/admin/panel/domain/VisiblePanelCardsUseCaseTest.kt   # NEW — role × card matrix
└── features/admin/members/presentation/util/MembersFailureTest.kt  # NEW — READ/WRITE × 403/404
```

**Structure Decision**: single `:app` module, feature-based layout as in `CLAUDE.md`. The only new package outside a
feature is `core/domain/access/`.

## Implementation Order

1. **Core access** — enums, `Access`, repository interface, use cases, `AccessTest`.
2. **Profile** — DTO, mapper, `ProfileAccessRepository` (single-flight), Hilt binding; remove `isAdmin` from
   `MeProfile` / `ProfileUiState`; DTO compatibility test updated.
3. **403 handling** — `AppErrorMessages` text, `AuthEventBus.PermissionDenied`, interceptor on `@Client`,
   `CoreViewModel` refresh.
4. **Drawer + panel** — `canOpenPanel`, "Painel de Gestão", `PanelCard` + use case + `AdminPanelViewModel`, empty
   state. (Backend 012 compatible from here: the release blocker is gone.)
5. **Members** — `FailureKind`, flags on list/profile.
6. **Reports** — flags on report menu and windows; `LeaveArea` on read refusals.
7. **Worship hub** — `canEdit` from `ObserveAccessUseCase`; drop `ProfileSnapshotRepository` import.
8. **Specs** — see below; `./gradlew :app:testDebugUnitTest`; quickstart walkthrough.

Steps 1–4 are the MVP (User Stories 1–2). Steps 5–7 are independent of each other.

## Spec updates (same commits as the code)

| File | Change |
|------|--------|
| `specs/core/spec.md` | profile contract table (`roles`, `permissions`, − `is_admin`); drawer row "Painel de Gestão — logado + algum papel"; boot text `isAdmin` → access; new "Acesso" section (core access model, 403 refresh) |
| `specs/admin/spec.md` | §1 route label; §2 title, panel now has a ViewModel, cards filtered by requirement + empty state; §2.1 add "Requisito" column; §5 rules rewritten on roles/levels; §6 report read-only for non-owner, settings hidden; §7.4 403 rule by read/write; known limitation of Gerar Escala |
| `specs/worshiphub/spec.md` | `isAdmin` → `canEdit` (`songs` ≥ `manage`); `IsAdminUser` references → scope `songs` / `manage` |
| `specs/004-protected-media-downloads/contracts/media-download.md` (+ spec.md line on `members/`) | `members/` requires `view` on `members` |
| `specs/005-admin-members-management/*` | "leader (`is_admin`)" → role/scope wording (spec FR-002 and assumptions, quickstart, research) |
| `specs/constitution.md` | 403 generic text differs from 401; interceptor as the one reader of `error_code` outside `ResponseExt` |

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| `PermissionDeniedInterceptor` reads error bodies outside `ResponseExt` | Profile refresh must follow **every** scoped refusal, including the member photo `ImageLoader`, which never produces an `AppError` | Refreshing from each ViewModel misses non-`Result` paths and repeats code in ~10 places; triggering from `ResponseExt` turns a pure mapper into a side-effecting one |
