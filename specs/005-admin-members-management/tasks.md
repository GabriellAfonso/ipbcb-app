# Tasks: Members Management for Church Leaders

**Input**: Design documents from `specs/005-admin-members-management/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/admin-members-api.md, quickstart.md

**Tests**: Required by `CLAUDE.md` (happy path + 1 error per use case, fakes preferred) and listed in plan.md /
research R13. Each test task comes right before the implementation it covers and must fail first.

**Organization**: Grouped by user story (spec.md US1–US5). Each story's commit carries its domain-spec update
(`CLAUDE.md` §7).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1–US5 from spec.md

## Path Conventions

- Production: `app/src/main/java/com/ipb/castelobranco/` → abbreviated `main/`
- Tests: `app/src/test/java/com/ipb/castelobranco/` → abbreviated `test/`
- Feature root: `main/features/admin/members/` → abbreviated `members/`
- Test commands: `./gradlew :app:testDebugUnitTest --tests "<pattern>"` — never `clean`, `--rerun-tasks`,
  `--no-daemon`

---

## Phase 1: Setup (Specs first)

**Purpose**: Domain specs describe the destination before any code (`CLAUDE.md` §7).

- [X] T001 [P] Update `specs/core/spec.md`: §4 new "Session-scoped caches" (`SessionScopedCache`, multibound
  `@IntoSet`, cleared by `CoreViewModel` on logout and when `isLoggedInFlow` goes true → false); §6.2
  `CoreViewModel.logout()` calls every `SessionScopedCache`; §6.7 `SquarePhotoPicker` (pick → UCrop 1:1 ≤ 512 px →
  bytes, temp file deleted after reading, sweeps `cacheDir/cropped_*`)
- [X] T002 [P] Update `specs/admin/spec.md`: §1 add the members routes (`graph/admin/members`: list, profile, form,
  history) nested in `adminGraph`; §2.1 "Membros" → "Navega para a lista de membros"; new §7 "Membros"
  describing the area per spec.md (status vs. validity, year 0001 = birth year unknown, grid without filters,
  profile, form, photo, history,
  delete, online-only/in-memory rule, 403/404 behavior) and the layering from plan.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core additions, domain types, data layer, DI and navigation shell every story needs.

**⚠️ CRITICAL**: No user story can start until this phase is complete.

### Core

- [X] T003 Create `fun interface SessionScopedCache { suspend fun clear() }` in
  `main/core/domain/session/SessionScopedCache.kt` (KDoc: in-memory, session-only data that must disappear on
  sign-out; see research R6)
- [X] T004 Add a "session-scoped caches" region to `test/core/presentation/viewmodel/CoreViewModelTest.kt` (reuses its setup): a recording fake
  `SessionScopedCache` is cleared once on `logout()` and once when the logged-in flow emits `true` then `false`;
  not cleared on `false` → `false`
- [X] T005 Inject `Set<@JvmSuppressWildcards SessionScopedCache>` into `CoreViewModel`
  (`main/core/presentation/viewmodel/CoreViewModel.kt`), call each `clear()` inside `logout()` before
  `logoutUseCase()`, and in the `isLoggedInFlow` collector when the previous value was `true` and the new one is
  `false`; add an empty `@Multibinds abstract fun sessionScopedCaches(): Set<SessionScopedCache>` in a new
  `main/core/di/SessionModule.kt` so the set exists with no contributors. T004 green
- [X] T006 [P] Extract the pick → UCrop → bytes flow from `ProfileScreen.kt` into
  `main/core/presentation/components/SquarePhotoPicker.kt`:
  `@Composable fun rememberSquarePhotoPicker(onPicked: (ByteArray) -> Unit, onCancelled: () -> Unit = {}): () -> Unit`
  with the same UCrop options (1:1, max 512×512, black toolbar, title "Recortar Foto"); after reading the output
  bytes delete the cropped file; on each launch delete leftover `cacheDir/cropped_*.jpg`; add a `@Preview` of a
  small demo button (required for `core/presentation/components`)
- [X] T007 Switch `main/features/profile/presentation/screens/ProfileScreen.kt` to `rememberSquarePhotoPicker`,
  keeping `viewModel.uploadProfilePhoto(bytes, "profile.jpg")` and `viewModel.clearError()` on cancel; remove the
  now unused launchers/imports (depends on T006)

### Domain

- [X] T008 [P] Create domain models in `members/domain/model/` per data-model.md: `NamedRef.kt`, `Gender.kt`
  (`MALE("M","Masculino")`, `FEMALE("F","Feminino")`, `fromApiCode`), `MemberSummary.kt`, `MemberRecord.kt` (+
  `toSummary()`, `toDraft()`), `MemberOptions.kt`, `MemberDraft.kt`, `MemberField.kt` (API keys + `fromApiKey`),
  `MemberChanges.kt` (+ `MemberDraft.changesFrom(original: MemberDraft?)`, trimmed strings, create rules),
  `HistoryEntry.kt` (`HistoryEntry`, `HistoryEditor`, `HistoryLine`)
- [X] T009 [P] Write `test/features/admin/members/domain/model/MemberChangesTest.kt`: create sends only filled
  fields and `IS_VALID` only when false; edit sends only changed fields; clearing role gives `ROLE → null`;
  whitespace-only change is no change; identical drafts → empty
- [X] T010 Create `MembersAdminRepository` interface in `members/domain/repository/MembersAdminRepository.kt`
  exactly as data-model.md (depends on T008)

### Data

- [X] T011 [P] Create `members/data/api/MembersAdminEndpoints.kt` (path constants: `api/admin/members/`,
  `api/admin/members/{id}/`, `.../photo/`, `.../history/`, `api/admin/members/options/`; header name
  `If-None-Match`, part name `photo`) and `members/data/api/MembersAdminApi.kt` with the methods of
  contracts/admin-members-api.md (`Response<T>` returns, `@Body JsonObject` for create/update, `@Multipart @PUT`
  for photo)
- [X] T012 [P] Create DTOs in `members/data/dto/MembersAdminDtos.kt` per the contract (`@Serializable`,
  `@SerialName` for snake_case)
- [X] T013 Write `test/features/admin/members/data/mapper/MemberMapperTest.kt`: record DTO → domain (dates, gender,
  empty strings, null status/role, `createdAt` parse); summary DTO → domain; `MemberChanges` → `JsonObject` keeps
  cleared fields as `JsonNull`, omits untouched ones, `ministry_ids` as int array, dates as `YYYY-MM-DD`, gender
  as `M`/`F`; history DTO → `HistoryEntry` with `editor = null`
- [X] T014 Implement `members/data/mapper/MemberMapper.kt` (DTO → domain, `MemberChanges.toJsonObject()` with
  `buildJsonObject`, research R2). T013 green
- [X] T015 [P] Create `test/features/admin/members/data/api/FakeMembersAdminApi.kt`: per-method scripted
  `Response` (success, `Response.error(code, body)`, or thrown `IOException`), records bodies, ids,
  `If-None-Match` values and call counts; and `test/features/admin/members/MembersTestFixtures.kt` with builders
  for DTOs and domain objects (`recordDto(...)`, `summaryDto(...)`, `optionsDto()`, `historyDto(...)`)
- [X] T016 Write `test/features/admin/members/data/repository/MembersAdminRepositoryImplTest.kt`: refresh 200
  fills the flow and stores the ETag; second refresh sends `If-None-Match` and 304 keeps the list; create inserts
  sorted by name; update replaces the summary; `uploadPhoto` updates `photoUrl`; delete removes; 404 on
  `getMember` returns `AppError` with `userMessage` "Este membro não existe mais" and drops the member; 403 →
  `AppError.Auth(403)`; `IOException` → `AppError.Network`; `clear()` resets the flow to `null` and forgets ETags
- [X] T017 Implement `members/data/repository/MembersAdminRepositoryImpl.kt` (`@Singleton`, in-memory
  `MutableStateFlow<List<MemberSummary>?>`, list ETag, record cache `id → (etag, record)`, `runCatching {} .mapError()`
  with `response.toAppError()`, `Mutex` around state updates; logs with `Timber` show ids only). T016 green

### DI and navigation shell

- [X] T018 Create `members/di/MembersAdminModule.kt`: `@Binds` repository, `@Provides` `MembersAdminApi` from
  `@AuthedRetrofit`, `@Provides @IntoSet SessionScopedCache` that calls `repository.clear()` and clears the
  `@MemberPhotoLoader` memory cache
- [X] T019 [P] Create `members/di/MemberPhotoLoaderModule.kt` with qualifier `@MemberPhotoLoader` and a
  `@Singleton ImageLoader` built from `@ApplicationContext` with `okHttpClient(@Client client)`,
  `diskCache(null)`, `respectCacheHeaders(false)`, default memory cache (research R3)
- [X] T020 [P] Create `members/presentation/state/MembersEvent.kt` (sealed: `ShowMessage`, `LeaveArea`,
  `MemberGone`, `Saved(memberId)`, `Deleted`) and `members/presentation/util/MembersFailure.kt`:
  `fun Throwable.toMembersEvent(): MembersEvent` — `AppError.Auth(403)` → `LeaveArea`, `Server(404)` →
  `MemberGone`, else `ShowMessage(toAppError().toUserMessage())`
- [X] T021 Create `members/presentation/navigation/MembersNavGraph.kt`: `object MembersRoutes` (`GRAPH =
  "graph/admin/members"`, `LIST`, `PROFILE = "MembersProfile/{memberId}"`, `FORM = "MembersForm?memberId={memberId}"`,
  `HISTORY = "MembersHistory/{memberId}"` + route builders) and `fun NavGraphBuilder.membersGraph(navController)`
  with `navigation(route = GRAPH, startDestination = LIST)`; destinations added by each story
- [X] T022 Add `members: () -> Unit` to `AdminNav` and call `membersGraph(navController)` inside `adminGraph` in
  `main/features/admin/panel/presentation/navigation/AdminNavGraph.kt`; `members = { navController.navigate(
  MembersRoutes.GRAPH) }`

**Checkpoint**: `./gradlew :app:compileDebugKotlin` passes; T004, T009, T013, T016 green.

---

## Phase 3: User Story 1 - Leader browses the roll and opens a member (Priority: P1) 🎯 MVP

**Goal**: The "Membros" card opens a grid of every member (search, invalid tag, status chips) and a read-only
profile.

**Independent Test**: As a leader, open Membros, search, open full and empty profiles and compare with server data;
as a non-leader, confirm there is no entry (quickstart §3 rows 1–6).

- [X] T023 [P] [US1] Write `test/features/admin/members/domain/usecase/ComputeMemberAgeUseCaseTest.kt` with a fixed
  `DateProvider`: 02/04/1990 on 26/09/2026 → "36 anos"; birthday tomorrow → one year less; 1 → "1 ano"; baptism
  12/06/2005 → "há 21 anos", same year → "este ano", 1 → "há 1 ano"; null or future → null; **0001-04-02 → null
  (year unknown)**
- [X] T024 [P] [US1] Create `members/domain/model/BirthDate.kt` (`UNKNOWN_BIRTH_YEAR = 1`,
  `LocalDate.hasUnknownYear()`) and implement `members/domain/usecase/ComputeMemberAgeUseCase.kt`
  (`ageLabel(birth)` returns null for `hasUnknownYear()`, `sinceLabel(date)`, research R10). T023 green
- [X] T025 [P] [US1] Create `ObserveMembersUseCase.kt`, `RefreshMembersUseCase.kt`, `GetMemberUseCase.kt` in
  `members/domain/usecase/`
- [X] T026 [P] [US1] Create `members/presentation/util/MemberFormatting.kt`: `initialsOf(name)`,
  `normalizeForSearch(text)` (NFD, strip marks, lowercase), `formatDate(LocalDate)` dd/MM/yyyy — or
  "dd/MM" when `hasUnknownYear()` —,
  `formatDateTime(Instant)` dd/MM/yyyy HH:mm in device zone, status label ("Sem situação" when null)
- [X] T027 [P] [US1] Create `members/presentation/state/MembersListUiState.kt` (`MembersListUiState`,
  `MemberCardUi`) and `members/presentation/state/MemberProfileUiState.kt` (`MemberProfileUiState`,
  `MemberProfileUi`) per data-model.md
- [X] T028 [US1] Write `test/features/admin/members/presentation/viewmodel/MembersListViewModelTest.kt` (fake
  repository or real repository over `FakeMembersAdminApi`): loading → content; search "jose" matches "José";
  empty roll vs no search result (`totalCount`); refresh failure → `error`; 403 → `LeaveArea` event
- [X] T029 [US1] Implement `members/presentation/viewmodel/MembersListViewModel.kt` (`combine(observeMembers,
  query)`, refresh on init and on `retry()`, exposes `@MemberPhotoLoader imageLoader`). T028 green
- [X] T030 [US1] Write `test/features/admin/members/presentation/viewmodel/MemberProfileViewModelTest.kt` (read
  part): loads record from `SavedStateHandle["memberId"]`, maps every label incl. "Não informado", "Sem cargo",
  "Nenhum ministério", "há 21 anos", and birth 0001-04-02 → "02/04" + "Desconhecida"; 404 → `MemberGone`; 403 → `LeaveArea`; network error → `error`
- [X] T031 [US1] Implement `members/presentation/viewmodel/MemberProfileViewModel.kt` (read part: load, map to
  `MemberProfileUi`, retry, `imageLoader`). T030 green
- [X] T032 [P] [US1] Create `members/presentation/components/MemberAvatar.kt`: `AsyncImage` with the passed
  `ImageLoader`, circle or rounded shape, `error`/`fallback` = initials on `primaryContainer`; `@Preview` with
  initials
- [X] T033 [P] [US1] Create `members/presentation/components/MemberCard.kt` (grid card from the design preview:
  132 dp photo area, name 2 lines, status chip, "Perfil inválido" tag, faded when invalid) and
  `members/presentation/components/ProfileSections.kt` (`SectionCard`, `InfoRow`, `MinistriesRow`, `StatusChip`,
  `Tag`), moved from `MembersDesignPreviews.kt`
- [X] T034 [US1] Create `members/presentation/screens/MembersListScreen.kt`: `MembersListScreen(viewModel, onBack,
  onOpenMember, onAddMember, onLeaveArea)` collecting state/events, and pure `MembersListContent` inside
  `BaseScreen(tabName = "Membros", showBackArrow = true)`: search field, `LazyVerticalGrid(GridCells.Fixed(2))`,
  FAB "Novo membro", loading / error with "Tentar novamente" / empty roll / "Nenhum membro encontrado para
  \"<termo>\""
- [X] T035 [US1] Create `members/presentation/screens/MemberProfileScreen.kt`: `MemberProfileScreen` + pure
  `MemberProfileContent` per design "Perfil A" (green band, 148 dp avatar, name, "<idade> · <sexo>", status and
  role chips, "Dados pessoais", "Vida na igreja", "Cadastrado em"); edit, photo,
  validity, history and delete controls are added by later stories
- [X] T036 [US1] Register `LIST` and `PROFILE` destinations in `membersGraph` (`MembersNavGraph.kt`) with
  `hiltViewModel()`; `LeaveArea` → `navController.popBackStack(AdminRoutes.ADMIN, inclusive = false)` +
  message; `MemberGone` → back to `LIST`
- [X] T037 [US1] Enable the "Membros" action in `main/features/admin/panel/presentation/screens/AdminScreen.kt`:
  remove `enabled = false`, `onClick = nav.members`

**Checkpoint**: US1 works alone — leaders browse and read profiles. Commit with T002's §7 (list/profile parts).

---

## Phase 4: User Story 2 - Leader creates and edits a member (Priority: P1)

**Goal**: One form for create/edit with server options, local + server validation, and the immediate "Perfil
válido" switch on the profile.

**Independent Test**: Create a full member, edit one field (only it is sent), trigger each validation rule, toggle
validity online and offline (quickstart §3 rows 7–13).

- [X] T038 [P] [US2] Write `test/features/admin/members/domain/usecase/ValidateMemberDraftUseCaseTest.kt`: every row
  of research R7 plus a valid draft → empty map; birth 0001-04-02 is not "future" and a baptism in 1990 with it
  passes (baptism-before-birth skipped)
- [X] T039 [P] [US2] Implement `members/domain/usecase/ValidateMemberDraftUseCase.kt` (injects `DateProvider`,
  messages from research R7 as constants). T038 green
- [X] T040 [P] [US2] Write `test/features/admin/members/domain/usecase/SaveMemberUseCaseTest.kt`: create calls
  `createMember` with `changesFrom(null)`; edit sends only changes; empty changes returns the original record
  without calling the repository; repository failure propagates
- [X] T041 [P] [US2] Implement `SaveMemberUseCase.kt`, `SetMemberValidityUseCase.kt`, `GetMemberOptionsUseCase.kt`
  in `members/domain/usecase/`. T040 green
- [X] T042 [P] [US2] Create `members/presentation/state/MemberFormUiState.kt` per data-model.md
- [X] T043 [US2] Write `test/features/admin/members/presentation/viewmodel/MemberFormViewModelTest.kt`: create mode
  loads options; edit mode loads record + options into the draft; local errors block save; server
  `field_errors` (`name`, `status_id`, `ministry_ids`) land on `MemberField`s, unknown key → `generalError`;
  `hasUnsavedChanges` true after a change and false after undoing it; success emits `Saved(id)`; options failure →
  `optionsError` with retry
- [X] T044 [US2] Implement `members/presentation/viewmodel/MemberFormViewModel.kt` (`memberId` from
  `SavedStateHandle`, `onDraftChanged`, `onSave`, `reloadOptions`, reload options after a 400 without field
  errors). T043 green
- [X] T045 [P] [US2] Create form components in `members/presentation/components/`: `GenderSelector.kt`
  (Masculino / Feminino / Não informado), `OptionPicker.kt` (single choice from `List<NamedRef>` + "Nenhum"),
  `MinistriesPicker.kt` (multi-choice chips), reusing `core/presentation/components/DateFieldWithPicker.kt` for
  dates, and `BirthDateField.kt`: date field + "Não sei o ano" switch; when on, a day/month picker (February stops
  at 28) producing `LocalDate.of(UNKNOWN_BIRTH_YEAR, m, d)`; opens with the switch on for a 0001 date (spec
  FR-016a)
- [X] T046 [US2] Create `members/presentation/screens/MemberFormScreen.kt`: `MemberFormScreen` + pure
  `MemberFormContent` in `BaseScreen` ("Novo membro" / "Editar membro"), fields in spec FR-016 order, error text
  under each field, general error banner, "Salvar" with progress, `BackHandler` → discard dialog "Descartar
  alterações?" when `hasUnsavedChanges`
- [X] T047 [US2] Extend `MemberProfileViewModelTest.kt`: `onValidityChanged(false)` updates UI at once and calls
  `SetMemberValidityUseCase`; failure restores the previous value and emits `ShowMessage`
- [X] T048 [US2] Add `onValidityChanged` to `MemberProfileViewModel.kt` (optimistic, `isSavingValidity`, revert on
  failure). T047 green
- [X] T049 [US2] Add to `MemberProfileScreen.kt`: edit icon in the band, "Perfil válido" switch card with the
  explanatory text for each state (spec FR-014)
- [X] T050 [US2] Register `FORM` in `membersGraph`: FAB → `FORM` without id; edit → `FORM?memberId=`; `Saved` in
  create mode → `navigate(PROFILE(id)) { popUpTo(FORM) { inclusive = true } }`, in edit mode →
  `safePopBackStack()`

**Checkpoint**: US1 + US2 — the roll can be kept current from the app.

---

## Phase 5: User Story 3 - Leader manages the member photo (Priority: P2)

**Goal**: Add, replace and remove the leader-only photo; failures fall back to initials.

**Independent Test**: Add → replace → remove a photo and check profile and grid; try an oversized/unsupported file
(quickstart §3 row 14).

- [X] T051 [P] [US3] Write `test/features/admin/members/domain/usecase/ValidateMemberPhotoUseCaseTest.kt`: JPEG,
  PNG, WEBP, GIF magic bytes → mime type; empty → error; > 10 MB → "A foto deve ter no máximo 10 MB."; unknown
  bytes → "Use uma imagem JPEG, PNG, WEBP ou GIF."
- [X] T052 [P] [US3] Implement `ValidateMemberPhotoUseCase.kt`, `UploadMemberPhotoUseCase.kt` (validates then
  uploads), `RemoveMemberPhotoUseCase.kt` in `members/domain/usecase/`. T051 green
- [X] T053 [US3] Extend `MemberProfileViewModelTest.kt`: `onPhotoPicked(bytes)` uploads and updates `photoUrl`;
  invalid bytes → message, no call; `onRemovePhotoConfirmed()` sets `photoUrl = null`; upload failure keeps the old
  photo and shows the error; `isPhotoBusy` while running
- [X] T054 [US3] Add `onPhotoPicked`, `onRemovePhotoRequested/Confirmed/Dismissed` to `MemberProfileViewModel.kt`.
  T053 green
- [X] T055 [US3] Add to `MemberProfileScreen.kt`: camera button on the avatar opening a small menu ("Escolher
  foto", and "Remover foto" when there is one); "Escolher foto" → `rememberSquarePhotoPicker`; remove
  confirmation dialog; progress over the avatar while `isPhotoBusy`

- [X] T074 [US3] Replace the camera button in `MemberProfileScreen.kt` (FR-023a): tap on the photo opens
  `MemberPhotoPreview` (square popped over a dimmed background); tap on it opens `MemberPhotoViewer` full screen
  with the camera menu top-left and close top-right (both new, in `components/`); initials stand in for no photo
- [X] T075 [US3] Extend `MemberFormViewModelTest.kt` (FR-022a): picked photo previews and marks unsaved; invalid
  bytes → message, nothing kept; save sends the fields then uploads; upload failure keeps the photo and the message,
  and a second save retries only the upload; create mode ignores photos
- [X] T076 [US3] Add `onPhotoPicked` and the post-save upload to `MemberFormViewModel.kt`; photo at the top of
  `MemberFormScreen.kt` (edit only) with a camera badge. T075 green

**Checkpoint**: Photos flow end to end with nothing written to disk (quickstart §2 grep).

---

## Phase 6: User Story 4 - Leader reads the edit history (Priority: P2)

**Goal**: History screen with Portuguese sentences and the last change on the profile.

**Independent Test**: A member with every entry type (creation, photo changed/removed, each field, deleted editor)
reads correctly (quickstart §3 row 15).

- [X] T056 [P] [US4] Write `test/features/admin/members/domain/usecase/BuildHistorySentenceUseCaseTest.kt`: one case
  per row of research R9 (created, photo changed/removed, status, birth_date formatted, birth_date "0001-04-02" →
  "02/04", gender M/F, is_active
  true/false, ministries, null → "vazio", unknown field, editor null → "Usuário removido")
- [X] T057 [P] [US4] Implement `BuildHistorySentenceUseCase.kt` and `GetMemberHistoryUseCase.kt` in
  `members/domain/usecase/`. T056 green
- [X] T058 [P] [US4] Create `members/presentation/state/MemberHistoryUiState.kt` (`HistoryLineUi`)
- [X] T059 [US4] Write `test/features/admin/members/presentation/viewmodel/MemberHistoryViewModelTest.kt`: lines in
  server order with formatted time; empty list; 404 → `MemberGone`; network error → `error`
- [X] T060 [US4] Implement `members/presentation/viewmodel/MemberHistoryViewModel.kt`. T059 green
- [X] T061 [US4] Create `members/presentation/screens/MemberHistoryScreen.kt` (`BaseScreen("Histórico")`, timeline
  list: editor bold + sentence + time, loading/error/empty "Nenhuma alteração registrada") and register `HISTORY`
  in `membersGraph`
- [X] T062 [US4] Load the first history line in `MemberProfileViewModel.kt` (`lastChange`, failure ignored) with a
  test case in `MemberProfileViewModelTest.kt`, and add the "Histórico de alterações" card to
  `MemberProfileScreen.kt` opening `HISTORY`

**Checkpoint**: Every change made in US2/US3 shows up as a readable sentence.

---

## Phase 7: User Story 5 - Leader deletes a member (Priority: P3)

**Goal**: Delete with a type-the-name confirmation; back to the grid without the member.

**Independent Test**: Wrong name keeps the button disabled; right name deletes and returns to the grid (quickstart
§3 row 16).

- [X] T063 [P] [US5] Write `test/features/admin/members/domain/model/DeleteConfirmationTest.kt`: exact, different
  case, surrounding spaces → match; partial or different name → no match
- [X] T064 [P] [US5] Create `members/domain/model/DeleteConfirmation.kt` and
  `members/domain/usecase/DeleteMemberUseCase.kt`. T063 green
- [X] T065 [US5] Extend `MemberProfileViewModelTest.kt`: delete success emits `Deleted`; failure closes the dialog,
  keeps the profile and emits `ShowMessage`
- [X] T066 [US5] Add `onDeleteRequested/Confirmed/Dismissed` and `deleteTyped` state to `MemberProfileViewModel.kt`.
  T065 green
- [X] T067 [US5] Create `members/presentation/components/DeleteMemberDialog.kt` (warning text "A ficha, o histórico
  e a foto de <nome> serão apagados para sempre.", text field "Digite o nome do membro para confirmar", "Excluir"
  enabled only on match, `@Preview`) and add the "Excluir membro" button to `MemberProfileScreen.kt`; `Deleted` →
  back to `LIST` in `membersGraph`

**Checkpoint**: All five stories complete.

---

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T068 Delete `members/presentation/screens/MembersDesignPreviews.kt`; add `@Preview`s (light/dark, fake data,
  status names Ativo/Inativo/Visitante) to `MembersListContent`, `MemberProfileContent`, `MemberFormContent`,
  `MemberHistoryContent`
- [X] T069 [P] Run the quickstart §2 audits (Timber ids only, no storage APIs, no hardcoded status names) and fix
  any hit
- [X] T070 [P] Check every new file against `CLAUDE.md`: 120-char lines, no magic strings (routes, keys, messages
  as constants), Portuguese UI text, no cross-feature imports (`features.profile` must not appear under
  `features/admin/members`)
- [X] T071 Run `./gradlew :app:testDebugUnitTest` and `./gradlew :app:compileDebugKotlin`; all green
- [ ] T072 Walk the device checklist in `quickstart.md` §3 (rows 1–20) and record results in the PR description
- [X] T073 Final pass on `specs/admin/spec.md` §7 and `specs/core/spec.md` so they match the shipped code

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none — do first (spec before code).
- **Foundational (Phase 2)**: after Setup; blocks every story.
- **US1 (Phase 3)**: after Foundational. MVP.
- **US2 (Phase 4)**: after Foundational; touches `MemberProfileViewModel`/`Screen` from US1 → do after US1.
- **US3, US4, US5 (Phases 5–7)**: each after US1 (they extend the profile); independent of each other and of US2,
  but they edit the same profile files, so run them one after another.
- **Polish (Phase 8)**: after the stories you ship.

### Within Phase 2

T003 → T004 → T005 · T006 → T007 · T008 → T009, T010 · T011, T012 → T013 → T014 → T015 → T016 → T017 → T018 ·
T019, T020 any time · T021 → T022.

### Within each story

Test task → implementation task (must go red, then green) → UI → navigation wiring.

## Parallel Opportunities

- Phase 1: T001 ∥ T002.
- Phase 2: T003 ∥ T006 ∥ T008 ∥ T011 ∥ T012 ∥ T019 ∥ T020 (different files).
- US1: T023 ∥ T025 ∥ T026 ∥ T027 ∥ T032 ∥ T033.
- US2: T038 ∥ T040 ∥ T042 ∥ T045.
- US3: T051 ∥ T052 (after T051 red).
- US4: T056 ∥ T058.
- US5: T063 ∥ T064 (after T063 red).
- Polish: T069 ∥ T070.

### Example — US1 kick-off

```text
Parallel: T023 age test · T025 use cases · T026 formatting · T027 UI state · T032 avatar · T033 card/sections
Then:     T024 → T028 → T029 → T030 → T031 → T034 → T035 → T036 → T037
```

## Implementation Strategy

### MVP first

1. Phase 1 + Phase 2.
2. Phase 3 (US1) → validate quickstart rows 1–6 → commit `feat(members): browse the roll and open member
   profiles` with the spec update. Leaders can already stop asking for Django admin access to read the roll.

### Incremental delivery

3. US2 → `feat(members): create and edit members`.
4. US3 → `feat(members): manage the leader-only member photo`.
5. US4 → `feat(members): show the member edit history`.
6. US5 → `feat(members): delete a member with name confirmation`.
7. Polish.

Core slices commit separately first: `feat(core): add session-scoped caches cleared on sign-out` (T003–T005),
`refactor(profile): extract square photo picker to core` (T006–T007).
