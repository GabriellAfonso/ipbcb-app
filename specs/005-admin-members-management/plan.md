# Implementation Plan: Members Management for Church Leaders

**Branch**: `005-admin-members-management` | **Date**: 2026-09-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-admin-members-management/spec.md`

## Summary

Leaders get a **Membros** area inside the admin panel, backed by the backend's leader-only members API (backend
`010-members-management`). A new sub-feature `features/admin/members/` follows the app's layering (data → domain
→ presentation, Hilt, `@AuthedRetrofit`) and adds a nested `membersGraph` with four screens: grid list (design
"Lista B", no filter tiles), profile (design "Perfil A"), create/edit form, and history.

Key technical choices (details in [research.md](research.md)):

- **Online only, memory only**: a `@Singleton` repository keeps the list, opened records and their ETags in
  memory; writes patch the list in place so every screen stays in sync (R5). Nothing is persisted.
- **Explicit nulls in PATCH** via `buildJsonObject` — the shared `Json` drops nulls (R2).
- **Member photos** through a dedicated Coil `ImageLoader` on the authenticated client with **no disk cache**
  (R3); failures fall back to initials.
- **Photo pick/crop** extracted from `ProfileScreen` into a shared core component, reused by both screens; the
  cropped temp file is deleted right after reading (R4).
- **Sign-out cleanup** through a new multibound core contract `SessionScopedCache`, called by `CoreViewModel` on
  logout and when the session drops (R6).

## Technical Context

**Language/Version**: Kotlin (JVM 17 target), Android, `minSdk 24` with core library desugaring (`java.time`)

**Primary Dependencies**: Jetpack Compose (Material 3), Hilt, Retrofit + OkHttp (`@AuthedRetrofit`, `@Client`),
kotlinx.serialization (`JsonObject` bodies), Coil 2.6 (new authenticated in-memory `ImageLoader`), UCrop — all
already in the project; **no new libraries**

**Storage**: none — in-memory only (repository state, ETags, Coil memory cache). One transient crop file in
`cacheDir`, deleted immediately (R4)

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; `FakeMembersAdminApi` (fakes preferred)

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: list of 1,000 members loads in one request (< 3 s on mobile); on-device search with no
noticeable delay; unchanged list/record answered with 304

**Constraints**: LGPD art. 11 — no member data or photo on disk, no member data in logs; status names never
hardcoded; features never import each other; Portuguese UI strings hardcoded; 120-char lines

**Scale/Scope**: hundreds of members; ~35 new production files, 4 touched (`AdminScreen`, `AdminNavGraph`,
`ProfileScreen`, `CoreViewModel`), ~10 test classes; 2 domain specs updated

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is still the unfilled template; the project's rules are `specs/constitution.md`
and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing data → presentation | ✅ Repository returns `Result` + `mapError()`; ViewModels read `AppError` only |
| `message` technical, `userMessage` for screen | ✅ Server `detail` via `toAppError()`; "Este membro não existe mais" set as `userMessage` in the repository |
| Presentation decides text via `toUserMessage()`; no per-screen generic text | ✅ ViewModels call `toUserMessage()`; form field texts come from the validation use case (domain rule copy, like `ValidateServiceWindowUseCase`) |
| One single point of HTTP error parsing | ✅ Only `Response.toAppError()`; `field_errors` from `AppError.Server.fieldErrors` |
| 401/403 → `Auth`, others → `Server` | ✅ Unchanged; 403 handled as "leave area" |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| Single Activity, UI 100% Compose | ✅ UCrop is the existing exception (Manifest) |
| UI → ViewModel → UseCase → Repository | ✅ |
| Features never import each other | ✅ Picker moves to `core/presentation/components`; session cleanup via `core/domain/session` |
| `domain/` has no Android | ✅ `java.time` + `DateProvider`; no `Uri`/`Context` in domain |
| Loading / success / error on every screen | ✅ All four UI states carry `isLoading`/`error` |
| `StateFlow` UI state, `SharedFlow` one-shot events | ✅ `MembersEvent` sealed |
| Screen / content split; dumb composables | ✅ `XScreen` collects, `XContent` pure |
| No `Context` in ViewModel | ✅ `ImageLoader` exposed, not `Context` |
| Multi-screen graph → `XNavGraph.kt`, local `XRoutes`, `safePopBackStack()` | ✅ `MembersNavGraph.kt`, `MembersRoutes` |
| Graph-scoped VM where shared | ✅ Not needed — shared state lives in the singleton repository; each screen uses `hiltViewModel()` like the admin screens |
| `@AuthedRetrofit` for protected APIs | ✅ API and image loader both on the authenticated stack |
| No PII in logs | ✅ ids only (R12) |
| Tests: happy path + 1 error per use case, fakes | ✅ R13 |
| Spec first, spec + code in the same commit | ✅ `specs/admin/spec.md` and `specs/core/spec.md` updated per slice |

**Gate result**: PASS. No violations.

**Post-design re-check**: PASS. The two core additions (`SessionScopedCache`, `SquarePhotoPicker`) each replace
something that would otherwise be duplicated or force a cross-feature import.

## Project Structure

### Documentation (this feature)

```text
specs/005-admin-members-management/
├── spec.md
├── plan.md                    # this file
├── research.md                # R1–R13
├── data-model.md              # domain types, repository, use cases, UI state
├── quickstart.md              # tests, audits, device checks
├── contracts/
│   └── admin-members-api.md   # consumed backend contract
├── checklists/
│   └── requirements.md
└── tasks.md                   # /speckit-tasks
```

### Source code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/
│   ├── domain/session/SessionScopedCache.kt              # NEW — fun interface clear()
│   ├── presentation/components/SquarePhotoPicker.kt      # NEW — pick → UCrop → bytes, deletes temp file
│   └── presentation/viewmodel/CoreViewModel.kt           # clears Set<SessionScopedCache> on logout / session drop
├── features/profile/presentation/screens/ProfileScreen.kt  # uses SquarePhotoPicker (behavior unchanged)
└── features/admin/
    ├── panel/presentation/
    │   ├── navigation/AdminNavGraph.kt                   # AdminNav.members + membersGraph(navController)
    │   └── screens/AdminScreen.kt                        # "Membros" enabled → nav.members
    └── members/
        ├── data/
        │   ├── api/MembersAdminApi.kt, MembersAdminEndpoints.kt
        │   ├── dto/MembersAdminDtos.kt
        │   ├── mapper/MemberMapper.kt                    # DTO → domain, MemberChanges → JsonObject
        │   └── repository/MembersAdminRepositoryImpl.kt  # in-memory state + ETags
        ├── di/
        │   ├── MembersAdminModule.kt                     # API, repository, SessionScopedCache @IntoSet
        │   └── MemberPhotoLoaderModule.kt                # @MemberPhotoLoader ImageLoader (no disk cache)
        ├── domain/
        │   ├── model/                                    # NamedRef, Gender, MemberSummary, MemberRecord,
        │   │                                             # MemberOptions, MemberDraft, MemberField,
        │   │                                             # MemberChanges, HistoryEntry, HistoryLine
        │   ├── repository/MembersAdminRepository.kt
        │   └── usecase/                                  # see data-model.md
        └── presentation/
            ├── navigation/MembersNavGraph.kt             # MembersRoutes: LIST, PROFILE/{id}, FORM?id=, HISTORY/{id}
            ├── state/                                    # UI states + MembersEvent
            ├── viewmodel/                                # MembersList-, MemberProfile-, MemberForm-, MemberHistoryViewModel
            ├── components/                               # MemberCard, MemberAvatar, SectionCard, InfoRow,
            │                                             # DeleteMemberDialog, GenderSelector, OptionPicker,
            │                                             # MinistriesPicker
            └── screens/                                  # MembersListScreen, MemberProfileScreen,
                                                          # MemberFormScreen, MemberHistoryScreen
                                                          # (MembersDesignPreviews.kt removed; previews move
                                                          #  onto the content composables)

app/src/test/java/com/ipb/castelobranco/
├── features/admin/members/
│   ├── data/api/FakeMembersAdminApi.kt
│   ├── data/mapper/MemberMapperTest.kt
│   ├── data/repository/MembersAdminRepositoryImplTest.kt
│   ├── domain/usecase/ValidateMemberDraftUseCaseTest.kt
│   ├── domain/usecase/BuildHistorySentenceUseCaseTest.kt
│   ├── domain/usecase/ComputeMemberAgeUseCaseTest.kt
│   ├── domain/usecase/ValidateMemberPhotoUseCaseTest.kt
│   ├── domain/usecase/SaveMemberUseCaseTest.kt
│   └── presentation/viewmodel/                           # list, profile, form, history VM tests
└── core/presentation/viewmodel/CoreViewModelSessionCacheTest.kt

specs/admin/spec.md     # §1 routes, §2.1 "Membros" state, new §7 Membros
specs/core/spec.md      # SessionScopedCache (§4), SquarePhotoPicker (§6.7), CoreViewModel logout (§6.2)
```

**Structure Decision**: Single `:app` module, feature-based. The area sits under `features/admin/members/` next to
`register`, `schedule` and `reports`, with its own nested graph like `reportsGraph`.

## Design Notes

### Navigation (`MembersNavGraph.kt`)

```text
MembersRoutes.GRAPH = "graph/admin/members"
  LIST                 → MembersListScreen          (start)
  PROFILE/{memberId}   → MemberProfileScreen
  FORM?memberId={id}   → MemberFormScreen            (no id = create)
  HISTORY/{memberId}   → MemberHistoryScreen
```

- Create saved → `navigate(PROFILE/{newId}) { popUpTo(FORM) { inclusive = true } }`.
- Edit saved → `safePopBackStack()` (profile observes the repository record).
- Deleted / `MemberGone` → `popBackStack(LIST, inclusive = false)`.
- `LeaveArea` (403) → `popBackStack(AdminRoutes.ADMIN, inclusive = false)` + snackbar message.

### Screens reuse `BaseScreen`

List, form and history use `BaseScreen(tabName, showBackArrow = true)`. The profile keeps the hero layout from
design A: `BaseScreen` for the top bar, with the green band drawn as the first item of the scrolling content.

### Validity switch

`MemberProfileViewModel.onValidityChanged(new)` updates the UI optimistically (`isSavingValidity = true`), calls
`SetMemberValidityUseCase`; on failure restores the previous value and emits `ShowMessage`.

### Form "unsaved changes"

`hasUnsavedChanges = draft.changesFrom(original).isNotEmpty()`; the screen's `BackHandler` shows the discard
dialog when true.

## Implementation Order

Each slice ships its spec update with its code (`CLAUDE.md` §7).

1. **Specs** — `specs/admin/spec.md` §7 "Membros" and §1/§2.1; `specs/core/spec.md` for the two core additions.
2. **Core** — `SessionScopedCache` + `CoreViewModel` wiring (+ test); `SquarePhotoPicker` extracted and
   `ProfileScreen` switched to it (manual check: profile photo unchanged).
3. **Data** — DTOs, API, mapper (+ `MemberMapperTest`), `FakeMembersAdminApi`, repository (+ test), DI modules.
4. **US1 list + profile (P1)** — use cases (age), list VM + screen, profile VM + screen (read-only parts),
   `AdminScreen`/`AdminNavGraph` wiring. Ships alone: leaders can browse.
5. **US2 form + validity (P1)** — validation, `SaveMemberUseCase`, form VM + screen, validity switch.
6. **US3 photo (P2)** — photo guard, upload/remove, `@MemberPhotoLoader`, avatar component with initials fallback.
7. **US4 history (P2)** — sentence builder, history VM + screen, last-change card on profile.
8. **US5 delete (P3)** — confirmation rule, dialog, navigation back to list.
9. **Cleanup + audits** — remove `MembersDesignPreviews.kt`, previews on content composables, quickstart §1–2,
   full test run, device checklist §3.

Suggested commits (spec + code together per slice):
`feat(core): add session-scoped caches cleared on sign-out`,
`refactor(profile): extract square photo picker to core`,
`feat(members): browse the roll and open member profiles`,
`feat(members): create and edit members`,
`feat(members): manage the leader-only member photo`,
`feat(members): show the member edit history`,
`feat(members): delete a member with name confirmation`.

## Risks

| Risk | Mitigation |
|------|------------|
| Coil request without disk cache re-downloads photos after process death | Accepted: online-only by design; memory cache covers the session |
| A 401 image request triggering a refresh storm when many cards load at once | Same `TokenAuthenticator` as every API call; it already serializes refreshes |
| UCrop temp file left behind if the app dies between crop and read | Picker also sweeps `cacheDir/cropped_*` on each launch of the picker; file holds a 512 px JPEG only briefly |
| Status list on the server changes (new status) | Nothing hardcoded; chips and pickers show whatever the server sends |
| Singleton repository outliving a leader who lost rights | 403 on the next call leaves the area; sign-out clears it (R6) |
| `explicitNulls = false` regression if someone switches the PATCH to a DTO | `MemberMapperTest` asserts `JsonNull` is present for cleared fields |

## Complexity Tracking

No violations.
