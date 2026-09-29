# Implementation Plan: Gallery Album Tree and Sync

**Branch**: `007-gallery-album-sync` | **Date**: 2026-09-29 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/007-gallery-album-sync/spec.md`

## Summary

The gallery stops being "download everything once, derive albums from folders" and becomes a local copy of the
server's gallery kept current by the backend change feed. One JSON snapshot holds the index (albums, photos, cursor);
originals live flat by photo id; covers are downloaded during sync; previews load over the network for photos not yet
downloaded. Screens are rebuilt around a UiState per screen and the album tree.

Key technical choices (details in [research.md](research.md)):

- **Index + cursor in one snapshot** (R1), `SnapshotCacheFactory`, key `gallery_index`.
- **Flat originals, covers by URL hash** (R2) in a new `GalleryMediaStore`.
- **Reconcile disk ⊆ index after every sync** (R3) — deletions, full sync, migration orphans and covers share one path.
- **Migration = file move + marker** (R4); pruning falls out of R3.
- **`GallerySyncer` with a `Mutex`**: skip when busy, cancellable by logout, session checked before every write (R5).
- **Coil loader on `@Client` with disk cache** for previews (R6).
- **Two works** (R7): periodic sync (any network, 6 h) + existing download worker (WiFi), now fed by the index.
- **Foreground via `LifecycleEventEffect(ON_START)`** in `AppNavHost` (R8) — no new library.
- **Routes by id** (R9): `Album/{albumId}`, `Photo/{albumId}/{photoId}`; removed items unwind the stack.
- **Graph-scoped ViewModel with per-key state functions** (R10); pure `GalleryTree` in domain.

## Technical Context

**Language/Version**: Kotlin (JVM 17 target), Android, `minSdk 24`

**Primary Dependencies**: Jetpack Compose (Material 3), Hilt (+ `hilt-work`), WorkManager 2.10, Retrofit + OkHttp
(`@AuthedRetrofit`, `@Client`), Coil 2.6, kotlinx.serialization, kotlinx.coroutines, `lifecycle-runtime-compose` —
all already in the project; **no new libraries**

**Storage**: snapshot `filesDir/snapshots/gallery_index.json`; originals `filesDir/gallery/photos/`; covers
`filesDir/gallery/covers/`; previews `cacheDir/gallery_thumbs/` (Coil disk cache, 100 MB); marker in
`GalleryPreferences` (`@SettingsPrefs` DataStore)

**Testing**: JUnit4 + MockK + kotlinx-coroutines-test + Turbine; fakes preferred (`FakeGalleryApi` extended,
`tempDirContext` for file tests, in-memory `SnapshotCache` fake)

**Target Platform**: Android phone, single `:app` module

**Project Type**: mobile-app

**Performance Goals**: gallery and album screens render from memory in < 1 s with 3,000 photos (SC-008); a delta
sync is one small JSON request

**Constraints**: offline-readable after the first sync; originals WiFi-first; media only with the member's JWT;
nothing of the gallery survives logout; features never import each other; Portuguese UI strings hardcoded; 120-char
lines

**Scale/Scope**: ~200 photos today, ≤3,000 in ten years; ~15 new production files, ~12 touched, ~9 test classes;
`specs/gallery/spec.md` rewritten

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` is the unfilled template; the rules are `specs/constitution.md` and `CLAUDE.md`.

### `specs/constitution.md` — error handling

| Rule | Status |
|------|--------|
| `AppError` is the only error crossing layers | ✅ `GallerySyncResult.Failed(AppError)`; worker and ViewModel never see `HttpException`/`IOException` |
| Conversion in the data layer, via `toAppError()` | ✅ `GallerySyncer` converts feed responses with `Response.toAppError()` and exceptions with `Throwable.toAppError()` |
| `message` technical, `userMessage` for screen | ✅ screen texts from `toUserMessage()` or authored gallery constants; no raw body shown |
| One HTTP error parsing point | ✅ only `ResponseExt.kt` |
| 403 is permission, not login | ✅ 403 never shows the login button (unchanged rule) |

### `CLAUDE.md`

| Rule | Status |
|------|--------|
| UI → ViewModel → UseCase → Repository | ✅ `SyncGalleryUseCase`, `GalleryAutoDownloadUseCase`; screens only know `GalleryViewModel` |
| Features don't import each other | ✅ everything under `features/gallery`; `CoreViewModel` already depends on `GalleryAutoDownloadUseCase` (existing exception, kept; only a new `SyncGalleryUseCase` is added next to it) |
| `domain/` without Android | ✅ `GalleryIndex`, `GalleryTree`, scheduler interfaces; `java.io.File` already used in domain today — kept to `GalleryLocalState` |
| Screen collects `StateFlow<UiState>`; dumb content composable | ✅ R10; fixes the current violation (content composables receiving the ViewModel) |
| Loading / success / error on every screen | ✅ `GalleryRootUiState`, `AlbumUiState`, `PhotoViewerUiState` |
| One-time events via `SharedFlow<UiEvent>` | ⚠️ `GalleryMessage` is a consumable `StateFlow<GalleryMessage?>` instead: during the stack unwind of R9 no screen may be collecting when the event fires, and a `SharedFlow` would drop it. Consumed once by the top screen |
| Graph-scoped ViewModel | ✅ `hiltViewModel(graphEntry)`, `isLoggedIn` passed as parameter (Pitfall #1) |
| `safePopBackStack()` | ✅ `GalleryNav.back` |
| Snapshot cache pattern | ✅ `GallerySnapshotModule` (Pitfall #3) |
| `@Client` / `@AuthedRetrofit` qualifiers | ✅ feed and files on `@AuthedRetrofit`; preview loader on `@Client` |
| No hardcoded secrets, no PII logs | ✅ member names in `members` never logged |
| Tests: happy path + 1 error per use case, fakes | ✅ see quickstart list |
| Spec and code in the same commit | ✅ `specs/gallery/spec.md` rewritten with the code |

**Gate: PASS.** Post-design re-check: PASS — no new library, no cross-feature import, no manual `CoroutineScope`
(R5 keeps syncs in caller scopes).

## Project Structure

### Documentation (this feature)

```text
specs/007-gallery-album-sync/
├── spec.md
├── plan.md              # this file
├── research.md          # R1–R11
├── data-model.md
├── quickstart.md
├── contracts/
│   └── gallery-client.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
app/src/main/java/com/ipb/castelobranco/
├── core/presentation/navigation/AppNavHost.kt          # ON_START → coreViewModel.onAppForeground()
├── core/presentation/viewmodel/CoreViewModel.kt        # onAppForeground(); login → sync + periodic; drop triggerIfNeeded
└── features/gallery/
    ├── data/
    │   ├── api/GalleryApi.kt, GalleryEndpoints.kt      # + getChanges(since); − getAllPhotos, getAlbumPhotos
    │   ├── dto/GalleryAlbumDto.kt (new), GalleryPhotoDto.kt (+thumbnail_url, position, members),
    │   │   GalleryChangesDto.kt (new), GalleryMappers.kt (new)
    │   ├── snapshot/GalleryIndexSnapshot.kt (new)
    │   ├── local/GalleryMediaStore.kt (new, replaces GalleryPhotoStorage.kt),
    │   │   GalleryLegacyMigration.kt (new), GalleryPreferences.kt (layout marker)
    │   ├── sync/GallerySyncer.kt (new)                 # lock, feed, apply, save, reconcile, covers
    │   ├── download/                                   # GalleryPhotoDownloader: storage → GalleryMediaStore; rest unchanged
    │   ├── repository/GalleryRepositoryImpl.kt         # localState: StateFlow<GalleryLocalState>, refreshLocalFiles()
    │   └── work/GalleryDownloadWorker.kt (reads index), GallerySyncWorker.kt (new),
    │       WorkManagerGalleryDownloadScheduler.kt, WorkManagerGallerySyncScheduler.kt (new)
    ├── di/GalleryModule.kt, GallerySnapshotModule.kt (new), GalleryThumbnailLoaderModule.kt (new)
    ├── domain/
    │   ├── model/GalleryAlbum.kt, GalleryPhoto.kt, GalleryIndex.kt, GalleryTree.kt, GalleryLocalState.kt,
    │   │   GallerySyncResult.kt                        # Album.kt removed
    │   ├── repository/GalleryRepository.kt            # rewritten
    │   ├── sync/GallerySyncScheduler.kt (new)
    │   └── usecase/SyncGalleryUseCase.kt (new), GalleryAutoDownloadUseCase.kt (logout, buttons);
    │       DownloadAllPhotosUseCase.kt removed
    └── presentation/
        ├── state/GalleryRootUiState.kt, AlbumUiState.kt, PhotoViewerUiState.kt, PhotoImage.kt, GalleryMessage.kt
        ├── viewmodel/GalleryViewModel.kt               # rootState, albumState(id), viewerState(albumId, photoId)
        ├── navigation/GalleryNavGraph.kt               # routes by id
        ├── components/GalleryComponents.kt             # AlbumTile (cover/black), PhotoTile (PhotoImage)
        └── screens/GalleryScreen.kt, AlbumScreen.kt, PhotoScreen.kt   # Screen + Content split

app/src/test/java/com/ipb/castelobranco/features/gallery/
├── domain/model/GalleryIndexTest.kt, GalleryTreeTest.kt
├── data/sync/GallerySyncerTest.kt, data/local/GalleryMediaStoreTest.kt, GalleryLegacyMigrationTest.kt
├── data/work/ (download worker reads index), data/download/ (existing, adapted)
├── domain/usecase/GalleryAutoDownloadUseCaseTest.kt (logout), SyncGalleryUseCaseTest.kt
└── presentation/GalleryViewModelTest.kt

specs/gallery/spec.md                                    # rewritten to the final state
```

**Structure Decision**: everything stays in `features/gallery` with the project's data/domain/presentation split;
the only core changes are the foreground hook in `AppNavHost` and the gallery calls in `CoreViewModel`.

## Implementation Order

1. **Domain model + tree** (`GalleryIndex`, `GalleryTree`, DTOs, mappers) — pure, test first.
2. **Storage**: `GalleryMediaStore`, snapshot module, `GalleryLegacyMigration`.
3. **`GallerySyncer`** (feed, apply, save, reconcile, covers, lock, clear) + `SyncGalleryUseCase`.
4. **Repository** `localState`; download worker fed by the index; `GalleryPhotoDownloader` on the new store.
5. **Scheduling**: `GallerySyncWorker` + scheduler; triggers in `CoreViewModel` / `AppNavHost`; logout path.
6. **Preview loader** module + logout clear.
7. **Presentation**: UiStates, ViewModel, routes, three screens (Screen/Content split), messages.
8. **Spec rewrite** `specs/gallery/spec.md`; remove dead code (`GalleryPhotoStorage`, `Album`, `DownloadAllPhotosUseCase`,
   `getAllPhotos`, `getAlbumPhotos`, `gallery_auto_download_triggered`).

## Risks

| Risk | Mitigation |
|------|-----------|
| `JsonSnapshotStorage.save` is not atomic (plain `writeText`) | Cursor inside the snapshot: a torn file decodes as "no index" → full read; files survive (R1) |
| Reconcile deletes an original the download worker just saved for a photo deleted meanwhile | Correct outcome — the photo is gone from the server |
| Many `ON_START` events (rotation, quick app switches) | Sync skips when busy; delta answer is tiny |
| Old `FAILED` download `WorkInfo` from before the update | `KEEP` on a finished unique work replaces it |
| Covers on mobile data | Few (one per album with a cover), 1000×1000 JPEG, downloaded once per URL |

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Removal message as consumable `StateFlow` instead of `SharedFlow` (CLAUDE.md, ViewModel) | The message must survive the back-stack unwind when an open album is deleted (R9) | A `SharedFlow` without replay is lost while screens switch; with replay it would be shown again on every re-collection |
