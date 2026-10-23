# Tasks: Access Requests

**Input**: Design documents from `specs/016-access-requests/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui.md

**Tests**: required by `CLAUDE.md` (use cases and ViewModels: happy path + at least one error; DTO backward
compatibility; push parsing).

Paths are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and `app/src/test/java/com/ipb/castelobranco/`
(test) unless they start with `specs/`.

## Phase 1: Setup

- [X] T001 Update the domain specs for the destination: access card + roll data in specs/profile/spec.md, "Perfis"
  in specs/admin/spec.md, member access / locked screens / pushes in specs/core/spec.md

## Phase 2: Foundational (blocks every story)

- [X] T002 Rename `isMember` → `memberAccess` and add `access_status`, `access_requests_blocked`,
  `data_correction_pending` (nullable, defaults) in features/profile/data/dto/MeProfileDto.kt; `memberAccess` +
  `memberAccessInfo` in features/profile/domain/model/MeProfile.kt; map in
  features/profile/data/snapshot/ProfileSnapshotRepository.kt; fix every test that builds the DTO/model
- [X] T003 [P] Cover the old snapshot (only `is_member`) and the new fields in
  features/profile/data/dto/MeProfileDtoBackwardCompatibilityTest.kt (test)
- [X] T004 Add `AccessStatus`, `MemberAccess`, `AccessRequestType`, `AccessPrompt` + `accessPromptFor()` in
  core/domain/access/MemberAccess.kt; `MemberAccessRepository`, `ObserveMemberAccessUseCase`, `RequestAccessUseCase`
  in core/domain/access/MemberAccessRepository.kt; `Scope.PROFILES` in core/domain/access/Scope.kt
- [X] T005 [P] Test `accessPromptFor` in core/domain/access/MemberAccessTest.kt (test)
- [X] T006 Add `POST me/access-requests/` and `GET me/member/` to features/profile/data/api/ProfileApi.kt and
  ProfileEndpoints.kt with DTOs (`CreateAccessRequestBody`, `OwnMemberDto`) in features/profile/data/dto/AccessRequestDtos.kt; update
  FakeProfileApi (test)
- [X] T007 Implement features/profile/data/access/ProfileMemberAccessRepository.kt (status from the snapshot, request
  + profile refresh, refresh also after a refusal) and bind it in features/profile/di/ProfileModule.kt
- [X] T008 [P] Test ProfileMemberAccessRepository (mapping, success refresh, refusal mapped + refresh) in
  features/profile/data/access/ProfileMemberAccessRepositoryTest.kt (test)

## Phase 3: User Story 1 — Ask for access from a locked screen (P1)

**Goal**: "Pedir acesso" / pending / blocked on gallery, schedule and birthdays.

**Independent test**: placeholder VM tests + manual quickstart 1–2, 6.

- [X] T009 [US1] Create core/presentation/components/access/MembersOnlyViewModel.kt (prompt state, dialog state,
  send, events) and core/presentation/components/access/MembersOnlyPlaceholder.kt (collector + pure content +
  `AccessRequestDialog` + previews)
- [X] T010 [P] [US1] Test MembersOnlyViewModel (prompt per status, send success, refusal message) in
  core/presentation/components/access/MembersOnlyViewModelTest.kt (test)
- [X] T011 [US1] Use `MembersOnlyPlaceholder` on the 403 branch in features/gallery/presentation/screens/GalleryScreen.kt,
  features/schedule/presentation/screens/ScheduleScreen.kt and core/presentation/screens/BirthdaysScreen.kt

## Phase 4: User Story 2 — Leaders decide requests in "Perfis" (P1)

**Goal**: Perfis card, Pendentes / Todos, detail with approve and deny.

**Independent test**: repository + VM tests; manual quickstart 3–4, 8.

- [X] T012 [US2] Create features/admin/profiles/data/api/ProfilesAdminApi.kt + ProfilesAdminEndpoints.kt and DTOs in
  features/admin/profiles/data/dto/ProfilesAdminDtos.kt
- [X] T013 [US2] Create the models in features/admin/profiles/domain/model/ManagedAccount.kt, the port
  features/admin/profiles/domain/repository/ProfilesAdminRepository.kt and the use cases in
  features/admin/profiles/domain/usecase/ProfilesAdminUseCases.kt
- [X] T014 [US2] Implement features/admin/profiles/data/repository/ProfilesAdminRepositoryImpl.kt (memory lists per
  tab, detail, actions updating the lists) and features/admin/profiles/di/ProfilesAdminModule.kt (binding +
  `@ProfilePhotoLoader` memory-only Coil loader)
- [X] T015 [P] [US2] Test ProfilesAdminRepositoryImpl (lists, approve removes from Pendentes, deny body, error
  mapping) in features/admin/profiles/data/repository/ProfilesAdminRepositoryImplTest.kt with a fake API (test)
- [X] T016 [US2] Create the list VM/state/screen: features/admin/profiles/presentation/viewmodel/ProfilesListViewModel.kt,
  state/ProfilesListUiState.kt, screens/ProfilesListScreen.kt, components/ProfileRow.kt
- [X] T017 [US2] Create the detail VM/state/screen with approve and deny:
  features/admin/profiles/presentation/viewmodel/ProfileDetailViewModel.kt, state/ProfileDetailUiState.kt,
  screens/ProfileDetailScreen.kt, components/ProfileDetailSections.kt, components/ProfilesDialogs.kt
- [X] T018 [P] [US2] Test both VMs (load, tab switch, approve, deny with reason, view-only hides actions, 409
  message + reload) in features/admin/profiles/presentation/viewmodel/ (test)
- [X] T019 [US2] Add features/admin/profiles/presentation/navigation/ProfilesNavGraph.kt, nest it in
  features/admin/panel/presentation/navigation/AdminNavGraph.kt, add `PanelCard.PROFILES` in
  features/admin/panel/domain/PanelCard.kt and its action in features/admin/panel/presentation/screens/AdminScreen.kt;
  update panel tests

## Phase 5: User Story 3 — Access card and roll data on the profile (P2)

**Goal**: card + "Meus dados na igreja" + "Solicitar correção", roll data memory-only.

**Independent test**: ProfileViewModel tests; manual quickstart 7, 10.

- [X] T020 [US3] Create features/profile/domain/model/OwnMember.kt, features/profile/domain/repository/OwnMemberRepository.kt,
  features/profile/data/repository/OwnMemberRepositoryImpl.kt (404 `MEMBER_NOT_LINKED` → null) and
  features/profile/domain/usecase/GetOwnMemberUseCase.kt; bind in ProfileModule
- [X] T021 [P] [US3] Test OwnMemberRepositoryImpl (200, 404 not linked, network error) in
  features/profile/data/repository/OwnMemberRepositoryImplTest.kt (test)
- [X] T022 [US3] Profile state + VM: access card, own member load/retry, access and correction dialogs, via
  `RequestAccessUseCase`, in features/profile/presentation/state/ProfileUiState.kt and viewmodel/ProfileViewModel.kt
- [X] T023 [P] [US3] Test the new ProfileViewModel paths in features/profile/presentation/viewmodel/ProfileViewModelTest.kt
  (test)
- [X] T024 [US3] Access card and roll section in features/profile/presentation/components/AccessSections.kt, correction
  dialog in AccountDialogs.kt, wired in features/profile/presentation/screens/ProfileScreen.kt

## Phase 6: User Story 4 — Revoke, block, link (P2)

**Goal**: management actions in the detail.

**Independent test**: detail VM tests; manual quickstart 5–7.

- [X] T025 [US4] Revoke (confirm, role note), block switch, link picker (search over Membros) and unlink in
  ProfileDetailViewModel.kt, ProfileDetailScreen.kt, ProfilesDialogs.kt (features/admin/profiles/presentation/)
- [X] T026 [P] [US4] Test revoke, block, link, unlink and 409 MEMBER_ALREADY_LINKED in
  features/admin/profiles/presentation/viewmodel/ProfileDetailViewModelTest.kt (test)
- [X] T034 [US4] "Acesso de membro" switch (grant with confirmation, revoke with confirmation) and the "Liberado por
  {nome} em {data}" note: `grant-access/` in ProfilesAdminApi.kt, repository and `GrantMemberAccessUseCase`,
  `access_granted_by` / `access_granted_at` in the DTO and `ManagedAccount`, `ConfirmAction.GRANT` in
  ProfileDetailViewModel.kt, ProfileDetailScreen.kt, ProfilesDialogs.kt
- [X] T035 [P] [US4] Test grant (confirm, call, message), grant-record mapping in
  ProfileDetailViewModelTest.kt and ProfilesAdminRepositoryImplTest.kt (test)

## Phase 7: User Story 5 — Push notifications (P3)

**Goal**: three pushes, notifications, taps.

**Independent test**: parsing + handler tests; manual quickstart 3–4, 7.

- [X] T027 [US5] Add the three messages to core/domain/push/PushMessage.kt
- [X] T028 [US5] Create core/data/push/AccessRequestNotifications.kt (`AccessRequestNotifier`, channel "Pedidos de
  acesso") and bind it; dispatch + profile refresh in core/data/push/PushMessageHandler.kt
- [X] T029 [P] [US5] Test parsing and handler dispatch in core/domain/push/PushMessageTest.kt and
  core/data/push/PushMessageHandlerTest.kt (test)
- [X] T030 [US5] Add `ProfilesPending` and `Profile` to core/presentation/navigation/NotificationTarget.kt; route them
  in core/presentation/navigation/AppNavHost.kt (`CoreViewModel.canOpenProfiles()`), with a navigation helper in
  AdminNavGraph.kt

## Phase 8: Polish

- [X] T031 Run `./gradlew :app:testDebugUnitTest` and `:app:compileDebugKotlin`; fix failures
- [X] T032 Grep the feature for `email`/`e-mail` and for logs of roll data; none allowed
- [ ] T033 **Manual, needs device + backend 022.** Walk specs/016-access-requests/quickstart.md

## Dependencies

- Phase 2 blocks everything. US1 needs T004–T008. US2 needs T004 (`Scope.PROFILES`). US3 needs T006–T007. US4
  extends US2. US5 needs US2 (Perfis route) and T007.
- MVP: Phase 2 + US1 + US2 (the request loop).

## Parallel examples

- T003, T005, T008 after their implementations; T015 with T016; T021 with T022.
