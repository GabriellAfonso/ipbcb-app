# Tasks: Gallery Trash

**Input**: Design documents from `specs/009-gallery-trash/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/gallery-trash-client.md, quickstart.md

**Tests**: requested by the spec (request's test list, SC-005).

Paths below are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and
`app/src/test/java/com/ipb/castelobranco/` (test) unless they start with `specs/`.

## Phase 1: Setup

No setup: no new library, no manifest change.

## Phase 2: Foundational (blocking for every story)

- [X] T001 [P] Add `TRASH`, `TRASH_ALBUM_RESTORE`, `TRASH_PHOTO_RESTORE` to `features/gallery/data/api/GalleryEndpoints.kt` and `getTrash`, `restoreAlbum`, `restorePhoto` to `features/gallery/data/api/GalleryApi.kt` (contract §1)
- [X] T002 [P] Create `GalleryTrashEntryDto` in `features/gallery/data/dto/GalleryTrashEntryDto.kt` and `toDomain()` (drops unknown kinds) in `features/gallery/data/dto/GalleryMappers.kt`
- [X] T003 [P] Create `TrashKind`, `TrashKey`, `TrashEntry`, `RestoreResult` in `features/gallery/domain/trash/TrashEntry.kt` (data-model)
- [X] T004 Add `trash()`, `restoreAlbum(id)`, `restorePhoto(id)` to `features/gallery/domain/manage/GalleryManageRepository.kt` and implement them in `features/gallery/data/manage/GalleryManageRepositoryImpl.kt` (restore = `write` with `UpsertAlbum`/`UpsertPhoto`)
- [X] T005 [P] Add `trashedParentId()` and `conflictingAlbumId()` (extras → `Long?`) to `features/gallery/domain/manage/GalleryWriteErrorKinds.kt`
- [X] T006 Create `LoadTrashUseCase` and `RestoreTrashItemUseCase` (classification + sync when the conflicting album is missing) in `features/gallery/domain/trash/TrashUseCases.kt`; add `restore` to `features/gallery/domain/manage/GalleryManageUseCases.kt`
- [X] T007 Extend test fakes: `features/gallery/data/api/FakeGalleryApi.kt` (trash answers), `features/gallery/domain/manage/FakeGalleryManageRepository.kt` (trash list, restore results), `features/gallery/presentation/viewmodel/GalleryManageFixtures.kt` (`restore` in `manageUseCases`)
- [X] T008 [P] Repository tests (restore applies locally, cursor unchanged, syncs; list mapping; 404 syncs) in `features/gallery/data/manage/GalleryManageRepositoryImplTest.kt`
- [X] T009 [P] Use case tests (each `RestoreResult`, conflict sync when missing) in `features/gallery/domain/trash/RestoreTrashItemUseCaseTest.kt`

## Phase 3: User Story 1 — See what is in the trash (P1) 🎯 MVP

**Goal**: owners open the trash from the root and see every entry with its texts.

**Independent test**: open the trash as owner with an album and a photo in it; non-owner has no icon.

- [X] T010 [P] [US1] Create `TrashRow`, `TrashUiState`, `TrashEvent` in `features/gallery/presentation/state/TrashUiState.kt`
- [X] T011 [P] [US1] Create `TrashTexts` (row mapping with `ZoneId`, counts, dates, result texts) in `features/gallery/presentation/viewmodel/TrashTexts.kt`
- [X] T012 [US1] Create `TrashViewModel` (load on init, refresh, error/empty, access loss → `Close`) in `features/gallery/presentation/viewmodel/TrashViewModel.kt`
- [X] T013 [US1] Create `TrashScreen` + `TrashContent` (explanation, rows with preview, pull-to-refresh, loading/error/empty) in `features/gallery/presentation/screens/TrashScreen.kt`
- [X] T014 [US1] Add `GalleryRoutes.TRASH`, `GalleryNav.toTrash` and the trash `composable` in `features/gallery/presentation/navigation/GalleryNavGraph.kt`
- [X] T015 [US1] Add `showTrash` to `GalleryRootUiState` in `features/gallery/presentation/state/GalleryUiState.kt` and the trash icon to `features/gallery/presentation/screens/GalleryScreen.kt`
- [X] T016 [P] [US1] Tests for row texts (null names, counts, singulars, dates, purge) in `features/gallery/presentation/viewmodel/TrashTextsTest.kt`
- [X] T017 [US1] Tests for load/empty/error/refresh/access loss in `features/gallery/presentation/viewmodel/TrashViewModelTest.kt`; `showTrash` follows `canDelete` in `features/gallery/presentation/viewmodel/GalleryViewModelManageTest.kt`

## Phase 4: User Story 2 — Restore an item (P1)

**Goal**: "Restaurar" brings the item back; one at a time; 404 reloads.

- [X] T018 [US2] Add `restore(key)` to `TrashViewModel` (one at a time, row leaves on success, 404 reload + message, other failures keep the list) in `features/gallery/presentation/viewmodel/TrashViewModel.kt`
- [X] T019 [US2] Row progress, disabled buttons and refresh while restoring, snackbar for `TrashEvent.Message` in `features/gallery/presentation/screens/TrashScreen.kt`
- [X] T020 [US2] Tests: success, 404 reload, other error, one restore at a time in `features/gallery/presentation/viewmodel/TrashViewModelTest.kt`

## Phase 5: User Story 3 — Resolve a refused restore (P2)

- [X] T021 [US3] Highlight the listed trashed parent, detail-only when not listed, conflict event with `openAlbumId` in `features/gallery/presentation/viewmodel/TrashViewModel.kt`
- [X] T022 [US3] Scroll to and highlight the row; "Abrir álbum" snackbar action → `nav.toAlbum` in `features/gallery/presentation/screens/TrashScreen.kt`
- [X] T023 [US3] Tests: parent listed / not listed, conflict exposes album in `features/gallery/presentation/viewmodel/TrashViewModelTest.kt`

## Phase 6: User Story 4 — Undo a delete (P2)

- [X] T024 [US4] ~~Add optional `snackbarHost` slot to `core/presentation/base/BaseScreen.kt`~~ — not needed: screens wrap their content in `GalleryMessageHost` with a `SnackbarHost` overlay, like the members screens (see plan Implementation Notes)
- [X] T025 [US4] Add `MessageAction` and `action` to `GalleryMessage`; `AlbumTrashed`/`PhotoTrashed` carry the undo, in `features/gallery/presentation/state/GalleryUiState.kt`
- [X] T026 [US4] `GalleryViewModel`: undo offered for single deletes with `owner`, single-photo selection posts `PhotoTrashed`, `undo(action)` via the restore use case with `TrashTexts`, in `features/gallery/presentation/viewmodel/GalleryViewModel.kt`
- [X] T027 [US4] `GalleryMessageEffect` as snackbar with action (consume first, launch on remembered scope, skipped by leaving screens) and hosts on `GalleryScreen.kt`, `AlbumScreen.kt`, `PhotoScreen.kt` in `features/gallery/presentation/screens/`
- [X] T028 [US4] Tests: undo after single album/photo delete, none after batch, none without owner, undo restore + refusal texts in `features/gallery/presentation/viewmodel/GalleryViewModelManageTest.kt`

## Phase 7: Polish

- [X] T029 Update `specs/gallery/spec.md` (header, §1 screens/messages, §8.1 controls, new trash section)
- [X] T030 Run `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"` and `./gradlew :app:assembleDebug`; fix failures

## Dependencies

- Phase 2 blocks every story. US1 before US2 (same ViewModel/screen). US3 after US2. US4 needs only Phase 2 and T011.
- Parallel: T001/T002/T003/T005; T008/T009; T010/T011/T016; US4 (T024–T025) alongside US2/US3.

## Implementation Strategy

MVP = Phase 2 + US1 (read-only trash). Then US2 (restore), US3 (refusals), US4 (undo), polish.
