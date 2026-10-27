# Implementation Plan: Bible Videos and Reading Progress

**Branch**: `019-bible-videos` | **Date**: 2026-10-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/019-bible-videos/spec.md`

## Summary

Two new data paths inside `features/bible`, both wired into the existing `bibleGraph`:

- **Videos**: `BibleVideoRepositoryImpl` extends `BaseSnapshotRepository` over `GET api/bible/videos/` (authless,
  ETag, snapshot key `bible_videos`), preloaded at boot and refreshed when the graph opens and after every write.
  Writes (`POST`, `PUT places`, `DELETE place`, `DELETE`, `PUT order`) go through an authed API and return
  `Result<…>` with `AppError`. Pure domain helpers build the sheet sections (chapter first, no repetition).
- **Progress**: `BibleProgressRepositoryImpl` keeps the server state (snapshot key `bible_progress`) and a queue of
  pending states (`BibleProgressQueueStore`, snapshot key `bible_progress_queue`, one `Mutex`, last state per
  target) and exposes the merge as a `StateFlow<BibleProgress>`. `BibleProgressSyncWorker` drains the queue in
  chunks of 500 (`POST progress/sync/`), removing exactly the items sent, then refetches. A `Refreshable` fetches
  at boot and after login; a `SessionScopedCache` tries one flush and clears both files at logout.

UI: the reader toolbar gains the play icon; a `ModalBottomSheet` (`BibleVideosSheet`) lists the sections, with
manager actions (add, menu, reorder) and the watched check; a new screen `BibleVideoPlaces` edits places; the
verse column gets the end-of-chapter button; the index shows checks, counters and the long-press book dialog.
`bibleGraph` receives `isLoggedIn` from `AppNavHost` (same as `galleryGraph`). Decisions in
[research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (JVM 17), Android, Jetpack Compose (Material3)

**Primary Dependencies**: Hilt, Retrofit + kotlinx.serialization, WorkManager (+ hilt-work), Coil 2,
`sh.calvin.reorderable` 3.1.0 (all present; no new library)

**Storage**: `SnapshotStorage` (`filesDir/snapshots`) — `bible_videos` (+ ETag), `bible_progress`,
`bible_progress_queue`

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + turbine; fakes preferred

**Target Platform**: Android (minSdk 24, desugaring on)

**Project Type**: mobile app, single `:app` module

**Performance Goals**: play icon and marks derived from in-memory state; no network on the reading path.

**Constraints**:
- `features/bible` must not import other features; login state enters as a parameter (Pitfall #1);
- `domain/` stays Android-free (sections, progress merge, change collapse are pure and JVM-tested);
- Global `Json` has `explicitNulls = false`: the whole-book `chapter: null` in write bodies is built with
  `buildJsonObject` + `JsonNull` so it is sent;
- 120-char lines; Portuguese UI strings; no magic strings.

**Scale/Scope**: ~a few hundred videos; ≤ 1 189 chapter targets + videos in the queue.

## Constitution Check

| Rule (`specs/constitution.md`, `CLAUDE.md`) | Status |
|---|---|
| Features never import each other | Pass: login state passed into `bibleGraph`; access via `core/domain/access` |
| `domain/` has no Android knowledge | Pass: models, sections, merge in pure Kotlin |
| Errors as `AppError`; parsing only in `ResponseExt` | Pass: repos map with `response.toAppError()`; `error_code` read from `AppError.Server.errorCode` |
| Screen/content split; `StateFlow`; one-shot events | Pass: `BibleVideosViewModel` exposes `StateFlow` + `Channel` events |
| Graph-scoped VM | Pass: `BibleVideosViewModel` via `hiltViewModel(graphEntry)`; places screen VM per entry (own args) |
| `safePopBackStack()` | Pass |
| No PII in logs | Pass: only counts and error classes logged |
| Spec and code together | Pass: `specs/bible/spec.md` updated with the code |

No violations.

## Project Structure

### Documentation (this feature)

```text
specs/019-bible-videos/
├── plan.md, research.md, data-model.md, quickstart.md
├── contracts/api.md, contracts/ui.md
├── checklists/requirements.md
└── tasks.md
specs/bible/spec.md                                  # domain spec updated with the feature
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/domain/access/Scope.kt                      # + BIBLE_VIDEOS("bible_videos")
├── core/presentation/navigation/AppNavHost.kt       # bibleGraph(navController, isLoggedIn)
├── MyApp.kt                                         # + progress sync enqueue at start
└── features/bible/
    ├── data/api/BibleVideosApi.kt, BibleProgressApi.kt, BibleEndpoints.kt (+ paths)
    ├── data/dto/BibleVideoDtos.kt, BibleProgressDtos.kt, BibleWriteBodies.kt
    ├── data/local/BibleProgressQueueStore.kt, QueuedProgressChange.kt
    ├── data/repository/BibleVideoRepositoryImpl.kt, BibleProgressRepositoryImpl.kt
    ├── data/work/BibleProgressSyncWorker.kt, WorkManagerBibleProgressSyncScheduler.kt
    ├── di/BibleVideosModule.kt                      # APIs, caches, bindings, Preloadable/Refreshable/SessionScopedCache
    ├── domain/model/BibleVideo.kt, BibleProgress.kt
    ├── domain/repository/BibleVideoRepository.kt, BibleProgressRepository.kt
    ├── domain/sync/BibleProgressSyncScheduler.kt
    ├── domain/usecase/SyncBibleProgressUseCase.kt
    └── presentation/
        ├── components/BibleReaderToolbar.kt         # + play icon
        ├── components/BibleVerseColumn.kt           # + footer slot (mark as read)
        ├── components/BibleVideosSheet.kt           # sheet, cards, add dialog, menu, reorder
        ├── screens/BibleReaderScreen.kt             # wires sheet + progress
        ├── screens/BibleIndexScreen.kt              # checks, counters, long-press dialog
        ├── screens/BibleVideoPlacesScreen.kt        # "Locais do vídeo" + picker
        ├── viewmodel/BibleViewModel.kt              # + progress, isSignedIn, mark chapter/book
        ├── viewmodel/BibleVideosViewModel.kt        # sheet + management + watched
        ├── viewmodel/BibleVideoPlacesViewModel.kt   # places draft
        └── navigation/BibleNavGraph.kt              # + places route, isLoggedIn

app/src/test/java/com/ipb/castelobranco/features/bible/
├── domain/model/BibleVideoSectionsTest.kt, BibleProgressTest.kt
├── data/local/BibleProgressQueueStoreTest.kt
├── data/repository/BibleProgressRepositoryImplTest.kt, BibleVideoRepositoryImplTest.kt
└── domain/usecase/SyncBibleProgressUseCaseTest.kt
```

**Structure Decision**: all inside `features/bible`; only `Scope`, `AppNavHost` and `MyApp` change in core/app.

## Implementation Order

1. Scope + domain models and pure helpers (+ tests).
2. DTOs, APIs, write bodies; video repository (+ tests).
3. Progress queue store, progress repository, sync use case, worker, scheduler, DI (+ tests).
4. ViewModels.
5. UI: toolbar icon, sheet, places screen, nav; end-of-chapter button; index marks.
6. `specs/bible/spec.md`, build, unit tests.

## Complexity Tracking

None.
