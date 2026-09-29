---

description: "Task list for 007-gallery-album-sync"
---

# Tasks: Gallery Album Tree and Sync

**Input**: Design documents from `specs/007-gallery-album-sync/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/gallery-client.md](contracts/gallery-client.md), [quickstart.md](quickstart.md)

**Tests**: Requested by the spec (SC-009 and the request's test list). JUnit4 + MockK + coroutines-test + Turbine,
fakes preferred. Within each story, write the tests first and see them fail.

**Organization**: One phase per user story, in priority order (P1: US1, US2, US6; P2: US3, US4; P3: US5).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: US1…US6 from spec.md

## Path Conventions

- `MAIN` = `app/src/main/java/com/ipb/castelobranco`
- `TEST` = `app/src/test/java/com/ipb/castelobranco`
- `GAL` = `MAIN/features/gallery`, `GALT` = `TEST/features/gallery`
- Run tests: `./gradlew :app:testDebugUnitTest --tests "com.ipb.castelobranco.features.gallery.*"` (never `clean`)

---

## Phase 1: Setup (Wire Types and API)

**Purpose**: The feed and resources as the backend sends them (data-model.md "Wire").

- [X] T001 [P] Create `GalleryAlbumDto` (`id`, `name`, `parent_id`, `description` default `""`, `event_date`, `cover_url`, `cover_source_album_id`, `position`) in `GAL/data/dto/GalleryAlbumDto.kt`
- [X] T002 [P] Create `GalleryPhotoMemberDto` (`id`, `name`) and extend `GalleryPhotoDto` with `thumbnail_url: String? = null`, `position: Int = 0`, `members: List<GalleryPhotoMemberDto> = emptyList()` (keep `fileExtension()`) in `GAL/data/dto/GalleryPhotoDto.kt`
- [X] T003 [P] Create `GalleryChangesDto` (`albums`, `photos`, `deleted_album_ids`, `deleted_photo_ids`, `cursor`, `full_sync_required`) in `GAL/data/dto/GalleryChangesDto.kt`
- [X] T004 Add `CHANGES = "${ApiConstants.BASE_PATH}gallery/changes/"` to `GAL/data/api/GalleryEndpoints.kt` and `suspend fun getChanges(@Query("since") since: String?): Response<GalleryChangesDto>` to `GAL/data/api/GalleryApi.kt` (keep `downloadFile`; `getAllPhotos`/`getAlbumPhotos` are removed in T060)
- [X] T005 Extend the test fake with a scripted `getChanges` (queue of responses, records every `since` received) and a map of URL → bytes/status for `downloadFile` in `GALT/data/api/FakeGalleryApi.kt`; add album/photo/changes builders to `GALT/data/GalleryTestFixtures.kt`

---

## Phase 2: Foundational (Index, Tree, Storage, Sync Core)

**Purpose**: The local copy every story reads. **No user story can start before this phase is done.**

### Tests (write first)

- [X] T006 [P] `GalleryIndexTest` in `GALT/domain/model/GalleryIndexTest.kt`: upsert new and changed album/photo by id; delete ids; same delta applied twice == once; delete wins over upsert of the same id; cursor replaced; `replaceWith` drops every id missing from the full read; photo moved (new `albumId`) keeps its id entry
- [X] T007 [P] `GalleryTreeTest` in `GALT/domain/model/GalleryTreeTest.kt`: roots, children, photos of album, parent; order by `position` then `id` at every level; album whose parent is absent and photo whose album is absent are not returned; `allPhotosInTreeOrder()` is pre-order
- [X] T008 [P] `GalleryMediaStoreTest` in `GALT/data/local/GalleryMediaStoreTest.kt` (port the atomic-write cases of `GalleryPhotoStorageTest`): original saved at `gallery/photos/{id}.{ext}`, truncated stream leaves no final file and no temp in `gallery/`, `listOriginalIds`, cover saved at `gallery/covers/{sha1(url)}.jpg`, `coverFile(url)`, `clearAll()` deletes `gallery/`
- [X] T009 [P] `GallerySyncerTest` (core cases) in `GALT/data/sync/GallerySyncerTest.kt` with an in-memory `SnapshotCache` fake: no index → `getChanges(null)` and index saved with cursor; with index → `getChanges(cursor)` and delta applied; `Synced.missingOriginals` counts photos without an original; `403`, `401` and `IOException` → `Failed(AppError)` and snapshot untouched; a second `sync()` while one is running returns `Skipped`

### Implementation

- [X] T010 [P] Domain models `GalleryAlbum`, `GalleryPhoto`, `GalleryMember` (event date as `LocalDate?`) in `GAL/domain/model/GalleryAlbum.kt` and `GAL/domain/model/GalleryPhoto.kt`
- [X] T011 `GalleryIndex` (maps by id + `cursor`), `GalleryDelta`, `applyDelta`, `replaceWith` — pure Kotlin, per data-model.md — in `GAL/domain/model/GalleryIndex.kt`
- [X] T012 `GalleryTree` built from a `GalleryIndex` (`roots`, `children`, `photosOf`, `parentOf`, `allPhotosInTreeOrder`, comparator `position` then `id`) in `GAL/domain/model/GalleryTree.kt`
- [X] T013 [P] `GalleryLocalState` (`index: GalleryIndex?`, `originals: Map<Long, File>`, `covers: Map<String, File>`) and `GallerySyncResult` (`Synced(missingOriginals)`, `Skipped`, `Failed(AppError)`) and `GallerySyncStatus` (`isRunning`, `lastError`, `hasCompletedOnce`) in `GAL/domain/model/GalleryLocalState.kt` and `GAL/domain/model/GallerySyncResult.kt`
- [X] T014 [P] `GalleryIndexSnapshot` (`albums`, `photos`, `cursor`) in `GAL/data/snapshot/GalleryIndexSnapshot.kt` and DTO ↔ domain mappers (`GalleryChangesDto.toDelta()`, snapshot ↔ `GalleryIndex`) in `GAL/data/dto/GalleryMappers.kt`
- [X] T015 [P] `GallerySnapshotModule` providing `SnapshotCache<GalleryIndexSnapshot>` with key `gallery_index` via `SnapshotCacheFactory` (same shape as `HymnalSnapshotModule`) in `GAL/di/GallerySnapshotModule.kt`
- [X] T016 `GalleryMediaStore` (replaces `GalleryPhotoStorage`; same temp-in-`cacheDir` atomic write; originals flat by id; covers by `sha1(url)`; `saveOriginal`, `originalFile`, `listOriginalIds`, `deleteOriginal`, `saveCover`, `coverFile`, `listCoverNames`, `deleteCover`, `clearAll`) in `GAL/data/local/GalleryMediaStore.kt`; provide it in `GAL/di/GalleryModule.kt`
- [X] T017 `GallerySyncer` core (R5): `Mutex` with `tryLock` → `Skipped`; session check (`AuthSession.isLoggedIn()`) before reading and again before saving; load snapshot; `getChanges(cursor)`; convert non-2xx with `Response.toAppError()` and exceptions with `toAppError()`; apply delta; save snapshot with the new cursor; publish `GalleryLocalState` and `GallerySyncStatus` as `StateFlow`s; keep the running `Job` for `clear()` (T045) in `GAL/data/sync/GallerySyncer.kt`
- [X] T018 Rewrite `GalleryRepository` (`localState: StateFlow<GalleryLocalState>`, `syncStatus: StateFlow<GallerySyncStatus>`, `suspend fun sync(): GallerySyncResult`, `suspend fun refreshLocalFiles()`, `suspend fun clear()`) in `GAL/domain/repository/GalleryRepository.kt` and `GalleryRepositoryImpl` delegating to `GallerySyncer`/`GalleryMediaStore` (load snapshot + file maps on `preload()`) in `GAL/data/repository/GalleryRepositoryImpl.kt`; keep the `Preloadable` binding in `GAL/di/GalleryModule.kt`
- [X] T019 `SyncGalleryUseCase` (`invoke()`: `repository.sync()`; on `Synced(missing > 0)` → `scheduler.enqueueWifiOnly(replaceExisting = false)`) in `GAL/domain/usecase/SyncGalleryUseCase.kt`, with `SyncGalleryUseCaseTest` (happy path enqueues, `Failed` and `Skipped` do not) in `GALT/domain/usecase/SyncGalleryUseCaseTest.kt`

**Checkpoint**: T006–T009 green; a sync fills the index from a fake feed.

---

## Phase 3: User Story 1 — Browse the album tree (Priority: P1) 🎯 MVP

**Goal**: Root, album and viewer screens driven by the index: covers (or black), order by `position` then `id`,
sub-albums above photos, parent subtitle, date and description, back one level, viewer by id.

**Independent Test**: spec US1 — roots B, A; A with A1 and three photos; open, navigate, back twice.

### Tests (write first)

- [X] T020 [P] [US1] Cover cases in `GALT/data/sync/GallerySyncerTest.kt`: cover of every album with `cover_url` downloaded during sync; existing file for the same URL not downloaded again; two albums with the same URL share one file; cover download `404`/`IOException` skipped and sync still `Synced`
- [X] T021 [P] [US1] Rewrite `GALT/presentation/viewmodel/GalleryViewModelTest.kt` for the new state: `rootState` (loading before first answer, root tiles ordered, cover file or `null`), `albumState(id)` (title, parent subtitle, `dd/MM/yyyy` date, description, sub-albums then photos ordered, empty flag), `viewerState(albumId, photoId)` (initial index, titles without extension)

### Implementation

- [X] T022 [US1] Cover download in `GallerySyncer` after save: for each distinct `cover_url` in the index without a file, `api.downloadFile(url)` → `mediaStore.saveCover`; failures skipped (R3 step 3) in `GAL/data/sync/GallerySyncer.kt`
- [X] T023 [P] [US1] UI models `AlbumTile`, `PhotoTile`, `PhotoImage` (`Original(file)`, `Preview(url)`, `None` — only `Original`/`None` are produced until US3), `GalleryRootUiState`, `AlbumUiState`, `PhotoViewerUiState`, `ViewerPhoto` per data-model.md in `GAL/presentation/state/`
- [X] T024 [US1] Rewrite `GalleryViewModel` (graph-scoped): inject `GalleryRepository`, `SyncGalleryUseCase`, `GalleryAutoDownloadUseCase`, `NetworkConnectivityObserver`, `WorkManager`; keep `downloadState` mapping and download actions; `init { sync() }` (gallery-open trigger); `rootState`, memoized `albumState(albumId)` and `viewerState(albumId, photoId)` built with `GalleryTree` on `Dispatchers.Default`; `retrySync()` in `GAL/presentation/viewmodel/GalleryViewModel.kt`
- [X] T025 [US1] Routes `GalleryMain`, `Album/{albumId}`, `Photo/{albumId}/{photoId}` and `GalleryNav(back, toAlbum(albumId), toPhoto(albumId, photoId))`; each destination resolves the graph-scoped ViewModel and collects its state with `collectAsStateWithLifecycle()` in `GAL/presentation/navigation/GalleryNavGraph.kt`
- [X] T026 [P] [US1] Components: `AlbumTile` (square cover from file, black `Box` when `null`, name below) and `PhotoTile` (renders `PhotoImage`: file via `AsyncImage`, grey box for `None`) in `GAL/presentation/components/GalleryComponents.kt`
- [X] T027 [US1] `GalleryScreen` (collector) + `GalleryContent(state, lambdas)`: keep the decision order and every banner/placeholder of the current screen (login, downloading, paused, waiting WiFi with "Usar dados móveis", 403 notice, 401/403/other placeholders, "Baixar Galeria Completa"); 2-column grid of root `AlbumTile`s; loading indicator while `isLoading`; "Não foi possível carregar a galeria. Verifique sua conexão." + "Tentar novamente" when no index and sync failed on network; "Nenhum álbum disponível." when index exists and has no roots — in `GAL/presentation/screens/GalleryScreen.kt`
- [X] T028 [US1] `AlbumScreen` + `AlbumContent(state, onAlbumClick, onPhotoClick, onBack)`: title + subtitle in the top bar, date and description when present, one `LazyVerticalGrid` with `GridItemSpan` so sub-albums take 2 columns and photos 3 (use a 6-column grid: album span 3, photo span 2), "Nenhuma foto neste álbum." when empty — in `GAL/presentation/screens/AlbumScreen.kt`
- [X] T029 [US1] `PhotoScreen` + `PhotoContent(state, lambdas)`: pager over `state.photos` from `initialIndex`, title = photo name without extension, keep zoom, save to device (display name = photo `name`, MIME from extension, not hardcoded `image/jpeg`) and share — in `GAL/presentation/screens/PhotoScreen.kt`
- [X] T030 [US1] Pass `isLoggedIn` unchanged and update `galleryGraph` call if its signature changed in `MAIN/core/presentation/navigation/AppNavHost.kt`

**Checkpoint**: US1 independent test passes on a device after one sync; T020–T021 green.

---

## Phase 4: User Story 2 — The copy follows the server (Priority: P1)

**Goal**: Deltas, deletions, full resync, disk reconcile, triggers (foreground, login, gallery open, 6 h), and missing
originals queued on WiFi through the existing download rules.

**Independent Test**: spec US2 — add, rename, move, reorder, delete on the server; foreground; all visible, moved file
kept, deleted file gone.

### Tests (write first)

- [X] T031 [P] [US2] Reconcile and resync cases in `GALT/data/sync/GallerySyncerTest.kt`: deleted photo → original deleted; deleted album subtree → originals and unused covers deleted; photo moved between albums keeps its file and no download is requested; `full_sync_required` → second call with `since = null`, index replaced, every original/cover not in it deleted; cover URL replaced → old file deleted, new downloaded; cover URL null → file deleted; shared cover kept while one album still uses it; `403` on the feed keeps index **and** files
- [X] T032 [P] [US2] Download worker reads the index in `GALT/data/work/GalleryDownloadWorkerTest.kt` (or adapt `GALT/data/repository/GalleryRepositoryImplDownloadTest.kt`): photos passed to the downloader are `allPhotosInTreeOrder()`; no index → sync first; sync `Failed(401|403)` → failure with that code; other failure → retry up to 3
- [X] T033 [P] [US2] `WorkManagerGallerySyncSchedulerTest` in `GALT/data/work/WorkManagerGallerySyncSchedulerTest.kt`: periodic 6 h, `CONNECTED`, unique `gallery_sync_periodic`, `KEEP`; `cancel()` cancels it

### Implementation

- [X] T034 [US2] `full_sync_required` in `GallerySyncer`: read again with `since = null`, `replaceWith`, save (R1) in `GAL/data/sync/GallerySyncer.kt`
- [X] T035 [US2] Reconcile after every successful save, under the lock (R3): delete originals whose id is not in the index; delete cover files whose name is not `sha1` of any album's `cover_url`; then covers download (T022); then publish refreshed file maps — in `GAL/data/sync/GallerySyncer.kt`
- [X] T036 [US2] Point `GalleryPhotoDownloader` at `GalleryMediaStore` (`exists` → `originalFile(id) != null`, `save` → `saveOriginal(id, ext, stream)`, drop metadata writes); logic, stop codes and `GalleryDownloadRun` unchanged — in `GAL/data/download/GalleryPhotoDownloader.kt`; adapt `GALT/data/download/GalleryPhotoDownloaderTest.kt`
- [X] T037 [US2] `GalleryDownloadWorker`: drop `api.getAllPhotos()`; load `repository.localState`; if no index run `repository.sync()` and map `Failed` like the old list failure (401/403 → `failure(message, code)`, others → retry up to `MAX_RETRIES`); feed `GalleryTree.allPhotosInTreeOrder()` (as DTO-equivalent input) to the downloader; `onAlbumDone`/final → `repository.refreshLocalFiles()` — in `GAL/data/work/GalleryDownloadWorker.kt`
- [X] T038 [P] [US2] `GallerySyncScheduler` interface (`schedulePeriodic()`, `cancel()`) in `GAL/domain/sync/GallerySyncScheduler.kt`; `GallerySyncWorker` (`@HiltWorker`, calls `SyncGalleryUseCase`; `Failed` → `Result.success()` so the period continues) in `GAL/data/work/GallerySyncWorker.kt`; `WorkManagerGallerySyncScheduler` (`PeriodicWorkRequest` 6 h, `CONNECTED`, `KEEP`, unique `gallery_sync_periodic`) in `GAL/data/work/WorkManagerGallerySyncScheduler.kt`; bind in `GAL/di/GalleryModule.kt`
- [X] T039 [US2] `GalleryAutoDownloadUseCase`: remove `triggerIfNeeded()`; `onLoginSuccess()` → `SyncGalleryUseCase()` + `syncScheduler.schedulePeriodic()`; add `onAppForeground()` → `SyncGalleryUseCase()` + `syncScheduler.schedulePeriodic()`; keep button methods — in `GAL/domain/usecase/GalleryAutoDownloadUseCase.kt`
- [X] T040 [US2] `CoreViewModel`: add `fun onAppForeground()` (launch `galleryAutoDownload.onAppForeground()` only when `authSession.isLoggedIn()`); drop the `triggerIfNeeded()` call in `startAppInitialization()`; login success calls the new suspend `onLoginSuccess()` inside `launch` — in `MAIN/core/presentation/viewmodel/CoreViewModel.kt`
- [X] T041 [US2] `LifecycleEventEffect(Lifecycle.Event.ON_START) { coreViewModel.onAppForeground() }` next to the `initialize()` effect in `MAIN/core/presentation/navigation/AppNavHost.kt`
- [X] T042 [US2] Root screen notices from `syncStatus`: `lastError` 403 with index → "Disponível apenas para membros." above the grid; network error with index → no message; wire in `GalleryViewModel.rootState` (`GAL/presentation/viewmodel/GalleryViewModel.kt`)
- [X] T043 [US2] Update `GALT/domain/usecase/GalleryAutoDownloadUseCaseTest.kt` for `onLoginSuccess`/`onAppForeground` (sync + periodic scheduled)

**Checkpoint**: US2 independent test passes; T031–T033, T043 green.

---

## Phase 5: User Story 6 — Logout leaves nothing behind (Priority: P1)

**Goal**: Sign-out cancels both works and any running sync, and deletes index, cursor, originals, covers and preview
cache.

**Independent Test**: spec US6 — sync, open albums online, sign out: gallery storage empty, no gallery work.

### Tests (write first)

- [X] T044 [P] [US6] In `GALT/data/sync/GallerySyncerTest.kt`: `clear()` during a suspended feed call cancels it and nothing is saved afterwards; after `clear()` snapshot, originals and covers are gone and `localState.index == null`; `sync()` with no session does nothing. In `GALT/domain/usecase/GalleryAutoDownloadUseCaseTest.kt`: `clearOnLogout()` cancels download and periodic sync, calls `repository.clear()` and clears the preview loader

### Implementation

- [X] T045 [US6] `GallerySyncer.clear()`: cancel the running sync job, then under the lock `snapshotCache.clear()`, `mediaStore.clearAll()`, reset published state — in `GAL/data/sync/GallerySyncer.kt`; expose through `GalleryRepositoryImpl.clear()`
- [X] T046 [US6] `GalleryAutoDownloadUseCase.clearOnLogout()`: `downloadScheduler.cancel()`, `syncScheduler.cancel()`, `repository.clear()`, `previewCache.clear()` (interface `GalleryPreviewCache` in `GAL/domain/repository/GalleryPreviewCache.kt`, implemented in T049) — in `GAL/domain/usecase/GalleryAutoDownloadUseCase.kt`

**Checkpoint**: quickstart scenario 12 passes; T044 green.

---

## Phase 6: User Story 3 — Previews before the download finishes (Priority: P2)

**Goal**: Photos without an original show `thumbnail_url` through the authenticated loader with a disk cache; grey
when neither is available.

**Independent Test**: spec US3 — on mobile data a new photo shows its preview; after WiFi download the tile uses the
original.

### Tests (write first)

- [X] T047 [P] [US3] In `GALT/presentation/viewmodel/GalleryViewModelTest.kt`: original on disk → `PhotoImage.Original`; no original + `thumbnailUrl` → `Preview(url)`; neither → `None`; viewer photo without original has `canSaveOrShare = false`; state switches to `Original` after `refreshLocalFiles()`

### Implementation

- [X] T048 [US3] `PhotoImage.Preview` produced by the ViewModel mapping (rule in data-model.md) in `GAL/presentation/viewmodel/GalleryViewModel.kt`
- [X] T049 [US3] `@GalleryThumbnailLoader` qualifier and `GalleryThumbnailLoaderModule` (`ImageLoader` on `@Client`, `respectCacheHeaders(false)`, disk cache `cacheDir/gallery_thumbs` max 100 MB) plus `GalleryPreviewCache` implementation clearing its disk and memory caches, in `GAL/di/GalleryThumbnailLoaderModule.kt`
- [X] T050 [US3] `PhotoTile` and the viewer render `Preview(url)` with the gallery loader (passed down from the Screen via the graph as a parameter, like `MemberPhotoLoader`), grey on error — in `GAL/presentation/components/GalleryComponents.kt`, `GAL/presentation/screens/AlbumScreen.kt`, `GAL/presentation/screens/PhotoScreen.kt`, `GAL/presentation/navigation/GalleryNavGraph.kt`
- [X] T051 [US3] Viewer: photo without original shows preview or grey, save/share buttons disabled — in `GAL/presentation/screens/PhotoScreen.kt`

**Checkpoint**: quickstart scenario 6 passes; T047 green.

---

## Phase 7: User Story 4 — Existing installs migrate without re-downloading (Priority: P2)

**Goal**: One-time move to the flat layout; orphans pruned by the first full sync (R3/R4).

**Independent Test**: spec US4 / quickstart scenario 13.

### Tests (write first)

- [X] T052 [P] [US4] `GalleryLegacyMigrationTest` in `GALT/data/local/GalleryLegacyMigrationTest.kt` (with `tempDirContext`): `gallery/{albumId}/{photoId}.jpg` moved to `gallery/photos/{photoId}.jpg` with identical bytes; `.json` files and album folders deleted; existing target → source deleted, target kept; rerun after a half-done run gives the same result; `photos/` and `covers/` folders are not treated as album folders; marker set; no API call
- [X] T053 [P] [US4] In `GALT/data/sync/GallerySyncerTest.kt`: migration runs before the first sync; after migration + first full sync, an original whose id is not in the feed is deleted and the others kept with zero `downloadFile` calls; migration not run again once the marker is set

### Implementation

- [X] T054 [US4] `GalleryPreferences`: replace `gallery_auto_download_triggered` with `gallery_layout_version: Int` (`layoutVersion()`, `setLayoutVersion(v)`) in `GAL/data/local/GalleryPreferences.kt`
- [X] T055 [US4] `GalleryLegacyMigration.runIfNeeded()` per R4 (numeric folders only; `renameTo`, fall back to copy+delete) in `GAL/data/local/GalleryLegacyMigration.kt`
- [X] T056 [US4] Call `migration.runIfNeeded()` under the lock at the start of `GallerySyncer.sync()` and in `GalleryRepositoryImpl.preload()` before listing files — in `GAL/data/sync/GallerySyncer.kt`, `GAL/data/repository/GalleryRepositoryImpl.kt`

**Checkpoint**: T052–T053 green.

---

## Phase 8: User Story 5 — Photo or album removed while open (Priority: P3)

**Goal**: Viewer moves on with "Esta foto foi removida" / "Esta foto foi movida para outro álbum"; an album screen
whose album is gone unwinds to the nearest existing ancestor with "Este álbum foi removido".

**Independent Test**: spec US5 / quickstart scenarios 7 and 8.

### Tests (write first)

- [X] T057 [P] [US5] In `GALT/presentation/viewmodel/GalleryViewModelTest.kt`: current photo deleted → target = next (previous when last), message `PhotoRemoved`; only photo deleted → `isClosed`; photo moved → `PhotoMoved`; album deleted → `albumState.isRemoved` and `AlbumRemoved`; message consumed once

### Implementation

- [X] T058 [US5] `GalleryMessage` (sealed) and consumable `StateFlow<GalleryMessage?>` with `consumeMessage()`; viewer tracks current photo via `onPageChanged(photoId)` and computes the new target on index change; album state sets `isRemoved` — in `GAL/presentation/state/GalleryMessage.kt`, `GAL/presentation/viewmodel/GalleryViewModel.kt`
- [X] T059 [US5] Screens: viewer scrolls the pager to the target and closes on `isClosed`; album screen calls `nav.back()` on `isRemoved`; the top screen shows the pending message in a snackbar and consumes it — in `GAL/presentation/screens/PhotoScreen.kt`, `GAL/presentation/screens/AlbumScreen.kt`, `GAL/presentation/screens/GalleryScreen.kt`

**Checkpoint**: T057 green; quickstart 7–8 pass.

---

## Phase 9: Polish & Cross-Cutting

- [X] T060 Remove dead code: `GAL/data/local/GalleryPhotoStorage.kt`, `GAL/domain/model/Album.kt`, `GAL/domain/usecase/DownloadAllPhotosUseCase.kt`, `getAllPhotos`/`getAlbumPhotos` and `DOWNLOAD_ALL_PHOTOS`/`DOWNLOAD_ALBUM` in `GAL/data/api/`; delete or port `GALT/data/local/GalleryPhotoStorageTest.kt`, `GALT/domain/usecase/DownloadAllPhotosUseCaseTest.kt`, `GALT/data/repository/GalleryRepositoryImplTest.kt`, `GALT/data/repository/GalleryRepositoryImplDownloadTest.kt`
- [X] T061 Rewrite `specs/gallery/spec.md` to the final state (endpoints used, index snapshot, disk layout, sync and reconcile, triggers table, download rules sections 3–4 kept and re-pointed to the index, previews rule amending "no screen loads a media URL", screens and routes, messages, logout) — same commit as the code
- [X] T062 Check 120-char lines, no magic strings (routes, keys, work names, messages as constants), no `Log.d`/`Timber.d` with member names, in all touched files under `GAL/`
- [X] T063 Run `./gradlew :app:testDebugUnitTest` (full suite, no `clean`) and fix failures
- [ ] T064 Run quickstart.md manual scenarios 1–14 on a device against backend `dev`

---

## Dependencies & Execution Order

### Phases

- **Setup (1)** → **Foundational (2)** → user stories.
- **US1 (3)** depends on Foundational.
- **US2 (4)** depends on Foundational; T035 uses T022 (covers) from US1; T042 edits the US1 ViewModel.
- **US6 (5)** depends on T017 (syncer) and T038 (sync scheduler) from US2.
- **US3 (6)** depends on US1 (tiles, ViewModel) and T046 (preview cache hook).
- **US4 (7)** depends on Foundational (T016, T017); independent of US1–US3.
- **US5 (8)** depends on US1 screens and ViewModel.
- **Polish (9)** after all stories.

### Within a story

Tests → models → data → ViewModel → screens. Tasks touching the same file (`GallerySyncer.kt`,
`GalleryViewModel.kt`, `GallerySyncerTest.kt`) run in sequence.

## Parallel Examples

```text
Phase 1:  T001, T002, T003 together; then T004, T005
Phase 2:  T006, T007, T008, T009 (tests) together; T010, T013, T014, T015 together; then T011 → T012; T016; T017 → T018 → T019
US1:      T020 + T021 (tests); T023 + T026 while T022 runs; then T024 → T025 → T027/T028/T029
US2:      T031, T032, T033 together; T036 and T038 in parallel with T034 → T035
US4:      T052 + T053 together; can run in parallel with US1/US2 after Phase 2
```

## Implementation Strategy

1. **MVP** = Phases 1–3 (US1): the tree, covers and order from one sync on gallery open. Already better than today,
   but deletions do not leave the device yet — do not release alone.
2. **Release candidate** = + US2 + US6 (all P1): the copy follows the server and nothing survives logout.
3. + US4 before shipping to existing users (without it, the update leaves old folders unreadable by the new layout).
4. + US3, US5, Polish. Commit spec and code together (T061).

**Release gate**: US1, US2, US4 and US6 are all required before the version reaches members; US3 and US5 can follow in
the same release if ready.
