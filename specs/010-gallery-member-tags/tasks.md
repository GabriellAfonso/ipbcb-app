# Tasks: Gallery Member Tags

**Input**: Design documents from `specs/010-gallery-member-tags/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/gallery-tags-client.md, quickstart.md

**Tests**: requested by the spec (request's test list, SC-006).

Paths below are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and
`app/src/test/java/com/ipb/castelobranco/` (test) unless they start with `specs/`.

## Phase 1: Setup

No setup: no new library, no manifest change.

## Phase 2: Foundational (blocking for every story)

- [X] T001 [P] Add `member_id` (`Long? = null`) to `features/profile/data/dto/MeProfileDto.kt`, `memberId` to `features/profile/domain/model/MeProfile.kt` and map it in `features/profile/data/snapshot/ProfileSnapshotRepository.kt`
- [X] T002 [P] Create `CurrentMemberRepository` and `ObserveOwnMemberIdUseCase` in `core/domain/member/`
- [X] T003 Create `ProfileCurrentMemberRepository` in `features/profile/data/access/ProfileCurrentMemberRepository.kt` and bind it in `features/profile/di/ProfileModule.kt`
- [X] T004 [P] Add `TAGGABLE_MEMBERS`, `PHOTO_MEMBERS`, `PHOTOS_MEMBERS` to `features/gallery/data/api/GalleryEndpoints.kt` and `getTaggableMembers`, `putPhotoMembers`, `changePhotoMembers` to `features/gallery/data/api/GalleryApi.kt` (contract §1)
- [X] T005 [P] Add `photoMembersBody` and `changeMembersBody` to `features/gallery/data/dto/GalleryWriteBodies.kt`
- [X] T006 [P] Add `GalleryLocalChange.UpsertPhotos` in `features/gallery/domain/model/GalleryLocalChange.kt` and apply it in `features/gallery/domain/model/GalleryIndex.kt`
- [X] T007 Add `taggableMembers()`, `setPhotoMembers()`, `changePhotoMembers()` to `features/gallery/domain/manage/GalleryManageRepository.kt` and implement them in `features/gallery/data/manage/GalleryManageRepositoryImpl.kt`
- [X] T008 [P] Create `GalleryPeople` (`taggedPeople`, `photosWithAll`, `peopleIn`, `TaggedPerson`) in `features/gallery/domain/tags/GalleryPeople.kt` and `NameSearch` in `features/gallery/domain/tags/NameSearch.kt`
- [X] T009 Create `TagSaveResult`, `TagBatchResult` in `features/gallery/domain/tags/TagModels.kt` and `LoadTaggableMembersUseCase`, `SetPhotoMembersUseCase`, `ChangePhotoMembersUseCase` in `features/gallery/domain/tags/TagUseCases.kt`; add them to `features/gallery/domain/manage/GalleryManageUseCases.kt`
- [X] T010 Extend test fakes: `core/testing/FakeCurrentMemberRepository.kt` (new), `features/gallery/data/api/FakeGalleryApi.kt`, `features/gallery/domain/manage/FakeGalleryManageRepository.kt`, `features/gallery/presentation/viewmodel/GalleryManageFixtures.kt`
- [X] T011 [P] Profile tests: `member_id` null/absent/present in `features/profile/data/dto/MeProfileDtoBackwardCompatibilityTest.kt`; live member id in `features/profile/data/access/ProfileCurrentMemberRepositoryTest.kt`
- [X] T012 [P] Data tests: bodies in `features/gallery/data/dto/GalleryWriteBodiesTest.kt`; `UpsertPhotos` in `features/gallery/domain/model/GalleryIndexApplyTest.kt`; tag writes applied without moving the cursor, 404 syncs, picker list read in `features/gallery/data/manage/GalleryManageRepositoryTagsTest.kt`
- [X] T013 [P] Domain tests: AND filter 1/2/3 people and empty, people counts/order, `peopleIn` in `features/gallery/domain/tags/GalleryPeopleTest.kt`; accent/case search in `features/gallery/domain/tags/NameSearchTest.kt`; unchanged save, chunking at 200, partial failure, forbidden stops in `features/gallery/domain/tags/TagUseCasesTest.kt`

## Phase 3: User Story 1 — See who is in a photo (P1) 🎯 MVP

**Goal**: every member opens "ⓘ" in the viewer and sees description, date and people.

**Independent test**: open a tagged and an untagged photo, check the sheet, swipe with it open.

- [X] T014 [US1] Add `albumId`, `description`, `dateTaken`, `people` to `ViewerPhoto` in `features/gallery/presentation/state/GalleryUiState.kt` and fill them in `features/gallery/presentation/viewmodel/GalleryUiMapper.kt`
- [X] T015 [P] [US1] Create `PhotoDetailsSheet` in `features/gallery/presentation/components/GalleryTagComponents.kt` (with preview)
- [X] T016 [US1] Add "ⓘ" and the sheet (following the current page) to `features/gallery/presentation/screens/PhotoScreen.kt`

## Phase 4: User Story 2 — Tag people in one photo (P1)

**Goal**: `manage` users save the people of a photo from the viewer menu or the sheet.

**Independent test**: tag two people, remove one, save without changes sends nothing.

- [X] T017 [P] [US2] Create `TagTexts` in `features/gallery/presentation/viewmodel/TagTexts.kt`
- [X] T018 [US2] Add `PickerMode`, `PeoplePickerState`, `GalleryDialogState.PeoplePicker` to `features/gallery/presentation/state/GalleryDialogState.kt`
- [X] T019 [US2] Add the picker to `features/gallery/presentation/viewmodel/GalleryViewModel.kt`: `openTagPhoto`, load/retry, query, toggle, save (single), 404 reload, close on access loss, busy dismiss
- [X] T020 [US2] Create `PeoplePickerSheet` in `features/gallery/presentation/components/GalleryTagComponents.kt` and render it from `GalleryDialogHost` in `features/gallery/presentation/components/GalleryManageComponents.kt` (new dialog actions)
- [X] T021 [US2] Add "Marcar pessoas" to the viewer menu and the sheet in `features/gallery/presentation/screens/PhotoScreen.kt`; wire dialog actions in `features/gallery/presentation/screens/GalleryScreen.kt`
- [X] T022 [P] [US2] Tests in `features/gallery/presentation/viewmodel/TagTextsTest.kt` and `features/gallery/presentation/viewmodel/GalleryViewModelTagsTest.kt` (pre-checked first, full set sent, unchanged sends nothing, 404 syncs + message + reload)

## Phase 5: User Story 3 — Filter the gallery by people (P1)

**Goal**: any member filters by people (AND) and pages through the result.

**Independent test**: filter by A, then A+B; open a result and swipe; untag and see it leave.

- [X] T023 [US3] Add `ViewerSource` and `GalleryMessage.PhotoLeftResult` to `features/gallery/presentation/state/GalleryUiState.kt`; make viewers keyed by source in `features/gallery/presentation/viewmodel/GalleryViewModel.kt` (leave handling, retagged skip, picker close)
- [X] T024 [P] [US3] Create `PeopleUiState`, `PersonRow` in `features/gallery/presentation/state/PeopleUiState.kt`
- [X] T025 [US3] Create `PeopleViewModel` (filter and mine modes) in `features/gallery/presentation/viewmodel/PeopleViewModel.kt`
- [X] T026 [US3] Create `PeopleScreen` + `PeopleContent` in `features/gallery/presentation/screens/PeopleScreen.kt`
- [X] T027 [US3] Add `PEOPLE`, `PEOPLE_PHOTO` routes and `toPeople`, `toMyPhotos`, `toPeoplePhoto` to `features/gallery/presentation/navigation/GalleryNavGraph.kt`; `PhotoScreen` takes a `ViewerSource` and uses the photo's album for its actions
- [X] T028 [US3] Add `showPeople` and the "Pessoas" icon in `features/gallery/presentation/state/GalleryUiState.kt` and `features/gallery/presentation/screens/GalleryScreen.kt`
- [X] T029 [P] [US3] Tests: `features/gallery/presentation/viewmodel/PeopleViewModelTest.kt` (list, search, AND results, hint, empty, vanished person) and result viewer paging + leave in `features/gallery/presentation/viewmodel/GalleryViewModelTagsTest.kt`

## Phase 6: User Story 4 — Minhas fotos (P2)

**Goal**: linked members open their photos in one tap.

**Independent test**: linked vs. unlinked account on the gallery root.

- [X] T030 [US4] Feed `ObserveOwnMemberIdUseCase` into `GalleryViewModel` (`ownMemberId`, `showMyPhotos`) and add the "Minhas fotos" entry to `features/gallery/presentation/screens/GalleryScreen.kt`
- [X] T031 [P] [US4] Tests: entry follows the member id live in `features/gallery/presentation/viewmodel/GalleryViewModelTagsTest.kt`; mine mode results, empty text and close in `features/gallery/presentation/viewmodel/PeopleViewModelTest.kt`

## Phase 7: User Story 5 — Tag people in many photos (P2)

**Goal**: `manage` users add or remove people in a selection.

**Independent test**: select photos, add then remove a person; more than 200 are split.

- [X] T032 [US5] Add `canRemovePeople` to `AlbumUiState` and `openBulkTag(albumId, remove)` + bulk save (chunks, one sync, selection ends on success, partial message) to `features/gallery/presentation/viewmodel/GalleryViewModel.kt`
- [X] T033 [US5] Add "Pessoas" with "Adicionar pessoas"/"Remover pessoas" to `SelectionActions` in `features/gallery/presentation/components/GalleryManageComponents.kt` and wire it in `features/gallery/presentation/screens/AlbumScreen.kt`
- [X] T034 [P] [US5] Tests in `features/gallery/presentation/viewmodel/GalleryViewModelTagsTest.kt` (remove list from selection, add/remove calls, selection ends, partial result keeps selection)

## Phase 8: Polish & Cross-Cutting

- [X] T035 Update `specs/gallery/spec.md` (details sheet, tagging, bulk tagging, filter, "Minhas fotos", member-id port) and the `members` doc in `features/gallery/domain/model/GalleryPhoto.kt` and `features/gallery/data/dto/GalleryPhotoDto.kt`
- [X] T036 Build and run every unit test: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest -q`; add "Implementation Notes" to `specs/010-gallery-member-tags/plan.md` for any deviation

## Dependencies

- Phase 2 blocks everything. T003 needs T001–T002; T007 needs T004–T006; T009 needs T007–T008; T010 needs T002, T007, T009.
- US1 (T014–T016) first: US2 and US3 extend `ViewerPhoto` and `PhotoScreen`.
- US2 (picker) before US5 (bulk reuses the picker).
- US3 before US4 (Minhas fotos reuses `PeopleViewModel`/`PeopleScreen` and the result viewer).
- Polish last.

## Parallel Examples

- Phase 2: T001, T002, T004, T005, T006, T008 touch different files.
- US2: T017 alongside T018.
- US3: T024 alongside T023.
- Test tasks marked [P] run once their story's code exists.

## Implementation Strategy

MVP = Phase 2 + US1 (people visible). Then US2 (tag one), US3 (filter), US4 (Minhas fotos), US5 (bulk). Everything
ships together in one delivery on `dev`, with `specs/gallery/spec.md` in the same commit as the code.
