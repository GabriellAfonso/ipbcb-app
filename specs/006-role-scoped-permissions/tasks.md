# Tasks: Role-Scoped Permissions in the App

**Input**: Design documents from `specs/006-role-scoped-permissions/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/profile-api.md,
contracts/permission-denied.md, quickstart.md

**Tests**: Required by `CLAUDE.md` (happy path + 1 error per use case, fakes preferred) and listed in plan.md. Each
test task comes right before the code it covers and must fail first.

**Organization**: Grouped by user story (spec.md US1–US6). Each story's commit carries its domain-spec update
(`CLAUDE.md` §7).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US6 from spec.md

## Path Conventions

- Production: `app/src/main/java/com/ipb/castelobranco/` → abbreviated `main/`
- Tests: `app/src/test/java/com/ipb/castelobranco/` → abbreviated `test/`
- Test commands: `./gradlew :app:testDebugUnitTest --tests "<pattern>"` — never `clean`, `--rerun-tasks`,
  `--no-daemon`

## Transitional note

`MeProfile.isAdmin` has readers in the core screen and four worship hub ViewModels. To keep the build green between
stories, T008 makes `is_admin` **optional** in the DTO (default `false`, so the 012 backend decodes) instead of
deleting it; each story moves its readers to `Access`; T049 deletes `isAdmin` everywhere once nothing reads it.

---

## Phase 1: Setup (Specs first)

**Purpose**: domain specs describe the destination before any code (`CLAUDE.md` §7). Each task edits only the
section named; later story phases refine them if the code diverges.

- [X] T001 [P] Update `specs/core/spec.md`: "Contrato de `GET /api/me/profile/`" table — remove `is_admin`, add
  `roles` (`List<RoleDto>`, default empty) and `permissions` (`Map<String, String?>`, default empty) with the
  parsing rules of `contracts/profile-api.md`; replace the paragraph on the removed `active` field's "checagens usam
  `is_member` / `is_admin`" with roles/scopes; §6.2 boot text "`isAdmin`/`isMember` ja valem desde o boot" → access
  and membership; §6.3 drawer row "Painel Admin | logado + admin" → "Painel de Gestão | logado + algum papel"; new
  section "Acesso" describing `core/domain/access` (`Scope`, `AccessLevel`, `Role`, `Access`, `AccessRepository`,
  deny on unknown, `Access.NONE`) and the 403 refresh (`PermissionDeniedInterceptor` →
  `AuthEventBus.Event.PermissionDenied` → `CoreViewModel` → single-flight refresh)
- [X] T002 [P] Update `specs/admin/spec.md`: §1 route table label "Painel Admin" → "Painel de Gestão"; §2 title
  "Painel de Gestão", the panel now has `AdminPanelViewModel` (visible cards + empty state); §2.1 add a "Requisito"
  column per `data-model.md` §4; §5 rewritten: entry shown when the profile lists any role, cards filtered by level,
  UI filter only (backend authorizes); §6 hymnal report — "Parâmetros de coleta" only for `owner` on
  `reports.hymnal_history`, service windows read-only below `owner`, read refusal leaves the reports area; §7.4 403
  rule split: read → message + leave area, write → message + stay; members actions by level (list/profile/history
  `view`, add/edit/change photo `manage`, delete/remove photo `owner`); add "Limitação conhecida": Gerar Escala uses
  `GET api/members/` (`is_member`), a non-member Liderança gets 403
- [X] T003 [P] Update `specs/worshiphub/spec.md`: line ~99 `isAdmin` → `canEdit`; create/edit buttons of chord
  charts and lyrics require `songs` ≥ `manage`; replace `IsAdminUser` at lines ~208, ~257, ~377 with "scope `songs`,
  level `manage`"; line ~386 "403 se nao admin" → "403 sem `manage` em `songs`"
- [X] T004 [P] Update `specs/004-protected-media-downloads/contracts/media-download.md` line 25 (`members/` →
  "`view` em `members` (Admin, Liderança)") and `specs/004-protected-media-downloads/spec.md` lines 13 and 25 to the
  same wording
- [X] T005 [P] Update `specs/005-admin-members-management/`: `spec.md` FR-002 and the assumption "Leader = the
  profile's administrator flag" → `members` ≥ `view`; `quickstart.md` line 9 "leader (`is_admin=true`)" → "Admin or
  Liderança role", line 46 "Painel Admin" → "Painel de Gestão"; `research.md` line 20 `authState.isAdmin` → any role
- [X] T006 [P] Update `specs/constitution.md` "Tratamento de erro": keep "401 e 403 viram `AppError.Auth(code)`";
  add that the generic text differs by code (401 "Faça login para continuar.", 403 "Você não tem permissão para
  esta ação."); under "Um único ponto de parsing de erro HTTP" add the one exception — `PermissionDeniedInterceptor`
  reads `error_code` via `parseApiError` only to fire an event, never builds an `AppError`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: the access model and its source. Every story depends on it.

**⚠️ CRITICAL**: no story work until this phase is complete.

- [X] T007 [P] Write `test/core/domain/access/AccessTest.kt`: `AccessLevel.fromWire` maps `"view"`/`"manage"`/
  `"owner"`, returns null for `null`, `""`, `"OWNER"`, `"admin"`; `Scope.fromWire` maps all 7 keys incl.
  `"reports.hymnal_history"`, null for unknown; `Role.fromWire` maps `admin`/`leader`/`media`, null otherwise;
  `allows` — `OWNER` allows `VIEW`/`MANAGE`/`OWNER`, `MANAGE` does not allow `OWNER`, missing scope allows nothing;
  `holds(Role.ADMIN)`; `Access.NONE` allows nothing and `hasAnyRole == false`
- [X] T008 Create `main/core/domain/access/AccessLevel.kt` (enum `VIEW, MANAGE, OWNER` in that order,
  `companion fun fromWire(value: String?): AccessLevel?`), `Scope.kt` (enum with `wireKey`, `fromWire`),
  `Role.kt` (enum with `wireId`, `fromWire`), `Access.kt` (`data class Access(roles: Set<Role>, hasAnyRole: Boolean,
  levels: Map<Scope, AccessLevel>)` with `allows(scope, minimum)`, `holds(role)`, `companion val NONE`) per
  `data-model.md` §2 — no Android imports. Wire strings as constants, not inline literals
- [X] T009 Create `main/core/domain/access/AccessRepository.kt` (`val access: Flow<Access>`,
  `suspend fun refresh()`), `ObserveAccessUseCase.kt` (`operator fun invoke(): Flow<Access>`; ViewModels turn it into state with `stateIn(viewModelScope, …, Access.NONE)`) and
  `RefreshAccessUseCase.kt` (`suspend operator fun invoke()`), both `@Inject constructor(repository)`
- [X] T010 Update `test/features/profile/data/dto/MeProfileDtoBackwardCompatibilityTest.kt`: (a) the old cache JSON
  `{"name","is_member","is_admin":true,"photo_url"}` decodes with `roles` empty and `permissions` empty; (b) the
  012 JSON from `contracts/profile-api.md` decodes with 2 roles and 7 permissions, `null` values kept; (c) a
  `permissions` value `"superowner"` and an unknown key decode without throwing. Use the `Json` from
  `SerializationModule`
- [X] T011 Update `main/features/profile/data/dto/MeProfileDto.kt`: add `@Serializable data class RoleDto(id: String,
  name: String)`; add `@SerialName("roles") val roles: List<RoleDto> = emptyList()` and
  `@SerialName("permissions") val permissions: Map<String, String?> = emptyMap()`; make
  `@SerialName("is_admin") val isAdmin: Boolean = false` (transitional, removed in T049)
- [X] T012 [P] Write `test/features/profile/data/access/AccessMapperTest.kt`: Admin profile → all 7 scopes `OWNER`,
  `holds(ADMIN)`; Liderança matrix → `members` `MANAGE`, `reports.hymnal_history` `VIEW`, no `OWNER`; Mídia matrix
  → `members` absent; empty roles + all null → `Access.NONE` equivalent; unknown role id only → `hasAnyRole` true,
  `roles` empty; unknown level / unknown key / missing key → absent
- [X] T013 Create `main/features/profile/data/access/AccessMapper.kt`: `fun MeProfileDto.toAccess(): Access` per
  `data-model.md` §3
- [X] T014 [P] Write `test/features/profile/data/access/ProfileAccessRepositoryTest.kt` with a fake
  `SnapshotFetcher<MeProfileDto>` / `SnapshotCache` driving a real `ProfileSnapshotRepository`: `access` is
  `Access.NONE` while `Loading` and on `Error`, mapped on `Data`, back to `NONE` after `clearCache()`; two concurrent
  `refresh()` calls hit the fetcher once (fetcher suspends on a `CompletableDeferred`)
- [X] T015 Add `val access: Access = Access.NONE` to `main/features/profile/domain/model/MeProfile.kt` and fill it
  in the mapper of `main/features/profile/data/snapshot/ProfileSnapshotRepository.kt` with `dto.toAccess()`. Create
  `main/features/profile/data/access/ProfileAccessRepository.kt` (`@Singleton`, implements `AccessRepository`):
  `access` = `profileSnapshot.observe().map { (it as? SnapshotState.Data)?.value?.access ?: Access.NONE }
  .distinctUntilChanged()`; `refresh()` = `if (!mutex.tryLock()) return`, then `profileSnapshot.refresh()` in
  `try/finally { mutex.unlock() }`
- [X] T016 Bind in `main/features/profile/di/ProfileModule.kt`: `@Binds @Singleton abstract fun
  bindAccessRepository(impl: ProfileAccessRepository): AccessRepository`; update the preload comment ("isAdmin
  gates…" → "access gates the management entry…")
- [X] T017 Run `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.domain.access.*"` and
  `--tests "com.ipb.castelobranco.features.profile.*"`; all green

**Checkpoint**: access is available app-wide; the app already decodes the 012 profile.

---

## Phase 3: User Story 1 — Managers keep reaching the panel (Priority: P1) 🎯 MVP

**Goal**: "Painel de Gestão" appears for anyone with a role; titled accordingly; old cache never crashes.

**Independent Test**: against backend 012, Admin and Liderança see the menu entry and panel; a member without role
does not; upgrading over an old cache boots offline without the entry (quickstart #1–#3, #6).

- [X] T018 [US1] Extend `test/core/presentation/viewmodel/CoreViewModelTest.kt`: with a `FakeAccessRepository`
  (in `test/core/testing/FakeAccessRepository.kt`, `MutableStateFlow<Access>` + refresh counter), `canOpenPanel` is
  false for `Access.NONE`, true for `hasAnyRole = true` with empty levels, and follows later emissions
- [X] T019 [US1] Update `main/core/presentation/viewmodel/CoreViewModel.kt`: inject `ObserveAccessUseCase`; expose
  `val canOpenPanel: StateFlow<Boolean>` = access `hasAnyRole`, built in `viewModelScope`
- [X] T020 [US1] Update `main/core/presentation/screens/CoreScreen.kt`: `UserAuthState.isAdmin` → `canOpenPanel`,
  fed by `viewModel.canOpenPanel` (not `profileUiState`); drawer condition `isLoggedIn && canOpenPanel`; label
  "Painel de Gestão" (extract the label to a `private const`)
- [X] T021 [US1] Update `main/features/admin/panel/presentation/screens/AdminScreen.kt`: `tabName` "Painel Admin" →
  "Painel de Gestão" (constant)
- [X] T022 [US1] Confirm `specs/core/spec.md` and `specs/admin/spec.md` match T018–T021 (drawer, title); fix any
  divergence

**Checkpoint**: release blocker gone — managers reach the panel on backend 012.

---

## Phase 4: User Story 2 — Each role sees only its cards (Priority: P1)

**Goal**: panel cards filtered by level/role; empty state when none.

**Independent Test**: sign in as each role, compare with `contracts/profile-api.md` "Examples → visible panel cards"
(quickstart #3–#5).

- [X] T023 [P] [US2] Write `test/features/admin/panel/domain/VisiblePanelCardsUseCaseTest.kt`: Admin → 9 cards in
  enum order; Liderança → WORSHIP, SCHEDULE, MEMBERS, NOTICES, REPORTS, GALLERY, EVENTS; Mídia → NOTICES, REPORTS,
  GALLERY, EVENTS; Liderança+Mídia → same as Liderança; `Access.NONE` → empty; unknown role only → empty
- [X] T024 [P] [US2] Create `main/features/admin/panel/domain/CardRequirement.kt` (sealed: `AtLeast(scope, level)`,
  `HasRole(role)`) and `main/features/admin/panel/domain/PanelCard.kt` (enum in display order with `requirement`,
  per `data-model.md` §4)
- [X] T025 [US2] Create `main/features/admin/panel/domain/VisiblePanelCardsUseCase.kt`:
  `operator fun invoke(access: Access): List<PanelCard>` — `PanelCard.entries.filter { satisfies }`
- [X] T026 [US2] Create `main/features/admin/panel/presentation/state/AdminPanelUiState.kt`
  (`cards: List<PanelCard> = emptyList()`, `isEmpty: Boolean = false`) and
  `main/features/admin/panel/presentation/viewmodel/AdminPanelViewModel.kt` (`@HiltViewModel`,
  `ObserveAccessUseCase` + `VisiblePanelCardsUseCase` → `StateFlow<AdminPanelUiState>` via `stateIn(viewModelScope)`)
- [X] T027 [US2] Refactor `main/features/admin/panel/presentation/screens/AdminScreen.kt`: `AdminScreen(nav,
  viewModel = hiltViewModel())` collects state; `AdminPanelContent(state, nav)` maps each visible `PanelCard` to its
  existing `AdminAction` (label, description, icon, colour, `enabled`, onClick — keep today's values, a `when`
  over `PanelCard`); when `state.isEmpty`, show a centred text "Nenhuma funcionalidade disponível para o seu
  perfil." instead of the grid. Update `AdminPanelDesignPreviews.kt` to pass a fake state
- [X] T028 [US2] Run `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.admin.panel.*"`

**Checkpoint**: Mídia no longer sees Membros; placeholders follow the matrix.

---

## Phase 5: User Story 3 — Members area respects the level (Priority: P1)

**Goal**: add/edit/change photo need `manage`; delete/remove photo need `owner`.

**Independent Test**: quickstart #7–#8.

- [X] T029 [P] [US3] Update `main/features/admin/members/presentation/state/MembersListUiState.kt` (`canAdd = false`)
  and `MemberProfileUiState.kt` (`canEdit`, `canChangePhoto`, `canDelete`, `canRemovePhoto`, all `false`)
- [X] T030 [US3] Update `main/features/admin/members/presentation/viewmodel/MembersListViewModel.kt` and
  `MemberProfileViewModel.kt`: inject `ObserveAccessUseCase`; collect in `viewModelScope` and update the flags
  (`allows(MEMBERS, MANAGE)` for add/edit/change photo, `allows(MEMBERS, OWNER)` for delete/remove photo); guard
  `onDeleteRequested`, `onRemovePhotoRequested`, `onPhotoPicked` to no-op when the flag is false
- [X] T031 [US3] Update `main/features/admin/members/presentation/screens/MembersListScreen.kt` (show the
  `ExtendedFloatingActionButton` only when `canAdd`), `MemberProfileScreen.kt` (top-bar edit `IconButton` only when
  `canEdit`; "Excluir membro" only when `canDelete`; validity switch enabled only when `canEdit`; pass
  `canChangePhoto`/`canRemovePhoto` down) and `main/features/admin/members/presentation/components/MemberPhotoViewer.kt`
  (change-photo action only when `canChangePhoto`, "Remover foto" only when `canRemovePhoto`)
- [X] T032 [US3] Add ViewModel flag tests in `test/features/admin/members/presentation/viewmodel/MemberProfileFlagsTest.kt`
  with `FakeAccessRepository` + `FakeMembersAdminApi`: `VIEW` → all false; `MANAGE` → edit/change photo true,
  delete/remove false; `OWNER` → all true; flags update when access changes
- [X] T033 [US3] Confirm `specs/admin/spec.md` §7 matches; run
  `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.admin.members.*"`

**Checkpoint**: a Liderança can never reach member deletion from the UI.

---

## Phase 6: User Story 4 — Denied action explains itself and the app catches up (Priority: P2)

**Goal**: 403 text, profile refresh after `PERMISSION_DENIED`, read ejects / write stays.

**Independent Test**: quickstart #12–#13; contract `contracts/permission-denied.md`.

- [X] T034 [P] [US4] Extend `test/core/presentation/error/AppErrorMessagesTest.kt`: `Auth(403)` without
  `userMessage` → "Você não tem permissão para esta ação."; `Auth(403, userMessage = "X")` → "X"; `Auth(401)` →
  "Faça login para continuar."
- [X] T035 [US4] Update `main/core/presentation/error/AppErrorMessages.kt`: `is AppError.Auth ->` 403 text vs 401
  text, codes as `private const`
- [X] T036 [P] [US4] Write `test/core/network/PermissionDeniedInterceptorTest.kt` (MockWebServer or a fake
  `Interceptor.Chain`, same style as `AuthInterceptorTest`): 403 + `{"error_code":"PERMISSION_DENIED",…}` emits
  `PermissionDenied` once and the response body is still readable downstream; 403 without body, 403 with another
  `error_code`, 401 and 200 emit nothing
- [X] T037 [US4] Add `data object PermissionDenied : Event()` to `main/core/domain/auth/AuthEventBus.kt` (KDoc: "a
  scoped endpoint refused the user; the profile may be stale")
- [X] T038 [US4] Create `main/core/network/PermissionDeniedInterceptor.kt` (`@Singleton`, `@Inject
  constructor(authEventBus)`): on `response.code == 403`, `response.peekBody(MAX_PEEK_BYTES)` →
  `parseApiError(...)?.errorCode == ERROR_CODE_PERMISSION_DENIED` → `authEventBus.emit(PermissionDenied)`; return
  the response unchanged. Register it in `main/core/di/HttpClientModule.kt` on the `@Client` builder only (after
  `authInterceptor`)
- [X] T039 [US4] Extend `test/core/presentation/viewmodel/CoreViewModelTest.kt`: a `PermissionDenied` event calls
  `FakeAccessRepository.refresh()`; `LoginSuccess` behaviour unchanged
- [X] T040 [US4] Update `main/core/presentation/viewmodel/CoreViewModel.kt`: inject `RefreshAccessUseCase`; in the
  existing `authEventBus.events.collect`, on `PermissionDenied` launch `refreshAccess()`
- [X] T041 [P] [US4] Write `test/features/admin/members/presentation/util/MembersFailureTest.kt`: READ × 403 →
  `LeaveArea`; WRITE × 403 → `ShowMessage` with the same text; 404 → `MemberGone` for both; 500 → `ShowMessage`
- [X] T042 [US4] Update `main/features/admin/members/presentation/util/MembersFailure.kt`: add
  `enum class FailureKind { READ, WRITE }`; `fun Throwable.toMembersEvent(kind: FailureKind)`; update KDoc. Update
  callers: `MembersListViewModel` (load → READ), `MemberHistoryViewModel` (READ), `MemberProfileViewModel`
  (`load` → READ; validity, photo upload, photo removal, delete → WRITE — give `fail()` a `kind` parameter),
  `MemberFormViewModel` (`failLoading` → READ; `handleSaveFailure`, `uploadPickedPhoto`, `onPhotoPicked` → WRITE).
  Update the `MembersEvent.LeaveArea` KDoc and the comment in `MembersNavGraph.kt`
- [X] T043 [US4] Reports read refusal: add `data class LeaveArea(val message: String)` to `HymnalReportEvent`,
  `ServiceWindowsEvent` (`main/features/admin/reports/hymnal/presentation/state/`) and `CollectionSettingsEvent`;
  in `HymnalReportViewModel.load`, `ServiceWindowsViewModel` load and `CollectionSettingsViewModel` load, when the
  error is `AppError.Auth` with code 403 emit `LeaveArea(error.toUserMessage())` instead of setting `error`; in
  `main/features/admin/reports/presentation/navigation/ReportsNavGraph.kt` (or the screens' event collectors, where
  they already collect `ShowMessage`) handle `LeaveArea` with a toast +
  `navController.popBackStack(ReportsRoutes.GRAPH, inclusive = true)`. Writes keep `ShowMessage`.
  *Done differently:* the settings read is public (`GET api/hymnal-history/settings/`), so
  `CollectionSettingsEvent` got no `LeaveArea`; report and windows events became buffered `Channel`s so a refusal on
  the first load is not lost before the screen collects
- [X] T044 [US4] Confirm `specs/core/spec.md` "Acesso", `specs/admin/spec.md` §6/§7.4 and `specs/constitution.md`
  match; run `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.core.*"`

**Checkpoint**: no refusal reads as a login problem; losing a role hides the lost actions after one refusal.

---

## Phase 7: User Story 5 — Hymnal report configuration is Admin-only (Priority: P2)

**Goal**: settings entry hidden and service windows read-only below `owner`.

**Independent Test**: quickstart #9–#10.

- [X] T045 [US5] Add `canOpenSettings: Boolean = false` to `HymnalReportUiState` and `canManage: Boolean = false` to
  `ServiceWindowsUiState`; `HymnalReportViewModel` and `ServiceWindowsViewModel` inject `ObserveAccessUseCase` and
  set them from `allows(HYMNAL_HISTORY_REPORT, OWNER)`; `ServiceWindowsViewModel` guards `onCreateRequested`,
  `onEditRequested`, `onActiveToggled`, `onDeleteRequested` when `!canManage`. In `HymnalReportScreen.kt`
  `ReportOverflowMenu` takes `canOpenSettings` and omits "Parâmetros de coleta" when false. In
  `ServiceWindowsScreen.kt` hide the create `IconButton` (line ~75) and pass nullable `onEdit`/`onToggleActive`/
  `onDelete` to `main/features/admin/reports/hymnal/presentation/components/ServiceWindowRow.kt`, which hides each
  control when its lambda is null. Add flag assertions to a new
  `test/features/admin/reports/hymnal/presentation/viewmodel/ServiceWindowsFlagsTest.kt` (`VIEW` → false,
  `OWNER` → true)

**Checkpoint**: Liderança and Mídia read the report without configuration controls.

---

## Phase 8: User Story 6 — Worship hub edit buttons follow `songs` (Priority: P2)

**Goal**: create/edit buttons need `songs` ≥ `manage`; worship hub stops importing `features.profile`.

**Independent Test**: quickstart #11.

- [X] T046 [P] [US6] Rename `isAdmin` → `canEdit` in `main/features/worshiphub/chordcharts/presentation/state/
  ChordChartsUiState.kt`, `ChordChartDetailUiState.kt`, `main/features/worshiphub/lyrics/presentation/state/
  LyricsUiState.kt`, `LyricsDetailUiState.kt`, the screens `ChordChartsScreen.kt`, `ChordChartDetailScreen.kt`,
  `LyricsScreen.kt`, `LyricsDetailScreen.kt`, and the parameter of
  `main/features/worshiphub/shared/presentation/components/SongContentListScreen.kt` (rename `AdminOverflowMenu` →
  `EditorOverflowMenu`)
- [X] T047 [US6] In `ChordChartsViewModel`, `ChordChartDetailViewModel`, `LyricsViewModel`, `LyricsDetailViewModel`
  replace the `ProfileSnapshotRepository` dependency with `ObserveAccessUseCase`; `canEdit =
  access.allows(Scope.SONGS, AccessLevel.MANAGE)`; rename `queryAndAdmin` → `queryAndAccess`. After this, `grep -r
  "features.profile" main/features/worshiphub` must be empty
- [X] T048 [US6] Confirm `specs/worshiphub/spec.md` matches; run
  `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.worshiphub.*"`

**Checkpoint**: Liderança edits songs content; Mídia only reads.

---

## Phase 9: Polish & Cross-Cutting

- [X] T049 Remove `isAdmin` for good: `MeProfileDto` (drop the transitional field), `MeProfile`,
  `ProfileSnapshotRepository` mapper, `ProfileUiState`, `ProfileViewModel`. `grep -rn "isAdmin\|is_admin"
  app/src/main` must be empty (the old-cache test in T010 still passes via `ignoreUnknownKeys`)
- [X] T050 [P] Search `specs/` for leftover "Painel Admin", `is_admin`, `isAdmin`, `IsAdminUser` outside
  `specs/001`–`specs/003` history docs; fix any that describe current behaviour
- [X] T051 Run the full suite `./gradlew :app:testDebugUnitTest`; all green
- [ ] T052 Walk `quickstart.md` #1–#15 on a device against backend 012; record results in the PR

---

## Dependencies & Execution Order

- **Setup (T001–T006)**: all [P], no dependencies.
- **Foundational (T007–T017)**: after Setup; blocks every story.
- **US1 (T018–T022)** → **US2 (T023–T028)**: US2 edits `AdminScreen.kt` after US1's title change (same file).
- **US3 (T029–T033)**, **US5 (T045)**, **US6 (T046–T048)**: each only needs Foundational; independent of each other.
- **US4 (T034–T044)**: needs Foundational; T042 touches the members ViewModels also edited in US3 — run after US3
  or merge carefully. T043 touches report ViewModels also edited in US5 — same note.
- **Polish (T049–T052)**: after US1 and US6 (last `isAdmin` readers gone).

## Parallel Examples

- Setup: T001–T006 together (six different spec files).
- Foundational: T007, T012, T014 (tests) together; then T008 → T009; T011 → T013 → T015 → T016.
- After Foundational: US3, US5 and US6 in parallel by three people; US1 → US2 in a fourth lane.
- US4: T034, T036, T041 (tests) together.

## Implementation Strategy

1. **MVP = Setup + Foundational + US1 + US2** — the app works on backend 012, panel shows per role. Shippable
   together with the backend.
2. Add **US3** (member deletion safety) — must also ship with the backend in practice, since a Liderança would
   otherwise see delete buttons that always fail.
3. Add **US4**, **US5**, **US6** in any order.
4. Polish removes the transitional `is_admin` field.
