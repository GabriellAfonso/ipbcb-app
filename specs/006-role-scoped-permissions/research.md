# Research: Role-Scoped Permissions in the App

Decisions taken while planning. Each one records what was chosen, why, and what was rejected.

## R1 — Where access lives

**Decision**: a new package `core/domain/access/` holds the vocabulary (`Scope`, `AccessLevel`, `Role`, `Access`)
and the interface `AccessRepository`. The implementation, `ProfileAccessRepository`, lives in
`features/profile/data/access/` — the only place that knows `MeProfileDto` — and is bound through Hilt
(`ProfileModule`). Panel, admin areas and worship hub inject the core use cases only.

**Rationale**: the spec requires access to be shared through `core/` so features never import each other.
Worship hub today imports `features.profile.data.snapshot.ProfileSnapshotRepository` directly to read `isAdmin`
(four ViewModels) — an existing violation of that rule, removed by this feature.

**Alternatives considered**: moving the whole profile snapshot to core — rejected, much bigger change for no gain;
reading access from `ProfileViewModel` — rejected, a ViewModel cannot be shared by admin and worship hub.

## R2 — Parsing the new profile, and the old cache

**Decision**: `MeProfileDto` drops `isAdmin` and gains
`roles: List<RoleDto> = emptyList()` and `permissions: Map<String, String?> = emptyMap()`, both with defaults.
Identifiers stay raw strings in the DTO; the mapper turns them into enums and **drops** anything it does not know.
The cache key (`me_profile`) stays.

**Rationale**:
- An old-shape file on disk decodes cleanly: `is_admin` is dropped by `ignoreUnknownKeys = true`, the two new fields
  take their empty defaults, so the profile reads as "no role" (FR-004) and keeps name and photo at boot.
- A raw `String` value never fails deserialization, so an unknown level or scope cannot throw. A throw would make
  `LocalSnapshotCache` delete the file and the network fetch fail entirely — the whole profile lost for one unknown
  word. Unknown values become "no access" in the mapper (FR-003).
- The ETag saved with the old file does not match the new body, so the first refresh returns 200 with the new shape.

**Alternatives considered**: a new cache key (`me_profile_v2`) — rejected, the user would lose name and photo at boot
until the first fetch, for nothing; enums in the DTO with a custom serializer — rejected, more code for the same
result as mapping strings.

## R3 — Comparing levels

**Decision**: `AccessLevel` is an enum declared in order `VIEW, MANAGE, OWNER`. `Access.allows(scope, minimum)` is
true when the scope's level is not null and `level >= minimum`. A missing scope is null. The Admin-only placeholder
cards use `Access.holds(Role.ADMIN)`. The panel entry uses `Access.hasAnyRole`, which counts **every** role in the
profile, known or not (spec edge case "unknown role only").

**Rationale**: ordinal comparison is the hierarchy itself; one function answers every question in the matrix and is
trivially unit-testable. The level already arrives combined across roles (backend 012 FR-010), so the app never
merges roles.

## R4 — How a 403 is shown

**Decision**: keep 403 as `AppError.Auth(code = 403)`. Change only `AppError.toUserMessage()`: when `userMessage` is
null and `code == 403`, the text is **"Você não tem permissão para esta ação."**; 401 keeps "Faça login para
continuar.". With the structured body, `ResponseExt` already copies `detail` into `userMessage` (FR-022), so nothing
changes there.

**Rationale**: every existing `is AppError.Auth` check keeps its behaviour — notably `BaseSnapshotRepository`, which
clears a cache on 401/403 so member-only content does not linger for a non-member, and the schedule and gallery
screens that already branch on `code`. The only visible defect was the generic text.

**Alternatives considered**: a new `AppError.Forbidden` — rejected, every `is AppError.Auth` site would need review
and the snapshot cache-clearing rule would silently stop covering 403.

`specs/constitution.md` changes accordingly: 401 and 403 still become `AppError.Auth(code)`, but the generic text
differs by code.

## R5 — Refreshing the profile after a refusal

**Decision**:
1. `PermissionDeniedInterceptor` (`core/network/`), an application interceptor on the authenticated `@Client`, looks
   at every response; on `403` it peeks the body (bounded, `peekBody`), parses it with the existing
   `parseApiError`, and when `error_code == "PERMISSION_DENIED"` emits `AuthEventBus.Event.PermissionDenied`.
   The response passes through untouched.
2. `CoreViewModel`, which already collects `AuthEventBus` for `LoginSuccess`, calls `RefreshAccessUseCase` on
   `PermissionDenied`.
3. `ProfileAccessRepository.refresh()` is single-flight: a `Mutex.tryLock()`; a call arriving while one is running
   returns at once (FR-024). The bus already coalesces (`extraBufferCapacity = 1`, `tryEmit`).

**Rationale**: one place covers every management call, including ones that never reach a ViewModel as a `Result`
(the member photo `ImageLoader`). No ViewModel has to remember to refresh. `CoreViewModel` is Activity-scoped and
alive for the whole session.

**Alternatives considered**: each ViewModel calling refresh on 403 — rejected, repetitive and easy to miss;
triggering from `ResponseExt.toAppError()` — rejected, that function must stay a pure mapper.

**Loop safety**: `GET api/me/profile/` never answers `PERMISSION_DENIED` (it is not a scoped endpoint), so the
refresh cannot trigger itself.

## R6 — Read refusal vs write refusal

**Decision**: the caller states the kind. In members, `toMembersEvent()` becomes `toMembersEvent(kind)` with
`FailureKind.READ | WRITE`:

| Kind  | 403                         | 404                     | other          |
|-------|-----------------------------|-------------------------|----------------|
| READ  | `LeaveArea` (back to panel) | `MemberGone`            | `ShowMessage`  |
| WRITE | `ShowMessage` (stay)        | `MemberGone`            | `ShowMessage`  |

- READ: list load, profile load, history load, form load of record and options.
- WRITE: save, validity toggle, photo upload, photo removal, delete.

In reports, a 403 on a load (report, windows list) emits a new `LeaveArea` event that pops `ReportsRoutes.GRAPH`;
writes keep `ShowMessage`. The settings read is public, so its screen has no read refusal to handle.

Gestão do Louvor and Gerar Escala keep their flow: their reads are not scoped (song list is public; the member list
needs `is_member` — the known limitation), and their writes already only show a message. They benefit from R4's
text.

**Rationale**: only the caller knows whether it was reading or writing; classifying by HTTP method in the network
layer would require carrying the method into `AppError` for no other use.

## R7 — Panel state

**Decision**: the panel gets a small `AdminPanelViewModel` (screen-scoped `hiltViewModel()`) that combines
`ObserveAccessUseCase` with the card catalogue and exposes `StateFlow<AdminPanelUiState>(cards, isEmpty)`. Each card
declares its requirement in `features/admin/panel/domain/PanelCard.kt` (an enum in display order with a
`CardRequirement`: `AtLeast(scope, level)` or `HasRole(role)`), and a pure `VisiblePanelCardsUseCase` filters. The
composable keeps building `AdminAction` from visible cards (colours, icons and navigation stay presentation), and
shows an empty-state text when the list is empty.

**Rationale**: the rule "comparison stays in the domain; composables get ready flags" (spec FR-010). The panel is
the first screen with a reason to change after load (profile refresh after a 403), so it now needs a state holder.

**Alternatives considered**: passing `Access` into `adminGraph` from `AppNavHost` — rejected, `AppNavHost` would
need to know access just to hand it over, and the panel would still compare levels in composition.

## R8 — Drawer entry

**Decision**: `CoreViewModel` exposes `canOpenPanel: StateFlow<Boolean>` from `ObserveAccessUseCase`
(`hasAnyRole`). `UserAuthState.isAdmin` becomes `canOpenPanel`; the drawer label becomes "Painel de Gestão".
`MeProfile.isAdmin` and `ProfileUiState.isAdmin` are removed.

## R9 — Flags on each screen

| Screen                 | New UI state flags                                           | Source                                   |
|------------------------|--------------------------------------------------------------|------------------------------------------|
| Members list           | `canAdd`                                                     | `members` ≥ `MANAGE`                     |
| Member profile         | `canEdit`, `canChangePhoto`, `canDelete`, `canRemovePhoto`    | `MANAGE`, `MANAGE`, `OWNER`, `OWNER`      |
| Hymnal report          | `canOpenSettings`                                            | `reports.hymnal_history` = `OWNER`        |
| Service windows        | `canManage` (create, edit, delete, activate toggle)          | `reports.hymnal_history` = `OWNER`        |
| Chord charts / lyrics (list and detail) | `canEdit` (replaces `isAdmin`)              | `songs` ≥ `MANAGE`                       |

Each ViewModel combines its data with `ObserveAccessUseCase`, so a profile refresh updates the buttons live
(SC-006). The "Janelas de culto" entry stays for everyone who reaches the report.

## R10 — Known limitation (not solved)

"Gerar Escala" loads `GET api/members/` (`is_member`). A Liderança who is not a member gets 403 there and sees the
permission text (R4). Recorded in `specs/admin/spec.md`; needs a backend change.
