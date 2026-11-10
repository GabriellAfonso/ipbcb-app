# Tasks: Trim a Video Before Uploading It to the Gallery

**Input**: Design documents from `specs/022-gallery-video-trim/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/upload-enqueue.md

**Tests**: Requested by SC-007 (every rule outside the UI has a unit test).

**Paths**: `G` = `app/src/main/java/com/ipb/castelobranco/features/gallery`, `T` =
`app/src/test/java/com/ipb/castelobranco/features/gallery`.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [X] T001 Add `androidx-media3-transformer` (version ref `media3`) to `gradle/libs.versions.toml` and `implementation` it in `app/build.gradle.kts`

## Phase 2: Foundational

- [X] T002 Add `TrimmableVideo`, `PickedMedia.trimmable` and `UploadEntry` in `G/domain/upload/UploadModels.kt`
- [X] T003 Add `PickedVideoInfo` and `VideoUploadCheck.canTrim` in `G/domain/upload/VideoUploadCheck.kt`
- [X] T004 Add `VideoTrimmer`, `TrimmedFile`, `TrimOutcome`, `TrimVideoUseCase` in `G/domain/upload/VideoTrim.kt`, and `trimVideo` in `G/domain/manage/GalleryManageUseCases.kt`
- [X] T005 Add the pure `UploadBatch` in `G/domain/upload/UploadBatch.kt`
- [X] T006 Change `enqueue` to take `List<UploadEntry>` and add `sweepOrphanCuts` in `G/domain/upload/GalleryUploadRepository.kt` and `G/domain/upload/UploadUseCases.kt`
- [X] T007 Add `newCutFileName` and `cutFiles` in `G/data/local/GalleryMediaStore.kt`
- [X] T008 Add `PickedVideoReader` in `G/data/upload/VideoProbe.kt`, `AndroidPickedVideoReader` in `G/data/upload/AndroidPickedVideoReader.kt`, shared track and date readers in `G/data/upload/AndroidVideoProbe.kt`, and `videoDisplayName` in `G/data/upload/PickedImageCopier.kt`
- [X] T009 Implement entries, `inspect` with `trimmable`, and the orphan sweep in `G/data/upload/GalleryUploadRepositoryImpl.kt`
- [X] T010 Bind reader, trimmer and the sweep `Preloadable` in `G/di/GalleryModule.kt`
- [X] T011 [P] Update `FakeUploadRepository` and `manageUseCases`, add `FakeVideoTrimmer`, in `T/presentation/viewmodel/GalleryManageFixtures.kt`
- [X] T012 [P] Test `canTrim` in `T/domain/upload/VideoUploadCheckTest.kt`
- [X] T013 [P] Test `UploadBatch` in `T/domain/upload/UploadBatchTest.kt`
- [X] T014 [P] Test entries, date from the original, no second copy, refused cut, and the sweep in `T/data/upload/GalleryUploadRepositoryImplTest.kt`

## Phase 3: User Story 1 - Cut a video (P1) MVP

**Goal**: Cut one video and queue only that part. **Independent test**: pick one MP4, move both handles, confirm.

- [X] T015 [US1] Implement `Media3VideoTrimmer` in `G/data/upload/Media3VideoTrimmer.kt`
- [X] T016 [US1] Add the texts in `G/presentation/viewmodel/VideoTexts.kt`
- [X] T017 [US1] Add `VideoTrimUiState` in `G/presentation/state/VideoTrimUiState.kt`
- [X] T018 [US1] Implement `VideoTrimController` in `G/presentation/viewmodel/VideoTrimController.kt`
- [X] T019 [US1] Integrate the controller and the open-editor event in `G/presentation/viewmodel/GalleryViewModel.kt`
- [X] T020 [US1] Add the player and timeline in `G/presentation/components/VideoTrimComponents.kt`
- [X] T021 [US1] Add `VideoTrimScreen` and `VideoTrimContent` in `G/presentation/screens/VideoTrimScreen.kt`
- [X] T022 [US1] Add the route in `G/presentation/navigation/GalleryNavGraph.kt` and navigate from `G/presentation/screens/AlbumScreen.kt`
- [X] T023 [P] [US1] Test selection, labels, cut, cancel and failure in `T/presentation/viewmodel/VideoTrimControllerTest.kt`
- [X] T024 [P] [US1] Test name, size and failure outcomes in `T/domain/upload/TrimVideoUseCaseTest.kt`

## Phase 4: User Story 2 - Over 100 MB (P1)

**Goal**: A video too big to send can be trimmed. **Independent test**: pick an MP4 over 100 MB.

- [X] T025 [US2] Apply `canUseWhole`, `canCut` and `tooBigMessage` in `G/presentation/state/VideoTrimUiState.kt` and show them in `G/presentation/screens/VideoTrimScreen.kt`
- [X] T026 [US2] Keep the person in the editor on `TrimOutcome.TooLarge` in `G/presentation/viewmodel/VideoTrimController.kt`
- [X] T027 [P] [US2] Test the limit rules in `T/presentation/viewmodel/VideoTrimControllerTest.kt`

## Phase 5: User Story 3 - Mixed batch (P2)

**Goal**: Decide a batch and keep picked order. **Independent test**: photos and three videos in mixed order.

- [X] T028 [US3] Implement position, "Enviar todos sem cortar" and discard in `G/presentation/viewmodel/VideoTrimController.kt`
- [X] T029 [US3] Show them in `G/presentation/screens/VideoTrimScreen.kt`, with the batch-gone exit
- [X] T030 [P] [US3] Test batches, discard and the cut ending while the question is open in `T/presentation/viewmodel/VideoTrimControllerTest.kt`
- [X] T031 [P] [US3] Replace the video batch tests in `T/presentation/viewmodel/GalleryViewModelVideoTest.kt`

## Phase 6: User Story 4 - Mobile-data question (P2)

**Goal**: The question counts what is really sent. **Independent test**: off WiFi, cut one, accept one whole.

- [X] T032 [US4] Ask after the last decision, with entries in `G/presentation/state/GalleryDialogState.kt` and `G/presentation/viewmodel/GalleryViewModel.kt`
- [X] T033 [P] [US4] Test the question in `T/presentation/viewmodel/GalleryViewModelVideoTest.kt`

## Phase 7: Polish

- [X] T034 Update `specs/017-gallery-videos/spec.md` (context, decisions 14 and 15, FR-022, FR-024, FR-025, out of scope) and `specs/gallery/spec.md` (sections 1, 2.4, 8.7, 11.6)
- [X] T035 Run `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"` and `:app:compileDebugKotlin`
- [ ] T036 Validate on real phones per `specs/022-gallery-video-trim/quickstart.md` (done by the user)

## Phase 8: Follow-up after the first device test

- [X] T037 Type the start or end time (FR-011a): `TrimTime` in `G/presentation/state/TrimTime.kt`, `TrimTimeEditor` in
  `G/presentation/state/VideoTrimUiState.kt`, `editTime`/`typeTime`/`applyTime`/`closeTimeEditor` in
  `G/presentation/viewmodel/VideoTrimController.kt`, the tappable times and dialog in
  `G/presentation/screens/VideoTrimScreen.kt`
- [X] T038 Knife handles with knobs, a frame along the selection, and dragging the selection whole (FR-011b) in
  `G/presentation/components/VideoTrimComponents.kt` and `moveSelection` in `G/presentation/viewmodel/VideoTrimController.kt`
- [X] T039 Viewer: the bottom bar steps up over the player's seek bar while it shows (017 FR-018a) in
  `G/presentation/components/GalleryVideoPlayer.kt` and `G/presentation/screens/PhotoScreen.kt`
- [X] T040 [P] Test `TrimTime` in `T/presentation/state/TrimTimeTest.kt` and typed times in
  `T/presentation/viewmodel/VideoTrimControllerTest.kt`

## Dependencies

Setup → Foundational → US1 → US2, US3 → US4 → Polish. US2 to US4 extend the screen and controller of US1.

## Parallel Example

T012, T013 and T014 touch different test files and can run together after T009.

## Implementation Strategy

MVP is US1. The controller holds every rule from the start, so later phases mostly add UI and tests.
