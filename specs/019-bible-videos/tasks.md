# Tasks: Bible Videos and Reading Progress

**Input**: Design documents from `specs/019-bible-videos/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: required by `CLAUDE.md` (happy path + one error per use case); fakes preferred.

Paths are relative to `app/src/main/java/com/ipb/castelobranco/` (main) and `app/src/test/java/com/ipb/castelobranco/`
(test) unless stated.

## Phase 1: Setup

- [X] T001 Add `BIBLE_VIDEOS("bible_videos")` to `core/domain/access/Scope.kt`
- [X] T002 Add video and progress paths to `features/bible/data/api/BibleEndpoints.kt`

## Phase 2: Foundational

- [X] T003 [P] Domain models `BibleVideoKind`, `BiblePlace`, `BibleVideoPlace`, `BibleVideo`, `BibleVideoSections`, `sectionsFor` in `features/bible/domain/model/BibleVideo.kt`
- [X] T004 [P] Domain models `BibleProgress`, `ProgressTarget`, `ProgressChange`, `applying` in `features/bible/domain/model/BibleProgress.kt`
- [X] T005 [P] DTOs + mappers in `features/bible/data/dto/BibleVideoDtos.kt` and `features/bible/data/dto/BibleProgressDtos.kt`; JSON write bodies (explicit `JsonNull`) in `features/bible/data/dto/BibleWriteBodies.kt`
- [X] T006 [P] Retrofit interfaces `BibleVideosApi` (public GET + authed writes) and `BibleProgressApi` in `features/bible/data/api/`
- [X] T007 Repository interfaces `BibleVideoRepository`, `BibleProgressRepository` in `features/bible/domain/repository/` and `BibleProgressSyncScheduler` in `features/bible/domain/sync/`
- [X] T008 Hilt module `features/bible/di/BibleVideosModule.kt` (APIs with qualifiers, snapshot caches, bindings, `Preloadable`, `Refreshable`, `SessionScopedCache`)
- [X] T009 Pass `isLoggedIn` into `bibleGraph` from `core/presentation/navigation/AppNavHost.kt`; `features/bible/presentation/navigation/BibleNavGraph.kt` signature
- [X] T010 [P] Tests for sections and progress merge in `features/bible/domain/model/BibleVideoSectionsTest.kt` and `BibleProgressTest.kt`

## Phase 3: User Story 1 — member watches a video (P1)

**Goal**: play icon + sheet + open link, signed-in or not, offline from cache.
**Independent test**: cached list with Rm 5 and Rm videos; icon, sections, badge, link.

- [X] T011 [US1] `BibleVideoRepositoryImpl` (BaseSnapshotRepository, authless fetch) in `features/bible/data/repository/BibleVideoRepositoryImpl.kt`
- [X] T012 [US1] `BibleVideosViewModel` (videos, sections for position, access, events) in `features/bible/presentation/viewmodel/BibleVideosViewModel.kt`
- [X] T013 [US1] Play icon in `features/bible/presentation/components/BibleReaderToolbar.kt`
- [X] T014 [US1] `BibleVideosSheet` + `BibleVideoCard` in `features/bible/presentation/components/BibleVideosSheet.kt`
- [X] T015 [US1] Wire sheet + `ACTION_VIEW` in `features/bible/presentation/screens/BibleReaderScreen.kt` and nav graph

## Phase 4: User Story 4 — member marks chapters (P1)

**Goal**: end-of-chapter button, index checks/counters, offline queue and sync.
**Independent test**: mark online/offline; index; server receives.

- [X] T016 [P] [US4] `QueuedProgressChange` + `BibleProgressQueueStore` in `features/bible/data/local/`
- [X] T017 [US4] `BibleProgressRepositoryImpl` (server cache, queue merge, refresh, syncOnce, flushAndClear) in `features/bible/data/repository/BibleProgressRepositoryImpl.kt`
- [X] T018 [US4] `SyncBibleProgressUseCase` in `features/bible/domain/usecase/SyncBibleProgressUseCase.kt`
- [X] T019 [US4] `BibleProgressSyncWorker` + `WorkManagerBibleProgressSyncScheduler` in `features/bible/data/work/`; enqueue in `MyApp.kt`
- [X] T020 [US4] `BibleViewModel`: progress, `setSignedIn`, `toggleChapterRead`, refresh on init in `features/bible/presentation/viewmodel/BibleViewModel.kt`
- [X] T021 [US4] Footer slot in `BibleVerseColumn.kt` + "Marcar como lido"/"Lido ✓" in `BibleReaderScreen.kt`
- [X] T022 [US4] Index: chapter cell check, book counter in `features/bible/presentation/screens/BibleIndexScreen.kt`
- [X] T023 [P] [US4] Tests: `features/bible/data/local/BibleProgressQueueStoreTest.kt`, `features/bible/data/repository/BibleProgressRepositoryImplTest.kt`, `features/bible/domain/usecase/SyncBibleProgressUseCaseTest.kt`

## Phase 5: User Story 2 — leader adds a video and its places (P1)

**Goal**: add dialog, 409 handling, "Locais do vídeo" screen with picker.
**Independent test**: as Liderança add, 409, edit places across books.

- [X] T024 [US2] Writes in `BibleVideoRepositoryImpl` (add with 409 decoding, setPlaces) + `AddVideoResult`
- [X] T025 [US2] Add dialog + manager empty state in `BibleVideosSheet.kt`; add flow in `BibleVideosViewModel.kt`
- [X] T026 [US2] `BibleVideoPlacesViewModel` in `features/bible/presentation/viewmodel/BibleVideoPlacesViewModel.kt`
- [X] T027 [US2] `BibleVideoPlacesScreen` + picker in `features/bible/presentation/screens/BibleVideoPlacesScreen.kt`; route in `BibleNavGraph.kt`
- [X] T028 [P] [US2] Tests: `features/bible/data/repository/BibleVideoRepositoryImplTest.kt` (add ok, 409, invalid URL)

## Phase 6: User Story 3 — leader orders and removes (P2)

- [X] T029 [US3] Repository: removePlace, delete, reorder in `BibleVideoRepositoryImpl.kt`
- [X] T030 [US3] Card menu, delete confirmation, reorder mode in `BibleVideosSheet.kt` + VM actions

## Phase 7: User Story 5 — mark a whole book (P2)

- [X] T031 [US5] `BibleViewModel.setBookRead` (only changing chapters) and long-press + confirmation dialog in `BibleIndexScreen.kt`

## Phase 8: User Story 6 — video watched (P3)

- [X] T032 [US6] Watched check, "Visto" badge, dim on the card; `toggleWatched` in `BibleVideosViewModel.kt`

## Phase 9: Polish

- [X] T033 Update `specs/bible/spec.md` (videos, progress, screens, storage)
- [X] T034 Build and run `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.bible.*"`

## Dependencies

Setup → Foundational → US1 → US4 → US2 → US3 → US5 → US6 → Polish. US4 depends only on Foundational; US2/US3
depend on US1 (sheet); US5 on US4; US6 on US1 + US4.

## Parallel examples

- T003, T004, T005, T006 (different files).
- T016 alongside T011.

## Implementation strategy

MVP = US1 + US4 (watch videos, mark chapters). Then management (US2, US3), then whole book and watched.
